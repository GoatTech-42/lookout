package dev.meshac.event;

/** Base type for the server events checks consume, replacing org.bukkit.event.Event. */
public abstract class MeshEvent {

	private boolean cancelled;

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
}
