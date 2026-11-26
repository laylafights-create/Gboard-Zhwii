package dev.jason.gboardpatches.extension.textcolor;

/**
 * Maps a text view's surroundings to one of the four color targets. Pure function so it can be
 * unit tested without Android; the runtime feeds it class and resource-entry names collected from
 * the view and its ancestors, nearest first. The first level that matches a rule wins, and
 * anything unrecognised is a key label so an unknown surface never gets a surprising color.
 */
final class GboardTextColorTargetClassifier {
    private GboardTextColorTargetClassifier() { }

    static GboardTextColorRuntime.Target classify(String[] classNames, String[] idNames) {
        int levels = Math.max(classNames == null ? 0 : classNames.length,
                idNames == null ? 0 : idNames.length);
        for (int i = 0; i < levels; i++) {
            String cls = at(classNames, i);
            String id = at(idNames, i);
            if (cls != null && cls.contains("PopupView")) return GboardTextColorRuntime.Target.POPUP;
            // Class names below are the ones the top-row-swipe feature already matches on Gboard 18.5.4.
            if (cls != null) {
                if (cls.endsWith("CandidatesHolderView") || cls.endsWith(".ProactiveSuggestionsHolderView")) {
                    return GboardTextColorRuntime.Target.SUGGESTION;
                }
                if (cls.endsWith(".AccessPointsBar") || cls.endsWith(".AccessPointsBarItemSoftKeyView")) {
                    return GboardTextColorRuntime.Target.TOOLBAR;
                }
            }
            if (id == null) continue;
            if (id.startsWith("key_pos_candidates") || id.contains("proactive_suggestions")
                    || id.contains("candidate")) {
                return GboardTextColorRuntime.Target.SUGGESTION;
            }
            if (id.startsWith("key_pos_header") || id.contains("access_points")
                    || id.contains("toolbar")) {
                return GboardTextColorRuntime.Target.TOOLBAR;
            }
        }
        return GboardTextColorRuntime.Target.KEY;
    }

    private static String at(String[] values, int index) {
        return values != null && index < values.length ? values[index] : null;
    }
}
