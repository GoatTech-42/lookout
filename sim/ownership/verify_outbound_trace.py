#!/usr/bin/env python3
"""Validate retained finite outbound native trace; this is not an all-version/runtime proof."""
from pathlib import Path
import sys,re

def validate(server,client,runner):
 assert 'Mixing ByteEncoderCompletionMixin from meshac.mixins.json into io.netty.handler.codec.MessageToByteEncoder' in server,'native superclass weave missing'
 assert 'MixinExtrasServiceImpl(version=0.5.5)' in server,'required MixinExtras not observed'
 assert '[OUTBOUND-DRIVER] deliberate disposable packet scenario begins' in server,'scenario start missing'
 assert '[OUTBOUND-DRIVER] deliberate scenario queued' in server,'scenario queued missing'
 assert 'gated=179/179' in server and 'mode=LOG_ONLY veil=false,false,false' in server,'containment state missing'
 assert 'NATIVE_EXCEPTION' not in server and 'Internal Exception:' not in client,'native exception/disconnect'
 assert 'DISCONNECT callback' not in server.split('Stopping the server')[0],'early client disconnect'
 assert 'Client disconnected with reason: Server closed' in client,'client deliberate close missing'
 assert 'Stopping the server' in server and 'OUTBOUND FINITE NATIVE ASSERTIONS PASS' in runner and 'CASE_BYTES_UNCHANGED True' in runner,'runner shutdown/completion missing'
 trace=server.split('[OUTBOUND-DRIVER] deliberate disposable packet scenario begins',1)[1]
 assert 'COMPLETE explosion' in trace,'explosion completion missing'
 trace=trace[:trace.index('COMPLETE explosion')+len('COMPLETE explosion')]+trace[trace.index('COMPLETE explosion')+len('COMPLETE explosion'):].split('\n',1)[0]
 raw=re.findall(r'\[OUTBOUND-OBSERVER\] PREPARE raw=([^ ]+) id=(\d+) thread=([^\n]+)',trace)
 motions=[(p,t) for kind,p,t in raw if kind=='ClientboundSetEntityMotionPacket'];explosions=[(p,t) for kind,p,t in raw if kind=='ClientboundExplodePacket']
 assert len(motions)==3 and len({p for p,t in motions})==3,'expected three distinct direct/twoarg/bundled native motions'
 assert len(explosions)==1,'expected one native explosion'
 assert all('Netty' in t for p,t in motions+explosions),'factory not on native channel loop'
 assert trace.count('[OUTBOUND-OBSERVER] COMPLETE velocity')==3,'velocity completion count'
 assert trace.count('[OUTBOUND-OBSERVER] COMPLETE explosion')==1,'explosion completion count'
 assert '[OUTBOUND-DRIVER] native caller completion success=true' in trace,'native caller listener failure'
 # Exactly one child between native bundle delimiters; outer is never encoded as a motion.
 delimiters=[m.start() for m in re.finditer('PREPARE raw=ClientboundBundleDelimiterPacket',trace)]
 assert len(delimiters)==2 and trace[delimiters[0]:delimiters[1]].count('PREPARE raw=ClientboundSetEntityMotionPacket')==1,'native unbundler child trace missing'
 for kind in ['ClientboundSetEntityMotionPacket','ClientboundExplodePacket']:
  for match in re.finditer('PREPARE raw='+kind,trace):
   segment=trace[match.end():];completion='COMPLETE '+('velocity' if kind.endswith('MotionPacket') else 'explosion')
   assert completion in segment,'completion missing after raw factory entry'
 print('Native outbound finite trace PASS: three native motions including bundled child; one explosion; completion callbacks and caller success; containment/shutdown retained')

if __name__=='__main__':
 if len(sys.argv)==4:validate(*(Path(p).read_text() for p in sys.argv[1:]))
 else:
  root=Path(__file__).resolve().parents[2]/'docs/build-evidence/outbound26.3';validate(*((root/p).read_text() for p in ['outbound-server.log','outbound-client.log','outbound-runner.log']))
