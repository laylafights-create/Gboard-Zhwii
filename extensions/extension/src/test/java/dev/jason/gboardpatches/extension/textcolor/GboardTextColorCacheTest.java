package dev.jason.gboardpatches.extension.textcolor;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public final class GboardTextColorCacheTest {
    private static final int STOCK = 0xFF123456;

    @Test public void unsetTargetKeepsStockColor() {
        GboardTextColorCache cache = new GboardTextColorCache();
        assertEquals(STOCK, cache.get(0, STOCK, 0, () -> new Integer[] {null, 1, 2, 3}));
    }

    @Test public void parsesOncePerWindowAndRefreshesAfterTtl() {
        GboardTextColorCache cache = new GboardTextColorCache();
        AtomicInteger loads = new AtomicInteger();
        GboardTextColorCache.Loader loader = () -> {
            loads.incrementAndGet();
            return new Integer[] {0xFFFF0000, null, null, null};
        };
        for (int i = 0; i < 100; i++) assertEquals(0xFFFF0000, cache.get(0, STOCK, i, loader));
        assertEquals(1, loads.get());
        cache.get(0, STOCK, GboardTextColorCache.TTL_MS + 1, loader);
        assertEquals(2, loads.get());
    }

    @Test public void unavailableStorageFallsBackAndRetriesSparingly() {
        GboardTextColorCache cache = new GboardTextColorCache();
        AtomicInteger loads = new AtomicInteger();
        GboardTextColorCache.Loader locked = () -> { loads.incrementAndGet(); return null; };
        assertEquals(STOCK, cache.get(0, STOCK, 10, locked));
        assertEquals(STOCK, cache.get(0, STOCK, 500, locked));
        assertEquals(1, loads.get());
        assertEquals(STOCK, cache.get(0, STOCK, 10 + GboardTextColorCache.RETRY_MS, locked));
        assertEquals(2, loads.get());
    }

    @Test public void invalidateForcesReloadAndLoaderFailureIsContained() {
        GboardTextColorCache cache = new GboardTextColorCache();
        assertEquals(7, cache.get(1, STOCK, 0, () -> new Integer[] {null, 7, null, null}));
        cache.invalidate();
        assertEquals(9, cache.get(1, STOCK, 1, () -> new Integer[] {null, 9, null, null}));
        cache.invalidate();
        assertEquals(STOCK, cache.get(1, STOCK, 2, () -> { throw new IllegalStateException(); }));
        assertEquals(STOCK, cache.get(99, STOCK, 3, () -> new Integer[] {1, 2, 3, 4}));
    }

    @Test public void worksWithNegativeNanoTimeOrigin() {
        GboardTextColorCache cache = new GboardTextColorCache();
        AtomicInteger loads = new AtomicInteger();
        GboardTextColorCache.Loader loader = () -> { loads.incrementAndGet(); return new Integer[] {5, 5, 5, 5}; };
        cache.get(0, STOCK, -5_000, loader);
        cache.get(0, STOCK, -4_900, loader);
        assertEquals(1, loads.get());
    }
}
