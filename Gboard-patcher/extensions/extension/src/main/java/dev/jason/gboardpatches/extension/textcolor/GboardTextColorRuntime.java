package dev.jason.gboardpatches.extension.textcolor;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewParent;

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
            return resolve(targetFor(view), stockColor);
        } catch (Throwable ignored) {
            return stockColor;
        }
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
    static void invalidate() { CACHE.invalidate(); TARGETS.clear(); }

    private static Integer[] load() {
        SharedPreferences p = GboardFlagRuntimeContext.preferencesOrNull();
        if (p == null) return null;
        Integer[] colors = new Integer[Target.values().length];
        colors[Target.KEY.ordinal()] = GboardTextColorSettings.readOrNull(p,
                GboardTextColorSettings.KEY_KEYS);
        colors[Target.SUGGESTION.ordinal()] = GboardTextColorSettings.readOrNull(p,
                GboardTextColorSettings.KEY_SUGGESTIONS);
        colors[Target.TOOLBAR.ordinal()] = GboardTextColorSettings.readOrNull(p,
                GboardTextColorSettings.KEY_TOOLBAR);
        colors[Target.POPUP.ordinal()] = GboardTextColorSettings.readOrNull(p,
                GboardTextColorSettings.KEY_POPUPS);
        return colors;
    }
}
