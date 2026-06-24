package com.esyion.wechat.channel;

import com.esyion.wechat.error.MediaError;
import com.esyion.wechat.wechat.MediaUtil;
import com.esyion.wechat.wechat.WechatApiClient;
import com.esyion.wechat.wechat.WechatTypes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Build inbound message from raw WeixinMessage.
 */
public class Inbound {

    public static ChannelMsg build(WechatApiClient api, String mediaTmpDir, WechatTypes.WeixinMessage msg) throws Exception {
        String fromUserId = msg.fromUserId != null ? msg.fromUserId : "unknown";
        String contextToken = msg.contextToken != null ? msg.contextToken : "";

        Path userDir = Paths.get(mediaTmpDir, sanitizeUserId(fromUserId));
        Files.createDirectories(userDir);

        StringBuilder text = new StringBuilder();
        List<MediaRef> media = new ArrayList<>();

        List<WechatTypes.MessageItem> items = msg.itemList;
        if (items != null) {
            for (WechatTypes.MessageItem item : items) {
                // Text
                if (item.type != null && item.type == WechatTypes.MESSAGE_ITEM_TYPE_TEXT
                        && item.textItem != null && item.textItem.text != null) {
                    text.append(item.textItem.text);
                    continue;
                }

                // Voice -> text
                if (item.type != null && item.type == WechatTypes.MESSAGE_ITEM_TYPE_VOICE
                        && item.voiceItem != null && item.voiceItem.text != null) {
                    text.append(item.voiceItem.text);
                }

                // Image
                if (item.type != null && item.type == WechatTypes.MESSAGE_ITEM_TYPE_IMAGE) {
                    WechatTypes.ImageItem img = item.imageItem;
                    if (img == null) continue;
                    if ((img.media == null || (img.media.encryptQueryParam == null && img.media.fullUrl == null))
                            && img.url == null) continue;

                    try {
                        byte[] buf = MediaUtil.downloadAndDecryptCdn(
                                api,
                                api.getCdnBaseUrl(),
                                img.media != null ? img.media.encryptQueryParam : null,
                                img.aeskey,
                                img.media != null ? img.media.aesKey : null,
                                img.url != null ? img.url : (img.media != null ? img.media.fullUrl : null),
                                "image"
                        );
                        Path outPath = userDir.resolve("img-" + System.currentTimeMillis() + ".jpg");
                        Files.write(outPath, buf);
                        media.add(new MediaRef(outPath, "image/jpeg"));
                    } catch (Exception e) {
                        throw new MediaError("decrypt", e);
                    }
                    continue;
                }

                // File
                if (item.type != null && item.type == WechatTypes.MESSAGE_ITEM_TYPE_FILE) {
                    WechatTypes.FileItem f = item.fileItem;
                    if (f == null) continue;
                    if ((f.media == null || (f.media.encryptQueryParam == null && f.media.fullUrl == null))) continue;

                    try {
                        byte[] buf = MediaUtil.downloadAndDecryptCdn(
                                api,
                                api.getCdnBaseUrl(),
                                f.media.encryptQueryParam,
                                null,
                                f.media.aesKey,
                                f.media.fullUrl,
                                "file"
                        );
                        String fileName = f.fileName != null ? f.fileName : "file-" + System.currentTimeMillis() + ".bin";
                        Path outPath = userDir.resolve(fileName);
                        Files.write(outPath, buf);
                        media.add(new MediaRef(outPath, "application/octet-stream"));
                    } catch (Exception e) {
                        throw new MediaError("decrypt", e);
                    }
                    continue;
                }

                // Video
                if (item.type != null && item.type == WechatTypes.MESSAGE_ITEM_TYPE_VIDEO) {
                    WechatTypes.VideoItem v = item.videoItem;
                    if (v == null) continue;
                    if ((v.media == null || (v.media.encryptQueryParam == null && v.media.fullUrl == null))) continue;

                    try {
                        byte[] buf = MediaUtil.downloadAndDecryptCdn(
                                api,
                                api.getCdnBaseUrl(),
                                v.media.encryptQueryParam,
                                null,
                                v.media.aesKey,
                                v.media.fullUrl,
                                "video"
                        );
                        Path outPath = userDir.resolve("video-" + System.currentTimeMillis() + ".mp4");
                        Files.write(outPath, buf);
                        media.add(new MediaRef(outPath, "video/mp4"));
                    } catch (Exception e) {
                        throw new MediaError("decrypt", e);
                    }
                    continue;
                }
            }
        }

        if (text.length() == 0 && media.isEmpty()) {
            text.append("[empty message]");
        }

        return new ChannelMsg(fromUserId, contextToken, text.toString(), media, msg);
    }

    private static String sanitizeUserId(String id) {
        if (id == null) return "unknown";
        // 在字符类中，连字符放在最后一位就不需要转义
        String sanitized = id.replaceAll("[^a-zA-Z0-9_@.]", "_");
        return sanitized.length() > 64 ? sanitized.substring(0, 64) : sanitized;
    }
}
