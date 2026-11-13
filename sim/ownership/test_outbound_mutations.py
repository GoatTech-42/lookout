#!/usr/bin/env python3
"""Reject wrong encoded-write timing, promise policy and callback ownership."""
from pathlib import Path
import tempfile,shutil,subprocess,sys,os
r=Path(__file__).resolve().parents[2]
mutations=[
 ('no-task','mixin/ByteEncoderCompletionMixin','promise.addListener(future -> task.run());','{}','run_outbound_encoded'),
 ('method-return','mixin/ByteEncoderCompletionMixin','promise.addListener(future -> task.run());','task.run();','run_outbound_encoded'),
 ('late-registration','mixin/ByteEncoderCompletionMixin','promise.addListener(future -> task.run());','ChannelFuture result = original.call(context, encoded, promise); promise.addListener(future -> task.run()); return result;','run_outbound_encoded'),
 ('success-only','mixin/ByteEncoderCompletionMixin','future -> task.run()','future -> { if (future.isSuccess()) task.run(); }','run_outbound_encoded'),
 ('void-not-unvoided','mixin/ByteEncoderCompletionMixin','promise = promise.unvoid();','{}','run_outbound_encoded'),
 ('native-promise-replaced','mixin/ByteEncoderCompletionMixin','promise = promise.unvoid();','promise = context.newPromise();','run_outbound_encoded'),
 ('non-native-encoder','mixin/ByteEncoderCompletionMixin','(Object) this instanceof PacketEncoder<?>','true','run_outbound_encoded'),
 ('empty-buffer','mixin/ByteEncoderCompletionMixin','&& bytes.isReadable()','','run_outbound_encoded'),
 ('no-session-recheck','packet/OutboundDispatcher','if (dev.meshac.tracker.ConfirmationTransport.isPlay(data, connection)\n                    && data.getPlayer().connection == connection.getPacketListener()) task.run();','task.run();','run_outbound_write'),
 ('no-listener-recheck','packet/OutboundDispatcher','&& data.getPlayer().connection == connection.getPacketListener()','&& true','run_outbound_write'),
 ('immediate-velocity-confirmation','tracker/VelocityTracker','return this::confirmVelocitySent;','confirmVelocitySent(); return null;','run_outbound_write'),
 ('no-self-entity-filter','packet/OutboundDispatcher','&& dev.meshac.compat.VersionBridge.motionEntity(motion) == data.getEntityId()','&& true','run_outbound_write'),
]
with tempfile.TemporaryDirectory(prefix='meshac-encoded-mutations-') as d:
 t=Path(d);shutil.copytree(r/'src',t/'src');shutil.copytree(r/'sim',t/'sim')
 for name,rel,old,new,test in mutations:
  p=t/'src/main/java/dev/meshac'/(rel+'.java');original=p.read_text();assert original.count(old)==1,(name,original.count(old));p.write_text(original.replace(old,new))
  result=subprocess.run([sys.executable,str(t/'sim/ownership'/(test+'.py'))],env=os.environ|{'MESHAC_PREP_OUTPUT':str(t/'output')},text=True,capture_output=True);p.write_text(original)
  assert result.returncode and ('AssertionError' in result.stderr or (name=='void-not-unvoided' and 'IllegalStateException: void future' in result.stderr)),(name,result.stdout,result.stderr)
  print('REJECTED:',name,flush=True)
print('Outbound encoded-write mutation checks PASS')
