package dev.jason.gboardpatches.extension.textcolor;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static dev.jason.gboardpatches.extension.textcolor.GboardSuggestionAnimationSettings.*;

/** The "suggestion strip animation" block of the Custom Text Color settings page. */
final class GboardSuggestionAnimationSection {
    private static final int TEXT = 0xFF202124;

    private GboardSuggestionAnimationSection() { }

    static void add(Context context, LinearLayout parent, SharedPreferences prefs) {
        TextView header = title(context, "Hiệu ứng thanh gợi ý");
        header.setTextSize(16);
        header.setPadding(0, dp(context, 24), 0, 0);
        parent.addView(header);

        List<Runnable> refreshers = new ArrayList<>();

        // On/off. When off, the tuning controls are hidden.
        LinearLayout switchRow = row(context, "Bật hiệu ứng gợi ý");
        Button toggle = new Button(context);
        switchRow.addView(toggle);
        parent.addView(switchRow);
        LinearLayout body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        parent.addView(body);
        Runnable showState = () -> {
            boolean on = isEnabled(prefs);
            toggle.setText(on ? "Bật" : "Tắt");
            body.setVisibility(on ? View.VISIBLE : View.GONE);
        };
        showState.run();

        LinearLayout stage = new LinearLayout(context);
        stage.setOrientation(LinearLayout.HORIZONTAL);
        stage.setBackgroundColor(0xFF25282D);
        stage.setPadding(0, dp(context, 8), 0, dp(context, 8));
        String[] words = {"Xin", "chào", "bạn"};
        TextView[] samples = new TextView[words.length];
        for (int i = 0; i < words.length; i++) {
            samples[i] = new TextView(context);
            samples[i].setText(words[i]);
            samples[i].setTextColor(Color.WHITE);
            samples[i].setTextSize(18);
            samples[i].setGravity(Gravity.CENTER);
            stage.addView(samples[i], new LinearLayout.LayoutParams(0, dp(context, 44), 1f));
        }
        body.addView(stage);
        Runnable replay = () -> {
            int[] current = values(prefs);
            for (int i = 0; i < samples.length; i++) {
                GboardSuggestionAnimator.preview(samples[i], current, i);
            }
        };
        toggle.setOnClickListener(v -> {
            setEnabled(prefs, !isEnabled(prefs));
            showState.run();
        });
        Button play = new Button(context);
        play.setText("Xem thử");
        play.setOnClickListener(v -> replay.run());
        body.addView(play);

        body.addView(title(context, "Mẫu có sẵn"));
        LinearLayout presets = strip(context, body);
        for (int i = 0; i < PRESET_NAMES.length; i++) {
            final int[] preset = PRESETS[i];
            Button button = new Button(context);
            button.setText(PRESET_NAMES[i]);
            button.setOnClickListener(v -> {
                applyPreset(prefs, preset);
                for (Runnable r : refreshers) r.run();
                replay.run();
            });
            presets.addView(button);
        }

        choices(context, body, prefs, "Kiểu hiệu ứng", STYLE, STYLE_NAMES, refreshers, replay);
        choices(context, body, prefs, "Kiểu chuyển động", EASING, EASING_NAMES, refreshers, replay);
        choices(context, body, prefs, "Thứ tự xuất hiện", ORDER, ORDER_NAMES, refreshers, replay);

        slider(context, body, prefs, DURATION, "Thời gian", " ms", refreshers, replay);
        slider(context, body, prefs, STAGGER, "Độ trễ giữa các từ", " ms", refreshers, replay);
        slider(context, body, prefs, STRENGTH, "Cường độ chuyển động / độ nảy", "%", refreshers, replay);
        slider(context, body, prefs, DISTANCE, "Khoảng trượt (kiểu Trượt, Rơi)", "%", refreshers, replay);
        slider(context, body, prefs, SCALE, "Cỡ ban đầu (Phóng to, Bật nảy, Xoay, Thu nhỏ)", "%",
                refreshers, replay);
        slider(context, body, prefs, ANGLE, "Góc (kiểu Lật, Xoay)", "°", refreshers, replay);

        LinearLayout fadeRow = row(context, "Kèm mờ sang rõ");
        Button fade = new Button(context);
        Runnable showFade = () -> fade.setText(values(prefs)[FADE] != 0 ? "Bật" : "Tắt");
        showFade.run();
        refreshers.add(showFade);
        fade.setOnClickListener(v -> {
            write(prefs, FADE, values(prefs)[FADE] != 0 ? 0 : 1);
            showFade.run();
            replay.run();
        });
        fadeRow.addView(fade);
        body.addView(fadeRow);

        slider(context, body, prefs, ALPHA, "Độ rõ ban đầu", "%", refreshers, replay);
        slider(context, body, prefs, SKIP, "Bỏ hiệu ứng nếu chữ đổi nhanh hơn", " ms",
                refreshers, replay);

        Button reset = new Button(context);
        reset.setText("Đặt lại hiệu ứng gợi ý");
        reset.setOnClickListener(v -> {
            applyPreset(prefs, DEFAULTS);
            for (Runnable r : refreshers) r.run();
            replay.run();
        });
        body.addView(reset);
    }

    private static void choices(Context context, LinearLayout root, SharedPreferences prefs,
            String name, int index, String[] names, List<Runnable> refreshers, Runnable replay) {
        root.addView(title(context, name));
        LinearLayout strip = strip(context, root);
        Button[] buttons = new Button[names.length];
        Runnable refresh = () -> {
            int selected = values(prefs)[index];
            for (int i = 0; i < buttons.length; i++) {
                buttons[i].setText(i == selected ? "✓ " + names[i] : names[i]);
                buttons[i].setAlpha(i == selected ? 1f : 0.6f);
            }
        };
        for (int i = 0; i < names.length; i++) {
            final int choice = i;
            buttons[i] = new Button(context);
            buttons[i].setOnClickListener(v -> {
                write(prefs, index, choice);
                refresh.run();
                replay.run();
            });
            strip.addView(buttons[i]);
        }
        refreshers.add(refresh);
        refresh.run();
    }

    private static void slider(Context context, LinearLayout root, SharedPreferences prefs,
            int index, String name, String unit, List<Runnable> refreshers, Runnable replay) {
        TextView label = new TextView(context);
        label.setTextColor(TEXT);
        label.setPadding(0, dp(context, 10), 0, 0);
        root.addView(label);
        SeekBar bar = new SeekBar(context);
        bar.setMax(MAX[index] - MIN[index]);
        root.addView(bar);
        Runnable refresh = () -> {
            int value = values(prefs)[index];
            bar.setProgress(value - MIN[index]);
            label.setText(name + ": " + value + unit);
        };
        refreshers.add(refresh);
        refresh.run();
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar b, int progress, boolean fromUser) {
                label.setText(name + ": " + (MIN[index] + progress) + unit);
                if (fromUser) write(prefs, index, MIN[index] + progress);
            }
            @Override public void onStartTrackingTouch(SeekBar b) { }
            @Override public void onStopTrackingTouch(SeekBar b) { replay.run(); }
        });
    }

    private static LinearLayout strip(Context context, LinearLayout root) {
        HorizontalScrollView scroll = new HorizontalScrollView(context);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.HORIZONTAL);
        scroll.addView(content);
        root.addView(scroll);
        return content;
    }

    private static LinearLayout row(Context context, String name) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(title(context, name), new LinearLayout.LayoutParams(0, dp(context, 52), 1f));
        return row;
    }

    private static TextView title(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setTextColor(TEXT);
        return view;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
