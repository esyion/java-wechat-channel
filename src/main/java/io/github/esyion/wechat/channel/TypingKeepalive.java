package io.github.esyion.wechat.channel;

import io.github.esyion.wechat.wechat.WechatApiClient;
import io.github.esyion.wechat.wechat.WechatTypes;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Typing keepalive heartbeat.
 */
public class TypingKeepalive {
    private final WechatApiClient api;
    private final String toUserId;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread heartbeatThread;

    public TypingKeepalive(WechatApiClient api, String toUserId) {
        this.api = api;
        this.toUserId = toUserId;
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            heartbeatThread = new Thread(() -> {
                String ticket = null;
                while (running.get()) {
                    try {
                        if (ticket == null) {
                            WechatTypes.GetConfigResp config = api.getConfig(toUserId, "");
                            ticket = config.typingTicket;
                        }
                        if (ticket != null) {
                            WechatTypes.SendTypingReq req = new WechatTypes.SendTypingReq();
                            req.ilinkUserId = toUserId;
                            req.typingTicket = ticket;
                            req.status = WechatTypes.TYPING_STATUS_TYPING;
                            api.sendTyping(req);
                        }
                        Thread.sleep(5000);
                    } catch (Exception e) {
                        if (!running.get()) break;
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            });
            heartbeatThread.setDaemon(true);
            heartbeatThread.start();
        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (heartbeatThread != null) {
                heartbeatThread.interrupt();
            }
            // Send cancel
            try {
                String ticket = api.getConfig(toUserId, "").typingTicket;
                if (ticket != null) {
                    WechatTypes.SendTypingReq req = new WechatTypes.SendTypingReq();
                    req.ilinkUserId = toUserId;
                    req.typingTicket = ticket;
                    req.status = WechatTypes.TYPING_STATUS_CANCEL;
                    api.sendTyping(req);
                }
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    public static void send(WechatApiClient api, String toUserId, boolean on) throws Exception {
        String ticket = api.getConfig(toUserId, "").typingTicket;
        if (ticket == null) return;

        WechatTypes.SendTypingReq req = new WechatTypes.SendTypingReq();
        req.ilinkUserId = toUserId;
        req.typingTicket = ticket;
        req.status = on ? WechatTypes.TYPING_STATUS_TYPING : WechatTypes.TYPING_STATUS_CANCEL;
        api.sendTyping(req);
    }
}
