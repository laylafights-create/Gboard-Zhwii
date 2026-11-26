package dev.jason.gboardpatches.extension.keypresssound;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import dev.jason.gboardpatches.extension.settings.GboardPatchesSettings;

/**
 * Entry point called at the top of the keyboard root view's dispatchTouchEvent. It never throws:
 * a failure switches the feature off for the rest of the process instead of breaking typing.
 */
public final class GboardKeypressSoundRuntime {
    /** The setting is re-read at most once per interval, so toggling it needs no restart. */
    private static final long RECHECK_MS = 1_000L;

    private static volatile boolean enabledValue;
    private static volatile long lastCheckMs = Long.MIN_VALUE / 2;

    private static volatile GboardKeypressSoundPlayer player;
    private static volatile Throwable failure;

    private GboardKeypressSoundRuntime() {}

    public static void onTouchEvent(View view, MotionEvent event) {
        if (view == null || event == null || failure != null) return;
        try {
            int action = event.getActionMasked();
            if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_POINTER_DOWN) return;
            Context context = view.getContext();
            if (context == null || !isEnabled(context)) return;
            GboardKeypressSoundPlayer current = player;
            if (current == null) {
                synchronized (GboardKeypressSoundRuntime.class) {
                    current = player;
                    if (current == null) {
                        current = new GboardKeypressSoundPlayer(context.getApplicationContext());
                        player = current;
                    }
                }
            }
            current.play();
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
                    GboardKeypressSoundSettings::enabled, (Boolean) null);
            enabledValue = Boolean.TRUE.equals(read);
        }
        return enabledValue;
    }
}
