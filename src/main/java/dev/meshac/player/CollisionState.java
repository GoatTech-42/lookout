package dev.meshac.player;

import dev.meshac.MeshServices;
import dev.meshac.check.CheckUtils;
import dev.meshac.check.ConditionUtils;
import dev.meshac.check.ExemptionManager;
import dev.meshac.config.MeshSettings;
import dev.meshac.data.PlayerData;
import dev.meshac.data.RotationSample;
import dev.meshac.geometry.box.AxisAlignedBox;
import dev.meshac.packet.Digging;
import dev.meshac.packet.Flying;
import dev.meshac.tracker.EntityTracker;
import dev.meshac.tracker.TrackedEntity;
import dev.meshac.util.Location;
import dev.meshac.util.MathUtils;
import dev.meshac.util.TextUtil;
import dev.meshac.util.Vec3d;
import dev.meshac.world.BlockUtils;
import dev.meshac.world.EntityTypeMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;



public class CollisionState {

    private final PlayerData playerData;

    private boolean fence;

    private boolean liquid;

    private boolean slime;

    private boolean soulSand;

    private boolean climbable;

    private boolean web;

    private boolean lilyPad;

    private boolean daylightDetector;

    private boolean brewingStand;

    private boolean wall;

    private boolean stairs;

    private boolean slab;

    private boolean honey;

    private boolean scaffolding;

    private boolean trapdoor;

    private boolean skull;

    private boolean endPortalFrame;

    private boolean campfire;

    private boolean sweetBerries;

    private boolean shulkerBox;

    private boolean surroundingBubbleColumn;

    private boolean snow;

    private boolean anvil;

    private boolean endRod;

    private boolean chain;

    private boolean piston;

    private boolean door;

    private boolean inOpenAir;

    private boolean cauldron;

    private boolean hopper;

    private boolean fenceGate;

    private boolean flowerPot;

    private boolean ladderOrVine;

    private boolean touchingWeb;

    private boolean touchingBubbleColumn;

    private boolean fullySubmerged;

    private boolean touchingLiquid;

    private boolean collidingVertically;

    private boolean wallClimbEligible;

    private boolean kelp;

    private boolean fullyStuck;

    private boolean partiallyStuck;

    private boolean touchingIce;

    private boolean onGround;

    private boolean groundAligned;

    private boolean shulker;

    private boolean boat;

    private boolean bell;

    private boolean bed;

    private boolean carpet;

    private boolean lectern;

    private boolean turtleEgg;

    private boolean seaPickle;

    private boolean ice;

    private boolean conduit;

    private boolean collidingHorizontally;

    private boolean nearRepeater;

    private boolean nearSolid;

    private boolean glassPane;

    private boolean surroundingIce;

    private boolean surroundingLiquid;

    private boolean surroundingSlime;

    private boolean surroundedByAir;

    private boolean moving;

    private boolean frostedIce;

    private boolean bubbleColumn;

    private boolean farmland;

    private boolean previousOnGround;

    private boolean flightAllowed;

    private boolean seagrass;

    private boolean chest;

    private boolean surroundingPiston;

    private boolean cake;

    private boolean surroundingBed;

    private boolean amethyst;

    private boolean dripstone;

    private boolean powderSnow;

    private boolean touchedEntity;

    private boolean olderOnGround;

    private boolean nearLivingEntity;

    private boolean nearDirtPath;

    private boolean lantern;

    private boolean nearWorldBorder;

    private boolean rail;

    private boolean nearCandle;

    private boolean sign;

    private boolean bamboo;

    private boolean pressurePlate;

    private boolean setbackPending;

    private boolean enchantingTable;

    private boolean nearSculk;

    private boolean nearSculkSensor;

    private boolean ironBars;

    private boolean inLiquid;

    private boolean solidFooting;

    private boolean slabAbove;

    private boolean nearDripleaf;

    private boolean nearCactus;

    private boolean nearHeavyCore;

    private boolean positionChanged;

    private boolean previousPositionChanged;

    private boolean olderPositionChanged;

    private boolean happyGhast;

    private boolean nearHappyGhast;

    private boolean nearDriedGhast;

    private boolean nearBlueIce;

    private Location position;

    private Location lastRecordedPosition;

    public boolean frozen;

    public List<Entity> nearbyEntities;

    public List<TrackedEntity> closeEntities = new ArrayList<>();

    public List<TrackedEntity> wideEntities = new ArrayList<>();

    private BlockState groundMaterialLegacy;

    private BlockState subGroundMaterialLegacy;

    private BlockState deepGroundMaterialLegacy;

    private List<BlockState> surroundingMaterialsLegacy;

    private List<BlockState> floorMaterials;

    private List<BlockState> ceilingMaterials;

    private List<BlockState> bodyMaterials;

    private List<BlockState> headMaterials;

    public List<BlockState> surroundingMaterials;

    public List<BlockState> areaScanMaterials;

    public List<BlockState> deepFloorMaterials;

    public BlockState groundMaterial;

    public BlockState subGroundMaterial;

    public BlockState deepGroundMaterial;

    private double x;

    private double y;

    private double z;

    private double previousX;

    private double previousY;

    private double previousZ;

    private double deltaX;

    private double deltaY;

    private double deltaZ;

    private double deltaXZ;

    private double deltaXYZ;

    private double previousDeltaX;

    private double previousDeltaZ;

    private double previousDeltaY;

    private double previousDeltaXZ;

    private double olderDeltaY;

    private double vehicleX;

    private double vehicleY;

    private double vehicleZ;

    private double previousVehicleX;

    private double previousVehicleY;

    private double previousVehicleZ;

    private double previousVehicleDeltaX;

    private double previousVehicleDeltaY;

    private double previousVehicleDeltaZ;

    private double vehicleDeltaX;

    private double vehicleDeltaY;

    private double vehicleDeltaZ;

    private double vehicleHorizontalSpeed;

    private double setbackX;

    private double setbackY;

    private double setbackZ;

    private double ghostFixBuffer;

    private double olderX;

    private double olderY;

    private double olderZ;

    private double joinX;

    private double joinY;

    private double joinZ;

    private double unloadedChunkBuffer;

    private double lastGlidingDeltaXZ;

    private int ticksSinceVehicle = 100;

    private int ticksSinceFlightAllowed = 100;

    private int ticksSinceTrapdoor = 100;

    private int airTicks;

    private int groundTicks;

    private int ticksSinceSwimming = 100;

    private int ticksSinceRiptide = 200;

    private int ticksWithoutGliding = 100;

    private int ticksSinceIceContact;

    private int ticksSinceDolphinsGrace = 100;

    private int ticksSinceLevitation = 100;

    private int ticksSinceBubbleColumn = 100;

    private int glidingTicks;

    private int ticksSinceSlowFalling = 100;

    private int ghostActivityTicks;

    private int groundAlignedTicks;

    private int ticksSinceWeb = 100;

    private int ticksSinceLiquid = 100;

    private int ticksOnLadder;

    private int ticksSinceJumpBoost = 100;

    private int ticksSinceSlime = 100;

    private int ticksSinceHoney = 100;

    private int ticksSinceVerticalCollision = 100;

    private int ticksSinceFence = 100;

    private int ticksSinceSoulSpeed = 100;

    private int ticksSinceBed = 100;

    private int ticksInVehicle;

    private int ticksSinceFirework = 200;

    private int ticksSinceIce = 100;

    private int ticksSincePiston = 100;

    private int lastWaterloggedTick;

    private int ticksSinceSpeedEffect = 100;

    private int ticksSinceNearbySlime = 500;

    private int ticksSinceNearbyPiston = 500;

    private int ticksSinceSurroundingIce = 500;

    private int ticksSinceSurroundingLiquid = 150;

    private int ticksSinceSurroundingSlime = 150;

    private int ticksSinceSurroundingBubbleColumn = 150;

    private int blockX;

    private int blockY;

    private int blockZ;

    private int ticksSinceScaffolding = 100;

    private int lastAbilitiesTick;

    private int ticksInOpenAir;

    private int ticksSinceShulkerBox = 100;

    private int ticksSinceWearingElytra = 100;

    private int lastGhostSetbackTick;

    private int ticksSinceFarmland = 100;

    private int ticksSinceStairs;

    private int ticksSinceSlab;

    private int ticksSinceEntityCollision = 100;

    private int ticksSinceSurroundingPiston = 100;

    private int ticksSinceFishingRod = 1000;

    private int ticksSinceHighFlySpeed = 200;

    private int ticksSinceNearbySlab = 100;

    private int ticksSinceSurroundingBed = 1000;

    private int ticksSinceWaterlogged = 1000;

    private int ticksSinceClimbable = 1000;

    private int nearbyBoatCount = 0;

    private int ticksSinceHorizontalCollision = 100;

    private int ticksSinceEntityCramming = 100;

    private int ticksSinceSolidFooting = 100;

    private int ticksSinceBoat = 100;

    private int ticksSinceShulker = 100;

    private int ticksSinceRiptideTrident = 1000;

    private int ticksSinceHappyGhast = 100;

    private int ghostBlockSetbacks = 0;

    private int unloadedChunkTicks = 500;

    private ServerLevel world;

    private Entity vehicle;

    private Entity previousVehicle;

    private float walkSpeed = 0.2F;

    private float flySpeed = 0.2F;

    private List<java.lang.Runnable> acknowledgedTasks = new ArrayList();

    private LinkedList<RotationSample> positionHistory = new LinkedList<>();

    private boolean exemptFlight;

    private boolean exemptCreative;

    private boolean exemptJoined;

    private boolean exemptLiquid;

    private boolean exemptLevitation;

    private boolean exemptSlowFalling;

    private boolean exemptRiptide;

    private boolean exemptVehicle;

    private boolean exemptLenientScaffolding;

    private boolean exemptGliding;

    private boolean exemptElytra;

    private boolean exemptTeleport;

    private boolean exemptEnderPearl;

    private boolean exemptChunk;

    private boolean exemptComboMode;

    private boolean exemptMythicMob;

    private boolean exemptClimbable;

    public CollisionState(PlayerData playerData2) {
        this.playerData = playerData2;
    }

