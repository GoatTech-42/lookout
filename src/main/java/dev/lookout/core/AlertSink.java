package dev.lookout.core;

/** Where lookout drops its notes. The glue sends these to local chat only. */
public interface AlertSink {
    void alert(int entityId, String playerName, String checkId, String message);
}
