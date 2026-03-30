package md.utm.go2web.cli;

import md.utm.go2web.http.HttpResponse;
import md.utm.go2web.render.HtmlRenderer;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public enum SearchEngine {
    DUCKDUCKGO {
        @Override
        public String buildUrl(String query) {
            return "http://html.duckduckgo.com/html/?q="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8);
        }

        @Override
        public List<HtmlRenderer.SearchResult> parseResults(HttpResponse response) {
            return HtmlRenderer.extractDuckDuckGoResults(response.body());
        }
    },

    YAHOO {
        @Override
        public String buildUrl(String query) {
            return "https://search.yahoo.com/search?p="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8);
        }

        @Override
        public List<HtmlRenderer.SearchResult> parseResults(HttpResponse response) {
            return HtmlRenderer.extractYahooResults(response.body());
        }
    };

    public abstract String buildUrl(String query);

    public abstract List<HtmlRenderer.SearchResult> parseResults(HttpResponse response);

    public static SearchEngine fromString(String name) {
        if (name == null) return DUCKDUCKGO;
        return switch (name.trim().toLowerCase()) {
            case "yahoo" -> YAHOO;
            default -> DUCKDUCKGO;
        };
    }
}