    public void processMovement(Flying wrapperPlayClientPlayerFlying) {
        this.olderOnGround = this.previousOnGround;
        this.previousOnGround = this.onGround;
        this.onGround = wrapperPlayClientPlayerFlying.isOnGround();
        this.olderPositionChanged = this.previousPositionChanged;
        this.previousPositionChanged = this.positionChanged;
        this.positionChanged = wrapperPlayClientPlayerFlying.hasPositionChanged();
        PlayerData playerData2 = this.playerData;
        MotionState motionState = playerData2.getMotionState();
        if (motionState.getTicksExisted() % 100 == 0) {
            this.ghostBlockSetbacks = 0;
        }
        if (wrapperPlayClientPlayerFlying.hasPositionChanged()) {
            if (this.positionHistory.size() == 5) {
                this.positionHistory.pollLast();
            }
            double x2 = wrapperPlayClientPlayerFlying.getX();
            double y2 = wrapperPlayClientPlayerFlying.getY();
            double z2 = wrapperPlayClientPlayerFlying.getZ();
            boolean onGround2 = wrapperPlayClientPlayerFlying.isOnGround();
            RotationSample current = new RotationSample(x2, y2, z2);
            RotationSample previous = this.getRotationSample(0);
            current.setOnGround(onGround2);
            current.setDeltaX(current.getX() - previous.getX());
            current.setDeltaY(current.getY() - previous.getY());
            current.setDeltaZ(current.getZ() - previous.getZ());
            double previousDeltaX = current.getDeltaX();
            double previousDeltaZ = current.getDeltaZ();
            current.setDeltaXZ(MathUtils.hypot(previousDeltaX, previousDeltaZ));
            this.positionHistory.addFirst(current);
        }
        if (wrapperPlayClientPlayerFlying.hasPositionChanged()) {
            this.setbackPending = this.blockX == 0 && this.blockY == 0 && this.blockZ == 0;
            PlayerData playerData3 = this.playerData;
            ServerLevel world2;
            if (playerData3.getPlayer() == null) {
                world2 = null;
            } else {
                playerData3 = this.playerData;
                world2 = (ServerLevel) playerData3.getPlayer().level();
            }
            this.world = world2;
            this.olderX = this.previousX;
            this.olderY = this.previousY;
            this.olderZ = this.previousZ;
            this.previousX = this.x;
            this.previousY = this.y;
            this.previousZ = this.z;
            this.x = wrapperPlayClientPlayerFlying.getX();
            this.y = wrapperPlayClientPlayerFlying.getY();
            this.z = wrapperPlayClientPlayerFlying.getZ();
            this.position = this.lastRecordedPosition == null ? new Location(this.world, this.x, this.y, this.z) : this.lastRecordedPosition;
            this.lastRecordedPosition = new Location(this.world, this.x, this.y, this.z);
            playerData3 = this.playerData;
            EntityTracker entityTracker = playerData3.getEntityTracker();
            double x3 = this.x;
            this.closeEntities = entityTracker.getNearbyEntities(x3, this.y, this.z, 2.0);
            playerData3 = this.playerData;
            EntityTracker entityTracker2 = playerData3.getEntityTracker();
            this.wideEntities = entityTracker2.getNearbyEntities(4.5);
            this.olderDeltaY = this.previousDeltaY;
            this.previousDeltaX = this.deltaX;
            this.previousDeltaY = this.deltaY;
            this.previousDeltaZ = this.deltaZ;
            this.previousDeltaXZ = this.deltaXZ;
            this.deltaX = this.x - this.previousX;
            this.deltaY = this.y - this.previousY;
            this.deltaZ = this.z - this.previousZ;
            double deltaX2 = this.deltaX;
            this.deltaXZ = MathUtils.hypot(deltaX2, this.deltaZ);
            double deltaX3 = this.deltaX;
            x3 = this.deltaY;
            this.deltaXYZ = MathUtils.length3(deltaX3, x3, this.deltaZ);
            this.moving = Math.abs(this.deltaX) > 0.0 || Math.abs(this.deltaZ) > 0.0 || Math.abs(this.deltaY) > 0.0;
            this.groundAligned = this.y % 0.015625 == 0.0;
            playerData2 = this.playerData;
            if (playerData2.getPlayer() != null) {
                playerData2 = this.playerData;
                if (playerData2.getPlayer().getVehicle() == null) {
                    this.ticksInVehicle = 0;
                }
            }
            if (ExemptionManager.isNewerThan1_13()) {
                this.scanNearbyBlocks();
            } else {
                this.scanNearbyBlocksLegacy();
            }
            this.applyGhostBlockFix();
            this.teleportOutOfUnloadedChunks();
            this.updateTickCounters();
            playerData2 = this.playerData;
            MotionState motionState2 = playerData2.getMotionState();
            if (motionState2.getMovementSpeed() > 0.11) {
            }
            this.cacheExemptions();
            if ((MeshSettings.settingsVelocityWorldBorder) && (ExemptionManager.isAtLeast1_8())) {
                WorldBorder worldBorder = this.world.getWorldBorder();
                double x4 = worldBorder.getCenterX();
                double z3 = worldBorder.getCenterZ();
                double scaledSize = worldBorder.getSize() / 2.0;
                double xDifferenceDifference = this.x - x4 - scaledSize;
                double adjustedXDifference = this.x - x4 + scaledSize;
                double zDifferenceDifference = this.z - z3 - scaledSize;
                double adjustedZDifference = this.z - z3 + scaledSize;
                if (xDifferenceDifference < 0.0) {
                    xDifferenceDifference *= -1.0;
                }
                if (adjustedXDifference < 0.0) {
                    adjustedXDifference *= -1.0;
                }
                if (zDifferenceDifference < 0.0) {
                    zDifferenceDifference *= -1.0;
                }
                if (adjustedZDifference < 0.0) {
                    adjustedZDifference *= -1.0;
                }
                double[] amountArray = new double[] { xDifferenceDifference, adjustedXDifference, zDifferenceDifference, adjustedZDifference };
                Arrays.sort(amountArray);
                this.nearWorldBorder = amountArray[0] < 1.0;
            }
        }
        this.updateGroundState();
    }

    public RotationSample getRotationSample(int index) {
        RotationSample rotationSample = new RotationSample(0.0, 0.0, 0.0);
        int position = 0;
        for (RotationSample candidate : this.positionHistory) {
            rotationSample = candidate;
            if (position == index)
                break;
            ++position;
        }
        return rotationSample;
    }

    private void cacheExemptions() {
        PlayerToggles playerToggles = this.playerData.getPlayerToggles();
        this.exemptFlight = playerToggles.isExempt(ExemptType.FLIGHT);
        this.exemptCreative = playerToggles.isExempt(ExemptType.CREATIVE);
        this.exemptJoined = playerToggles.isExempt(ExemptType.JOINED);
        this.exemptLiquid = playerToggles.isExempt(ExemptType.LIQUID);
        this.exemptLevitation = playerToggles.isExempt(ExemptType.LEVITATION);
        this.exemptSlowFalling = playerToggles.isExempt(ExemptType.SLOW_FALLING);
        this.exemptRiptide = playerToggles.isExempt(ExemptType.RIPTIDE);
        this.exemptVehicle = playerToggles.isExempt(ExemptType.VEHICLE);
        this.exemptLenientScaffolding = playerToggles.isExempt(ExemptType.LENIENT_SCAFFOLDING);
        this.exemptGliding = playerToggles.isExempt(ExemptType.GLIDING);
        this.exemptElytra = playerToggles.isExempt(ExemptType.ELYTRA);
        this.exemptTeleport = playerToggles.isExempt(ExemptType.TELEPORT);
        this.exemptEnderPearl = playerToggles.isExempt(ExemptType.ENDER_PEARL);
        this.exemptChunk = playerToggles.isExempt(ExemptType.CHUNK);
        this.exemptComboMode = playerToggles.isExempt(ExemptType.COMBO_MODE);
        this.exemptMythicMob = playerToggles.isExempt(ExemptType.MYTHIC_MOB);
        this.exemptClimbable = playerToggles.isExempt(ExemptType.CLIMBABLE);
    }

    private void scanNearbyBlocks() {
        if (this.surroundingMaterials != null && this.groundMaterial != null && this.subGroundBlockState != null && this.deepGroundBlockState != null) {
            boolean inOpenAir2;
            this.blockX = (int) Math.floor(this.x);
            this.blockY = (int) Math.floor(this.y);
            this.blockZ = (int) Math.floor(this.z);
            this.floorMaterials = CheckUtils.getFloorMaterials(this.playerData);
            PlayerData playerData2 = this.playerData;
            this.ceilingMaterials = CheckUtils.getCeilingMaterials(playerData2);
            this.bodyMaterials = CheckUtils.getBodyMaterials(this.playerData);
            this.headMaterials = CheckUtils.getHeadMaterials(this.playerData);
            playerData2 = this.playerData;
            this.fullyStuck = CheckUtils.isEnclosedInSolidBlocks(playerData2);
            playerData2 = this.playerData;
            this.partiallyStuck = CheckUtils.isEnclosedInSolidBlocks(playerData2);
            this.collidingVertically = !ConditionUtils.allMatch((Collection) this.ceilingMaterials, (Predicate<BlockState>) BlockUtils::isAir);
            this.collidingHorizontally = !ConditionUtils.allMatch((Collection) this.bodyMaterials, (Predicate<BlockState>) BlockUtils::isAir);
            this.nearSolid = ConditionUtils.anyMatch((Collection) this.surroundingMaterials, (Predicate<BlockState>) BlockUtils::isSolid);
            inOpenAir2 = ((ConditionUtils.allMatch((Collection) this.bodyMaterials, (Predicate<BlockState>) BlockUtils::isAir)) && (ConditionUtils.allMatch((Collection) this.floorMaterials, (Predicate<BlockState>) BlockUtils::isAir)));
            this.inOpenAir = inOpenAir2;
            this.scanModernContacts();
        }
    }

