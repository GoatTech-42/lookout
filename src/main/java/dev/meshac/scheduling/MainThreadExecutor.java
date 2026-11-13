package dev.meshac.scheduling;

import java.util.Objects;
import dev.meshac.MeshServices;
import net.minecraft.server.MinecraftServer;

/**
 * Dispatches work to the server main thread.
 */
public final class MainThreadExecutor {

    private MainThreadExecutor() {
    }

    public static void execute(Runnable task) {
        MinecraftServer server = MeshServices.server();
        if (server == null) {
            throw new IllegalStateException("Meshac server has not been initialized");
        }
        server.execute(Objects.requireNonNull(task, "task"));
    }
}
