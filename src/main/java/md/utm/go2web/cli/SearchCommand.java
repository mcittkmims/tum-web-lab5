package md.utm.go2web.cli;

import md.utm.go2web.http.HttpClient;
import md.utm.go2web.http.HttpResponse;
import md.utm.go2web.render.HtmlRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

@Command(name = "-s", description = "Search DuckDuckGo and print top 10 results")
public class SearchCommand implements Runnable {

    @Parameters(index = "0..*", description = "Search terms")
    private List<String> terms;

    private final HttpClient client;

    public SearchCommand() {
        this.client = new HttpClient();
    }

    public SearchCommand(HttpClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        if (terms == null || terms.isEmpty()) {
            System.err.println("Please provide a search term.");
            return;
        }

        String query = String.join(" ", terms);
        List<HtmlRenderer.SearchResult> results = search(query);

        if (results.isEmpty()) {
            System.out.println("No results found.");
            return;
        }

        for (int i = 0; i < results.size(); i++) {
            HtmlRenderer.SearchResult r = results.get(i);
            System.out.printf("%d. %s%n   %s%n", i + 1, r.title(), r.url());
        }

        System.out.print("\nEnter number to open a result (or press Enter to exit): ");
        Scanner scanner = new Scanner(System.in);
        String input;
        try {
            input = scanner.nextLine().trim();
        } catch (java.util.NoSuchElementException e) {
            return;
        }
        if (!input.isEmpty()) {
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= results.size()) {
                    String url = results.get(choice - 1).url();
                    System.out.println("\nFetching: " + url + "\n");
                    new UrlCommand(client).fetchAndPrint(url);
                } else {
                    System.err.println("Invalid selection.");
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid input.");
            }
        }
    }

    private List<HtmlRenderer.SearchResult> search(String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "http://html.duckduckgo.com/html/?q=" + encoded;
        try {
            HttpResponse response = client.fetch(url);
            return HtmlRenderer.extractDuckDuckGoResults(response.body());
        } catch (Exception e) {
            System.err.println("Search failed: " + e.getMessage());
            return List.of();
        }
    }
}
