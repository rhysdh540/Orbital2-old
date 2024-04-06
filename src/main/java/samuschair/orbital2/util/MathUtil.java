package samuschair.orbital2.util;

public final class MathUtil {
	/**
	 * Rounds a float to one decimal place.
	 * @param f the float to round
	 * @return the rounded float
	 */
	public static float round(float f, int places) {
		float factor = (float) Math.pow(10, places);
		return Math.round(f * factor) / factor;
	}

	public static double round(double d, int places) {
		double factor = Math.pow(10, places);
		return Math.round(d * factor) / factor;
	}
}
