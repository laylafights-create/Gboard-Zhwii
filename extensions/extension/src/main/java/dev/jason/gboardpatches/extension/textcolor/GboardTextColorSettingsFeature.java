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
    @Override public String getEntryTitle() { return "Màu chữ tùy chỉnh"; }
    @Override public String getEntrySummary() {
        return "Tùy chỉnh màu chữ của phím, gợi ý, thanh công cụ và popup mà không đổi nền.";
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
                        "Màu chữ", Collections.singletonList(
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
        hint.setText("Nhập mã màu HEX (ví dụ #FFFFFF). Mỗi nhóm được lưu tự động.");
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

        String[] labels = {"Chữ phím", "Chữ gợi ý", "Chữ thanh công cụ", "Chữ popup"};
        String[] keys = {GboardTextColorSettings.KEY_KEYS, GboardTextColorSettings.KEY_SUGGESTIONS,
                GboardTextColorSettings.KEY_TOOLBAR, GboardTextColorSettings.KEY_POPUPS};
        String[] samples = {"Q     W     E     R     T     Y", "Xin     chào     cảm ơn",
                "Công cụ   \u2699   \u22EE   GIF", "Menu popup   Sao chép   Dán"};
        TextView[] previews = new TextView[labels.length];
        for (int i = 0; i < previews.length; i++) {
            previews[i] = sample(context, samples[i]);
            preview.addView(previews[i]);
        }
        for (int i = 0; i < previews.length; i++) {
            addColorRow(context, root, host, prefs, labels[i], keys[i],
                    GboardTextColorSettings.DEFAULT_KEYS, previews[i]);
        }

        addRainbowRow(context, root, prefs, previews[0]);
        addFontRow(context, root, host, prefs);
        addAnimationRow(context, root, prefs);
        addEnterAnimationSection(context, root, prefs);
        addChipRows(context, root, host, prefs);

        Button reset = new Button(context);
        reset.setText("Đặt lại tất cả màu");
        reset.setOnClickListener(v -> {
            GboardTextColorSettings.reset(prefs);
            GboardPatchesSettingsContract.refresh(host);
        });
        root.addView(reset);
        return root;
    }

    private void addRainbowRow(Context context, LinearLayout root, SharedPreferences prefs,
            TextView keyPreview) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(context);
        title.setText("Chữ phím cầu vồng");
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(0xFF202124);
        row.addView(title, new LinearLayout.LayoutParams(0, dp(context, 52), 1f));
        Button toggle = new Button(context);
        Runnable apply = () -> {
            boolean on = GboardTextColorSettings.isRainbow(prefs);
            toggle.setText(on ? "Bật" : "Tắt");
            if (on) {
                keyPreview.setText(rainbowSample());
            } else {
                keyPreview.setText("Q     W     E     R     T     Y");
                keyPreview.setTextColor(GboardTextColorSettings.read(prefs,
                        GboardTextColorSettings.KEY_KEYS, GboardTextColorSettings.DEFAULT_KEYS));
            }
        };
        toggle.setOnClickListener(v -> {
            GboardTextColorSettings.setRainbow(prefs, !GboardTextColorSettings.isRainbow(prefs));
            apply.run();
        });
        row.addView(toggle);
        root.addView(row);
        apply.run();
    }

    private void addFontRow(Context context, LinearLayout root,
            GboardPatchesSettingsContract.FeatureHost host, SharedPreferences prefs) {
        TextView title = new TextView(context);
        title.setText("Font bàn phím");
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(0xFF202124);
        root.addView(title);
        final TextView status = new TextView(context);
        status.setTextColor(0xFF707782);
        root.addView(status);
        Runnable refresh = () -> {
            String name = GboardCustomFont.currentName(prefs);
            status.setText(name == null || name.isEmpty()
                    ? "Đang dùng font gốc của Gboard" : "Đang dùng: " + name);
        };
        refresh.run();
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button pick = new Button(context);
        pick.setText("Nhập font (.ttf / .otf)");
        pick.setOnClickListener(v -> GboardPatchesSettingsContract.openBinaryDocument(host,
                new String[]{"font/ttf", "font/otf", "font/sfnt", "application/x-font-ttf",
                        "application/x-font-otf", "application/octet-stream"},
                GboardCustomFont.MAX_BYTES, doc -> {
                    if (doc == null) return;
                    String error = GboardCustomFont.importFont(context, prefs,
                            doc.getDisplayName(), doc.getData());
                    GboardPatchesSettingsContract.showMessage(host,
                            error != null ? error : "Đã nhập font");
                    refresh.run();
                }));
        row.addView(pick);
        Button clear = new Button(context);
        clear.setText("Xoá font");
        clear.setOnClickListener(v -> {
            GboardCustomFont.remove(context, prefs);
            refresh.run();
        });
        row.addView(clear);
        root.addView(row);
    }

    private void addAnimationRow(Context context, LinearLayout root, SharedPreferences prefs) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(context);
        title.setText("Hiệu ứng mở bàn phím");
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(0xFF202124);
        row.addView(title, new LinearLayout.LayoutParams(0, dp(context, 52), 1f));
        Button toggle = new Button(context);
        toggle.setText(GboardTextColorSettings.isEnterAnimation(prefs) ? "Bật" : "Tắt");
        toggle.setOnClickListener(v -> {
            GboardTextColorSettings.setEnterAnimation(prefs,
                    !GboardTextColorSettings.isEnterAnimation(prefs));
            toggle.setText(GboardTextColorSettings.isEnterAnimation(prefs) ? "Bật" : "Tắt");
        });
        row.addView(toggle);
        root.addView(row);
    }

    private void addEnterAnimationSection(Context context, LinearLayout root, SharedPreferences prefs) {
        TextView header = new TextView(context);
        header.setText("Tùy chỉnh hiệu ứng mở bàn phím");
        header.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.setTextSize(16);
        header.setTextColor(0xFF202124);
        header.setPadding(0, dp(context, 24), 0, 0);
        root.addView(header);

        // Live preview: the same animation code the keyboard uses, on a mock keyboard.
        android.widget.FrameLayout stage = new android.widget.FrameLayout(context);
        stage.setBackgroundColor(0xFF25282D);
        LinearLayout.LayoutParams stageLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 150));
        stageLp.topMargin = dp(context, 8);
        TextView mock = new TextView(context);
        mock.setText("\u2328   Bàn phím");
        mock.setTextColor(Color.WHITE);
        mock.setTextSize(18);
        mock.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable mockBg = new android.graphics.drawable.GradientDrawable();
        mockBg.setColor(0xFF3F5A8C);
        mockBg.setCornerRadius(dp(context, 18));
        mock.setBackground(mockBg);
        android.widget.FrameLayout.LayoutParams mockLp = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT, dp(context, 100), Gravity.BOTTOM);
        mockLp.setMargins(dp(context, 8), 0, dp(context, 8), dp(context, 8));
        stage.addView(mock, mockLp);
        root.addView(stage, stageLp);

        Runnable replay = () -> GboardKeyboardEnterAnimation.play(mock,
                GboardEnterAnimationSettings.read(prefs));
        Button preview = new Button(context);
        preview.setText("Xem thử");
        preview.setOnClickListener(v -> replay.run());
        root.addView(preview);

        java.util.List<Runnable> refreshers = new java.util.ArrayList<>();

        // Presets.
        TextView presetTitle = new TextView(context);
        presetTitle.setText("Mẫu có sẵn");
        presetTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        presetTitle.setTextColor(0xFF202124);
        root.addView(presetTitle);
        android.widget.HorizontalScrollView scroll = new android.widget.HorizontalScrollView(context);
        LinearLayout presets = new LinearLayout(context);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        String[] names = {"Dynamic Island", "Mượt", "Nảy mạnh", "Trượt lên", "Chỉ hiện dần"};
        int[][] values = {GboardEnterAnimationSettings.PRESET_DYNAMIC,
                GboardEnterAnimationSettings.PRESET_SMOOTH, GboardEnterAnimationSettings.PRESET_BOUNCY,
                GboardEnterAnimationSettings.PRESET_SLIDE, GboardEnterAnimationSettings.PRESET_FADE};
        for (int i = 0; i < names.length; i++) {
            final int[] preset = values[i];
            Button b = new Button(context);
            b.setText(names[i]);
            b.setOnClickListener(v -> {
                GboardEnterAnimationSettings.applyPreset(prefs, preset);
                for (Runnable r : refreshers) r.run();
                replay.run();
            });
            presets.addView(b);
        }
        scroll.addView(presets);
        root.addView(scroll);

        // Sliders. Index order matches GboardEnterAnimationSettings.KEYS.
        addAnimSlider(context, root, prefs, 0, "Thời gian", false, 1f, " ms", refreshers, replay);
        addAnimSlider(context, root, prefs, 1, "Độ nảy", true, 1f, "%", refreshers, replay);
        addAnimSlider(context, root, prefs, 2, "Độ nhanh của lò xo", false, 10f, "", refreshers, replay);
        addAnimSlider(context, root, prefs, 3, "Kích thước ngang lúc bắt đầu", false, 1f, "%", refreshers, replay);
        addAnimSlider(context, root, prefs, 4, "Kích thước dọc lúc bắt đầu", false, 1f, "%", refreshers, replay);
        addAnimSlider(context, root, prefs, 5, "Khoảng trượt từ dưới lên", false, 1f, "%", refreshers, replay);

        LinearLayout fadeRow = new LinearLayout(context);
        fadeRow.setGravity(Gravity.CENTER_VERTICAL);
        fadeRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView fadeTitle = new TextView(context);
        fadeTitle.setText("Hiện dần (mờ sang rõ)");
        fadeTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        fadeTitle.setTextColor(0xFF202124);
        fadeRow.addView(fadeTitle, new LinearLayout.LayoutParams(0, dp(context, 52), 1f));
        Button fadeToggle = new Button(context);
        Runnable fadeRefresh = () -> fadeToggle.setText(
                GboardEnterAnimationSettings.readInt(prefs, 6) != 0 ? "Bật" : "Tắt");
        refreshers.add(fadeRefresh);
        fadeRefresh.run();
        fadeToggle.setOnClickListener(v -> {
            GboardEnterAnimationSettings.write(prefs, 6,
                    GboardEnterAnimationSettings.readInt(prefs, 6) != 0 ? 0 : 1);
            fadeRefresh.run();
            replay.run();
        });
        fadeRow.addView(fadeToggle);
        root.addView(fadeRow);
        addAnimSlider(context, root, prefs, 7, "Tốc độ hiện dần", false, 10f, "", refreshers, replay);

        Button reset = new Button(context);
        reset.setText("Đặt lại hiệu ứng");
        reset.setOnClickListener(v -> {
            GboardEnterAnimationSettings.reset(prefs);
            for (Runnable r : refreshers) r.run();
            replay.run();
        });
        root.addView(reset);
    }

    /**
     * One labelled slider for a single animation parameter. When {@code invert} is set the slider
     * counts from the top of the range down (used for "bounce", which is 100 minus damping).
     */
    private static void addAnimSlider(Context context, LinearLayout root, SharedPreferences prefs,
            int index, String name, boolean invert, float divisor, String unit,
            java.util.List<Runnable> refreshers, Runnable replay) {
        final int min = GboardEnterAnimationSettings.MIN[index];
        final int max = GboardEnterAnimationSettings.MAX[index];
        TextView label = new TextView(context);
        label.setTextColor(0xFF202124);
        label.setPadding(0, dp(context, 10), 0, 0);
        root.addView(label);
        android.widget.SeekBar bar = new android.widget.SeekBar(context);
        bar.setMax(max - min);
        root.addView(bar);

        java.util.function.IntConsumer show = progress -> {
            int value = invert ? max - progress : min + progress;
            String text = invert ? Integer.toString(max - min == 0 ? 0 : progress)
                    : (divisor == 1f ? Integer.toString(value)
                            : String.format(java.util.Locale.ROOT, "%.1f", value / divisor));
            label.setText(name + ": " + text + unit);
        };
        Runnable refresh = () -> {
            int value = GboardEnterAnimationSettings.readInt(prefs, index);
            int progress = invert ? max - value : value - min;
            bar.setProgress(progress);
            show.accept(progress);
        };
        refreshers.add(refresh);
        refresh.run();
        bar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar b, int progress, boolean fromUser) {
                show.accept(progress);
                if (fromUser) GboardEnterAnimationSettings.write(prefs, index,
                        invert ? max - progress : min + progress);
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar b) { }
            @Override public void onStopTrackingTouch(android.widget.SeekBar b) { replay.run(); }
        });
    }

    private void addChipRows(Context context, LinearLayout root,
            GboardPatchesSettingsContract.FeatureHost host, SharedPreferences prefs) {
        // Transparency of the white pill in the suggestion strip.
        TextView transTitle = new TextView(context);
        transTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        transTitle.setTextColor(0xFF202124);
        transTitle.setPadding(0, dp(context, 12), 0, 0);
        root.addView(transTitle);
        android.widget.SeekBar slider = new android.widget.SeekBar(context);
        slider.setMax(100);
        int saved = GboardTextColorSettings.readChipTransparency(prefs);
        slider.setProgress(saved);
        transTitle.setText("Độ trong suốt nút gợi ý: " + saved + "%");
        slider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar bar, int progress,
                    boolean fromUser) {
                transTitle.setText("Độ trong suốt nút gợi ý: " + progress + "%");
                if (fromUser) GboardTextColorSettings.writeChipTransparency(prefs, progress);
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar bar) { }
            @Override public void onStopTrackingTouch(android.widget.SeekBar bar) {
                GboardTextColorSettings.writeChipTransparency(prefs, bar.getProgress());
            }
        });
        root.addView(slider);

        // Label shown on that pill.
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(context);
        title.setText("Chữ trên nút gợi ý");
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(0xFF202124);
        row.addView(title, new LinearLayout.LayoutParams(0, dp(context, 52), 1f));
        Button edit = new Button(context);
        edit.setText(chipLabelButton(prefs));
        edit.setOnClickListener(v -> GboardPatchesSettingsContract.showTextInputDialog(
                host, "Chữ trên nút gợi ý", "Để trống để giữ chữ gốc của Gboard",
                GboardTextColorSettings.readChipLabel(prefs),
                value -> {
                    GboardTextColorSettings.writeChipLabel(prefs, value);
                    edit.setText(chipLabelButton(prefs));
                }));
        row.addView(edit);
        root.addView(row);
    }

    private static String chipLabelButton(SharedPreferences prefs) {
        String label = GboardTextColorSettings.readChipLabel(prefs);
        return label.isEmpty() ? "Chữ gốc" : label;
    }

    private static android.text.SpannableString rainbowSample() {
        String text = "Q     W     E     R     T     Y";
        android.text.SpannableString out = new android.text.SpannableString(text);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ') continue;
            out.setSpan(new android.text.style.ForegroundColorSpan(
                    GboardTextColorRainbow.colorFor(String.valueOf(c), Color.WHITE)),
                    i, i + 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return out;
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
                host, label, "HEX, ví dụ #FFFFFF hoặc #AARRGGBB",
                GboardTextColorSettings.isSet(prefs, key)
                        ? GboardTextColorSettings.hex(prefs, key, fallback) : "",
                value -> {
                    if (GboardTextColorSettings.write(prefs, key, value)) {
                        preview.setTextColor(GboardTextColorSettings.read(prefs, key, fallback));
                        edit.setText(buttonText(prefs, key, fallback));
                    } else {
                        GboardPatchesSettingsContract.showMessage(host,
                                "Mã HEX không hợp lệ. Dùng #RRGGBB hoặc #AARRGGBB.");
                    }
                }));
        row.addView(edit);
        root.addView(row);
        preview.setTextColor(GboardTextColorSettings.read(prefs, key, fallback));
    }

    private static String buttonText(SharedPreferences prefs, String key, int fallback) {
        return GboardTextColorSettings.isSet(prefs, key)
                ? GboardTextColorSettings.hex(prefs, key, fallback) : "Mặc định";
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
