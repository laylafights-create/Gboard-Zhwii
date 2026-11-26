package dev.jason.gboardpatches.extension.keypresseffect;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.List;

import dev.jason.gboardpatches.extension.R;
import dev.jason.gboardpatches.extension.settings.GboardPatchesFeatureAvailability;
import dev.jason.gboardpatches.extension.settings.GboardPatchesSettings;
import dev.jason.gboardpatches.extension.settings.GboardPatchesSettingsContract;
import dev.jason.gboardpatches.extension.settings.GboardSettingsText;

public final class GboardKeypressEffectSettingsFeature
        implements GboardPatchesSettingsContract.Feature {
    private final Context context;

    public GboardKeypressEffectSettingsFeature(Context context) {
        this.context = context;
    }

    @Override public String getEntryTitle() {
        return text(R.string.gboard_patches_keypress_effect_title);
    }

    @Override public String getEntrySummary() {
        return text(R.string.gboard_patches_keypress_effect_entry_summary);
    }

    @Override public boolean isAvailable(Context host) {
        return GboardPatchesFeatureAvailability.hasFeature(host,
                GboardPatchesFeatureAvailability.FEATURE_KEYPRESS_EFFECT);
    }

    @Override public GboardPatchesSettingsContract.Screen buildScreen(
            GboardPatchesSettingsContract.FeatureHost host) {
        try {
            Context owner = host.getContext();
            SharedPreferences preferences = GboardPatchesSettings.preferencesOrNull(owner);
            var toggle = new GboardPatchesSettingsContract.ToggleRow(
                    getEntryTitle(), "", preferences != null,
                    GboardKeypressEffectSettings.enabled(preferences),
                    value -> write(preferences, value))
                    .setMasterToggle(true);
            int depth = GboardKeypressEffectSettings.sinkDepthPercent(preferences);
            var depthRow = new GboardPatchesSettingsContract.SelectorRow(
                    text(R.string.gboard_patches_keypress_effect_sink_title),
                    text(R.string.gboard_patches_keypress_effect_sink_summary),
                    depth + "%", preferences != null,
                    () -> GboardPatchesSettingsContract.showPositiveIntegerDialog(host,
                            text(R.string.gboard_patches_keypress_effect_sink_dialog_title),
                            text(R.string.gboard_patches_keypress_effect_sink_dialog_hint),
                            depth, value -> writeDepth(preferences, value)));
            return new GboardPatchesSettingsContract.Screen(
                    getEntryTitle(), text(R.string.gboard_patches_header_badge), getEntryTitle(),
                    text(R.string.gboard_patches_keypress_effect_summary), List.of(),
                    List.of(new GboardPatchesSettingsContract.Section(
                            text(R.string.gboard_patches_flag_patch_section_feature), List.of(toggle, depthRow))),
                    GboardPatchesSettingsContract.RefreshPolicy.none(),
                    GboardPatchesSettingsContract.PanelStyle.FLAT);
        } catch (Throwable failure) {
            try { Log.w("GboardPatches", "Failed to render keypress effect settings", failure); }
            catch (Throwable ignored) { /* The warning screen below is still returned. */ }
            return new GboardPatchesSettingsContract.Screen(
                    getEntryTitle(), text(R.string.gboard_patches_header_badge), getEntryTitle(),
                    text(R.string.gboard_patches_keypress_effect_summary),
                    List.of(new GboardPatchesSettingsContract.StatusBlock(
                            text(R.string.gboard_patches_flag_patch_error_title),
                            text(R.string.gboard_patches_flag_patch_error_summary),
                            GboardPatchesSettingsContract.StatusTone.WARNING)), List.of());
        }
    }

    @SuppressLint("ApplySharedPref")
    private static void write(SharedPreferences preferences, boolean value) {
        if (preferences == null
                || !preferences.edit().putBoolean(GboardKeypressEffectSettings.ENABLED, value).commit()) {
            throw new IllegalStateException("Unable to save keypress effect");
        }
    }

    @SuppressLint("ApplySharedPref")
    private static void writeDepth(SharedPreferences preferences, int value) {
        if (preferences == null || !preferences.edit().putInt(GboardKeypressEffectSettings.SINK_DEPTH,
                GboardKeypressEffectSettings.clampSinkDepth(value)).commit()) {
            throw new IllegalStateException("Unable to save keypress sink depth");
        }
    }

    private String text(int id) {
        return GboardSettingsText.get(context, id);
    }
}
