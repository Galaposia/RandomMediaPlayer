package com.randommedia.player;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class PlayerActivity extends Activity {
    public static final String EXTRA_TREE_URI = "tree_uri";
    public static final String EXTRA_IMAGE_SECONDS = "image_seconds";
    public static final String EXTRA_COLLAGE_ENABLED = "collage_enabled";
    public static final String EXTRA_COLLAGE_PERCENT = "collage_percent";

    private FrameLayout root;
    private FrameLayout mediaContainer;
    private LinearLayout controls;
    private TextView infoText;
    private Button pauseButton;
    private ProgressBar progress;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Random random = new Random();
    private final ArrayList<VideoView> activeVideos = new ArrayList<>();
    private final ArrayList<AnimatedImageDrawable> activeAnimations = new ArrayList<>();
    private final ArrayList<Slide> history = new ArrayList<>();

    private ArrayList<MediaItem> media = new ArrayList<>();
    private ArrayList<Integer> deck = new ArrayList<>();
    private int deckPosition = 0;
    private int historyIndex = -1;
    private int lastDeckIndex = -1;
    private boolean collageEnabled;
    private int collagePercent;
    private long imageDurationMs;
    private boolean paused = false;
    private boolean loading = true;
    private int renderToken = 0;
    private long timerDeadlineMs = 0;
    private long timerRemainingMs = 0;
    private boolean currentUsesTimer = false;
    private Runnable timedAdvance;
    private Runnable hideControlsRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        imageDurationMs = Math.max(1, getIntent().getIntExtra(EXTRA_IMAGE_SECONDS, 10)) * 1000L;
        collageEnabled = getIntent().getBooleanExtra(EXTRA_COLLAGE_ENABLED, true);
        collagePercent = clamp(getIntent().getIntExtra(EXTRA_COLLAGE_PERCENT, 15), 0, 100);

        buildPlayerUi();
        enterImmersive();
        scanMedia();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        enterImmersive();
        if (historyIndex >= 0 && historyIndex < history.size()) {
            showSlide(history.get(historyIndex));
        }
    }

    private void buildPlayerUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        root.setClickable(true);
        root.setOnClickListener(v -> toggleControls());

        mediaContainer = new FrameLayout(this);
        mediaContainer.setBackgroundColor(Color.BLACK);
        root.addView(mediaContainer, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        progress = new ProgressBar(this);
        FrameLayout.LayoutParams progressLp = new FrameLayout.LayoutParams(dp(64), dp(64));
        progressLp.gravity = Gravity.CENTER;
        root.addView(progress, progressLp);

        infoText = new TextView(this);
        infoText.setTextColor(Color.WHITE);
        infoText.setTextSize(15);
        infoText.setGravity(Gravity.CENTER);
        infoText.setPadding(dp(16), dp(8), dp(16), dp(8));
        FrameLayout.LayoutParams infoLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        infoLp.gravity = Gravity.TOP;
        infoLp.topMargin = dp(16);
        root.addView(infoText, infoLp);

        controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(dp(8), dp(8), dp(8), dp(12));
        controls.setBackgroundColor(0x99000000);

        Button prev = controlButton("ANTERIOR");
        pauseButton = controlButton("PAUSA");
        Button next = controlButton("SIGUIENTE");
        Button close = controlButton("SALIR");
        prev.setOnClickListener(v -> previousSlide());
        pauseButton.setOnClickListener(v -> togglePause());
        next.setOnClickListener(v -> nextSlide());
        close.setOnClickListener(v -> finish());

        controls.addView(prev, weighted());
        controls.addView(pauseButton, weighted());
        controls.addView(next, weighted());
        controls.addView(close, weighted());

        FrameLayout.LayoutParams controlsLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        controlsLp.gravity = Gravity.BOTTOM;
        root.addView(controls, controlsLp);

        setContentView(root);
    }

    private void scanMedia() {
        String tree = getIntent().getStringExtra(EXTRA_TREE_URI);
        if (tree == null) {
            showFatal("No se ha recibido ninguna carpeta.");
            return;
        }
        infoText.setText("Buscando imágenes y vídeos…");
        executor.execute(() -> {
            ArrayList<MediaItem> found = MediaScanner.scan(this, Uri.parse(tree));
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                loading = false;
                progress.setVisibility(View.GONE);
                media = found;
                if (media.isEmpty()) {
                    showFatal("No se han encontrado imágenes, GIF o vídeos compatibles en esta carpeta.");
                    return;
                }
                rebuildDeck();
                infoText.setText(media.size() + (media.size() == 1 ? " archivo encontrado" : " archivos encontrados"));
                handler.postDelayed(() -> infoText.setVisibility(View.GONE), 1800);
                nextSlide();
            });
        });
    }

    private void rebuildDeck() {
        deck.clear();
        for (int i = 0; i < media.size(); i++) deck.add(i);
        Collections.shuffle(deck, random);
        if (deck.size() > 1 && deck.get(0) == lastDeckIndex) {
            Collections.swap(deck, 0, 1);
        }
        deckPosition = 0;
    }

    private MediaItem drawFromDeck(Set<Integer> avoid) {
        if (media.isEmpty()) return null;
        int attempts = 0;
        while (attempts < media.size() * 3 + 3) {
            if (deckPosition >= deck.size()) rebuildDeck();
            int idx = deck.get(deckPosition++);
            lastDeckIndex = idx;
            attempts++;
            if (avoid == null || !avoid.contains(idx)) return media.get(idx);
        }
        return media.get(random.nextInt(media.size()));
    }

    private Slide generateSlide() {
        boolean makeCollage = collageEnabled && media.size() >= 2 && random.nextInt(100) < collagePercent;
        int count = 1;
        if (makeCollage) count = 2 + random.nextInt(Math.min(4, media.size()) - 1);

        ArrayList<MediaItem> items = new ArrayList<>();
        Set<Integer> usedIndices = new HashSet<>();
        for (int i = 0; i < count; i++) {
            MediaItem chosen = null;
            int guard = 0;
            while (guard++ < media.size() * 2 + 2) {
                if (deckPosition >= deck.size()) rebuildDeck();
                int idx = deck.get(deckPosition++);
                lastDeckIndex = idx;
                if (usedIndices.add(idx)) {
                    chosen = media.get(idx);
                    break;
                }
            }
            if (chosen != null) items.add(chosen);
        }
        if (items.isEmpty()) items.add(drawFromDeck(null));
        return new Slide(items);
    }

    private void nextSlide() {
        if (loading || media.isEmpty()) return;
        paused = false;
        pauseButton.setText("PAUSA");

        if (historyIndex + 1 < history.size()) {
            historyIndex++;
            showSlide(history.get(historyIndex));
            return;
        }

        Slide slide = generateSlide();
        history.add(slide);
        // Evita crecimiento ilimitado durante sesiones muy largas.
        if (history.size() > 250) {
            history.remove(0);
        } else {
            historyIndex++;
        }
        if (history.size() > 250) historyIndex = history.size() - 1;
        showSlide(slide);
    }

    private void previousSlide() {
        if (loading || history.isEmpty()) return;
        if (historyIndex > 0) {
            historyIndex--;
            paused = false;
            pauseButton.setText("PAUSA");
            showSlide(history.get(historyIndex));
        }
    }

    private void showSlide(Slide slide) {
        int token = ++renderToken;
        stopCurrentPlayback();
        mediaContainer.removeAllViews();
        mediaContainer.setBackgroundColor(Color.BLACK);
        currentUsesTimer = false;
        timerRemainingMs = 0;

        if (slide.items.size() <= 1) {
            MediaItem item = slide.items.get(0);
            if (item.video) showSingleVideo(item, token);
            else showSingleImage(item, token);
        } else {
            showCollage(slide.items, token);
        }
        scheduleControlsHide();
    }

    private void showSingleImage(MediaItem item, int token) {
        ImageView view = new ImageView(this);
        view.setBackgroundColor(Color.BLACK);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mediaContainer.addView(view, match());
        loadImage(item, view, token, true);
        startSlideTimer(imageDurationMs);
    }

    private void showSingleVideo(MediaItem item, int token) {
        VideoView video = new VideoView(this);
        video.setBackgroundColor(Color.BLACK);
        mediaContainer.addView(video, match());
        activeVideos.add(video);
        try {
            video.setVideoURI(item.asUri());
            video.setOnPreparedListener(mp -> {
                if (token != renderToken) return;
                mp.setLooping(false);
                mp.setVolume(1f, 1f);
                if (!paused) video.start();
            });
            video.setOnCompletionListener(mp -> {
                if (token == renderToken && !paused) nextSlide();
            });
            video.setOnErrorListener((mp, what, extra) -> {
                if (token == renderToken) {
                    Toast.makeText(this, "No se pudo reproducir: " + item.name, Toast.LENGTH_SHORT).show();
                    handler.postDelayed(this::nextSlide, 500);
                }
                return true;
            });
        } catch (Exception e) {
            handler.postDelayed(this::nextSlide, 500);
        }
    }

    private void showCollage(List<MediaItem> items, int token) {
        View collage = buildCollageLayout(items, token);
        mediaContainer.addView(collage, match());

        int videoCount = 0;
        for (MediaItem item : items) if (item.video) videoCount++;
        if (videoCount == 0) {
            startSlideTimer(imageDurationMs);
            return;
        }

        AtomicInteger remainingVideos = new AtomicInteger(videoCount);
        // VideoViews have already been inserted. Their listeners are attached in buildMediaCell.
        for (VideoView video : activeVideos) {
            Object tag = video.getTag();
            if (!(tag instanceof Integer) || ((Integer) tag) != token) continue;
            video.setOnCompletionListener(mp -> {
                if (token != renderToken) return;
                if (remainingVideos.decrementAndGet() <= 0 && !paused) nextSlide();
            });
            video.setOnErrorListener((mp, what, extra) -> {
                if (token != renderToken) return true;
                if (remainingVideos.decrementAndGet() <= 0 && !paused) handler.postDelayed(this::nextSlide, 200);
                return true;
            });
        }
    }

    private View buildCollageLayout(List<MediaItem> items, int token) {
        int n = items.size();
        if (n == 2) {
            LinearLayout pair = new LinearLayout(this);
            pair.setBackgroundColor(Color.BLACK);
            boolean vertical = random.nextBoolean();
            pair.setOrientation(vertical ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
            pair.addView(buildMediaCell(items.get(0), token), weightedCellFor(pair));
            pair.addView(buildMediaCell(items.get(1), token), weightedCellFor(pair));
            return pair;
        }

        if (n == 3) {
            boolean mirrored = random.nextBoolean();
            LinearLayout outer = new LinearLayout(this);
            outer.setOrientation(LinearLayout.HORIZONTAL);
            outer.setBackgroundColor(Color.BLACK);

            View large = buildMediaCell(items.get(0), token);
            LinearLayout stacked = new LinearLayout(this);
            stacked.setOrientation(LinearLayout.VERTICAL);
            stacked.addView(buildMediaCell(items.get(1), token), weightedCellFor(stacked));
            stacked.addView(buildMediaCell(items.get(2), token), weightedCellFor(stacked));

            if (!mirrored) {
                outer.addView(large, weightedCellFor(outer));
                outer.addView(stacked, weightedCellFor(outer));
            } else {
                outer.addView(stacked, weightedCellFor(outer));
                outer.addView(large, weightedCellFor(outer));
            }
            return outer;
        }

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackgroundColor(Color.BLACK);
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(buildMediaCell(items.get(0), token), weightedCellFor(row1));
        row1.addView(buildMediaCell(items.get(1), token), weightedCellFor(row1));
        row2.addView(buildMediaCell(items.get(2), token), weightedCellFor(row2));
        row2.addView(buildMediaCell(items.get(3), token), weightedCellFor(row2));
        outer.addView(row1, weightedCellFor(outer));
        outer.addView(row2, weightedCellFor(outer));
        return outer;
    }

    private View buildMediaCell(MediaItem item, int token) {
        FrameLayout cell = new FrameLayout(this);
        cell.setBackgroundColor(Color.BLACK);
        int gap = dp(2);
        cell.setPadding(gap, gap, gap, gap);

        if (item.video) {
            VideoView video = new VideoView(this);
            video.setBackgroundColor(Color.BLACK);
            video.setTag(token);
            cell.addView(video, match());
            activeVideos.add(video);
            try {
                video.setVideoURI(item.asUri());
                video.setOnPreparedListener(mp -> {
                    if (token != renderToken) return;
                    mp.setLooping(false);
                    mp.setVolume(0f, 0f);
                    if (!paused) video.start();
                });
            } catch (Exception ignored) {}
            return cell;
        }

        ImageView image = new ImageView(this);
        image.setBackgroundColor(Color.BLACK);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cell.addView(image, match());
        loadImage(item, image, token, false);
        return cell;
    }

    private void loadImage(MediaItem item, ImageView target, int token, boolean fitEntireImage) {
        executor.execute(() -> {
            try {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), item.asUri());
                Drawable drawable = ImageDecoder.decodeDrawable(source, (decoder, info, src) -> {
                    int sw = Math.max(1, info.getSize().getWidth());
                    int sh = Math.max(1, info.getSize().getHeight());
                    int maxW = Math.max(720, getResources().getDisplayMetrics().widthPixels);
                    int maxH = Math.max(720, getResources().getDisplayMetrics().heightPixels);
                    float scale = Math.min(1f, Math.min((float) maxW / sw, (float) maxH / sh));
                    if (scale < 0.999f) {
                        decoder.setTargetSize(Math.max(1, Math.round(sw * scale)), Math.max(1, Math.round(sh * scale)));
                    }
                });
                runOnUiThread(() -> {
                    if (token != renderToken || isFinishing()) return;
                    target.setScaleType(fitEntireImage ? ImageView.ScaleType.FIT_CENTER : ImageView.ScaleType.CENTER_CROP);
                    target.setImageDrawable(drawable);
                    if (drawable instanceof AnimatedImageDrawable) {
                        AnimatedImageDrawable animated = (AnimatedImageDrawable) drawable;
                        animated.setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);
                        activeAnimations.add(animated);
                        if (!paused) animated.start();
                    }
                });
            } catch (IOException | RuntimeException e) {
                runOnUiThread(() -> {
                    if (token != renderToken) return;
                    target.setBackgroundColor(Color.rgb(20, 20, 20));
                });
            }
        });
    }

    private void startSlideTimer(long durationMs) {
        currentUsesTimer = true;
        timerRemainingMs = durationMs;
        scheduleTimer(durationMs);
    }

    private void scheduleTimer(long delayMs) {
        cancelTimer();
        timerRemainingMs = Math.max(1, delayMs);
        timerDeadlineMs = SystemClock.uptimeMillis() + timerRemainingMs;
        timedAdvance = () -> {
            timerRemainingMs = 0;
            if (!paused && !isFinishing()) nextSlide();
        };
        handler.postDelayed(timedAdvance, timerRemainingMs);
    }

    private void cancelTimer() {
        if (timedAdvance != null) handler.removeCallbacks(timedAdvance);
        timedAdvance = null;
    }

    private void togglePause() {
        if (loading) return;
        paused = !paused;
        pauseButton.setText(paused ? "REANUDAR" : "PAUSA");

        if (paused) {
            if (currentUsesTimer && timedAdvance != null) {
                timerRemainingMs = Math.max(1, timerDeadlineMs - SystemClock.uptimeMillis());
                cancelTimer();
            }
            for (VideoView video : activeVideos) {
                try { if (video.isPlaying()) video.pause(); } catch (Exception ignored) {}
            }
            for (AnimatedImageDrawable animated : activeAnimations) {
                try { animated.stop(); } catch (Exception ignored) {}
            }
            controls.setVisibility(View.VISIBLE);
            cancelHideControls();
        } else {
            if (currentUsesTimer) scheduleTimer(timerRemainingMs > 0 ? timerRemainingMs : imageDurationMs);
            for (VideoView video : activeVideos) {
                try { video.start(); } catch (Exception ignored) {}
            }
            for (AnimatedImageDrawable animated : activeAnimations) {
                try { animated.start(); } catch (Exception ignored) {}
            }
            scheduleControlsHide();
        }
    }

    private void stopCurrentPlayback() {
        cancelTimer();
        cancelHideControls();
        for (VideoView video : activeVideos) {
            try { video.stopPlayback(); } catch (Exception ignored) {}
        }
        activeVideos.clear();
        for (AnimatedImageDrawable animated : activeAnimations) {
            try { animated.stop(); } catch (Exception ignored) {}
        }
        activeAnimations.clear();
    }

    private void toggleControls() {
        if (controls.getVisibility() == View.VISIBLE) {
            if (!paused) controls.setVisibility(View.GONE);
        } else {
            controls.setVisibility(View.VISIBLE);
            scheduleControlsHide();
        }
    }

    private void scheduleControlsHide() {
        cancelHideControls();
        if (paused) return;
        hideControlsRunnable = () -> controls.setVisibility(View.GONE);
        handler.postDelayed(hideControlsRunnable, 3200);
    }

    private void cancelHideControls() {
        if (hideControlsRunnable != null) handler.removeCallbacks(hideControlsRunnable);
        hideControlsRunnable = null;
    }

    private void showFatal(String message) {
        loading = false;
        progress.setVisibility(View.GONE);
        infoText.setVisibility(View.VISIBLE);
        infoText.setText(message);
        controls.setVisibility(View.VISIBLE);
    }

    private void enterImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        enterImmersive();
    }

    @Override
    protected void onDestroy() {
        ++renderToken;
        stopCurrentPlayback();
        executor.shutdownNow();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private Button controlButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setPadding(dp(4), dp(8), dp(4), dp(8));
        return b;
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    private LinearLayout.LayoutParams weightedCellFor(LinearLayout parent) {
        if (parent.getOrientation() == LinearLayout.HORIZONTAL) {
            return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        }
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
    }

    private FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Slide {
        final ArrayList<MediaItem> items;
        Slide(List<MediaItem> items) {
            this.items = new ArrayList<>(items);
        }
    }
}
