package dev.meshac.world;

import java.util.LinkedList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block classification helpers. Ported from Vulcan's BlockUtils; Bukkit Material name
 * matching became registry-id name matching on BlockState.
 */
public final class BlockUtils {

	private static final BlockState SPONGE = Blocks.SPONGE.defaultBlockState();

	private BlockUtils() {
	}

	public static String nameOf(BlockState state) {
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().toUpperCase();
	}

	public static List<BlockState> getSurroundingMaterials(ServerLevel level, int x, int y, int z, int radius) {
		List<BlockState> materials = new LinkedList<>();
		if (level == null) return materials;
		for (int blockX = x - radius; blockX <= x + radius; blockX++) {
			for (int blockY = y - radius; blockY <= y + radius + 1; blockY++) {
				for (int blockZ = z - radius; blockZ <= z + radius; blockZ++) {
					if (level.hasChunk(blockX >> 4, blockZ >> 4)) {
						materials.add(level.getBlockState(new BlockPos(blockX, blockY, blockZ)));
					} else {
						materials.add(SPONGE);
					}
				}
			}
		}
		return materials;
	}

	public static List<BlockState> getDeepFloorMaterials(ServerLevel level, int x, int y, int z) {
		List<BlockState> materials = new LinkedList<>();
		if (level == null) return materials;
		materials.add(getMaterial(level, x + 1, y - 2, z));
		materials.add(getMaterial(level, x - 1, y - 2, z));
		materials.add(getMaterial(level, x, y - 2, z + 1));
		materials.add(getMaterial(level, x, y - 2, z - 1));
		materials.add(getMaterial(level, x + 1, y - 2, z + 1));
		materials.add(getMaterial(level, x - 1, y - 2, z + 1));
		materials.add(getMaterial(level, x - 1, y - 2, z - 1));
		materials.add(getMaterial(level, x, y - 2, z));
		materials.add(getMaterial(level, x + 1, y - 2, z - 1));
		return materials;
	}

	public static BlockState getMaterial(ServerLevel level, int x, int y, int z) {
		if (level == null) return SPONGE;
		return level.hasChunk(x >> 4, z >> 4) ? level.getBlockState(new BlockPos(x, y, z)) : SPONGE;
	}

