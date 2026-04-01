package md.utm.go2web.cli;

import md.utm.go2web.http.HttpClient;
import md.utm.go2web.http.HttpResponse;
import md.utm.go2web.render.HtmlRenderer;

import java.util.List;
import java.util.Scanner;

public class SearchCommand {

    private final HttpClient client;

    public SearchCommand() {
        this.client = new HttpClient();
    }

    public SearchCommand(HttpClient client) {
        this.client = client;
    }

    public void runSearch(List<String> terms, String engineName) {
        SearchEngine engine = resolveEngine(engineName);
        String query = String.join(" ", terms);
        List<HtmlRenderer.SearchResult> results = search(query, engine);

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

    private List<HtmlRenderer.SearchResult> search(String query, SearchEngine engine) {
        String url = engine.buildUrl(query);
        try {
            HttpResponse response = client.fetch(url);
            if (response.cached()) System.out.println("[cached]");
            return engine.parseResults(response);
        } catch (Exception e) {
            System.err.println("Search failed: " + e.getMessage());
            return List.of();
        }
    }

    private SearchEngine resolveEngine(String engineName) {
        return SearchEngine.fromString(engineName);
    }
}
