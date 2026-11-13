package dev.meshac.check;

import dev.meshac.MeshServices;
import dev.meshac.config.MeshSettings;
import dev.meshac.config.MeshVersion;
import dev.meshac.data.PlayerData;
import dev.meshac.player.ClickTimingData;
import dev.meshac.player.CollisionState;
import dev.meshac.player.MotionState;
import dev.meshac.player.PlayerToggles;
import dev.meshac.tick.TickProcessor;
import dev.meshac.tracker.ConnectionTracker;
import dev.meshac.tracker.RelativeMoveTracker;
import dev.meshac.tracker.VelocityTracker;
import dev.meshac.util.FastMath;
import dev.meshac.world.BlockUtils;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;

/**
 * Exemption predicates shared by every check. Ported from Vulcan's ExemptType 1:1;
 * only platform calls changed (Bukkit world/player -> 26.3 server classes).
 */
public enum ExemptType {

	TPS(ExemptType::matchesTps),
	CHUNK(ExemptType::matchesChunk),
	JOINED(ExemptType::matchesJoined),
	CAKE(ExemptType::matchesCake),
	BAMBOO(ExemptType::matchesBamboo),
	HAPPY_GHAST(ExemptType::matchesHappyGhast),
	PACKET_EXPLOSION(ExemptType::matchesPacketExplosion),
	ENTITY_CRAM_FIX(ExemptType::matchesEntityCramFix),
	AROUND_SLIME(ExemptType::matchesAroundSlime),
	SIGN(ExemptType::matchesSign),
	PRESSURE_PLATE(ExemptType::matchesPressurePlate),
	SWIMMING(ExemptType::matchesSwimming),
	SWIMMING_JESUS(ExemptType::matchesSwimmingJesus),
	COLLIDING_HORIZONTALLY(ExemptType::matchesCollidingHorizontally),
	DRIPSTONE(ExemptType::matchesDripstone),
	HIGH_FLY_SPEED(ExemptType::matchesHighFlySpeed),
	GLITCHED_BLOCKS_ABOVE(ExemptType::matchesGlitchedBlocksAbove),
	PROJECTILE_DAMAGE(ExemptType::matchesProjectileDamage),
	CHEST(ExemptType::matchesChest),
	ATTACK_DAMAGE(ExemptType::matchesAttackDamage),
	SKULL(ExemptType::matchesSkull),
	EXPLOSION(ExemptType::matchesExplosion),
	STAIRS(ExemptType::matchesStairs),
	HIGH_LEVITATION(ExemptType::matchesHighLevitation),
	FENCE_GATE(ExemptType::matchesFenceGate),
	POWDER_SNOW(ExemptType::matchesPowderSnow),
	DRAGON_DAMAGE(ExemptType::matchesDragonDamage),
	JUMP_BOOST_RAN_OUT(ExemptType::matchesJumpBoostRanOut),
	LAGGED_NEAR_GROUND(ExemptType::matchesLaggedNearGround),
	SNOW(ExemptType::matchesSnow),
	NETHERITE_ARMOR(ExemptType::matchesNetheriteArmor),
	FAST_ZERO(ExemptType::matchesFastZero),
	JOINED_CHUNK_LOAD(ExemptType::matchesJoinedChunkLoad),
	NEAR_SOLID(ExemptType::matchesNearSolid),
	CAMPFIRE(ExemptType::matchesCampfire),
	CANCELLED_MOVE(ExemptType::matchesCancelledMove),
	CARPET(ExemptType::matchesCarpet),
	FLOWER_POT(ExemptType::matchesFlowerPot),
	EMPTIED_BUCKET(ExemptType::matchesEmptiedBucket),
	END_ROD(ExemptType::matchesEndRod),
	HOPPER(ExemptType::matchesHopper),
	CHAIN(ExemptType::matchesChain),
	DOOR(ExemptType::matchesDoor),
	PICKED_UP_ITEM(ExemptType::matchesPickedUpItem),
	ANVIL(ExemptType::matchesAnvil),
	LAGGED_NEAR_GROUND_MODERN(ExemptType::matchesLaggedNearGroundModern),
	FULLY_SUBMERGED(ExemptType::matchesFullySubmerged),
	RESPAWN(ExemptType::matchesRespawn),
	ICE(ExemptType::matchesIce),
	BED(ExemptType::matchesBed),
	SLAB(ExemptType::matchesSlab),
	HIGH_JUMP_BOOST(ExemptType::matchesHighJumpBoost),
	LILY_PAD(ExemptType::matchesLilyPad),
	HIGH_SPEED(ExemptType::matchesHighSpeed),
	WALL(ExemptType::matchesWall),
	SWEET_BERRIES(ExemptType::matchesSweetBerries),
	CONDUIT(ExemptType::matchesConduit),
	PLACED_WEB(ExemptType::matchesPlacedWeb),
	PISTON(ExemptType::matchesPiston),
	CHORUS_FRUIT(ExemptType::matchesChorusFruit),
	ENDER_PEARL(ExemptType::matchesEnderPearl),
	NEAR_GROUND(ExemptType::matchesNearGround),
	SLEEPING(ExemptType::matchesSleeping),
	SOUL_SPEED(ExemptType::matchesSoulSpeed),
	TRAPDOOR(ExemptType::matchesTrapdoor),
	FISHING_ROD(ExemptType::matchesFishingRod),
	PLUGIN_LOAD(ExemptType::matchesPluginLoad),
	RIPTIDE(ExemptType::matchesRiptide),
	FIREWORK(ExemptType::matchesFirework),
	SPECTATOR(ExemptType::matchesSpectator),
	CAULDRON(ExemptType::matchesCauldron),
	ENTITY_COLLISION(ExemptType::matchesEntityCollision),
	DEATH(ExemptType::matchesDeath),
	DEPTH_STRIDER(ExemptType::matchesDepthStrider),
	LEVITATION(ExemptType::matchesLevitation),
	FROZEN(ExemptType::matchesFrozen),
	DIGGING(ExemptType::matchesDigging),
	BLOCK_BREAK(ExemptType::matchesBlockBreak),
	PLACING(ExemptType::matchesPlacing),
	SERVER_POSITION(ExemptType::matchesServerPosition),
	SERVER_POSITION_FAST(ExemptType::matchesServerPositionFast),
	CANCELLED_PLACE(ExemptType::matchesCancelledPlace),
	BLOCK_PLACE(ExemptType::matchesBlockPlace),
	BLOCK_PLACE_FAST(ExemptType::matchesBlockPlaceFast),
	SWIMMING_ON_OLD_VERSION(ExemptType::matchesSwimmingOnOldVersion),
	FULLY_STUCK(ExemptType::matchesFullyStuck),
	PLACED_CLIMBABLE(ExemptType::matchesPlacedClimbable),
	PARTIALLY_STUCK(ExemptType::matchesPartiallyStuck),
	NOT_MOVING(ExemptType::matchesNotMoving),
	PLACED_SLIME(ExemptType::matchesPlacedSlime),
	FIREBALL(ExemptType::matchesFireball),
	WORLD_CHANGE(ExemptType::matchesWorldChange),
	FALL_DAMAGE(ExemptType::matchesFallDamage),
	ILLEGAL_BLOCK(ExemptType::matchesIllegalBlock),
	HONEY(ExemptType::matchesHoney),
	SCAFFOLDING(ExemptType::matchesScaffolding),
	HALF_BLOCK(ExemptType::matchesHalfBlock),
	SHULKER(ExemptType::matchesShulker),
	GLIDING(ExemptType::matchesGliding),
	ELYTRA(ExemptType::matchesElytra),
	VELOCITY(ExemptType::matchesVelocity),
	DEAD(ExemptType::matchesDead),
	WEB(ExemptType::matchesWeb),
	SOUL_SAND(ExemptType::matchesSoulSand),
	CINEMATIC(ExemptType::matchesCinematic),
	FAST(ExemptType::matchesFast),
	LENIENT_SCAFFOLDING(ExemptType::matchesLenientScaffolding),
	WINDOW_CLICK(ExemptType::matchesWindowClick),
	DROPPED_ITEM(ExemptType::matchesDroppedItem),
	CANCELLED_BREAK(ExemptType::matchesCancelledBreak),
	MYTHIC_MOB(ExemptType::matchesMythicMob),
	CREATIVE(ExemptType::matchesCreative),
	AUTOCLICKER_NON_DIG(ExemptType::matchesAutoclickerNonDig),
	AUTOCLICKER(ExemptType::matchesAutoclicker),
	COLLIDING_VERTICALLY(ExemptType::matchesCollidingVertically),
	SLOW_FALLING(ExemptType::matchesSlowFalling),
	BUBBLE_COLUMN(ExemptType::matchesBubbleColumn),
	FENCE(ExemptType::matchesFence),
	JUMP_BOOST(ExemptType::matchesJumpBoost),
	FLIGHT(ExemptType::matchesFlight),
	COMBO_MODE(ExemptType::matchesComboMode),
	DOLPHINS_GRACE(ExemptType::matchesDolphinsGrace),
	TELEPORT(ExemptType::matchesTeleport),
	SERVER_VERSION(ExemptType::matchesServerVersion),
	KELP(ExemptType::matchesKelp),
	CLIENT_VERSION(ExemptType::matchesClientVersion),
	SLIME(ExemptType::matchesSlime),
	BOAT(ExemptType::matchesBoat),
	SEA_PICKLE(ExemptType::matchesSeaPickle),
	SEAGRASS(ExemptType::matchesSeagrass),
	TURTLE_EGG(ExemptType::matchesTurtleEgg),
	SHULKER_BOX(ExemptType::matchesShulkerBox),
	WATERLOGGED(ExemptType::matchesWaterlogged),
	CLIMBABLE(ExemptType::matchesClimbable),
	LIQUID(ExemptType::matchesLiquid),
	GLASS_PANE(ExemptType::matchesGlassPane),
	FARMLAND(ExemptType::matchesFarmland),
	VEHICLE(ExemptType::matchesVehicle),
	VOID(ExemptType::matchesVoid);

