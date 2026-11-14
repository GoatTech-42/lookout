package dev.meshac.product;

import com.mojang.brigadier.CommandDispatcher;
import dev.meshac.product.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /mesh for staff: cases, case, pardon, unban, ban, kick, status, watch, monitor, reload. */
public final class MeshCommands {
	public static void register(CommandDispatcher<CommandSourceStack> d) {
		d.register(Commands.literal("mesh").requires(Perm.admin())
			.then(Commands.literal("cases").executes(c -> list(c.getSource(), Cases.recent(10)))
				.then(Commands.argument("player", StringArgumentType.word()).executes(c -> list(c.getSource(), Cases.of(StringArgumentType.getString(c, "player"))))))
			.then(Commands.literal("case").then(Commands.argument("id", StringArgumentType.word()).executes(c -> show(c.getSource(), StringArgumentType.getString(c, "id")))))
			.then(Commands.literal("pardon").then(Commands.argument("id", StringArgumentType.word()).executes(c -> {
				if (simulate(c.getSource(), "pardon")) return 1;
				String pid = StringArgumentType.getString(c, "id");
				Cases.PardonResult pr = Cases.pardon(pid);
				switch (pr) {
					case PARDONED -> c.getSource().sendSuccess(() -> Component.literal("Pardoned."), true);
					case PARDONED_NOT_SAVED -> c.getSource().sendFailure(Component.literal("The case store could not be saved, so the pardon was NOT applied (nothing changed). Fix the store (disk writable?) and retry."));
				case PARDON_UNCERTAIN -> {
					Cases.Case pc = Cases.find(pid);
					if (pc != null && pc.pardoned) c.getSource().sendFailure(Component.literal("The case store write had an UNDETERMINED outcome (a rename was interrupted mid-commit). The pardon is applied in memory but may not be on disk; /mesh reload reconciliation may undo it. No unknown bytes were overwritten."));
					else c.getSource().sendFailure(Component.literal("The case store (or its recovery backup) is unreconciled after an undetermined write, so the pardon was NOT applied (nothing changed). Run /mesh reload to reconcile before removing anything else."));
				}
					case NOT_FOUND -> c.getSource().sendFailure(Component.literal("No such case (or already pardoned)."));
					case AMBIGUOUS -> c.getSource().sendFailure(Component.literal("Case id " + pid + " is shared by several stored cases (duplicate historical id); refusing to pardon by id. Use /mesh case " + pid + " to review them and /mesh unban <player> to lift a ban."));
				}
				return pr == Cases.PardonResult.PARDONED ? 1 : 0; })))
			.then(Commands.literal("unban").then(Commands.argument("player", StringArgumentType.word()).executes(c -> {
				if (simulate(c.getSource(), "unban")) return 1;
				String target = StringArgumentType.getString(c, "player");
				Cases.UnbanResult ur = Cases.unban(target);
				if (ur.ambiguousName()) c.getSource().sendFailure(Component.literal("Several stored players have used the name " + target + " (a recycled name); refusing to lift by name. Run /mesh unban with one of these uuids instead: " + String.join(", ", Cases.uuidsForName(target))));
				else if (!ur.persisted() && ur.lifted() == 0 && (Cases.lastCommitUncertain || Cases.lastBackupUncertain)) c.getSource().sendFailure(Component.literal("The case store (or its recovery backup) is unreconciled after an undetermined write; refusing removals until you run /mesh reload."));
				else if (!ur.persisted() && ur.lifted() > 0) c.getSource().sendFailure(Component.literal("Lifted " + ur.lifted() + " ban(s) in memory, but the store write had an UNDETERMINED outcome; the lift exists only in memory and /mesh reload reconciliation may undo it."));
				else if (!ur.persisted()) c.getSource().sendFailure(Component.literal("The case store could not be saved, so no ban was lifted (nothing changed). Fix the store (disk writable?) and retry."));
				else c.getSource().sendSuccess(() -> Component.literal(ur.lifted() > 0 ? "Lifted " + ur.lifted() + " ban(s)." : "No active ban."), true);
				return ur.lifted(); })))
			.then(Commands.literal("ban").then(Commands.argument("player", StringArgumentType.word())
				.then(Commands.argument("duration", StringArgumentType.word()).then(Commands.argument("reason", StringArgumentType.greedyString()).executes(c -> ban(c.getSource(),
					StringArgumentType.getString(c, "player"), StringArgumentType.getString(c, "duration"), StringArgumentType.getString(c, "reason")))))))
			.then(Commands.literal("status").then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player()).executes(c -> {
				ServerPlayer t = net.minecraft.commands.arguments.EntityArgument.getPlayer(c, "player"); String s = Verdict.status(t);
				c.getSource().sendSuccess(() -> Component.literal(s), false); return 1; })))
			.then(Commands.literal("watch").executes(c -> {
				ServerPlayer me = c.getSource().getPlayer(); if (me == null) { c.getSource().sendFailure(Component.literal("Players only.")); return 0; }
				boolean on = Verdict.WATCH.add(me.getUUID()); if (!on) Verdict.WATCH.remove(me.getUUID());
				c.getSource().sendSuccess(() -> Component.literal(on ? "Watching flags." : "Stopped watching."), false); return 1; }))
			.then(Commands.literal("monitor").executes(c -> { c.getSource().sendSuccess(() -> Component.literal("Monitor mode is " + (Config.get().monitor() ? "on" : "off") + "."), false); return 1; })
				.then(Commands.literal("on").executes(c -> { Config.get().monitorOnly = true; c.getSource().sendSuccess(() -> Component.literal("Monitor mode on: flags are logged; only egregiousChecks-listed checks enforce."), true); return 1; }))
				.then(Commands.literal("off").executes(c -> { Config.get().monitorOnly = false; c.getSource().sendSuccess(() -> Component.literal("Monitor mode off."), true); return 1; })))
			.then(Commands.literal("kick").then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player()).then(Commands.argument("reason", StringArgumentType.greedyString()).executes(c -> {
				ServerPlayer t = net.minecraft.commands.arguments.EntityArgument.getPlayer(c, "player"); String why = StringArgumentType.getString(c, "reason");
				if (simulate(c.getSource(), "kick " + t.getGameProfile().name())) return 1;
				Cases.Case cs = Cases.add(t.getGameProfile().name(), t.getUUID(), "kick", why, c.getSource().getTextName(), 0, List.of("Staff kick")); dev.meshac.safety.Effects.disconnect(t, Screens.kick(why, cs));
				c.getSource().sendSuccess(() -> Component.literal("Kicked " + t.getGameProfile().name() + (cs.persisted ? ", case " + cs.id : ", case " + cs.id + " (warning: the case store could not be saved; the case is recorded in memory only, persists on the next successful save, and is lost on restart otherwise)")), true); return 1; }))))
			.then(Commands.literal("reload").executes(c -> { Config.load(); Config.applyVeil();
				boolean hadCases = !Cases.recent(1).isEmpty();
				Cases.LoadResult lr = Cases.load();
				if (lr == Cases.LoadResult.OK) c.getSource().sendSuccess(() -> Component.literal("meshac reloaded." + (Cases.lastMergedUnsaved > 0 ? " " + Cases.lastMergedUnsaved + " unsaved case(s) were kept in memory (still enforced; they persist on the next successful save)." : "")), true);
				else if (lr == Cases.LoadResult.MISSING) c.getSource().sendSuccess(() -> Component.literal(hadCases ? "meshac reloaded; the case store file is gone, so in-memory cases were cleared (deleting the file plus reloading retires cases)." + (Cases.lastMergedUnsaved > 0 ? " " + Cases.lastMergedUnsaved + " unsaved case(s) were kept in memory." : "") : "meshac reloaded."), true);
				else if (lr == Cases.LoadResult.RECOVERED_FROM_BACKUP) c.getSource().sendSuccess(() -> Component.literal("meshac reloaded; the case store file was unreadable, so cases were recovered from the last good backup (the newest changes - cases, lifts and pardons - may be missing). The unreadable file " + (Cases.lastPreservedCorrupt != null ? "was preserved as " + Cases.lastPreservedCorrupt + "." : "could NOT be preserved; saves are blocked until it can be preserved (is the config directory writable?).")), true);
				else c.getSource().sendFailure(Component.literal("meshac reloaded, but the case store could not be read and no usable backup exists - a single invalid record makes the whole store unreadable; the previous in-memory cases remain in force (at startup that is none: zero historical cases are enforced until repaired). The unreadable file " + (Cases.lastPreservedCorrupt != null ? "was preserved as " + Cases.lastPreservedCorrupt + "." : "could NOT be preserved; saves are blocked until it can be preserved (is the config directory writable?).") + " A failed read does not reconcile an earlier undetermined write: repair the store from the preserved copy (or a good backup), then run /mesh reload again."));
				return lr == Cases.LoadResult.FAILED ? 0 : 1; })));
	}

	private static boolean simulate(CommandSourceStack source, String action) {
		if (dev.meshac.safety.Containment.mode() == dev.meshac.safety.Containment.Mode.ENFORCE) return false;
		dev.meshac.safety.Containment.dispatch(new dev.meshac.safety.Containment.Effect("staff-action", source.getTextName(), action), null);
		source.sendSuccess(() -> Component.literal("[log-only] simulated " + action + ": no case changes or player effects."), false);
		return true;
	}
	private static int list(CommandSourceStack s, List<Cases.Case> cs) {
		if (cs.isEmpty()) { s.sendSuccess(() -> Component.literal("No cases."), false); return 0; }
		for (Cases.Case c : cs) s.sendSuccess(() -> Component.literal(c.id + "  " + c.action + "  " + c.player + "  " + c.reason + (c.pardoned ? "  (pardoned)" : c.lifted ? "  (lifted)" : "") + (c.persisted ? "" : "  (unsaved - in memory only)")), false);
		return cs.size();
	}
	private static int show(CommandSourceStack s, String id) {
		java.util.List<Cases.Case> matches = Cases.findAll(id);
		if (matches.isEmpty()) { s.sendSuccess(() -> Component.literal("No such case."), false); return 0; }
		if (matches.size() > 1) {
			s.sendSuccess(() -> Component.literal(matches.size() + " cases share id " + id + " (duplicate historical id):"), false);
			for (Cases.Case c : matches) s.sendSuccess(() -> Component.literal("  " + c.id + "  " + c.action + "  " + c.player + "  " + c.uuid + (c.pardoned ? "  (pardoned)" : c.lifted ? "  (lifted)" : "")), false);
			s.sendSuccess(() -> Component.literal("Pardon by id is refused for ambiguous ids; use /mesh unban <player> to lift a ban."), false);
			return 0;
		}
		Cases.Case c = matches.get(0);
		s.sendSuccess(() -> Component.literal(c.id + " " + c.action + " " + c.player + "\n" + c.reason + "\nEvidence:\n" + String.join("\n", c.evidence)), false);
		return 1;
	}
	/** Staff ban: minutes 0 = permanent. Works on offline players only if they have joined before (name lookup via the profile cache). */
	/** Duration: 0 = permanent; plain number = minutes; or number plus s, m, h/hr, d, w (case-insensitive). Returns millis, or -1 if invalid. */
	static long parseDuration(String t) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,9})\\s*(s|sec|secs|m|min|mins|h|hr|hrs|d|day|days|w|wk|wks)?").matcher(t.trim().toLowerCase(java.util.Locale.ROOT));
		if (!m.matches()) return -1;
		long n = Long.parseLong(m.group(1)); String u = m.group(2) == null ? "m" : m.group(2);
		long unit = switch (u.charAt(0)) { case 's' -> 1000L; case 'h' -> 3_600_000L; case 'd' -> 86_400_000L; case 'w' -> 604_800_000L; default -> 60_000L; };
		return n * unit;
	}
	private static int ban(CommandSourceStack s, String name, String duration, String reason) {
		long ms = parseDuration(duration);
		if (ms < 0) { s.sendFailure(Component.literal("Usage: /mesh ban <player> <duration> <reason>. Duration: 0 = permanent, or 30s, 10m, 3hr, 1d, 2w (plain number = minutes).")); return 0; }
		int minutes = ms == 0 ? 0 : 1;
		ServerPlayer p = s.getServer().getPlayerList().getPlayerByName(name);
		if (p == null) { s.sendFailure(Component.literal("Player must be online.")); return 0; }
		if (dev.meshac.safety.Containment.mode() != dev.meshac.safety.Containment.Mode.ENFORCE) {
			dev.meshac.safety.Effects.disconnect(p, Component.literal("simulated staff ban: " + reason));
			s.sendSuccess(() -> Component.literal("[log-only] simulated ban of " + name + ": nothing persisted, player not disconnected, no webhook."), true);
			return 1;
		}
		Cases.Case c = Cases.add(p.getGameProfile().name(), p.getUUID(), minutes == 0 ? "ban" : "tempban", reason, s.getTextName(), ms == 0 ? 0 : System.currentTimeMillis() + ms, List.of("staff action"));
		if (dev.meshac.safety.Effects.disconnect(p, Screens.ban(c))) Discord.post(c);
		if (c.persisted) s.sendSuccess(() -> Component.literal("Banned " + name + ", case " + c.id), true);
		else s.sendSuccess(() -> Component.literal("Banned " + name + ", case " + c.id + " (warning: the case store could not be saved; the ban is enforced in memory only - the player WAS removed - persists on the next successful save, and is lost on restart otherwise)"), true);
		return 1;
	}
}

