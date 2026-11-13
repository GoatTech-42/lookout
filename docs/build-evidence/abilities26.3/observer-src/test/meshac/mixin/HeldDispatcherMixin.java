package test.meshac.mixin;
@org.spongepowered.asm.mixin.Mixin(value=dev.meshac.packet.PacketDispatcher.class,remap=false)
public abstract class HeldDispatcherMixin {
 @org.spongepowered.asm.mixin.injection.Inject(method="onHeldItemChange",at=@org.spongepowered.asm.mixin.injection.At("HEAD"),remap=false)
 private static void dispatch(net.minecraft.server.level.ServerPlayer player,net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket p,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){test.meshac.HeldTrace.record("DISPATCH",p);}
}
