package co.onemeter.oneapp.ui;

import static android.util.Log.DEBUG;
import static android.util.Log.INFO;
import static android.util.Log.ERROR;
import static android.util.Log.WARN;

public final class Log {
	
	public static final String TAG = "onemeter";
	public static final int LOG_LEVEL = DEBUG;

	@SuppressWarnings("unused")
	private static boolean isLoggable(int level) {
		return level >= LOG_LEVEL;
	}
	
	private static String toString(Object...objects) {
		StringBuilder sb = new StringBuilder();
		for (Object o : objects) {
			sb.append(o);
		}
		return sb.toString();
	}
	
	public static void i(Object...objects) {
		if (isLoggable(INFO)) {
			android.util.Log.i(TAG, toString(objects));
		}
	}
	
	public static void i(Throwable t, Object...objects) {
		if (isLoggable(INFO)) {
			android.util.Log.i(TAG, toString(objects), t);
		}
	}
	
	public static void d(Object...objects) {
		if (isLoggable(DEBUG)) {
			android.util.Log.d(TAG, toString(objects));
		}
	}
	
	public static void d(Throwable t, Object...objects) {
		if (isLoggable(DEBUG)) {
			android.util.Log.d(TAG, toString(objects), t);
		}
	}
	
	public static void w(Object...objects) {
		if (isLoggable(WARN)) {
			android.util.Log.w(TAG, toString(objects));
		}
	}
	
	public static void w(Throwable t, Object...objects) {
		if (isLoggable(WARN)) {
			android.util.Log.w(TAG, toString(objects), t);
		}
	}
	
	public static void e(Object...objects) {
		if (isLoggable(ERROR)) {
			android.util.Log.e(TAG, toString(objects));
		}
	}
	
	public static void e(Throwable t, Object...objects) {
		if (isLoggable(ERROR)) {
			android.util.Log.e(TAG, toString(objects), t);
		}
	}
	
	public static void f(Object...objects) {
		if (isLoggable(ERROR)) {
			android.util.Log.e(TAG, toString(objects));
			throw new RuntimeException("Fatal error : " + toString(objects));
		}
	}
	
	public static void f(Throwable t, Object...objects) {
		if (isLoggable(ERROR)) {
			android.util.Log.e(TAG, toString(objects), t);
			throw new RuntimeException("Fatal error : " + toString(objects), t);
		}
	}

}
