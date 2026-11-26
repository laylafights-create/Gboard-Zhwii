package dev.jason.gboardpatches.extension.textcolor;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Rainbow tint for icon-only keys and toolbar icons. Only small, single-color glyphs are
 * color-filtered, so photos, clipboard images and screenshot thumbnails keep their pixels.
 * All state is touched on the UI thread only. Which Gboard icons are real ImageViews is
 * unverified without the APK.
 */
final class GboardTextColorIconTinter {
    private static final long MIN_INTERVAL_MS = 500L;
    private static final int ROW_DP = 24;
    private static final int SAMPLE_PX = 24;

    /** Tinted view -> the drawable that passed the check (held weakly through the state). */
    private static final Map<ImageView, State> TINTED = new WeakHashMap<>();
    private static final Map<Drawable, Boolean> VERDICTS = new WeakHashMap<>();
    /** Rejected drawables and when to sample them again (they may still be loading or fading). */
    private static final Map<Drawable, Long> REJECTED = new WeakHashMap<>();
    private static final long REJECT_RETRY_MS = 100L;
    private static final Map<View, Boolean> GUARDED_ROOTS = new WeakHashMap<>();
    private static volatile boolean pending;
    private static volatile long lastRunMs;
    private static volatile boolean enabled;
    private static volatile boolean hasTints;

    private static final class State {
        final java.lang.ref.WeakReference<Drawable> drawable;
        final int color;
        final ColorFilter filter;
        final boolean onDrawable;
        State(Drawable d, int color, ColorFilter filter, boolean onDrawable) {
            this.filter = filter;
            this.onDrawable = onDrawable;
            this.drawable = new java.lang.ref.WeakReference<>(d);
            this.color = color;
        }
    }

    private GboardTextColorIconTinter() { }

    /** Called from the draw hook. Mutations are posted, never done in onDraw. */
    static void sync(View from, boolean rainbowOn) {
        if (from == null) return;
        enabled = rainbowOn;
        if (!rainbowOn && !hasTints) return;
        if (pending) return;
        View root = from.getRootView();
        if (root == null) return;
        pending = true;
        // A throttled call is delayed rather than dropped, so the last change is never missed.
        long wait = MIN_INTERVAL_MS - (System.nanoTime() / 1_000_000L - lastRunMs);
        boolean posted = root.postDelayed(() -> {
            try {
                lastRunMs = System.nanoTime() / 1_000_000L;
                if (enabled) {
                    guard(root);
                    refresh(root);
                } else {
                    clearAll();
                }
                hasTints = !TINTED.isEmpty();
            } catch (Throwable ignored) {
            } finally { pending = false; }
        }, Math.max(0L, Math.min(wait, MIN_INTERVAL_MS)));
        if (!posted) pending = false;
    }

    /**
     * Retints before every frame. A keyboard switch can swap an icon's drawable, reset its filter
     * or add new views; fixing that here means no frame shows the untinted icon.
     */
    private static void guard(View root) {
        if (GUARDED_ROOTS.containsKey(root)) return;
        ViewTreeObserver observer = root.getViewTreeObserver();
        if (!observer.isAlive()) return;
        java.lang.ref.WeakReference<View> weakRoot = new java.lang.ref.WeakReference<>(root);
        observer.addOnPreDrawListener(() -> {
            try {
                View current = weakRoot.get();
                if (enabled && current != null) refresh(current);
            } catch (Throwable ignored) { }
            return true;
        });
        GUARDED_ROOTS.put(root, Boolean.TRUE);
    }

    private static void refresh(View root) {
        List<Icon> icons = new ArrayList<>();
        walk(root, icons);
        paint(icons);
        hasTints = !TINTED.isEmpty();
    }

    private static final class Icon {
        final ImageView view;
        final int row, x;
        Icon(ImageView view, int row, int x) { this.view = view; this.row = row; this.x = x; }
    }

    /** Colors icons in reading order, so up to 12 visible icons all get different hues. */
    private static void paint(List<Icon> icons) {
        java.util.Collections.sort(icons, (a, b) ->
                a.row != b.row ? Integer.compare(a.row, b.row) : Integer.compare(a.x, b.x));
        for (int i = 0; i < icons.size(); i++) {
            ImageView img = icons.get(i).view;
            Drawable d = img.getDrawable();
            int color = GboardTextColorRainbow.colorForSeed(i);
            State current = TINTED.get(img);
            if (current != null && current.color == color && current.drawable.get() == d
                    && img.getColorFilter() == current.filter
                    && (!current.onDrawable || d.getColorFilter() == current.filter)) {
                continue; // already correct: do not invalidate, or every frame would redraw
            }
            img.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            ColorFilter applied = img.getColorFilter();
            TINTED.put(img, new State(d, color, applied, d.getColorFilter() == applied));
        }
    }

    private static void walk(View v, List<Icon> icons) {
        if (v.getVisibility() != View.VISIBLE) return;
        if (v instanceof ImageView) collect((ImageView) v, icons);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) walk(g.getChildAt(i), icons);
        }
    }

    private static void collect(ImageView img, List<Icon> icons) {
        Drawable d = img.getDrawable();
        float density = img.getResources().getDisplayMetrics().density;
        if (d == null || !GboardTextColorIconPolicy.isIconSize(img.getWidth(), img.getHeight(),
                density) || !isGlyph(img, d)) {
            untint(img);
            return;
        }
        int[] at = new int[2];
        img.getLocationOnScreen(at);
        int rowPx = Math.max(1, Math.round(ROW_DP * density));
        icons.add(new Icon(img, (at[1] + img.getHeight() / 2) / rowPx, at[0]));
    }

    /** Renders a small sample without our filter. Anything that cannot be sampled is left alone. */
    private static boolean isGlyph(ImageView img, Drawable d) {
        Drawable shown = d.getCurrent();
        Boolean known = VERDICTS.get(shown);
        if (known != null) return known;
        long now = System.nanoTime() / 1_000_000L;
        Long retryAt = REJECTED.get(shown);
        if (retryAt != null && now - retryAt < 0) return false;
        untint(img);
        boolean result = false;
        Rect old = new Rect(d.getBounds());
        Bitmap sample = null;
        try {
            sample = Bitmap.createBitmap(SAMPLE_PX, SAMPLE_PX, Bitmap.Config.ARGB_8888);
            d.setBounds(0, 0, SAMPLE_PX, SAMPLE_PX);
            d.draw(new Canvas(sample));
            int[] pixels = new int[SAMPLE_PX * SAMPLE_PX];
            sample.getPixels(pixels, 0, SAMPLE_PX, 0, 0, SAMPLE_PX, SAMPLE_PX);
            result = GboardTextColorIconPolicy.isMonochromeGlyph(pixels);
        } catch (Throwable ignored) { // e.g. hardware bitmaps cannot be drawn in software
        } finally {
            d.setBounds(old);
            if (sample != null) sample.recycle();
        }
        // A blank sample may only mean "still loading", so only positive results are cached.
        if (result) {
            VERDICTS.put(shown, true);
            REJECTED.remove(shown);
        } else {
            REJECTED.put(shown, now + REJECT_RETRY_MS);
        }
        return result;
    }

    /** Clears the filter only on views this class tinted. */
    private static void untint(ImageView img) {
        if (TINTED.remove(img) != null) img.clearColorFilter();
    }

    private static void clearAll() {
        for (ImageView img : new ArrayList<>(TINTED.keySet())) {
            if (img != null) untint(img);
        }
    }
}
