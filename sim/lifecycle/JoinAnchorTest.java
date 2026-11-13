import dev.meshac.data.*;
import dev.meshac.packet.*;
import net.minecraft.network.*;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/** Deterministic queues test our hooks and tracker, not Netty or the Mixin transformer. */
public final class JoinAnchorTest {
    static ServerPlayer player() {
        var p = new ServerPlayer();
        var c = new Connection();
        var game = new ServerGamePacketListenerImpl();
        p.connection = game; game.player = p; game.c = c; c.listener = game;
        return p;
    }
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        var initial = player();
        PlayerDataManager.onJoin(initial);
        check(!PlayerDataManager.get(initial).isHandshakeComplete(), "new session skipped JOIN confirmation");
        PlayerDataManager.onQuit(initial);

        var p = player(); var c = p.connection.c;
        p.connection.send(new ClientboundLoginPacket());
        var data = PlayerDataManager.get(p);
        check(data != null, "login HEAD did not create session before write");
        check(data.getEntityId() == 37 && p.getUUID().equals(data.getUniqueId()) && "fixture".equals(data.getPlayerName()), "early initialization lost identity");
        check(c.writes.isEmpty(), "login escaped deterministic queue");
        check(!data.isHandshakeComplete(), "handshake true before anchor reply");
        int count = dev.meshac.check.manager.CheckManager.count;
        new dev.meshac.Meshac().onInitializeServer();
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.callback.call(p.connection, null, null);
        check(dev.meshac.safety.Effects.bans == 1, "Fabric JOIN reuse skipped inherited-ban containment");
        check(PlayerDataManager.get(p) == data && dev.meshac.check.manager.CheckManager.count == count, "Fabric JOIN replaced early session/checks");
        data.getConfirmationTracker().sendConfirmation(true);
        check(c.ch.loop.tasks.size() == 1, "periodic confirmation scheduled before handshake");
        c.synchronousPong = true;
        c.ch.loop.drain();
        check(c.writes.equals(java.util.List.of("login", "ping")), "login write must precede anchor exactly once");
        check(data.getConfirmationTracker().getJoinConfirmationId() >= -11500 && data.getConfirmationTracker().getJoinConfirmationId() < -11000, "anchor outside upstream range");
        check(data.isHandshakeComplete(), "matching synchronous anchor reply did not finish handshake");
        check(data.getConfirmationTracker().getPendingConfirmations().isEmpty(), "anchor entered periodic confirmation queue");
        // A second login callback must not restart this session or produce another JOIN anchor.
        p.connection.send(new ClientboundLoginPacket()); c.ch.loop.drain();
        check(c.writes.equals(java.util.List.of("login", "ping", "login")), "duplicate login emitted another anchor");
        data.getConfirmationTracker().sendConfirmation(true);
        check(c.writes.size() == 3, "periodic confirmation ran before event-loop task");
        c.ch.loop.drain();
        check(c.writes.size() == 4 && data.getConfirmationTracker().getPendingConfirmations().isEmpty(), "periodic confirmation not observed/answered exactly once");
        c.synchronousPong = false;
        data.getConfirmationTracker().sendConfirmation(true);
        c.ch.encoder.play = false; c.ch.loop.drain();
        check(c.writes.size() == 4, "confirmation leaked into configuration phase");
        c.ch.encoder.play = true;
        data.getConfirmationTracker().sendConfirmation(true); c.connected = false; c.ch.loop.drain();
        check(c.writes.size() == 4, "queued confirmation sent after connection close");

        var q = player(); var qc = q.connection.c;
        q.connection.send(new ClientboundLoginPacket()); qc.ch.encoder.play = false; qc.ch.loop.drain();
        check(qc.writes.equals(java.util.List.of("login")), "anchor sent after phase transfer");
        check(!PlayerDataManager.get(q).isHandshakeComplete(), "phase transfer falsely completed handshake");
        // Old connection's queued login must not anchor a replacement session with the same UUID.
        var replacement = player(); replacement.uuid = q.getUUID();
        qc.ch.encoder.play = true; q.connection.send(new ClientboundLoginPacket());
        PlayerDataManager.onQuit(q); replacement.connection.send(new ClientboundLoginPacket());
        qc.ch.loop.drain();
        check(qc.writes.equals(java.util.List.of("login", "login")), "stale connection anchored replacement session");
        replacement.connection.c.synchronousPong = true; replacement.connection.c.ch.loop.drain();
        check(PlayerDataManager.get(replacement).getPlayer() == replacement && PlayerDataManager.get(replacement).isHandshakeComplete(), "new connection did not get its own anchor");
        // A reentrant login during anchor send cannot produce a second anchor or replace checks.
        var r = player(); var rc = r.connection.c;
        r.connection.send(new ClientboundLoginPacket());
        var rd = PlayerDataManager.get(r);
        rc.onPing = () -> r.connection.send(new ClientboundLoginPacket());
        rc.ch.loop.drain();
        check(rc.writes.equals(java.util.List.of("login", "ping", "login")) && PlayerDataManager.get(r) == rd, "reentrant login duplicated anchor/session");
        PacketDispatcher.onConfirmation(r, 1, MeshPacketType.PONG, null);
        check(!rd.isHandshakeComplete(), "unrelated pong completed handshake");
        PacketDispatcher.onConfirmation(r, rd.getConfirmationTracker().getJoinConfirmationId(), MeshPacketType.PONG, null);
        check(rd.isHandshakeComplete(), "matching pong failed after unrelated pong");
        int beforeInline = rc.writes.size();
        rd.getConfirmationTracker().sendConfirmation(false);
        check(rd.getConfirmationTracker().getPendingConfirmations().size() == 1 && rc.writes.size() == beforeInline, "inline generator changed to deferred generator");
        int inlineId = rd.getConfirmationTracker().getPendingConfirmations().peek().getId();
        rc.ch.loop.drain();
        PacketDispatcher.onConfirmation(r, inlineId, MeshPacketType.PONG, null);
        check(rd.getConfirmationTracker().getPendingConfirmations().isEmpty(), "inline confirmation did not complete");
        final int[] callbacks = {0};
        rd.getConfirmationTracker().runAfterNextConfirmation(() -> callbacks[0]++);
        dev.meshac.config.MeshSettings.connectionOtherAnticheatSupport = true;
        rd.getConfirmationTracker().sendConfirmation(true); rc.ch.loop.drain();
        check(rd.getConfirmationTracker().getPendingConfirmations().size() == 1 && callbacks[0] == 0, "support-mode confirmation queue missing or callback premature");
        int periodic = rd.getConfirmationTracker().getPendingConfirmations().peek().getId();
        PacketDispatcher.onConfirmation(r, periodic, MeshPacketType.PONG, null);
        check(callbacks[0] == 1 && rd.getConfirmationTracker().getPendingConfirmations().isEmpty(), "support-mode callback not completed once");
        dev.meshac.config.MeshSettings.connectionOtherAnticheatSupport = false;
        var closed = player(); closed.connection.send(new ClientboundLoginPacket());
        closed.connection.c.connected = false; closed.connection.c.ch.loop.drain();
        check(closed.connection.c.writes.equals(java.util.List.of("login")), "closed connection emitted JOIN anchor");
        System.out.println("JoinAnchorTest PASS (real session/tracker/dispatch/hooks; deterministic channel boundaries, not weave/runtime)");
    }
}
