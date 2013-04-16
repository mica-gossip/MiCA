package org.princehouse.mica.base;

import java.util.List;

import org.princehouse.mica.util.Functional;

public enum LogFlag {
	init(0, true), state(1, true), view(2, true), rate(3, true), select(4, true), error(
			5, true), runtime(6, true), user(7, true), merge(8,true), debug(9,true), state_initial(10,true), 
			detectFailure(11,true), gossip(12,true), serialization(13,true);

	public int mask;
	public boolean defaultValue;

	private LogFlag(int i, boolean defaultValue) {
		mask = 1 << i;
		this.defaultValue = defaultValue;
	}

	/**
	 * Set the given flag in a set of flags
	 * 
	 * @param flags
	 * @param flag
	 * @return The updated set of flags
	 */
	public int set(int flags) {
		return LogFlag.set(flags, mask);
	}

	public static int set(int flags, int mask) {
		return flags | mask;
	}

	public int unset(int flags) {
		return LogFlag.unset(flags, mask);
	}

	public static int unset(int flags, int mask) {
		return (flags | mask) ^ mask;
	}

	public boolean test(int flags) {
		return (flags & mask) == mask;
	}

	public boolean test() {
		return test(LogFlag.currentLogMask);
	}

	public static int set(int flags, List<Object> flagNames) {
		return set(flags, maskOf(valuesOf(flagNames)));
	}

	public static int unset(int flags, List<Object> flagNames) {
		return unset(flags, maskOf(valuesOf(flagNames)));
	}

	@SuppressWarnings("unchecked")
	public static int maskOf(Object flags) {

		if (flags instanceof List) {
			int mask = 0;
			for (LogFlag f : valuesOf((List<Object>) flags)) {
				mask = f.set(mask);
			}
			return mask;
		} else if(flags instanceof Integer) {
			return (Integer)flags;
		} else if(flags instanceof LogFlag) {
			return ((LogFlag) flags).mask;
		} else {
			throw new RuntimeException(String.format("don't know how to interpret flags argument %s", flags));
		}
	}

	public static List<LogFlag> valuesOf(List<Object> flagNames) {
		List<LogFlag> temp = Functional.list();
		for (Object s : flagNames) {
			if (s instanceof String) {
				temp.add(LogFlag.valueOf((String) s));
			} else if (s instanceof LogFlag) {
				temp.add((LogFlag) s);
			} else {
				throw new RuntimeException();
			}
		}
		return temp;
	}

	public int setDefault(int flags) {
		if (defaultValue)
			return set(flags);
		else
			return unset(flags);
	}

	public static int defaultMask() {
		int mask = 0;
		for (LogFlag f : LogFlag.values()) {
			mask = f.setDefault(mask);
		}
		return mask;
	}

	private static int currentLogMask = defaultMask();

	public static void setCurrentLogMask(int mask) {
		currentLogMask = mask;
	}

	public static int getCurrentLogMask() {
		return currentLogMask;
	}

	/**
	 * Test a set of flags against the current mask.
	 * 
	 * @param flags
	 * @return True if all given flags are set in the current mask.
	 */
	public static boolean testConjunctive(Object flags) {
		int iflags = maskOf(flags);
		return (getCurrentLogMask() & iflags) == iflags;
	}

}
