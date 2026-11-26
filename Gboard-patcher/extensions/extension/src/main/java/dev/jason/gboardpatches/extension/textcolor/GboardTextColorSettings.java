package dev.jason.gboardpatches.extension.textcolor;

import android.content.SharedPreferences;
import android.graphics.Color;

/** Persistent text-only color preferences. No background/theme values are stored here. */
public final class GboardTextColorSettings {
    public static final String KEY_KEYS = "pref_text_color_keys";
    public static final String KEY_SUGGESTIONS = "pref_text_color_suggestions";
    public static final String KEY_TOOLBAR = "pref_text_color_toolbar";
    public static final String KEY_POPUPS = "pref_text_color_popups";
    public static final int DEFAULT_KEYS = Color.WHITE;
    public static final int DEFAULT_SUGGESTIONS = Color.WHITE;
    public static final int DEFAULT_TOOLBAR = Color.WHITE;
    public static final int DEFAULT_POPUPS = Color.WHITE;

    private GboardTextColorSettings() { }

    public static int read(SharedPreferences p, String key, int fallback) {
        if (p == null) return fallback;
        try {
            String value = p.getString(key, null);
            return value == null ? fallback : Color.parseColor(value);
        } catch (Throwable ignored) { return fallback; }
    }

    /** Returns the saved color, or null when unset/invalid so callers can keep the stock color. */
    public static Integer readOrNull(SharedPreferences p, String key) {
        if (p == null) return null;
        try {
            String value = p.getString(key, null);
            return value == null ? null : Color.parseColor(value);
        } catch (Throwable ignored) { return null; }
    }

    public static boolean isSet(SharedPreferences p, String key) {
        return readOrNull(p, key) != null;
    }

    public static boolean write(SharedPreferences p, String key, String hex) {
        if (p == null || !isValidHex(hex)) return false;
        try {
            p.edit().putString(key, normalize(hex)).apply(); // in-memory immediately; no UI-thread disk write
            GboardTextColorRuntime.invalidate();
            return true;
        } catch (Throwable ignored) { return false; }
    }

    public static boolean isValidHex(String value) {
        if (value == null) return false;
        String v = value.trim();
        if (v.startsWith("#")) v = v.substring(1);
        return v.matches("(?i)[0-9a-f]{6}([0-9a-f]{2})?");
    }

    public static String normalize(String value) {
        String v = value.trim();
        if (!v.startsWith("#")) v = "#" + v;
        if (v.length() == 7) v = "#FF" + v.substring(1);
        return v.toUpperCase(java.util.Locale.ROOT);
    }

    public static String hex(SharedPreferences p, String key, int fallback) {
        return String.format(java.util.Locale.ROOT, "#%08X", read(p, key, fallback));
    }

    public static boolean reset(SharedPreferences p) {
        if (p == null) return false;
        try {
            p.edit().remove(KEY_KEYS).remove(KEY_SUGGESTIONS)
                    .remove(KEY_TOOLBAR).remove(KEY_POPUPS).apply();
            GboardTextColorRuntime.invalidate();
            return true;
        } catch (Throwable ignored) { return false; }
    }
}
