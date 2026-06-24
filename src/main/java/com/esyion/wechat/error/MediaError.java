package com.esyion.wechat.error;

/**
 * Media download/decrypt/upload/encrypt failure.
 */
public class MediaError extends RuntimeException {
    private final String phase;

    public MediaError(String phase, Throwable cause) {
        super("Media error during: " + phase, cause);
        this.phase = phase;
    }

    public MediaError(String phase, String message) {
        super(message);
        this.phase = phase;
    }

    public String getPhase() {
        return phase;
    }

    public static final String DOWNLOAD = "download";
    public static final String DECRYPT = "decrypt";
    public static final String UPLOAD = "upload";
    public static final String ENCRYPT = "encrypt";
}
