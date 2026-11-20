package dev.lookout.mc;

import dev.lookout.core.AlertSink;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 1.21.11 override of the src/main reference alerter: same log surfaces, chat route is
 * Gui.getChat().addMessage (26.3 uses Gui.chatListener().handleSystemMessage; 26.1.2 has
 * addClientSystemMessage, which 1.21.11's ChatComponent does not carry).
 * build-remap.gradle excludes the src/main copy for this variant. Drops lookout's notes into the local chat feed. Nobody else ever sees them. */
public class ChatAlerter implements AlertSink {

    private static final Logger LOGGER = LoggerFactory.getLogger("lookout");

    // Trace mode adds a marker only; names and messages stay in local chat.
    private static final boolean TRACE = !System.getenv().getOrDefault("MESHAC_TRACE", "").isEmpty();

    @Override
    public void alert(int entityId, String playerName, String checkId, String message) {
        String name = playerName == null || playerName.isBlank() ? "someone" : playerName;
        // shim risk R9: the log line is the evidence surface and must exist even
        // when the chat route is unavailable.
        if (TRACE) {
            LOGGER.info("[lookout] trace alert entity={} check={}", entityId, checkId);
        } else {
            LOGGER.info("[lookout] alert entity={} check={}", entityId, checkId);
        }
        Minecraft client = Minecraft.getInstance();
        if (client.gui == null) return;
        client.gui.getChat().addMessage(
                Component.literal("[lookout] " + name + ": " + message));
    }
}