	private final Function<PlayerData, Boolean> function;

	ExemptType(Function<PlayerData, Boolean> function) {
		this.function = function;
	}

	public Function<PlayerData, Boolean> getFunction() {
		return this.function;
	}

	private static Boolean matchesVoid(PlayerData playerData) {
		boolean belowYLimit;
		if (ExemptionManager.isAtLeast1_18()) {
			belowYLimit = playerData.getCollisionState().getY() < -70.0;
		} else {
			belowYLimit = playerData.getCollisionState().getY() < 0.0;
		}
		return belowYLimit;
	}

	private static Boolean matchesVehicle(PlayerData playerData) {
		if (playerData.getCollisionState().getTicksSinceVehicle() < 25) return true;
		return playerData.getPlayer() != null && playerData.getPlayer().getVehicle() != null;
	}

	private static Boolean matchesFarmland(PlayerData playerData) {
		return playerData.getCollisionState().getTicksSinceFarmland() < 20;
	}

	private static Boolean matchesGlassPane(PlayerData playerData) {
		return playerData.getCollisionState().isGlassPane();
	}

	private static Boolean matchesLiquid(PlayerData playerData) {
		return playerData.getCollisionState().getTicksSinceLiquid() < 25;
	}

	private static Boolean matchesClimbable(PlayerData playerData) {
		return playerData.getCollisionState().isLadderOrVine() || playerData.getCollisionState().isClimbable();
	}