    private void scanModernContacts() {
        if (this.ceilingMaterials != null) {
            this.slabAbove = false;
            for (BlockState material2 : this.ceilingMaterials) {
                this.slabAbove |= BlockUtils.isSlab(material2);
            }
        }
        this.touchingIce = false;
        if (this.floorMaterials != null) {
            for (BlockState material3 : this.floorMaterials) {
                this.touchingIce = this.touchingIce | BlockUtils.isIce(material3);
            }
        }
        if (this.surroundingMaterials != null && (false || this.nearbyEntities != null)) {
            this.slime = false;
            this.fence = false;
            this.liquid = false;
            this.carpet = false;
            this.soulSand = false;
            this.climbable = false;
            this.web = false;
            this.flowerPot = false;
            this.trapdoor = false;
            this.endPortalFrame = false;
            this.nearDirtPath = false;
            this.nearCandle = false;
            this.daylightDetector = false;
            this.brewingStand = false;
            this.wall = false;
            this.cauldron = false;
            this.stairs = false;
            this.ice = false;
            this.fenceGate = false;
            this.snow = false;
            this.anvil = false;
            this.nearRepeater = false;
            this.rail = false;
            this.nearDripleaf = false;
            this.slab = false;
            this.hopper = false;
            this.door = false;
            this.lilyPad = false;
            this.piston = false;
            this.glassPane = false;
            this.skull = false;
            this.bed = false;
            this.campfire = false;
            this.sweetBerries = false;
            this.conduit = false;
            this.lantern = false;
            this.ironBars = false;
            this.bamboo = false;
            this.lectern = false;
            this.bell = false;
            this.seaPickle = false;
            this.shulkerBox = false;
            this.kelp = false;
            this.turtleEgg = false;
            this.endRod = false;
            this.chain = false;
            this.scaffolding = false;
            this.honey = false;
            this.nearSculk = false;
            this.nearSculkSensor = false;
            this.amethyst = false;
            this.enchantingTable = false;
            this.pressurePlate = false;
            this.sign = false;
            this.frostedIce = false;
            this.bubbleColumn = false;
            this.farmland = false;
            this.seagrass = false;
            this.chest = false;
            this.cake = false;
            this.dripstone = false;
            this.powderSnow = false;
            this.nearCactus = false;
            this.nearHeavyCore = false;
            this.nearDriedGhast = false;
            this.nearBlueIce = false;
            for (BlockState material4 : this.surroundingMaterials) {
                this.slime |= material4.is(Blocks.SLIME_BLOCK);
                this.liquid |= (material4.is(Blocks.WATER) || material4.is(Blocks.LAVA));
                this.web |= material4.is(Blocks.COBWEB);
                this.flowerPot |= material4.is(Blocks.FLOWER_POT);
                this.endPortalFrame |= material4.is(Blocks.END_PORTAL_FRAME);
                this.daylightDetector |= material4.is(Blocks.DAYLIGHT_DETECTOR);
                this.brewingStand |= material4.is(Blocks.BREWING_STAND);
                this.ice |= (material4.is(Blocks.ICE) || material4.is(Blocks.PACKED_ICE) || material4.is(Blocks.FROSTED_ICE) || material4.is(Blocks.BLUE_ICE));
                this.snow |= material4.is(Blocks.SNOW);
                this.anvil |= (material4.is(Blocks.ANVIL) || material4.is(Blocks.CHIPPED_ANVIL) || material4.is(Blocks.DAMAGED_ANVIL));
                this.nearRepeater |= material4.is(Blocks.REPEATER);
                this.hopper |= material4.is(Blocks.HOPPER);
                this.lilyPad |= material4.is(Blocks.LILY_PAD);
                this.piston |= (material4.is(Blocks.PISTON) || material4.is(Blocks.PISTON_HEAD) || material4.is(Blocks.MOVING_PISTON) || material4.is(Blocks.STICKY_PISTON));
                this.conduit |= material4.is(Blocks.CONDUIT);
                this.seaPickle |= material4.is(Blocks.SEA_PICKLE);
                this.kelp |= (material4.is(Blocks.KELP) || material4.is(Blocks.KELP_PLANT));
                this.turtleEgg |= material4.is(Blocks.TURTLE_EGG);
                this.endRod |= material4.is(Blocks.END_ROD);
                boolean scaffolding2 = this.scaffolding;
                this.scaffolding = scaffolding2 | BlockUtils.isScaffolding(material4);
                scaffolding2 = this.honey;
                this.honey = scaffolding2 | BlockUtils.isHoney(material4);
                this.cauldron |= material4.is(Blocks.CAULDRON);
                this.cake |= material4.is(Blocks.CAKE);
                scaffolding2 = this.ironBars;
                this.ironBars = scaffolding2 | BlockUtils.isIronBars(material4);
                scaffolding2 = this.enchantingTable;
                this.enchantingTable = scaffolding2 | BlockUtils.isEnchantingTable(material4);
                scaffolding2 = this.amethyst;
                this.amethyst = scaffolding2 | BlockUtils.isAmethyst(material4);
                scaffolding2 = this.rail;
                this.rail = scaffolding2 | BlockUtils.isRail(material4);
                scaffolding2 = this.lantern;
                this.lantern = scaffolding2 | BlockUtils.isLantern(material4);
                scaffolding2 = this.powderSnow;
                this.powderSnow = scaffolding2 | BlockUtils.isPowderSnow(material4);
                scaffolding2 = this.dripstone;
                this.dripstone = scaffolding2 | BlockUtils.isDripstone(material4);
                scaffolding2 = this.chest;
                this.chest = scaffolding2 | BlockUtils.isChest(material4);
                scaffolding2 = this.seagrass;
                this.seagrass = scaffolding2 | BlockUtils.isSeagrass(material4);
                scaffolding2 = this.bell;
                this.bell = scaffolding2 | BlockUtils.isBell(material4);
                scaffolding2 = this.chain;
                this.chain = scaffolding2 | BlockUtils.isChain(material4);
                scaffolding2 = this.lectern;
                this.lectern = scaffolding2 | BlockUtils.isLectern(material4);
                scaffolding2 = this.sweetBerries;
                this.sweetBerries = scaffolding2 | BlockUtils.isSweetBerries(material4);
                scaffolding2 = this.bubbleColumn;
                this.bubbleColumn = scaffolding2 | BlockUtils.isBubbleColumn(material4);
                scaffolding2 = this.frostedIce;
                this.frostedIce = scaffolding2 | BlockUtils.isFrostedIce(material4);
                scaffolding2 = this.soulSand;
                this.soulSand = scaffolding2 | BlockUtils.isSoulSandOrSoil(material4);
                scaffolding2 = this.carpet;
                this.carpet = scaffolding2 | BlockUtils.isCarpet(material4);
                scaffolding2 = this.trapdoor;
                this.trapdoor = scaffolding2 | BlockUtils.isTrapdoor(material4);
                scaffolding2 = this.wall;
                this.wall = scaffolding2 | BlockUtils.isWall(material4);
                scaffolding2 = this.farmland;
                this.farmland = scaffolding2 | BlockUtils.isFarmland(material4);
                scaffolding2 = this.stairs;
                this.stairs = scaffolding2 | BlockUtils.isStairs(material4);
                scaffolding2 = this.pressurePlate;
                this.pressurePlate = scaffolding2 | BlockUtils.isPressurePlate(material4);
                scaffolding2 = this.fence;
                this.fence = scaffolding2 | BlockUtils.isFence(material4);
                scaffolding2 = this.climbable;
                this.climbable = scaffolding2 | BlockUtils.isClimbable(material4);
                scaffolding2 = this.fenceGate;
                this.fenceGate = scaffolding2 | BlockUtils.isFenceGate(material4);
                this.slab |= BlockUtils.isSlab(material4);
                scaffolding2 = this.door;
                this.door = scaffolding2 | BlockUtils.isDoor(material4);
                scaffolding2 = this.glassPane;
                this.glassPane = scaffolding2 | BlockUtils.isGlassPane(material4);
                scaffolding2 = this.skull;
                this.skull = scaffolding2 | BlockUtils.isSkull(material4);
                scaffolding2 = this.bed;
                this.bed = scaffolding2 | BlockUtils.isBed(material4);
                scaffolding2 = this.campfire;
                this.campfire = scaffolding2 | BlockUtils.isCampfire(material4);
                scaffolding2 = this.shulkerBox;
                this.shulkerBox = scaffolding2 | BlockUtils.isShulkerBox(material4);
                scaffolding2 = this.nearDirtPath;
                this.nearDirtPath = scaffolding2 | BlockUtils.isDirtPath(material4);
                scaffolding2 = this.sign;
                this.sign = scaffolding2 | BlockUtils.isSign(material4);
                scaffolding2 = this.bamboo;
                this.bamboo = scaffolding2 | BlockUtils.isBamboo(material4);
                scaffolding2 = this.nearSculk;
                this.nearSculk = scaffolding2 | BlockUtils.isSculk(material4);
                scaffolding2 = this.nearSculkSensor;
                this.nearSculkSensor = scaffolding2 | BlockUtils.isSculkSensor(material4);
                scaffolding2 = this.nearCandle;
                this.nearCandle = scaffolding2 | BlockUtils.isCandle(material4);
                scaffolding2 = this.nearDripleaf;
                this.nearDripleaf = scaffolding2 | BlockUtils.isDripleaf(material4);
                scaffolding2 = this.nearCactus;
                this.nearCactus = scaffolding2 | BlockUtils.isCactus(material4);
                scaffolding2 = this.nearHeavyCore;
                this.nearHeavyCore = scaffolding2 | BlockUtils.isHeavyCore(material4);
                scaffolding2 = this.nearDriedGhast;
                this.nearDriedGhast = scaffolding2 | BlockUtils.isDriedGhast(material4);
                scaffolding2 = this.nearBlueIce;
                this.nearBlueIce = scaffolding2 | BlockUtils.isBlueIce(material4);
            }
            this.boat = false;
            this.shulker = false;
            this.touchedEntity = false;
            this.happyGhast = false;
            if (!false) {
                for (Entity entity2 : this.nearbyEntities) {
                    boolean boat2 = this.boat;
                    this.boat = boat2 | EntityTypeMatcher.isBoat(entity2);
                    boat2 = this.shulker;
                    this.shulker = boat2 | EntityTypeMatcher.isShulker(entity2);
                    boat2 = this.happyGhast;
                    this.happyGhast = boat2 | EntityTypeMatcher.isHappyGhast(entity2);
                    if (entity2 instanceof LivingEntity) {
                        this.touchedEntity = true;
                    }
                    if (entity2 instanceof FireworkRocketEntity) {
                        this.ticksSinceFirework = 0;
                    }
                    if (entity2 instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon || entity2 instanceof net.minecraft.world.entity.boss.EnderDragonPart) {
                        MotionState motionState = this.playerData.getMotionState();
                        motionState.setTicksSinceDragonDamage(0);
                    }
                }
            } else {
                for (TrackedEntity trackedEntity : this.closeEntities) {
                    boolean boat3 = this.boat;
                    this.boat = boat3 | trackedEntity.getEntityType() == net.minecraft.world.entity.net.minecraft.world.entity.EntityType<?>.BOAT;
                    boat3 = this.shulker;
                    this.shulker = boat3 | trackedEntity.getEntityType() == net.minecraft.world.entity.net.minecraft.world.entity.EntityType<?>.SHULKER;
                    boat3 = this.happyGhast;
                    this.happyGhast = boat3 | trackedEntity.getEntityType() == net.minecraft.world.entity.net.minecraft.world.entity.EntityType<?>.HAPPY_GHAST;
                    if (trackedEntity.getEntityType() == net.minecraft.world.entity.net.minecraft.world.entity.EntityType<?>.PLAYER || trackedEntity.getEntityType().canSerialize() /* VERIFY: living-entity predicate */) {
                        this.touchedEntity = true;
                    }
                    if (trackedEntity.getEntityType() == net.minecraft.world.entity.net.minecraft.world.entity.EntityType<?>.ENDER_DRAGON) {
                        MotionState motionState2 = this.playerData.getMotionState();
                        motionState2.setTicksSinceDragonDamage(0);
                    }
                }
                AxisAlignedBox axisAlignedBox = new AxisAlignedBox(this.x - 0.3, this.y, this.z - 0.3, this.x + 0.3, this.y + 1.8, this.z + 0.3);
                AxisAlignedBox axisAlignedBox2 = axisAlignedBox.expand(0.03, 0.03, 0.03);
                this.happyGhast = this.wideEntities.stream().filter(CollisionState::isHappyGhast).anyMatch(entity -> CollisionState.intersectsTrackedEntity(axisAlignedBox2, entity));
            }
            if (false) {
                if (this.closeEntities.size() > MeshSettings.settingsEntityCramEntitiesAmount) {
                    this.ticksSinceEntityCramming = 0;
                }
            } else if (this.nearbyEntities.size() > MeshSettings.settingsEntityCramEntitiesAmount) {
                this.ticksSinceEntityCramming = 0;
            }
            this.touchingBubbleColumn = CheckUtils.touchesBubbleColumn(this.playerData);
            this.ladderOrVine = CheckUtils.touchesClimbable(this.playerData);
            PlayerData playerData2 = this.playerData;
            this.touchingWeb = CheckUtils.touchesWeb(playerData2);
            playerData2 = this.playerData;
            this.touchingLiquid = CheckUtils.touchesLiquid(playerData2);
            this.fullySubmerged = CheckUtils.isFullySubmerged(this.playerData);
        }
        if (this.boat || this.shulker || this.trapdoor || this.carpet || this.campfire || this.brewingStand || this.nearRepeater || this.daylightDetector || this.skull || this.lilyPad || this.shulkerBox || this.flowerPot || this.happyGhast) {
            this.ghostActivityTicks = 0;
        }
    }

    private void scanNearbyBlocksLegacy() {
        boolean inOpenAir2;
        int index2 = 25924;
        this.blockX = (int) Math.floor(this.x);
        this.blockY = (int) Math.floor(this.y);
        this.blockZ = (int) Math.floor(this.z);
        ServerLevel world2 = this.world;
        int blockX2 = this.blockX;
        int blockY2 = this.blockY;
        int blockZ2 = this.blockZ;
        this.surroundingMaterialsLegacy = BlockUtils.getSurroundingMaterials(world2, blockX2, blockY2, blockZ2, 1);
        int blockX3 = (short) index2;
        this.floorMaterials = CheckUtils.getFloorMaterialsLegacy(this.playerData);
        this.ceilingMaterials = CheckUtils.getCeilingMaterialsLegacy(this.playerData);
        this.bodyMaterials = CheckUtils.getBodyMaterialsLegacy(this.playerData);
        this.headMaterials = CheckUtils.getHeadMaterialsLegacy(this.playerData);
        ServerLevel world3 = this.world;
        blockX3 = this.blockX;
        blockZ2 = (int) Math.floor(this.y - 1.0);
        int blockZ3 = this.blockZ;
        this.groundMaterialLegacy = BlockUtils.getMaterial(world3, blockX3, blockZ2, blockZ3);
        ServerLevel world4 = this.world;
        blockX3 = this.blockX;
        blockZ2 = (int) Math.floor(this.y - 2.0);
        int blockZ4 = this.blockZ;
        this.subGroundMaterialLegacy = BlockUtils.getMaterial(world4, blockX3, blockZ2, blockZ4);
        ServerLevel world5 = this.world;
        blockX3 = this.blockX;
        blockZ2 = (int) Math.floor(this.y - 3.0);
        int blockZ5 = this.blockZ;
        this.deepGroundMaterialLegacy = BlockUtils.getMaterial(world5, blockX3, blockZ2, blockZ5);
        this.touchingIce = CheckUtils.touchesIceLegacy(this.playerData);
        this.fullyStuck = CheckUtils.isEnclosedInSolidBlocksLegacy(this.playerData);
        this.partiallyStuck = CheckUtils.touchesSolidBlocks(this.playerData);
        this.collidingVertically = !ConditionUtils.allMatch((Collection) this.ceilingMaterials, (Predicate<BlockState>) BlockUtils::isAir);
        this.collidingHorizontally = !ConditionUtils.allMatch((Collection) this.bodyMaterials, (Predicate<BlockState>) BlockUtils::isAir);
        this.nearSolid = ConditionUtils.anyMatch((Collection) this.surroundingMaterialsLegacy, (Predicate<BlockState>) BlockUtils::isSolid);
        inOpenAir2 = ((ConditionUtils.allMatch((Collection) this.bodyMaterials, (Predicate<BlockState>) BlockUtils::isAir)) && (ConditionUtils.allMatch((Collection) this.floorMaterials, (Predicate<BlockState>) BlockUtils::isAir)));
        this.inOpenAir = inOpenAir2;
        this.scanLegacyContacts();
    }

