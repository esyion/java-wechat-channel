package com.esyion.wechat.store;

/**
 * Session persistence interface.
 * Keys with "sync_buf" and "ctx:" prefixes are reserved for library use.
 */
public interface Store {
    /**
     * Get a value by key.
     * @return the value, or null if not found
     */
    java.util.concurrent.CompletableFuture<String> get(String key);

    /**
     * Set a value for a key.
     */
    java.util.concurrent.CompletableFuture<Void> set(String key, String value);

    /**
     * Delete a key.
     */
    java.util.concurrent.CompletableFuture<Void> delete(String key);

    /**
     * Flush pending writes to durable storage.
     */
    java.util.concurrent.CompletableFuture<Void> flush();
}