	private static Boolean matchesWaterlogged(PlayerData playerData) {
		return ExemptionManager.isNewerThan1_13() && playerData.getCollisionState().getTicksSinceWaterlogged() < 30;
	}

	private static Boolean matchesShulkerBox(PlayerData playerData) {
		return playerData.getCollisionState().isShulkerBox() || playerData.getCollisionState().getTicksSinceShulkerBox() < 60;
	}

	private static Boolean matchesTurtleEgg(PlayerData playerData) {
		return playerData.getCollisionState().isTurtleEgg();
	}

	private static Boolean matchesSeagrass(PlayerData playerData) {
		return playerData.getCollisionState().isSeagrass();
	}

	private static Boolean matchesSeaPickle(PlayerData playerData) {
		return playerData.getCollisionState().isSeaPickle();
	}

	private static Boolean matchesBoat(PlayerData playerData) {
		return playerData.getCollisionState().isBoat();
	}

	private static Boolean matchesSlime(PlayerData playerData) {
		return playerData.getCollisionState().isSlime() || playerData.getCollisionState().getTicksSinceSlime() < 65;
	}

	private static Boolean matchesClientVersion(PlayerData playerData) {
		return playerData.getClientVersion().isNewerThanOrEquals(MeshVersion.V_1_9);
	}

	private static Boolean matchesKelp(PlayerData playerData) {
		return playerData.getCollisionState().isKelp();
	}

	private static Boolean matchesServerVersion(PlayerData playerData) {
		return ExemptionManager.isAtLeast1_9();
	}

	private static Boolean matchesTeleport(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceTeleport() < 10 || playerData.getMotionState().isTeleporting();
	}

	private static Boolean matchesDolphinsGrace(PlayerData playerData) {
		return ExemptionManager.isAtLeast1_9()
				&& (playerData.getCollisionState().getTicksSinceDolphinsGrace() < 50 || CheckUtils.hasDolphinsGrace(playerData));
	}

