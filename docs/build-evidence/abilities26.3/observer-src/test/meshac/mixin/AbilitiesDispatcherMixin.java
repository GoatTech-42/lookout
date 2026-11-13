package test.meshac.mixin;
@org.spongepowered.asm.mixin.Mixin(value=dev.meshac.packet.PacketDispatcher.class,remap=false)
public abstract class AbilitiesDispatcherMixin {
 @org.spongepowered.asm.mixin.injection.Inject(method="onAbilities",at=@org.spongepowered.asm.mixin.injection.At("HEAD"),remap=false)
 private static void dispatch(net.minecraft.server.level.ServerPlayer player,Object raw,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){if(raw instanceof net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket p)test.meshac.AbilitiesTrace.record("DISPATCH",p);}
}
