package dev.meshac.punish;

import dev.meshac.MeshServices;
import dev.meshac.check.AbstractCheck;
import dev.meshac.data.PlayerData;

/**
 * meshac heat-ladder (Luke's design, ported from the original meshac). PARITY TODO: port the
 * ladder implementation from the meshac repo (heat ladder v2); until then this runs log-only,
 * which must remain strictly side-effect-free (no kicks, no setbacks, no state mutation beyond
 * the log line).
 */
public class HeatLadder {

	public void apply(AbstractCheck check, PlayerData data) {
		MeshServices.log("[meshac] punishment ladder (log-only): " + data.getPlayerName()
				+ " exceeded " + check.getCheckKey());
	}
}
