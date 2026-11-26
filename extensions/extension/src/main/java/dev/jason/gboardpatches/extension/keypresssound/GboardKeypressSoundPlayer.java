package dev.jason.gboardpatches.extension.keypresssound;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Base64;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

/**
 * Plays the embedded samples with {@link SoundPool}. The samples are written once to the cache
 * directory because SoundPool loads from files. Each press picks a sample that differs from the
 * previous one and shifts the rate by a few percent so fast typing does not sound repeated.
 */
final class GboardKeypressSoundPlayer {
    private static final float VOLUME = 0.7f;

    private final Random random = new Random();
    private final SoundPool pool;
    private final int[] ids;
    private final int[] loaded;
    private int loadedCount;
    private int last = -1;

    GboardKeypressSoundPlayer(Context context) throws Exception {
        pool = new SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();
        String[] samples = GboardKeypressSoundData.samples();
        ids = new int[samples.length];
        loaded = new int[samples.length];
        pool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            if (status != 0) return;
            synchronized (this) { loaded[loadedCount++] = sampleId; }
        });
        File dir = new File(context.getCacheDir(), "gboard_patches_keypress_sound");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("cache dir");
        for (int i = 0; i < samples.length; i++) {
            File file = new File(dir, "key" + i + ".wav");
            byte[] bytes = Base64.decode(samples[i], Base64.DEFAULT);
            if (!file.isFile() || file.length() != bytes.length) {
                try (FileOutputStream out = new FileOutputStream(file)) { out.write(bytes); }
            }
            ids[i] = pool.load(file.getAbsolutePath(), 1);
        }
    }

    synchronized void play() {
        if (loadedCount == 0) return;
        int pick;
        do { pick = random.nextInt(loadedCount); } while (loadedCount > 1 && pick == last);
        last = pick;
        float rate = 0.95f + random.nextFloat() * 0.10f;
        float volume = VOLUME * (0.8f + random.nextFloat() * 0.2f);
        pool.play(loaded[pick], volume, volume, 1, 0, rate);
    }
}
