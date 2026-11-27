package dev.jason.gboardpatches.extension.textcolor;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import dev.jason.gboardpatches.extension.flagsettings.GboardFlagRuntimeContext;

import static dev.jason.gboardpatches.extension.textcolor.GboardSuggestionAnimationSettings.*;

/**
 * Animates a suggestion label whenever its text changes. Driven by the existing draw-path hook;
 * view properties are only changed from a posted runnable, never inside the draw pass.
 * UI-thread only and never throws into Gboard.
 */
final class GboardSuggestionAnimator {
    private static final long TTL_MS = 750L;
    private static final Random RANDOM = new Random();

    /** Per-view bookkeeping. Must not reference the view, or the weak key would never clear. */
    private static final class State {
        String text = "";
        long changedAt;
        boolean pending;
        ValueAnimator animator;
    }

    private static final Map<View, State> STATES = new WeakHashMap<>();

    private static volatile boolean enabled;
    private static volatile int[] config = DEFAULTS;
    private static volatile long nextRefreshMs;

    private GboardSuggestionAnimator() { }

    static void invalidate() { nextRefreshMs = 0L; }

    /** @return true when this frame must be drawn invisible because the entrance has not begun. */
    static boolean onTextView(final TextView view) {
        try {
            refresh();
            CharSequence current = view.getText();
            if (current == null) current = "";
            State state = STATES.get(view);
            if (!enabled) {
                if (state != null) {
                    // Forget the view; restore it outside the draw pass.
                    STATES.remove(view);
                    final State old = state;
                    view.post(() -> stop(old));
                }
                return false;
            }
            if (state == null) {
                state = new State();
                STATES.put(view, state);
            }
            if (state.text.contentEquals(current)) return state.pending;
            state.text = current.toString();
            long now = SystemClock.uptimeMillis();
            final int[] values = config;
            boolean tooSoon = state.changedAt != 0L && now - state.changedAt < values[SKIP];
            state.changedAt = now;
            if (state.text.isEmpty() || tooSoon || !view.isAttachedToWindow()) {
                state.pending = false;
                if (state.animator != null) {
                    final State running = state;
                    view.post(() -> stop(running));
                }
                return false;
            }
            state.pending = true;
            final State target = state;
            view.post(() -> {
                try {
                    if (!target.pending) return;
                    target.pending = false;
                    start(view, target, values, delayFor(view, values));
                } catch (Throwable ignored) {
                    stop(target);
                } finally {
                    // Property changes do not re-run onDraw: redraw to replace the hidden frame.
                    view.invalidate();
                }
            });
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Used by the settings preview so it runs exactly the keyboard's animation. */
    static void preview(View view, int[] values, int slot) {
        try {
            State state = STATES.get(view);
            if (state == null) {
                state = new State();
                STATES.put(view, state);
            }
            start(view, state, values, (long) orderIndex(slot, values[ORDER]) * values[STAGGER]);
        } catch (Throwable ignored) { }
    }

    private static long delayFor(View view, int[] values) {
        if (values[STAGGER] == 0) return 0L;
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int width = view.getResources().getDisplayMetrics().widthPixels;
        return (long) orderIndex(slotFor(location[0] + view.getWidth() / 2f, width), values[ORDER])
                * values[STAGGER];
    }

    /** Which third of the screen a label sits in: 0, 1 or 2. */
    static int slotFor(float centerX, int screenWidth) {
        if (screenWidth <= 0) return 0;
        return Math.max(0, Math.min(2, (int) (centerX * 3f / screenWidth)));
    }

    static int orderIndex(int slot, int order) {
        if (order == 1) return 2 - slot;
        if (order == 2) return Math.abs(slot - 1);
        return slot;
    }

    private static void start(final View view, final State state, int[] values, long delayMs) {
        stop(state);
        final int style = values[STYLE] == STYLE_RANDOM ? RANDOM.nextInt(STYLE_RANDOM) : values[STYLE];
        // alpha, tx, ty, sx, sy, rotation, rotationX, rotationY
        final float[] base = {view.getAlpha(), view.getTranslationX(), view.getTranslationY(),
                view.getScaleX(), view.getScaleY(), view.getRotation(), view.getRotationX(),
                view.getRotationY()};
        final int[] snapshot = values.clone();
        final float width = view.getWidth();
        final float height = view.getHeight();

        final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(values[DURATION]);
        animator.setStartDelay(delayMs);
        animator.setInterpolator(interpolator(values));
        // The animator holds the view strongly; State drops the animator on end so the key can clear.
        animator.addUpdateListener(a ->
                apply(view, base, snapshot, style, (Float) a.getAnimatedValue(), width, height));
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) {
                restore(view, base);
                if (state.animator == animator) state.animator = null;
            }
        });
        state.animator = animator;
        apply(view, base, snapshot, style, 0f, width, height); // holds during the stagger delay
        animator.start();
    }

    private static void stop(State state) {
        ValueAnimator running = state.animator;
        state.animator = null;
        if (running != null) running.cancel(); // onAnimationEnd restores the view
    }

    private static TimeInterpolator interpolator(int[] values) {
        switch (values[EASING]) {
            case 1: return new LinearInterpolator();
            case 2: return new OvershootInterpolator(0.5f + values[STRENGTH] * 0.04f);
            case 3: return new BounceInterpolator();
            case 4: return new GboardKeyboardEnterAnimation.SpringInterpolator(
                    1f - values[STRENGTH] * 0.007f, 14f, 600L);
            default: return new DecelerateInterpolator(1f + values[STRENGTH] / 50f);
        }
    }

    /**
     * Offsets of one frame relative to rest: {tx, ty, sx, sy, rotation, rotationX, rotationY}.
     * {@code p} runs 0 to 1 and may pass 1 with overshooting easings. Pure, for unit tests.
     */
    static float[] frame(int[] values, int style, float p, float width, float height) {
        float left = 1f - p;
        float shift = values[DISTANCE] / 100f * left;
        float scale = values[SCALE] / 100f;
        float angle = values[ANGLE] * left;
        float[] f = {0f, 0f, 1f, 1f, 0f, 0f, 0f};
        switch (style) {
            case 1: f[1] = height * shift; break;
            case 2: f[1] = -height * shift; break;
            case 3: f[0] = -width * 0.5f * shift; break;
            case 4: f[0] = width * 0.5f * shift; break;
            case 5: f[2] = f[3] = scale + (1f - scale) * p; break;
            case 6: // pop: grows from the start size and rises a little
                f[2] = f[3] = scale + (1f - scale) * p;
                f[1] = height * 0.25f * left;
                break;
            case 7: f[5] = angle; break;
            case 8: f[6] = angle; break;
            case 9:
                f[4] = -angle * 0.5f;
                f[2] = f[3] = scale + (1f - scale) * p;
                break;
            case 10: { // starts larger than rest and settles down
                float big = 2f - scale;
                f[2] = f[3] = big + (1f - big) * p;
                break;
            }
            case 11: // drop with a squash that relaxes
                f[1] = -height * shift;
                f[3] = 1f + 0.25f * left;
                break;
            default: break; // 0: fade only
        }
        f[2] = Math.max(0f, f[2]);
        f[3] = Math.max(0f, f[3]);
        return f;
    }

    private static void apply(View view, float[] base, int[] values, int style, float p,
            float width, float height) {
        float[] f = frame(values, style, p, width, height);
        view.setTranslationX(base[1] + f[0]);
        view.setTranslationY(base[2] + f[1]);
        view.setScaleX(base[3] * f[2]);
        view.setScaleY(base[4] * f[3]);
        view.setRotation(base[5] + f[4]);
        view.setRotationX(base[6] + f[5]);
        view.setRotationY(base[7] + f[6]);
        // "Fade only" must fade even if the fade switch is off, or it would do nothing.
        float alpha = 1f;
        if (values[FADE] != 0 || style == 0) {
            float from = values[ALPHA] / 100f;
            alpha = Math.max(0f, Math.min(1f, from + (1f - from) * p));
        }
        view.setAlpha(base[0] * alpha);
    }

    private static void restore(View view, float[] base) {
        view.setAlpha(base[0]);
        view.setTranslationX(base[1]);
        view.setTranslationY(base[2]);
        view.setScaleX(base[3]);
        view.setScaleY(base[4]);
        view.setRotation(base[5]);
        view.setRotationX(base[6]);
        view.setRotationY(base[7]);
    }

    private static void refresh() {
        long now = SystemClock.uptimeMillis();
        if (now < nextRefreshMs) return;
        nextRefreshMs = now + TTL_MS;
        try {
            SharedPreferences p = GboardFlagRuntimeContext.preferencesOrNull();
            if (p == null) return;
            enabled = GboardSuggestionAnimationSettings.isEnabled(p);
            config = GboardSuggestionAnimationSettings.values(p);
        } catch (Throwable ignored) { }
    }
}
