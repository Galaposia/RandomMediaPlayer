package com.randommedia.player;

import android.content.Context;
import android.graphics.ImageDecoder;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import java.io.IOException;

public final class MediaItem {
    public enum Orientation {
        HORIZONTAL,
        VERTICAL,
        SQUARE
    }

    public final String uri;
    public final String mimeType;
    public final String name;
    public final boolean video;

    private volatile boolean dimensionsResolved;
    private volatile int displayWidth = 1;
    private volatile int displayHeight = 1;

    public MediaItem(String uri, String mimeType, String name, boolean video) {
        this.uri = uri;
        this.mimeType = mimeType == null ? "" : mimeType;
        this.name = name == null ? "" : name;
        this.video = video;
    }

    public Uri asUri() {
        return Uri.parse(uri);
    }

    /**
     * Lee únicamente la información necesaria para decidir el hueco del collage.
     * Se llama desde el executor de PlayerActivity y el resultado queda cacheado.
     */
    public void resolveDimensions(Context context) {
        if (dimensionsResolved) return;
        synchronized (this) {
            if (dimensionsResolved) return;

            int width = 0;
            int height = 0;
            try {
                if (video) {
                    int[] size = readVideoDimensions(context);
                    width = size[0];
                    height = size[1];
                } else {
                    int[] size = readImageDimensions(context);
                    width = size[0];
                    height = size[1];
                }
            } catch (IOException | RuntimeException ignored) {
                // Un archivo sin metadatos legibles se trata como cuadrado.
            }

            if (width <= 0 || height <= 0) {
                displayWidth = 1;
                displayHeight = 1;
            } else {
                displayWidth = width;
                displayHeight = height;
            }
            dimensionsResolved = true;
        }
    }

    public float aspectRatio() {
        return (float) displayWidth / (float) displayHeight;
    }

    public Orientation orientation() {
        float ratio = aspectRatio();
        if (ratio > 1.15f) return Orientation.HORIZONTAL;
        if (ratio < 0.87f) return Orientation.VERTICAL;
        return Orientation.SQUARE;
    }

    private int[] readImageDimensions(Context context) throws IOException {
        final int[] result = {0, 0};
        ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), asUri());
        ImageDecoder.decodeDrawable(source, (decoder, info, src) -> {
            result[0] = info.getSize().getWidth();
            result[1] = info.getSize().getHeight();
            // Decodificar una miniatura evita cargar la imagen completa solo para leer su proporción.
            decoder.setTargetSize(1, 1);
        });
        return result;
    }

    private int[] readVideoDimensions(Context context) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, asUri());
            int width = parseMetadata(retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            int height = parseMetadata(retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            int rotation = parseMetadata(retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
            if (rotation == 90 || rotation == 270) {
                int originalWidth = width;
                width = height;
                height = originalWidth;
            }
            return new int[]{width, height};
        } finally {
            try {
                retriever.release();
            } catch (IOException | RuntimeException ignored) {
                // Liberar el lector no debe impedir continuar con la presentación.
            }
        }
    }

    private static int parseMetadata(String value) {
        if (value == null) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
