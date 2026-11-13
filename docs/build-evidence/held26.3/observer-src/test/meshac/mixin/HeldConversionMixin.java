package test.meshac.mixin;
@org.spongepowered.asm.mixin.Mixin(value=dev.meshac.packet.Packets.class,remap=false)
public abstract class HeldConversionMixin {
 @org.spongepowered.asm.mixin.injection.Inject(method="toHeldItemChange",at=@org.spongepowered.asm.mixin.injection.At("HEAD"),remap=false)
 private static void convert(net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket p,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<dev.meshac.packet.HeldItemChange> ci){test.meshac.HeldTrace.record("CONVERT",p);}
}