    private void scanLegacyContacts() {
        this.nearCactus = false;
        this.nearDripleaf = false;
        this.ironBars = false;
        this.amethyst = false;
        this.enchantingTable = false;
        this.pressurePlate = false;
        this.bamboo = false;
        this.sign = false;
        this.cake = false;
        this.chest = false;
        this.farmland = false;
        this.bed = false;
        this.skull = false;
        this.glassPane = false;
        this.piston = false;
        this.lilyPad = false;
        this.door = false;
        this.hopper = false;
        this.slab = false;
        this.rail = false;
        this.boat = false;
        this.nearRepeater = false;
        this.anvil = false;
        this.snow = false;
        this.fenceGate = false;
        this.ice = false;
        this.stairs = false;
        this.cauldron = false;
        this.wall = false;
        this.brewingStand = false;
        this.daylightDetector = false;
        this.lantern = false;
        this.nearDirtPath = false;
        this.endPortalFrame = false;
        this.trapdoor = false;
        this.flowerPot = false;
        this.web = false;
        this.climbable = false;
        this.soulSand = false;
        this.carpet = false;
        this.liquid = false;
        this.fence = false;
        this.slime = false;
        if ((this.surroundingMaterialsLegacy != null) && (this.nearbyEntities != null)) {
            for (BlockState material : this.surroundingMaterialsLegacy) {
                this.slime |= BlockUtils.isSlimeBlock(material);
                BlockState material38 = material;
                this.fence |= BlockUtils.isFence(material38);
                this.liquid |= BlockUtils.isLiquid(material);
                BlockState material2 = material;
                this.carpet |= BlockUtils.isCarpet(material2);
                BlockState material3 = material;
                this.soulSand |= BlockUtils.isSoulSandOrSoil(material3);
                BlockState material4 = material;
                this.climbable |= BlockUtils.isClimbable(material4);
                BlockState material5 = material;
                this.web |= BlockUtils.isWeb(material5);
                BlockState material6 = material;
                this.flowerPot |= BlockUtils.isFlowerPot(material6);
                BlockState material7 = material;
                this.trapdoor |= BlockUtils.isTrapdoor(material7);
                BlockState material8 = material;
                this.endPortalFrame |= BlockUtils.isEndPortalFrame(material8);
                BlockState material9 = material;
                this.daylightDetector |= BlockUtils.isDaylightDetector(material9);
                BlockState material10 = material;
                this.brewingStand |= BlockUtils.isBrewingStand(material10);
                BlockState material11 = material;
                this.wall |= BlockUtils.isWall(material11);
                BlockState material12 = material;
                this.cauldron |= BlockUtils.isCauldron(material12);
                BlockState material13 = material;
                this.stairs |= BlockUtils.isStairs(material13);
                BlockState material14 = material;
                this.ice |= BlockUtils.isIce(material14);
                BlockState material15 = material;
                this.fenceGate |= BlockUtils.isFenceGate(material15);
                BlockState material16 = material;
                this.snow |= BlockUtils.isSnow(material16);
                BlockState material17 = material;
                this.anvil |= BlockUtils.isAnvil(material17);
                BlockState material18 = material;
                this.nearRepeater |= BlockUtils.isRepeater(material18);
                this.slab |= BlockUtils.isSlab(material);
                BlockState material19 = material;
                this.hopper |= BlockUtils.isHopper(material19);
                BlockState material20 = material;
                this.door |= BlockUtils.isDoor(material20);
                BlockState material21 = material;
                this.lilyPad |= BlockUtils.isLilyPad(material21);
                this.piston |= BlockUtils.isPiston(material);
                BlockState material22 = material;
                this.glassPane |= BlockUtils.isGlassPane(material22);
                BlockState material23 = material;
                this.skull |= BlockUtils.isSkull(material23);
                BlockState material24 = material;
                this.bed |= BlockUtils.isBed(material24);
                BlockState material25 = material;
                this.farmland |= BlockUtils.isFarmland(material25);
                BlockState material26 = material;
                this.chest |= BlockUtils.isChest(material26);
                BlockState material27 = material;
                this.cake |= BlockUtils.isCake(material27);
                BlockState material28 = material;
                this.nearDirtPath |= BlockUtils.isDirtPath(material28);
                BlockState material29 = material;
                this.lantern |= BlockUtils.isLantern(material29);
                BlockState material30 = material;
                this.rail |= BlockUtils.isRail(material30);
                BlockState material31 = material;
                this.sign |= BlockUtils.isSign(material31);
                BlockState material32 = material;
                this.bamboo |= BlockUtils.isBamboo(material32);
                BlockState material33 = material;
                this.pressurePlate |= BlockUtils.isPressurePlate(material33);
                BlockState material34 = material;
                this.enchantingTable |= BlockUtils.isEnchantingTable(material34);
                BlockState material35 = material;
                this.amethyst |= BlockUtils.isAmethyst(material35);
                BlockState material36 = material;
                this.ironBars |= BlockUtils.isIronBars(material36);
                BlockState material37 = material;
                this.nearCactus |= BlockUtils.isCactus(material37);
            }
            for (Entity material : this.nearbyEntities) {
                Entity material39 = material;
                this.boat |= EntityTypeMatcher.isBoat(material39);
                if (!(material instanceof EnderDragon) && !(material instanceof EnderDragonPart))
                    continue;
                Integer value = 0;
                this.playerData.getMotionState().setTicksSinceDragonDamage(value);
            }
            PlayerData playerData5 = this.playerData;
            this.ladderOrVine = CheckUtils.touchesClimbableLegacy(playerData5);
            PlayerData playerData2 = this.playerData;
            this.touchingWeb = CheckUtils.touchesWebLegacy(playerData2);
            PlayerData playerData3 = this.playerData;
            this.touchingLiquid = CheckUtils.touchesLiquidLegacy(playerData3);
            PlayerData playerData4 = this.playerData;
            this.fullySubmerged = CheckUtils.isFullySubmergedLegacy(playerData4);
        }
        if (!(this.boat)) {
            if (!(this.shulker)) {
                if (!(this.trapdoor)) {
                    if (!(this.carpet)) {
                        if (!(this.brewingStand)) {
                            if (!(this.nearRepeater)) {
                                if (!(this.daylightDetector)) {
                                    if (!(this.skull)) {
                                        if (!(this.lilyPad)) {
                                            if ((this.flowerPot)) {
                                                this.ghostActivityTicks = 0;
                                            }
                                        } else {
                                            this.ghostActivityTicks = 0;
                                        }
                                    } else {
                                        this.ghostActivityTicks = 0;
                                    }
                                } else {
                                    this.ghostActivityTicks = 0;
                                }
                            } else {
                                this.ghostActivityTicks = 0;
                            }
                        } else {
                            this.ghostActivityTicks = 0;
                        }
                    } else {
                        this.ghostActivityTicks = 0;
                    }
                } else {
                    this.ghostActivityTicks = 0;
                }
            } else {
                this.ghostActivityTicks = 0;
            }
        } else {
            this.ghostActivityTicks = 0;
        }
    }

    public void handleVehicleMove(WrapperPlayClientVehicleMove wrapperPlayClientVehicleMove) {
        ++this.ticksInVehicle;
        this.previousVehicle = this.vehicle;
        if (this.playerData.getPlayer() != null) {
            this.vehicle = this.playerData.getPlayer().getVehicle();
        }
        if (this.previousVehicle != this.vehicle) {
            this.ticksInVehicle = 0;
        }
        this.vehicleX = wrapperPlayClientVehicleMove.getPosition().getX();
        this.vehicleY = wrapperPlayClientVehicleMove.getPosition().getY();
        this.vehicleZ = wrapperPlayClientVehicleMove.getPosition().getZ();
        Double vehicleX2 = null;
        Double vehicleY2 = null;
        Double vehicleZ2 = this.vehicleZ;
        vehicleY2 = this.vehicleY;
        vehicleX2 = this.vehicleX;
        this.unloadedChunkTicks = this.playerData.getWorldBlockTracker().hasTrackedBlockNearby(vehicleX2, vehicleY2, vehicleZ2) ? 0 : ++this.unloadedChunkTicks;
        Double vehicleX3 = null;
        Double vehicleY3 = null;
        Double vehicleZ3 = null;
        Double value = 2.0;
        vehicleZ3 = this.vehicleZ;
        vehicleY3 = this.vehicleY;
        vehicleX3 = this.vehicleX;
        this.closeEntities = this.playerData.getEntityTracker().getNearbyEntities(vehicleX3, vehicleY3, vehicleZ3, value);
        this.previousVehicleDeltaX = this.vehicleDeltaX;
        this.previousVehicleDeltaY = this.vehicleDeltaY;
        this.previousVehicleDeltaZ = this.vehicleDeltaZ;
        this.vehicleDeltaX = this.vehicleX - this.previousVehicleX;
        this.vehicleDeltaY = this.vehicleY - this.previousVehicleY;
        this.vehicleDeltaZ = this.vehicleZ - this.previousVehicleZ;
        Double vehicleDeltaX2 = null;
        Double vehicleDeltaZ2 = this.vehicleDeltaZ;
        vehicleDeltaX2 = this.vehicleDeltaX;
        this.vehicleHorizontalSpeed = MathUtils.hypot(vehicleDeltaX2, vehicleDeltaZ2);
        if ((ExemptionManager.isBefore1_13()) && (ExemptionManager.isAtLeast1_9())) {
            ServerLevel world2 = null;
            Integer value2 = null;
            Integer value3 = null;
            Integer value4 = null;
            Integer value5 = 1;
            value4 = (int) Math.floor(this.vehicleZ);
            value3 = (int) Math.floor(this.vehicleY);
            value2 = (int) Math.floor(this.vehicleX);
            world2 = this.world;
            this.areaScanMaterials = BlockUtils.getSurroundingMaterials(world2, value2, value3, value4, value5);
        }
        if (this.areaScanMaterials != null) {
            this.surroundingBed = false;
            this.surroundingBubbleColumn = false;
            this.surroundingSlime = false;
            this.surroundingPiston = false;
            this.surroundingLiquid = false;
            this.surroundingIce = false;
            for (Object value6 : this.areaScanMaterials) {
                Object value7 = value6;
                this.surroundingIce |= BlockUtils.isIce((BlockState) value7);
                this.surroundingLiquid |= BlockUtils.isLiquid((BlockState) value6);
                this.surroundingPiston |= BlockUtils.isPiston((BlockState) value6);
                this.surroundingSlime |= BlockUtils.isSlimeBlock((BlockState) value6);
                Object value8 = value6;
                this.surroundingBubbleColumn |= BlockUtils.isBubbleColumn((BlockState) value8);
                Object value9 = value6;
                this.surroundingBed |= BlockUtils.isBed((BlockState) value9);
            }
            Collection areaScanMaterials2 = null;
            Predicate value10 = (Predicate<BlockState>) BlockUtils::isAir;
            areaScanMaterials2 = this.areaScanMaterials;
            this.surroundedByAir = ConditionUtils.allMatch(areaScanMaterials2, value10);
        }
        this.nearbyBoatCount = 0;
        this.nearLivingEntity = false;
        this.nearHappyGhast = false;
        if ((this.nearbyEntities != null) && (!false)) {
            for (Object value11 : this.nearbyEntities) {
                if (value11 instanceof Boat) {
                    ++this.nearbyBoatCount;
                }
                if (this.vehicle != null) {
                    if (this.vehicle.getPassengers().contains(value11))
                        continue;
                }
                if (this.vehicle != null) {
                    if (this.vehicle.getEntityId() == ((Entity) value11).getEntityId())
                        continue;
                }
                if (value11 instanceof LivingEntity) {
                    this.nearLivingEntity = true;
                }
                if (!EntityTypeMatcher.isHappyGhast((Entity) value11))
                    continue;
                this.nearHappyGhast = true;
            }
        }
        if ((false) && (this.closeEntities != null)) {
            for (TrackedEntity value12 : this.closeEntities) {
                if (net.minecraft.world.entity.net.minecraft.world.entity.EntityType<?>.isTypeInstanceOf((value12).getEntityType(), net.minecraft.world.entity.net.minecraft.world.entity.EntityType<?>.BOAT)) {
                    ++this.nearbyBoatCount;
                }
                if ((value12).getEntityId() == this.playerData.getVehicleTracker().getVehicleEntityId())
                    continue;
                if ((value12).getEntityType().isInstanceOf(net.minecraft.world.entity.net.minecraft.world.entity.EntityType<?>.LIVINGENTITY)) {
                    this.nearLivingEntity = true;
                }
                if ((value12).getEntityType() != net.minecraft.world.entity.net.minecraft.world.entity.EntityType<?>.HAPPY_GHAST)
                    continue;
                this.nearHappyGhast = true;
            }
        }
        this.ticksSinceSurroundingIce = this.surroundingIce ? 0 : ++this.ticksSinceSurroundingIce;
        this.ticksSinceSurroundingPiston = this.surroundingPiston ? 0 : ++this.ticksSinceSurroundingPiston;
        this.ticksSinceSurroundingBed = this.surroundingBed ? 0 : ++this.ticksSinceSurroundingBed;
        this.ticksSinceSurroundingLiquid = this.surroundingLiquid ? 0 : ++this.ticksSinceSurroundingLiquid;
        this.ticksSinceSurroundingSlime = this.surroundingSlime ? 0 : ++this.ticksSinceSurroundingSlime;
        this.ticksSinceSurroundingBubbleColumn = this.surroundingBubbleColumn ? 0 : ++this.ticksSinceSurroundingBubbleColumn;
        this.ticksInOpenAir = this.surroundedByAir ? ++this.ticksInOpenAir : 0;
        if (this.nearHappyGhast) {
            this.ticksInOpenAir = 0;
        }
        this.previousVehicleX = this.vehicleX;
        this.previousVehicleY = this.vehicleY;
        this.previousVehicleZ = this.vehicleZ;
    }

