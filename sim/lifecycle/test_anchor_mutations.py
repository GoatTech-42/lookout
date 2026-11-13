#!/usr/bin/env python3
"""Reject plausible bounded-anchor regressions using the actual-class ordering fixture."""
from pathlib import Path
import tempfile,shutil,subprocess,sys
r=Path(__file__).resolve().parents[2]
mutations=[
 ('handshake-default','data/PlayerData','private boolean handshakeComplete = false;','private boolean handshakeComplete = true;'),
 ('missing-early-publication','mixin/ServerGamePacketListenerSendMixin','dev.meshac.data.PlayerDataManager.onJoin(game.player);','{}'),
 ('replace-fabric-session','data/PlayerDataManager','if (current != null && current.getPlayer() == player) return;',''),
 ('anchor-before-login-write','mixin/ServerGamePacketListenerSendMixin','OutboundDispatcher.onSend(game.player, packet);','OutboundDispatcher.onSend(game.player, packet);\n if(packet instanceof net.minecraft.network.protocol.game.ClientboundLoginPacket)dev.meshac.data.PlayerDataManager.get(game.player).getConfirmationTracker().sendJoinConfirmation();'),
 ('no-anchor','mixin/ConnectionMixin','data.getConfirmationTracker().sendJoinConfirmation();','{}'),
 ('duplicate-anchor','tracker/ConfirmationTracker','if (joinConfirmationSent) return;',''),
 ('anchor-ID-after-send','tracker/ConfirmationTracker','sendPacket(joinConfirmationId);','short id = joinConfirmationId; joinConfirmationId = -69; sendPacket(id); joinConfirmationId = id;'),
 ('ignored-event-loop','tracker/ConfirmationTracker','if (inEventLoop) ConfirmationTransport.inEventLoop(playerData, () -> {','if (false) ConfirmationTransport.inEventLoop(playerData, () -> {'),
 ('no-play-guard','tracker/ConfirmationTransport','== ConnectionProtocol.PLAY','!= null'),
 ('no-close-recheck','tracker/ConfirmationTransport','if (connection == null || !connection.isConnected()) return false;','if (connection == null) return false;'),
 ('skip-ban-on-reuse','Meshac','if (activeBan != null','if (dev.meshac.data.PlayerDataManager.get(handler.getPlayer()) == null && activeBan != null'),
]
with tempfile.TemporaryDirectory(prefix='meshac-anchor-mutations-') as d:
 t=Path(d);shutil.copytree(r/'src',t/'src');shutil.copytree(r/'sim',t/'sim')
 for name,rel,old,new in mutations:
  p=t/'src/main/java/dev/meshac'/(rel+'.java');original=p.read_text();assert original.count(old)==1,(name,original.count(old));p.write_text(original.replace(old,new))
  result=subprocess.run([sys.executable,str(t/'sim/lifecycle/run_join_anchor.py')],text=True,capture_output=True)
  p.write_text(original)
  assert result.returncode!=0 and 'AssertionError' in result.stderr,(name,result.stdout,result.stderr)
  print('REJECTED:',name)
print('Join anchor mutations PASS')
