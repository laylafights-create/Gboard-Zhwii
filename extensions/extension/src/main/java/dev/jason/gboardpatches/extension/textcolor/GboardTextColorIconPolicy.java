package dev.jason.gboardpatches.extension.textcolor;

/** Pure rules deciding which images may be recolored. */
final class GboardTextColorIconPolicy {
    private static final int MIN_ALPHA = 128;
    private static final int CHANNEL_TOLERANCE = 48;
    /** A filled circle covers ~0.785 and a photo or rounded card even more. */
    private static final float MAX_COVERAGE = 0.70f;

    private GboardTextColorIconPolicy() { }

    /** True only for a single-color shape on transparency; photos and thumbnails fail. */
    static boolean isMonochromeGlyph(int[] argb) {
        if (argb == null || argb.length == 0) return false;
        int solid = 0, minR = 255, minG = 255, minB = 255, maxR = 0, maxG = 0, maxB = 0;
        for (int c : argb) {
            if ((c >>> 24) < MIN_ALPHA) continue;
            solid++;
            int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
            minR = Math.min(minR, r); maxR = Math.max(maxR, r);
            minG = Math.min(minG, g); maxG = Math.max(maxG, g);
            minB = Math.min(minB, b); maxB = Math.max(maxB, b);
        }
        return solid > 0 && solid <= argb.length * MAX_COVERAGE
                && maxR - minR <= CHANNEL_TOLERANCE
                && maxG - minG <= CHANNEL_TOLERANCE
                && maxB - minB <= CHANNEL_TOLERANCE;
    }

    /** Rejects dividers, underlines and edge strips, which are thin or very elongated. */
    static boolean isIconSize(int width, int height, float density) {
        int shortSide = Math.min(width, height), longSide = Math.max(width, height);
        return shortSide >= Math.round(12 * density) && longSide <= Math.round(64 * density)
                && longSide * 2 <= shortSide * 5;
    }
}
