package dev.jason.gboardpatches.extension.textcolor;

import static dev.jason.gboardpatches.extension.textcolor.GboardTextColorRuntime.Target.KEY;
import static dev.jason.gboardpatches.extension.textcolor.GboardTextColorRuntime.Target.POPUP;
import static dev.jason.gboardpatches.extension.textcolor.GboardTextColorRuntime.Target.SUGGESTION;
import static dev.jason.gboardpatches.extension.textcolor.GboardTextColorRuntime.Target.TOOLBAR;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class GboardTextColorTargetClassifierTest {
    private static final String TEXT_VIEW = "x.AutoCenterScaleTextView";

    @Test public void unknownSurfaceIsKey() {
        assertEquals(KEY, GboardTextColorTargetClassifier.classify(
                new String[] {TEXT_VIEW, "x.SoftKeyView"}, new String[] {null, null}));
        assertEquals(KEY, GboardTextColorTargetClassifier.classify(null, null));
    }

    @Test public void popupAncestorWins() {
        assertEquals(POPUP, GboardTextColorTargetClassifier.classify(
                new String[] {TEXT_VIEW, "x.BasicPopupView"}, new String[] {null, "key_pos_header_x"}));
    }

    @Test public void suggestionAndToolbarIds() {
        assertEquals(SUGGESTION, GboardTextColorTargetClassifier.classify(
                new String[] {TEXT_VIEW, "x.Frame"}, new String[] {null, "key_pos_candidates_1"}));
        assertEquals(SUGGESTION, GboardTextColorTargetClassifier.classify(
                new String[] {TEXT_VIEW}, new String[] {"proactive_suggestions_bar"}));
        assertEquals(TOOLBAR, GboardTextColorTargetClassifier.classify(
                new String[] {TEXT_VIEW, "x.Frame"}, new String[] {null, "key_pos_header_toolbar"}));
    }

    @Test public void knownGboardHolderClassesAndIds() {
        assertEquals(SUGGESTION, GboardTextColorTargetClassifier.classify(
                new String[] {TEXT_VIEW, "com.google.android.apps.inputmethod.libs.proactivesuggestion"
                        + ".ProactiveSuggestionsHolderView"}, new String[] {null, null}));
        assertEquals(SUGGESTION, GboardTextColorTargetClassifier.classify(
                new String[] {TEXT_VIEW, "x.CandidatesHolderView"}, new String[] {null, null}));
        assertEquals(SUGGESTION, GboardTextColorTargetClassifier.classify(
                new String[] {TEXT_VIEW, "x.Frame"}, new String[] {null, "softkey_holder_fixed_candidates"}));
        assertEquals(SUGGESTION, GboardTextColorTargetClassifier.classify(
                new String[] {TEXT_VIEW, "x.Frame"}, new String[] {null, "key_pos_proactive_suggestions"}));
        assertEquals(TOOLBAR, GboardTextColorTargetClassifier.classify(
                new String[] {TEXT_VIEW, "com.google.android.libraries.inputmethod.accesspoint.widget"
                        + ".AccessPointsBarItemSoftKeyView"}, new String[] {null, null}));
        assertEquals(TOOLBAR, GboardTextColorTargetClassifier.classify(
                new String[] {TEXT_VIEW, "x.SoftKeyView", "com.google.android.libraries.inputmethod"
                        + ".accesspoint.widget.AccessPointsBar"}, new String[] {null, null, null}));
    }

    @Test public void nearestLevelWins() {
        assertEquals(SUGGESTION, GboardTextColorTargetClassifier.classify(
                new String[] {TEXT_VIEW, "x.Frame"},
                new String[] {"key_pos_candidates_0", "key_pos_header_0"}));
    }
}
