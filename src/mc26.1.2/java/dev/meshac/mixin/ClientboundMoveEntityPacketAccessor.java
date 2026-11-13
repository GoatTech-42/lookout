package dev.meshac.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;


@Mixin(ClientboundMoveEntityPacket.class)
public interface ClientboundMoveEntityPacketAccessor {

	@Accessor("entityId")
	int meshac$getEntityId();

	@Accessor("xa") short meshac$getXa();
	@Accessor("ya") short meshac$getYa();
	@Accessor("za") short meshac$getZa();

	@Accessor("yRot")
	byte meshac$getYRot();

	@Accessor("xRot")
	byte meshac$getXRot();

	@Accessor("hasRot")
	boolean meshac$hasRot();

	@Accessor("hasPos")
	boolean meshac$hasPos();

	@Accessor("onGround")
	boolean meshac$isOnGround();
}
