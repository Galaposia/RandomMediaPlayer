package com.randommedia.player;

import android.net.Uri;

public final class MediaItem {
    public final String uri;
    public final String mimeType;
    public final String name;
    public final boolean video;

    public MediaItem(String uri, String mimeType, String name, boolean video) {
        this.uri = uri;
        this.mimeType = mimeType == null ? "" : mimeType;
        this.name = name == null ? "" : name;
        this.video = video;
    }

    public Uri asUri() {
        return Uri.parse(uri);
    }
}
