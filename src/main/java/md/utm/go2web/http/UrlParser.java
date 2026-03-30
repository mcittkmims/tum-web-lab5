package md.utm.go2web.http;

public class UrlParser {

    public record ParsedUrl(String scheme, String host, int port, String pathAndQuery) {}

    public static ParsedUrl parse(String rawUrl) {
        String url = rawUrl.trim();

        String scheme;
        if (url.startsWith("https://")) {
            scheme = "https";
            url = url.substring(8);
        } else if (url.startsWith("http://")) {
            scheme = "http";
            url = url.substring(7);
        } else {
            scheme = "http";
        }

        int defaultPort = scheme.equals("https") ? 443 : 80;

        int slashIdx = url.indexOf('/');
        String hostAndPort;
        String pathAndQuery;

        if (slashIdx == -1) {
            hostAndPort = url;
            pathAndQuery = "/";
        } else {
            hostAndPort = url.substring(0, slashIdx);
            pathAndQuery = url.substring(slashIdx);
            if (pathAndQuery.isEmpty()) pathAndQuery = "/";
        }

        int colonIdx = hostAndPort.indexOf(':');
        String host;
        int port;
        if (colonIdx == -1) {
            host = hostAndPort;
            port = defaultPort;
        } else {
            host = hostAndPort.substring(0, colonIdx);
            port = Integer.parseInt(hostAndPort.substring(colonIdx + 1));
        }

        return new ParsedUrl(scheme, host, port, pathAndQuery);
    }
}
