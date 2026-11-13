package dev.meshac.check.impl.player.badpackets.state;

import dev.meshac.check.AbstractCheck;
import dev.meshac.check.ExemptType;
import dev.meshac.check.ExemptionManager;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.data.PlayerData;
import dev.meshac.packet.MeshPacketEvent;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;

@CheckInfo(name = "Bad Packets", type = 'A', complexType = "Abilities", description = "Spoofed abilities packets.")
public class BadPacketsA extends AbstractCheck {

	public BadPacketsA(PlayerData playerData) {
		super(playerData);
	}

	@Override
	public void handlePacket(MeshPacketEvent packetEvent) {
		if (this.isAbilitiesPacket(packetEvent)) {
			if (this.playerData.getCollisionState().isSetbackPending()) {
				return;
			}
			ServerboundPlayerAbilitiesPacket packet = packetEvent.raw();
			boolean exempt = this.isExempt(new ExemptType[] { ExemptType.DEATH });
			if (this.playerData.getPlayer() == null) {
				return;
			}
			// 26.3 ServerboundPlayerAbilitiesPacket carries only isFlying; the pre-1.9
			// flightAllowed Optional branch of upstream is unreachable on this protocol and dropped.
			if ((!this.playerData.getPlayer().getAbilities().mayfly && !exempt) && (!this.playerData.getCollisionState().isFlightAllowed())) {
				if (ExemptionManager.isAtLeast1_9()) {
					if (this.increaseBuffer() > this.maximumBuffer) {
						this.fail("flying=" + packet.isFlying());
					} else {
						this.decayBuffer();
					}
				}
			}
		}
	}

	@Override
	public void update() {
	}
}
