package md.utm.go2web.http;

import md.utm.go2web.cache.FileCache;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HttpClient {

    private static final int MAX_REDIRECTS = 10;
    private static final int TIMEOUT_MS = 15_000;

    private final FileCache cache;

    public HttpClient() {
        FileCache c = null;
        try {
            c = new FileCache();
        } catch (IOException ignored) {}
        this.cache = c;
    }

    public HttpClient(FileCache cache) {
        this.cache = cache;
    }

    /** Preferred content types in order, e.g. "application/json" or "text/html" */
    public enum ContentPreference {
        JSON("application/json, text/html;q=0.9, */*;q=0.8"),
        HTML("text/html, application/json;q=0.9, */*;q=0.8"),
        ANY("*/*");

        public final String acceptHeader;
        ContentPreference(String acceptHeader) { this.acceptHeader = acceptHeader; }
    }

    public HttpResponse fetch(String url) throws IOException {
        return fetch(url, ContentPreference.ANY);
    }

    public HttpResponse fetch(String url, ContentPreference preference) throws IOException {
        String prefKey = preference.name();
        if (cache != null) {
            String cached = cache.get(url, prefKey);
            if (cached != null) {
                String contentType = cache.getContentType(url, prefKey);
                return new HttpResponse(200, "OK", Map.of("content-type", contentType), cached, true);
            }
        }
        HttpResponse response = fetchWithRedirects(url, MAX_REDIRECTS, preference);
        if (cache != null && response.statusCode() == 200) {
            cache.put(url, prefKey, response.body(), response.contentType());
        }
        return response;
    }

    private HttpResponse fetchWithRedirects(String url, int remainingRedirects,
                                             ContentPreference preference) throws IOException {
        UrlParser.ParsedUrl parsed = UrlParser.parse(url);

        String requestLine = "GET " + parsed.pathAndQuery() + " HTTP/1.1\r\n";
        String headers =
                "Host: " + parsed.host() + "\r\n" +
                "Accept: " + preference.acceptHeader + "\r\n" +
                "Accept-Encoding: identity\r\n" +
                "Connection: close\r\n" +
                "User-Agent: go2web/1.0\r\n" +
                "\r\n";
        String rawRequest = requestLine + headers;

        HttpResponse response = sendRawRequest(parsed, rawRequest);

        if (response.isRedirect()) {
            if (remainingRedirects == 0) {
                throw new IOException("Too many redirects (max " + MAX_REDIRECTS + ")");
            }
            String location = response.location();
            if (location == null || location.isBlank()) {
                return response;
            }
            location = resolveLocation(location, parsed);
            return fetchWithRedirects(location, remainingRedirects - 1, preference);
        }

        return response;
    }

    private String resolveLocation(String location, UrlParser.ParsedUrl base) {
        if (location.startsWith("http://") || location.startsWith("https://")) {
            return location;
        }
        // Protocol-relative: //host/path
        if (location.startsWith("//")) {
            return base.scheme() + ":" + location;
        }
        int defaultPort = base.scheme().equals("https") ? 443 : 80;
        String portPart = base.port() != defaultPort ? ":" + base.port() : "";
        String baseOrigin = base.scheme() + "://" + base.host() + portPart;
        // Absolute path
        if (location.startsWith("/")) {
            return baseOrigin + location;
        }
        // Relative path — resolve against current path directory
        String currentPath = base.pathAndQuery();
        int lastSlash = currentPath.lastIndexOf('/');
        String dir = lastSlash >= 0 ? currentPath.substring(0, lastSlash + 1) : "/";
        return baseOrigin + dir + location;
    }

    private HttpResponse sendRawRequest(UrlParser.ParsedUrl parsed, String rawRequest)
            throws IOException {
        try (Socket socket = openSocket(parsed.scheme(), parsed.host(), parsed.port())) {
            socket.setSoTimeout(TIMEOUT_MS);

            OutputStream out = socket.getOutputStream();
            out.write(rawRequest.getBytes(StandardCharsets.US_ASCII));
            out.flush();

            return readResponse(socket.getInputStream());
        }
    }

    private Socket openSocket(String scheme, String host, int port) throws IOException {
        if ("https".equals(scheme)) {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket ssl = (SSLSocket) factory.createSocket(host, port);
            SSLParameters params = ssl.getSSLParameters();
            params.setServerNames(List.of(new SNIHostName(host)));
            params.setApplicationProtocols(new String[]{"http/1.1"});
            ssl.setSSLParameters(params);
            ssl.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
            ssl.startHandshake();
            return ssl;
        }
        return new Socket(host, port);
    }

    private HttpResponse readResponse(InputStream in) throws IOException {
        // Buffer bytes to parse status line and headers, then hand off body reading
        PushbackInputStream pis = new PushbackInputStream(in, 1);

        // Read status line
        String statusLine = readLine(pis);
        if (statusLine == null || statusLine.isEmpty()) {
            throw new IOException("Empty response from server");
        }
        String[] statusParts = statusLine.split(" ", 3);
        int statusCode = Integer.parseInt(statusParts[1].trim());
        String statusMessage = statusParts.length > 2 ? statusParts[2].trim() : "";

        // Read headers
        Map<String, String> headers = new LinkedHashMap<>();
        String line;
        while ((line = readLine(pis)) != null && !line.isEmpty()) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                String key = line.substring(0, colon).trim().toLowerCase();
                String value = line.substring(colon + 1).trim();
                headers.put(key, value);
            }
        }

        // Determine charset from Content-Type
        Charset charset = extractCharset(headers.getOrDefault("content-type", ""));

        // Read body
        String body;
        String transferEncoding = headers.getOrDefault("transfer-encoding", "");
        String contentLengthStr = headers.get("content-length");

        if ("chunked".equalsIgnoreCase(transferEncoding)) {
            body = readChunkedBody(pis, charset);
        } else if (contentLengthStr != null) {
            int length = Integer.parseInt(contentLengthStr.trim());
            body = readFixedBody(pis, length, charset);
        } else {
            body = readUntilEof(pis, charset);
        }

        return new HttpResponse(statusCode, statusMessage, headers, body);
    }

    private String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\r') {
                int next = in.read();
                if (next != '\n') {
                    // push back if not LF
                    if (in instanceof PushbackInputStream pis) pis.unread(next);
                }
                break;
            }
            if (b == '\n') break;
            sb.append((char) b);
        }
        return b == -1 && sb.isEmpty() ? null : sb.toString();
    }

    private String readChunkedBody(InputStream in, Charset charset) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        while (true) {
            String sizeLine = readLine(in);
            if (sizeLine == null) break;
            // strip chunk extensions
            int semicolon = sizeLine.indexOf(';');
            if (semicolon >= 0) sizeLine = sizeLine.substring(0, semicolon);
            int chunkSize = Integer.parseInt(sizeLine.trim(), 16);
            if (chunkSize == 0) break;
            byte[] chunk = in.readNBytes(chunkSize);
            result.write(chunk);
            readLine(in); // trailing CRLF after chunk data
        }
        return result.toString(charset);
    }

    private String readFixedBody(InputStream in, int length, Charset charset) throws IOException {
        byte[] buf = in.readNBytes(length);
        return new String(buf, charset);
    }

    private String readUntilEof(InputStream in, Charset charset) throws IOException {
        return new String(in.readAllBytes(), charset);
    }

    private Charset extractCharset(String contentType) {
        String lower = contentType.toLowerCase();
        int idx = lower.indexOf("charset=");
        if (idx >= 0) {
            String cs = contentType.substring(idx + 8).trim();
            int end = cs.indexOf(';');
            if (end >= 0) cs = cs.substring(0, end);
            cs = cs.trim().replace("\"", "");
            try {
                return Charset.forName(cs);
            } catch (Exception ignored) {}
        }
        return StandardCharsets.UTF_8;
    }
}
