package com.esyion.wechat.wechat;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-128-ECB crypto utilities for CDN media.
 */
public class CryptoUtil {

    /**
     * Encrypt data with AES-128-ECB.
     */
    public static byte[] encryptAesEcb(byte[] plaintext, byte[] key) throws Exception {
        if (key.length != 16) {
            throw new IllegalArgumentException("AES-128 key must be 16 bytes, got " + key.length);
        }
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return cipher.doFinal(plaintext);
    }

    /**
     * Decrypt data with AES-128-ECB.
     */
    public static byte[] decryptAesEcb(byte[] ciphertext, byte[] key) throws Exception {
        if (key.length != 16) {
            throw new IllegalArgumentException("AES-128 key must be 16 bytes, got " + key.length);
        }
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return cipher.doFinal(ciphertext);
    }

    /**
     * Calculate AES-128-ECB padded size.
     */
    public static int aesEcbPaddedSize(int plaintextSize) {
        return ((plaintextSize / 16) + 1) * 16;
    }

    /**
     * Generate a fresh 16-byte AES key.
     */
    public static byte[] generateAesKey() {
        byte[] key = new byte[16];
        new SecureRandom().nextBytes(key);
        return key;
    }

    /**
     * Generate a 16-byte filekey as hex string.
     */
    public static String generateFilekey() {
        byte[] key = new byte[16];
        new SecureRandom().nextBytes(key);
        return bytesToHex(key);
    }

    /**
     * MD5 hex (uppercase).
     */
    public static String md5Hex(byte[] buf) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(buf);
        return bytesToHex(digest).toUpperCase();
    }

    /**
     * Parse CDNMedia.aes_key into a raw 16-byte AES key.
     * Supports:
     *   - base64(raw 16 bytes)
     *   - base64(hex string of 16 bytes)
     */
    public static byte[] parseAesKey(String aesKeyBase64) {
        byte[] decoded = Base64.getDecoder().decode(aesKeyBase64);
        if (decoded.length == 16) {
            return decoded;
        }
        if (decoded.length == 32) {
            // Could be hex string
            String hex = new String(decoded, StandardCharsets.US_ASCII);
            if (hex.matches("^[0-9a-fA-F]{32}$")) {
                return hexStringToBytes(hex);
            }
        }
        throw new IllegalArgumentException(
                "aes_key must decode to 16 raw bytes or 32-char hex, got " + decoded.length + " bytes");
    }

    /**
     * Convert hex AES key to base64 for outbound messages.
     * The hex string's ASCII bytes are base64-encoded.
     */
    public static String aesKeyHexToBase64(String hexKey) {
        return Base64.getEncoder().encodeToString(hexKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Convert hex AES key to raw 16-byte buffer.
     */
    public static byte[] aesKeyHexToBytes(String hexKey) {
        byte[] buf = hexStringToBytes(hexKey);
        if (buf.length != 16) {
            throw new IllegalArgumentException("Expected 16 bytes from hex key, got " + buf.length);
        }
        return buf;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexStringToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
