package test.meshac.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=dev.meshac.tracker.VelocityTracker.class,remap=false)
public class VelocityCompleteTraceMixin {
 @Inject(method="confirmVelocitySent",at=@At("HEAD"))private void velocity(CallbackInfo ci){System.out.println("[OUTBOUND-OBSERVER] COMPLETE velocity thread="+Thread.currentThread().getName());}
 @Inject(method="confirmExplosionSent",at=@At("HEAD"))private void explosion(CallbackInfo ci){System.out.println("[OUTBOUND-OBSERVER] COMPLETE explosion thread="+Thread.currentThread().getName());}
}
