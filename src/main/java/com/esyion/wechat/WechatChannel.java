package com.esyion.wechat;

import com.esyion.wechat.channel.*;
import com.esyion.wechat.config.Config;
import com.esyion.wechat.error.ChannelError;
import com.esyion.wechat.store.FileStore;
import com.esyion.wechat.store.MemoryStore;
import com.esyion.wechat.store.Store;
import com.esyion.wechat.wechat.WechatApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Main entry point for creating a WeChat channel.
 */
public class WechatChannel {
    private static final Logger log = LoggerFactory.getLogger(WechatChannel.class);

    /**
     * Create a fully-wired WeChat channel handle.
     *
     * @param opts channel options
     * @return channel handle with start/stop
     * @throws ChannelError if botToken or accountId is missing
     */
    public static ChannelHandle createChannel(CreateChannelOpts opts) {
        if (opts.botToken == null || opts.botToken.isEmpty()) {
            throw new ChannelError(ChannelError.AUTH_REQUIRED, "botToken is required");
        }
        if (opts.accountId == null || opts.accountId.isEmpty()) {
            throw new ChannelError(ChannelError.INVALID_TOKEN, "accountId is required");
        }

        // Load config with env overrides
        Config config = new Config("WECHAT_CHANNEL_");

        String baseUrl = opts.baseUrl != null ? opts.baseUrl : config.getBaseUrl();
        String cdnBaseUrl = opts.cdnBaseUrl != null ? opts.cdnBaseUrl : config.getCdnBaseUrl();
        String channelVersion = opts.channelVersion != null ? opts.channelVersion : config.getChannelVersion();
        String botAgent = opts.botAgent != null ? opts.botAgent : channelVersion;
        int longPollTimeoutMs = opts.longPollTimeoutMs > 0 ? opts.longPollTimeoutMs : config.getLongPollTimeoutMs();

        WechatApiClient api = new WechatApiClient(
                baseUrl, cdnBaseUrl, opts.botToken, channelVersion, botAgent,
                15000, longPollTimeoutMs
        );

        String stateDir = opts.stateDir != null ? opts.stateDir : config.getStateDir();
        Store store = opts.store != null ? opts.store : new FileStore(stateDir + "/store.json");
        String mediaTmpDir = opts.mediaTmpDir != null ? opts.mediaTmpDir : stateDir + "/media";

        java.util.function.BiConsumer<Throwable, java.util.Map<String, Object>> onError =
                opts.onError != null ? opts.onError : (e, ctx) -> {
                    log.error("Error in phase {}: {}", ctx != null ? ctx.get("phase") : "unknown", e.getMessage());
                };

        Set<String> blockedUsers = opts.blockedUsers != null
                ? new HashSet<>(opts.blockedUsers) : new HashSet<>();

        AtomicReference<CompletableFuture<Void>> loopFuture = new AtomicReference<>();
        CountDownLatch stopLatch = new CountDownLatch(1);

        ChannelHandle handle = new ChannelHandle(api, () -> {
            CompletableFuture<Void> future = new CompletableFuture<>();
            loopFuture.set(future);

            LongPoll longPoll = new LongPoll(
                    api, store, mediaTmpDir,
                    (msg, reply) -> {
                        if (!blockedUsers.contains(msg.fromUserId) && opts.onMessage != null) {
                            return opts.onMessage.apply(msg, reply);
                        }
                        return CompletableFuture.completedFuture(null);
                    },
                    onError,
                    longPollTimeoutMs
            );

            longPoll.run().whenComplete((result, err) -> {
                if (err != null) {
                    future.completeExceptionally(err);
                } else {
                    future.complete(result);
                }
                stopLatch.countDown();
            });

            return future;
        }, () -> {
            CompletableFuture<Void> f = loopFuture.get();
            if (f != null) {
                f.complete(null);
            }
            try {
                api.notifyStop();
            } catch (Exception e) {
                onError.accept(e, java.util.Map.of("phase", "notifyStop"));
            }
            store.flush().join();
            stopLatch.countDown();
        });

        return handle;
    }

    /**
     * Create a channel and start it, blocking until stopped.
     */
    public static void run(CreateChannelOpts opts) {
        ChannelHandle handle = createChannel(opts);
        handle.start().join();
    }
}
