from pathlib import Path
import subprocess,os,json,shutil
r=Path('/tmp/meshac-smoke-26.3');d=r/'held-observer';shutil.copytree(r/'outbound-observer',d,dirs_exist_ok=True)
(d/'src/test/meshac/HeldTrace.java').write_text('''package test.meshac;
public final class HeldTrace {
 private static final java.util.Map<Object,Integer> ids=new java.util.IdentityHashMap<>();private static int next;
 public static synchronized void record(String stage,net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket p){int id=ids.computeIfAbsent(p,k->++next);System.out.println("[HELD-OBSERVER] "+stage+" packet="+id+" slot="+p.getSlot()+" thread="+Thread.currentThread().getName());}
}''')
for src,name in [('MovementConnectionMixin','HeldConnectionMixin'),('MovementDispatcherMixin','HeldDispatcherMixin')]:
 s=(d/f'src/test/meshac/mixin/{src}.java').read_text().replace(src,name).replace('ServerboundMovePlayerPacket','ServerboundSetCarriedItemPacket').replace('MovementTrace','HeldTrace').replace('method="onMove"','method="onHeldItemChange"').replace('ServerboundSetCarriedItemPacket)test.meshac.HeldTrace.record("ADMITTED_CHANNEL",p)', 'ServerboundSetCarriedItemPacket held)test.meshac.HeldTrace.record("ADMITTED_CHANNEL",held)')
 (d/f'src/test/meshac/mixin/{name}.java').write_text(s)
(d/'src/test/meshac/mixin/HeldHandlerMixin.java').write_text('''package test.meshac.mixin;
@org.spongepowered.asm.mixin.Mixin(value=net.minecraft.server.network.ServerGamePacketListenerImpl.class,priority=900)
public abstract class HeldHandlerMixin {
 @org.spongepowered.asm.mixin.injection.Inject(method="handleSetCarriedItem",at=@org.spongepowered.asm.mixin.injection.At("HEAD"))
 private void head(net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket p,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){test.meshac.HeldTrace.record("HANDLER_HEAD",p);}
 @org.spongepowered.asm.mixin.injection.Inject(method="handleSetCarriedItem",at=@org.spongepowered.asm.mixin.injection.At("TAIL"))
 private void tail(net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket p,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){test.meshac.HeldTrace.record("HANDLER_TAIL",p);}
}''')
(d/'src/test/meshac/mixin/HeldConversionMixin.java').write_text('''package test.meshac.mixin;
@org.spongepowered.asm.mixin.Mixin(value=dev.meshac.packet.Packets.class,remap=false)
public abstract class HeldConversionMixin {
 @org.spongepowered.asm.mixin.injection.Inject(method="toHeldItemChange",at=@org.spongepowered.asm.mixin.injection.At("HEAD"),remap=false)
 private static void convert(net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket p,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<dev.meshac.packet.wrapper.HeldItemChange> ci){test.meshac.HeldTrace.record("CONVERT",p);}
}'''.replace('dev.meshac.packet.wrapper.HeldItemChange','dev.meshac.packet.HeldItemChange'))
config=json.loads((d/'classes/anchor-observer.mixins.json').read_text());config['mixins']+=['HeldConnectionMixin','HeldDispatcherMixin','HeldHandlerMixin','HeldConversionMixin'];(d/'classes/anchor-observer.mixins.json').write_text(json.dumps(config))
mod=json.loads((d/'classes/fabric.mod.json').read_text());mod['version']='test-only-held';(d/'classes/fabric.mod.json').write_text(json.dumps(mod))
native=next((Path.home()/'.gradle-meshac/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.3').glob('*.jar'))
cp=[native,Path('/tmp/meshac-ingress-rest/build/libs/meshac-26.3-2.9.7.28-meshac-0.1.0.jar')]+list((r/'server/libraries').rglob('*.jar'))+list((r/'observer/fabric-api').rglob('*.jar'))
jdk=Path.home()/'jdk25/bin';subprocess.run([str(jdk/'javac'),'-proc:none','-cp',os.pathsep.join(map(str,cp)),'-d',str(d/'classes'),*map(str,(d/'src').rglob('*.java'))],check=True)
subprocess.run([str(jdk/'jar'),'cf',str(d/'test-only-held-observer.jar'),'-C',str(d/'classes'),'.'],check=True)
