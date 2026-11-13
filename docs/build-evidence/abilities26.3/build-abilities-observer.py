from pathlib import Path
import shutil,subprocess,json,os
r=Path('/tmp/meshac-smoke-26.3');d=r/'abilities-observer';shutil.copytree(r/'held-observer',d,dirs_exist_ok=True)
(d/'src/test/meshac/AbilitiesTrace.java').write_text('''package test.meshac;
public final class AbilitiesTrace {
 private static final java.util.Map<Object,Integer> ids=new java.util.IdentityHashMap<>();private static int next;
 public static synchronized void record(String stage,net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket p){int id=ids.computeIfAbsent(p,k->++next);System.out.println("[ABILITIES-OBSERVER] "+stage+" packet="+id+" flying="+p.isFlying()+" thread="+Thread.currentThread().getName());}
}''')
for src in ['HeldConnectionMixin','HeldDispatcherMixin','HeldHandlerMixin']:
 name=src.replace('Held','Abilities');s=(d/f'src/test/meshac/mixin/{src}.java').read_text().replace(src,name).replace('ServerboundSetCarriedItemPacket','ServerboundPlayerAbilitiesPacket').replace('HeldTrace','AbilitiesTrace').replace('onHeldItemChange','onAbilities').replace('handleSetCarriedItem','handlePlayerAbilities')
 if name=='AbilitiesDispatcherMixin':s=s.replace('net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket p,','Object raw,').replace('test.meshac.AbilitiesTrace.record("DISPATCH",p);','if(raw instanceof net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket p)test.meshac.AbilitiesTrace.record("DISPATCH",p);')
 (d/f'src/test/meshac/mixin/{name}.java').write_text(s)
(d/'src/test/meshac/mixin/AbilitiesEventMixin.java').write_text('''package test.meshac.mixin;
@org.spongepowered.asm.mixin.Mixin(value=dev.meshac.packet.MeshPacketEvent.class,remap=false)
public class AbilitiesEventMixin {
 @org.spongepowered.asm.mixin.injection.Inject(method="<init>",at=@org.spongepowered.asm.mixin.injection.At("TAIL"),remap=false)
 private void event(dev.meshac.packet.MeshPacketType type,net.minecraft.server.level.ServerPlayer player,Object raw,boolean inbound,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){if(type==dev.meshac.packet.MeshPacketType.PLAYER_ABILITIES&&raw instanceof net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket p){test.meshac.AbilitiesTrace.record("EVENT",p);System.out.println("[ABILITIES-OBSERVER] EVENT_META inbound="+inbound+" currentPlayer="+(dev.meshac.data.PlayerDataManager.get(player).getPlayer()==player));}}
}''')
config=json.loads((d/'classes/anchor-observer.mixins.json').read_text());config['mixins']+=['AbilitiesConnectionMixin','AbilitiesDispatcherMixin','AbilitiesHandlerMixin','AbilitiesEventMixin'];(d/'classes/anchor-observer.mixins.json').write_text(json.dumps(config))
mod=json.loads((d/'classes/fabric.mod.json').read_text());mod['version']='test-only-abilities-constructed';mod['entrypoints']['server'].append('test.meshac.AbilitiesDriver');(d/'classes/fabric.mod.json').write_text(json.dumps(mod))
shutil.copy('/tmp/AbilitiesDriver.java',d/'src/test/meshac/AbilitiesDriver.java')
native=next((Path.home()/'.gradle-meshac/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.3').glob('*.jar'))
cp=[native,Path('/tmp/meshac-abilities/build/libs/meshac-26.3-2.9.7.28-meshac-0.1.0.jar')]+list((r/'server/libraries').rglob('*.jar'))+list((r/'observer/fabric-api').rglob('*.jar'))
jdk=Path.home()/'jdk25/bin';subprocess.run([str(jdk/'javac'),'-proc:none','-cp',os.pathsep.join(map(str,cp)),'-d',str(d/'classes'),*map(str,(d/'src').rglob('*.java'))],check=True)
subprocess.run([str(jdk/'jar'),'cf',str(d/'test-only-abilities-observer.jar'),'-C',str(d/'classes'),'.'],check=True)
