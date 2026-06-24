package com.esyion.wechat.wechat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

/**
 * QR code login flow.
 */
public class QRLogin {
    private final WechatApiClient api;
    private final String botType;

    public QRLogin(WechatApiClient api, String botType) {
        this.api = api;
        this.botType = botType != null ? botType : "3";
    }

    /**
     * Request a fresh QR code.
     */
    public QrCodeResult requestQrCode() throws IOException {
        WechatTypes.GetBotQrcodeResp resp = api.getBotQrcode(botType);
        if (resp.qrcode == null || resp.qrcodeImgContent == null) {
            throw new IOException("Failed to fetch QR code");
        }
        return new QrCodeResult(resp.qrcode, resp.qrcodeImgContent);
    }

    /**
     * Poll QR status until terminal state.
     */
    public LoginResult pollQrLogin(String qrcode, int timeoutMs, ExecutorService executor,
                                    java.util.function.Function<String, CompletableFuture<String>> onVerifyCode,
                                    java.util.function.BiConsumer<String, java.util.Map<String, Object>> onStatus) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        String pendingVerifyCode = null;
        int refreshCount = 0;
        String currentQrcode = qrcode;

        while (System.currentTimeMillis() < deadline) {
            try {
                WechatTypes.GetQrcodeStatusResp status = api.getQrcodeStatus(
                        currentQrcode,
                        pendingVerifyCode,
                        35000
                );

                String s = status.status;
                if (onStatus != null) {
                    java.util.Map<String, Object> info = new java.util.HashMap<>();
                    info.put("botId", status.ilinkBotId);
                    info.put("hasBotToken", status.botToken != null);
                    onStatus.accept(s, info);
                }

                switch (s) {
                    case QrStatus.WAIT:
                        break;
                    case QrStatus.SCANED:
                        pendingVerifyCode = null;
                        break;
                    case QrStatus.NEED_VERIFYCODE:
                        if (onVerifyCode != null) {
                            try {
                                String prompt = pendingVerifyCode != null
                                        ? "You entered the wrong code. Please retry:"
                                        : "Enter the 6-digit code shown on WeChat:";
                                pendingVerifyCode = onVerifyCode.apply(prompt).get(60, TimeUnit.SECONDS);
                            } catch (Exception e) {
                                return LoginResult.failure("Verify code error: " + e.getMessage());
                            }
                        } else {
                            return LoginResult.failure("Server requested verify code but no handler provided");
                        }
                        continue;
                    case QrStatus.EXPIRED:
                    case QrStatus.VERIFY_CODE_BLOCKED:
                        refreshCount++;
                        if (refreshCount > 3) {
                            return LoginResult.failure("QR expired " + refreshCount + " times. Please retry later.");
                        }
                        try {
                            WechatTypes.GetBotQrcodeResp refreshed = api.getBotQrcode(botType);
                            currentQrcode = refreshed.qrcode;
                            pendingVerifyCode = null;
                        } catch (IOException e) {
                            // keep polling
                        }
                        break;
                    case QrStatus.BINDED_REDIRECT:
                        return LoginResult.failure("Already connected to this instance.");
                    case QrStatus.SCANED_BUT_REDIRECT:
                        if (status.redirectHost != null) {
                            return LoginResult.failure("IDC redirect required to " + status.redirectHost + ". Please re-run login.");
                        }
                        break;
                    case QrStatus.CONFIRMED:
                        if (status.ilinkBotId == null) {
                            return LoginResult.failure("Login confirmed but ilink_bot_id missing");
                        }
                        return LoginResult.success(
                                status.botToken,
                                status.ilinkBotId,
                                status.baseUrl != null ? status.baseUrl : api.getCdnBaseUrl(),
                                status.ilinkUserId
                        );
                }

                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return LoginResult.failure("Interrupted");
            } catch (IOException e) {
                // Network error - treat as wait, keep polling
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return LoginResult.failure("Interrupted");
                }
            }
        }

        return LoginResult.failure("Login timed out");
    }

    public static class QrCodeResult {
        public final String qrcode;
        public final String qrcodeImgContent;

        public QrCodeResult(String qrcode, String qrcodeImgContent) {
            this.qrcode = qrcode;
            this.qrcodeImgContent = qrcodeImgContent;
        }
    }
}
