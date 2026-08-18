package com.randommedia.player;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_TREE = 1001;
    private static final String PREFS = "random_media_settings";
    private static final String KEY_TREE = "tree_uri";
    private static final String KEY_SECONDS = "image_seconds";
    private static final String KEY_COLLAGE = "collage_enabled";
    private static final String KEY_COLLAGE_PERCENT = "collage_percent";

    private SharedPreferences prefs;
    private TextView folderText;
    private TextView durationText;
    private TextView collagePercentText;
    private SeekBar durationSeek;
    private SeekBar collageSeek;
    private Switch collageSwitch;
    private Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildUi();
        restoreUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(245, 245, 245));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(28), dp(24), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = text("RANDOM MEDIA PLAYER", 26, true);
        title.setTextColor(Color.rgb(30, 30, 30));
        root.addView(title);

        TextView version = text("Primera versión funcional · v0.1", 14, false);
        version.setTextColor(Color.DKGRAY);
        root.addView(version, marginTop(4));

        root.addView(space(24));
        root.addView(section("Carpeta de contenido"));

        folderText = text("No hay ninguna carpeta seleccionada", 14, false);
        folderText.setTextColor(Color.DKGRAY);
        folderText.setPadding(dp(12), dp(12), dp(12), dp(12));
        folderText.setBackgroundColor(Color.WHITE);
        root.addView(folderText, fullWidth());

        Button folderButton = button("SELECCIONAR CARPETA");
        folderButton.setOnClickListener(v -> chooseFolder());
        root.addView(folderButton, marginTop(10));

        root.addView(space(24));
        root.addView(section("Tiempo por imagen / GIF"));
        durationText = text("10 segundos", 18, true);
        durationText.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(durationText, fullWidth());

        durationSeek = new SeekBar(this);
        durationSeek.setMax(59); // 1..60 s
        durationSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int seconds = progress + 1;
                durationText.setText(seconds == 1 ? "1 segundo" : seconds + " segundos");
                if (fromUser) prefs.edit().putInt(KEY_SECONDS, seconds).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        root.addView(durationSeek, fullWidth());

        root.addView(space(20));
        collageSwitch = new Switch(this);
        collageSwitch.setText("Activar collages de 2 a 4 archivos");
        collageSwitch.setTextSize(16);
        collageSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            collageSeek.setEnabled(isChecked);
            collagePercentText.setEnabled(isChecked);
            prefs.edit().putBoolean(KEY_COLLAGE, isChecked).apply();
        });
        root.addView(collageSwitch, fullWidth());

        root.addView(space(12));
        root.addView(section("Frecuencia de collage"));
        collagePercentText = text("15 %", 18, true);
        collagePercentText.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(collagePercentText, fullWidth());

        collageSeek = new SeekBar(this);
        collageSeek.setMax(100);
        collageSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                collagePercentText.setText(String.format(Locale.getDefault(), "%d %%", progress));
                if (fromUser) prefs.edit().putInt(KEY_COLLAGE_PERCENT, progress).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        root.addView(collageSeek, fullWidth());

        root.addView(space(28));
        startButton = button("▶  INICIAR PRESENTACIÓN");
        startButton.setTextSize(17);
        startButton.setMinHeight(dp(56));
        startButton.setOnClickListener(v -> startPlayer());
        root.addView(startButton, fullWidth());

        root.addView(space(16));
        TextView note = text(
                "Las imágenes y GIF usan el tiempo elegido. Los vídeos se reproducen una vez completos. " +
                        "Dentro de un collage los vídeos se silencian para evitar varias pistas de audio simultáneas.",
                13, false);
        note.setTextColor(Color.GRAY);
        root.addView(note, fullWidth());

        setContentView(scroll);
    }

    private void restoreUi() {
        String uri = prefs.getString(KEY_TREE, null);
        updateFolderLabel(uri);

        int seconds = clamp(prefs.getInt(KEY_SECONDS, 10), 1, 60);
        durationSeek.setProgress(seconds - 1);

        boolean collage = prefs.getBoolean(KEY_COLLAGE, true);
        collageSwitch.setChecked(collage);

        int percent = clamp(prefs.getInt(KEY_COLLAGE_PERCENT, 15), 0, 100);
        collageSeek.setProgress(percent);
        collageSeek.setEnabled(collage);
        collagePercentText.setEnabled(collage);
    }

    private void chooseFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_TREE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_TREE || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;

        int takeFlags = data.getFlags() &
                (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            getContentResolver().takePersistableUriPermission(uri, takeFlags);
        } catch (SecurityException ignored) {
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignoredAgain) {}
        }

        prefs.edit().putString(KEY_TREE, uri.toString()).apply();
        updateFolderLabel(uri.toString());
        Toast.makeText(this, "Carpeta guardada", Toast.LENGTH_SHORT).show();
    }

    private void startPlayer() {
        String tree = prefs.getString(KEY_TREE, null);
        if (tree == null || tree.isEmpty()) {
            Toast.makeText(this, "Selecciona primero una carpeta", Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_TREE_URI, tree);
        intent.putExtra(PlayerActivity.EXTRA_IMAGE_SECONDS,
                clamp(prefs.getInt(KEY_SECONDS, 10), 1, 60));
        intent.putExtra(PlayerActivity.EXTRA_COLLAGE_ENABLED,
                prefs.getBoolean(KEY_COLLAGE, true));
        intent.putExtra(PlayerActivity.EXTRA_COLLAGE_PERCENT,
                clamp(prefs.getInt(KEY_COLLAGE_PERCENT, 15), 0, 100));
        startActivity(intent);
    }

    private void updateFolderLabel(String uri) {
        if (uri == null || uri.isEmpty()) {
            folderText.setText("No hay ninguna carpeta seleccionada");
            return;
        }
        try {
            Uri parsed = Uri.parse(uri);
            String shown = parsed.getLastPathSegment();
            folderText.setText(shown == null ? uri : shown.replace("primary:", "/"));
        } catch (Exception e) {
            folderText.setText(uri);
        }
    }

    private TextView section(String value) {
        TextView t = text(value, 16, true);
        t.setTextColor(Color.rgb(45, 45, 45));
        t.setPadding(0, 0, 0, dp(8));
        return t;
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        return b;
    }

    private View space(int dp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(dp)));
        return v;
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams marginTop(int topDp) {
        LinearLayout.LayoutParams p = fullWidth();
        p.topMargin = dp(topDp);
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
