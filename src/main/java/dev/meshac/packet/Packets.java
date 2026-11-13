package dev.meshac.packet;

import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Converters from 26.3 Mojmap serverbound packets to the ported wrapper records. */
public final class Packets {

	private Packets() {
	}

	public static Flying toFlying(ServerboundMovePlayerPacket p, net.minecraft.server.level.ServerPlayer player) {
		boolean hp = p.hasPosition();
		boolean hr = p.hasRotation();
		return new Flying(hp, hr,
				hp ? p.getX(player.getX()) : 0, hp ? p.getY(player.getY()) : 0, hp ? p.getZ(player.getZ()) : 0,
				hr ? p.getYRot(player.getYRot()) : 0, hr ? p.getXRot(player.getXRot()) : 0,
				p.isOnGround(), p.horizontalCollision());
	}

	public static MeshPacketType flyingType(ServerboundMovePlayerPacket p) {
		boolean hp = p.hasPosition();
		boolean hr = p.hasRotation();
		if (hp && hr) return MeshPacketType.POSITION_ROTATION;
		if (hp) return MeshPacketType.POSITION;
		if (hr) return MeshPacketType.ROTATION;
		return MeshPacketType.GROUND_ONLY;
	}

	public static InteractEntity toInteractEntity(ServerboundInteractPacket p) {
		// 26.3: interact is a flat record; attack moved to ServerboundAttackPacket (see toAttackEntity).
		InteractEntity.Action action = p.location() == null ? InteractEntity.Action.INTERACT : InteractEntity.Action.INTERACT_AT;
		return new InteractEntity(p.entityId(), action, p.hand(), p.location(), p.usingSecondaryAction());
	}

	public static InteractEntity toAttackEntity(ServerboundAttackPacket p) {
		return new InteractEntity(p.entityId(), InteractEntity.Action.ATTACK, InteractionHand.MAIN_HAND, null, false);
	}

	public static EntityAction toEntityAction(ServerboundPlayerCommandPacket p) {
		// 26.3: sneak moved out of this packet into ServerboundPlayerInputPacket (see PARITY.md).
		EntityAction.Action action = switch (p.getAction()) {
			case START_SPRINTING -> EntityAction.Action.START_SPRINTING;
			case STOP_SPRINTING -> EntityAction.Action.STOP_SPRINTING;
			case START_RIDING_JUMP -> EntityAction.Action.START_JUMP_HORSE;
			case STOP_RIDING_JUMP -> EntityAction.Action.STOP_JUMP_HORSE;
			case OPEN_INVENTORY -> EntityAction.Action.OPEN_INVENTORY;
			case START_FALL_FLYING -> EntityAction.Action.START_FLYING_WITH_ELYTRA;
			case STOP_SLEEPING -> null;
		};
		if (action == null) return null;
		return new EntityAction(action, p.getData());
	}

public static Digging toDigging(ServerboundPlayerActionPacket p) {
		Digging.Action action = switch (p.getAction().name()) {
			case "START_DESTROY_BLOCK" -> Digging.Action.START_DIGGING;
			case "CHANGE_DESTROY_DIRECTION" -> Digging.Action.CHANGE_DESTROY_DIRECTION;
			case "STAB" -> Digging.Action.STAB;
			case "ABORT_DESTROY_BLOCK" -> Digging.Action.CANCELLED_DIGGING;
			case "STOP_DESTROY_BLOCK" -> Digging.Action.FINISHED_DIGGING;
			case "RELEASE_USE_ITEM" -> Digging.Action.RELEASE_USE_ITEM;
			case "DROP_ALL_ITEMS" -> Digging.Action.DROP_ITEM_STACK;
			case "DROP_ITEM" -> Digging.Action.DROP_ITEM;
			case "SWAP_ITEM_WITH_OFFHAND" -> Digging.Action.SWAP_ITEM_WITH_OFFHAND;
			default -> throw new IllegalArgumentException("Unsupported native digging action: " + p.getAction());
		};
		return new Digging(action, p.getPos(), p.getDirection(), p.getSequence());
	}

	public static Placement toPlacement(ServerboundUseItemOnPacket p) {
		BlockHitResult hit = dev.meshac.compat.VersionBridge.hit(p);
		return new Placement(hit.getBlockPos(), hit.getDirection(), dev.meshac.compat.VersionBridge.hand(p),
				(float) hit.getLocation().x, (float) hit.getLocation().y, (float) hit.getLocation().z,
				hit.isInside(), dev.meshac.compat.VersionBridge.sequence(p));
	}

	public static HeldItemChange toHeldItemChange(ServerboundSetCarriedItemPacket p) {
		return new HeldItemChange(p.getSlot());
	}
}
