package test.meshac.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=dev.meshac.packet.OutboundDispatcher.class,remap=false)
public class OutboundTraceMixin {
 @Inject(method="beforeVelocityWrite(Lnet/minecraft/network/Connection;Ljava/lang/Object;)Ljava/lang/Runnable;",at=@At("HEAD"))
 private static void prepare(net.minecraft.network.Connection c,Object p,CallbackInfoReturnable<Runnable> ci){System.out.println("[OUTBOUND-OBSERVER] PREPARE raw="+p.getClass().getSimpleName()+" id="+System.identityHashCode(p)+" thread="+Thread.currentThread().getName());}
}
