"""Integration tests against a real HTTP server and an isolated SQLite database."""
import concurrent.futures
import json
import tempfile
import threading
import time
import unittest
import urllib.error
import urllib.request
from pathlib import Path
from server import Database, make_server


class ApiTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory()
        self.db=Database(Path(self.temp.name)/'test.db')
        self.server=make_server('127.0.0.1',0,self.db)
        self.thread=threading.Thread(target=self.server.serve_forever,daemon=True)
        self.thread.start()
        self.base=f'http://127.0.0.1:{self.server.server_port}'
        self.patient=self.register('patient','patient')
        self.dentist=self.register('dentist','dentist')
        self.http('POST','/me/availability',{'available':True},self.dentist)

    def tearDown(self):
        self.server.shutdown();self.server.server_close();self.thread.join();self.temp.cleanup()

    def http(self,method,path,data=None,token='',expected=200):
        req=urllib.request.Request(self.base+path,data=json.dumps(data).encode() if data is not None else None,method=method,headers={'Content-Type':'application/json','Authorization':'Bearer '+token})
        try:
            response=urllib.request.urlopen(req,timeout=10)
        except urllib.error.HTTPError as e:
            response=e
        with response:
            body=json.load(response);self.assertEqual(expected,response.status,body);return body

    def register(self,name,role,city='Joaçaba'):
        return self.http('POST','/auth/register',dict(name=name,email=name+'@test.local',password='Test12345!',role=role,city=city,clinic='Clínica teste',address='Rua de teste, 123',cro='SC000'))['token']

    def create(self,token=None,**changes):
        data=dict(symptom='Dor intensa',pain=8,swelling=True,bleeding=False,description='Teste fictício',city='Joaçaba')
        data.update(changes)
        return self.http('POST','/calls',data,token or self.patient)['call']

    def accept(self,call):
        return self.http('POST',f"/calls/{call['id']}/accept",{},self.dentist)['call']

    def test_full_lifecycle_and_persistence(self):
        call=self.create();self.assertEqual(1,call['position'])
        accepted=self.accept(call);self.assertEqual('accepted',accepted['status'])
        self.assertNotIn('arrival_code',accepted)
        patient_view=self.http('GET',f"/calls/{call['id']}",token=self.patient)['call']
        self.assertAlmostEqual(2700,patient_view['expires_at']-time.time(),delta=3)
        code=patient_view['arrival_code']
        self.http('POST',f"/calls/{call['id']}/status",dict(status='in_care',arrival_code=code),self.dentist)
        result=self.http('POST',f"/calls/{call['id']}/status",dict(status='completed',outcome='Teste concluído'),self.dentist)
        self.assertEqual('completed',result['call']['status'])
        fresh=Database(self.db.path)
        self.assertEqual('completed',fresh.handle('GET',f"/calls/{call['id']}",{},self.patient)['call']['status'])

    def test_authentication_and_logout(self):
        self.http('GET','/calls',expected=401)
        self.http('POST','/auth/login',dict(email='patient@test.local',password='wrong'),expected=401)
        self.http('POST','/auth/logout',{},self.patient)
        self.http('GET','/calls',token=self.patient,expected=401)

    def test_patient_isolation_and_role_permissions(self):
        call=self.create();other=self.register('another','patient')
        self.http('GET',f"/calls/{call['id']}",token=other,expected=403)
        self.http('POST',f"/calls/{call['id']}/accept",{},self.patient,expected=409)
        self.http('POST','/me/availability',{'available':True},self.patient,expected=403)

    def test_duplicate_active_call(self):
        self.create()
        self.http('POST','/calls',dict(symptom='Dor intensa',pain=5,swelling=False,bleeding=False,city='Joaçaba'),self.patient,expected=409)

    def test_validation(self):
        for pain in (-1,11,True,'8'):
            self.http('POST','/calls',dict(symptom='Dor intensa',pain=pain,swelling=False,bleeding=False,city='Joaçaba'),self.patient,expected=400)
        self.http('POST','/calls',dict(symptom='Other',pain=2,swelling=False,bleeding=False,city='Joaçaba'),self.patient,expected=400)

    def test_queue_order_and_decline(self):
        first=self.create();other=self.register('second','patient');second=self.create(other)
        self.http('POST',f"/calls/{second['id']}/accept",{},self.dentist,expected=409)
        self.http('POST',f"/calls/{first['id']}/decline",{},self.dentist)
        self.accept(second)
        self.assertEqual('queued',self.http('GET',f"/calls/{first['id']}",token=self.patient)['call']['status'])

    def test_queue_appears_before_closed_history(self):
        old=self.create();self.accept(old)
        self.http('POST',f"/calls/{old['id']}/cancel",dict(reason='Teste encerrado'),self.patient)
        current=self.create()
        self.assertEqual(current['id'],self.http('GET','/calls',token=self.dentist)['calls'][0]['id'])

    def test_simultaneous_accepts_have_one_winner(self):
        call=self.create();other=self.register('second-dentist','dentist')
        self.http('POST','/me/availability',dict(available=True),other)
        def accept(token):
            try:
                return self.db.handle('POST',f"/calls/{call['id']}/accept",{},token)['call']['dentist_id']
            except Exception:
                return None
        with concurrent.futures.ThreadPoolExecutor(2) as pool:
            results=list(pool.map(accept,[self.dentist,other]))
        self.assertEqual(1,sum(v is not None for v in results))

    def test_expiration_enforced_by_server(self):
        call=self.create();self.accept(call)
        with self.db.connect() as db:db.execute('UPDATE calls SET expires_at=? WHERE id=?',(int(time.time())-1,call['id']))
        self.assertEqual('expired',self.http('GET',f"/calls/{call['id']}",token=self.patient)['call']['status'])
        self.create()

    def test_wrong_arrival_code_and_transition(self):
        call=self.create();self.accept(call)
        code=self.http('GET',f"/calls/{call['id']}",token=self.patient)['call']['arrival_code']
        wrong='0000' if code!='0000' else '9999'
        self.http('POST',f"/calls/{call['id']}/status",dict(status='in_care',arrival_code=wrong),self.dentist,expected=400)
        self.http('POST',f"/calls/{call['id']}/status",dict(status='completed',outcome='Invalid'),self.dentist,expected=409)

    def test_cancellation_requires_reason_and_releases_patient(self):
        call=self.create()
        self.http('POST',f"/calls/{call['id']}/cancel",dict(reason=''),self.patient,expected=400)
        self.http('POST',f"/calls/{call['id']}/cancel",dict(reason='Não preciso mais'),self.patient)
        self.assertEqual(1,self.create()['position'])

    def test_city_and_availability_filter(self):
        self.create(city='Outra cidade')
        self.assertEqual([],self.http('GET','/calls',token=self.dentist)['calls'])
        self.http('POST','/me/availability',dict(available=False),self.dentist)
        self.assertEqual([],self.http('GET','/calls',token=self.dentist)['calls'])

    def test_return_status(self):
        call=self.create();self.accept(call)
        code=self.http('GET',f"/calls/{call['id']}",token=self.patient)['call']['arrival_code']
        self.http('POST',f"/calls/{call['id']}/status",dict(status='in_care',arrival_code=code),self.dentist)
        result=self.http('POST',f"/calls/{call['id']}/status",dict(status='return_needed',outcome='Reavaliar',return_days=7),self.dentist)
        self.assertEqual(7,result['call']['return_days'])
        self.http('POST',f"/calls/{call['id']}/status",dict(status='completed',outcome='Retorno realizado'),self.dentist)


if __name__=='__main__':unittest.main(verbosity=2)
