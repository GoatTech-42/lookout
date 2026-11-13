package dev.meshac.packet;

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
		final InteractEntity.Action[] action = {InteractEntity.Action.INTERACT};
		final InteractionHand[] hand = {InteractionHand.MAIN_HAND};
		final Vec3[] target = {null};
		p.dispatch(new ServerboundInteractPacket.Handler() {
			@Override public void onInteraction(InteractionHand h) {
				action[0] = InteractEntity.Action.INTERACT;
				hand[0] = h;
			}
			@Override public void onInteraction(InteractionHand h, Vec3 pos) {
				action[0] = InteractEntity.Action.INTERACT_AT;
				hand[0] = h;
				target[0] = pos;
			}
			@Override public void onAttack() {
				action[0] = InteractEntity.Action.ATTACK;
			}
		});
		return new InteractEntity(p.getEntityId(), action[0], hand[0], target[0], p.isUsingSecondaryAction());
	}

	public static EntityAction toEntityAction(ServerboundPlayerCommandPacket p) {
		EntityAction.Action action = switch (p.getAction()) {
			case START_SNEAKING -> EntityAction.Action.START_SNEAKING;
			case STOP_SNEAKING -> EntityAction.Action.STOP_SNEAKING;
			case START_SPRINTING -> EntityAction.Action.START_SPRINTING;
			case STOP_SPRINTING -> EntityAction.Action.STOP_SPRINTING;
			case START_JUMP_HORSE -> EntityAction.Action.START_JUMP_HORSE;
			case STOP_JUMP_HORSE -> EntityAction.Action.STOP_JUMP_HORSE;
			case OPEN_INVENTORY -> EntityAction.Action.OPEN_INVENTORY;
			case START_FALL_FLYING -> EntityAction.Action.START_FLYING_WITH_ELYTRA;
		};
		return new EntityAction(action, p.getData());
	}

	public static Digging toDigging(ServerboundPlayerActionPacket p) {
		Digging.Action action = switch (p.getAction()) {
			case START_DESTROY_BLOCK -> Digging.Action.START_DIGGING;
			case ABORT_DESTROY_BLOCK -> Digging.Action.CANCELLED_DIGGING;
			case STOP_DESTROY_BLOCK -> Digging.Action.FINISHED_DIGGING;
			case RELEASE_USE_ITEM -> Digging.Action.RELEASE_USE_ITEM;
			case DROP_ALL_ITEMS -> Digging.Action.DROP_ITEM_STACK;
			case DROP_ITEM -> Digging.Action.DROP_ITEM;
			case SWAP_ITEM_WITH_OFFHAND -> Digging.Action.SWAP_ITEM_WITH_OFFHAND;
		};
		return new Digging(action, p.getPos(), p.getDirection(), p.getSequence());
	}

	public static Placement toPlacement(ServerboundUseItemOnPacket p) {
		BlockHitResult hit = p.getHitResult();
		return new Placement(hit.getBlockPos(), hit.getDirection(), p.getHand(),
				(float) hit.getLocation().x, (float) hit.getLocation().y, (float) hit.getLocation().z,
				hit.isInside(), p.getSequence());
	}

	public static HeldItemChange toHeldItemChange(ServerboundSetCarriedItemPacket p) {
		return new HeldItemChange(p.getSlot());
	}
}
