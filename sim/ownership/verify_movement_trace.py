#!/usr/bin/env python3
"""Verify finite native movement trace. An anchor-only success cannot satisfy these assertions."""
from pathlib import Path
import sys,json
def verify(text,clienttext,runnertext):
 import re,collections
 assert 'NATIVE_EXCEPTION' not in text,'native movement exception observed'
 assert 'Internal Exception:' not in clienttext,'client disconnected with internal exception'
 assert 'System chat: Stopping the server' in text and 'Stopping server' in text and 'Saving worlds' in text,'deliberate server shutdown missing'
 assert 'Client disconnected with reason: Server closed' in clienttext,'client did not observe deliberate server close'
 assert 'CASE_BYTES_UNCHANGED True' in runnertext and 'BOUNDED ANCHOR RUNTIME ASSERTIONS PASS; visual inspection separate' in runnertext and 'MOVEMENT QUALIFICATION PASS packets=' in runnertext,'runner completion/cleanup markers missing'
 prestop=text.split('Stopping the server')[0]
 assert 'DISCONNECT callback' not in prestop,'client disconnected before deliberate server stop'
 packets=collections.defaultdict(collections.Counter)
 sequences=collections.defaultdict(list)
 for stage,packet,thread in re.findall(r'\[MOVE-OBSERVER\] (ADMITTED_CHANNEL|DISPATCH|HANDLER_HEAD|SERVER_CAPTURE|HANDLER_TAIL) packet=(\d+) thread=([^\n]+)',text):
  packets[packet][stage]+=1
  sequences[packet].append(stage+(':server' if stage=='HANDLER_HEAD' and thread.strip()=='Server thread' else ':network' if stage=='HANDLER_HEAD' else ''))
  if stage in ['ADMITTED_CHANNEL','DISPATCH']:assert 'Netty' in thread,(stage,thread)
  if stage in ['SERVER_CAPTURE','HANDLER_TAIL']:assert thread.strip()=='Server thread',(stage,thread)
 assert len(packets)>=5,'too few native movements observed'
 for packet,counts in packets.items():assert counts=={'ADMITTED_CHANNEL':1,'DISPATCH':1,'HANDLER_HEAD':2,'SERVER_CAPTURE':1,'HANDLER_TAIL':1},(packet,counts)
 for packet,sequence in sequences.items():assert sequence==['ADMITTED_CHANNEL','DISPATCH','HANDLER_HEAD:network','HANDLER_HEAD:server','SERVER_CAPTURE','HANDLER_TAIL'],(packet,'native stage order',sequence)
 coords=re.findall(r'\[MOVE-OBSERVER\] EVENT_CALL from=([^ ]+) to=([^ ]+) thread=([^\n]+)',text)
 assert len(coords)==len(packets),'TAIL/event invocation counts differ'
 assert any(a!=b for a,b,_ in coords),'no actual client position change observed'
 captures=re.findall(r'\[MOVE-OBSERVER\] CAPTURE_COORD x=([^ ]+) y=([^ ]+) z=([^ ]+) thread=Server thread',text)
 assert [','.join(c) for c in captures]==[a for a,_,_ in coords],'authoritative from coordinates differ from successful server capture'
 assert all(t.strip()=='Server thread' for _,_,t in coords),'event invoked outside server thread'
 return {'packets':len(packets),'dispatches':sum(c['DISPATCH'] for c in packets.values()),'handlerHEAD':sum(c['HANDLER_HEAD'] for c in packets.values()),'serverCaptureTailEvents':len(coords),'changedPositions':sum(a!=b for a,b,_ in coords)}
if __name__=='__main__':
 print(json.dumps(verify(Path(sys.argv[1]).read_text(),Path(sys.argv[2]).read_text(),Path(sys.argv[3]).read_text()),indent=2))
