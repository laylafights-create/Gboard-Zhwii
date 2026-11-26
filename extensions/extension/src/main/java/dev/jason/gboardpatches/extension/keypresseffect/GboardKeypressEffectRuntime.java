package dev.jason.gboardpatches.extension.keypresseffect;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import java.util.WeakHashMap;

import dev.jason.gboardpatches.extension.settings.GboardPatchesSettings;

/**
 * Entry point called at the top of the keyboard root view's dispatchTouchEvent. On every finger
 * down it bursts small rainbow light streaks and shows a pad that sinks the pressed key; the
 * pad fades out when the finger comes up. It never throws: a failure switches
 * the feature off for the rest of the process instead of breaking typing.
 */
public final class GboardKeypressEffectRuntime {
    /** The setting is re-read at most once per interval, so toggling it needs no restart. */
    private static final long RECHECK_MS = 1_000L;

    private static volatile boolean enabledValue;
    private static volatile int sinkDepthValue = GboardKeypressEffectSettings.DEFAULT_SINK_DEPTH;
    private static volatile long lastCheckMs = Long.MIN_VALUE / 2;
    private static volatile Throwable failure;

    /** Touch events arrive on the UI thread, so this map needs no locking. */
    private static final WeakHashMap<View, GboardKeypressEffectLayer> LAYERS = new WeakHashMap<>();

    private GboardKeypressEffectRuntime() {}

    public static void onTouchEvent(View view, MotionEvent event) {
        if (view == null || event == null || failure != null) return;
        try {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP
                    || action == MotionEvent.ACTION_CANCEL) {
                GboardKeypressEffectLayer existing = LAYERS.get(view);
                if (existing != null) {
                    if (action == MotionEvent.ACTION_CANCEL) existing.releaseAll();
                    else existing.release(event.getPointerId(event.getActionIndex()));
                }
                return;
            }
            if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_POINTER_DOWN) return;
            Context context = view.getContext();
            if (context == null || !isEnabled(context)) return;
            if (view.getWidth() <= 0 || view.getHeight() <= 0) return;

            GboardKeypressEffectLayer layer = LAYERS.get(view);
            if (layer == null) {
                layer = new GboardKeypressEffectLayer(
                        context.getResources().getDisplayMetrics().density);
                view.getOverlay().add(layer);
                LAYERS.put(view, layer);
            }
            layer.setBounds(0, 0, view.getWidth(), view.getHeight());
            int index = event.getActionIndex();
            layer.setSinkDepth(sinkDepthValue);
            layer.press(event.getPointerId(index), event.getX(index), event.getY(index));
            layer.spawn(event.getX(index), event.getY(index));
        } catch (Throwable error) {
            failure = error;
        }
    }

    /** The error that switched the feature off for this process, or null while it is usable. */
    public static Throwable failure() {
        return failure;
    }

    private static boolean isEnabled(Context context) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastCheckMs >= RECHECK_MS) {
            lastCheckMs = now;
            Boolean read = GboardPatchesSettings.readRuntimeSetting(context,
                    GboardKeypressEffectSettings::enabled, (Boolean) null);
            enabledValue = Boolean.TRUE.equals(read);
            Integer depth = GboardPatchesSettings.readRuntimeSetting(context,
                    GboardKeypressEffectSettings::sinkDepthPercent, (Integer) null);
            sinkDepthValue = depth != null ? depth : GboardKeypressEffectSettings.DEFAULT_SINK_DEPTH;
        }
        return enabledValue;
    }
}
