package dev.meshac.mixin;

import dev.meshac.data.PlayerData;
import dev.meshac.data.PlayerDataManager;
import dev.meshac.event.MeshBlock;
import dev.meshac.event.MeshBlockBreakEvent;
import dev.meshac.event.MeshBlockPlaceEvent;
import dev.meshac.packet.PacketDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Server-side block break/place feeds, replacing Bukkit's BlockBreakEvent/BlockPlaceEvent.
 * PARITY: upstream events were cancellable by protection plugins; here nothing cancels them, so
 * isCancelled() is always false for checks (noted in PARITY.md). Place position approximation:
 * inside-block hits place at the hit pos, otherwise at the face offset - BlockItem edge cases
 * (replacable blocks) may differ by one position; flagged for compile-stage review.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

	@Shadow protected ServerPlayer player;

	@Inject(method = "destroyBlock", at = @At("RETURN"))
	private void meshac$destroy(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue()) return;
		PlayerData data = PlayerDataManager.get(player);
		if (data == null) return;
		PacketDispatcher.dispatchEvents(data, new MeshBlockBreakEvent(player, new MeshBlock(player.level(), pos)));
	}

	@Inject(method = "useItemOn", at = @At("RETURN"))
	private void meshac$useOn(ServerPlayer serverPlayer, net.minecraft.world.level.Level level, ItemStack stack,
			InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<?> cir) {
		PlayerData data = PlayerDataManager.get(serverPlayer);
		if (data == null) return;
		if (!(stack.getItem() instanceof BlockItem)) return;
		BlockPos placed = hit.isInside() ? hit.getBlockPos() : hit.getBlockPos().relative(hit.getDirection());
		PacketDispatcher.dispatchEvents(data, new MeshBlockPlaceEvent(serverPlayer,
				new MeshBlock(serverPlayer.level(), placed),
				new MeshBlock(serverPlayer.level(), hit.getBlockPos()),
				hand, stack));
	}
}
