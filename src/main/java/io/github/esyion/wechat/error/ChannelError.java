package io.github.esyion.wechat.error;

/**
 * Channel-level errors: missing credentials, invalid state, etc.
 */
public class ChannelError extends RuntimeException {
    private final String code;

    public ChannelError(String code, String message) {
        super(message);
        this.code = code;
    }

    public ChannelError(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static final String AUTH_REQUIRED = "AUTH_REQUIRED";
    public static final String INVALID_TOKEN = "INVALID_TOKEN";
    public static final String ABORTED = "ABORTED";
}
