#!/usr/bin/env python3
"""Finite held-item ingress guard and replay mutation checks."""
from pathlib import Path
import tempfile,shutil,subprocess,sys,os
r=Path(__file__).resolve().parents[2]
mutations=[
('wrong-heldIngress-descriptor', 'ConnectionMixin', 'target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"))\n    private void meshac$heldIngress', 'target = "Lnet/minecraft/network/Connection;wrongTarget(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"))\n    private void meshac$heldIngress'),
('late-heldIngress-shift', 'ConnectionMixin', 'target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"))\n    private void meshac$heldIngress', 'target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V", shift = At.Shift.AFTER))\n    private void meshac$heldIngress'),
 ('missing-channel-dispatch','ConnectionMixin','dev.meshac.packet.PacketDispatcher.onHeldItemChange(game.player, held);','{}'),
 ('replayed-head-dispatch','ServerGamePacketListenerImplMixin','\t@Inject(method = "handleClientCommand"','\t@Inject(method = "handleSetCarriedItem", at = @At("HEAD")) private void fixture$replay(net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket p, CallbackInfo ci){PacketDispatcher.onHeldItemChange(player,p);}\n\t@Inject(method = "handleClientCommand"'),
 ('missing-held-listener-guard','ConnectionMixin','if (!(packet instanceof net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket held)\n                || !(getPacketListener() instanceof ServerGamePacketListenerImpl game)\n                || game.player.connection != game) return;','if (!(packet instanceof net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket held)\n                || !(getPacketListener() instanceof ServerGamePacketListenerImpl game)) return;'),
 ('missing-held-transport-guard','ConnectionMixin','if (data != null && dev.meshac.tracker.ConfirmationTransport.isPlay(data, (Connection) (Object) this))\n            dev.meshac.packet.PacketDispatcher.onHeldItemChange(game.player, held);','if (data != null) dev.meshac.packet.PacketDispatcher.onHeldItemChange(game.player, held);'),
]
with tempfile.TemporaryDirectory(prefix='meshac-held-mutations-') as d:
 t=Path(d);shutil.copytree(r/'src',t/'src');shutil.copytree(r/'sim',t/'sim')
 for name,cls,old,new in mutations:
  p=t/'src/main/java/dev/meshac/mixin'/(cls+'.java');original=p.read_text();assert original.count(old)==1,(name,original.count(old));p.write_text(original.replace(old,new))
  result=subprocess.run([sys.executable,str(t/'sim/ownership/run_rest_ingress.py')],env=os.environ|{'MESHAC_PREP_OUTPUT':str(t/'output')},text=True,capture_output=True);p.write_text(original)
  assert result.returncode and 'AssertionError' in result.stderr,(name,result.stdout,result.stderr)
  print('REJECTED:',name)
print('Finite held-item mutation checks PASS')
