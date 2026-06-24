package com.esyion.wechat;

import com.esyion.wechat.channel.*;
import com.esyion.wechat.config.Config;
import com.esyion.wechat.error.ChannelError;
import com.esyion.wechat.store.FileStore;
import com.esyion.wechat.store.MemoryStore;
import com.esyion.wechat.store.Store;
import com.esyion.wechat.wechat.QRLogin;
import com.esyion.wechat.wechat.WechatApiClient;
import com.esyion.wechat.wechat.LoginResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Channel handle with start/stop control.
 */
class ChannelHandleImpl implements ChannelHandle {
    private static final Logger log = LoggerFactory.getLogger(ChannelHandleImpl.class);
    private final WechatApiClient api;
    private final BiFunction<ChannelMsg, Reply, CompletableFuture<Void>> onMessage;
    private final BiConsumer<Throwable, java.util.Map<String, Object>> onError;
    private final Store store;
    private final String mediaTmpDir;
    private final int longPollTimeoutMs;
    private final Set<String> blockedUsers;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicReference<CompletableFuture<?>> loopFuture = new AtomicReference<>();
    private final CountDownLatch stopLatch = new CountDownLatch(1);

    ChannelHandleImpl(WechatApiClient api,
                     BiFunction<ChannelMsg, Reply, CompletableFuture<Void>> onMessage,
                     BiConsumer<Throwable, java.util.Map<String, Object>> onError,
                     Store store, String mediaTmpDir,
                     int longPollTimeoutMs, Set<String> blockedUsers) {
        this.api = api;
        this.onMessage = onMessage;
        this.onError = onError;
        this.store = store;
        this.mediaTmpDir = mediaTmpDir;
        this.longPollTimeoutMs = longPollTimeoutMs;
        this.blockedUsers = blockedUsers;
    }

    public WechatApiClient getApi() {
        return api;
    }

    @Override
    public CompletableFuture<Void> start() {
        if (!started.compareAndSet(false, true)) {
            throw new ChannelError(ChannelError.ABORTED, "channel already started");
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        loopFuture.set(future);

        Thread loopThread = new Thread(() -> {
            try {
                LongPoll longPoll = new LongPoll(
                        api, store, mediaTmpDir,
                        (msg, reply) -> {
                            if (!blockedUsers.contains(msg.fromUserId) && onMessage != null) {
                                return onMessage.apply(msg, reply);
                            }
                            return CompletableFuture.completedFuture(null);
                        },
                        onError,
                        longPollTimeoutMs
                );

                longPoll.run().get();
            } catch (Exception e) {
                if (!future.isDone()) {
                    future.completeExceptionally(e);
                }
            } finally {
                stopLatch.countDown();
            }
        }, "wechat-long-poll");

        loopThread.start();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stop() {
        CompletableFuture<?> f = loopFuture.get();
        if (f != null && !f.isDone()) {
            f.complete(null);
        }
        try {
            api.notifyStop();
        } catch (Exception e) {
            onError.accept(e, java.util.Map.of("phase", "notifyStop"));
        }
        store.flush().join();
        stopLatch.countDown();
        return CompletableFuture.completedFuture(null);
    }
}

/**
 * Main entry point for creating a WeChat channel.
 */
public class WechatChannelProvider {

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

        BiConsumer<Throwable, java.util.Map<String, Object>> onError =
                opts.onError != null ? opts.onError : (e, ctx) -> {
                    log.error("Error in phase {}: {}", ctx != null ? ctx.get("phase") : "unknown", e);
                };

        Set<String> blockedUsers = opts.blockedUsers != null
                ? new HashSet<>(opts.blockedUsers) : new HashSet<>();

        return new ChannelHandleImpl(
                api, opts.onMessage, onError, store, mediaTmpDir,
                longPollTimeoutMs, blockedUsers
        );
    }

    /**
     * Request a QR code for login.
     */
    public static QRLogin.QrCodeResult requestQrCode(String baseUrl) throws java.io.IOException {
        WechatApiClient api = new WechatApiClient(
                baseUrl, baseUrl, null, "wechat-channel/0.1.0", "wechat-channel/0.1.0",
                15000, 15000
        );
        QRLogin login = new QRLogin(api, "3");
        return login.requestQrCode();
    }

    /**
     * Wait for QR code login to complete.
     */
    public static LoginResult waitForLogin(String baseUrl, String qrcode, int timeoutMs) {
        try {
            WechatApiClient api = new WechatApiClient(
                    baseUrl, baseUrl, null, "wechat-channel/0.1.0", "wechat-channel/0.1.0",
                    15000, 35000
            );
            QRLogin login = new QRLogin(api, "3");
            return login.pollQrLogin(qrcode, timeoutMs,
                    java.util.concurrent.Executors.newSingleThreadExecutor(),
                    null, null);
        } catch (Exception e) {
            return LoginResult.failure("Login failed: " + e.getMessage());
        }
    }
}
