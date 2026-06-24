package com.esyion.wechat;

import com.esyion.wechat.channel.ChannelHandle;
import com.esyion.wechat.channel.CreateChannelOpts;
import com.esyion.wechat.wechat.LoginResult;
import com.esyion.wechat.wechat.QRLogin;
import com.esyion.wechat.wechat.WechatApiClient;

/**
 * Example demonstrating wechat-channel usage.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("WeChat Channel Java SDK");
        System.out.println("======================");
        System.out.println();

        // ===== Step 1: QR Code Login =====
        System.out.println("Step 1: Requesting QR code...");

        WechatApiClient api = new WechatApiClient(
                "https://ilinkai.weixin.qq.com",
                "https://novac2c.cdn.weixin.qq.com/c2c",
                null,  // no token needed for login
                "wechat-channel/0.1.0",
                "wechat-channel/0.1.0",
                15000, 15000
        );

        QRLogin login = new QRLogin(api, "3");
        QRLogin.QrCodeResult qr = login.requestQrCode();

        System.out.println("QR Code content: " + qr.qrcode);
        System.out.println("QR Image content (base64): " + qr.qrcodeImgContent.substring(0, 50) + "...");
        System.out.println("Please scan the QR code with WeChat app.");

        // ===== Step 2: Wait for Login =====
        System.out.println("\nStep 2: Waiting for login...");

        LoginResult result = login.pollQrLogin(qr.qrcode, 120000, null, null, null);

        if (!result.connected) {
            System.out.println("Login failed: " + result.message);
            return;
        }

        System.out.println("Login successful!");
        System.out.println("  botToken: " + result.botToken);
        System.out.println("  accountId: " + result.accountId);
        System.out.println("  baseUrl: " + result.baseUrl);

        // ===== Step 3: Create Channel =====
        System.out.println("\nStep 3: Creating channel...");

        ChannelHandle handle = WechatChannelProvider.createChannel(
                new CreateChannelOpts()
                        .botToken(result.botToken)
                        .accountId(result.accountId)
                        .baseUrl(result.baseUrl)
                        .onMessage((msg, reply) -> {
                            System.out.println("\n[Message] from " + msg.fromUserId + ": " + msg.text);

                            // Reply with text
                            reply.text("I received: " + msg.text);

                            // Or reply with media
                            // reply.media("/path/to/image.png");
                        })
                        .onError((err, ctx) -> {
                            System.err.println("[Error] phase=" + ctx.get("phase") + ": " + err);
                        })
        );

        // ===== Step 4: Start =====
        System.out.println("\nStep 4: Starting channel...");
        handle.start();

        System.out.println("Channel running. Press Ctrl+C to stop.");

        // ===== Step 5: Graceful Shutdown =====
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down...");
            handle.stop();
        }));
    }
}