	public static boolean isCake(BlockState s) { return nameOf(s).contains("CAKE"); }
	public static boolean isAir(BlockState s) { return s.isAir(); }
	public static boolean isDriedGhast(BlockState s) { return nameOf(s).contains("DRIED_GHAST"); }
	public static boolean isHeavyCore(BlockState s) { return nameOf(s).contains("HEAVY_CORE"); }
	public static boolean isSolid(BlockState s) { return s.isSolid(); }
	public static boolean isSign(BlockState s) { return nameOf(s).contains("SIGN"); }
	public static boolean isRepeater(BlockState s) { return nameOf(s).contains("REPEATER"); }
	public static boolean isBlueIce(BlockState s) { return nameOf(s).equals("BLUE_ICE"); }
	public static boolean isSculk(BlockState s) { return nameOf(s).equals("SCULK"); }
	public static boolean isSculkSensor(BlockState s) { return nameOf(s).contains("SCULK_SENSOR"); }
	public static boolean isFlowerPot(BlockState s) { return nameOf(s).contains("FLOWER_POT") || nameOf(s).contains("POTTED"); }
	public static boolean isConduit(BlockState s) { return nameOf(s).contains("CONDUIT"); }
	public static boolean isBamboo(BlockState s) { return nameOf(s).contains("BAMBOO"); }
	public static boolean isIronBars(BlockState s) { return nameOf(s).equals("IRON_BARS"); }
	public static boolean isLava(BlockState s) { return nameOf(s).contains("LAVA"); }
	public static boolean isPressurePlate(BlockState s) { return nameOf(s).contains("PRESSURE_PLATE"); }
	public static boolean isGlassPane(BlockState s) { return nameOf(s).contains("GLASS_PANE"); }
	public static boolean isRail(BlockState s) { return nameOf(s).contains("RAIL"); }
	public static boolean isGlass(BlockState s) { return nameOf(s).contains("GLASS") && !nameOf(s).contains("PANE"); }
	public static boolean isPortalFrame(BlockState s) { return nameOf(s).contains("FRAME"); }
	public static boolean isPowderSnow(BlockState s) { return nameOf(s).equals("POWDER_SNOW"); }
	public static boolean isSeagrass(BlockState s) { return nameOf(s).contains("SEAGRASS"); }
	public static boolean isNoteBlock(BlockState s) { return nameOf(s).equals("NOTE_BLOCK"); }
	public static boolean isKelp(BlockState s) { return nameOf(s).contains("KELP"); }
	public static boolean isAmethyst(BlockState s) { return nameOf(s).contains("AMETHYST"); }
	public static boolean isDripstone(BlockState s) { return nameOf(s).contains("DRIPSTONE"); }
	public static boolean isShulkerBox(BlockState s) { return nameOf(s).contains("SHULKER_BOX"); }
	public static boolean isEndRod(BlockState s) { return nameOf(s).equals("END_ROD"); }
	public static boolean isCactus(BlockState s) { return nameOf(s).equals("CACTUS"); }
	public static boolean isCauldron(BlockState s) { return nameOf(s).contains("CAULDRON"); }
	public static boolean isFenceGate(BlockState s) { return nameOf(s).contains("FENCE_GATE"); }
	public static boolean isGrowable(BlockState s) { return nameOf(s).contains("SAPLING") || nameOf(s).contains("CROP") || nameOf(s).equals("WHEAT") || nameOf(s).equals("CARROTS") || nameOf(s).equals("POTATOES") || nameOf(s).equals("BEETROOTS"); }
	public static boolean isSweetBerryBush(BlockState s) { return nameOf(s).equals("SWEET_BERRY_BUSH"); }
	public static boolean isPiston(BlockState s) { return nameOf(s).contains("PISTON"); }
	public static boolean isDoor(BlockState s) { return nameOf(s).contains("DOOR") && !nameOf(s).contains("TRAP"); }
	public static boolean isHopper(BlockState s) { return nameOf(s).equals("HOPPER"); }
	public static boolean isChain(BlockState s) { return nameOf(s).equals("CHAIN"); }

	public static boolean isClimbable(BlockState s) {
		String n = nameOf(s);
		return n.equals("LADDER") || n.contains("VINE") || n.equals("SCAFFOLDING");
	}

	public static boolean isSnow(BlockState s) { return nameOf(s).equals("SNOW") || nameOf(s).equals("SNOW_LAYER"); }

	public static boolean isLiquid(BlockState s) {
		String n = nameOf(s);
		return n.contains("LAVA") || n.contains("WATER") || n.contains("BUBBLE");
	}

	public static boolean isSeaPickle(BlockState s) { return nameOf(s).contains("PICKLE"); }

	public static boolean isTurtleEgg(BlockState s) { return nameOf(s).equals("TURTLE_EGG"); }

	public static boolean isLectern(BlockState s) { return nameOf(s).equals("LECTERN"); }

	public static boolean isWeb(BlockState s) { return nameOf(s).equals("WEB") || nameOf(s).equals("COBWEB"); }

	public static boolean isAnvil(BlockState s) {
		String n = nameOf(s);
		return n.equals("ANVIL") || n.equals("DAMAGED_ANVIL") || n.equals("CHIPPED_ANVIL");
	}

	public static boolean isSlime(BlockState s) { return nameOf(s).contains("SLIME"); }
	public static boolean isTnt(BlockState s) { return nameOf(s).equals("TNT"); }
	public static boolean isSlimeBlock(BlockState s) { return nameOf(s).equals("SLIME_BLOCK"); }
	public static boolean isBoat(BlockState s) { return nameOf(s).contains("BOAT"); }
	public static boolean isFarmland(BlockState s) { return nameOf(s).contains("FARMLAND"); }

	public static boolean isWall(BlockState s) { return nameOf(s).contains("WALL") && !nameOf(s).contains("SIGN"); }

	public static boolean isSweetBerries(BlockState s) { return nameOf(s).equals("SWEET_BERRY_BUSH"); }
	public static boolean isGlassBottle(BlockState s) { return nameOf(s).equals("GLASS_BOTTLE"); }

	public static boolean isLilyPad(BlockState s) { return nameOf(s).equals("LILY_PAD") || nameOf(s).equals("WATER_LILY"); }

