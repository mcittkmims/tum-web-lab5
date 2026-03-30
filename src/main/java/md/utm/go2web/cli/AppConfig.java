package md.utm.go2web.cli;

import md.utm.go2web.http.HttpClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class AppConfig {

    private static final Path CONFIG_FILE =
            Path.of(System.getProperty("go2web.home", System.getProperty("user.home")), "go2web.config");

    private final Map<String, String> values = new HashMap<>();

    public AppConfig() {
        if (Files.exists(CONFIG_FILE)) {
            try {
                for (String line : Files.readAllLines(CONFIG_FILE)) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        values.put(line.substring(0, eq).trim().toLowerCase(),
                                   line.substring(eq + 1).trim());
                    }
                }
            } catch (IOException ignored) {}
        }
    }

    public SearchEngine engine() {
        return SearchEngine.fromString(values.getOrDefault("engine", "duckduckgo"));
    }

    public HttpClient.ContentPreference accept() {
        String val = values.getOrDefault("accept", "ANY").toUpperCase();
        try {
            return HttpClient.ContentPreference.valueOf(val);
        } catch (IllegalArgumentException e) {
            return HttpClient.ContentPreference.ANY;
        }
    }
}
