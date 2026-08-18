package com.randommedia.player;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.webkit.MimeTypeMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class MediaScanner {
    private MediaScanner() {}

    public static ArrayList<MediaItem> scan(Context context, Uri treeUri) {
        ArrayList<MediaItem> result = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        String rootId;
        try {
            rootId = DocumentsContract.getTreeDocumentId(treeUri);
        } catch (Exception e) {
            return result;
        }

        ArrayDeque<String> pendingDirs = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        pendingDirs.add(rootId);

        while (!pendingDirs.isEmpty()) {
            String parentId = pendingDirs.removeFirst();
            if (!visited.add(parentId)) continue;

            Uri childrenUri;
            try {
                childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId);
            } catch (Exception e) {
                continue;
            }

            String[] projection = {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
            };

            try (Cursor cursor = resolver.query(childrenUri, projection, null, null, null)) {
                if (cursor == null) continue;
                int idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
                int nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                int mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE);

                while (cursor.moveToNext()) {
                    String id = cursor.getString(idCol);
                    String name = cursor.getString(nameCol);
                    String mime = cursor.getString(mimeCol);

                    if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                        pendingDirs.addLast(id);
                        continue;
                    }

                    boolean isVideo = isVideo(name, mime);
                    boolean isImage = isImage(name, mime);
                    if (!isVideo && !isImage) continue;

                    Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id);
                    result.add(new MediaItem(documentUri.toString(), mime, name, isVideo));
                }
            } catch (SecurityException ignored) {
                // Algún proveedor puede denegar una subcarpeta concreta.
            } catch (Exception ignored) {
                // Un archivo defectuoso no debe detener todo el escaneo.
            }
        }
        return result;
    }

    private static boolean isImage(String name, String mime) {
        if (mime != null && mime.toLowerCase(Locale.ROOT).startsWith("image/")) {
            String m = mime.toLowerCase(Locale.ROOT);
            return !m.contains("svg") && !m.contains("icon");
        }
        String ext = extension(name);
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") ||
                ext.equals("gif") || ext.equals("webp") || ext.equals("bmp") ||
                ext.equals("heic") || ext.equals("heif") || ext.equals("avif");
    }

    private static boolean isVideo(String name, String mime) {
        if (mime != null && mime.toLowerCase(Locale.ROOT).startsWith("video/")) return true;
        String ext = extension(name);
        return ext.equals("mp4") || ext.equals("webm") || ext.equals("mkv") ||
                ext.equals("3gp") || ext.equals("m4v") || ext.equals("mov") ||
                ext.equals("avi") || ext.equals("ts");
    }

    private static String extension(String name) {
        if (name == null) return "";
        String ext = MimeTypeMap.getFileExtensionFromUrl(name);
        if (ext == null || ext.isEmpty()) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0 && dot + 1 < name.length()) ext = name.substring(dot + 1);
        }
        return ext == null ? "" : ext.toLowerCase(Locale.ROOT);
    }
}
