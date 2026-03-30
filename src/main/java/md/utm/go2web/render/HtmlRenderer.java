package md.utm.go2web.render;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class HtmlRenderer {

    public static String toPlainText(String html) {
        Document doc = Jsoup.parse(html);
        doc.select("script, style, head, noscript, svg, img").remove();
        return doc.body() != null ? doc.body().wholeText().replaceAll("[ \\t]+", " ").trim() : "";
    }

    public record SearchResult(String title, String url) {}

    public static List<SearchResult> extractDuckDuckGoResults(String html) {
        Document doc = Jsoup.parse(html);
        List<SearchResult> results = new ArrayList<>();

        for (Element result : doc.select("div.result")) {
            Element anchor = result.selectFirst("a.result__a");
            if (anchor == null) continue;
            String title = anchor.text().trim();
            String href = anchor.attr("href");
            String url = resolveHref(href);
            if (!title.isEmpty() && !url.isEmpty()) {
                results.add(new SearchResult(title, url));
            }
            if (results.size() == 10) break;
        }

        if (results.isEmpty()) {
            for (Element anchor : doc.select("h2.result__title a")) {
                String title = anchor.text().trim();
                String href = anchor.attr("href");
                String url = resolveHref(href);
                if (!title.isEmpty() && !url.isEmpty()) {
                    results.add(new SearchResult(title, url));
                }
                if (results.size() == 10) break;
            }
        }

        return results;
    }

    public static List<SearchResult> extractYahooResults(String html) {
        Document doc = Jsoup.parse(html);
        List<SearchResult> results = new ArrayList<>();

        for (Element anchor : doc.select("div.algo h3.title a")) {
            String title = anchor.text().trim();
            String href = anchor.attr("href");
            if (!title.isEmpty() && !href.isEmpty()) {
                results.add(new SearchResult(title, href));
            }
            if (results.size() == 10) break;
        }

        return results;
    }

    private static String resolveHref(String href) {
        if (href.startsWith("//duckduckgo.com/l/") || href.contains("uddg=")) {
            try {
                int idx = href.indexOf("uddg=");
                if (idx >= 0) {
                    String encoded = href.substring(idx + 5);
                    int amp = encoded.indexOf('&');
                    if (amp >= 0) encoded = encoded.substring(0, amp);
                    return java.net.URLDecoder.decode(encoded, java.nio.charset.StandardCharsets.UTF_8);
                }
            } catch (Exception ignored) {}
        }
        return href;
    }
}
