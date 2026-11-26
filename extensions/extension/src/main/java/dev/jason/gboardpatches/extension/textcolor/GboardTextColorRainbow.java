package dev.jason.gboardpatches.extension.textcolor;

/** Pure helper: picks a vivid per-label color so every key gets its own hue. */
final class GboardTextColorRainbow {
    static final int[] PALETTE = {
            0xFFFF3B30, 0xFFFF9500, 0xFFFFCC00, 0xFF8BC34A, 0xFF34C759, 0xFF00C7BE,
            0xFF00B0FF, 0xFF2979FF, 0xFF7C4DFF, 0xFFD500F9, 0xFFFF4081, 0xFFFF6E40
    };

    private GboardTextColorRainbow() { }

    /** Stable palette color for an arbitrary seed (used for icons, which have no label). */
    static int colorForSeed(int seed) {
        return PALETTE[Math.floorMod(seed * 5, PALETTE.length)];
    }

    /** Returns {@code stockColor} for empty/null labels (icons), else a stable palette color. */
    static int colorFor(CharSequence label, int stockColor) {
        if (label == null || label.length() == 0) return stockColor;
        // Function keys get their own fixed hues so they do not all come out green.
        if (label.length() > 1) {
            if ("?123".contentEquals(label)) return 0xFFFF4081; // pink
            if (label.length() > 4) return 0xFF2979FF; // space bar language name: blue
        }
        int cp = Character.toLowerCase(Character.codePointAt(label, 0));
        // 5 is coprime with 12, so neighbouring letters land on well-separated hues.
        return PALETTE[Math.floorMod(cp * 5, PALETTE.length)];
    }

    /** Suggestion-strip words: a stable palette color per whole word, so each chip differs. */
    static int colorForSuggestion(CharSequence text, int stockColor) {
        if (text == null || text.length() == 0) return stockColor;
        int h = 0;
        for (int i = 0; i < text.length(); i++) h = h * 31 + Character.toLowerCase(text.charAt(i));
        return PALETTE[Math.floorMod(h, PALETTE.length)];
    }
}
