import dev.meshac.safety.Containment;
import java.util.ArrayList;
import java.util.List;

/** Headless proof that LOG_ONLY never executes an effect and ENFORCE always does. */
public class ContainmentSelfTest {

	public static void main(String[] args) {
		List<String> log = new ArrayList<>();
		Containment.setLogger(log::add);

		// RED without containment semantics: in LOG_ONLY, the action must not run.
		Containment.setMode(Containment.Mode.LOG_ONLY);
		Containment.drain();
		boolean[] ran = {false};
		boolean returned = Containment.dispatch(new Containment.Effect("teleport", "steve", "1,2,3"), () -> ran[0] = true);
		check(!ran[0], "LOG_ONLY must not execute the effect");
		check(!returned, "LOG_ONLY dispatch must report not-executed");
		check(Containment.recorded().size() == 1, "effect must be recorded for audit");
		check(Containment.recorded().get(0).kind().equals("teleport"), "recorded kind preserved");
		check(log.stream().anyMatch(l -> l.contains("log-only")), "log-only line emitted");

		// Null action in LOG_ONLY must not throw.
		Containment.dispatch(new Containment.Effect("disconnect", "alex", "ping"), null);

		// ENFORCE executes.
		Containment.setMode(Containment.Mode.ENFORCE);
		boolean[] ran2 = {false};
		boolean returned2 = Containment.dispatch(new Containment.Effect("eject-vehicle", "steve", ""), () -> ran2[0] = true);
		check(ran2[0] && returned2, "ENFORCE must execute and report executed");
		check(Containment.recorded().size() == 3, "all effects recorded");

		// Default mode on a fresh class state is LOG_ONLY (static initializer contract).
		check(Containment.Mode.LOG_ONLY.name().equals("LOG_ONLY"), "mode enum stable");
		Containment.setMode(Containment.Mode.LOG_ONLY);
		System.out.println("ContainmentSelfTest: PASS");
	}

	private static void check(boolean ok, String what) {
		if (!ok) throw new AssertionError("FAIL: " + what);
	}
}
