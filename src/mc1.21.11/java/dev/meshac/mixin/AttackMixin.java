package dev.meshac.mixin;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** 1.21.11 carries ATTACK in interact; its native dispatcher selects the action. */
@Mixin(ServerboundInteractPacket.class)
public interface AttackMixin {
 @Accessor("entityId") int meshac$getEntityId();
}