	public static boolean isBed(BlockState s) { return nameOf(s).contains("BED") && !nameOf(s).equals("BEDROCK"); }

	public static boolean isEndPortalFrame(BlockState s) { return nameOf(s).equals("END_PORTAL_FRAME") || nameOf(s).equals("ENDER_PORTAL_FRAME"); }

	public static boolean isFence(BlockState s) { return nameOf(s).contains("FENCE"); }
	public static boolean isDaylightDetector(BlockState s) { return nameOf(s).equals("DAYLIGHT_DETECTOR"); }
	public static boolean isStairs(BlockState s) { return nameOf(s).contains("STAIR"); }

	public static boolean isSlab(BlockState s) { return nameOf(s).contains("SLAB") || nameOf(s).contains("STEP"); }

	public static boolean isTrapdoor(BlockState s) { return nameOf(s).contains("TRAP") && nameOf(s).contains("DOOR"); }

	public static boolean isSkull(BlockState s) { return nameOf(s).contains("SKULL") || nameOf(s).contains("HEAD"); }

	public static boolean isHoney(BlockState s) { return nameOf(s).equals("HONEY_BLOCK"); }

	public static boolean isBubbleColumn(BlockState s) { return nameOf(s).equals("BUBBLE_COLUMN"); }

	public static boolean isScaffolding(BlockState s) { return nameOf(s).equals("SCAFFOLDING"); }

	public static boolean isCampfire(BlockState s) { return nameOf(s).equals("CAMPFIRE") || nameOf(s).equals("SOUL_CAMPFIRE"); }

	public static boolean isBrewingStand(BlockState s) { return nameOf(s).equals("BREWING_STAND"); }

	public static boolean isFrostedIce(BlockState s) { return nameOf(s).equals("FROSTED_ICE"); }

	public static boolean isCarpet(BlockState s) { return nameOf(s).contains("CARPET"); }

	public static boolean isIce(BlockState s) {
		String n = nameOf(s);
		return n.equals("ICE") || n.equals("PACKED_ICE") || n.equals("BLUE_ICE") || n.equals("FROSTED_ICE");
	}

	public static boolean isTripwire(BlockState s) { return nameOf(s).contains("WIRE") || nameOf(s).contains("STRING"); }

	public static boolean isBell(BlockState s) { return nameOf(s).equals("BELL"); }

	public static boolean isSoulBlock(BlockState s) { return nameOf(s).contains("SOUL"); }

	public static boolean isSoulSand(BlockState s) { return nameOf(s).equalsIgnoreCase("SOUL_SAND"); }

	public static boolean isSoulSandOrSoil(BlockState s) { return nameOf(s).equals("SOUL_SAND") || nameOf(s).equalsIgnoreCase("SOUL_SOIL"); }

	public static boolean isChest(BlockState s) { return nameOf(s).contains("CHEST"); }

	public static boolean isDeepslate(BlockState s) { return nameOf(s).contains("DEEPSLATE"); }

	public static boolean isDirtPath(BlockState s) { return nameOf(s).contains("PATH"); }

	public static boolean isSand(BlockState s) { return nameOf(s).contains("SAND") && !nameOf(s).contains("STONE"); }

	public static boolean isGravel(BlockState s) { return nameOf(s).contains("GRAVEL"); }

	public static boolean isLantern(BlockState s) { return nameOf(s).contains("LANTERN"); }

	public static boolean isConcretePowder(BlockState s) { return nameOf(s).contains("CONCRETE") && nameOf(s).contains("POWDER"); }

	public static boolean isStrippedWood(BlockState s) { return nameOf(s).contains("STRIPPED"); }

	public static boolean isEnchantingTable(BlockState s) { return nameOf(s).contains("ENCHANT"); }

	public static boolean isCandle(BlockState s) { return nameOf(s).contains("CANDLE"); }

	public static boolean isDripleaf(BlockState s) { return nameOf(s).contains("DRIP") && nameOf(s).contains("LEAF"); }

	public static boolean isRespawnAnchor(BlockState s) { return nameOf(s).contains("ANCHOR"); }

	public static boolean isGlowstone(BlockState s) { return nameOf(s).equals("GLOWSTONE"); }
}
