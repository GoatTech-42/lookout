#!/usr/bin/env python3
"""Reject missing native weave/completion/bundle/shutdown evidence in retained logs."""
from pathlib import Path
from verify_outbound_trace import validate
root=Path(__file__).resolve().parents[2]/'docs/build-evidence/outbound26.3'
s,c,r=[(root/x).read_text() for x in ['outbound-server.log','outbound-client.log','outbound-runner.log']]
validate(s,c,r)
for name,a,b,d in [('no-weave',s.replace('Mixing ByteEncoderCompletionMixin','removed'),c,r),('lost-completion',s.replace('COMPLETE explosion','removed'),c,r),('no-bundle-child',s.replace('PREPARE raw=ClientboundBundleDelimiterPacket','removed'),c,r),('truncated-runner',s,c,''),('early-disconnect','DISCONNECT callback\n'+s,c,r)]:
 try:validate(a,b,d)
 except AssertionError as e:print('REJECTED',name,str(e))
 else:raise AssertionError(name+' passed')
print('Outbound native validator mutation checks PASS')
