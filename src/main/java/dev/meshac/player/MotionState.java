package dev.meshac.player;

import dev.meshac.MeshServices;
import dev.meshac.check.CheckUtils;
import dev.meshac.check.ExemptionManager;
import dev.meshac.collection.EvictingList;
import dev.meshac.config.MeshVersion;
import dev.meshac.data.PlayerData;
import dev.meshac.data.RotationSample;
import dev.meshac.packet.Digging;
import dev.meshac.packet.EntityAction;
import dev.meshac.packet.Flying;
import dev.meshac.packet.InteractEntity;
import dev.meshac.util.AttributeUtils;
import dev.meshac.util.MathUtils;
import dev.meshac.util.Vec3d;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;

/**
 * Per-player motion/effect state. Ported from Vulcan's MotionState; packet wrappers became the
 * interceptor's direct calls, Bukkit inventory/enchant access became Mojmap equivalents.
 */
public class MotionState {

	private final PlayerData playerData;
	private boolean sprinting;
	private boolean entityActionReceived;
	private boolean itemUseReceived;
	private boolean digging;
	private boolean releaseUseItemReceived;
	private boolean diggingPacketReceived;
	private boolean teleporting;
	private boolean speedEffect;
	private boolean packetJumpBoostEffect;
	private boolean packetLevitationEffect;
	private boolean slowFallingEffect;
	private boolean slownessEffect;
	private boolean depthStrider;
	private boolean wearingElytra;
	private boolean gliding;
	private boolean pendingSwimFix;
	private boolean swiftSneak;
	private boolean lastPlacementValid;
	private boolean jumpBoostEffect;
	private boolean raining;
	private boolean aquaAffinity;
	private boolean packetGliding;
	private boolean packetSwimming;
	private boolean packetRiptide;
	private double movementSpeed = 0.1;
	private double stepHeight = 0.6;
	private double jumpStrength = 0.42;
	private double gravity = 0.08;
	private double previousJumpStrength = 0.42;
	private double previousGravity = 0.08;
	private double entityInteractionRange = 3.0;
	private double bounciness = 0.0;
	private double airDragModifier = 1.0;
	private double frictionModifier = 1.0;
	public final EvictingList<Vec3d> pendingServerPositions = new EvictingList<>(15);
	private int lastDigTick;
	private int lastDroppedItemTick;
	private int lastAbortDigTick;
	private int ticksNotSprinting;
	private int ticksSinceJoin = 0;
	private int lastWindowClickTick;
	private int heldItemAmount;
	private int previousHeldItemAmount;
	private int heldItemSlot;
	private int previousHeldItemSlot;
	private int ticksSinceServerPosition = 100;
	private int lastBedEnterTick;
	private int lastBedLeaveTick;
	private int lastItemPickupTick;
	private int lastCancelledBreakTick;
	private int lastDamageTick;
	private int ticksSinceDragonDamage = 100;
	private int sprintingTicks;
	private int lastRespawnTick;
	private int lastProjectileLaunchTick;
	private int ticksSinceAttackDamage = 100;
	private int ticksSinceFallDamage = 100;
	private int ticksSinceProjectileDamage;
	private int ticksSinceBucketEmpty = 100;
	private int ticksSinceBlockPlace = 100;
	private int ticksSinceClimbablePlace = 100;
	private int ticksSinceSlimePlace = 150;
	private int ticksSinceCancelledPlace = 100;
	private int ticksSinceWorldChange = 100;
	private int ticksSinceTeleport = 100;
	private int ticksSinceDeath = 100;
	private int ticksSinceKnockbackDamage = 100;
	private int lastPlacedBlockX;
	private int lastPlacedBlockY;
	private int lastPlacedBlockZ;
	private int ticksSinceBottleUse = 100;
	private int ticksSinceFireball = 100;
	private int speedAmplifier = 0;
	private int jumpBoostAmplifier = 0;
	private int levitationAmplifier = 0;
	private int ticksSinceChorusFruit = 100;
	private int ticksSinceWebPlace = 100;
	private int ticksSinceEnderPearl = 100;
	private int ticksSinceDamage = 100;
	private int ticksSinceIcePlace = 100;
	private int ticksSinceBlockBreak = 100;
	private int ticksExisted = 0;
	private int strafeGraceTicks = 100;
	private int ticksSinceMythicMob = 500;
	private int ticksSinceCancelledMove = 1000;
	private int ticksSinceRavagerDamage = 1000;
	private int ticksSinceCrystalDamage = 100;
	private int ticksSinceBucketUse = 100;
	private int lastSpeedAmplifier = 0;
	private int ticksSinceValidPlacement = 100;
	private int slownessAmplifier = 0;
	private int ticksSinceJumpStrengthChange;
	private int ticksSinceGravityChange;
	private int ticksSinceGliding = 100;
	private int ticksSinceSoulPlace = 100;
	private double lastDiggedBlockX;
	private double lastDiggedBlockZ;
	private double distanceFromDiggedBlock;
	private double lastBrokenBlockX;
	private double lastBrokenBlockY;
	private double lastBrokenBlockZ;
	private double distanceFromBrokenBlock;
	private double distanceFromPlacedBlock;
	private double lastGlidingHorizontalSpeed;
	private long digStartedAtMillis;
	private long digFinishedAtMillis;
	private long digCancelledAtMillis;
	private GameType gameMode = GameType.SURVIVAL;
	private ItemStack helmet;
	private ItemStack chestplate;
	private ItemStack leggings;
	private ItemStack boots;
	private ItemStack offHandItem;

