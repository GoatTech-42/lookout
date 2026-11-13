package dev.meshac.check.impl.player.fastbreak;

import dev.meshac.check.AbstractCheck;
import dev.meshac.check.ExemptType;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.config.MeshSettings;
import dev.meshac.data.PlayerData;
import dev.meshac.event.MeshBlock;
import dev.meshac.event.MeshBlockBreakEvent;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshInteractEvent;
import dev.meshac.world.BlockUtils;
import dev.meshac.world.BreakTimeUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

@CheckInfo(name = "Fast Break", type = 'A', complexType = "Delay", description = "Breaking blocks too quickly.")
public class FastBreakA extends AbstractCheck {

	private long breakCompletionAtMillis;

	public FastBreakA(PlayerData playerData) {
		super(playerData);
	}

	@Override
	public void update() {
	}

	@Override
	public void handleEvent(MeshEvent event) {
		if (playerData.getPlayer() == null)
			return;
		if (event instanceof MeshBlockBreakEvent broken)
			checkBlockBreak(broken);
		else if (event instanceof MeshInteractEvent interaction && interaction.getAction() == MeshInteractEvent.Action.LEFT_CLICK_BLOCK)
			setTimestamp(interaction);
	}

	private void setTimestamp(MeshInteractEvent event) {
		MeshBlock block = event.getClickedBlock();
		if (block != null && !BlockUtils.isLiquid(block.getType()))
			breakCompletionAtMillis = System.currentTimeMillis() + BreakTimeUtil.getBlockBreakTime(block, event.getPlayer());
	}

	private void checkBlockBreak(MeshBlockBreakEvent event) {
		if (event.isCancelled() || event.getPlayer().gameMode.getGameModeForPlayer() != GameType.SURVIVAL || isExempt(new ExemptType[] { ExemptType.VEHICLE }) || checkItems(event.getPlayer()))
			return;
		double remainingMillis = System.currentTimeMillis() - breakCompletionAtMillis;
		if (remainingMillis < MeshSettings.checksPlayerFastbreakAMinDifference)
			fail("block=" + event.getBlock().getType() + " diff=" + remainingMillis + " tool=" + event.getPlayer().getMainHandItem().getItem());
		else
			decayBuffer();
	}

	private boolean checkItems(ServerPlayer player) {
		return player.getMainHandItem().getItem().toString().contains("copper");
	}
}