	private static Boolean matchesComboMode(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceAttackDamage() < 25
				&& playerData.getPlayer() != null && playerData.getMaximumNoDamageTicks() < 15;
	}

	private static Boolean matchesFlight(PlayerData playerData) {
		if (playerData.getPlayer() != null && playerData.getPlayer().getAbilities().mayfly) return true;
		return playerData.getCollisionState().getTicksSinceFlightAllowed() < MeshSettings.settingsFlightCooldown;
	}

	private static Boolean matchesJumpBoost(PlayerData playerData) {
		return playerData.getCollisionState().getTicksSinceJumpBoost() < 40 || playerData.getMotionState().isJumpBoostEffect();
	}

	private static Boolean matchesFence(PlayerData playerData) {
		return playerData.getCollisionState().isFence() || playerData.getCollisionState().getTicksSinceFence() < 7;
	}

	private static Boolean matchesBubbleColumn(PlayerData playerData) {
		return ExemptionManager.isAtLeast1_9() && playerData.getCollisionState().getTicksSinceBubbleColumn() < 40;
	}

	private static Boolean matchesSlowFalling(PlayerData playerData) {
		return ExemptionManager.isAtLeast1_9() && playerData.getCollisionState().getTicksSinceSlowFalling() < 40;
	}

	private static Boolean matchesCollidingVertically(PlayerData playerData) {
		return playerData.getCollisionState().getTicksSinceVerticalCollision() < 20;
	}

	private static Boolean matchesAutoclicker(PlayerData playerData) {
		PlayerToggles toggles = playerData.getPlayerToggles();
		if (!toggles.isAnyExempt(PLACING, DIGGING, BLOCK_BREAK, FAST, DROPPED_ITEM, WINDOW_CLICK, CREATIVE, CANCELLED_BREAK)
				&& System.currentTimeMillis() - playerData.getMotionState().getDigStartedAtMillis() >= 110L
				&& playerData.getCollisionState().getTicksSinceFirework() >= 20
				&& System.currentTimeMillis() - playerData.getClickTimingData().getLastPlaceAt() >= 3000L
				&& System.currentTimeMillis() - playerData.getClickTimingData().getLastDigAt() >= 250L
				&& playerData.getMotionState().getTicksSinceBottleUse() >= 15
				&& System.currentTimeMillis() - playerData.getMotionState().getDigCancelledAtMillis() >= 110L
				&& System.currentTimeMillis() - playerData.getMotionState().getDigFinishedAtMillis() >= 110L
				&& MeshServices.ticks().getTotalTicks() >= 20
				&& MeshServices.ticks().getTotalTicks() - playerData.getMotionState().getLastProjectileLaunchTick() >= 20
				&& MeshServices.ticks().getTotalTicks() - playerData.getRelativeMoveTracker().getLastItemFrameInteractTick() >= 20) {
			return playerData.getMotionState().getDistanceFromDiggedBlock() < 6.5
					&& playerData.getMotionState().getDistanceFromDiggedBlock() > 0.0;
		}
		return true;
	}

	private static Boolean matchesAutoclickerNonDig(PlayerData playerData) {
		PlayerToggles toggles = playerData.getPlayerToggles();
		if (!toggles.isAnyExempt(PLACING, DIGGING, BLOCK_BREAK, FAST, DROPPED_ITEM, WINDOW_CLICK, CANCELLED_BREAK, CREATIVE)
				&& playerData.getMotionState().getTicksSinceBottleUse() >= 15
				&& MeshServices.ticks().getTotalTicks() - playerData.getRelativeMoveTracker().getLastItemFrameInteractTick() >= 20
				&& MeshServices.ticks().getTotalTicks() - playerData.getMotionState().getLastProjectileLaunchTick() >= 20) {
			return false;
		}
		return true;
	}

	private static Boolean matchesCreative(PlayerData playerData) {
		if (playerData.getPlayer() != null && playerData.getPlayer().isCreative()) return true;
		return playerData.getMotionState().getGameMode() == GameType.CREATIVE;
	}

	private static Boolean matchesMythicMob(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceMythicMob() < 50;
	}

	private static Boolean matchesCancelledBreak(PlayerData playerData) {
		return MeshServices.ticks().getTotalTicks() - playerData.getMotionState().getLastCancelledBreakTick() < 40;
	}

