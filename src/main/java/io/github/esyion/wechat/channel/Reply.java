package io.github.esyion.wechat.channel;

import java.util.concurrent.CompletableFuture;

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
