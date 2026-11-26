package dev.jason.gboardpatches.extension.textcolor;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Collections;

import dev.jason.gboardpatches.extension.settings.GboardPatchesFeatureAvailability;
import dev.jason.gboardpatches.extension.settings.GboardPatchesSettings;
import dev.jason.gboardpatches.extension.settings.GboardPatchesSettingsContract;

/** Settings UI for independently persisted Gboard text color groups. */
public final class GboardTextColorSettingsFeature implements GboardPatchesSettingsContract.Feature {
    private final Context appContext;
    public GboardTextColorSettingsFeature(Context context) {
        Context app = context == null ? null : context.getApplicationContext();
        appContext = app != null ? app : context;
    }
    @Override public String getEntryTitle() { return "Custom Text Color"; }
    @Override public String getEntrySummary() {
        return "Customize key, suggestion, toolbar and popup text colors without changing backgrounds.";
    }
    @Override public boolean isAvailable(Context context) {
        return GboardPatchesFeatureAvailability.hasFeature(context,
                GboardPatchesFeatureAvailability.FEATURE_CUSTOM_TEXT_COLOR);
    }

    @Override public GboardPatchesSettingsContract.Screen buildScreen(
            GboardPatchesSettingsContract.FeatureHost host) {
        Context context = host.getContext();
        SharedPreferences prefs = GboardPatchesSettings.preferencesOrNull(context);
        return new GboardPatchesSettingsContract.Screen(
                getEntryTitle(), "", getEntryTitle(), getEntrySummary(),
                Collections.emptyList(),
                Collections.singletonList(new GboardPatchesSettingsContract.Section(
                        "Text colors", Collections.singletonList(
                        new GboardPatchesSettingsContract.CustomViewRow(
                                ignored -> createView(context, host, prefs))))),
                GboardPatchesSettingsContract.RefreshPolicy.none(),
                GboardPatchesSettingsContract.PanelStyle.FLAT);
    }

    private View createView(Context context, GboardPatchesSettingsContract.FeatureHost host,
            SharedPreferences prefs) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(context, 16);
        root.setPadding(pad, pad, pad, pad);

        TextView hint = new TextView(context);
        hint.setText("Enter HEX colors (for example #FFFFFF). Each group is saved automatically.");
        hint.setTextColor(0xFF707782);
        root.addView(hint);

        LinearLayout preview = new LinearLayout(context);
        preview.setOrientation(LinearLayout.VERTICAL);
        preview.setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12));
        preview.setBackgroundColor(0xFF25282D); // Preview-only surface; never applied to Gboard.
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        previewLp.topMargin = dp(context, 12);
        root.addView(preview, previewLp);

        String[] labels = {"Key text", "Suggestion text", "Toolbar text", "Popup text"};
        String[] keys = {GboardTextColorSettings.KEY_KEYS, GboardTextColorSettings.KEY_SUGGESTIONS,
                GboardTextColorSettings.KEY_TOOLBAR, GboardTextColorSettings.KEY_POPUPS};
        String[] samples = {"Q     W     E     R     T     Y", "Hello     World     Thanks",
                "Toolbar   \u2699   \u22EE   GIF", "Popup menu   Copy   Paste"};
        TextView[] previews = new TextView[labels.length];
        for (int i = 0; i < previews.length; i++) {
            previews[i] = sample(context, samples[i]);
            preview.addView(previews[i]);
        }
        for (int i = 0; i < previews.length; i++) {
            addColorRow(context, root, host, prefs, labels[i], keys[i],
                    GboardTextColorSettings.DEFAULT_KEYS, previews[i]);
        }

        Button reset = new Button(context);
        reset.setText("Reset all colors");
        reset.setOnClickListener(v -> {
            GboardTextColorSettings.reset(prefs);
            GboardPatchesSettingsContract.refresh(host);
        });
        root.addView(reset);
        return root;
    }

    private void addColorRow(Context context, LinearLayout root,
            GboardPatchesSettingsContract.FeatureHost host, SharedPreferences prefs,
            String label, String key, int fallback, TextView preview) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(context);
        title.setText(label);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(0xFF202124);
        row.addView(title, new LinearLayout.LayoutParams(0, dp(context, 52), 1f));
        Button edit = new Button(context);
        edit.setText(buttonText(prefs, key, fallback));
        edit.setOnClickListener(v -> GboardPatchesSettingsContract.showTextInputDialog(
                host, label, "HEX, e.g. #FFFFFF or #AARRGGBB",
                GboardTextColorSettings.isSet(prefs, key)
                        ? GboardTextColorSettings.hex(prefs, key, fallback) : "",
                value -> {
                    if (GboardTextColorSettings.write(prefs, key, value)) {
                        preview.setTextColor(GboardTextColorSettings.read(prefs, key, fallback));
                        edit.setText(buttonText(prefs, key, fallback));
                    } else {
                        GboardPatchesSettingsContract.showMessage(host,
                                "Invalid HEX. Use #RRGGBB or #AARRGGBB.");
                    }
                }));
        row.addView(edit);
        root.addView(row);
        preview.setTextColor(GboardTextColorSettings.read(prefs, key, fallback));
    }

    private static String buttonText(SharedPreferences prefs, String key, int fallback) {
        return GboardTextColorSettings.isSet(prefs, key)
                ? GboardTextColorSettings.hex(prefs, key, fallback) : "Stock";
    }

    private static TextView sample(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(15);
        view.setPadding(0, dp(context, 5), 0, dp(context, 5));
        view.setTextColor(Color.WHITE);
        return view;
    }
    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
