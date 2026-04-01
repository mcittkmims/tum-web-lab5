package md.utm.go2web.http;

import java.util.Map;

public record HttpResponse(
        int statusCode,
        String statusMessage,
        Map<String, String> headers,
        String body,
        boolean cached
) {
    public HttpResponse(int statusCode, String statusMessage, Map<String, String> headers, String body) {
        this(statusCode, statusMessage, headers, body, false);
    }
    public boolean isRedirect() {
        return statusCode == 301 || statusCode == 302 || statusCode == 303
                || statusCode == 307 || statusCode == 308;
    }

    public String contentType() {
        return headers.getOrDefault("content-type", "");
    }

    public boolean isJson() {
        return contentType().contains("application/json");
    }

    public boolean isHtml() {
        return contentType().contains("text/html");
    }

    public String location() {
        return headers.get("location");
    }
}
