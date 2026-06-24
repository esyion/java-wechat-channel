package com.esyion.wechat.channel;

import com.esyion.wechat.wechat.WechatTypes;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Inbound message passed to onMessage handlers.
 */
public class ChannelMsg {
    public final String fromUserId;
    public final String contextToken;
    public final String text;
    public final List<MediaRef> media;
    public final WechatTypes.WeixinMessage raw;

    public ChannelMsg(String fromUserId, String contextToken, String text,
                     List<MediaRef> media, WechatTypes.WeixinMessage raw) {
        this.fromUserId = fromUserId;
        this.contextToken = contextToken;
        this.text = text;
        this.media = media;
        this.raw = raw;
    }
}

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

/**
 * Reply helper for sending responses.
 */
public interface Reply {
    CompletableFuture<Void> text(String content);
    CompletableFuture<Void> text(String content, int maxChars);
    CompletableFuture<Void> media(String filePath);
    CompletableFuture<Void> media(String filePath, String caption);
    CompletableFuture<Void> typing(boolean on);
}

/**
 * Create channel options.
 */
public class CreateChannelOpts {
    public String botToken;
    public String accountId;
    public String baseUrl;
    public String cdnBaseUrl;
    public String channelVersion;
    public String botAgent;
    public String botType;
    public String stateDir;
    public com.esyion.wechat.store.Store store;
    public java.util.function.BiFunction<ChannelMsg, Reply, CompletableFuture<Void>> onMessage;
    public java.util.function.BiConsumer<Throwable, java.util.Map<String, Object>> onError;
    public int longPollTimeoutMs;
    public String mediaTmpDir;
    public java.util.Set<String> blockedUsers;
}

/**
 * Channel handle returned by createChannel.
 */
public class ChannelHandle {
    public final WechatApiClient api;

    public ChannelHandle(WechatApiClient api) {
        this.api = api;
    }

    public CompletableFuture<Void> start();
    public CompletableFuture<Void> stop() ;
}
