package dev.jason.gboardpatches.patches.gboard.features.textcolor

import app.morphe.patcher.patch.resourcePatch
import dev.jason.gboardpatches.patches.gboard.features.featureflags.applyFeatureMarker
import dev.jason.gboardpatches.patches.shared.Constants.COMPATIBILITY_GBOARD

internal const val CUSTOM_TEXT_COLOR_FEATURE_MARKER =
    "dev.jason.gboardpatches.feature.custom_text_color"

internal val gboardTextColorFeatureMarkerPatch = resourcePatch(
    description = "標記 Custom Text Color feature 已被打入 target APK，共用 settings UI 過濾。",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    finalize { applyFeatureMarker(CUSTOM_TEXT_COLOR_FEATURE_MARKER) }
}
