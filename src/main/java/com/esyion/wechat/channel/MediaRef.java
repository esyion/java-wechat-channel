package com.esyion.wechat.channel;

import java.nio.file.Path;

/**
 * Reference to a local decrypted media file.
 */
public class MediaRef {
    public final Path path;
    public final String mime;

    public MediaRef(Path path, String mime) {
        this.path = path;
        this.mime = mime;
    }

    public String getPath() {
        return path.toString();
    }
}
