package md.utm.go2web.cli;

import md.utm.go2web.http.HttpClient;
import md.utm.go2web.http.HttpResponse;
import md.utm.go2web.render.HtmlRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Command(name = "-u", description = "Fetch a URL and print human-readable content")
public class UrlCommand implements Runnable {

    @Parameters(index = "0", description = "The URL to fetch")
    private String url;

    private final HttpClient client;

    public UrlCommand() {
        this.client = new HttpClient();
    }

    public UrlCommand(HttpClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        fetchAndPrint(url);
    }

    public void fetchAndPrint(String targetUrl) {
        try {
            HttpResponse response = client.fetch(targetUrl);
            System.out.println(renderResponse(response));
        } catch (Exception e) {
            System.err.println("Error fetching " + targetUrl + ": " + e.getMessage());
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
