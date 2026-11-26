package dev.jason.gboardpatches.patches.gboard.features.keypresssound

import app.morphe.patcher.patch.resourcePatch
import dev.jason.gboardpatches.patches.gboard.features.featureflags.applyFeatureMarker
import dev.jason.gboardpatches.patches.shared.Constants.COMPATIBILITY_GBOARD

internal val gboardKeypressSoundFeatureMarkerPatch = resourcePatch(
    description = "標記按鍵音效 feature 已被打入 target APK，共用 settings UI 過濾。",
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    finalize {
        applyFeatureMarker(KEYPRESS_SOUND_FEATURE_MARKER_NAME)
    }
}

private const val KEYPRESS_SOUND_FEATURE_MARKER_NAME =
    "dev.jason.gboardpatches.feature.keypress_sound"
