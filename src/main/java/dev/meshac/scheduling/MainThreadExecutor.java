package dev.meshac.scheduling;

import java.util.Objects;
import dev.meshac.MeshServices;

/**
 * Dispatches Bukkit work to the active server scheduler.
 */
public final class MainThreadExecutor {

    private MainThreadExecutor() {
    }

    public static void execute(Runnable task) {
        Scheduler scheduler = MeshServices.getScheduler();
        if (scheduler == null) {
            throw new IllegalStateException("Vulcan scheduler has not been initialized");
        }
        scheduler.runTask(Objects.requireNonNull(task, "task"));
    }
}
