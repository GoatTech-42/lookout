#!/usr/bin/env python3
"""Behavioral mutation checks for the finite movement ingress boundary."""
from pathlib import Path
import tempfile,shutil,subprocess,sys,os
r=Path(__file__).resolve().parents[2]
mutations=[
('wrong-movementIngress-descriptor', 'mixin/ConnectionMixin', 'target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"))\n    private void meshac$movementIngress', 'target = "Lnet/minecraft/network/Connection;wrongTarget(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"))\n    private void meshac$movementIngress'),
('late-movementIngress-shift', 'mixin/ConnectionMixin', 'target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"))\n    private void meshac$movementIngress', 'target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V", shift = At.Shift.AFTER))\n    private void meshac$movementIngress'),
 ('no-ingress','mixin/ConnectionMixin','dev.meshac.packet.PacketDispatcher.onMove(game.player, move);','{}'),
 ('duplicate-handler-dispatch','mixin/ServerGamePacketListenerImplMixin','meshac$fromX = player.getX();','PacketDispatcher.onMove(player, p); meshac$fromX = player.getX();'),
 ('network-capture','mixin/ServerGamePacketListenerImplMixin','@Inject(method = "handleMovePlayer", at = @At(value = "INVOKE",\n            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",\n            shift = At.Shift.AFTER))','@Inject(method = "handleMovePlayer", at = @At("HEAD"))'),
 ('no-current-player-listener','mixin/ConnectionMixin','if (!(packet instanceof net.minecraft.network.protocol.game.ServerboundMovePlayerPacket move)\n                || !(getPacketListener() instanceof ServerGamePacketListenerImpl game)\n                || game.player.connection != game) return;','if (!(packet instanceof net.minecraft.network.protocol.game.ServerboundMovePlayerPacket move)\n                || !(getPacketListener() instanceof ServerGamePacketListenerImpl game)) return;'),
 ('no-transport-admission','mixin/ConnectionMixin','if (data != null && dev.meshac.tracker.ConfirmationTransport.isPlay(data, (Connection) (Object) this))\n            dev.meshac.packet.PacketDispatcher.onMove(game.player, move);','if (data != null) dev.meshac.packet.PacketDispatcher.onMove(game.player, move);'),
 ('wrong-ingress-target','mixin/ConnectionMixin','Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"))\n    private void meshac$movementIngress','Connection;wrongTarget(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"))\n    private void meshac$movementIngress'),
]
with tempfile.TemporaryDirectory(prefix='meshac-move-mutations-') as d:
 t=Path(d);shutil.copytree(r/'src',t/'src');shutil.copytree(r/'sim',t/'sim')
 for name,rel,old,new in mutations:
  p=t/'src/main/java/dev/meshac'/(rel+'.java');original=p.read_text();assert original.count(old)==1,(name,original.count(old));p.write_text(original.replace(old,new))
  result=subprocess.run([sys.executable,str(t/'sim/ownership/run_move_ingress.py')],env=os.environ|{'MESHAC_PREP_OUTPUT':str(t/'output')},text=True,capture_output=True);p.write_text(original)
  assert result.returncode!=0 and 'AssertionError' in result.stderr,(name,result.stdout,result.stderr)
  print('REJECTED:',name)
print('Movement ingress mutation checks PASS')
