package md.utm.go2web.http;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpClient {

    private static final int MAX_REDIRECTS = 10;
    private static final int TIMEOUT_MS = 15_000;

    public HttpResponse fetch(String url) throws IOException {
        return fetchWithRedirects(url, MAX_REDIRECTS);
    }

    private HttpResponse fetchWithRedirects(String url, int remainingRedirects) throws IOException {
        UrlParser.ParsedUrl parsed = UrlParser.parse(url);

        String requestLine = "GET " + parsed.pathAndQuery() + " HTTP/1.1\r\n";
        String headers =
                "Host: " + parsed.host() + "\r\n" +
                "Accept: application/json, text/html;q=0.9, */*;q=0.8\r\n" +
                "Accept-Encoding: identity\r\n" +
                "Connection: close\r\n" +
                "User-Agent: go2web/1.0\r\n" +
                "\r\n";
        String rawRequest = requestLine + headers;

        HttpResponse response = sendRawRequest(parsed, rawRequest);

        if (response.isRedirect() && remainingRedirects > 0) {
            String location = response.location();
            if (location == null || location.isBlank()) {
                return response;
            }
            if (!location.startsWith("http://") && !location.startsWith("https://")) {
                location = parsed.scheme() + "://" + parsed.host()
                        + (parsed.port() != (parsed.scheme().equals("https") ? 443 : 80)
                        ? ":" + parsed.port() : "")
                        + (location.startsWith("/") ? location : "/" + location);
            }
            return fetchWithRedirects(location, remainingRedirects - 1);
        }

        return response;
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
