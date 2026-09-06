"""API REST do MVP acadêmico OdontoAgora. Python 3.10+, somente biblioteca padrão.

Uso: python server.py --demo
Servidor local de demonstração. Para publicar, usar HTTPS/reverse proxy e revisão de segurança.
"""
import argparse
import hashlib
import hmac
import json
import re
import secrets
import sqlite3
import time
import unicodedata
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit

ACTIVE = ('queued', 'accepted', 'in_care', 'return_needed')
SYMPTOMS = ('Dor intensa', 'Dente quebrado', 'Sangramento', 'Inchaço')


class ApiError(Exception):
    def __init__(self, code, message):
        self.code, self.message = code, message


def require(condition, message, code=400):
    if not condition:
        raise ApiError(code, message)


def field(data, key, minimum=1, maximum=160):
    value = data.get(key, '')
    require(isinstance(value, str), f'Campo inválido: {key}.')
    value = value.strip()
    require(minimum <= len(value) <= maximum, f'Confira o campo {key} ({minimum} a {maximum} caracteres).')
    return value


def city_key(city):
    return ''.join(c for c in unicodedata.normalize('NFKD', city.casefold()) if not unicodedata.combining(c)).strip()


def password_hash(password, salt):
    return hashlib.pbkdf2_hmac('sha256', password.encode(), bytes.fromhex(salt), 200_000).hex()


def public_user(row):
    return {k: row[k] for k in ('id', 'name', 'role', 'city', 'clinic', 'address', 'cro', 'available')}


class Connection(sqlite3.Connection):
    def __exit__(self, *args):
        try:
            return super().__exit__(*args)
        finally:
            self.close()


