package dev.jason.gboardpatches.extension.keypresseffect;

import android.content.SharedPreferences;

public final class GboardKeypressEffectSettings {
    public static final String ENABLED = "pref_keypress_effect_enabled";
    public static final boolean DEFAULT_ENABLED = false;

    /** How deep the pressed key looks sunk in, in percent. 100 is the standard look. */
    public static final String SINK_DEPTH = "pref_keypress_effect_sink_depth_percent";
    public static final int DEFAULT_SINK_DEPTH = 100;
    public static final int MIN_SINK_DEPTH = 1;
    public static final int MAX_SINK_DEPTH = 300;

    private GboardKeypressEffectSettings() {}

    public static boolean enabled(SharedPreferences preferences) {
        if (preferences == null) return DEFAULT_ENABLED;
        Object value = preferences.getAll().get(ENABLED);
        return Boolean.TRUE.equals(value) || "true".equals(value);
    }

    public static int sinkDepthPercent(SharedPreferences preferences) {
        if (preferences == null) return DEFAULT_SINK_DEPTH;
        Object value = preferences.getAll().get(SINK_DEPTH);
        try {
            if (value instanceof Number) return clampSinkDepth(((Number) value).intValue());
            if (value instanceof String) return clampSinkDepth(Integer.parseInt(((String) value).trim()));
        } catch (NumberFormatException ignored) {
            // Falls through to the default below.
        }
        return DEFAULT_SINK_DEPTH;
    }

    public static int clampSinkDepth(int value) {
        return Math.max(MIN_SINK_DEPTH, Math.min(MAX_SINK_DEPTH, value));
    }
}
