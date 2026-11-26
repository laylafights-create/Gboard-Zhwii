package dev.jason.gboardpatches.extension.textcolor;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Imported keyboard font. The picked .ttf/.otf is validated, then copied into the app's private
 * files dir; the draw hook applies it to key, suggestion, toolbar and popup labels. Typeface
 * changes are posted, never done inside onDraw, and are guarded so a view is only touched once.
 */
public final class GboardCustomFont {
    public static final String KEY_FONT_NAME = "pref_custom_font_name";
    static final String FILE_NAME = "gboard_custom_font.bin";
    public static final int MAX_BYTES = 20 * 1024 * 1024;

    private static final long TTL_MS = 750L;
    private static final Map<TextView, Typeface> APPLIED =
            Collections.synchronizedMap(new WeakHashMap<TextView, Typeface>());
    private static volatile Typeface cached;
    private static volatile long cachedStamp = -1L;
    private static volatile long nextCheckMs;

    private GboardCustomFont() { }

    /** Validates and stores the font. Returns null on success, else a short error message. */
    public static String importFont(Context context, SharedPreferences prefs, String name, byte[] data) {
        if (context == null || data == null || data.length < 64) return "File font không hợp lệ";
        if (data.length > MAX_BYTES) return "File font quá lớn (tối đa 20 MB)";
        File target = new File(context.getFilesDir(), FILE_NAME);
        File temp = new File(context.getFilesDir(), FILE_NAME + ".tmp");
        try {
            try (FileOutputStream out = new FileOutputStream(temp)) { out.write(data); }
            Typeface loaded = Typeface.createFromFile(temp);
            if (loaded == null || loaded == Typeface.DEFAULT) {
                temp.delete();
                return "Không đọc được font này";
            }
            if (target.exists() && !target.delete()) { temp.delete(); return "Không ghi được font"; }
            if (!temp.renameTo(target)) { temp.delete(); return "Không ghi được font"; }
            if (prefs != null) prefs.edit().putString(KEY_FONT_NAME, name == null ? "" : name).apply();
            nextCheckMs = 0L;
            return null;
        } catch (Throwable t) {
            temp.delete();
            return "Không đọc được font này";
        }
    }

    public static void remove(Context context, SharedPreferences prefs) {
        try {
            if (context != null) new File(context.getFilesDir(), FILE_NAME).delete();
            if (prefs != null) prefs.edit().remove(KEY_FONT_NAME).apply();
        } catch (Throwable ignored) { }
        nextCheckMs = 0L;
    }

    public static String currentName(SharedPreferences prefs) {
        try { return prefs == null ? "" : prefs.getString(KEY_FONT_NAME, ""); }
        catch (Throwable ignored) { return ""; }
    }

    /** Draw-path hook. Cheap: a cached lookup, the file is re-checked every ~750 ms. */
    static void apply(final TextView view) {
        try {
            final Typeface font = typefaceFor(view.getContext());
            Typeface done = APPLIED.get(view);
            if (font == null) {
                if (done != null) { // font removed: hand the view back to Gboard's own face
                    APPLIED.remove(view);
                    view.post(() -> { try { view.setTypeface(null); } catch (Throwable ignored) { } });
                }
                return;
            }
            if (done == font) return;
            APPLIED.put(view, font);
            view.post(() -> { try { view.setTypeface(font); } catch (Throwable ignored) { } });
        } catch (Throwable ignored) { }
    }

    private static Typeface typefaceFor(Context context) {
        long now = android.os.SystemClock.uptimeMillis();
        if (now < nextCheckMs) return cached;
        nextCheckMs = now + TTL_MS;
        try {
            File file = new File(context.getFilesDir(), FILE_NAME);
            if (!file.isFile()) { cached = null; cachedStamp = -1L; return null; }
            long stamp = file.lastModified() ^ file.length();
            if (cached == null || stamp != cachedStamp) {
                cached = Typeface.createFromFile(file);
                cachedStamp = stamp;
            }
        } catch (Throwable ignored) { cached = null; }
        return cached;
    }
}
