package dev.jason.gboardpatches.extension.textcolor;

import android.content.SharedPreferences;
import android.graphics.Color;

/** Persistent text-only color preferences. No background/theme values are stored here. */
public final class GboardTextColorSettings {
    public static final String KEY_KEYS = "pref_text_color_keys";
    public static final String KEY_SUGGESTIONS = "pref_text_color_suggestions";
    public static final String KEY_TOOLBAR = "pref_text_color_toolbar";
    public static final String KEY_POPUPS = "pref_text_color_popups";
    public static final String KEY_RAINBOW = "pref_text_color_rainbow";
    public static final String KEY_ENTER_ANIMATION = "pref_keyboard_enter_animation";
    /** Percent of see-through for the suggestion-strip pill fill: 0 = stock, 100 = invisible. */
    public static final String KEY_CHIP_TRANSPARENCY = "pref_chip_transparency";
    /** Replacement label for the pill; an empty string keeps Gboard's own text. */
    public static final String KEY_CHIP_LABEL = "pref_chip_label";
    public static final String DEFAULT_CHIP_LABEL = "Chọn";
    public static final int MAX_CHIP_LABEL_LENGTH = 20;
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

    public static boolean isRainbow(SharedPreferences p) {
        try { return p != null && p.getBoolean(KEY_RAINBOW, false); } catch (Throwable ignored) { return false; }
    }

    public static boolean setRainbow(SharedPreferences p, boolean on) {
        if (p == null) return false;
        try {
            p.edit().putBoolean(KEY_RAINBOW, on).apply();
            GboardTextColorRuntime.invalidate();
            return true;
        } catch (Throwable ignored) { return false; }
    }

    /** On unless the user turned it off. */
    public static boolean isEnterAnimation(SharedPreferences p) {
        try { return p != null && p.getBoolean(KEY_ENTER_ANIMATION, true); } catch (Throwable ignored) { return false; }
    }

    public static boolean setEnterAnimation(SharedPreferences p, boolean on) {
        if (p == null) return false;
        try {
            p.edit().putBoolean(KEY_ENTER_ANIMATION, on).apply();
            return true;
        } catch (Throwable ignored) { return false; }
    }

    public static int readChipTransparency(SharedPreferences p) {
        if (p == null) return 0;
        try {
            Object raw = p.getAll().get(KEY_CHIP_TRANSPARENCY);
            int value = raw instanceof Number ? ((Number) raw).intValue()
                    : Integer.parseInt(String.valueOf(raw));
            return Math.max(0, Math.min(100, value));
        } catch (Throwable ignored) { return 0; }
    }

    public static boolean writeChipTransparency(SharedPreferences p, int percent) {
        if (p == null) return false;
        try {
            p.edit().putInt(KEY_CHIP_TRANSPARENCY, Math.max(0, Math.min(100, percent))).apply();
            GboardSuggestionChipTweaks.invalidate();
            return true;
        } catch (Throwable ignored) { return false; }
    }

    /** Unset means the default "Chọn"; an explicitly saved empty string means "keep stock". */
    public static String readChipLabel(SharedPreferences p) {
        if (p == null) return DEFAULT_CHIP_LABEL;
        try {
            String value = p.getString(KEY_CHIP_LABEL, null);
            return value == null ? DEFAULT_CHIP_LABEL : value;
        } catch (Throwable ignored) { return DEFAULT_CHIP_LABEL; }
    }

    public static boolean writeChipLabel(SharedPreferences p, String label) {
        if (p == null) return false;
        try {
            String value = label == null ? "" : label.trim();
            if (value.length() > MAX_CHIP_LABEL_LENGTH) value = value.substring(0, MAX_CHIP_LABEL_LENGTH);
            p.edit().putString(KEY_CHIP_LABEL, value).apply();
            GboardSuggestionChipTweaks.invalidate();
            return true;
        } catch (Throwable ignored) { return false; }
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
                    .remove(KEY_TOOLBAR).remove(KEY_POPUPS).remove(KEY_RAINBOW).apply();
            GboardTextColorRuntime.invalidate();
            return true;
        } catch (Throwable ignored) { return false; }
    }
}