	public MotionState(PlayerData playerData) {
		this.playerData = playerData;
	}

	public void handleEntityAction(EntityAction packet) {
		this.entityActionReceived = true;
		switch (packet.getAction()) {
			case START_SPRINTING -> this.sprinting = true;
			case STOP_SPRINTING -> this.sprinting = false;
			case START_SNEAKING, STOP_SNEAKING -> { }
			case START_FLYING_WITH_ELYTRA -> {
				if (this.playerData.getPlayer() != null && !ExemptionManager.isBefore1_9()
						&& !this.playerData.getClientVersion().isOlderThan(MeshVersion.V_1_15)) {
					CollisionState collisionState = this.playerData.getCollisionState();
					if (collisionState.isOnGround()) return;
					if (collisionState.isPreviousOnGround()) return;
					ItemStack chestplate = this.playerData.getPlayer().getItemBySlot(EquipmentSlot.CHEST);
					if (chestplate != null && chestplate.is(Items.ELYTRA)) this.packetGliding = true;
				}
			}
		}
	}

	/** Outbound attributes packet for this player (post-confirmation). */
	public void handleAttributes(ClientboundUpdateAttributesPacket packet) {
		this.playerData.getConfirmationTracker().runAfterConfirmation(() -> applyAttributes(packet));
	}

	public void handleEntityInteraction(InteractEntity packet) {
		if (packet.getAction() == InteractEntity.Action.ATTACK) {
			this.digging = false;
			this.distanceFromDiggedBlock = 1000.0;
		}
	}

	public void setLastItemPickupTick() { this.lastItemPickupTick = MeshServices.ticks(); }
	public void recordDig() { this.lastDigTick = MeshServices.ticks(); }

	public void handleDigging(Digging packet) {
		this.diggingPacketReceived = true;
		switch (packet.getAction()) {
			case START_DIGGING -> {
				this.digging = true;
				this.digStartedAtMillis = System.currentTimeMillis();
			}
			case FINISHED_DIGGING -> {
				this.digFinishedAtMillis = System.currentTimeMillis();
				this.lastDiggedBlockX = packet.getBlockPosition().getX();
				this.lastDiggedBlockZ = packet.getBlockPosition().getZ();
				this.digging = false;
			}
			case CANCELLED_DIGGING -> {
				this.digCancelledAtMillis = System.currentTimeMillis();
				this.lastDiggedBlockX = packet.getBlockPosition().getX();
				this.lastDiggedBlockZ = packet.getBlockPosition().getZ();
				this.digging = false;
			}
			case RELEASE_USE_ITEM -> this.releaseUseItemReceived = true;
			case DROP_ITEM, DROP_ITEM_STACK -> this.lastDroppedItemTick = MeshServices.ticks();
		}
	}

	public void recordEntityDamage(net.minecraft.world.entity.EntityType<?> entityType) {
		if (entityType == net.minecraft.world.entity.EntityTypes.ENDER_DRAGON) {
			this.ticksSinceDragonDamage = 0;
		} else if (entityType == net.minecraft.world.entity.EntityTypes.FIREBALL
				|| entityType == net.minecraft.world.entity.EntityTypes.SMALL_FIREBALL
				|| entityType == net.minecraft.world.entity.EntityTypes.DRAGON_FIREBALL) {
			this.ticksSinceFireball = 0;
		} else if (entityType == net.minecraft.world.entity.EntityTypes.END_CRYSTAL) {
			this.ticksSinceCrystalDamage = 0;
		}
		if (ExemptionManager.isAtLeast1_14() && entityType == net.minecraft.world.entity.EntityTypes.RAVAGER) {
			this.ticksSinceRavagerDamage = 0;
		}
	}

