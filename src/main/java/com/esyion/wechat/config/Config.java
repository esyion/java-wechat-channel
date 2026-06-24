package com.esyion.wechat.config;

import java.util.Map;

/**
 * Configuration properties for WeChat channel.
 */
public class Config {
    private String baseUrl = "https://ilinkai.weixin.qq.com";
    private String cdnBaseUrl = "https://novac2c.cdn.weixin.qq.com/c2c";
    private String channelVersion = "wechat-channel/0.1.0";
    private String stateDir = System.getProperty("user.home") + "/.wechat-channel";
    private int longPollTimeoutMs = 35000;
    private String mediaTmpDir;

    public Config() {}

    public Config(String prefix) {
        loadFromEnv(prefix);
    }

    public void loadFromEnv(String prefix) {
        Map<String, String> env = System.getenv();
        if (env.containsKey(prefix + "BASE_URL")) {
            this.baseUrl = env.get(prefix + "BASE_URL");
        }
        if (env.containsKey(prefix + "CDN_BASE_URL")) {
            this.cdnBaseUrl = env.get(prefix + "CDN_BASE_URL");
        }
        if (env.containsKey(prefix + "STATE_DIR")) {
            this.stateDir = env.get(prefix + "STATE_DIR");
        }
        if (env.containsKey(prefix + "LONG_POLL_TIMEOUT_MS")) {
            this.longPollTimeoutMs = Integer.parseInt(env.get(prefix + "LONG_POLL_TIMEOUT_MS"));
        }
        this.mediaTmpDir = this.stateDir + "/media";
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getCdnBaseUrl() {
        return cdnBaseUrl;
    }

    public void setCdnBaseUrl(String cdnBaseUrl) {
        this.cdnBaseUrl = cdnBaseUrl;
    }

    public String getChannelVersion() {
        return channelVersion;
    }

    public void setChannelVersion(String channelVersion) {
        this.channelVersion = channelVersion;
    }

    public String getStateDir() {
        return stateDir;
    }

    public void setStateDir(String stateDir) {
        this.stateDir = stateDir;
    }

    public int getLongPollTimeoutMs() {
        return longPollTimeoutMs;
    }

    public void setLongPollTimeoutMs(int longPollTimeoutMs) {
        this.longPollTimeoutMs = longPollTimeoutMs;
    }

    public String getMediaTmpDir() {
        return mediaTmpDir;
    }

    public void setMediaTmpDir(String mediaTmpDir) {
        this.mediaTmpDir = mediaTmpDir;
    }
}
