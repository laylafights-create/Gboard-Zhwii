package dev.jason.gboardpatches.extension.keypresssound;

import android.content.SharedPreferences;

public final class GboardKeypressSoundSettings {
    public static final String ENABLED = "pref_keypress_sound_enabled";
    public static final boolean DEFAULT_ENABLED = false;

    private GboardKeypressSoundSettings() {}

    public static boolean enabled(SharedPreferences preferences) {
        if (preferences == null) return DEFAULT_ENABLED;
        Object value = preferences.getAll().get(ENABLED);
        return Boolean.TRUE.equals(value) || "true".equals(value);
    }
}
