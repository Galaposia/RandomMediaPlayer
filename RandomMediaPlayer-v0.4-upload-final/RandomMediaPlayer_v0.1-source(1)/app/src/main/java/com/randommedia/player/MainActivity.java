package com.randommedia.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ColorStateList;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
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

    private static final int INK = Color.rgb(34, 43, 91);
    private static final int MUTED_INK = Color.rgb(79, 87, 135);
    private static final int ACCENT = Color.rgb(42, 171, 181);

    private SharedPreferences prefs;
    private TextView folderText;
    private TextView durationText;
    private TextView collagePercentText;
    private TextView folderCardSubtitle;
    private TextView durationCardSubtitle;
    private TextView collageCardSubtitle;
    private SeekBar durationSeek;
    private SeekBar collageSeek;
    private Switch collageSwitch;
    private Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(12, 18, 48));
        getWindow().setNavigationBarColor(Color.rgb(9, 13, 35));
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildUi();
        restoreUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(gradient(
                new int[]{Color.rgb(13, 20, 55), Color.rgb(42, 56, 119), Color.rgb(15, 22, 58)},
                GradientDrawable.Orientation.TL_BR, 0, 0, 0));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        int outerPadding = getResources().getConfiguration().screenWidthDp >= 700 ? 30 : 14;
        page.setPadding(dp(outerPadding), dp(18), dp(outerPadding), dp(22));
        scroll.addView(page, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(18), dp(18), dp(18));
        panel.setElevation(dp(12));
        panel.setBackground(gradient(
                new int[]{Color.rgb(225, 231, 255), Color.rgb(166, 178, 225)},
                GradientDrawable.Orientation.TOP_BOTTOM, 24, Color.rgb(103, 118, 181), 1));
        page.addView(panel, fullWidth());

        LinearLayout brand = row();
        brand.setGravity(Gravity.CENTER);
        TextView logo = text("▶", 24, true);
        logo.setTextColor(Color.WHITE);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(gradient(
                new int[]{Color.rgb(57, 205, 184), Color.rgb(31, 128, 175)},
                GradientDrawable.Orientation.TL_BR, 50, Color.WHITE, 2));
        brand.addView(logo, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout brandWords = new LinearLayout(this);
        brandWords.setOrientation(LinearLayout.VERTICAL);
        brandWords.setPadding(dp(12), 0, 0, 0);
        TextView title = text("RANDOM MEDIA PLAYER", 24, true);
        title.setTextColor(INK);
        title.setLetterSpacing(0.035f);
        brandWords.addView(title);
        TextView version = text("MENÚ DE PRESENTACIÓN · v0.4", 11, true);
        version.setTextColor(Color.rgb(75, 120, 159));
        version.setLetterSpacing(0.10f);
        brandWords.addView(version, marginTop(2));
        brand.addView(brandWords);
        panel.addView(brand, fullWidth());

        TextView prompt = text("Prepara tu sesión", 16, true);
        prompt.setTextColor(INK);
        prompt.setGravity(Gravity.CENTER);
        panel.addView(prompt, marginTop(16));
        TextView hint = text("Elige el contenido y ajusta cómo quieres verlo", 13, false);
        hint.setTextColor(MUTED_INK);
        hint.setGravity(Gravity.CENTER);
        panel.addView(hint, marginTop(3));
        panel.addView(buildCards(), marginTop(14));

        LinearLayout settings = new LinearLayout(this);
        settings.setOrientation(LinearLayout.VERTICAL);
        settings.setPadding(dp(16), dp(13), dp(16), dp(13));
        settings.setBackground(gradient(
                new int[]{0xF7FFFFFF, 0xEDEBF0FF},
                GradientDrawable.Orientation.TOP_BOTTOM, 17, Color.rgb(139, 151, 204), 1));
        panel.addView(settings, marginTop(15));

        LinearLayout durationHeader = row();
        TextView durationLabel = text("Tiempo por imagen / GIF", 14, true);
        durationLabel.setTextColor(INK);
        durationHeader.addView(durationLabel, weighted());
        durationText = pill("10 segundos");
        durationHeader.addView(durationText);
        settings.addView(durationHeader, fullWidth());

        durationSeek = styledSeekBar();
        durationSeek.setMax(59);
        durationSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int seconds = progress + 1;
                String value = seconds == 1 ? "1 segundo" : seconds + " segundos";
                durationText.setText(value);
                durationCardSubtitle.setText(seconds + " s por imagen");
                if (fromUser) prefs.edit().putInt(KEY_SECONDS, seconds).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        settings.addView(durationSeek, marginTop(5));

        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(205, 211, 237));
        LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        dividerLp.topMargin = dp(8);
        dividerLp.bottomMargin = dp(9);
        settings.addView(divider, dividerLp);

        collageSwitch = new Switch(this);
        collageSwitch.setText("Crear collages inteligentes de 2 a 4 archivos");
        collageSwitch.setTextColor(INK);
        collageSwitch.setTextSize(14);
        collageSwitch.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        collageSwitch.setThumbTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{ACCENT, Color.rgb(151, 157, 181)}));
        collageSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            collageSeek.setEnabled(isChecked);
            collagePercentText.setEnabled(isChecked);
            updateCollageCard(isChecked, collageSeek.getProgress());
            prefs.edit().putBoolean(KEY_COLLAGE, isChecked).apply();
        });
        settings.addView(collageSwitch, fullWidth());

        LinearLayout collageHeader = row();
        collageHeader.setPadding(0, dp(7), 0, 0);
        TextView collageLabel = text("Frecuencia", 13, false);
        collageLabel.setTextColor(MUTED_INK);
        collageHeader.addView(collageLabel, weighted());
        collagePercentText = pill("15 %");
        collageHeader.addView(collagePercentText);
        settings.addView(collageHeader, fullWidth());

        collageSeek = styledSeekBar();
        collageSeek.setMax(100);
        collageSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                collagePercentText.setText(String.format(Locale.getDefault(), "%d %%", progress));
                updateCollageCard(collageSwitch.isChecked(), progress);
                if (fromUser) prefs.edit().putInt(KEY_COLLAGE_PERCENT, progress).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        settings.addView(collageSeek, marginTop(2));

        LinearLayout actions = row();
        Button help = button("?");
        help.setTextSize(20);
        help.setTextColor(INK);
        help.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        help.setBackground(ripple(Color.rgb(231, 235, 252), 50, Color.rgb(113, 128, 188)));
        help.setOnClickListener(v -> showHelp());
        actions.addView(help, new LinearLayout.LayoutParams(dp(48), dp(48)));

        Button folderButton = button("CAMBIAR CARPETA");
        folderButton.setTextSize(12);
        folderButton.setTextColor(INK);
        folderButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        folderButton.setBackground(ripple(Color.rgb(233, 237, 253), 15, Color.rgb(113, 128, 188)));
        folderButton.setOnClickListener(v -> chooseFolder());
        LinearLayout.LayoutParams folderLp = new LinearLayout.LayoutParams(0, dp(50), .8f);
        folderLp.leftMargin = dp(10);
        actions.addView(folderButton, folderLp);

        startButton = button("▶  INICIAR PRESENTACIÓN");
        startButton.setTextSize(14);
        startButton.setTextColor(Color.WHITE);
        startButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        startButton.setLetterSpacing(0.035f);
        startButton.setBackground(new RippleDrawable(
                ColorStateList.valueOf(0x55FFFFFF),
                gradient(new int[]{Color.rgb(67, 205, 171), Color.rgb(29, 137, 183)},
                        GradientDrawable.Orientation.LEFT_RIGHT, 16, Color.WHITE, 1), null));
        startButton.setOnClickListener(v -> startPlayer());
        LinearLayout.LayoutParams startLp = new LinearLayout.LayoutParams(0, dp(54), 1.35f);
        startLp.leftMargin = dp(10);
        actions.addView(startButton, startLp);
        panel.addView(actions, marginTop(15));

        folderText = text("Selecciona una carpeta para comenzar", 12, false);
        folderText.setTextColor(Color.rgb(220, 226, 255));
        folderText.setGravity(Gravity.CENTER);
        folderText.setMaxLines(2);
        page.addView(folderText, marginTop(10));

        setContentView(scroll);
    }

    private View buildCards() {
        LinearLayout cards = new LinearLayout(this);
        cards.setOrientation(LinearLayout.HORIZONTAL);
        cards.setGravity(Gravity.CENTER);

        CardViews folder = optionCard("▰", "CARPETA", "Toca para elegir",
                new int[]{Color.rgb(73, 174, 202), Color.rgb(64, 121, 158)});
        CardViews duration = optionCard("◷", "IMÁGENES", "10 s por imagen",
                new int[]{Color.rgb(112, 94, 188), Color.rgb(65, 76, 149)});
        CardViews collage = optionCard("▦", "COLLAGE", "Activo · 15 %",
                new int[]{Color.rgb(63, 178, 165), Color.rgb(67, 91, 157)});
        folderCardSubtitle = folder.subtitle;
        durationCardSubtitle = duration.subtitle;
        collageCardSubtitle = collage.subtitle;

        folder.root.setOnClickListener(v -> chooseFolder());
        duration.root.setOnClickListener(v -> Toast.makeText(this,
                "Ajusta el tiempo en el control inferior", Toast.LENGTH_SHORT).show());
        collage.root.setOnClickListener(v -> collageSwitch.setChecked(!collageSwitch.isChecked()));

        boolean wide = getResources().getConfiguration().screenWidthDp >= 700;
        if (wide) {
            cards.addView(folder.root, cardWeight(false));
            cards.addView(duration.root, cardWeight(true));
            cards.addView(collage.root, cardWeight(true));
            return cards;
        }

        cards.addView(folder.root, fixedCard(false));
        cards.addView(duration.root, fixedCard(true));
        cards.addView(collage.root, fixedCard(true));
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.addView(cards, new HorizontalScrollView.LayoutParams(
                HorizontalScrollView.LayoutParams.WRAP_CONTENT,
                HorizontalScrollView.LayoutParams.WRAP_CONTENT));
        return scroller;
    }

    private CardViews optionCard(String icon, String label, String subtitleValue, int[] colors) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(7), dp(7), dp(7), dp(9));
        card.setClickable(true);
        card.setFocusable(true);
        card.setElevation(dp(5));
        card.setBackground(ripple(Color.rgb(244, 246, 255), 14, Color.rgb(87, 101, 161)));

        TextView art = text(icon, 45, true);
        art.setTextColor(0xEFFFFFFF);
        art.setGravity(Gravity.CENTER);
        art.setShadowLayer(dp(3), 0, dp(2), 0x66000000);
        art.setBackground(gradient(colors, GradientDrawable.Orientation.TL_BR,
                9, 0x99FFFFFF, 1));
        card.addView(art, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(88)));

        TextView name = text(label, 13, true);
        name.setTextColor(INK);
        name.setGravity(Gravity.CENTER);
        name.setLetterSpacing(0.06f);
        card.addView(name, marginTop(7));

        TextView subtitle = text(subtitleValue, 11, false);
        subtitle.setTextColor(MUTED_INK);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setMaxLines(1);
        card.addView(subtitle, marginTop(2));
        return new CardViews(card, subtitle);
    }

    private void restoreUi() {
        String uri = prefs.getString(KEY_TREE, null);
        updateFolderLabel(uri);

        int seconds = clamp(prefs.getInt(KEY_SECONDS, 10), 1, 60);
        durationSeek.setProgress(seconds - 1);

        int percent = clamp(prefs.getInt(KEY_COLLAGE_PERCENT, 15), 0, 100);
        collageSeek.setProgress(percent);

        boolean collage = prefs.getBoolean(KEY_COLLAGE, true);
        collageSwitch.setChecked(collage);
        collageSeek.setEnabled(collage);
        collagePercentText.setEnabled(collage);
        updateCollageCard(collage, percent);
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
            folderText.setText("Selecciona una carpeta para comenzar");
            folderCardSubtitle.setText("Toca para elegir");
            return;
        }
        String shown = displayFolderName(uri);
        folderText.setText("Carpeta activa: " + shown);
        folderCardSubtitle.setText(shown);
    }

    private String displayFolderName(String uri) {
        try {
            Uri parsed = Uri.parse(uri);
            String shown = parsed.getLastPathSegment();
            if (shown == null || shown.isEmpty()) return "Carpeta seleccionada";
            shown = Uri.decode(shown).replace("primary:", "");
            int slash = Math.max(shown.lastIndexOf('/'), shown.lastIndexOf(':'));
            if (slash >= 0 && slash + 1 < shown.length()) shown = shown.substring(slash + 1);
            return shown.isEmpty() ? "Almacenamiento interno" : shown;
        } catch (Exception e) {
            return "Carpeta seleccionada";
        }
    }

    private void updateCollageCard(boolean enabled, int percent) {
        if (collageCardSubtitle == null) return;
        collageCardSubtitle.setText(enabled ? "Activo · " + percent + " %" : "Desactivado");
        collageCardSubtitle.setTextColor(enabled ? MUTED_INK : Color.rgb(143, 145, 158));
    }

    private void showHelp() {
        new AlertDialog.Builder(this)
                .setTitle("Cómo preparar la presentación")
                .setMessage("1. Elige una carpeta con imágenes o vídeos.\n\n" +
                        "2. Ajusta el tiempo de las imágenes.\n\n" +
                        "3. Activa los collages y elige su frecuencia.\n\n" +
                        "Los vídeos se reproducen completos. Las imágenes y los vídeos mantienen su proporción sin deformarse ni recortarse.")
                .setPositiveButton("ENTENDIDO", null)
                .show();
    }

    private SeekBar styledSeekBar() {
        SeekBar seek = new SeekBar(this);
        seek.setProgressTintList(ColorStateList.valueOf(ACCENT));
        seek.setThumbTintList(ColorStateList.valueOf(Color.rgb(31, 138, 174)));
        return seek;
    }

    private TextView pill(String value) {
        TextView result = text(value, 13, true);
        result.setTextColor(Color.WHITE);
        result.setGravity(Gravity.CENTER);
        result.setPadding(dp(11), dp(4), dp(11), dp(4));
        result.setBackground(gradient(
                new int[]{Color.rgb(65, 176, 183), Color.rgb(48, 126, 172)},
                GradientDrawable.Orientation.LEFT_RIGHT, 30, 0, 0));
        return result;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private GradientDrawable gradient(int[] colors,
                                      GradientDrawable.Orientation orientation,
                                      int radiusDp,
                                      int strokeColor,
                                      int strokeDp) {
        GradientDrawable background = new GradientDrawable(orientation, colors);
        background.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) background.setStroke(dp(strokeDp), strokeColor);
        return background;
    }

    private RippleDrawable ripple(int color, int radiusDp, int strokeColor) {
        GradientDrawable content = gradient(
                new int[]{color, color}, GradientDrawable.Orientation.LEFT_RIGHT,
                radiusDp, strokeColor, 1);
        return new RippleDrawable(ColorStateList.valueOf(0x334B5FAD), content, null);
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        return b;
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

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    private LinearLayout.LayoutParams cardWeight(boolean margin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        if (margin) p.leftMargin = dp(10);
        return p;
    }

    private LinearLayout.LayoutParams fixedCard(boolean margin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(182),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        if (margin) p.leftMargin = dp(10);
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class CardViews {
        final LinearLayout root;
        final TextView subtitle;

        CardViews(LinearLayout root, TextView subtitle) {
            this.root = root;
            this.subtitle = subtitle;
        }
    }
}
