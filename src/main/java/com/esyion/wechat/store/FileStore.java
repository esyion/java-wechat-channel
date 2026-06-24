package com.esyion.wechat.store;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON file-based store with atomic writes.
 */
public class FileStore implements Store {
    private final Path filePath;
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private volatile boolean dirty = false;

    public FileStore(String filePath) {
        this.filePath = Paths.get(filePath);
        load();
    }

    private void load() {
        if (!Files.exists(filePath)) {
            return;
        }
        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            if (content != null && !content.isBlank()) {
                synchronized (cache) {
                    cache.putAll(parseJson(content));
                }
            }
        } catch (IOException e) {
            // Ignore, start with empty store
        }
    }

    @Override
    public CompletableFuture<String> get(String key) {
        synchronized (cache) {
            return CompletableFuture.completedFuture(cache.get(key));
        }
    }

    @Override
    public CompletableFuture<Void> set(String key, String value) {
        synchronized (cache) {
            cache.put(key, value);
            dirty = true;
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> delete(String key) {
        synchronized (cache) {
            cache.remove(key);
            dirty = true;
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> flush() {
        synchronized (cache) {
            if (!dirty) {
                return CompletableFuture.completedFuture(null);
            }
            try {
                String json = toJson(cache);
                // Write to temp file first, then rename for atomicity
                Path tempFile = filePath.resolveSibling(filePath.getFileName() + ".tmp");
                Files.writeString(tempFile, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                dirty = false;
            } catch (IOException e) {
                return CompletableFuture.failedFuture(e);
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private Map<String, String> parseJson(String json) {
        Map<String, String> result = new ConcurrentHashMap<>();
        if (json == null || json.isBlank() || json.equals("{}")) {
            return result;
        }
        // Simple JSON parser for flat string-string map
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            return result;
        }
        json = json.substring(1, json.length() - 1);
        if (json.isBlank()) {
            return result;
        }
        int i = 0;
        while (i < json.length()) {
            // Skip whitespace
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= json.length()) break;

            // Parse key
            if (json.charAt(i) != '"') break;
            i++;
            int keyStart = i;
            while (i < json.length() && json.charAt(i) != '"') i++;
            String key = json.substring(keyStart, i);
            i++; // skip closing quote

            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= json.length() || json.charAt(i) != ':') break;
            i++; // skip colon

            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= json.length()) break;

            // Parse value
            String value;
            if (json.charAt(i) == '"') {
                i++;
                int valueStart = i;
                while (i < json.length() && json.charAt(i) != '"') i++;
                value = json.substring(valueStart, i);
                i++; // skip closing quote
            } else {
                int valueStart = i;
                while (i < json.length() && json.charAt(i) != ',' && json.charAt(i) != '}') i++;
                value = json.substring(valueStart, i).trim();
                if (value.equals("null")) value = null;
            }

            result.put(key, value);

            while (i < json.length() && (json.charAt(i) == ',' || Character.isWhitespace(json.charAt(i)))) i++;
        }
        return result;
    }

    private String toJson(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            if (entry.getValue() == null) {
                sb.append("null");
            } else {
                sb.append("\"").append(escapeJson(entry.getValue())).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