class Database:
    def __init__(self, path):
        self.path = str(path)
        Path(path).parent.mkdir(parents=True, exist_ok=True)
        with self.connect() as db:
            db.executescript('''
                PRAGMA journal_mode=WAL;
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY, name TEXT NOT NULL, email TEXT UNIQUE NOT NULL,
                    salt TEXT NOT NULL, password TEXT NOT NULL, role TEXT NOT NULL,
                    city TEXT NOT NULL, city_key TEXT NOT NULL, clinic TEXT NOT NULL DEFAULT '',
                    address TEXT NOT NULL DEFAULT '', cro TEXT NOT NULL DEFAULT '', available INTEGER NOT NULL DEFAULT 0);
                CREATE TABLE IF NOT EXISTS sessions (token TEXT PRIMARY KEY, user_id INTEGER NOT NULL REFERENCES users(id), expires INTEGER NOT NULL);
                CREATE TABLE IF NOT EXISTS calls (
                    id INTEGER PRIMARY KEY, patient_id INTEGER NOT NULL REFERENCES users(id),
                    dentist_id INTEGER REFERENCES users(id), symptom TEXT NOT NULL, pain INTEGER NOT NULL,
                    swelling INTEGER NOT NULL, bleeding INTEGER NOT NULL, description TEXT NOT NULL,
                    city TEXT NOT NULL, city_key TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'queued',
                    created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, expires_at INTEGER,
                    arrival_code TEXT, reason TEXT NOT NULL DEFAULT '', outcome TEXT NOT NULL DEFAULT '',
                    return_days INTEGER NOT NULL DEFAULT 0);
                CREATE TABLE IF NOT EXISTS declines (call_id INTEGER REFERENCES calls(id), dentist_id INTEGER REFERENCES users(id), PRIMARY KEY(call_id,dentist_id));
                CREATE UNIQUE INDEX IF NOT EXISTS one_active_patient ON calls(patient_id) WHERE status IN ('queued','accepted','in_care','return_needed');
                CREATE INDEX IF NOT EXISTS queue_city ON calls(city_key,status,created_at,id);
            ''')

    def connect(self):
        db = sqlite3.connect(self.path, timeout=10, factory=Connection)
        db.row_factory = sqlite3.Row
        db.execute('PRAGMA foreign_keys=ON')
        return db

    def register(self, db, data):
        name, email = field(data, 'name', 2, 80), field(data, 'email', 5, 160).lower()
        require(re.fullmatch(r'[^\s@]+@[^\s@]+\.[^\s@]+', email), 'Informe um e-mail válido.')
        password = field(data, 'password', 8, 128)
        role = field(data, 'role')
        require(role in ('patient', 'dentist'), 'Perfil inválido.')
        city = field(data, 'city', 2, 80)
        clinic = field(data, 'clinic', 2, 100) if role == 'dentist' else ''
        address = field(data, 'address', 5, 160) if role == 'dentist' else ''
        cro = field(data, 'cro', 3, 30) if role == 'dentist' else ''
        salt = secrets.token_hex(16)
        try:
            uid = db.execute('INSERT INTO users(name,email,salt,password,role,city,city_key,clinic,address,cro) VALUES(?,?,?,?,?,?,?,?,?,?)',
                (name,email,salt,password_hash(password,salt),role,city,city_key(city),clinic,address,cro)).lastrowid
        except sqlite3.IntegrityError:
            raise ApiError(409, 'Este e-mail já está cadastrado. Entre na sua conta.')
        return db.execute('SELECT * FROM users WHERE id=?', (uid,)).fetchone()

    def demo(self):
        with self.connect() as db:
            for name,email,role in [('Mariana Silva','paciente@demo.local','patient'),('Marina Lopes','dentista@demo.local','dentist')]:
                if not db.execute('SELECT id FROM users WHERE email=?',(email,)).fetchone():
                    self.register(db, dict(name=name,email=email,role=role,password='Odonto123!',city='Joaçaba',clinic='Clínica Sorriso Centro',address='Rua das Flores, 248 - Centro, Joaçaba',cro='SC 00000 (demonstração)'))
            db.execute("UPDATE users SET available=1 WHERE email='dentista@demo.local'")

    def expire(self, db):
        now = int(time.time())
        db.execute("UPDATE calls SET status='expired',reason='Prazo de chegada encerrado',updated_at=? WHERE status='accepted' AND expires_at<=?", (now, now))
        db.execute('DELETE FROM sessions WHERE expires<=?', (now,))

    def call_json(self, db, row, viewer):
        result = dict(row)
        result.pop('city_key', None)
        patient = db.execute('SELECT * FROM users WHERE id=?', (row['patient_id'],)).fetchone()
        result['patient_name'] = patient['name']
        result['dentist'] = public_user(db.execute('SELECT * FROM users WHERE id=?', (row['dentist_id'],)).fetchone()) if row['dentist_id'] else None
        result['position'] = db.execute("SELECT count(*) FROM calls WHERE city_key=? AND status='queued' AND (created_at<? OR (created_at=? AND id<=?))", (row['city_key'],row['created_at'],row['created_at'],row['id'])).fetchone()[0] if row['status']=='queued' else 0
        result['available_dentists'] = db.execute("SELECT count(*) FROM users u WHERE role='dentist' AND available=1 AND city_key=? AND NOT EXISTS(SELECT 1 FROM declines d WHERE d.dentist_id=u.id AND d.call_id=?)", (row['city_key'],row['id'])).fetchone()[0]
        # Arrival code is shown to the patient only and verified by the server.
        if viewer['role']=='dentist':
            result.pop('arrival_code', None)
        return result

    def handle(self, method, path, data, token):
        with self.connect() as db:
            # Serializes read/check/write operations, including competing accepts.
            db.execute('BEGIN IMMEDIATE')
            self.expire(db)
            if method=='GET' and path=='/health':
                return {'status':'ok','service':'OdontoAgora','version':'1.0','server_time':int(time.time())}
            if method=='POST' and path in ('/auth/register','/auth/login'):
                if path.endswith('register'):
                    user = self.register(db, data)
                else:
                    email = field(data,'email',5,160).lower()
                    password = field(data,'password',1,128)
                    user = db.execute('SELECT * FROM users WHERE email=?',(email,)).fetchone()
                    require(user and hmac.compare_digest(user['password'],password_hash(password,user['salt'])), 'E-mail ou senha incorretos.', 401)
                raw = secrets.token_urlsafe(32)
                db.execute('INSERT INTO sessions VALUES(?,?,?)',(hashlib.sha256(raw.encode()).hexdigest(),user['id'],int(time.time())+86400))
                return {'token':raw,'user':public_user(user)}
            user = db.execute('SELECT u.* FROM users u JOIN sessions s ON s.user_id=u.id WHERE s.token=? AND s.expires>?', (hashlib.sha256(token.encode()).hexdigest(),int(time.time()))).fetchone()
            require(user is not None, 'Sua sessão expirou. Entre novamente.', 401)
            uid = user['id']
            if path=='/auth/logout' and method=='POST':
                db.execute('DELETE FROM sessions WHERE token=?',(hashlib.sha256(token.encode()).hexdigest(),))
                return {'ok':True}
            if path=='/me' and method=='GET':
                return {'user':public_user(user)}
            if path=='/me/availability' and method=='POST':
                require(user['role']=='dentist','Apenas dentistas podem alterar o plantão.',403)
                require(type(data.get('available')) is bool,'Disponibilidade inválida.')
                db.execute('UPDATE users SET available=? WHERE id=?',(int(data['available']),uid))
                return {'user':public_user(db.execute('SELECT * FROM users WHERE id=?',(uid,)).fetchone())}
            if path=='/calls' and method=='GET':
                if user['role']=='patient':
                    rows=db.execute('SELECT * FROM calls WHERE patient_id=? ORDER BY id DESC LIMIT 100',(uid,)).fetchall()
                else:
                    rows=db.execute("SELECT * FROM calls c WHERE dentist_id=? OR (status='queued' AND city_key=? AND ?=1 AND NOT EXISTS(SELECT 1 FROM declines d WHERE d.call_id=c.id AND d.dentist_id=?)) ORDER BY CASE WHEN status IN ('accepted','in_care','return_needed') THEN 0 WHEN status='queued' THEN 1 ELSE 2 END, CASE WHEN status='queued' THEN created_at ELSE -created_at END, CASE WHEN status='queued' THEN id ELSE -id END LIMIT 100", (uid,user['city_key'],user['available'],uid)).fetchall()
                return {'calls':[self.call_json(db,r,user) for r in rows],'server_time':int(time.time())}
            if path=='/calls' and method=='POST':
                require(user['role']=='patient','Somente pacientes podem criar chamados.',403)
                symptom=field(data,'symptom')
                require(symptom in SYMPTOMS,'Selecione uma ocorrência válida.')
                pain=data.get('pain')
                require(type(pain) is int and 0<=pain<=10,'A dor deve estar entre 0 e 10.')
                require(type(data.get('swelling')) is bool and type(data.get('bleeding')) is bool,'Responda às perguntas da triagem.')
                city=field(data,'city',2,80)
                desc=field(data,'description',0,500)
                require(not db.execute("SELECT id FROM calls WHERE patient_id=? AND status IN ('queued','accepted','in_care','return_needed')",(uid,)).fetchone(),'Você já tem um chamado ativo.',409)
                now=int(time.time())
                cid=db.execute('INSERT INTO calls(patient_id,symptom,pain,swelling,bleeding,description,city,city_key,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?)', (uid,symptom,pain,int(data['swelling']),int(data['bleeding']),desc,city,city_key(city),now,now)).lastrowid
                return {'call':self.call_json(db,db.execute('SELECT * FROM calls WHERE id=?',(cid,)).fetchone(),user),'server_time':now}
            match=re.fullmatch(r'/calls/(\d+)(?:/(accept|decline|status|cancel))?',path)
            require(match is not None,'Rota não encontrada.',404)
            cid,action=int(match[1]),match[2]
            call=db.execute('SELECT * FROM calls WHERE id=?',(cid,)).fetchone()
            require(call is not None,'Chamado não encontrado.',404)
            owns=call['patient_id']==uid or call['dentist_id']==uid
            can_view=user['role']=='dentist' and call['status']=='queued' and call['city_key']==user['city_key'] and user['available'] and not db.execute('SELECT 1 FROM declines WHERE call_id=? AND dentist_id=?',(cid,uid)).fetchone()
            require(owns or can_view,'Você não tem acesso a este chamado.',403)
            if method=='GET' and action is None:
                return {'call':self.call_json(db,call,user),'server_time':int(time.time())}
            require(method=='POST' and action is not None,'Método não permitido.',405)
            now=int(time.time())
            if action=='accept':
                require(can_view and user['role']=='dentist','Chamado indisponível para aceite.',409)
                require(not db.execute("SELECT id FROM calls WHERE dentist_id=? AND status IN ('accepted','in_care')",(uid,)).fetchone(),'Conclua seu atendimento atual antes de aceitar outro.',409)
                first=db.execute("SELECT id FROM calls c WHERE status='queued' AND city_key=? AND NOT EXISTS(SELECT 1 FROM declines d WHERE d.call_id=c.id AND d.dentist_id=?) ORDER BY created_at,id LIMIT 1",(user['city_key'],uid)).fetchone()
                require(first and first[0]==cid,'Atenda o primeiro chamado da sua fila ou recuse-o antes de avançar.',409)
                db.execute("UPDATE calls SET dentist_id=?,status='accepted',expires_at=?,arrival_code=?,updated_at=? WHERE id=?",(uid,now+2700,str(secrets.randbelow(9000)+1000),now,cid))
            elif action=='decline':
                require(can_view,'Chamado indisponível.',409)
                db.execute('INSERT OR IGNORE INTO declines VALUES(?,?)',(cid,uid))
                return {'ok':True}
            elif action=='cancel':
                require(owns,'Somente os participantes podem cancelar.',403)
                require(call['status'] in ACTIVE,'Este chamado já foi encerrado.',409)
                reason=field(data,'reason',5,300)
                db.execute("UPDATE calls SET status='cancelled',reason=?,updated_at=? WHERE id=?",(reason,now,cid))
            elif action=='status':
                require(user['role']=='dentist' and call['dentist_id']==uid,'Apenas o dentista responsável pode atualizar o atendimento.',403)
                status=field(data,'status')
                transitions={'accepted':('in_care',),'in_care':('return_needed','completed'),'return_needed':('completed',)}
                require(status in transitions.get(call['status'],()),'Mudança de status não permitida.',409)
                if status=='in_care':
                    require(hmac.compare_digest(field(data,'arrival_code',4,4),call['arrival_code']),'Código de chegada incorreto.')
                outcome=field(data,'outcome',3,300) if status in ('completed','return_needed') else ''
                days=data.get('return_days',0)
                require(type(days) is int and 0<=days<=365,'Prazo de retorno inválido.')
                require(status!='return_needed' or days>0,'Informe o prazo de retorno.')
                db.execute('UPDATE calls SET status=?,outcome=?,return_days=?,updated_at=? WHERE id=?',(status,outcome,days,now,cid))
            return {'call':self.call_json(db,db.execute('SELECT * FROM calls WHERE id=?',(cid,)).fetchone(),user),'server_time':now}


