package dev.jason.gboardpatches.extension.textcolor;

import android.content.SharedPreferences;

/** Persisted on/off switch, tuning values and presets for the suggestion-strip text animation. */
final class GboardSuggestionAnimationSettings {
    static final String KEY_ENABLED = "pref_suggest_anim_enabled";

    // Indexes into KEYS / MIN / MAX and every values array.
    static final int STYLE = 0, EASING = 1, DURATION = 2, STAGGER = 3, ORDER = 4,
            DISTANCE = 5, SCALE = 6, ANGLE = 7, STRENGTH = 8, FADE = 9, ALPHA = 10, SKIP = 11;

    private static final String[] KEYS = {
            "pref_suggest_anim_style", "pref_suggest_anim_easing",
            "pref_suggest_anim_duration_ms", "pref_suggest_anim_stagger_ms",
            "pref_suggest_anim_order", "pref_suggest_anim_distance",
            "pref_suggest_anim_scale", "pref_suggest_anim_angle",
            "pref_suggest_anim_strength", "pref_suggest_anim_fade",
            "pref_suggest_anim_alpha", "pref_suggest_anim_skip_ms"};
    static final int[] MIN = {0, 0, 60, 0, 0, 0, 10, 0, 0, 0, 0, 0};
    static final int[] MAX = {12, 4, 900, 150, 2, 150, 180, 180, 100, 1, 100, 500};

    static final String[] STYLE_NAMES = {"Mờ dần", "Trượt lên", "Trượt xuống", "Trượt từ trái",
            "Trượt từ phải", "Phóng to", "Bật nảy", "Lật dọc", "Lật ngang", "Xoay",
            "Thu nhỏ vào", "Rơi xuống", "Ngẫu nhiên"};
    static final int STYLE_RANDOM = 12;
    static final String[] EASING_NAMES = {"Chậm dần", "Đều", "Vượt đà", "Nảy", "Lò xo"};
    static final String[] ORDER_NAMES = {"Trái → phải", "Phải → trái", "Giữa → ngoài"};

    // {style, easing, duration, stagger, order, distance, scale, angle, strength, fade, alpha, skip}
    static final int[] DEFAULTS = {1, 0, 220, 30, 0, 60, 60, 90, 40, 1, 0, 60};
    static final String[] PRESET_NAMES = {"Mặc định", "Êm nhẹ", "Bật nảy", "Gợn sóng", "Lật thẻ", "Siêu nhanh"};
    static final int[][] PRESETS = {
            DEFAULTS,
            {0, 0, 300, 0, 0, 0, 100, 0, 0, 1, 20, 60},
            {6, 2, 320, 40, 2, 0, 40, 0, 70, 1, 0, 60},
            {1, 4, 480, 80, 0, 90, 100, 0, 60, 1, 0, 80},
            {7, 0, 340, 60, 0, 0, 100, 90, 0, 1, 20, 80},
            {5, 0, 110, 0, 0, 0, 85, 0, 0, 1, 30, 40}};

    private GboardSuggestionAnimationSettings() { }

    /** On unless the user turned it off. */
    static boolean isEnabled(SharedPreferences p) {
        try { return p != null && p.getBoolean(KEY_ENABLED, true); }
        catch (Throwable ignored) { return false; }
    }

    static void setEnabled(SharedPreferences p, boolean on) {
        if (p == null) return;
        try {
            p.edit().putBoolean(KEY_ENABLED, on).apply();
            GboardSuggestionAnimator.invalidate();
        } catch (Throwable ignored) { }
    }

    static int clamp(int index, int value) {
        return Math.max(MIN[index], Math.min(MAX[index], value));
    }

    static int[] values(SharedPreferences p) {
        int[] out = DEFAULTS.clone();
        if (p == null) return out;
        for (int i = 0; i < KEYS.length; i++) {
            try { out[i] = clamp(i, p.getInt(KEYS[i], DEFAULTS[i])); }
            catch (Throwable ignored) { /* wrong stored type: keep the default */ }
        }
        return out;
    }

    static void write(SharedPreferences p, int index, int value) {
        if (p == null) return;
        try {
            p.edit().putInt(KEYS[index], clamp(index, value)).apply();
            GboardSuggestionAnimator.invalidate();
        } catch (Throwable ignored) { }
    }

    /** Replaces every tuning value; the on/off switch is left alone. */
    static void applyPreset(SharedPreferences p, int[] preset) {
        if (p == null || preset == null || preset.length != KEYS.length) return;
        try {
            SharedPreferences.Editor editor = p.edit();
            for (int i = 0; i < KEYS.length; i++) editor.putInt(KEYS[i], clamp(i, preset[i]));
            editor.apply();
            GboardSuggestionAnimator.invalidate();
        } catch (Throwable ignored) { }
    }
}
