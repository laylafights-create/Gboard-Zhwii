package dev.jason.gboardpatches.extension.textcolor;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import dev.jason.gboardpatches.extension.flagsettings.GboardFlagRuntimeContext;

/**
 * Spring entrance for the keyboard. Every aspect (duration, bounce, speed, start size, slide
 * distance, fade) comes from {@link GboardEnterAnimationSettings}; the defaults reproduce the
 * Dynamic-Island-like curve measured from the reference recording. The same {@link #play} is used
 * by the settings preview, so what you see there is exactly what the keyboard does.
 *
 * Only scale and alpha follow the overshoot. The slide is clamped at its resting place because the
 * keyboard would otherwise lift off the bottom edge and open a gap.
 */
public final class GboardKeyboardEnterAnimation {
    private static final Map<View, ValueAnimator> ACTIVE =
            Collections.synchronizedMap(new WeakHashMap<View, ValueAnimator>());

    private GboardKeyboardEnterAnimation() { }

    /** Damped spring step response: 0 at t=0, then settles on 1 (overshooting when zeta < 1). */
    static final class SpringInterpolator implements TimeInterpolator {
        private final float zeta;
        private final float wn;
        private final float wd;
        private final float seconds;

        SpringInterpolator(float zeta, float naturalFreq, long durationMs) {
            this.zeta = Math.max(0.2f, Math.min(1f, zeta));
            this.wn = Math.max(1f, naturalFreq);
            this.wd = (float) (this.wn * Math.sqrt(Math.max(0.0001, 1f - this.zeta * this.zeta)));
            this.seconds = durationMs / 1000f;
        }

        private float spring(float t) {
            if (zeta >= 0.999f) { // critically damped: no overshoot
                return (float) (1.0 - Math.exp(-wn * t) * (1.0 + wn * t));
            }
            double decay = Math.exp(-zeta * wn * t);
            double k = zeta * wn / wd;
            return (float) (1.0 - decay * (Math.cos(wd * t) + k * Math.sin(wd * t)));
        }

        @Override public float getInterpolation(float input) {
            if (input >= 1f) return 1f;
            float value = spring(input * seconds);
            // Blend the last 15% into exactly 1 so a not-quite-settled spring never snaps.
            if (input > 0.85f) {
                float x = (input - 0.85f) / 0.15f;
                float ease = x * x * (3f - 2f * x);
                value = value + (1f - value) * ease;
            }
            return value;
        }
    }

    /** Called at the start of onWindowShown. Never throws into Gboard. */
    public static void onWindowShown(Object receiver) {
        try {
            if (!(receiver instanceof InputMethodService)) return;
            Dialog dialog = ((InputMethodService) receiver).getWindow();
            Window window = dialog == null ? null : dialog.getWindow();
            final View area = window == null ? null : window.findViewById(android.R.id.inputArea);
            if (area == null) return;
            cancel(area);
            reset(area);
            // Read on every show, so settings take effect without a restart.
            SharedPreferences prefs = GboardFlagRuntimeContext
                    .preferencesOrNull((InputMethodService) receiver);
            if (!GboardTextColorSettings.isEnterAnimation(prefs)) return;
            final GboardEnterAnimationSettings.Params params = GboardEnterAnimationSettings.read(prefs);
            // Wait for the first frame so the keyboard height is known.
            area.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override public boolean onPreDraw() {
                    area.getViewTreeObserver().removeOnPreDrawListener(this);
                    play(area, params);
                    return true;
                }
            });
            area.invalidate();
        } catch (Throwable ignored) { }
    }

    /** Plays the entrance on {@code area} with {@code params}. The view must already be laid out. */
    public static void play(final View area, final GboardEnterAnimationSettings.Params params) {
        try {
            cancel(area);
            reset(area);
            final float height = area.getHeight();
            if (height <= 0f || params == null) return;
            // Pivot on the bottom edge so the keyboard grows upward and no gap opens under it.
            area.setPivotX(area.getWidth() / 2f);
            area.setPivotY(height);
            apply(area, params, 0f, height);

            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(params.durationMs);
            animator.setInterpolator(new SpringInterpolator(params.zeta, params.wn, params.durationMs));
            animator.addUpdateListener(a -> apply(area, params, (Float) a.getAnimatedValue(), height));
            animator.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator a) { ACTIVE.remove(area); reset(area); }
                @Override public void onAnimationCancel(Animator a) { ACTIVE.remove(area); reset(area); }
            });
            ACTIVE.put(area, animator);
            animator.start();
        } catch (Throwable ignored) { reset(area); }
    }

    private static void cancel(View area) {
        area.animate().cancel();
        ValueAnimator running = ACTIVE.remove(area);
        if (running != null) running.cancel();
    }

    /** p is spring progress: 0 at the start, above 1 at an overshoot peak, 1 when settled. */
    private static void apply(View area, GboardEnterAnimationSettings.Params s, float p, float height) {
        area.setScaleX(s.startScaleX + (1f - s.startScaleX) * p);
        area.setScaleY(s.startScaleY + (1f - s.startScaleY) * p);
        area.setTranslationY(height * s.startOffset * (1f - Math.min(p, 1f)));
        area.setAlpha(s.fade ? Math.max(0f, Math.min(1f, p * s.fadeSpeed)) : 1f);
    }

    private static void reset(View area) {
        area.setScaleX(1f);
        area.setScaleY(1f);
        area.setTranslationY(0f);
        area.setAlpha(1f);
    }
}
