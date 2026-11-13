#!/usr/bin/env python3
"""Actual PlayerDataManager JOIN/QUIT against session/native boundary doubles. Not Fabric runtime."""
from pathlib import Path
import tempfile,subprocess,os
root=Path(__file__).resolve().parents[2];jdk=Path(os.environ.get('JAVA_HOME',str(Path.home()/'jdk25')))/'bin'
s={
'dev/meshac/MeshServices':'public class MeshServices {static final java.util.Map<java.util.UUID,dev.meshac.data.PlayerData> p=new java.util.HashMap<>();public static java.util.Map<java.util.UUID,dev.meshac.data.PlayerData> players(){return p;}}',
'dev/meshac/data/PlayerData':'public class PlayerData {int entityId=-42069;public PlayerData(net.minecraft.server.level.ServerPlayer p){}public void setEntityId(int i){entityId=i;}public int getEntityId(){return entityId;}public void setClientVersion(Object v){}public void setChecks(Object c){}}',
'dev/meshac/data/ClientVersionResolver':'public class ClientVersionResolver {public static Object resolve(Object c){return null;}}',
'dev/meshac/check/manager/CheckManager':'public class CheckManager {public static Object createChecks(Object d){return null;}}',
'dev/meshac/mixin/ServerCommonPacketListenerImplAccessor':'public interface ServerCommonPacketListenerImplAccessor {Object meshac$getConnection();}',
'dev/meshac/veil/Veil':'public class Veil {public static void grace(Object p,int t){}public static void forgetPlayer(Object p){}}',
'net/minecraft/world/entity/player/Player':'public class Player {final java.util.UUID uuid=java.util.UUID.randomUUID();public java.util.UUID getUUID(){return uuid;}}',
'net/minecraft/server/level/ServerPlayer':'public class ServerPlayer extends net.minecraft.world.entity.player.Player {public Object connection;public int id;public int getId(){return id;}public ServerPlayer level(){return this;}public ServerPlayer getServer(){return this;}public int getTickCount(){return 0;}}',
}
test='''public class JoinIdentityTest {public static void main(String[] args){
var p=new net.minecraft.server.level.ServerPlayer();p.id=37;
// Login packet already passed before Fabric JOIN: no OutboundDispatcher login invocation.
dev.meshac.data.PlayerDataManager.onJoin(p);
if(dev.meshac.data.PlayerDataManager.get(p).getEntityId()!=37)throw new AssertionError("JOIN did not capture native player entity ID without login hook");
var q=new net.minecraft.server.level.ServerPlayer();q.id=902;dev.meshac.data.PlayerDataManager.onJoin(q);
if(dev.meshac.data.PlayerDataManager.get(q).getEntityId()!=902||dev.meshac.data.PlayerDataManager.get(p).getEntityId()!=37)throw new AssertionError("cross-session identity mixup");
dev.meshac.data.PlayerDataManager.onQuit(p);if(dev.meshac.data.PlayerDataManager.get(p)!=null)throw new AssertionError("quit retained session");
p.id=88;dev.meshac.data.PlayerDataManager.onJoin(p);if(dev.meshac.data.PlayerDataManager.get(p).getEntityId()!=88)throw new AssertionError("rejoin reused old entity ID");
System.out.println("JoinIdentityTest PASS (missed-login, two sessions, quit/rejoin)");}}'''
with tempfile.TemporaryDirectory(prefix='meshac-join-') as d:
 t=Path(d);files=[]
 for n,body in s.items():
  p=t/(n+'.java');p.parent.mkdir(parents=True,exist_ok=True);p.write_text('package '+n.rsplit('/',1)[0].replace('/','.')+'; '+body);files.append(str(p))
 p=t/'JoinIdentityTest.java';p.write_text(test);files.extend([str(p),str(root/'src/main/java/dev/meshac/data/PlayerDataManager.java')])
 subprocess.run([str(jdk/'javac'),'-d',str(t/'classes'),*files],check=True)
 subprocess.run([str(jdk/'java'),'-cp',str(t/'classes'),'JoinIdentityTest'],check=True)
