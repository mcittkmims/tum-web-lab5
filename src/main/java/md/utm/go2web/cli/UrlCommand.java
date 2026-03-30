package md.utm.go2web.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import md.utm.go2web.http.HttpClient;
import md.utm.go2web.http.HttpResponse;
import md.utm.go2web.render.HtmlRenderer;

public class UrlCommand {

    private final HttpClient client;

    public UrlCommand() {
        this.client = new HttpClient();
    }

    public UrlCommand(HttpClient client) {
        this.client = client;
    }

    public void fetchAndPrint(String url) {
        fetchAndPrint(url, HttpClient.ContentPreference.ANY);
    }

    public void fetchAndPrint(String url, HttpClient.ContentPreference preference) {
        try {
            HttpResponse response = client.fetch(url, preference);
            System.out.println(renderResponse(response));
        } catch (Exception e) {
            System.err.println("Error fetching " + url + ": " + e.getMessage());
        }
    }

    public static String renderResponse(HttpResponse response) {
        if (response.isJson()) {
            try {
                ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
                Object json = mapper.readValue(response.body(), Object.class);
                return mapper.writeValueAsString(json);
            } catch (Exception ignored) {}
        }
        if (response.isHtml()) {
            return HtmlRenderer.toPlainText(response.body());
        }
        return response.body();
    }
}
