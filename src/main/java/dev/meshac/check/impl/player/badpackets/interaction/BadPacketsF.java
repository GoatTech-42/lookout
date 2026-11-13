package dev.meshac.check.impl.player.badpackets.interaction;

import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.data.PlayerData;
import dev.meshac.packet.MeshPacketEvent;

/**
 * UNSUPPORTED on 26.3 (PARITY.md): upstream read analog forward/sideways floats from the Steer
 * Vehicle packet and flagged |value| > 0.98. The 26.3 protocol replaced that packet with
 * ServerboundPlayerInput, which carries boolean input flags only - there are no analog values
 * to threshold. Mapping booleans to +/-1.0 would flag every rider (false positives), so this
 * check is gated inert until an equivalent signal exists. Fail-safe: never flags.
 */
@CheckInfo(name = "Bad Packets", type = 'F', complexType = "Steer Vehicle", description = "Invalid Steer Vehicle packet.")
public class BadPacketsF extends AbstractCheck {

	public BadPacketsF(PlayerData playerData) {
		super(playerData);
	}

	@Override
	public void handlePacket(MeshPacketEvent packetEvent) {
		// inert - see class comment
	}

	@Override
	public void update() {
	}
}
