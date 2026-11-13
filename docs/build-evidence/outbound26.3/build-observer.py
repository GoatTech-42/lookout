from pathlib import Path
import subprocess,os,json,shutil
r=Path('/tmp/meshac-smoke-26.3');d=r/'outbound-observer';shutil.copytree(r/'move-observer',d,dirs_exist_ok=True)
(d/'src/test/meshac/OutboundDriver.java').write_text('''package test.meshac;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.phys.Vec3;
public class OutboundDriver implements DedicatedServerModInitializer {
 int ready;boolean done;
 public void onInitializeServer(){ServerTickEvents.END_SERVER_TICK.register(server->{if(done||server.getPlayerList().getPlayers().isEmpty())return;var p=server.getPlayerList().getPlayers().getFirst();var data=dev.meshac.data.PlayerDataManager.get(p);if(data==null||!data.isHandshakeComplete())return;if(++ready<80)return;done=true;
  System.out.println("[OUTBOUND-DRIVER] deliberate disposable packet scenario begins");
  p.connection.send(new ClientboundSetEntityMotionPacket(p.getId(),new Vec3(0.01,0.02,0.03)));
  p.connection.send(new ClientboundSetEntityMotionPacket(p.getId(),new Vec3(0.02,0.03,0.04)),future->System.out.println("[OUTBOUND-DRIVER] native caller completion success="+future.isSuccess()));
  p.connection.send(new ClientboundBundlePacket(java.util.List.of(new ClientboundSetEntityMotionPacket(p.getId(),new Vec3(0.03,0.04,0.05)))));
  p.connection.send(new ClientboundExplodePacket(p.position(),0,0,java.util.Optional.of(new Vec3(0.01,0.01,0.01)),net.minecraft.core.particles.ParticleTypes.EXPLOSION,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,net.minecraft.util.random.WeightedList.of(),false));
  System.out.println("[OUTBOUND-DRIVER] deliberate scenario queued");
 });}
}''')
(d/'src/test/meshac/mixin/OutboundTraceMixin.java').write_text('''package test.meshac.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=dev.meshac.packet.OutboundDispatcher.class,remap=false)
public class OutboundTraceMixin {
 @Inject(method="beforeVelocityWrite(Lnet/minecraft/network/Connection;Ljava/lang/Object;)Ljava/lang/Runnable;",at=@At("HEAD"))
 private static void prepare(net.minecraft.network.Connection c,Object p,CallbackInfoReturnable<Runnable> ci){System.out.println("[OUTBOUND-OBSERVER] PREPARE raw="+p.getClass().getSimpleName()+" id="+System.identityHashCode(p)+" thread="+Thread.currentThread().getName());}
}
''')
(d/'src/test/meshac/mixin/VelocityCompleteTraceMixin.java').write_text('''package test.meshac.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=dev.meshac.tracker.VelocityTracker.class,remap=false)
public class VelocityCompleteTraceMixin {
 @Inject(method="confirmVelocitySent",at=@At("HEAD"))private void velocity(CallbackInfo ci){System.out.println("[OUTBOUND-OBSERVER] COMPLETE velocity thread="+Thread.currentThread().getName());}
 @Inject(method="confirmExplosionSent",at=@At("HEAD"))private void explosion(CallbackInfo ci){System.out.println("[OUTBOUND-OBSERVER] COMPLETE explosion thread="+Thread.currentThread().getName());}
}
''')
config=json.loads((d/'classes/anchor-observer.mixins.json').read_text());config['mixins']+=['OutboundTraceMixin','VelocityCompleteTraceMixin'];(d/'classes/anchor-observer.mixins.json').write_text(json.dumps(config))
mod=json.loads((d/'classes/fabric.mod.json').read_text());mod['version']='test-only-outbound';mod['entrypoints']['server'].append('test.meshac.OutboundDriver');(d/'classes/fabric.mod.json').write_text(json.dumps(mod))
native=next((Path.home()/'.gradle-meshac/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.3').glob('*.jar'))
cp=[native,Path('/tmp/meshac-afterwrite/build/libs/meshac-26.3-2.9.7.28-meshac-0.1.0.jar')]+list((r/'server/libraries').rglob('*.jar'))+list((r/'observer/fabric-api').rglob('*.jar'))
jdk=Path.home()/'jdk25/bin';subprocess.run([str(jdk/'javac'),'-proc:none','-cp',os.pathsep.join(map(str,cp)),'-d',str(d/'classes'),*map(str,(d/'src').rglob('*.java'))],check=True)
subprocess.run([str(jdk/'jar'),'cf',str(d/'test-only-outbound-observer.jar'),'-C',str(d/'classes'),'.'],check=True)
