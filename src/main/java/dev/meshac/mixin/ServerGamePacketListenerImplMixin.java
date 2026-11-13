package dev.meshac.mixin;

import dev.meshac.packet.MeshPacketType;
import dev.meshac.packet.PacketDispatcher;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Inbound game-packet hooks feeding the Vulcan-order dispatcher. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

	@Shadow public ServerPlayer player;

	private double meshac$fromX;
	private double meshac$fromY;
	private double meshac$fromZ;

	@Inject(method = "handleMovePlayer", at = @At("HEAD"))
	private void meshac$move(ServerboundMovePlayerPacket p, CallbackInfo ci) {
		meshac$fromX = player.getX();
		meshac$fromY = player.getY();
		meshac$fromZ = player.getZ();
		PacketDispatcher.onMove(player, p);
	}

	@Inject(method = "handleMovePlayer", at = @At("TAIL"))
	private void meshac$moveTail(ServerboundMovePlayerPacket p, CallbackInfo ci) {
		// Authoritative post-move feed, replacing Bukkit's PlayerMoveEvent (MONITOR priority).
		dev.meshac.event.MoveEventDispatcher.fireMove(player, meshac$fromX, meshac$fromY, meshac$fromZ);
	}

	@Inject(method = "handleInteract", at = @At("HEAD"))
	private void meshac$interact(ServerboundInteractPacket p, CallbackInfo ci) {
		PacketDispatcher.onInteractEntity(player, p);
	}

	@Inject(method = "handlePlayerCommand", at = @At("HEAD"))
	private void meshac$command(ServerboundPlayerCommandPacket p, CallbackInfo ci) {
		PacketDispatcher.onEntityAction(player, p);
	}

	@Inject(method = "handlePlayerAction", at = @At("HEAD"))
	private void meshac$action(ServerboundPlayerActionPacket p, CallbackInfo ci) {
		PacketDispatcher.onDigging(player, p);
	}

	@Inject(method = "handleUseItemOn", at = @At("HEAD"))
	private void meshac$useOn(ServerboundUseItemOnPacket p, CallbackInfo ci) {
		PacketDispatcher.onPlacement(player, p);
	}

	@Inject(method = "handleSetCarriedItem", at = @At("HEAD"))
	private void meshac$carried(ServerboundSetCarriedItemPacket p, CallbackInfo ci) {
		PacketDispatcher.onHeldItemChange(player, p);
	}

	@Inject(method = "handlePlayerAbilities", at = @At("HEAD"))
	private void meshac$abilities(net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket p, CallbackInfo ci) {
		PacketDispatcher.onAbilities(player, p);
	}

	@Inject(method = "handleClientCommand", at = @At("HEAD"))
	private void meshac$clientCommand(ServerboundClientCommandPacket p, CallbackInfo ci) {
		if (p.getAction() == ServerboundClientCommandPacket.Action.PERFORM_RESPAWN)
			PacketDispatcher.onClientStatusRespawn(player, p);
	}
}