    public void scanFoliaEntities() {
        if ((this.nearbyEntities != null) && (this.playerData.getPlayer() != null) && (this.playerData.getPlayer().getVehicle() != null)) {
            for (Entity entity : this.nearbyEntities) {
                if (entity instanceof Boat) {
                    ++this.nearbyBoatCount;
                }
                if (this.vehicle != null) {
                    if (this.vehicle.getPassengers().contains(entity))
                        continue;
                }
                if (this.vehicle != null) {
                    if (this.vehicle.getEntityId() == entity.getEntityId())
                        continue;
                }
                if (entity instanceof LivingEntity) {
                    this.nearLivingEntity = true;
                }
                if (EntityTypeMatcher.isHappyGhast(entity)) {
                    this.nearHappyGhast = true;
                }
            }
        }
    }

    public void updateGroundState() {
        this.ticksSinceFishingRod++;
        if (this.onGround) {
            this.groundTicks++;
        } else {
            this.groundTicks = 0;
        }
        if (!this.onGround) {
            this.airTicks++;
        } else {
            this.airTicks = 0;
        }
    }

    public void updateTickCounters() {
        boolean inLiquid2;
        this.ticksSinceEntityCramming++;
        if (this.inOpenAir) {
            this.ghostActivityTicks++;
        } else {
            this.ghostActivityTicks = 0;
        }
        if (this.groundAligned) {
            this.groundAlignedTicks++;
        } else {
            this.groundAlignedTicks = 0;
        }
        if (ExemptionManager.isNewerThan1_13()) {
            this.solidFooting = this.groundMaterial != null && this.groundMaterial.isSolid() && !BlockUtils.isLiquid(this.groundMaterial);
        } else {
            this.solidFooting = this.groundMaterialLegacy != null && this.groundMaterialLegacy.isSolid() && !BlockUtils.isLiquid(this.groundMaterialLegacy);
        }
        if (!this.solidFooting) {
            this.ticksSinceSolidFooting++;
        } else {
            this.ticksSinceSolidFooting = 0;
        }
        if (this.flySpeed > 0.11F) {
            this.ticksSinceHighFlySpeed = 0;
        } else {
            this.ticksSinceHighFlySpeed++;
        }
        if (!this.slime && (this.groundMaterialLegacy == null || !BlockUtils.isSlimeBlock(this.groundMaterialLegacy)) && (this.subGroundMaterialLegacy == null || !BlockUtils.isSlimeBlock(this.subGroundMaterialLegacy)) && (this.deepGroundMaterialLegacy == null || !BlockUtils.isSlimeBlock(this.deepGroundMaterialLegacy)) && (this.groundMaterial == null || !BlockUtils.isSlimeBlock(this.groundMaterial)) && (this.subGroundBlockState == null || !BlockUtils.isSlimeBlock(this.subGroundMaterial)) && (this.deepGroundBlockState == null || !BlockUtils.isSlimeBlock(this.deepGroundMaterial))) {
            this.ticksSinceSlime++;
        } else {
            this.ticksSinceSlime = 0;
        }
        this.ticksSinceWaterlogged++;
        if (this.ice) {
            this.ticksSinceIce = 0;
        } else {
            this.ticksSinceIce++;
        }
        if (this.boat) {
            this.ticksSinceBoat = 0;
        } else {
            this.ticksSinceBoat++;
        }
        if (this.shulker) {
            this.ticksSinceShulker = 0;
        } else {
            this.ticksSinceShulker++;
        }
        MotionState speedMotion = this.playerData.getMotionState();
        if (!speedMotion.isSpeedEffect()) {
            this.ticksSinceSpeedEffect++;
        } else {
            this.ticksSinceSpeedEffect = 0;
        }
        if (ExemptionManager.isAtLeast1_8()) {
            PlayerData playerData2 = this.playerData;
            if (playerData2.getPlayer() != null) {
                playerData2 = this.playerData;
                if (playerData2.getPlayer().getGameMode() == GameMode.SPECTATOR) {
                }
            }
        }
        if (this.bed) {
            this.ticksSinceBed = 0;
        } else {
            this.ticksSinceBed++;
        }
        if (this.farmland) {
            this.ticksSinceFarmland = 0;
        } else {
            this.ticksSinceFarmland++;
        }
        if (this.slab) {
            this.ticksSinceSlab = 0;
        } else {
            this.ticksSinceSlab++;
        }
        if (this.happyGhast) {
            this.ticksSinceHappyGhast = 0;
        } else {
            this.ticksSinceHappyGhast++;
        }
        if (this.piston) {
            this.ticksSincePiston = 0;
        } else {
            this.ticksSincePiston++;
        }
        if (this.touchedEntity) {
            this.ticksSinceEntityCollision = 0;
        } else {
            this.ticksSinceEntityCollision++;
        }
        PlayerData playerData3 = this.playerData;
        MotionState motionState = playerData3.getMotionState();
        if (!motionState.isPacketJumpBoostEffect()) {
            playerData3 = this.playerData;
            MotionState motionState2 = playerData3.getMotionState();
            if (!motionState2.isJumpBoostEffect()) {
                this.ticksSinceJumpBoost++;
            } else {
                this.ticksSinceJumpBoost = 0;
            }
        } else {
            this.ticksSinceJumpBoost = 0;
        }
        if (this.collidingVertically) {
            this.ticksSinceVerticalCollision = 0;
        } else {
            this.ticksSinceVerticalCollision++;
        }
        if (this.collidingHorizontally) {
            this.ticksSinceHorizontalCollision = 0;
        } else {
            this.ticksSinceHorizontalCollision++;
        }
        if (!(this.frostedIce)) {
        }
        if (this.fence) {
            this.ticksSinceFence = 0;
        } else {
            this.ticksSinceFence++;
        }
        if (this.touchingIce) {
            this.ticksSinceIceContact = 0;
        } else {
            this.ticksSinceIceContact++;
        }
        if (this.trapdoor) {
            this.ticksSinceTrapdoor = 0;
        } else {
            this.ticksSinceTrapdoor++;
        }
        if (this.web) {
            this.ticksSinceWeb = 0;
        } else {
            this.ticksSinceWeb++;
        }
        if (this.climbable) {
            this.ticksSinceClimbable = 0;
        } else {
            this.ticksSinceClimbable++;
        }
        if (!this.shulker && !this.shulkerBox) {
            this.ticksSinceShulkerBox++;
        } else {
            this.ticksSinceShulkerBox = 0;
        }
        if (this.scaffolding) {
            this.ticksSinceScaffolding = 0;
        } else {
            this.ticksSinceScaffolding++;
        }
        if (!this.touchingLiquid && !this.liquid && !this.touchingBubbleColumn && !this.bubbleColumn) {
            this.ticksSinceLiquid++;
        } else {
            this.ticksSinceLiquid = 0;
        }
        if (this.ladderOrVine) {
            this.ticksOnLadder++;
        } else {
            this.ticksOnLadder = 0;
        }
        if (this.stairs) {
            this.ticksSinceStairs = 0;
        } else {
            this.ticksSinceStairs++;
        }
        if (this.bodyMaterials != null) {
            if (ConditionUtils.anyMatch((Collection) this.bodyMaterials, (Predicate<BlockState>) BlockUtils::isSlimeBlock)) {
                this.ticksSinceNearbySlime = 0;
            } else {
                this.ticksSinceNearbySlime++;
            }
        } else {
            this.ticksSinceNearbySlime++;
        }
        if (this.bodyMaterials != null) {
            if (ConditionUtils.anyMatch((Collection) this.bodyMaterials, (Predicate<BlockState>) BlockUtils::isPiston)) {
                this.ticksSinceNearbyPiston = 0;
            } else {
                this.ticksSinceNearbyPiston++;
            }
        } else {
            this.ticksSinceNearbyPiston++;
        }
        if (this.bodyMaterials != null) {
            if (ConditionUtils.anyMatch((Collection) this.bodyMaterials, (Predicate<BlockState>) material -> BlockUtils.isSlab(material))) {
                this.ticksSinceNearbySlab = 0;
            } else {
                this.ticksSinceNearbySlab++;
            }
        } else {
            this.ticksSinceNearbySlab++;
        }
        inLiquid2 = ((this.floorMaterials != null) && (ConditionUtils.allMatch((Collection) this.floorMaterials, (Predicate<BlockState>) material -> BlockUtils.isLiquid(material))));
        this.inLiquid = inLiquid2;
        if (!(this.inLiquid)) {
        }
        PlayerData playerData4 = this.playerData;
        if (playerData4.getPlayer() != null) {
            playerData4 = this.playerData;
            if (!playerData4.getPlayer().isInsideVehicle()) {
                playerData4 = this.playerData;
                if (playerData4.getPlayer().getVehicle() == null) {
                    this.ticksSinceVehicle++;
                } else {
                    this.ticksSinceVehicle = 0;
                }
            } else {
                this.ticksSinceVehicle = 0;
            }
        }
        this.ticksSinceFirework++;
        playerData4 = this.playerData;
        if (playerData4.getPlayer() != null) {
            playerData4 = this.playerData;
            if (!playerData4.getPlayer().getAllowFlight() && !this.flightAllowed) {
                this.ticksSinceFlightAllowed++;
            } else {
                this.ticksSinceFlightAllowed = 0;
            }
        }
        if (ExemptionManager.isAtLeast1_9()) {
            MotionState slowFallingMotion = this.playerData.getMotionState();
            if (!slowFallingMotion.isSlowFallingEffect()) {
                this.ticksSinceSlowFalling++;
            } else {
                this.ticksSinceSlowFalling = 0;
            }
            MotionState swimMotion = this.playerData.getMotionState();
            if (!CheckUtils.isSwimming(this.playerData) && !swimMotion.isPacketSwimming()) {
                this.ticksSinceSwimming++;
            } else {
                this.ticksSinceSwimming = 0;
            }
            MotionState glideMotion = this.playerData.getMotionState();
            if (!glideMotion.isGliding() && !glideMotion.isPacketGliding()) {
                this.ticksWithoutGliding++;
            } else {
                this.ticksWithoutGliding = 0;
            }
            if (CheckUtils.isGliding(this.playerData)) {
                this.glidingTicks++;
            } else {
                this.glidingTicks = 0;
            }
            MotionState glideMotion2 = this.playerData.getMotionState();
            if (CheckUtils.isGliding(this.playerData) || glideMotion2.isPacketGliding()) {
                this.lastGlidingDeltaXZ = this.deltaXZ;
            }
            if (ExemptionManager.isAtLeast1_16() && this.soulSand) {
                playerData4 = this.playerData;
                MotionState motionState3 = playerData4.getMotionState();
                if (motionState3.getBoots() != null) {
                    playerData4 = this.playerData;
                    MotionState motionState4 = playerData4.getMotionState();
                    if (motionState4.getBoots().getEnchantmentLevel(Enchantment.SOUL_SPEED) > 0) {
                        this.ticksSinceSoulSpeed = 0;
                    } else {
                        this.ticksSinceSoulSpeed++;
                    }
                } else {
                    this.ticksSinceSoulSpeed++;
                }
            } else {
                this.ticksSinceSoulSpeed++;
            }
            playerData4 = this.playerData;
            MotionState motionState5 = playerData4.getMotionState();
            if (motionState5.isWearingElytra()) {
                this.ticksSinceWearingElytra = 0;
            } else {
                this.ticksSinceWearingElytra++;
            }
            if (!CheckUtils.isRiptiding(this.playerData)) {
                playerData4 = this.playerData;
                MotionState motionState6 = playerData4.getMotionState();
                if (!motionState6.isPacketRiptide()) {
                    this.ticksSinceRiptide++;
                } else {
                    this.ticksSinceRiptide = 0;
                }
            } else {
                this.ticksSinceRiptide = 0;
            }
            if (this.honey) {
                this.ticksSinceHoney = 0;
            } else {
                this.ticksSinceHoney++;
            }
            MotionState levitationMotion = this.playerData.getMotionState();
            if (!levitationMotion.isPacketLevitationEffect()) {
                this.ticksSinceLevitation++;
            } else {
                this.ticksSinceLevitation = 0;
            }
            if (CheckUtils.hasDolphinsGrace(this.playerData)) {
                this.ticksSinceDolphinsGrace = 0;
            } else {
                this.ticksSinceDolphinsGrace++;
            }
            if (!this.touchingBubbleColumn && !this.bubbleColumn) {
                this.ticksSinceBubbleColumn++;
            } else {
                this.ticksSinceBubbleColumn = 0;
            }
            if (CheckUtils.holdsRiptideTrident(this.playerData)) {
                this.ticksSinceRiptideTrident = 0;
            } else {
                this.ticksSinceRiptideTrident++;
            }
        }
    }

