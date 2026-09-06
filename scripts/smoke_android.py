"""Real Android UI smoke test using the SDK's adb and UI Automator hierarchy.

Run only against the fictitious --demo server. It creates one demonstration call.
Usage: python scripts/smoke_android.py --adb PATH_TO_ADB --serial emulator-5554
"""
import argparse
import json
import re
import subprocess
import time
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

parser=argparse.ArgumentParser()
parser.add_argument('--adb',default='adb')
parser.add_argument('--serial',default='emulator-5554')
parser.add_argument('--out',default='docs/screenshots')
args=parser.parse_args()
out=Path(args.out);out.mkdir(parents=True,exist_ok=True)


def adb(*parts):
    return subprocess.check_output([args.adb,'-s',args.serial,*parts],timeout=30)


def tree():
    adb('shell','uiautomator','dump','/sdcard/odonto-ui.xml')
    return ET.fromstring(adb('shell','cat','/sdcard/odonto-ui.xml'))


def nodes():return list(tree().iter('node'))


def center(node):
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.attrib['bounds']))
    return ((x1+x2)//2,(y1+y2)//2)


def scroll(root):
    view=next((n for n in root if n.attrib.get('scrollable')=='true'),None)
    if view is None:return False
    x1,y1,x2,y2=map(int,re.findall(r'\d+',view.attrib['bounds']))
    adb('shell','input','swipe',str((x1+x2)//2),str(y2-100),str((x1+x2)//2),str(y1+100),'350')
    return True


def click(label,allow_scroll=True):
    for _ in range(7):
        current=nodes()
        found=next((n for n in current if n.attrib.get('text')==label and n.attrib.get('enabled')=='true'),None)
        if found is not None:
            x,y=center(found);adb('shell','input','tap',str(x),str(y));time.sleep(.4);return
        if allow_scroll:scroll(current)
        time.sleep(.3)
    raise AssertionError('Control not found: '+label+'; visible: '+str([n.attrib.get('text') for n in current]))


def wait(label):
    for _ in range(8):
        current=nodes()
        if any(label in n.attrib.get('text','') for n in current):return current
        time.sleep(.5)
    raise AssertionError('Screen not found: '+label+'; visible: '+str([n.attrib.get('text') for n in current]))


def shot(name):
    (out/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'))
    print('Verified:',name,flush=True)


def text_input(value,resource=None):
    current=nodes()
    target=next(n for n in current if n.attrib.get('class')=='android.widget.EditText' and (resource is None or n.attrib.get('resource-id','').endswith('/'+resource)))
    x,y=center(target);adb('shell','input','tap',str(x),str(y));adb('shell','input','text',value.replace(' ','%s'))


def logout():
    click('Opções',False);click('Sair da conta',False);wait('Sou paciente')


def login(role):
    click('Sou '+role);wait('Que bom ter você aqui');click('Entrar na demonstração');wait('Olá,')


def demo_api(path,data=None,token=''):
    request=urllib.request.Request('http://127.0.0.1:8080'+path,data=json.dumps(data).encode() if data is not None else None,headers={'Content-Type':'application/json','Authorization':'Bearer '+token})
    with urllib.request.urlopen(request,timeout=10) as response:return json.load(response)

# Isolate repeat runs from previous calls belonging to the fictitious demo account.
token=demo_api('/auth/login',dict(email='paciente@demo.local',password='Odonto123!'))['token']
for call in demo_api('/calls',token=token)['calls']:
    if call['status'] in ('queued','accepted','in_care','return_needed'):
        demo_api(f"/calls/{call['id']}/cancel",dict(reason='Encerramento do teste demonstrativo anterior'),token)
demo_api('/auth/logout',{},token)
adb('shell','pm','clear','br.edu.odontoagora')
adb('shell','am','start','-n','br.edu.odontoagora/.MainActivity')
wait('Sou paciente');shot('01-acesso')
login('paciente');shot('02-painel-paciente')
click('Solicitar atendimento agora');wait('O que aconteceu?');shot('03-triagem')
click('Buscar atendimento agora →');wait('Procurando dentista disponível');shot('04-buscando')
logout();login('dentista');shot('05-fila-dentista');click('Ver atendimento');wait('Chamado urgente');shot('06-chamado-dentista')
click('Aceitar atendimento →');wait('Vaga reservada');shot('07-reserva-dentista')
logout();login('paciente');click('Ver atendimento');wait('Dentista encontrado!');shot('08-confirmado')
current=nodes()
for _ in range(4):
    code=next((n.attrib['text'] for n in current if re.fullmatch(r'\d{4}',n.attrib.get('text',''))),None)
    if code:break
    scroll(current);current=nodes()
assert code,'Arrival code not displayed'
logout();login('dentista');click('Ver atendimento');wait('Vaga reservada')
click('Confirmar chegada do paciente');wait('Peça ao paciente');text_input(code);click('Confirmar',False);wait('Cuidado em andamento');shot('09-em-atendimento')
click('Concluir atendimento');wait('Desfecho / orientação');text_input('Atendimento demonstrativo concluido',resource='outcome');click('Salvar',False);wait('Atendimento concluído');shot('10-concluido-dentista')
logout();login('paciente');click('Ver atendimento');wait('Atendimento concluído');shot('11-concluido-paciente')
logout();shot('12-pronto-para-apresentar')
print('PASS: login, triage, create, queue, dentist accept, patient confirmation, arrival code, completion, both profiles and logout.',flush=True)
