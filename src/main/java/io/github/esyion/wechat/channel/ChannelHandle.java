package io.github.esyion.wechat.channel;

import io.github.esyion.wechat.wechat.WechatApiClient;

import java.util.concurrent.CompletableFuture;

/**
 * Channel handle returned by createChannel.
 */
public interface ChannelHandle {
    WechatApiClient getApi();
    CompletableFuture<Void> start();
    CompletableFuture<Void> stop();
}
