package io.github.esyion.wechat.wechat;

import io.github.esyion.wechat.error.WechatApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * WeChat ilink API client implementing 11 endpoints.
 */
public class WechatApiClient {
    private static final Logger log = LoggerFactory.getLogger(WechatApiClient.class);
    private static final String ILINK_APP_ID = "bot";

    private final String baseUrl;
    private final String cdnBaseUrl;
    private String botToken;
    private final String channelVersion;
    private final String botAgent;
    private final int defaultTimeoutMs;
    private final int longPollTimeoutMs;
    private final ObjectMapper mapper;
    private final OkHttpClient httpClient;

    public WechatApiClient(String baseUrl, String cdnBaseUrl, String botToken,
                          String channelVersion, String botAgent,
                          int defaultTimeoutMs, int longPollTimeoutMs) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.cdnBaseUrl = cdnBaseUrl.replaceAll("/+$", "");
        this.botToken = botToken;
        this.channelVersion = channelVersion;
        this.botAgent = botAgent;
        this.defaultTimeoutMs = defaultTimeoutMs;
        this.longPollTimeoutMs = longPollTimeoutMs;
        this.mapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(defaultTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(defaultTimeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(defaultTimeoutMs, TimeUnit.MILLISECONDS)
                .build();
    }

    public void setBotToken(String token) {
        this.botToken = token;
    }

    public String getCdnBaseUrl() {
        return cdnBaseUrl;
    }

    // ==================== HTTP Helpers ====================

    private Headers commonHeaders() {
        return new Headers.Builder()
                .add("Content-Type", "application/json")
                .add("AuthorizationType", "ilink_bot_token")
                .add("X-WECHAT-UIN", randomWechatUin())
                .add("iLink-App-Id", ILINK_APP_ID)
                .add("iLink-App-ClientVersion", "0")
                .build();
    }

    private Headers authHeaders() {
        Headers.Builder builder = commonHeaders().newBuilder();
        if (botToken != null && !botToken.isEmpty()) {
            builder.add("Authorization", "Bearer " + botToken);
        }
        return builder.build();
    }

    private String randomWechatUin() {
        Random r = new Random();
        int u32 = r.nextInt(Integer.MAX_VALUE);
        return java.util.Base64.getEncoder().encodeToString(String.valueOf(u32).getBytes());
    }

    private <T> T postJson(String endpoint, Object body, boolean useAuth, int timeoutMs, Class<T> clazz) throws IOException {
        String url = baseUrl + "/" + endpoint.replaceAll("^/+", "");
        RequestBody requestBody = RequestBody.create(
                mapper.writeValueAsString(body),
                MediaType.parse("application/json")
        );
        Headers headers = useAuth ? authHeaders() : commonHeaders();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .headers(headers)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            log.debug("POST {} -> {}", url, responseBody);
            if (!response.isSuccessful()) {
                throw new IOException("POST " + endpoint + " " + response.code() + ": " + responseBody);
            }
            if (clazz == String.class) {
                return clazz.cast(responseBody);
            }
            return mapper.readValue(responseBody, clazz);
        }
    }

