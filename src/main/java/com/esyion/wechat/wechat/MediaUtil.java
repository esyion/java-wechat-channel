package com.esyion.wechat.wechat;

import com.esyion.wechat.error.MediaError;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

/**
 * Media upload/download utilities for CDN.
 */
public class MediaUtil {

    /**
     * Download and decrypt CDN media.
     */
    public static byte[] downloadAndDecryptCdn(WechatApiClient api, String cdnBaseUrl,
                                                 String encryptedQueryParam, String aesKeyHex,
                                                 String aesKeyBase64, String fullUrl, String label) throws Exception {
        String downloadUrl;

        if (fullUrl != null && !fullUrl.isEmpty()) {
            downloadUrl = fullUrl;
        } else if (encryptedQueryParam != null && !encryptedQueryParam.isEmpty()) {
            downloadUrl = cdnBaseUrl + "/c2cdownload?" + encryptedQueryParam;
        } else {
            throw new MediaError(label, "No download URL available");
        }

        byte[] encrypted;
        try {
            encrypted = api.cdnDownload(downloadUrl);
        } catch (IOException e) {
            throw new MediaError("download", e);
        }

        if (encrypted == null || encrypted.length == 0) {
            return new byte[0];
        }

        byte[] key;
        if (aesKeyHex != null && !aesKeyHex.isEmpty()) {
            key = CryptoUtil.aesKeyHexToBytes(aesKeyHex);
        } else if (aesKeyBase64 != null && !aesKeyBase64.isEmpty()) {
            key = CryptoUtil.parseAesKey(aesKeyBase64);
        } else {
            throw new MediaError("decrypt", "No AES key available");
        }

        try {
            return CryptoUtil.decryptAesEcb(encrypted, key);
        } catch (Exception e) {
            throw new MediaError("decrypt", e);
        }
    }

    /**
     * Encrypt and upload to CDN.
     */
    public static WechatTypes.UploadedFileInfo uploadToCdn(WechatApiClient api, String toUserId,
                                                           int mediaType, String filePath) throws Exception {
        Path path = Paths.get(filePath);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new MediaError("upload", "Not a file: " + filePath);
        }

        byte[] fileData = Files.readAllBytes(path);
        long rawSize = fileData.length;

        // Calculate MD5
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] rawMd5 = md.digest(fileData);
        String rawMd5Hex = CryptoUtil.bytesToHex(rawMd5).toUpperCase();

        // Generate AES key
        String aesKeyHex = CryptoUtil.generateFilekey();
        byte[] aesKey = CryptoUtil.aesKeyHexToBytes(aesKeyHex);

        // Encrypt
        byte[] encrypted = CryptoUtil.encryptAesEcb(fileData, aesKey);
        long cipherSize = encrypted.length;

        // Get upload URL
        WechatTypes.GetUploadUrlReq req = new WechatTypes.GetUploadUrlReq();
        req.filekey = UUID.randomUUID().toString().replace("-", "");
        req.mediaType = mediaType;
        req.toUserId = toUserId;
        req.rawsize = rawSize;
        req.rawfilemd5 = rawMd5Hex;
        req.filesize = cipherSize;
        req.noNeedThumb = true;
        req.aeskey = aesKeyHex;

        WechatTypes.GetUploadUrlResp uploadResp = api.getUploadUrl(req);

        // Upload encrypted data
        if (uploadResp.uploadFullUrl != null && !uploadResp.uploadFullUrl.isEmpty()) {
            api.cdnUpload(uploadResp.uploadFullUrl, encrypted, "application/octet-stream");
        } else if (uploadResp.uploadParam != null && !uploadResp.uploadParam.isEmpty()) {
            api.cdnUpload(uploadResp.uploadParam, encrypted, "application/octet-stream");
        }

        WechatTypes.UploadedFileInfo info = new WechatTypes.UploadedFileInfo();
        info.fileKey = req.filekey;
        info.downloadEncryptedQueryParam = uploadResp.uploadParam != null ? uploadResp.uploadParam : "";
        info.aeskey = aesKeyHex;
        info.fileSize = rawSize;
        info.fileSizeCiphertext = cipherSize;

        return info;
    }

    /**
     * Upload image.
     */
    public static WechatTypes.UploadedFileInfo uploadImage(WechatApiClient api, String filePath, String toUserId) throws Exception {
        return uploadToCdn(api, toUserId, WechatTypes.UPLOAD_MEDIA_TYPE_IMAGE, filePath);
    }

    /**
     * Upload video.
     */
    public static WechatTypes.UploadedFileInfo uploadVideo(WechatApiClient api, String filePath, String toUserId) throws Exception {
        return uploadToCdn(api, toUserId, WechatTypes.UPLOAD_MEDIA_TYPE_VIDEO, filePath);
    }

    /**
     * Upload generic file.
     */
    public static WechatTypes.UploadedFileInfo uploadAttachment(WechatApiClient api, String filePath, String toUserId) throws Exception {
        return uploadToCdn(api, toUserId, WechatTypes.UPLOAD_MEDIA_TYPE_FILE, filePath);
    }

    /**
     * Get MIME type from filename.
     */
    public static String getMimeFromFilename(String filename) {
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".bmp")) return "image/bmp";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".mov")) return "video/quicktime";
        if (lower.endsWith(".avi")) return "video/x-msvideo";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".silk")) return "audio/silk";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "application/msword";
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) return "application/vnd.ms-excel";
        if (lower.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }

    /**
     * Get extension for MIME type.
     */
    public static String getExtensionForMime(String mime) {
        switch (mime) {
            case "image/png": return ".png";
            case "image/jpeg": return ".jpg";
            case "image/gif": return ".gif";
            case "image/webp": return ".webp";
            case "video/mp4": return ".mp4";
            case "audio/mpeg": return ".mp3";
            case "audio/silk": return ".silk";
            default: return "";
        }
    }
}
