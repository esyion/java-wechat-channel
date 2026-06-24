package io.github.esyion.wechat.channel;

import io.github.esyion.wechat.error.WechatApiError;
import io.github.esyion.wechat.store.Store;
import io.github.esyion.wechat.wechat.WechatApiClient;
import io.github.esyion.wechat.wechat.WechatTypes;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Long poll loop with error recovery.
 */
public class LongPoll {
    private static final int SESSION_EXPIRED = -14;
    private static final int CONSECUTIVE_FAILURE_LIMIT = 3;
    private static final long BACKOFF_MS = 30_000;
    private static final long RETRY_DELAY_MS = 2_000;
    private static final long SESSION_PAUSE_MS = 60 * 60 * 1000;

    private final WechatApiClient api;
    private final Store store;
    private final String mediaTmpDir;
    private final java.util.function.BiFunction<ChannelMsg, Reply, CompletableFuture<Void>> onMessage;
    private final java.util.function.BiConsumer<Throwable, java.util.Map<String, Object>> onError;
    private final int longPollTimeoutMs;
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    public LongPoll(WechatApiClient api, Store store, String mediaTmpDir,
                   java.util.function.BiFunction<ChannelMsg, Reply, CompletableFuture<Void>> onMessage,
                   java.util.function.BiConsumer<Throwable, java.util.Map<String, Object>> onError,
                   int longPollTimeoutMs) {
        this.api = api;
        this.store = store;
        this.mediaTmpDir = mediaTmpDir;
        this.onMessage = onMessage;
        this.onError = onError != null ? onError : (e, ctx) -> {};
        this.longPollTimeoutMs = longPollTimeoutMs;
    }

    public void stop() {
        stopped.set(true);
    }

    public CompletableFuture<Void> run() {
        return CompletableFuture.runAsync(() -> {
            try {
                api.notifyStart();
            } catch (Exception e) {
                onError.accept(e, java.util.Map.of("phase", "notifyStart"));
            }

            int consecutive = 0;
            long sessionPausedUntil = 0;
            ExecutorService replyExecutor = Executors.newCachedThreadPool();

            while (!stopped.get()) {
                long now = System.currentTimeMillis();
                if (now < sessionPausedUntil) {
                    try {
                        long sleepMs = sessionPausedUntil - now;
                        TimeUnit.MILLISECONDS.sleep(sleepMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }

                try {
                    String syncBuf = store.get("sync_buf").get();
                    WechatTypes.GetUpdatesResp resp = api.getUpdates(
                            syncBuf != null ? syncBuf : "",
                            longPollTimeoutMs,
                            new okhttp3.OkHttpClient.Builder()
                                    .connectTimeout(longPollTimeoutMs, TimeUnit.MILLISECONDS)
                                    .readTimeout(longPollTimeoutMs, TimeUnit.MILLISECONDS)
                                    .build()
                    );

                    if (resp == null) {
                        onError.accept(new Exception("getUpdates returned null"), java.util.Map.of("phase", "getUpdates"));
                        consecutive++;
                        long wait = consecutive >= CONSECUTIVE_FAILURE_LIMIT ? BACKOFF_MS : RETRY_DELAY_MS;
                        TimeUnit.MILLISECONDS.sleep(wait);
                        continue;
                    }

                    boolean isError = (resp.ret != null && resp.ret != 0) || (resp.errcode != null && resp.errcode != 0);
                    if (isError) {
                        if (resp.errcode != null && resp.errcode == SESSION_EXPIRED
                                || resp.ret != null && resp.ret == SESSION_EXPIRED) {
                            sessionPausedUntil = System.currentTimeMillis() + SESSION_PAUSE_MS;
                            onError.accept(new WechatApiError(resp.ret, resp.errcode, resp.errmsg),
                                    java.util.Map.of("phase", "sessionExpired"));
                            continue;
                        }
                        consecutive++;
                        onError.accept(new WechatApiError(resp.ret, resp.errcode, resp.errmsg),
                                java.util.Map.of("phase", "getUpdates"));
                        long wait = consecutive >= CONSECUTIVE_FAILURE_LIMIT ? BACKOFF_MS : RETRY_DELAY_MS;
                        TimeUnit.MILLISECONDS.sleep(wait);
                        continue;
                    }

                    consecutive = 0;

                    if (resp.getUpdatesBuf != null && !resp.getUpdatesBuf.isEmpty()) {
                        store.set("sync_buf", resp.getUpdatesBuf);
                    }

                    if (resp.msgs != null) {
                        for (WechatTypes.WeixinMessage fullMsg : resp.msgs) {
                            if (stopped.get()) break;

                            String userId = fullMsg.fromUserId != null ? fullMsg.fromUserId : "";
                            if (userId.isEmpty()) continue;

                            String contextToken = fullMsg.contextToken != null ? fullMsg.contextToken : "";
                            if (contextToken.isEmpty()) {
                                try {
                                    String stored = store.get("ctx:" + userId).get();
                                    contextToken = stored != null ? stored : "";
                                } catch (Exception e) {
                                    contextToken = "";
                                }
                            }
                            if (fullMsg.contextToken != null && !fullMsg.contextToken.isEmpty()) {
                                store.set("ctx:" + userId, fullMsg.contextToken);
                            }

                            ChannelMsg msg;
                            try {
                                msg = Inbound.build(api, mediaTmpDir, fullMsg);
                            } catch (Exception e) {
                                onError.accept(e, java.util.Map.of("phase", "inbound"));
                                continue;
                            }

                            ReplyImpl reply = new ReplyImpl(api, userId, contextToken);

                            try {
                                onMessage.apply(msg, reply);
                            } catch (Exception e) {
                                onError.accept(e, java.util.Map.of("phase", "handler"));
                            }
                        }
                    }
                } catch (Exception e) {
                    if (stopped.get()) break;
                    consecutive++;
                    onError.accept(e, java.util.Map.of("phase", "getUpdates"));
                    long wait = consecutive >= CONSECUTIVE_FAILURE_LIMIT ? BACKOFF_MS : RETRY_DELAY_MS;
                    try {
                        TimeUnit.MILLISECONDS.sleep(wait);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            replyExecutor.shutdown();
        });
    }
}
