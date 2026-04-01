package md.utm.go2web.cache;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

public class FileCache {

    private static final long TTL_SECONDS = 3600;
    private static final Path CACHE_DIR =
            Path.of(System.getProperty("go2web.home", System.getProperty("user.home")), ".go2web-cache");

    private final ObjectMapper mapper = new ObjectMapper();

    public FileCache() throws IOException {
        Files.createDirectories(CACHE_DIR);
    }

    public String get(String url, String preference) {
        String key = keyFor(url, preference);
        Path bodyFile = CACHE_DIR.resolve(key);
        Path metaFile = CACHE_DIR.resolve(key + ".meta");
        if (!Files.exists(bodyFile) || !Files.exists(metaFile)) return null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = mapper.readValue(metaFile.toFile(), Map.class);
            long cachedAt = ((Number) meta.get("cachedAt")).longValue();
            if (System.currentTimeMillis() / 1000 - cachedAt > TTL_SECONDS) {
                Files.deleteIfExists(bodyFile);
                Files.deleteIfExists(metaFile);
                return null;
            }
            return Files.readString(bodyFile, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public String getContentType(String url, String preference) {
        String key = keyFor(url, preference);
        Path metaFile = CACHE_DIR.resolve(key + ".meta");
        if (!Files.exists(metaFile)) return "";
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = mapper.readValue(metaFile.toFile(), Map.class);
            return (String) meta.getOrDefault("contentType", "");
        } catch (Exception e) {
            return "";
        }
    }

    public void put(String url, String preference, String body, String contentType) {
        String key = keyFor(url, preference);
        Path bodyFile = CACHE_DIR.resolve(key);
        Path metaFile = CACHE_DIR.resolve(key + ".meta");
        try {
            Files.writeString(bodyFile, body, StandardCharsets.UTF_8);
            Map<String, Object> meta = Map.of(
                    "contentType", contentType != null ? contentType : "",
                    "cachedAt", System.currentTimeMillis() / 1000
            );
            mapper.writeValue(metaFile.toFile(), meta);
        } catch (IOException e) {
            // cache write failure is non-fatal
        }
    }

    public static String keyFor(String url, String preference) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String key = url + "\0" + (preference != null ? preference : "ANY");
            byte[] hash = md.digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