	private static Boolean matchesDroppedItem(PlayerData playerData) {
		return MeshServices.ticks().getTotalTicks() - playerData.getMotionState().getLastDroppedItemTick() < 15;
	}

	private static Boolean matchesWindowClick(PlayerData playerData) {
		return MeshServices.ticks().getTotalTicks() - playerData.getMotionState().getLastWindowClickTick() < 15;
	}

	private static Boolean matchesLenientScaffolding(PlayerData playerData) {
		return MeshSettings.settingsLenientScaffolding && ExemptionManager.isAtLeast1_9()
				&& playerData.getMotionState().getDistanceFromPlacedBlock() < 3.0
				&& playerData.getMotionState().getDistanceFromPlacedBlock() > 0.0;
	}

	private static Boolean matchesFast(PlayerData playerData) {
		return playerData.getConnectionTracker().isFastMovement();
	}

	private static Boolean matchesCinematic(PlayerData playerData) {
		return MeshSettings.settingsCinematic && playerData.getRotationTracker().isCinematic();
	}

	private static Boolean matchesSoulSand(PlayerData playerData) {
		return playerData.getCollisionState().isSoulSand();
	}

	private static Boolean matchesWeb(PlayerData playerData) {
		return playerData.getCollisionState().getTicksSinceWeb() < 25 && playerData.getCollisionState().getDeltaY() < 0.1;
	}

	private static Boolean matchesDead(PlayerData playerData) {
		return playerData.getPlayer() != null && !playerData.getPlayer().isAlive();
	}

	private static Boolean matchesVelocity(PlayerData playerData) {
		return playerData.getVelocityTracker().getTicksSinceVelocity() < 40;
	}

	private static Boolean matchesElytra(PlayerData playerData) {
		return playerData.getMotionState().isWearingElytra() || playerData.getCollisionState().getTicksSinceWearingElytra() < 100;
	}

	private static Boolean matchesGliding(PlayerData playerData) {
		return ExemptionManager.isAtLeast1_9() && playerData.getMotionState().getTicksSinceGliding() < 10;
	}

	private static Boolean matchesShulker(PlayerData playerData) {
		return playerData.getCollisionState().isShulker() || playerData.getCollisionState().getTicksSinceShulker() < 40;
	}

	private static Boolean matchesHalfBlock(PlayerData playerData) {
		return playerData.getCollisionState().isStairs() || playerData.getCollisionState().isSlab();
	}

	private static Boolean matchesScaffolding(PlayerData playerData) {
		return ExemptionManager.isAtLeast1_9()
				&& (playerData.getCollisionState().isScaffolding() || playerData.getCollisionState().getTicksSinceScaffolding() < 30);
	}

	private static Boolean matchesHoney(PlayerData playerData) {
		return ExemptionManager.isAtLeast1_9()
				&& (playerData.getCollisionState().isHoney() || playerData.getCollisionState().getTicksSinceHoney() < 30);
	}

	private static Boolean matchesIllegalBlock(PlayerData playerData) {
		CollisionState c = playerData.getCollisionState();
		return c.isDaylightDetector() || c.isBrewingStand() || c.isCarpet() || c.isWall() || c.isSkull()
				|| c.isEndPortalFrame() || c.isScaffolding() || c.isCampfire();
	}

	private static Boolean matchesFallDamage(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceFallDamage() < 40;
	}

	private static Boolean matchesWorldChange(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceWorldChange() < 40;
	}

	private static Boolean matchesFireball(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceFireball() < 25;
	}

	private static Boolean matchesPlacedSlime(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceSlimePlace() < 120;
	}

	private static Boolean matchesNotMoving(PlayerData playerData) {
		return !playerData.getCollisionState().isMoving();
	}

	private static Boolean matchesPartiallyStuck(PlayerData playerData) {
		return playerData.getCollisionState().isPartiallyStuck();
	}

	private static Boolean matchesPlacedClimbable(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceClimbablePlace() < 30;
	}

	private static Boolean matchesFullyStuck(PlayerData playerData) {
		return playerData.getCollisionState().isFullyStuck();
	}

	private static Boolean matchesSwimmingOnOldVersion(PlayerData playerData) {
		return playerData.getCollisionState().getTicksSinceLiquid() < 60
				&& playerData.getClientVersion().isNewerThanOrEquals(MeshVersion.V_1_13)
				&& ExemptionManager.isBefore1_13();
	}

	private static Boolean matchesBlockPlaceFast(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceBlockPlace() < 15;
	}

