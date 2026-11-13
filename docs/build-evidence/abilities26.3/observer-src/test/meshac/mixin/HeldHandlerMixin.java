package test.meshac.mixin;
@org.spongepowered.asm.mixin.Mixin(value=net.minecraft.server.network.ServerGamePacketListenerImpl.class,priority=900)
public abstract class HeldHandlerMixin {
 @org.spongepowered.asm.mixin.injection.Inject(method="handleSetCarriedItem",at=@org.spongepowered.asm.mixin.injection.At("HEAD"))
 private void head(net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket p,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){test.meshac.HeldTrace.record("HANDLER_HEAD",p);}
 @org.spongepowered.asm.mixin.injection.Inject(method="handleSetCarriedItem",at=@org.spongepowered.asm.mixin.injection.At("TAIL"))
 private void tail(net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket p,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){test.meshac.HeldTrace.record("HANDLER_TAIL",p);}
}