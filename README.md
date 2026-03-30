# go2web

A command-line HTTP client built on raw TCP sockets (no HTTP libraries). Makes HTTP/HTTPS requests, follows redirects, parses HTML to plain text, caches responses, and searches the web.

## Requirements

- Java 17+
- Maven 3.6+

## Build

```bash
mvn clean package -q
chmod +x go2web
```

## Usage

```
go2web -h                          # show help
go2web -u <URL>                    # fetch URL and print human-readable content
go2web -u <URL> --accept JSON      # prefer JSON response
go2web -u <URL> --accept HTML      # prefer HTML response
go2web -s <search term>            # search DuckDuckGo, print top 10 results
go2web -s <search term> --engine yahoo     # search Yahoo
go2web -s <search term> --engine duckduckgo  # search DuckDuckGo (default)
```

## Examples

```bash
# Fetch a webpage as plain text
./go2web -u https://example.com

# Fetch a JSON API endpoint
./go2web -u https://httpbin.org/get --accept JSON

# Follows HTTP redirects automatically (e.g. http → https)
./go2web -u http://google.com

# Search DuckDuckGo and optionally open a result
./go2web -s "java socket programming"

# Search Yahoo
./go2web -s "java socket programming" --engine yahoo
```

## Config File

Set the default search engine in `~/.go2web-config`:

```
engine=yahoo
```

Supported values: `duckduckgo` (default), `yahoo`.

The `--engine` flag always overrides the config file.

## Cache

Responses are cached in `~/.go2web-cache/` with a 1-hour TTL.

## Technical Notes

- HTTP and HTTPS requests use `java.net.Socket` and `javax.net.ssl.SSLSocket` — no HTTP libraries.
- Redirects (301, 302, 303, 307, 308) are followed up to 10 hops.
- HTML is rendered to plain text using [jsoup](https://jsoup.org/).
- JSON responses are pretty-printed using [Jackson](https://github.com/FasterXML/jackson).
- CLI parsing uses [picocli](https://picocli.info/).
