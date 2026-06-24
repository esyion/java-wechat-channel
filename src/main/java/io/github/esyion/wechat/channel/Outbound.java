package io.github.esyion.wechat.channel;

import io.github.esyion.wechat.error.MediaError;
import io.github.esyion.wechat.wechat.CryptoUtil;
import io.github.esyion.wechat.wechat.MediaUtil;
import io.github.esyion.wechat.wechat.WechatApiClient;
import io.github.esyion.wechat.wechat.WechatTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Outbound message sending utilities.
 */
public class Outbound {

    public static CompletableFuture<Void> sendText(WechatApiClient api, String toUserId,
                                                    String contextToken, String text, int maxChars) {
        return CompletableFuture.runAsync(() -> {
            try {
                for (String chunk : chunkText(text, maxChars)) {
                    WechatTypes.SendMessageReq req = new WechatTypes.SendMessageReq();
                    req.msg = new WechatTypes.SendMessageReq.Msg();
                    req.msg.fromUserId = "";
                    req.msg.toUserId = toUserId;
                    req.msg.clientId = "wac:" + System.currentTimeMillis() + "-" + Long.toHexString(Double.doubleToLongBits(Math.random()));
                    req.msg.messageType = WechatTypes.MESSAGE_TYPE_BOT;
                    req.msg.messageState = WechatTypes.MESSAGE_STATE_FINISH;
                    req.msg.contextToken = contextToken;

                    WechatTypes.MessageItem item = new WechatTypes.MessageItem();
                    item.type = WechatTypes.MESSAGE_ITEM_TYPE_TEXT;
                    item.textItem = new WechatTypes.TextItem();
                    item.textItem.text = chunk;
                    req.msg.itemList = List.of(item);

                    api.sendMessage(req);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to send text", e);
            }
        });
    }

    public static CompletableFuture<Void> sendMedia(WechatApiClient api, String toUserId,
                                                    String contextToken, String filePath, String caption) {
        return CompletableFuture.runAsync(() -> {
            try {
                String mime = MediaUtil.getMimeFromFilename(filePath);

                WechatTypes.UploadedFileInfo uploaded;
                if (mime.startsWith("image/")) {
                    uploaded = MediaUtil.uploadImage(api, filePath, toUserId);
                } else if (mime.startsWith("video/")) {
                    uploaded = MediaUtil.uploadVideo(api, filePath, toUserId);
                } else {
                    uploaded = MediaUtil.uploadAttachment(api, filePath, toUserId);
                }

                // Send caption first if present
                if (caption != null && !caption.isEmpty()) {
                    WechatTypes.SendMessageReq captionReq = new WechatTypes.SendMessageReq();
                    captionReq.msg = new WechatTypes.SendMessageReq.Msg();
                    captionReq.msg.fromUserId = "";
                    captionReq.msg.toUserId = toUserId;
                    captionReq.msg.clientId = newClientId();
                    captionReq.msg.messageType = WechatTypes.MESSAGE_TYPE_BOT;
                    captionReq.msg.messageState = WechatTypes.MESSAGE_STATE_FINISH;
                    captionReq.msg.contextToken = contextToken;
                    WechatTypes.MessageItem captionItem = new WechatTypes.MessageItem();
                    captionItem.type = WechatTypes.MESSAGE_ITEM_TYPE_TEXT;
                    captionItem.textItem = new WechatTypes.TextItem();
                    captionItem.textItem.text = caption;
                    captionReq.msg.itemList = List.of(captionItem);
                    api.sendMessage(captionReq);
                }

                // Send media message
                WechatTypes.SendMessageReq mediaReq = new WechatTypes.SendMessageReq();
                mediaReq.msg = new WechatTypes.SendMessageReq.Msg();
                mediaReq.msg.fromUserId = "";
                mediaReq.msg.toUserId = toUserId;
                mediaReq.msg.clientId = newClientId();
                mediaReq.msg.messageType = WechatTypes.MESSAGE_TYPE_BOT;
                mediaReq.msg.messageState = WechatTypes.MESSAGE_STATE_FINISH;
                mediaReq.msg.contextToken = contextToken;

                WechatTypes.MessageItem mediaItem = buildMediaItem(uploaded, mime);
                mediaReq.msg.itemList = List.of(mediaItem);

                api.sendMessage(mediaReq);
            } catch (Exception e) {
                throw new RuntimeException("Failed to send media", e);
            }
        });
    }

    private static WechatTypes.MessageItem buildMediaItem(WechatTypes.UploadedFileInfo uploaded, String mime) {
        WechatTypes.MessageItem item = new WechatTypes.MessageItem();
        String aesKeyBase64 = CryptoUtil.aesKeyHexToBase64(uploaded.aeskey);

        if (mime.startsWith("image/")) {
            item.type = WechatTypes.MESSAGE_ITEM_TYPE_IMAGE;
            item.imageItem = new WechatTypes.ImageItem();
            item.imageItem.aeskey = uploaded.aeskey;
            item.imageItem.media = new WechatTypes.CdnMedia();
            item.imageItem.media.encryptQueryParam = uploaded.downloadEncryptedQueryParam;
            item.imageItem.media.aesKey = aesKeyBase64;
            item.imageItem.media.encryptType = 1;
            item.imageItem.midSize = (int) uploaded.fileSizeCiphertext;
        } else if (mime.startsWith("video/")) {
            item.type = WechatTypes.MESSAGE_ITEM_TYPE_VIDEO;
            item.videoItem = new WechatTypes.VideoItem();
            item.videoItem.media = new WechatTypes.CdnMedia();
            item.videoItem.media.encryptQueryParam = uploaded.downloadEncryptedQueryParam;
            item.videoItem.media.aesKey = aesKeyBase64;
            item.videoItem.media.encryptType = 1;
            item.videoItem.videoSize = (int) uploaded.fileSizeCiphertext;
        } else {
            item.type = WechatTypes.MESSAGE_ITEM_TYPE_FILE;
            item.fileItem = new WechatTypes.FileItem();
            item.fileItem.media = new WechatTypes.CdnMedia();
            item.fileItem.media.encryptQueryParam = uploaded.downloadEncryptedQueryParam;
            item.fileItem.media.aesKey = aesKeyBase64;
            item.fileItem.media.encryptType = 1;
            item.fileItem.fileName = "file";
            item.fileItem.len = String.valueOf(uploaded.fileSize);
        }

        return item;
    }

    private static String newClientId() {
        return "wac:" + System.currentTimeMillis() + "-" + Long.toHexString(Double.doubleToLongBits(Math.random()));
    }

    /**
     * Split text into chunks at maxChars, preferring line breaks and word boundaries.
     */
    public static List<String> chunkText(String text, int max) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        if (text.length() <= max) {
            chunks.add(text);
            return chunks;
        }

        String remaining = text;
        while (!remaining.isEmpty()) {
            if (remaining.length() <= max) {
                chunks.add(remaining);
                break;
            }

            int cut = remaining.lastIndexOf('\n', max);
            if (cut < max * 0.6) {
                cut = remaining.lastIndexOf(' ', max);
            }
            if (cut < max * 0.6) {
                cut = max;
            }

            chunks.add(remaining.substring(0, cut));
            remaining = remaining.substring(cut).replaceFirst("^\\s+", "");
        }

        return chunks;
    }
}
