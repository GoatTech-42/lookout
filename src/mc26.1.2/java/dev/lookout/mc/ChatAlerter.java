package dev.lookout.mc;

import dev.lookout.core.AlertSink;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** 26.1.2 capture adapter of the src/main reference alerter: same message, posted via
 * Gui.getChat().addClientSystemMessage (26.3 uses Gui.chatListener().handleSystemMessage).
 * Drops lookout's notes into the local chat feed. Nobody else ever sees them. */
public class ChatAlerter implements AlertSink {

    @Override
    public void alert(int entityId, String playerName, String checkId, String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui == null) return;
        String name = playerName == null || playerName.isBlank() ? "someone" : playerName;
        client.gui.getChat().addClientSystemMessage(
                Component.literal("[lookout] " + name + ": " + message));
    }
}