    public void setTicksSinceRiptide(WrapperPlayClientPlayerDigging wrapperPlayClientPlayerDigging) {
        PlayerData playerData2 = this.playerData;
        if ((playerData2.getPlayer() != null) && (ExemptionManager.isAtLeast1_9() && wrapperPlayClientPlayerDigging.getAction() == DiggingAction.RELEASE_USE_ITEM)) {
            boolean condition = this.ticksSinceLiquid < 30 || this.ticksSinceWaterlogged < 30;
            playerData2 = this.playerData;
            boolean condition2 = playerData2.getPlayer().getPlayerWeather() == WeatherType.DOWNFALL;
            playerData2 = this.playerData;
            boolean hasStorm2 = playerData2.getPlayer().getWorld().hasStorm();
            playerData2 = this.playerData;
            boolean recentBucketEmpty = playerData2.getMotionState().getTicksSinceBucketEmpty() < 20;
            if (!condition && !hasStorm2 && !condition2 && !recentBucketEmpty) {
                playerData2 = this.playerData;
                if (!playerData2.getMotionState().isRaining()) {
                    return;
                }
            }
            if (this.ticksSinceRiptideTrident < 20) {
                this.ticksSinceRiptide = 0;
            }
        }
    }

    public boolean isNearGround() {
        if (ExemptionManager.isNewerThan1_13()) {
            if (this.groundMaterial != null && this.subGroundBlockState != null) {
                return !BlockUtils.isAir(this.groundMaterial) || !BlockUtils.isAir(this.subGroundMaterial);
            }
        } else if (this.groundMaterialLegacy != null && this.subGroundMaterialLegacy != null) {
            return !BlockUtils.isAir(this.groundMaterialLegacy) || !BlockUtils.isAir(this.subGroundMaterialLegacy);
        }
        return false;
    }

    public boolean isNearGroundWide() {
        return this.groundMaterial != null && this.subGroundBlockState != null && this.deepGroundBlockState != null ? !BlockUtils.isAir(this.groundMaterial) || !BlockUtils.isAir(this.subGroundMaterial) || !BlockUtils.isAir(this.deepGroundMaterial) : false;
    }

    public boolean hasNoGroundBeneath() {
        if (ExemptionManager.isNewerThan1_13()) {
            if (this.floorMaterials == null)
                return false;
            if (this.deepFloorMaterials == null)
                return false;
            Collection floorMaterials2 = null;
            Predicate value = (Predicate<BlockState>) BlockUtils::isAir;
            floorMaterials2 = this.floorMaterials;
            if (!ConditionUtils.allMatch(floorMaterials2, value))
                return false;
            Collection deepFloorMaterials2 = null;
            Predicate value2 = (Predicate<BlockState>) BlockUtils::isAir;
            deepFloorMaterials2 = this.deepFloorMaterials;
            if (!ConditionUtils.allMatch(deepFloorMaterials2, value2))
                return false;
            return true;
        }
        if (this.floorMaterials == null)
            return false;
        Collection floorMaterials3 = null;
        Predicate value3 = (Predicate<BlockState>) BlockUtils::isAir;
        floorMaterials3 = this.floorMaterials;
        if (!ConditionUtils.allMatch(floorMaterials3, value3))
            return false;
        return true;
    }

    public double getDeltaXZChange() {
        return Math.abs(this.deltaXZ - this.previousDeltaXZ);
    }

    public void recordFishingHook() {
        this.ticksSinceFishingRod = 0;
    }

    public void recordHeadWaterlogged() {
        this.ticksSinceWaterlogged = 0;
                this.lastWaterloggedTick = MeshServices.ticks();
    }

    private void applyGhostBlockFix() {
        boolean condition;
        PlayerData playerData2 = this.playerData;
        if ((playerData2.getPlayer() != null) && (MeshSettings.ghostBlocksFixEnabled) && (!(ExemptionManager.getTps() < MeshSettings.ghostBlocksFixMinimumTps))) {
            playerData2 = this.playerData;
            playerData2.getPlayer().hasPermission("vulcan.bypass.ghostblock");
            playerData2 = this.playerData;
            if (playerData2.getUser().getClientVersion().isOlderThan(ClientVersion.V_1_17)) {
                if (ExemptionManager.isAtLeast1_17() && MeshSettings.ghostBlocksFixIgnoreLegacyVia) {
                    if (this.getY() < 5.0) {
                        return;
                    }
                }
            }
            boolean anyExempt;
            playerData2 = this.playerData;
            PlayerToggles playerToggles = playerData2.getPlayerToggles();
            anyExempt = playerToggles.isAnyExempt(new ExemptType[] { ExemptType.LAGGED_NEAR_GROUND, ExemptType.BLOCK_PLACE_FAST, ExemptType.NOT_MOVING, ExemptType.LAGGED_NEAR_GROUND_MODERN, ExemptType.FLIGHT, ExemptType.TELEPORT, ExemptType.SWIMMING, ExemptType.DEATH, ExemptType.HAPPY_GHAST });
            if (!MeshSettings.settingsIgnoreFlagsGeyserClientBrand || !this.playerData.getClientBrand().toLowerCase().contains("geyser")) {
                if (MeshSettings.settingsIgnoreGeyserPrefixes) {
                    playerData2 = this.playerData;
                    if (playerData2.getPlayerName().startsWith(MeshSettings.settingsIgnoreGeyserPrefix)) {
                        condition = true;
                    } else {
                        playerData2 = this.playerData;
                        if (playerData2.getPlayer() == null || !this.playerData.getUniqueId().toString().startsWith("00000")) {
                            condition = false;
                        } else {
                            condition = true;
                        }
                    }
                } else {
                    playerData2 = this.playerData;
                    if (playerData2.getPlayer() == null || !this.playerData.getUniqueId().toString().startsWith("00000")) {
                        condition = false;
                    } else {
                        condition = true;
                    }
                }
            } else {
                condition = true;
            }
            boolean condition2 = condition;
            boolean condition3 = this.ghostActivityTicks > MeshSettings.ghostBlocksFixTicks && this.groundTicks >= 1 && this.groundAlignedTicks >= 1;
            if (condition3 && !anyExempt && !condition2) {
                if (++this.ghostFixBuffer > MeshSettings.ghostBlocksFixBufferMax) {
                    if (MeshSettings.settingsEnableApi) {
                        if (vulcanGhostBlockEvent.isCancelled()) {
                            return;
                        }
                    }
                    if (MeshSettings.ghostBlocksFixSetback) {
                        playerData2 = this.playerData;
                        CollisionState collisionState = playerData2.getCollisionState();
                        if (collisionState.getSetbackX() == 0.0) {
                            playerData2 = this.playerData;
                            CollisionState collisionState2 = playerData2.getCollisionState();
                            if (collisionState2.getSetbackY() == 0.0) {
                                playerData2 = this.playerData;
                                CollisionState collisionState3 = playerData2.getCollisionState();
                                if ((collisionState3.getSetbackZ() != 0.0)) {
                                    playerData2 = this.playerData;
                                    playerData2.setMitigationPending(true);
                                    this.ghostBlockSetbacks++;
                                                                        this.lastGhostSetbackTick = MeshServices.ticks();
                                }
                            } else {
                                playerData2 = this.playerData;
                                playerData2.setMitigationPending(true);
                                this.ghostBlockSetbacks++;
                                                                this.lastGhostSetbackTick = MeshServices.ticks();
                            }
                        } else {
                            playerData2 = this.playerData;
                            playerData2.setMitigationPending(true);
                            this.ghostBlockSetbacks++;
                                                        this.lastGhostSetbackTick = MeshServices.ticks();
                        }
                    }
                    if (MeshSettings.ghostBlocksFixMessageEnabled) {
                        playerData2 = this.playerData;
                        Player player2 = playerData2.getPlayer();
                        String ghostBlocksFixMessage2 = MeshSettings.ghostBlocksFixMessage;
                        player2.sendMessage(TextUtil.parse(ghostBlocksFixMessage2));
                    }
                    if (MeshSettings.ghostBlocksFixStaffMessageEnabled) {
                        AlertManager alertManager = MeshServices.alerts();
                        String text = MeshSettings.ghostBlocksFixStaffMessage.replaceAll("%x%", Integer.toString(this.blockX)).replaceAll("%y%", Integer.toString(this.blockY)).replaceAll("%z%", Integer.toString(this.blockZ)).replaceAll("%ticks%", Integer.toString(this.ghostActivityTicks));
                        PlayerData playerData3 = this.playerData;
                        text = text.replaceAll("%world%", (ServerLevel) playerData3.getPlayer().level().getName());
                        playerData3 = this.playerData;
                        alertManager.broadcastAlert(text.replaceAll("%player%", playerData3.getPlayerName()));
                    }
                    if (MeshSettings.ghostBlocksFixPrintToConsole) {
                        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("meshac");
                        Level info = Level.INFO;
                        String text2 = MeshSettings.ghostBlocksFixConsoleMessage.replaceAll("%ticks%", Integer.toString(this.ghostActivityTicks));
                        PlayerData playerData4 = this.playerData;
                        text2 = text2.replaceAll("%player%", playerData4.getPlayerName());
                        playerData4 = this.playerData;
                        logger.log(info, TextUtil.parse(text2.replaceAll("%world%", playerData4.getPlayer().getWorld().getName()).replaceAll("%z%", Integer.toString(this.blockZ)).replaceAll("%y%", Integer.toString(this.blockY)).replaceAll("%x%", Integer.toString(this.blockX))));
                    }
                    this.ghostActivityTicks = 0;
                    this.ghostFixBuffer = 0.0;
                }
            } else if (this.ghostFixBuffer > 0.0) {
                this.ghostFixBuffer = this.ghostFixBuffer - MeshSettings.ghostBlocksFixBufferDecay;
            }
            return;
        }
    }

