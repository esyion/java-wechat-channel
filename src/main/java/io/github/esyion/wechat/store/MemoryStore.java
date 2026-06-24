package io.github.esyion.wechat.store;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for testing or single-instance usage.
 */
public class MemoryStore implements Store {
    private final Map<String, String> map = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<String> get(String key) {
        return CompletableFuture.completedFuture(map.get(key));
    }

    @Override
    public CompletableFuture<Void> set(String key, String value) {
        map.put(key, value);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> delete(String key) {
        map.remove(key);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> flush() {
        return CompletableFuture.completedFuture(null);
    }
}
