package us.cuongpnh.study.localbroadcastmanager.util;

import java.lang.reflect.Type;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

public class Utils {
	private static Context mContext = null;
	public static String PREFS_SONG_LIST = "song_list";
	public static String PREFS_SONG_INDEX = "song_index";
	public static String PREFS_SONG_SHUFFLE = "song_shuffle";
	public static String PREFS_SONG_REPEAT = "song_repeat";

	public static void init(Context context) {
		setContext(context);
	}

	public static Context getContext() {
		return mContext;
	}

	public static void setContext(Context mContext) {
		Utils.mContext = mContext;
	}

	public static void storeObject(Context context, String key, Object value) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		Editor editor = prefs.edit();
		editor.putString(key, toJson(value));
		editor.commit();
	}

	public static void storeString(Context context, String key, String value) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		Editor editor = prefs.edit();
		editor.putString(key, value);
		editor.commit();
	}

	public static void storeInt(Context context, String key, int value) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		Editor editor = prefs.edit();
		editor.putInt(key, value);
		editor.commit();
	}

	public static void storeBoolean(Context context, String key, boolean val) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		Editor editor = prefs.edit();
		editor.putBoolean(key, val);
		editor.commit();
	}

	public static String getString(Context context, String key,
			String defaultValue) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		return prefs.getString(key, defaultValue);
	}

	public static int getInt(Context context, String key, int defaultValue) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		return prefs.getInt(key, defaultValue);
	}

	public static boolean getBoolean(Context context, String key,
			boolean defaultValue) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		return prefs.getBoolean(key, defaultValue);
	}

	public static Object getObject(Context context, String key, Type type,
			Object defaultValue) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		String json = prefs.getString(key, null);
		if(json == null || json.isEmpty())
		{
			return defaultValue;
		}
		return fromJson(json, type);
	}

	/**
	 * Clears all data from preferences
	 */
	public static void clearPrefData(Context context) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		Editor editor = prefs.edit();
		editor.clear();
		editor.commit();
	}

	public static String toJson(Object object) {
		if (object == null) {
			return "";
		}
		Gson gson = new Gson();
		return gson.toJson(object);
	}

	public static Object fromJson(String json, Type type) {
		Gson gson = new Gson();
		return gson.fromJson(json, type);

	}
}