    public void handlePlayerAbilities(ServerAbilities wrapperPlayServerPlayerAbilities) {
        this.playerData.getConfirmationTracker().runAfterConfirmation(() -> this.applyPlayerAbilities(wrapperPlayServerPlayerAbilities));
    }

    public boolean isOnSoulSand() {
        if (ExemptionManager.isNewerThan1_13()) {
            if (this.surroundingMaterials == null)
                return false;
            if (this.surroundingMaterials.isEmpty())
                return false;
            BlockState value = this.surroundingMaterials.get(16);
            if (!BlockUtils.isSoulSand(value))
                return false;
            return true;
        }
        if (this.surroundingMaterialsLegacy == null)
            return false;
        if (this.surroundingMaterialsLegacy.isEmpty())
            return false;
        if (!BlockUtils.isSoulSand(this.surroundingMaterialsLegacy.get(16)))
            return false;
        return true;
    }

    public void requestSetback() {
        if (!(this.y < this.setbackY) || !MeshSettings.settingsDontSetbackLowerPositions) {
            PlayerData playerData2 = this.playerData;
            CollisionState collisionState = playerData2.getCollisionState();
            if (collisionState.getSetbackX() == 0.0) {
                playerData2 = this.playerData;
                CollisionState collisionState2 = playerData2.getCollisionState();
                if (collisionState2.getSetbackY() == 0.0) {
                    playerData2 = this.playerData;
                    CollisionState collisionState3 = playerData2.getCollisionState();
                    if (collisionState3.getSetbackZ() == 0.0) {
                        return;
                    }
                }
            }
            playerData2 = this.playerData;
            playerData2.setMitigationPending(true);
        }
    }

    public void teleportOutOfUnloadedChunks() {
        boolean condition;
        if (MeshSettings.unloadedChunksSetbackEnabled) {
            PlayerData playerData2 = this.playerData;
            if (playerData2.getPlayer() != null) {
                playerData2 = this.playerData;
                PlayerToggles playerToggles = playerData2.getPlayerToggles();
                if (!playerToggles.isAnyExempt(new ExemptType[] { ExemptType.JOINED, ExemptType.TELEPORT, ExemptType.WORLD_CHANGE, ExemptType.LIQUID, ExemptType.CHUNK, ExemptType.WORLD_CHANGE, ExemptType.FLIGHT, ExemptType.ELYTRA, ExemptType.GLIDING, ExemptType.SLOW_FALLING })) {
                    playerData2 = this.playerData;
                    if (playerData2.getMotionState().getTicksSinceJoin() >= 20) {
                        playerData2 = this.playerData;
                        if (!playerData2.getMotionState().isTeleporting()) {
                            condition = false;
                        } else {
                            condition = true;
                        }
                    } else {
                        condition = true;
                    }
                } else {
                    condition = true;
                }
                boolean condition2 = condition;
                double deltaYDifference = Math.abs(Math.abs(this.deltaY) - 0.09800000190734881);
                if (this.deltaY < -0.09F && !this.onGround && !condition2 && deltaYDifference < 0.005) {
                    if (++this.unloadedChunkBuffer > MeshSettings.unloadedChunksMaxTicks) {
                        playerData2 = this.playerData;
                        RotationTracker rotationTracker = playerData2.getRotationTracker();
                        float yaw = rotationTracker.getYaw();
                        playerData2 = this.playerData;
                        RotationTracker rotationTracker2 = playerData2.getRotationTracker();
                        float pitch = rotationTracker2.getPitch();
                        Location location = new Location(this.world, this.setbackX, this.setbackY, this.setbackZ, yaw, pitch);
                        if (false) {
                            WrapperPlayServerPlayerPositionAndLook wrapperPlayServerPlayerPositionAndLook = new WrapperPlayServerPlayerPositionAndLook(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch(), (byte) 0, 69, false);
                            playerData2 = this.playerData;
                            playerData2.getUser().sendPacket(wrapperPlayServerPlayerPositionAndLook);
                        } else {
                            MeshServices.mainThread(() -> this.teleportToLoadedChunk(location));
                        }
                        if (!MeshSettings.unloadedChunksMessage.isEmpty()) {
                            AlertManager alertManager = MeshServices.alerts();
                            String text = MeshSettings.unloadedChunksMessage.replaceAll("%x%", Integer.toString(this.blockX)).replaceAll("%y%", Integer.toString(this.blockY)).replaceAll("%z%", Integer.toString(this.blockZ)).replaceAll("%ticks%", Integer.toString(this.ghostActivityTicks));
                            PlayerData playerData3 = this.playerData;
                            text = text.replaceAll("%world%", (ServerLevel) playerData3.getPlayer().level().getName());
                            playerData3 = this.playerData;
                            alertManager.broadcastAlert(text.replaceAll("%player%", playerData3.getPlayerName()));
                        }
                        this.unloadedChunkBuffer = 0.0;
                    } else if (this.unloadedChunkBuffer > 0.0) {
                        this.unloadedChunkBuffer -= 0.025;
                    }
                }
                return;
            }
        }
    }

    public void queueAcknowledgedTask(Runnable runnable) {
        this.acknowledgedTasks.add(runnable);
    }

    public void runAcknowledgedTasks() {
        this.acknowledgedTasks.forEach(Runnable::run);
        this.acknowledgedTasks.clear();
    }

    public boolean isFence() {
        return this.fence;
    }

    public boolean isSlime() {
        return this.slime;
    }

    public boolean isSoulSand() {
        return this.soulSand;
    }

    public boolean isClimbable() {
        return this.climbable;
    }

    public boolean isWeb() {
        return this.web;
    }

    public boolean isLilyPad() {
        return this.lilyPad;
    }

    public boolean isDaylightDetector() {
        return this.daylightDetector;
    }

    public boolean isBrewingStand() {
        return this.brewingStand;
    }

    public boolean isWall() {
        return this.wall;
    }

    public boolean isStairs() {
        return this.stairs;
    }

    public boolean isSlab() {
        return this.slab;
    }

    public boolean isHoney() {
        return this.honey;
    }

    public boolean isScaffolding() {
        return this.scaffolding;
    }

    public boolean isTrapdoor() {
        return this.trapdoor;
    }

    public boolean isSkull() {
        return this.skull;
    }

    public boolean isEndPortalFrame() {
        return this.endPortalFrame;
    }

    public boolean isCampfire() {
        return this.campfire;
    }

    public boolean isSweetBerries() {
        return this.sweetBerries;
    }

    public boolean isShulkerBox() {
        return this.shulkerBox;
    }

    public boolean isSnow() {
        return this.snow;
    }

    public boolean isAnvil() {
        return this.anvil;
    }

    public boolean isEndRod() {
        return this.endRod;
    }

    public boolean isChain() {
        return this.chain;
    }

    public boolean isPiston() {
        return this.piston;
    }

    public boolean isDoor() {
        return this.door;
    }

    public boolean isInOpenAir() {
        return this.inOpenAir;
    }

    public boolean isCauldron() {
        return this.cauldron;
    }

    public boolean isHopper() {
        return this.hopper;
    }

    public boolean isFenceGate() {
        return this.fenceGate;
    }

    public boolean isFlowerPot() {
        return this.flowerPot;
    }

    public boolean isLadderOrVine() {
        return this.ladderOrVine;
    }

    public boolean isTouchingWeb() {
        return this.touchingWeb;
    }

    public boolean isFullySubmerged() {
        return this.fullySubmerged;
    }

    public boolean isCollidingVertically() {
        return this.collidingVertically;
    }

    public boolean isKelp() {
        return this.kelp;
    }

    public boolean isFullyStuck() {
        return this.fullyStuck;
    }

