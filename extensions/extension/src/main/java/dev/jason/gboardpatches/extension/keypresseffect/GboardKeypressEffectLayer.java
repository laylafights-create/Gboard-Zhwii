package dev.jason.gboardpatches.extension.keypresseffect;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Random;

/**
 * Draws the key-press effect: a few small rainbow-colored light streaks shooting outward from the
 * touch point and fading away, plus a soft dark pad under the finger so the pressed key looks
 * sunk in. There are deliberately no dots or sparkles. The layer lives in the keyboard view's
 * overlay and keeps redrawing itself only while a burst or a pad is still visible.
 */
final class GboardKeypressEffectLayer extends Drawable {
    private static final long DURATION_MS = 520L;
    private static final int MAX_BURSTS = 14;
    private static final int RAY_COUNT = 4;
    private static final long PAD_IN_MS = 60L;
    private static final long PAD_OUT_MS = 110L;
    private static final long PAD_MAX_MS = 3_000L;
    private static final int PAD_ALPHA = 56;

    /** Red, orange, yellow, green, cyan, blue, violet. */
    private static final int[] RAY_COLORS = {
            0xFFFF3B30, 0xFFFF9500, 0xFFFFD60A, 0xFF34C759, 0xFF00C7FF, 0xFF3B5BFF, 0xFFAF52DE,
    };

    private final float density;
    private int sinkDepth = GboardKeypressEffectSettings.DEFAULT_SINK_DEPTH;
    private final Random random = new Random();
    private final ArrayList<Burst> bursts = new ArrayList<>();
    private final ArrayList<Pad> pads = new ArrayList<>();
    private final Paint rayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint padPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF padRect = new RectF();

    GboardKeypressEffectLayer(float density) {
        this.density = density;
        rayPaint.setStyle(Paint.Style.STROKE);
        rayPaint.setStrokeCap(Paint.Cap.BUTT);
        padPaint.setStyle(Paint.Style.FILL);
        padPaint.setColor(0xFF000000);
    }

    /** Starts a burst at the given point, in the host view's coordinates. */
    void spawn(float x, float y) {
        if (bursts.size() >= MAX_BURSTS) bursts.remove(0);
        Burst burst = new Burst();
        burst.x = x;
        burst.y = y;
        burst.startMs = SystemClock.uptimeMillis();

        float rayBase = random.nextFloat() * (float) (Math.PI * 2);
        burst.rays = new Ray[RAY_COUNT];
        int colorStart = random.nextInt(RAY_COLORS.length);
        for (int i = 0; i < RAY_COUNT; i++) {
            Ray ray = new Ray();
            ray.angle = rayBase + i * (float) (Math.PI * 2 / RAY_COUNT)
                    + (random.nextFloat() - 0.5f) * 0.6f;
            ray.reach = dp(11f + random.nextFloat() * 7f);
            ray.length = dp(4f + random.nextFloat() * 4f);
            ray.width = dp(0.9f + random.nextFloat() * 0.5f);
            ray.color = RAY_COLORS[(colorStart + i * 2) % RAY_COLORS.length];
            burst.rays[i] = ray;
        }

        bursts.add(burst);
        invalidateSelf();
    }

    /** Sets how deep the pressed key looks sunk in, in percent (100 is the standard look). */
    void setSinkDepth(int percent) {
        sinkDepth = GboardKeypressEffectSettings.clampSinkDepth(percent);
    }

    /** A finger went down: shows the pad that makes the pressed key look sunk in. */
    void press(int pointerId, float x, float y) {
        release(pointerId);
        Pad pad = new Pad();
        pad.id = pointerId;
        pad.x = x;
        pad.y = y;
        pad.downMs = SystemClock.uptimeMillis();
        pad.upMs = -1L;
        pads.add(pad);
        invalidateSelf();
    }

    /** The finger came up: the pad fades out. */
    void release(int pointerId) {
        long now = SystemClock.uptimeMillis();
        for (Pad pad : pads) {
            if (pad.id == pointerId && pad.upMs < 0L) pad.upMs = now;
        }
        invalidateSelf();
    }

    void releaseAll() {
        long now = SystemClock.uptimeMillis();
        for (Pad pad : pads) {
            if (pad.upMs < 0L) pad.upMs = now;
        }
        invalidateSelf();
    }

    @Override public void draw(Canvas canvas) {
        long now = SystemClock.uptimeMillis();
        drawPads(canvas, now);
        for (int i = bursts.size() - 1; i >= 0; i--) {
            Burst burst = bursts.get(i);
            float t = (now - burst.startMs) / (float) DURATION_MS;
            if (t >= 1f) {
                bursts.remove(i);
                continue;
            }
            if (t < 0f) t = 0f;
            drawRays(canvas, burst, t);
        }
        if (!bursts.isEmpty() || !pads.isEmpty()) invalidateSelf();
    }

    private void drawPads(Canvas canvas, long now) {
        float depth = sinkDepth / 100f;
        float inset = Math.max(0.6f, 1f - 0.07f * depth);
        int maxAlpha = Math.min(255, Math.round(PAD_ALPHA * depth));
        float halfW = dp(38f) * inset / 2f;
        float halfH = dp(46f) * inset / 2f;
        for (int i = pads.size() - 1; i >= 0; i--) {
            Pad pad = pads.get(i);
            if (pad.upMs < 0L && now - pad.downMs > PAD_MAX_MS) pad.upMs = now;
            float in = Math.min(1f, (now - pad.downMs) / (float) PAD_IN_MS);
            float out = pad.upMs < 0L ? 1f : 1f - (now - pad.upMs) / (float) PAD_OUT_MS;
            if (out <= 0f) {
                pads.remove(i);
                continue;
            }
            padPaint.setAlpha(Math.round(maxAlpha * in * out));
            padRect.set(pad.x - halfW, pad.y - halfH, pad.x + halfW, pad.y + halfH);
            canvas.drawRoundRect(padRect, dp(7f), dp(7f), padPaint);
        }
    }

    private void drawRays(Canvas canvas, Burst burst, float t) {
        float outEase = easeOut(t);
        float inEase = t * t;
        float alpha = 1f - t;
        for (Ray ray : burst.rays) {
            float outer = dp(3f) + (ray.reach + ray.length) * outEase;
            float inner = Math.max(dp(2f), outer - ray.length * (1f - inEase * 0.6f));
            float cos = (float) Math.cos(ray.angle);
            float sin = (float) Math.sin(ray.angle);
            rayPaint.setColor(ray.color);
            rayPaint.setAlpha(Math.round(255f * alpha));
            rayPaint.setStrokeWidth(ray.width * (1f - 0.5f * t));
            canvas.drawLine(burst.x + cos * inner, burst.y + sin * inner,
                    burst.x + cos * outer, burst.y + sin * outer, rayPaint);
        }
    }

    private static float easeOut(float t) {
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    private float dp(float value) {
        return value * density;
    }

    @Override public void setAlpha(int alpha) {}

    @Override public void setColorFilter(ColorFilter colorFilter) {}

    @Override public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    private static final class Burst {
        float x;
        float y;
        long startMs;
        Ray[] rays;
    }

    private static final class Pad {
        int id;
        float x;
        float y;
        long downMs;
        long upMs;
    }

    private static final class Ray {
        float angle;
        float reach;
        float length;
        float width;
        int color;
    }
}
