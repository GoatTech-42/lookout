package test.meshac.mixin;
@org.spongepowered.asm.mixin.Mixin(value=dev.meshac.event.MoveEventDispatcher.class,remap=false)
public abstract class MovementEventMixin {
 @org.spongepowered.asm.mixin.injection.Inject(method="fireMove",at=@org.spongepowered.asm.mixin.injection.At("HEAD"),remap=false)
 private static void event(net.minecraft.server.level.ServerPlayer player,double x,double y,double z,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){System.out.println("[MOVE-OBSERVER] EVENT_CALL from="+x+","+y+","+z+" to="+player.getX()+","+player.getY()+","+player.getZ()+" thread="+Thread.currentThread().getName());}
}
