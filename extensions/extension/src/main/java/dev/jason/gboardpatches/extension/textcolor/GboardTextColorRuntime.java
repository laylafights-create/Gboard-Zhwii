package dev.jason.gboardpatches.extension.textcolor;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewParent;
import android.widget.TextView;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import dev.jason.gboardpatches.extension.flagsettings.GboardFlagRuntimeContext;

/** Read-only runtime color accessors for the Gboard rendering hook. */
public final class GboardTextColorRuntime {
    public enum Target { KEY, SUGGESTION, TOOLBAR, POPUP }

    private static final GboardTextColorCache CACHE = new GboardTextColorCache();
    private static final int MAX_ANCESTOR_LEVELS = 6;
    private static final Map<View, Target> TARGETS =
            Collections.synchronizedMap(new WeakHashMap<View, Target>());

    private GboardTextColorRuntime() { }

    /**
     * Returns the user's color for {@code target}, or {@code stockColor} when the user has not
     * set one (or storage is unavailable), so an untouched install keeps Gboard's own colors.
     * Cheap enough for the draw path: a cached array lookup, refreshed at most every ~750 ms.
     */
    public static int resolve(Target target, int stockColor) {
        if (target == null) return stockColor;
        try {
            return CACHE.get(target.ordinal(), stockColor,
                    System.nanoTime() / 1_000_000L, GboardTextColorRuntime::load);
        } catch (Throwable ignored) {
            return stockColor;
        }
    }

    /**
     * Draw-path hook for Gboard's key-label text view: called with the color the view was about
     * to draw with, returns the color to draw instead. Any failure returns {@code stockColor}.
     */
    public static int resolveForView(View view, int stockColor) {
        if (view == null) return stockColor;
        try {
            Target target = targetFor(view);
            if (view instanceof TextView
                    && (target == Target.SUGGESTION || target == Target.TOOLBAR)) {
                GboardSuggestionChipTweaks.onTextView((TextView) view);
            }
            if (view instanceof TextView) GboardCustomFont.apply((TextView) view);
            boolean hideFrame = view instanceof TextView && target == Target.SUGGESTION
                    && GboardSuggestionAnimator.onTextView((TextView) view);
            boolean rainbow = rainbowEnabled();
            GboardTextColorIconTinter.sync(view, rainbow);
            int color = colorFor(view, target, rainbow, stockColor);
            // One transparent frame, so the new text does not flash before its entrance starts.
            return hideFrame ? color & 0x00FFFFFF : color;
        } catch (Throwable ignored) {
            return stockColor;
        }
    }

    private static int colorFor(View view, Target target, boolean rainbow, int stockColor) {
        if (rainbow && view instanceof TextView) {
            if (target == Target.KEY) {
                return randomKeyColor((TextView) view, stockColor);
            }
            if (target == Target.SUGGESTION) {
                return GboardTextColorRainbow.colorForSuggestion(
                        ((TextView) view).getText(), stockColor);
            }
        }
        return resolve(target, stockColor);
    }

    /** Rainbow flag rides in the cache snapshot as the slot after the targets (1 = on). */
    private static boolean rainbowEnabled() {
        try {
            return CACHE.get(Target.values().length, 0,
                    System.nanoTime() / 1_000_000L, GboardTextColorRuntime::load) == 1;
        } catch (Throwable ignored) { return false; }
    }

    private static final class Pick {
        final String text;
        final int color;
        Pick(String text, int color) { this.text = text; this.color = color; }
    }

    private static final java.util.Random RANDOM = new java.util.Random();
    private static final Map<View, Pick> PICKS =
            Collections.synchronizedMap(new WeakHashMap<View, Pick>());

    /**
     * Random palette color per key: drawn once for a view and kept while its label is unchanged
     * (no flicker between frames), redrawn when the label changes or the cache is invalidated.
     * Icons keep the stock color; multi-character function keys keep their fixed hues.
     */
    private static int randomKeyColor(TextView view, int stockColor) {
        CharSequence label = view.getText();
        if (label == null || label.length() == 0) return stockColor;
        if (label.length() > 1) return GboardTextColorRainbow.colorFor(label, stockColor);
        String text = label.toString();
        Pick pick = PICKS.get(view);
        if (pick == null || !pick.text.equals(text)) {
            int[] palette = GboardTextColorRainbow.PALETTE;
            pick = new Pick(text, palette[RANDOM.nextInt(palette.length)]);
            PICKS.put(view, pick);
        }
        return pick.color;
    }

    private static Target targetFor(View view) {
        Target known = TARGETS.get(view);
        if (known != null) return known;
        String[] classNames = new String[MAX_ANCESTOR_LEVELS];
        String[] idNames = new String[MAX_ANCESTOR_LEVELS];
        Object node = view;
        int levels = 0;
        while (node instanceof View && levels < MAX_ANCESTOR_LEVELS) {
            View v = (View) node;
            classNames[levels] = v.getClass().getName();
            idNames[levels] = entryNameOrNull(v);
            levels++;
            ViewParent parent = v.getParent();
            node = parent;
        }
        Target target = GboardTextColorTargetClassifier.classify(classNames, idNames);
        // A detached view has no ancestors yet, so its answer is not final.
        if (view.getParent() != null) TARGETS.put(view, target);
        return target;
    }

    private static String entryNameOrNull(View v) {
        int id = v.getId();
        if (id == View.NO_ID) return null;
        try {
            return v.getResources().getResourceEntryName(id);
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    /** Drops cached colors so the next draw sees a value written in this process. */
    static void invalidate() { CACHE.invalidate(); TARGETS.clear(); PICKS.clear(); }

    private static Integer[] load() {
        SharedPreferences p = GboardFlagRuntimeContext.preferencesOrNull();
        if (p == null) return null;
        Integer[] colors = new Integer[Target.values().length + 1];
        colors[Target.KEY.ordinal()] = GboardTextColorSettings.readOrNull(p,
                GboardTextColorSettings.KEY_KEYS);
        colors[Target.SUGGESTION.ordinal()] = GboardTextColorSettings.readOrNull(p,
                GboardTextColorSettings.KEY_SUGGESTIONS);
        colors[Target.TOOLBAR.ordinal()] = GboardTextColorSettings.readOrNull(p,
                GboardTextColorSettings.KEY_TOOLBAR);
        colors[Target.POPUP.ordinal()] = GboardTextColorSettings.readOrNull(p,
                GboardTextColorSettings.KEY_POPUPS);
        colors[Target.values().length] = GboardTextColorSettings.isRainbow(p) ? 1 : 0;
        return colors;
    }
}