    public boolean isPartiallyStuck() {
        return this.partiallyStuck;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public boolean isGroundAligned() {
        return this.groundAligned;
    }

    public boolean isShulker() {
        return this.shulker;
    }

    public boolean isBoat() {
        return this.boat;
    }

    public boolean isBed() {
        return this.bed;
    }

    public boolean isCarpet() {
        return this.carpet;
    }

    public boolean isLectern() {
        return this.lectern;
    }

    public boolean isTurtleEgg() {
        return this.turtleEgg;
    }

    public boolean isSeaPickle() {
        return this.seaPickle;
    }

    public boolean isIce() {
        return this.ice;
    }

    public boolean isConduit() {
        return this.conduit;
    }

    public boolean isCollidingHorizontally() {
        return this.collidingHorizontally;
    }

    public boolean isWallClimbEligible() {
        return this.wallClimbEligible;
    }

    public void setWallClimbEligible(boolean wallClimbEligible) {
        this.wallClimbEligible = wallClimbEligible;
    }

    public boolean isNearRepeater() {
        return this.nearRepeater;
    }

    public boolean isNearSolid() {
        return this.nearSolid;
    }

    public boolean isGlassPane() {
        return this.glassPane;
    }

    public boolean isSurroundedByAir() {
        return this.surroundedByAir;
    }

    public boolean isMoving() {
        return this.moving;
    }

    public boolean isPreviousOnGround() {
        return this.previousOnGround;
    }

    public boolean isFlightAllowed() {
        return this.flightAllowed;
    }

    public boolean isSeagrass() {
        return this.seagrass;
    }

    public boolean isChest() {
        return this.chest;
    }

    public boolean isCake() {
        return this.cake;
    }

    public boolean isAmethyst() {
        return this.amethyst;
    }

    public boolean isDripstone() {
        return this.dripstone;
    }

    public boolean isPowderSnow() {
        return this.powderSnow;
    }

    public boolean isOlderOnGround() {
        return this.olderOnGround;
    }

    public boolean hasLivingEntityNearby() {
        return this.nearLivingEntity;
    }

    public boolean isNearDirtPath() {
        return this.nearDirtPath;
    }

    public boolean isLantern() {
        return this.lantern;
    }

    public boolean isNearWorldBorder() {
        return this.nearWorldBorder;
    }

    public boolean isNearCandle() {
        return this.nearCandle;
    }

    public boolean isSign() {
        return this.sign;
    }

    public boolean isBamboo() {
        return this.bamboo;
    }

    public boolean isPressurePlate() {
        return this.pressurePlate;
    }

    public boolean isSetbackPending() {
        return this.setbackPending;
    }

    public boolean isNearSculkSensor() {
        return this.nearSculkSensor;
    }

    public boolean isInLiquid() {
        return this.inLiquid;
    }

    public boolean hasSlabAbove() {
        return this.slabAbove;
    }

    public boolean isNearDripleaf() {
        return this.nearDripleaf;
    }

    public boolean isNearCactus() {
        return this.nearCactus;
    }

    public boolean isNearHeavyCore() {
        return this.nearHeavyCore;
    }

    public boolean isPreviousPositionChanged() {
        return this.previousPositionChanged;
    }

    public boolean isOlderPositionChanged() {
        return this.olderPositionChanged;
    }

    public boolean isHappyGhast() {
        return this.happyGhast;
    }

    public boolean isNearDriedGhast() {
        return this.nearDriedGhast;
    }

    public boolean isFrozen() {
        return this.frozen;
    }

    public List<Entity> getNearbyEntities() {
        return this.nearbyEntities;
    }

    public BlockState getGroundMaterialLegacy() {
        return this.groundMaterialLegacy;
    }

    public BlockState getSubGroundMaterialLegacy() {
        return this.subGroundMaterialLegacy;
    }

    public BlockState getDeepGroundMaterialLegacy() {
        return this.deepGroundMaterialLegacy;
    }

    public List<BlockState> getSurroundingMaterialsLegacy() {
        return this.surroundingMaterialsLegacy;
    }

    public List<BlockState> getFloorMaterials() {
        return this.floorMaterials;
    }

    public List<BlockState> getCeilingMaterials() {
        return this.ceilingMaterials;
    }

    public List<BlockState> getHeadMaterials() {
        return this.headMaterials;
    }

    public List<BlockState> getSurroundingMaterials() {
        return this.surroundingMaterials;
    }

    public BlockState getGroundMaterial() {
        return this.groundMaterial;
    }

    public BlockState getSubGroundMaterial() {
        return this.subGroundMaterial;
    }

    public BlockState getDeepGroundMaterial() {
        return this.deepGroundMaterial;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public double getDeltaX() {
        return this.deltaX;
    }

    public double getDeltaY() {
        return this.deltaY;
    }

    public double getDeltaZ() {
        return this.deltaZ;
    }

    public double getDeltaXZ() {
        return this.deltaXZ;
    }

    public double getPreviousDeltaX() {
        return this.previousDeltaX;
    }

    public double getPreviousDeltaZ() {
        return this.previousDeltaZ;
    }

    public double getPreviousDeltaY() {
        return this.previousDeltaY;
    }

    public double getPreviousDeltaXZ() {
        return this.previousDeltaXZ;
    }

    public double getVehicleY() {
        return this.vehicleY;
    }

    public double getPreviousVehicleDeltaY() {
        return this.previousVehicleDeltaY;
    }

    public double getVehicleDeltaY() {
        return this.vehicleDeltaY;
    }

    public double getVehicleHorizontalSpeed() {
        return this.vehicleHorizontalSpeed;
    }

    public double getSetbackX() {
        return this.setbackX;
    }

    public double getSetbackY() {
        return this.setbackY;
    }

    public double getSetbackZ() {
        return this.setbackZ;
    }

    public double getJoinX() {
        return this.joinX;
    }

    public double getJoinY() {
        return this.joinY;
    }

    public double getJoinZ() {
        return this.joinZ;
    }

    public double getLastGlidingDeltaXZ() {
        return this.lastGlidingDeltaXZ;
    }

    public int getTicksSinceVehicle() {
        return this.ticksSinceVehicle;
    }

    public int getTicksSinceFlightAllowed() {
        return this.ticksSinceFlightAllowed;
    }

    public int getTicksSinceTrapdoor() {
        return this.ticksSinceTrapdoor;
    }

    public int getAirTicks() {
        return this.airTicks;
    }

    public int getGroundTicks() {
        return this.groundTicks;
    }

    public int getTicksSinceSwimming() {
        return this.ticksSinceSwimming;
    }

    public int getTicksSinceRiptide() {
        return this.ticksSinceRiptide;
    }

    public int getTicksWithoutGliding() {
        return this.ticksWithoutGliding;
    }

    public int getTicksSinceIceContact() {
        return this.ticksSinceIceContact;
    }

    public int getTicksSinceDolphinsGrace() {
        return this.ticksSinceDolphinsGrace;
    }

    public int getTicksSinceLevitation() {
        return this.ticksSinceLevitation;
    }

    public int getTicksSinceBubbleColumn() {
        return this.ticksSinceBubbleColumn;
    }

    public int getGlidingTicks() {
        return this.glidingTicks;
    }

    public int getTicksSinceSlowFalling() {
        return this.ticksSinceSlowFalling;
    }

    public int getGhostActivityTicks() {
        return this.ghostActivityTicks;
    }

    public int getGroundAlignedTicks() {
        return this.groundAlignedTicks;
    }

    public int getTicksSinceWeb() {
        return this.ticksSinceWeb;
    }

    public int getTicksSinceLiquid() {
        return this.ticksSinceLiquid;
    }

    public int getTicksOnLadder() {
        return this.ticksOnLadder;
    }

    public int getTicksSinceJumpBoost() {
        return this.ticksSinceJumpBoost;
    }

    public int getTicksSinceSlime() {
        return this.ticksSinceSlime;
    }

    public int getTicksSinceHoney() {
        return this.ticksSinceHoney;
    }

    public int getTicksSinceVerticalCollision() {
        return this.ticksSinceVerticalCollision;
    }

    public int getTicksSinceFence() {
        return this.ticksSinceFence;
    }

    public int getTicksSinceSoulSpeed() {
        return this.ticksSinceSoulSpeed;
    }

    public int getTicksSinceBed() {
        return this.ticksSinceBed;
    }

    public int getTicksInVehicle() {
        return this.ticksInVehicle;
    }

    public int getTicksSinceFirework() {
        return this.ticksSinceFirework;
    }

    public int getTicksSinceIce() {
        return this.ticksSinceIce;
    }

    public int getTicksSincePiston() {
        return this.ticksSincePiston;
    }

    public int getTicksSinceSpeedEffect() {
        return this.ticksSinceSpeedEffect;
    }

    public int getTicksSinceNearbySlime() {
        return this.ticksSinceNearbySlime;
    }

    public int getTicksSinceNearbyPiston() {
        return this.ticksSinceNearbyPiston;
    }

    public int getTicksSinceSurroundingIce() {
        return this.ticksSinceSurroundingIce;
    }

    public int getTicksSinceSurroundingLiquid() {
        return this.ticksSinceSurroundingLiquid;
    }

    public int getTicksSinceSurroundingSlime() {
        return this.ticksSinceSurroundingSlime;
    }

    public int getTicksSinceSurroundingBubbleColumn() {
        return this.ticksSinceSurroundingBubbleColumn;
    }

    public int getBlockX() {
        return this.blockX;
    }

    public int getBlockY() {
        return this.blockY;
    }

    public int getBlockZ() {
        return this.blockZ;
    }

    public int getTicksSinceScaffolding() {
        return this.ticksSinceScaffolding;
    }

    public int getTicksInOpenAir() {
        return this.ticksInOpenAir;
    }

    public int getTicksSinceShulkerBox() {
        return this.ticksSinceShulkerBox;
    }

    public int getTicksSinceWearingElytra() {
        return this.ticksSinceWearingElytra;
    }

    public int getTicksSinceFarmland() {
        return this.ticksSinceFarmland;
    }

    public int getTicksSinceStairs() {
        return this.ticksSinceStairs;
    }

    public int getTicksSinceSlab() {
        return this.ticksSinceSlab;
    }

    public int getTicksSinceEntityCollision() {
        return this.ticksSinceEntityCollision;
    }

    public int getTicksSinceSurroundingPiston() {
        return this.ticksSinceSurroundingPiston;
    }

    public int getTicksSinceFishingRod() {
        return this.ticksSinceFishingRod;
    }

    public int getTicksSinceHighFlySpeed() {
        return this.ticksSinceHighFlySpeed;
    }

    public int getTicksSinceNearbySlab() {
        return this.ticksSinceNearbySlab;
    }

    public int getTicksSinceSurroundingBed() {
        return this.ticksSinceSurroundingBed;
    }

    public int getTicksSinceWaterlogged() {
        return this.ticksSinceWaterlogged;
    }

    public int getTicksSinceClimbable() {
        return this.ticksSinceClimbable;
    }

    public int getNearbyBoatCount() {
        return this.nearbyBoatCount;
    }

    public int getTicksSinceHorizontalCollision() {
        return this.ticksSinceHorizontalCollision;
    }

    public int getTicksSinceEntityCramming() {
        return this.ticksSinceEntityCramming;
    }

    public int getTicksSinceSolidFooting() {
        return this.ticksSinceSolidFooting;
    }

    public int getTicksSinceBoat() {
        return this.ticksSinceBoat;
    }

    public int getTicksSinceShulker() {
        return this.ticksSinceShulker;
    }

    public int getTicksSinceHappyGhast() {
        return this.ticksSinceHappyGhast;
    }

    public int getGhostBlockSetbacks() {
        return this.ghostBlockSetbacks;
    }

    public int getUnloadedChunkTicks() {
        return this.unloadedChunkTicks;
    }

    public float getWalkSpeed() {
        return this.walkSpeed;
    }

    public boolean isExemptFlight() {
        return this.exemptFlight;
    }

    public boolean isExemptCreative() {
        return this.exemptCreative;
    }

    public boolean isExemptJoined() {
        return this.exemptJoined;
    }

    public boolean isExemptLiquid() {
        return this.exemptLiquid;
    }

    public boolean isExemptLevitation() {
        return this.exemptLevitation;
    }

    public boolean isExemptSlowFalling() {
        return this.exemptSlowFalling;
    }

    public boolean isExemptRiptide() {
        return this.exemptRiptide;
    }

    public boolean isExemptVehicle() {
        return this.exemptVehicle;
    }

    public boolean isExemptLenientScaffolding() {
        return this.exemptLenientScaffolding;
    }

    public boolean isExemptGliding() {
        return this.exemptGliding;
    }

    public boolean isExemptElytra() {
        return this.exemptElytra;
    }

    public boolean isExemptTeleport() {
        return this.exemptTeleport;
    }

    public boolean isExemptEnderPearl() {
        return this.exemptEnderPearl;
    }

    public boolean isExemptChunk() {
        return this.exemptChunk;
    }

    public boolean isExemptComboMode() {
        return this.exemptComboMode;
    }

    public boolean isExemptMythicMob() {
        return this.exemptMythicMob;
    }

    public boolean isExemptClimbable() {
        return this.exemptClimbable;
    }

    public void setSetbackX(double setbackX2) {
        this.setbackX = setbackX2;
    }

    public void setSetbackY(double setbackY2) {
        this.setbackY = setbackY2;
    }

    public void setSetbackZ(double setbackZ2) {
        this.setbackZ = setbackZ2;
    }

    public void setJoinX(double x) {
        this.joinX = x;
    }

    public void setJoinY(double y) {
        this.joinY = y;
    }

    public void setJoinZ(double z) {
        this.joinZ = z;
    }

    public void setTicksSinceRiptide(int ticksSinceRiptide2) {
        this.ticksSinceRiptide = ticksSinceRiptide2;
    }

    public void setTicksSinceFirework(int ticksSinceFirework2) {
        this.ticksSinceFirework = ticksSinceFirework2;
    }

    public void setTicksSinceSpeedEffect(int ticksSinceSpeedEffect2) {
        this.ticksSinceSpeedEffect = ticksSinceSpeedEffect2;
    }

    public void setFrozen(boolean frozen2) {
        this.frozen = frozen2;
    }

    public void setNearbyEntities(List nearbyEntities2) {
        this.nearbyEntities = nearbyEntities2;
    }

    public void setSurroundingMaterials(List packedMaterials2) {
        this.surroundingMaterials = packedMaterials2;
    }

    public void setAreaScanMaterials(List areaScanMaterials2) {
        this.areaScanMaterials = areaScanMaterials2;
    }

    public void setDeepFloorMaterials(List deepFloorMaterials2) {
        this.deepFloorMaterials = deepFloorMaterials2;
    }

    public void setGroundMaterial(BlockState groundMaterial2) {
        this.groundMaterial = groundMaterial2;
    }

    public void setSubGroundMaterial(BlockState subGroundMaterial2) {
        this.subGroundBlockState = subGroundMaterial2;
    }

    public void setDeepGroundMaterial(BlockState deepGroundMaterial2) {
        this.deepGroundBlockState = deepGroundMaterial2;
    }

    private void teleportToLoadedChunk(Location location) {
        this.playerData.getPlayer().connection.teleport(location.getX(), location.getY(), location.getZ(), this.playerData.getRotationTracker().getYaw(), this.playerData.getRotationTracker().getPitch(), java.util.Set.of(), false);
    }

    private void applyPlayerAbilities(ServerAbilities wrapperPlayServerPlayerAbilities2) {
        this.ticksSinceFlightAllowed = 0;
                this.lastAbilitiesTick = MeshServices.ticks();
        this.flightAllowed = wrapperPlayServerPlayerAbilities2.flightAllowed();
        this.flySpeed = wrapperPlayServerPlayerAbilities2.flySpeed() * 2.0F;
    }

    private static boolean intersectsTrackedEntity(AxisAlignedBox axisAlignedBox, TrackedEntity trackedEntity) {
        AxisAlignedBox axisAlignedBox2 = trackedEntity.getAxisAlignedBox();
        return axisAlignedBox2.intersects(axisAlignedBox);
    }

    private static boolean isHappyGhast(TrackedEntity trackedEntity) {
        EntityType happyGhast2 = net.minecraft.world.entity.net.minecraft.world.entity.EntityType<?>.HAPPY_GHAST;
        return happyGhast2.equals(trackedEntity.getEntityType());
    }

    public void recordFeetWaterlogged() {
        this.ticksSinceWaterlogged = 0;
                this.lastWaterloggedTick = MeshServices.ticks();
    }

	private static BlockState levelState(ServerLevel level, int x, int y, int z) {
		if (level == null) return Blocks.AIR.defaultBlockState();
		return level.getBlockState(new BlockPos(x, y, z));
	}

	/** Abilities snapshot carried in place of the packet wrapper. */
	public record ServerAbilities(boolean flightAllowed, float flySpeed) {
	}
}