class Handler(BaseHTTPRequestHandler):
    server_version = 'OdontoAgora/1.0'
    def log_message(self, fmt, *args):
        # No request bodies, tokens, symptoms or names in the access log.
        pass

    def dispatch(self):
        try:
            self.connection.settimeout(15)
            size=int(self.headers.get('Content-Length','0'))
            require(0<=size<=16384,'Corpo da requisição muito grande.',413)
            data=json.loads(self.rfile.read(size)) if size else {}
            require(isinstance(data,dict),'Envie um objeto JSON.')
            token=self.headers.get('Authorization','').removeprefix('Bearer ')
            result=self.server.database.handle(self.command,urlsplit(self.path).path,data,token)
            code=200
        except ApiError as error:
            code,result=error.code,{'error':error.message}
        except (ValueError,UnicodeDecodeError):
            code,result=400,{'error':'JSON ou parâmetro inválido.'}
        except Exception:
            code,result=500,{'error':'Erro interno. Tente novamente.'}
        body=json.dumps(result,ensure_ascii=False).encode()
        self.send_response(code)
        self.send_header('Content-Type','application/json; charset=utf-8')
        self.send_header('Cache-Control','no-store')
        self.send_header('X-Content-Type-Options','nosniff')
        self.send_header('Content-Length',str(len(body)))
        self.end_headers()
        try:
            self.wfile.write(body)
        except (BrokenPipeError,ConnectionResetError):
            pass

    do_GET=dispatch
    do_POST=dispatch


def make_server(host,port,database):
    server=ThreadingHTTPServer((host,port),Handler)
    server.database=database
    return server


if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--host',default='127.0.0.1')
    parser.add_argument('--port',type=int,default=8080)
    parser.add_argument('--db',default=str(Path(__file__).parent/'data/odontoagora.db'))
    parser.add_argument('--demo',action='store_true')
    args=parser.parse_args()
    database=Database(args.db)
    if args.demo:
        database.demo()
    server=make_server(args.host,args.port,database)
    print(f'OdontoAgora API: http://{args.host}:{args.port} | Ctrl+C para encerrar',flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        server.server_close()
