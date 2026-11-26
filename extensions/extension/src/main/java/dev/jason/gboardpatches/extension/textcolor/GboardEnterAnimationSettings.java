package dev.jason.gboardpatches.extension.textcolor;

import android.content.SharedPreferences;

/** Persisted tuning for the keyboard entrance animation, plus the built-in presets. */
public final class GboardEnterAnimationSettings {
    public static final String KEY_DURATION = "pref_enter_anim_duration_ms";
    public static final String KEY_DAMPING = "pref_enter_anim_damping";      // percent, 100 = no bounce
    public static final String KEY_FREQ = "pref_enter_anim_freq10";          // rad/s x10
    public static final String KEY_SCALE_X = "pref_enter_anim_scale_x";      // percent at start
    public static final String KEY_SCALE_Y = "pref_enter_anim_scale_y";      // percent at start
    public static final String KEY_OFFSET = "pref_enter_anim_offset";        // percent of height below
    public static final String KEY_FADE = "pref_enter_anim_fade";            // 1 / 0
    public static final String KEY_FADE_SPEED = "pref_enter_anim_fade_speed10";

    private static final String[] KEYS = {KEY_DURATION, KEY_DAMPING, KEY_FREQ, KEY_SCALE_X,
            KEY_SCALE_Y, KEY_OFFSET, KEY_FADE, KEY_FADE_SPEED};
    // Slider limits, in the same order as KEYS.
    static final int[] MIN = {200, 20, 30, 30, 10, 0, 0, 10};
    static final int[] MAX = {1500, 100, 180, 100, 100, 100, 1, 50};

    /** {duration, damping, freq10, scaleX, scaleY, offset, fade, fadeSpeed10} */
    public static final int[] PRESET_DYNAMIC = {900, 56, 71, 85, 45, 35, 1, 25};
    public static final int[] PRESET_SMOOTH = {550, 100, 100, 92, 70, 40, 1, 30};
    public static final int[] PRESET_BOUNCY = {1100, 30, 80, 80, 30, 45, 1, 25};
    public static final int[] PRESET_SLIDE = {500, 100, 120, 100, 100, 100, 0, 25};
    public static final int[] PRESET_FADE = {400, 100, 120, 100, 100, 0, 1, 25};
    public static final int[] DEFAULTS = PRESET_DYNAMIC;

    private GboardEnterAnimationSettings() { }

    /** Immutable snapshot used by the animation. */
    public static final class Params {
        public final long durationMs;
        public final float zeta;       // damping ratio, 0.2 - 1.0
        public final float wn;         // natural frequency, rad/s
        public final float startScaleX;
        public final float startScaleY;
        public final float startOffset; // fraction of height pushed below the bottom edge
        public final boolean fade;
        public final float fadeSpeed;

        Params(int[] v) {
            durationMs = v[0];
            zeta = v[1] / 100f;
            wn = v[2] / 10f;
            startScaleX = v[3] / 100f;
            startScaleY = v[4] / 100f;
            startOffset = v[5] / 100f;
            fade = v[6] != 0;
            fadeSpeed = v[7] / 10f;
        }
    }

    public static Params read(SharedPreferences p) {
        return new Params(values(p));
    }

    public static int[] values(SharedPreferences p) {
        int[] v = new int[KEYS.length];
        for (int i = 0; i < KEYS.length; i++) v[i] = readInt(p, i);
        return v;
    }

    public static int readInt(SharedPreferences p, int index) {
        int fallback = DEFAULTS[index];
        if (p == null) return fallback;
        try {
            Object raw = p.getAll().get(KEYS[index]);
            if (raw == null) return fallback;
            int value;
            if (raw instanceof Boolean) value = ((Boolean) raw) ? 1 : 0;
            else if (raw instanceof Number) value = ((Number) raw).intValue();
            else value = Integer.parseInt(String.valueOf(raw));
            return Math.max(MIN[index], Math.min(MAX[index], value));
        } catch (Throwable ignored) { return fallback; }
    }

    public static boolean write(SharedPreferences p, int index, int value) {
        if (p == null) return false;
        try {
            p.edit().putInt(KEYS[index], Math.max(MIN[index], Math.min(MAX[index], value))).apply();
            return true;
        } catch (Throwable ignored) { return false; }
    }

    public static boolean applyPreset(SharedPreferences p, int[] preset) {
        if (p == null || preset == null || preset.length != KEYS.length) return false;
        try {
            SharedPreferences.Editor e = p.edit();
            for (int i = 0; i < KEYS.length; i++) {
                e.putInt(KEYS[i], Math.max(MIN[i], Math.min(MAX[i], preset[i])));
            }
            e.apply();
            return true;
        } catch (Throwable ignored) { return false; }
    }

    public static boolean reset(SharedPreferences p) {
        return applyPreset(p, DEFAULTS);
    }
}
