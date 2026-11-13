package test.meshac.mixin;
@org.spongepowered.asm.mixin.Mixin(value=dev.meshac.packet.MeshPacketEvent.class,remap=false)
public class AbilitiesEventMixin {
 @org.spongepowered.asm.mixin.injection.Inject(method="<init>",at=@org.spongepowered.asm.mixin.injection.At("TAIL"),remap=false)
 private void event(dev.meshac.packet.MeshPacketType type,net.minecraft.server.level.ServerPlayer player,Object raw,boolean inbound,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){if(type==dev.meshac.packet.MeshPacketType.PLAYER_ABILITIES&&raw instanceof net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket p){test.meshac.AbilitiesTrace.record("EVENT",p);System.out.println("[ABILITIES-OBSERVER] EVENT_META inbound="+inbound+" currentPlayer="+(dev.meshac.data.PlayerDataManager.get(player).getPlayer()==player));}}
}