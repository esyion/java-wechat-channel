package io.github.esyion.wechat.wechat;

import java.util.concurrent.CompletableFuture;

/**
 * QR login status states.
 */
class QrStatus {
    static final String WAIT = "wait";
    static final String SCANED = "scaned";
    static final String CONFIRMED = "confirmed";
    static final String EXPIRED = "expired";
    static final String SCANED_BUT_REDIRECT = "scaned_but_redirect";
    static final String NEED_VERIFYCODE = "need_verifycode";
    static final String VERIFY_CODE_BLOCKED = "verify_code_blocked";
    static final String BINDED_REDIRECT = "binded_redirect";
}

/**
 * QR login handle with matrix and render helpers.
 */
class QrMatrix {
    private final boolean[][] matrix;

    public QrMatrix(boolean[][] matrix) {
        this.matrix = matrix;
    }

    public boolean[][] getMatrix() {
        return matrix;
    }

    public int getWidth() {
        return matrix.length > 0 ? matrix[0].length : 0;
    }

    public int getHeight() {
        return matrix.length;
    }

    public String toTerminal(int margin, boolean invert) {
        StringBuilder sb = new StringBuilder();
        String dark = invert ? " " : "█";
        String light = invert ? "█" : " ";
        for (int i = 0; i < margin; i++) {
            sb.append(light.repeat(getWidth() + margin * 2)).append("\n");
        }
        for (boolean[] row : matrix) {
            sb.append(light.repeat(margin));
            for (boolean cell : row) {
                sb.append(cell ? dark : light);
            }
            sb.append(light.repeat(margin)).append("\n");
        }
        for (int i = 0; i < margin; i++) {
            sb.append(light.repeat(getWidth() + margin * 2));
            if (i < margin - 1) sb.append("\n");
        }
        return sb.toString();
    }
}

/**
 * Login handle exposed to users.
 */
interface QRLoginHandle {
    boolean[][] getMatrix();
    String toTerminal(int margin, boolean invert);
    byte[] toPng(int size, int margin);
    String toSvg(int margin);
    String toDataUrl(int size, int margin);
    CompletableFuture<LoginResult> waitForLogin(int timeoutMs);
}
