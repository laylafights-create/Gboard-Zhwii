package dev.jason.gboardpatches.extension.textcolor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class GboardTextColorRainbowTest {
    @Test public void emptyLabelKeepsStock() {
        assertEquals(0x11223344, GboardTextColorRainbow.colorFor("", 0x11223344));
        assertEquals(0x11223344, GboardTextColorRainbow.colorFor(null, 0x11223344));
    }

    @Test public void sameLabelIsStableAndCaseInsensitive() {
        assertEquals(GboardTextColorRainbow.colorFor("a", 0), GboardTextColorRainbow.colorFor("A", 0));
    }

    @Test public void neighbouringLettersDiffer() {
        assertNotEquals(GboardTextColorRainbow.colorFor("q", 0), GboardTextColorRainbow.colorFor("w", 0));
        assertNotEquals(GboardTextColorRainbow.colorFor("a", 0), GboardTextColorRainbow.colorFor("s", 0));
    }

    @Test public void suggestionColorIsStableAndEmptyKeepsStock() {
        assertEquals(GboardTextColorRainbow.colorForSuggestion("Game", 0),
                GboardTextColorRainbow.colorForSuggestion("game", 0));
        assertEquals(0x11223344, GboardTextColorRainbow.colorForSuggestion("", 0x11223344));
    }
}
