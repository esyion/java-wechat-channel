package io.github.esyion.wechat.channel;

import io.github.esyion.wechat.store.Store;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Create channel options.
 */
public class CreateChannelOpts {
    public String botToken;
    public String accountId;
    public String baseUrl;
    public String cdnBaseUrl;
    public String channelVersion;
    public String botAgent;
    public String botType;
    public String stateDir;
    public Store store;
    public BiFunction<ChannelMsg, Reply, CompletableFuture<Void>> onMessage;
    public BiConsumer<Throwable, java.util.Map<String, Object>> onError;
    public int longPollTimeoutMs;
    public String mediaTmpDir;
    public Set<String> blockedUsers;

    public CreateChannelOpts botToken(String botToken) {
        this.botToken = botToken;
        return this;
    }

    public CreateChannelOpts accountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public CreateChannelOpts baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public CreateChannelOpts cdnBaseUrl(String cdnBaseUrl) {
        this.cdnBaseUrl = cdnBaseUrl;
        return this;
    }

    public CreateChannelOpts channelVersion(String channelVersion) {
        this.channelVersion = channelVersion;
        return this;
    }

    public CreateChannelOpts botAgent(String botAgent) {
        this.botAgent = botAgent;
        return this;
    }

    public CreateChannelOpts botType(String botType) {
        this.botType = botType;
        return this;
    }

    public CreateChannelOpts stateDir(String stateDir) {
        this.stateDir = stateDir;
        return this;
    }

    public CreateChannelOpts store(Store store) {
        this.store = store;
        return this;
    }

    public CreateChannelOpts onMessage(BiFunction<ChannelMsg, Reply, CompletableFuture<Void>> onMessage) {
        this.onMessage = onMessage;
        return this;
    }

    public CreateChannelOpts onError(BiConsumer<Throwable, java.util.Map<String, Object>> onError) {
        this.onError = onError;
        return this;
    }

    public CreateChannelOpts longPollTimeoutMs(int longPollTimeoutMs) {
        this.longPollTimeoutMs = longPollTimeoutMs;
        return this;
    }

    public CreateChannelOpts mediaTmpDir(String mediaTmpDir) {
        this.mediaTmpDir = mediaTmpDir;
        return this;
    }

    public CreateChannelOpts blockedUsers(Set<String> blockedUsers) {
        this.blockedUsers = blockedUsers;
        return this;
    }
}