	public void setLastCancelledBreakTick() { this.lastCancelledBreakTick = MeshServices.ticks(); }

	public void setLastRespawnTick() { this.lastRespawnTick = MeshServices.ticks(); }

	public void recordItemUse() {
		this.itemUseReceived = true;
		if (!ExemptionManager.isAtLeast1_9() || this.playerData.getPlayer() == null) return;
		ServerPlayer player = this.playerData.getPlayer();
		if (!player.getMainHandItem().is(Items.FIREWORK_ROCKET) && !player.getOffhandItem().is(Items.FIREWORK_ROCKET)) return;
		this.playerData.getCollisionState().setTicksSinceFirework(0);
	}

	public void recordWindowClick() { this.lastWindowClickTick = MeshServices.ticks(); }

	public void updateDiggingDistance() {
		if (this.digging) this.lastAbortDigTick = MeshServices.ticks();
		if (this.lastDiggedBlockX != 0.0 && this.lastDiggedBlockZ != 0.0) {
			double x = this.playerData.getCollisionState().getX();
			double z = this.playerData.getCollisionState().getZ();
			double xDifference = Math.abs(x - this.lastDiggedBlockX);
			this.distanceFromDiggedBlock = MathUtils.hypot(xDifference, Math.abs(z - this.lastDiggedBlockZ));
			if (this.distanceFromDiggedBlock > 12.0) {
				this.lastDiggedBlockX = this.lastDiggedBlockZ = this.distanceFromDiggedBlock = 0.0;
			}
		}
	}

	public void recordBlockPlace() { this.ticksSinceBlockPlace = 0; }

	public void updateHeldItem() {
		ServerPlayer player = this.playerData.getPlayer();
		this.heldItemAmount = player == null ? 0 : player.getMainHandItem().getCount();
		this.heldItemSlot = player == null ? 0 : player.getInventory().getSelectedSlot();
		if (this.heldItemAmount == this.previousHeldItemAmount && this.heldItemSlot == this.previousHeldItemSlot) {
			this.ticksSinceCancelledPlace = 0;
		}
		this.previousHeldItemSlot = this.heldItemSlot;
		this.previousHeldItemAmount = this.heldItemAmount;
	}

	public void recordBlockBreak(double lastBrokenBlockX, double lastBrokenBlockY, double lastBrokenBlockZ) {
		if (this.digging) this.digging = false;
		this.ticksSinceBlockBreak = 0;
		this.lastBrokenBlockX = lastBrokenBlockX;
		this.lastBrokenBlockY = lastBrokenBlockY;
		this.lastBrokenBlockZ = lastBrokenBlockZ;
	}

	public void recordTeleport() { this.ticksSinceTeleport = 0; }

	/** Outbound position packet for this player (post-confirmation). */
	public void handleServerPosition(double x, double y, double z) {
		this.playerData.getConfirmationTracker().runAfterConfirmation(() -> this.pendingServerPositions.add(new Vec3d(x, y, z)));
	}

