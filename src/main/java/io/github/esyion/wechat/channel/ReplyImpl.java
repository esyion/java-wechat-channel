package io.github.esyion.wechat.channel;

import io.github.esyion.wechat.wechat.WechatApiClient;

import java.util.concurrent.CompletableFuture;

/**
 * Reply helper implementation.
 */
public class ReplyImpl implements Reply {
    private final WechatApiClient api;
    private final String toUserId;
    private final String contextToken;
    private final int defaultMaxChars;

    public ReplyImpl(WechatApiClient api, String toUserId, String contextToken) {
        this(api, toUserId, contextToken, 4000);
    }

    public ReplyImpl(WechatApiClient api, String toUserId, String contextToken, int defaultMaxChars) {
        this.api = api;
        this.toUserId = toUserId;
        this.contextToken = contextToken;
        this.defaultMaxChars = defaultMaxChars;
    }

    @Override
    public CompletableFuture<Void> text(String content) {
        return Outbound.sendText(api, toUserId, contextToken, content, defaultMaxChars);
    }

    @Override
    public CompletableFuture<Void> text(String content, int maxChars) {
        return Outbound.sendText(api, toUserId, contextToken, content, maxChars);
    }

    @Override
    public CompletableFuture<Void> media(String filePath) {
        return Outbound.sendMedia(api, toUserId, contextToken, filePath, null);
    }

    @Override
    public CompletableFuture<Void> media(String filePath, String caption) {
        return Outbound.sendMedia(api, toUserId, contextToken, filePath, caption);
    }

    @Override
    public CompletableFuture<Void> typing(boolean on) {
        return CompletableFuture.runAsync(() -> {
            try {
                TypingKeepalive.send(api, toUserId, on);
            } catch (Exception e) {
                // Ignore typing errors
            }
        });
    }
}
