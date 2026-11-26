package dev.jason.gboardpatches.extension.textcolor;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewParent;
import android.widget.TextView;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import dev.jason.gboardpatches.extension.flagsettings.GboardFlagRuntimeContext;

/**
 * Tweaks for the white pill ("Chụp...") that Gboard shows in the suggestion strip: a custom label
 * and an adjustable fill transparency. The pill is found by its text, so it is only touched while
 * it actually shows that text. Everything is UI-thread only and never throws into Gboard.
 *
 * Which exact view holds the pill fill is unverified without the APK, so the fill is taken from
 * the text view itself or, failing that, the nearest ancestor within a few levels that has one.
 */
final class GboardSuggestionChipTweaks {
    /** Text the stock pill starts with (Vietnamese "Chụp màn hình"; shown ellipsized). */
    private static final String MATCH_PREFIX = "Chụp";
    private static final int MAX_ANCESTOR_LEVELS = 4;
    private static final long TTL_MS = 750L;

    /** Views currently known to be the pill's label. Weak so recycled views are not retained. */
    private static final Map<TextView, Boolean> CHIPS =
            Collections.synchronizedMap(new WeakHashMap<TextView, Boolean>());
    private static final Map<TextView, Long> LAST_POST =
            Collections.synchronizedMap(new WeakHashMap<TextView, Long>());

    private static volatile int transparency;
    private static volatile String label = GboardTextColorSettings.DEFAULT_CHIP_LABEL;
    private static volatile long nextRefreshMs;

    private GboardSuggestionChipTweaks() { }

    static void invalidate() { nextRefreshMs = 0L; }

    /** Draw-path hook: called for text views in the suggestion strip and toolbar. */
    static void onTextView(final TextView view) {
        try {
            refresh();
            CharSequence current = view.getText();
            String wanted = label;
            boolean known = CHIPS.containsKey(view);
            boolean matches = current != null && (current.toString().startsWith(MATCH_PREFIX)
                    || (!wanted.isEmpty() && wanted.contentEquals(current)));
            if (!known) {
                if (current == null || !current.toString().startsWith(MATCH_PREFIX)) return;
                CHIPS.put(view, Boolean.TRUE);
            } else if (!matches) {
                // The view was recycled for another suggestion: put its fill back and let go.
                CHIPS.remove(view);
                setFillAlpha(view, 0);
                return;
            }
            if (!wanted.isEmpty() && !wanted.contentEquals(current)) postLabel(view, wanted);
            setFillAlpha(view, transparency);
        } catch (Throwable ignored) { }
    }

    private static void postLabel(final TextView view, final String wanted) {
        long now = android.os.SystemClock.uptimeMillis();
        Long last = LAST_POST.get(view);
        if (last != null && now - last < 150L) return; // avoid a rebind fight with Gboard
        LAST_POST.put(view, now);
        // Never change text from inside a draw pass.
        view.post(() -> {
            try {
                if (!wanted.contentEquals(view.getText())) view.setText(wanted);
            } catch (Throwable ignored) { }
        });
    }

    /** transparencyPercent: 0 keeps the stock fill, 100 makes it fully see-through. */
    private static void setFillAlpha(View textView, int transparencyPercent) {
        int alpha = 255 - Math.round(transparencyPercent * 255f / 100f);
        View holder = textView;
        for (int level = 0; level <= MAX_ANCESTOR_LEVELS; level++) {
            Drawable background = holder.getBackground();
            if (background != null) {
                if (background.getAlpha() != alpha) {
                    background.mutate();
                    background.setAlpha(alpha);
                    holder.invalidate();
                }
                return;
            }
            ViewParent parent = holder.getParent();
            if (!(parent instanceof View)) return;
            holder = (View) parent;
        }
    }

    private static void refresh() {
        long now = android.os.SystemClock.uptimeMillis();
        if (now < nextRefreshMs) return;
        nextRefreshMs = now + TTL_MS;
        try {
            SharedPreferences p = GboardFlagRuntimeContext.preferencesOrNull();
            if (p == null) return;
            transparency = GboardTextColorSettings.readChipTransparency(p);
            label = GboardTextColorSettings.readChipLabel(p);
        } catch (Throwable ignored) { }
    }
}
