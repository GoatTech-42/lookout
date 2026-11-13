#!/usr/bin/env python3
"""Mutation checks against retained complete native logs, no native relaunch."""
from pathlib import Path
from verify_movement_trace import verify
r=Path(__file__).resolve().parents[2]/'docs/build-evidence/move26.3';server=(r/'move-server.log').read_text();client=(r/'move-client.log').read_text();runner=(r/'move-runner.log').read_text()
print('FINAL GREEN',verify(server,client,runner))
mutations=[('swap-admission-dispatch',server.replace('ADMITTED_CHANNEL','TEMP_STAGE').replace('DISPATCH packet=','ADMITTED_CHANNEL packet=').replace('TEMP_STAGE','DISPATCH'),client,runner),('truncate-before-stop',server.split('System chat: Stopping the server')[0],client,runner),('missing-client-close',server,client.replace('Client disconnected with reason: Server closed','removed-close-marker'),runner),('missing-runner-completion',server,client,runner.split('MOVEMENT QUALIFICATION PASS packets=')[0]),('failed-native-run',(r/'failure2/move-server.log').read_text(),(r/'failure2/move-client.log').read_text(),(r/'failure2/move-runner.log').read_text())]
for name,s,c,l in mutations:
 try:verify(s,c,l)
 except AssertionError as e:print('REJECTED:',name,str(e)[:180])
 else:raise AssertionError('validator accepted '+name)
print('Movement validator mutation checks PASS')
