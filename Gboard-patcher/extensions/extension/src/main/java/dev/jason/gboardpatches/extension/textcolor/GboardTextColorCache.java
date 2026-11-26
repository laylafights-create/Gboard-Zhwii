package dev.jason.gboardpatches.extension.textcolor;

/**
 * Hot-path cache for the four text colors. The renderer asks for a color on every draw, so the
 * stored HEX strings are parsed once per refresh window instead of once per call. Settings may be
 * written from another process, hence a short TTL rather than a change listener. A failed load
 * (locked or unavailable storage) reports stock colors and is retried sparingly.
 */
final class GboardTextColorCache {
    static final long TTL_MS = 750L;
    static final long RETRY_MS = 1_000L;
    private static final long STALE = Long.MIN_VALUE;

    /** Returns one entry per target (null = unset), or null when storage is unavailable. */
    interface Loader { Integer[] load(); }

    private volatile Integer[] snapshot;
    private volatile long nextRefreshMs = STALE;

    int get(int index, int stockColor, long nowMs, Loader loader) {
        long next = nextRefreshMs;
        if (next == STALE || nowMs - next >= 0) refresh(nowMs, loader);
        Integer[] s = snapshot;
        Integer v = s != null && index >= 0 && index < s.length ? s[index] : null;
        return v != null ? v : stockColor;
    }

    void invalidate() { nextRefreshMs = STALE; }

    private synchronized void refresh(long nowMs, Loader loader) {
        long next = nextRefreshMs;
        if (next != STALE && nowMs - next < 0) return; // another thread already refreshed
        Integer[] loaded = null;
        try { loaded = loader.load(); } catch (Throwable ignored) { }
        snapshot = loaded;
        nextRefreshMs = nowMs + (loaded != null ? TTL_MS : RETRY_MS);
    }
}