	private static Boolean matchesBlockPlace(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceBlockPlace() < 30;
	}

	private static Boolean matchesCancelledPlace(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceCancelledPlace() < 40;
	}

	private static Boolean matchesServerPositionFast(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceServerPosition() < 5;
	}

	private static Boolean matchesServerPosition(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceServerPosition() < 8;
	}

	private static Boolean matchesPlacing(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceBlockPlace() < 10;
	}

	private static Boolean matchesBlockBreak(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceBlockBreak() < 25;
	}

	private static Boolean matchesDigging(PlayerData playerData) {
		MotionState m = playerData.getMotionState();
		return m.isDigging()
				|| MeshServices.ticks().getTotalTicks() - m.getLastAbortDigTick() < 20
				|| MeshServices.ticks().getTotalTicks() - m.getLastDigTick() < 20;
	}

	private static Boolean matchesFrozen(PlayerData playerData) {
		return playerData.getCollisionState().isFrozen();
	}

	private static Boolean matchesLevitation(PlayerData playerData) {
		return ExemptionManager.isAtLeast1_9() && playerData.getCollisionState().getTicksSinceLevitation() < 75;
	}

	private static Boolean matchesDepthStrider(PlayerData playerData) {
		return playerData.getMotionState().isDepthStrider() && playerData.getCollisionState().getTicksSinceLiquid() < 30;
	}

	private static Boolean matchesDeath(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceDeath() < 25;
	}

	private static Boolean matchesEntityCollision(PlayerData playerData) {
		return MeshSettings.settingsEntityCollision && playerData.getCollisionState().getTicksSinceEntityCollision() < 8;
	}

	private static Boolean matchesCauldron(PlayerData playerData) {
		return playerData.getCollisionState().isCauldron();
	}

	private static Boolean matchesSpectator(PlayerData playerData) {
		return ExemptionManager.isAtLeast1_8() && playerData.getPlayer() != null && playerData.getPlayer().isSpectator();
	}

	private static Boolean matchesFirework(PlayerData playerData) {
		return ExemptionManager.isAtLeast1_9() && playerData.getCollisionState().getTicksSinceFirework() < 150;
	}

	private static Boolean matchesRiptide(PlayerData playerData) {
		return ExemptionManager.isNewerThan1_13() && playerData.getCollisionState().getTicksSinceRiptide() < 80;
	}

	private static Boolean matchesPluginLoad(PlayerData playerData) {
		return System.currentTimeMillis() - MeshServices.getStartedAtMillis() < 10000L;
	}

	private static Boolean matchesFishingRod(PlayerData playerData) {
		return ExemptionManager.isAtLeast1_9() && playerData.getCollisionState().getTicksSinceFishingRod() < 100;
	}

	private static Boolean matchesTrapdoor(PlayerData playerData) {
		return playerData.getCollisionState().getTicksSinceTrapdoor() < 20 || playerData.getCollisionState().isTrapdoor();
	}

	private static Boolean matchesSoulSpeed(PlayerData playerData) {
		return ExemptionManager.isAtLeast1_16() && playerData.getCollisionState().getTicksSinceSoulSpeed() < 60;
	}

	private static Boolean matchesSleeping(PlayerData playerData) {
		if (playerData.getPlayer() != null && playerData.getPlayer().isSleeping()) return true;
		TickProcessor ticks = MeshServices.ticks();
		return ticks.getTotalTicks() - playerData.getMotionState().getLastBedEnterTick() < 40
				|| ticks.getTotalTicks() - playerData.getMotionState().getLastBedLeaveTick() < 40;
	}

	private static Boolean matchesNearGround(PlayerData playerData) {
		return playerData.getCollisionState().isNearGround();
	}

	private static Boolean matchesEnderPearl(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceEnderPearl() < 5;
	}

	private static Boolean matchesChorusFruit(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceChorusFruit() < 25;
	}

	private static Boolean matchesPiston(PlayerData playerData) {
		return playerData.getCollisionState().isPiston() || playerData.getCollisionState().getTicksSincePiston() < 20;
	}

	private static Boolean matchesPlacedWeb(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceWebPlace() < 25;
	}

	private static Boolean matchesConduit(PlayerData playerData) {
		return playerData.getCollisionState().isConduit();
	}

	private static Boolean matchesSweetBerries(PlayerData playerData) {
		return playerData.getCollisionState().isSweetBerries();
	}

	private static Boolean matchesWall(PlayerData playerData) {
		return playerData.getCollisionState().isWall();
	}