    private <T> T getJson(String endpoint, int timeoutMs, Class<T> clazz) throws IOException {
        String url = baseUrl + "/" + endpoint.replaceAll("^/+", "");
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(commonHeaders())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("GET " + endpoint + " " + response.code() + ": " + responseBody);
            }
            if (clazz == String.class) {
                return clazz.cast(responseBody);
            }
            return mapper.readValue(responseBody, clazz);
        }
    }

    private WechatTypes.BaseInfo baseInfo() {
        WechatTypes.BaseInfo info = new WechatTypes.BaseInfo();
        info.channelVersion = channelVersion;
        info.botAgent = botAgent;
        return info;
    }

    // ==================== Login APIs ====================

    public WechatTypes.GetBotQrcodeResp getBotQrcode(String botType) throws IOException {
        String endpoint = "ilink/bot/get_bot_qrcode?bot_type=" + botType;
        WechatTypes.GetBotQrcodeResp resp = postJson(endpoint,
                Map.of("local_token_list", java.util.Collections.emptyList()),
                false, defaultTimeoutMs, WechatTypes.GetBotQrcodeResp.class);
        return resp;
    }

    public WechatTypes.GetQrcodeStatusResp getQrcodeStatus(String qrcode, String verifyCode, int timeoutMs) throws IOException {
        String endpoint = "ilink/bot/get_qrcode_status?qrcode=" + qrcode;
        if (verifyCode != null && !verifyCode.isEmpty()) {
            endpoint += "&verify_code=" + verifyCode;
        }
        return getJson(endpoint, timeoutMs, WechatTypes.GetQrcodeStatusResp.class);
    }

    // ==================== Lifecycle APIs ====================

    public WechatTypes.NotifyResp notifyStart() throws IOException {
        return postJson("ilink/bot/msg/notifystart",
                Map.of("base_info", baseInfo()),
                true, 10000, WechatTypes.NotifyResp.class);
    }

    public WechatTypes.NotifyResp notifyStop() throws IOException {
        return postJson("ilink/bot/msg/notifystop",
                Map.of("base_info", baseInfo()),
                true, 10000, WechatTypes.NotifyResp.class);
    }

    // ==================== Main Loop APIs ====================

    public WechatTypes.GetUpdatesResp getUpdates(String getUpdatesBuf, int timeoutMs, okhttp3.OkHttpClient httpClient) throws IOException {
        WechatTypes.GetUpdatesReq req = new WechatTypes.GetUpdatesReq();
        req.getUpdatesBuf = getUpdatesBuf;
        req.baseInfo = baseInfo();

        String url = baseUrl + "/ilink/bot/getupdates";
        RequestBody requestBody = RequestBody.create(
                mapper.writeValueAsString(req),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .headers(authHeaders())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            log.debug("POST {} -> {}", url, responseBody);
            if (!response.isSuccessful()) {
                throw new IOException("POST getupdates " + response.code() + ": " + responseBody);
            }
            return mapper.readValue(responseBody, WechatTypes.GetUpdatesResp.class);
        }
    }

    // ==================== Outbound APIs ====================

    public void sendMessage(WechatTypes.SendMessageReq req) throws IOException {
        postJson("ilink/bot/sendmessage", req, true, defaultTimeoutMs, String.class);
    }

    public WechatTypes.GetUploadUrlResp getUploadUrl(WechatTypes.GetUploadUrlReq req) throws IOException {
        return postJson("ilink/bot/getuploadurl", req, true, defaultTimeoutMs, WechatTypes.GetUploadUrlResp.class);
    }

    public WechatTypes.GetConfigResp getConfig(String ilinkUserId, String contextToken) throws IOException {
        Map<String, Object> req = new java.util.HashMap<>();
        req.put("ilink_user_id", ilinkUserId);
        req.put("context_token", contextToken != null ? contextToken : "");
        req.put("base_info", baseInfo());
        return postJson("ilink/bot/getconfig", req, true, 10000, WechatTypes.GetConfigResp.class);
    }

    public void sendTyping(WechatTypes.SendTypingReq req) throws IOException {
        postJson("ilink/bot/sendtyping", req, true, 10000, String.class);
    }

    // ==================== CDN Upload/Download ====================

    public String cdnUpload(String uploadUrl, byte[] data, String contentType) throws IOException {
        RequestBody body = RequestBody.create(data, MediaType.parse(contentType));
        Request request = new Request.Builder()
                .url(uploadUrl)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("CDN upload failed: " + response.code());
            }
            return response.body() != null ? response.body().string() : "";
        }
    }

    public byte[] cdnDownload(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("CDN download failed: " + response.code());
            }
            return response.body() != null ? response.body().bytes() : new byte[0];
        }
    }
}
