package com.esyion.wechat;

import com.esyion.wechat.channel.ChannelHandle;
import com.esyion.wechat.channel.ChannelMsg;
import com.esyion.wechat.channel.CreateChannelOpts;
import com.esyion.wechat.channel.Reply;
import com.esyion.wechat.wechat.LoginResult;
import com.esyion.wechat.wechat.QRLogin;
import com.esyion.wechat.wechat.WechatApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Example demonstrating wechat-channel usage.
 */
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        log.info("WeChat Channel Java SDK");
        log.info("======================");

        // ===== Step 1: QR Code Login =====
        log.info("Step 1: Requesting QR code...");

        WechatApiClient api = new WechatApiClient(
                "https://ilinkai.weixin.qq.com",
                "https://novac2c.cdn.weixin.qq.com/c2c",
                null,  // no token needed for login
                "wechat-channel/0.1.0",
                "wechat-channel/0.1.0",
                15000, 35000
        );

        QRLogin login = new QRLogin(api, "3");
        QRLogin.QrCodeResult qr = login.requestQrCode();

        log.info("QR Code content: {}", qr.qrcode);
        log.info("QR Image content (base64): {}...", qr.qrcodeImgContent.substring(0, Math.min(50, qr.qrcodeImgContent.length())));
        log.info("Please scan the QR code with WeChat app.");

        // ===== Step 2: Wait for Login =====
        log.info("\nStep 2: Waiting for login...");

        LoginResult result = login.pollQrLogin(qr.qrcode, 120000, null, null, null);

        if (!result.connected) {
            log.error("Login failed: {}", result.message);
            return;
        }

        log.info("Login successful!");
        log.info("  botToken: {}", result.botToken);
        log.info("  accountId: {}", result.accountId);
        log.info("  baseUrl: {}", result.baseUrl);

        // ===== Step 3: Create Channel =====
        log.info("\nStep 3: Creating channel...");

        ChannelHandle handle = WechatChannelProvider.createChannel(
                new CreateChannelOpts()
                        .botToken(result.botToken)
                        .accountId(result.accountId)
                        .baseUrl(result.baseUrl)
                        .onMessage((ChannelMsg msg, Reply reply) -> {
                            log.info("\n[Message] from {}: {}", msg.fromUserId, msg.text);

                            // Reply with text
                            reply.text("I received: " + msg.text);

                            // Or reply with media
                            // reply.media("/path/to/image.png");

                            return CompletableFuture.completedFuture(null);
                        })
                        .onError((err, ctx) -> {
                            log.error("[Error] phase={}: {}", ctx.get("phase"), err.getMessage());
                        })
        );

        // ===== Step 4: Start =====
        log.info("\nStep 4: Starting channel...");
        handle.start();

        log.info("Channel running. Press Ctrl+C to stop.");

        // ===== Step 5: Graceful Shutdown =====
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("\nShutting down...");
            handle.stop();
        }));

        // Keep main thread alive
        Thread.currentThread().join();
    }
}