	private static Boolean matchesHighSpeed(PlayerData playerData) {
		return playerData.getMotionState().getSpeedAmplifier() > 5;
	}

	private static Boolean matchesLilyPad(PlayerData playerData) {
		return playerData.getCollisionState().isLilyPad();
	}

	private static Boolean matchesHighJumpBoost(PlayerData playerData) {
		return playerData.getMotionState().getJumpBoostAmplifier() > 2;
	}

	private static Boolean matchesSlab(PlayerData playerData) {
		return playerData.getCollisionState().isSlab();
	}

	private static Boolean matchesBed(PlayerData playerData) {
		return playerData.getCollisionState().isBed() || playerData.getCollisionState().getTicksSinceBed() < 30;
	}

	private static Boolean matchesIce(PlayerData playerData) {
		return playerData.getCollisionState().getTicksSinceIce() < 20 || playerData.getCollisionState().isIce();
	}

	private static Boolean matchesRespawn(PlayerData playerData) {
		return MeshServices.ticks().getTotalTicks() - playerData.getMotionState().getLastRespawnTick() < 20;
	}

	private static Boolean matchesFullySubmerged(PlayerData playerData) {
		return playerData.getCollisionState().isFullySubmerged();
	}

	private static Boolean matchesLaggedNearGroundModern(PlayerData playerData) {
		if (ExemptionManager.isNewerThan1_13() && playerData.getConnectionTracker().getMovementDelayMillis() < 10L) {
			return playerData.getCollisionState().isNearGroundWide() || !playerData.getCollisionState().hasNoGroundBeneath();
		}
		return false;
	}

	private static Boolean matchesAnvil(PlayerData playerData) {
		return playerData.getCollisionState().isAnvil();
	}

	private static Boolean matchesPickedUpItem(PlayerData playerData) {
		return MeshServices.ticks().getTotalTicks() - playerData.getMotionState().getLastItemPickupTick() < 30;
	}

	private static Boolean matchesDoor(PlayerData playerData) {
		return playerData.getCollisionState().isDoor();
	}

	private static Boolean matchesChain(PlayerData playerData) {
		return playerData.getCollisionState().isChain();
	}

	private static Boolean matchesHopper(PlayerData playerData) {
		return playerData.getCollisionState().isHopper();
	}

	private static Boolean matchesEndRod(PlayerData playerData) {
		return playerData.getCollisionState().isEndRod();
	}

	private static Boolean matchesEmptiedBucket(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceBucketEmpty() < 15;
	}

	private static Boolean matchesFlowerPot(PlayerData playerData) {
		return playerData.getCollisionState().isFlowerPot();
	}

	private static Boolean matchesCarpet(PlayerData playerData) {
		return playerData.getCollisionState().isCarpet();
	}

	private static Boolean matchesCancelledMove(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceCancelledMove() < 5;
	}

	private static Boolean matchesCampfire(PlayerData playerData) {
		return playerData.getCollisionState().isCampfire();
	}

	private static Boolean matchesNearSolid(PlayerData playerData) {
		return playerData.getCollisionState().isNearSolid();
	}

	private static Boolean matchesJoinedChunkLoad(PlayerData playerData) {
		if (System.currentTimeMillis() - playerData.getJoinTime() < 30000L) {
			return playerData.getCollisionState().getDeltaY() < -0.05F && playerData.getCollisionState().getDeltaY() > -0.1F;
		}
		return false;
	}

	private static Boolean matchesFastZero(PlayerData playerData) {
		return playerData.getCollisionState().getDeltaY() == 0.0 && playerData.getConnectionTracker().isFastMovement();
	}

	private static Boolean matchesNetheriteArmor(PlayerData playerData) {
		return CheckUtils.isWearingNetheriteArmor(playerData);
	}

	private static Boolean matchesSnow(PlayerData playerData) {
		return playerData.getCollisionState().isSnow();
	}

	private static Boolean matchesLaggedNearGround(PlayerData playerData) {
		if (!playerData.getCollisionState().isNearGround() && playerData.getCollisionState().hasNoGroundBeneath()) return false;
		return playerData.getConnectionTracker().isFastMovement();
	}

	private static Boolean matchesJumpBoostRanOut(PlayerData playerData) {
		return playerData.getCollisionState().getTicksSinceJumpBoost() > 0 && playerData.getCollisionState().getTicksSinceJumpBoost() < 30;
	}

	private static Boolean matchesDragonDamage(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceDragonDamage() < 60;
	}