	/** Outbound entity-effect packet for this player (post-confirmation). */
	public void handlePotionEffect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier) {
		this.playerData.getConfirmationTracker().runAfterConfirmation(() -> applyPotionEffect(effect, amplifier));
	}

	/** Outbound remove-effect packet for this player (post-confirmation). */
	public void handleRemovedPotionEffect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
		this.playerData.getConfirmationTracker().runAfterConfirmation(() -> removePotionEffect(effect));
	}

	/** Outbound game-event packet (post-confirmation): id 3 = change game mode, 1/2 = rain start/stop. */
	public void handleGameState(byte eventId, float value) {
		this.playerData.getConfirmationTracker().runAfterConfirmation(() -> applyGameMode(eventId, value));
	}

	public void setTicksSinceWorldChange() { this.ticksSinceWorldChange = 0; }
	public void setTicksSinceDeath() { this.ticksSinceDeath = 0; }

	public void recordDamageCause(DamageCause damageCause) {
		if (damageCause == null) return;
		this.lastDamageTick = MeshServices.ticks();
		this.ticksSinceDamage = 0;
		if (damageCause != DamageCause.FALL && damageCause != DamageCause.SUFFOCATION) {
			this.ticksSinceKnockbackDamage = 0;
		}
		switch (damageCause) {
			case ENTITY_ATTACK -> this.ticksSinceAttackDamage = 0;
			case FALL -> this.ticksSinceFallDamage = 0;
			case PROJECTILE -> this.ticksSinceProjectileDamage = 0;
			default -> { }
		}
	}

	public void recordEnderPearl() { this.ticksSinceEnderPearl = 0; }
	public void setLastBedEnterTick() { this.lastBedEnterTick = MeshServices.ticks(); }
	public void setLastBedLeaveTick() { this.lastBedLeaveTick = MeshServices.ticks(); }

	public void processMovement(Flying packet) {
		this.teleporting = false;
		if (packet.hasPositionChanged() && packet.hasRotationChanged() && !packet.isOnGround()) {
			for (Vec3d vector : this.pendingServerPositions) {
				double xDifference = vector.getX() - packet.getX();
				double yDifference = vector.getY() - packet.getY();
				double zDifference = vector.getZ() - packet.getZ();
				if (Math.abs(xDifference) < 1.0E-8 && Math.abs(yDifference) < 1.0E-8 && Math.abs(zDifference) < 1.0E-8) {
					this.teleporting = true;
				}
			}
		}
		if (this.teleporting) {
			RotationSample latest = this.playerData.getCollisionState().getRotationSample(0);
			RotationSample previous = this.playerData.getCollisionState().getRotationSample(1);
			latest.setDeltaX(0.0);
			latest.setDeltaY(0.0);
			latest.setDeltaZ(0.0);
			latest.setDeltaXZ(0.0);
			latest.setTeleportInvalidated(true);
			latest.setOnGround(previous.isOnGround());
		}
		if (this.teleporting) this.ticksSinceServerPosition = 0;
		else this.ticksSinceServerPosition++;
		if (this.ticksSinceServerPosition > 100) this.pendingServerPositions.clear();
		this.releaseUseItemReceived = false;
		this.diggingPacketReceived = false;
		this.entityActionReceived = false;
		this.itemUseReceived = false;
		this.ticksSinceSoulPlace++;
		this.ticksSinceGravityChange++;
		this.ticksSinceJumpStrengthChange++;
		this.ticksExisted++;
		this.ticksSinceFallDamage++;
		this.ticksSinceAttackDamage++;
		this.ticksSinceKnockbackDamage++;
		this.ticksSinceProjectileDamage++;
		this.ticksSinceDragonDamage++;
		this.ticksSinceBucketUse++;
		this.ticksSinceMythicMob++;
		this.ticksSinceBucketEmpty++;
		this.ticksSinceBlockPlace++;
		this.ticksSinceBottleUse++;
		this.ticksSinceClimbablePlace++;
		this.ticksSinceSlimePlace++;
		this.ticksSinceCrystalDamage++;
		this.ticksSinceWorldChange++;
		this.ticksSinceTeleport++;
		this.ticksSinceDeath++;
		this.ticksSinceFireball++;
		this.ticksSinceWebPlace++;
		this.ticksSinceChorusFruit++;
		this.strafeGraceTicks++;
		this.ticksSinceEnderPearl++;
		this.ticksSinceCancelledPlace++;
		this.ticksSinceDamage++;
		this.ticksSinceIcePlace++;
		this.ticksSinceBlockBreak++;
		this.ticksSinceRavagerDamage++;
		if (this.lastPlacementValid) this.ticksSinceValidPlacement = 0;
		else this.ticksSinceValidPlacement++;
		if (packet.hasPositionChanged()) {
			this.ticksSinceJoin++;
			if (this.playerData.getPlayer() != null) {
				ServerPlayer player = this.playerData.getPlayer();
				this.helmet = player.getItemBySlot(EquipmentSlot.HEAD);
				this.chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
				this.leggings = player.getItemBySlot(EquipmentSlot.LEGS);
				this.boots = player.getItemBySlot(EquipmentSlot.FEET);
				if (ExemptionManager.isAtLeast1_9()) this.offHandItem = player.getOffhandItem();
				this.depthStrider = ExemptionManager.isNewerThan1_7_10() && this.boots != null
						&& dev.meshac.util.EnchantUtil.has(this.boots, Enchantments.DEPTH_STRIDER);
				this.aquaAffinity = this.helmet != null && dev.meshac.util.EnchantUtil.has(this.helmet, Enchantments.AQUA_AFFINITY);
				this.swiftSneak = ExemptionManager.isAtLeast1_19() && this.leggings != null
						&& dev.meshac.util.EnchantUtil.has(this.leggings, Enchantments.SWIFT_SNEAK);
				this.wearingElytra = ExemptionManager.isAtLeast1_9() && this.chestplate != null && this.chestplate.is(Items.ELYTRA);
				this.gliding = this.wearingElytra && CheckUtils.isGliding(this.playerData);
			}
			if (this.packetGliding) this.ticksSinceGliding = 0;
			else this.ticksSinceGliding++;
			if (this.gliding) this.lastGlidingHorizontalSpeed = this.playerData.getCollisionState().getDeltaXZ();
			double x = packet.getX();
			double y = packet.getY();
			double z = packet.getZ();
			if (this.sprinting) this.sprintingTicks++;
			else this.sprintingTicks = 0;
			if (!this.sprinting) this.ticksNotSprinting++;
			else this.ticksNotSprinting = 0;
			if (this.lastBrokenBlockX != 0.0 && this.lastBrokenBlockZ != 0.0) {
				double xDifference = Math.abs(x - this.lastBrokenBlockX);
				double yDifference = Math.abs(y - this.lastBrokenBlockY);
				this.distanceFromBrokenBlock = MathUtils.length3(xDifference, yDifference, Math.abs(z - this.lastBrokenBlockZ));
				if (this.distanceFromBrokenBlock > 8.0) {
					this.lastBrokenBlockX = this.lastBrokenBlockZ = this.distanceFromBrokenBlock = 0.0;
				}
			}
			if (this.lastPlacedBlockX != 0 && this.lastPlacedBlockY != 0 && this.lastPlacedBlockZ != 0) {
				double xDifference = Math.abs(x - this.lastPlacedBlockX);
				double yDifference = Math.abs(y - this.lastPlacedBlockY);
				this.distanceFromPlacedBlock = MathUtils.length3(xDifference, yDifference, Math.abs(z - this.lastPlacedBlockZ));
				if (this.distanceFromPlacedBlock > 12.0) {
					this.lastPlacedBlockX = this.lastPlacedBlockY = this.lastPlacedBlockZ = 0;
				}
			}
		}
	}

	public void setTicksSinceSlimePlace() { this.ticksSinceSlimePlace = 0; }
	public void recordCancelledPlace() { this.ticksSinceCancelledPlace = 0; }
	public void recordIcePlace() { this.ticksSinceIcePlace = 0; }
	public void recordClimbablePlace() { this.ticksSinceClimbablePlace = 0; }
	public void recordWebPlace() { this.ticksSinceWebPlace = 0; }
	public void setLastProjectileLaunchTick() { this.lastProjectileLaunchTick = MeshServices.ticks(); }

	public void recordPlacedBlock(BlockPos placed) {
		this.lastPlacedBlockX = placed.getX();
		this.lastPlacedBlockY = placed.getY();
		this.lastPlacedBlockZ = placed.getZ();
	}

	public void recordChorusFruit() { this.ticksSinceChorusFruit = 0; }

	/** Outbound entity-metadata packet for this player (post-confirmation). */
	public void handlePlayerMetadata(ClientboundSetEntityDataPacket packet) {
		this.playerData.getConfirmationTracker().runAfterConfirmation(() -> applyPlayerMetadata(packet));
	}

	public static SynchedEntityData.DataValue<?> findMetadata(List<SynchedEntityData.DataValue<?>> list, int index) {
		for (SynchedEntityData.DataValue<?> data : list) {
			if (data.id() == index) return data;
		}
		return null;
	}

	public double applyAirDrag(double amount) {
		return !ExemptionManager.isAtLeast26_1() ? amount : Math.max(0.0, Math.min(1.0, 1.0 - (1.0 - amount) * this.airDragModifier));
	}

	public double maximumHorizontalMotion(double amount, boolean condition) {
		double drag = this.applyAirDrag(0.91);
		if (drag <= 0.91) return 0.0;
		double scaled = amount * drag * (condition ? 0.6 : 1.0);
		double base = condition ? this.movementSpeed * 1.3 : 0.026;
		return scaled + base + 0.02;
	}

	public double applyFriction(double amount) {
		return !ExemptionManager.isAtLeast26_1() ? amount : Math.max(0.0, Math.min(1.0, 1.0 - (1.0 - amount) * this.frictionModifier));
	}

	public boolean isSprinting() { return this.sprinting; }
	public boolean isEntityActionReceived() { return this.entityActionReceived; }
	public boolean isItemUseReceived() { return this.itemUseReceived; }
	public boolean isDigging() { return this.digging; }
	public boolean isReleaseUseItemReceived() { return this.releaseUseItemReceived; }
	public boolean isDiggingPacketReceived() { return this.diggingPacketReceived; }
	public boolean isTeleporting() { return this.teleporting; }
	public boolean isSpeedEffect() { return this.speedEffect; }
	public boolean isPacketJumpBoostEffect() { return this.packetJumpBoostEffect; }
	public boolean isPacketLevitationEffect() { return this.packetLevitationEffect; }
	public boolean isSlowFallingEffect() { return this.slowFallingEffect; }
	public boolean isSlownessEffect() { return this.slownessEffect; }
	public boolean isDepthStrider() { return this.depthStrider; }
	public boolean isWearingElytra() { return this.wearingElytra; }
	public boolean isGliding() { return this.gliding; }
	public boolean isPendingSwimFix() { return this.pendingSwimFix; }
	public boolean isSwiftSneak() { return this.swiftSneak; }
	public boolean isLastPlacementValid() { return this.lastPlacementValid; }
	public boolean isJumpBoostEffect() { return this.jumpBoostEffect; }
	public boolean isRaining() { return this.raining; }
	public boolean isPacketGliding() { return this.packetGliding; }
	public boolean isPacketSwimming() { return this.packetSwimming; }
	public boolean isPacketRiptide() { return this.packetRiptide; }
	public double getMovementSpeed() { return this.movementSpeed; }
	public double getStepHeight() { return this.stepHeight; }
	public double getJumpStrength() { return this.jumpStrength; }
	public double getGravity() { return this.gravity; }
	public double getPreviousJumpStrength() { return this.previousJumpStrength; }
	public double getPreviousGravity() { return this.previousGravity; }
	public double getEntityInteractionRange() { return this.entityInteractionRange; }
	public double getBounciness() { return this.bounciness; }
	public double getAirDragModifier() { return this.airDragModifier; }
	public double getFrictionModifier() { return this.frictionModifier; }
	public int getLastDigTick() { return this.lastDigTick; }
	public int getLastDroppedItemTick() { return this.lastDroppedItemTick; }
	public int getLastAbortDigTick() { return this.lastAbortDigTick; }
	public int getTicksNotSprinting() { return this.ticksNotSprinting; }
	public int getTicksSinceJoin() { return this.ticksSinceJoin; }
	public int getLastWindowClickTick() { return this.lastWindowClickTick; }
	public int getTicksSinceServerPosition() { return this.ticksSinceServerPosition; }
	public int getLastBedEnterTick() { return this.lastBedEnterTick; }
	public int getLastBedLeaveTick() { return this.lastBedLeaveTick; }
	public int getLastItemPickupTick() { return this.lastItemPickupTick; }
	public int getLastCancelledBreakTick() { return this.lastCancelledBreakTick; }
	public int getTicksSinceDragonDamage() { return this.ticksSinceDragonDamage; }
	public int getSprintingTicks() { return this.sprintingTicks; }
	public int getLastRespawnTick() { return this.lastRespawnTick; }
	public int getLastProjectileLaunchTick() { return this.lastProjectileLaunchTick; }
	public int getTicksSinceAttackDamage() { return this.ticksSinceAttackDamage; }
	public int getTicksSinceFallDamage() { return this.ticksSinceFallDamage; }
	public int getTicksSinceProjectileDamage() { return this.ticksSinceProjectileDamage; }
	public int getTicksSinceBucketEmpty() { return this.ticksSinceBucketEmpty; }
	public int getTicksSinceBlockPlace() { return this.ticksSinceBlockPlace; }
	public int getTicksSinceClimbablePlace() { return this.ticksSinceClimbablePlace; }
	public int getTicksSinceSlimePlace() { return this.ticksSinceSlimePlace; }
	public int getTicksSinceCancelledPlace() { return this.ticksSinceCancelledPlace; }
	public int getTicksSinceWorldChange() { return this.ticksSinceWorldChange; }
	public int getTicksSinceTeleport() { return this.ticksSinceTeleport; }
	public int getTicksSinceDeath() { return this.ticksSinceDeath; }
	public int getTicksSinceKnockbackDamage() { return this.ticksSinceKnockbackDamage; }
	public int getTicksSinceBottleUse() { return this.ticksSinceBottleUse; }
	public int getTicksSinceFireball() { return this.ticksSinceFireball; }
	public int getSpeedAmplifier() { return this.speedAmplifier; }
	public int getJumpBoostAmplifier() { return this.jumpBoostAmplifier; }
	public int getLevitationAmplifier() { return this.levitationAmplifier; }
	public int getTicksSinceChorusFruit() { return this.ticksSinceChorusFruit; }
	public int getTicksSinceWebPlace() { return this.ticksSinceWebPlace; }
	public int getTicksSinceEnderPearl() { return this.ticksSinceEnderPearl; }
	public int getTicksSinceDamage() { return this.ticksSinceDamage; }
	public int getTicksSinceIcePlace() { return this.ticksSinceIcePlace; }
	public int getTicksSinceBlockBreak() { return this.ticksSinceBlockBreak; }
	public int getTicksExisted() { return this.ticksExisted; }
	public int getStrafeGraceTicks() { return this.strafeGraceTicks; }
	public void setStrafeGraceTicks(int strafeGraceTicks) { this.strafeGraceTicks = strafeGraceTicks; }
	public int getTicksSinceMythicMob() { return this.ticksSinceMythicMob; }
	public int getTicksSinceCancelledMove() { return this.ticksSinceCancelledMove; }
	public int getTicksSinceRavagerDamage() { return this.ticksSinceRavagerDamage; }
	public int getTicksSinceCrystalDamage() { return this.ticksSinceCrystalDamage; }
	public int getTicksSinceBucketUse() { return this.ticksSinceBucketUse; }
	public int getLastSpeedAmplifier() { return this.lastSpeedAmplifier; }
	public int getTicksSinceValidPlacement() { return this.ticksSinceValidPlacement; }
	public int getSlownessAmplifier() { return this.slownessAmplifier; }
	public int getTicksSinceJumpStrengthChange() { return this.ticksSinceJumpStrengthChange; }
	public int getTicksSinceGravityChange() { return this.ticksSinceGravityChange; }
	public int getTicksSinceGliding() { return this.ticksSinceGliding; }
	public int getTicksSinceSoulPlace() { return this.ticksSinceSoulPlace; }
	public double getDistanceFromDiggedBlock() { return this.distanceFromDiggedBlock; }
	public double getDistanceFromBrokenBlock() { return this.distanceFromBrokenBlock; }
	public double getDistanceFromPlacedBlock() { return this.distanceFromPlacedBlock; }
	public long getDigStartedAtMillis() { return this.digStartedAtMillis; }
	public long getDigFinishedAtMillis() { return this.digFinishedAtMillis; }
	public long getDigCancelledAtMillis() { return this.digCancelledAtMillis; }
	public GameType getGameMode() { return this.gameMode; }
	public ItemStack getBoots() { return this.boots; }
	public void setPendingSwimFix(boolean pendingSwimFix) { this.pendingSwimFix = pendingSwimFix; }
	public void setLastPlacementValid(boolean lastPlacementValid) { this.lastPlacementValid = lastPlacementValid; }
	public void setTicksSinceDragonDamage(int ticksSinceDragonDamage) { this.ticksSinceDragonDamage = ticksSinceDragonDamage; }
	public void setTicksSinceBucketEmpty(int ticksSinceBucketEmpty) { this.ticksSinceBucketEmpty = ticksSinceBucketEmpty; }
	public void setTicksSinceBottleUse(int ticksSinceBottleUse) { this.ticksSinceBottleUse = ticksSinceBottleUse; }
	public void setTicksSinceCancelledMove(int ticksSinceCancelledMove) { this.ticksSinceCancelledMove = ticksSinceCancelledMove; }
	public void setTicksSinceBucketUse(int ticksSinceBucketUse) { this.ticksSinceBucketUse = ticksSinceBucketUse; }
	public void setTicksSinceSoulPlace(int ticksSinceSoulPlace) { this.ticksSinceSoulPlace = ticksSinceSoulPlace; }
	public void resetDigStartTime() { this.digStartedAtMillis = 0L; }
	public void resetDigFinishTime() { this.digFinishedAtMillis = 0L; }
	public void resetDigCancelTime() { this.digCancelledAtMillis = 0L; }

	private void applyPlayerMetadata(ClientboundSetEntityDataPacket packet) {
		SynchedEntityData.DataValue<?> flags = findMetadata(packet.packedItems(), 0);
		if (flags != null && flags.value() instanceof Byte value) {
			this.packetGliding = (value & 128) != 0 && playerData.getClientVersion().isNewerThanOrEquals(MeshVersion.V_1_9);
			this.packetSwimming = (value & 16) != 0;
		}
		if (ExemptionManager.isNewerThan1_13() && playerData.getClientVersion().isNewerThanOrEquals(MeshVersion.V_1_9)) {
			SynchedEntityData.DataValue<?> handState = findMetadata(packet.packedItems(), ExemptionManager.isAtLeast1_17() ? 8 : 7);
			if (handState != null && handState.value() instanceof Byte value) this.packetRiptide = (value & 4) != 0;
		}
	}

	private void applyGameMode(byte eventId, float value) {
		if (eventId == 3) {
			switch ((int) value) {
				case 0 -> gameMode = GameType.SURVIVAL;
				case 1 -> gameMode = GameType.CREATIVE;
				case 2 -> gameMode = GameType.ADVENTURE;
				case 3 -> { if (ExemptionManager.isNewerThan1_7_10()) gameMode = GameType.SPECTATOR; }
			}
		} else if (eventId == 1) raining = true;
		else if (eventId == 2) raining = false;
	}

	private void removePotionEffect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
		if (effect == MobEffects.SPEED) {
			speedAmplifier = 0;
			speedEffect = false;
			playerData.getCollisionState().setTicksSinceSpeedEffect(0);
		} else if (effect == MobEffects.SLOWNESS) {
			slownessEffect = false;
			slownessAmplifier = 0;
		} else if (effect == MobEffects.JUMP_BOOST) {
			jumpBoostAmplifier = 0;
			packetJumpBoostEffect = false;
		} else if (effect == MobEffects.LEVITATION) {
			levitationAmplifier = 0;
			packetLevitationEffect = false;
		} else if (effect == MobEffects.SLOW_FALLING) {
			slowFallingEffect = false;
		}
	}

	private void applyPotionEffect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier) {
		amplifier = amplifier + 1;
		if (effect == MobEffects.SPEED) {
			speedEffect = true;
			speedAmplifier = amplifier;
			lastSpeedAmplifier = amplifier;
		} else if (effect == MobEffects.SLOWNESS) {
			slownessEffect = true;
			slownessAmplifier = amplifier;
		} else if (effect == MobEffects.JUMP_BOOST) {
			packetJumpBoostEffect = true;
			jumpBoostAmplifier = amplifier;
		} else if (effect == MobEffects.LEVITATION) {
			packetLevitationEffect = true;
			levitationAmplifier = amplifier;
		} else if (effect == MobEffects.SLOW_FALLING) {
			slowFallingEffect = true;
		}
	}

	private void applyAttributes(ClientboundUpdateAttributesPacket packet) {
		for (ClientboundUpdateAttributesPacket.AttributeSnapshot snapshot : packet.getValues()) {
			var attribute = snapshot.attribute();
			if (attribute == net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT) {
				stepHeight = snapshot.base();
			} else if (attribute == net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH) {
				previousJumpStrength = jumpStrength;
				jumpStrength = snapshot.base();
				if (jumpStrength != previousJumpStrength) ticksSinceJumpStrengthChange = 0;
			} else if (attribute == net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY) {
				previousGravity = gravity;
				gravity = snapshot.base();
				if (gravity != previousGravity) ticksSinceGravityChange = 0;
			} else if (attribute == net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) {
				movementSpeed = AttributeUtils.getMovementSpeedWithoutSprint(snapshot);
			} else if (attribute == net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE) {
				entityInteractionRange = snapshot.base();
			} else if (attribute == net.minecraft.world.entity.ai.attributes.Attributes.BOUNCINESS) {
				bounciness = snapshot.base();
			} else if (attribute == net.minecraft.world.entity.ai.attributes.Attributes.AIR_DRAG_MODIFIER) {
				airDragModifier = snapshot.base();
			} else if (attribute == net.minecraft.world.entity.ai.attributes.Attributes.FRICTION_MODIFIER) {
				frictionModifier = snapshot.base();
			}
		}
	}
}
