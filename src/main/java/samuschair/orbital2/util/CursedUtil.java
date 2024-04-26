package samuschair.orbital2.util;

public final class CursedUtil {
	private CursedUtil() {}

	/**
	 * Throws a checked exception as an unchecked exception.
	 * @param t the exception to throw
	 * @return never returns, the return type is here so you can write {@code throw asUnchecked(exception);}
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Throwable> RuntimeException asUnchecked(Throwable t) throws T {
		throw (T) t;
	}

	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			throw asUnchecked(e);
		}
	}

	public static Class<?> getCallerClass(int depth) {
		return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.walk(s -> s.skip(depth + 1).findFirst().orElseThrow(() -> new IllegalStateException("No caller found")))
				.getDeclaringClass();
	}

	public static Class<?> getCallerClass() {
		return getCallerClass(1);
	}
}
