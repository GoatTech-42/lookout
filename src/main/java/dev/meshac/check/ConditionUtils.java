package dev.meshac.check;

import java.util.Collection;
import java.util.function.Predicate;

/** Collection predicate helpers. Ported from Vulcan's ConditionUtils. */
public final class ConditionUtils {

	private ConditionUtils() {
	}

	public static <T> boolean allMatch(Collection<T> collection, Predicate<T> predicate) {
		if (collection == null) return false;
		for (T element : collection) {
			if (!predicate.test(element)) return false;
		}
		return true;
	}

	public static <T> boolean anyMatch(Collection<T> collection, Predicate<T> predicate) {
		if (collection == null) return false;
		for (T element : collection) {
			if (predicate.test(element)) return true;
		}
		return false;
	}
}
