package test.meshac.mixin;
@org.spongepowered.asm.mixin.Mixin(value=net.minecraft.server.network.ServerGamePacketListenerImpl.class,priority=900)
public abstract class AbilitiesHandlerMixin {
 @org.spongepowered.asm.mixin.injection.Inject(method="handlePlayerAbilities",at=@org.spongepowered.asm.mixin.injection.At("HEAD"))
 private void head(net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket p,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){test.meshac.AbilitiesTrace.record("HANDLER_HEAD",p);}
 @org.spongepowered.asm.mixin.injection.Inject(method="handlePlayerAbilities",at=@org.spongepowered.asm.mixin.injection.At("TAIL"))
 private void tail(net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket p,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){test.meshac.AbilitiesTrace.record("HANDLER_TAIL",p);}
}