	private static Boolean matchesPowderSnow(PlayerData playerData) {
		return playerData.getCollisionState().isPowderSnow();
	}

	private static Boolean matchesFenceGate(PlayerData playerData) {
		return playerData.getCollisionState().isFenceGate();
	}

	private static Boolean matchesHighLevitation(PlayerData playerData) {
		return ExemptionManager.isAtLeast1_9() && playerData.getMotionState().getLevitationAmplifier() > 10;
	}

	private static Boolean matchesStairs(PlayerData playerData) {
		return playerData.getCollisionState().isStairs();
	}

	private static Boolean matchesExplosion(PlayerData playerData) {
		return playerData.getVelocityTracker().getTicksSinceExplosion() < 50;
	}

	private static Boolean matchesSkull(PlayerData playerData) {
		return playerData.getCollisionState().isSkull();
	}

	private static Boolean matchesAttackDamage(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceAttackDamage() < 80;
	}

	private static Boolean matchesChest(PlayerData playerData) {
		return playerData.getCollisionState().isChest();
	}

	private static Boolean matchesProjectileDamage(PlayerData playerData) {
		return playerData.getMotionState().getTicksSinceProjectileDamage() < 20;
	}

	private static Boolean matchesGlitchedBlocksAbove(PlayerData playerData) {
		return playerData.getCollisionState().getHeadMaterials() != null
				&& !playerData.getCollisionState().getHeadMaterials().stream().allMatch(BlockUtils::isAir);
	}

	private static Boolean matchesHighFlySpeed(PlayerData playerData) {
		return playerData.getCollisionState().getTicksSinceHighFlySpeed() < 100;
	}

	private static Boolean matchesDripstone(PlayerData playerData) {
		return playerData.getCollisionState().isDripstone();
	}

	private static Boolean matchesCollidingHorizontally(PlayerData playerData) {
		return playerData.getCollisionState().isCollidingHorizontally();
	}

	private static Boolean matchesSwimmingJesus(PlayerData playerData) {
		return ExemptionManager.isAtLeast1_9() && playerData.getCollisionState().getTicksSinceSwimming() < 5;
	}

	private static Boolean matchesSwimming(PlayerData playerData) {
		return ExemptionManager.isAtLeast1_9() && playerData.getCollisionState().getTicksSinceSwimming() < 40;
	}

	private static Boolean matchesPressurePlate(PlayerData playerData) {
		return playerData.getCollisionState().isPressurePlate();
	}

	private static Boolean matchesSign(PlayerData playerData) {
		return playerData.getCollisionState().isSign();
	}

	private static Boolean matchesAroundSlime(PlayerData playerData) {
		return playerData.getCollisionState().getTicksSinceNearbySlime() < 20;
	}

	private static Boolean matchesEntityCramFix(PlayerData playerData) {
		return MeshSettings.settingsEntityCramFixEnabled
				&& playerData.getCollisionState().getTicksSinceEntityCramming() < MeshSettings.settingsEntityCramExemptTicks;
	}

	private static Boolean matchesPacketExplosion(PlayerData playerData) {
		return playerData.getVelocityTracker().getTicksSinceExplosion() < 16;
	}

	private static Boolean matchesHappyGhast(PlayerData playerData) {
		return playerData.getCollisionState().isHappyGhast() || playerData.getCollisionState().getTicksSinceHappyGhast() < 25;
	}

	private static Boolean matchesBamboo(PlayerData playerData) {
		return playerData.getCollisionState().isBamboo();
	}

	private static Boolean matchesCake(PlayerData playerData) {
		return playerData.getCollisionState().isCake();
	}

	private static Boolean matchesJoined(PlayerData playerData) {
		return System.currentTimeMillis() - playerData.getJoinTime() < MeshSettings.settingsJoinCheckWaitTime
				|| playerData.getMotionState().getTicksSinceJoin() < MeshSettings.settingsMinTicksExisted;
	}

	private static Boolean matchesChunk(PlayerData playerData) {
		if (playerData.getPlayer() != null) {
			ServerLevel level = (ServerLevel) playerData.getPlayer().level();
			int x = FastMath.floor(playerData.getCollisionState().getX()) >> 4;
			int z = FastMath.floor(playerData.getCollisionState().getZ()) >> 4;
			return !level.hasChunk(x, z);
		}
		return false;
	}

	private static Boolean matchesTps(PlayerData playerData) {
		return ExemptionManager.getTps() < 19.25;
	}
}
