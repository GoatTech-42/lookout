#!/usr/bin/env python3
"""Mutation-check the retained native-constructed abilities trace validator."""
from pathlib import Path
from verify_abilities_trace import verify
r=Path(__file__).resolve().parents[2]/'docs/build-evidence/abilities26.3'
s,c,t=((r/f'abilities-constructed-{part}.log').read_text() for part in ['server','client','runner'])
verify(s,c,t)
line=next(x for x in s.splitlines(True) if '[ABILITIES-OBSERVER] DISPATCH packet=1 ' in x)
checks=[
 ('duplicate-dispatch',s.replace(line,line+line),c,t),
 ('missing-dispatch',s.replace(line,''),c,t),
 ('altered-field',s.replace('EVENT packet=1 flying=true','EVENT packet=1 flying=false'),c,t),
 ('reordered-stages',s.replace('ADMITTED_CHANNEL packet=1','TEMP packet=1').replace('DISPATCH packet=1','ADMITTED_CHANNEL packet=1').replace('TEMP packet=1','DISPATCH packet=1'),c,t),
 ('wrong-thread',s.replace('flying=true thread=Netty','flying=true thread=Server'),c,t),
 ('wrong-direction',s.replace('EVENT_META inbound=true','EVENT_META inbound=false'),c,t),
 ('no-live-entry',s.replace('livePLAY=true','livePLAY=false'),c,t),
 ('no-shutdown',s.replace('Stopping the server',''),c,t),
 ('no-client-close',s,c.replace('Server closed',''),t),
 ('truncated-runner',s,c,t.split('ABILITIES NATIVE CONSTRUCTED HANDOFF ASSERTIONS PASS')[0]),
]
for folder in ['abilities-interrupted','abilities-failed-input','abilities-failed-hotbar','failed-readiness']:
 checks.append(('blocked-'+folder,*((r/folder/f'abilities-{part}.log').read_text() for part in ['server','client','runner'])))
checks.append(('constructed-first-no-movement',*((r/'abilities-constructed-first'/f'abilities-constructed-{part}.log').read_text() for part in ['server','client','runner'])))
for name,a,b,d in checks:
 try:verify(a,b,d)
 except AssertionError:print('REJECTED:',name)
 else:raise AssertionError('mutation survived '+name)
print('Native-constructed abilities validator mutation checks PASS')
