# go2web

`go2web` is a command-line HTTP client built for the Web Development laboratory. It performs HTTP and HTTPS requests over raw sockets, renders HTML into readable terminal output, supports simple web search, follows redirects, and stores cache entries on disk.

## Overview

The project intentionally avoids built-in or third-party HTTP client libraries for network requests. Instead, it implements request construction, socket communication, response parsing, redirect handling, and cache management directly in Java.

## Features

- Fetches `http://` and `https://` URLs from the terminal
- Supports content negotiation with `--accept JSON` and `--accept HTML`
- Renders HTML responses into human-readable text
- Pretty-prints JSON responses
- Searches the web and prints the top 10 results
- Supports DuckDuckGo and Yahoo search
- Follows redirects up to 10 hops
- Persists cached responses on disk

## Tech Stack

- Java 17
- Maven
- Picocli for CLI parsing
- Jsoup for HTML-to-text rendering
- Jackson for cache metadata and JSON formatting

## Project Structure

```text
.
├── go2web
├── scripts/
├── src/main/java/md/utm/go2web/
│   ├── cache/
│   ├── cli/
│   ├── http/
│   └── render/
├── src/main/resources/
├── pom.xml
└── README.md
```

## Build From Source

Requirements:

- Java 17 or newer
- Maven 3.6 or newer

Build the shaded jar:

```bash
mvn clean package
```

Run the launcher script from the repository root:

```bash
chmod +x go2web
./go2web -h
```

## Usage

```bash
go2web -h
go2web -u https://example.com
go2web -u https://httpbin.org/json --accept JSON
go2web -u https://example.com --accept HTML
go2web -s "java socket programming"
go2web -s "java socket programming" --engine yahoo
```

## Configuration

When the launcher script is used, the application stores config and cache relative to the launcher location.

Supported config file:

```text
go2web.config
```

Example:

```text
engine=yahoo
accept=JSON
```

Supported `engine` values:

- `duckduckgo`
- `yahoo`

Supported `accept` values:

- `ANY`
- `HTML`
- `JSON`

Command-line flags override config values.

## Cache

Cached responses are stored in:

```text
.go2web-cache/
```

Entries use a default TTL of 1 hour unless the server provides a cache lifetime through HTTP headers.

## Notes

- Redirects are followed automatically for common 3xx responses.
- HTTPS requests use `SSLSocket` with HTTP/1.1 over TLS.
- The repository also contains install and uninstall scripts intended for packaged distributions.

## Repository

- GitHub: <https://github.com/mcittkmims/tum-web-lab5>
