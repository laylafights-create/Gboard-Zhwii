package dev.jason.gboardpatches.patches.gboard.registry

import dev.jason.gboardpatches.patches.gboard.shared.generated.GboardTargetAdmission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GboardCustomTextColorPatchRegistryTest {
    @Test
    fun customTextColorIsAnIndependentDefaultEnabledPublicPatch() {
        assertEquals("Custom Text Color", gboardCustomTextColorPatch.name)
        assertTrue(gboardCustomTextColorPatch.default)
        assertTrue(GboardPublishedPatchCatalog.morpheRegistrations.contains(gboardCustomTextColorPatch))
        assertEquals(
            setOf("custom_text_color.draw"),
            GboardTargetAdmission.admittedContributionIdsByFeature["custom_text_color"],
        )
    }
}
