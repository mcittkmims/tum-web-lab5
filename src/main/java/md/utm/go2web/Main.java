package md.utm.go2web;

import md.utm.go2web.cli.AppConfig;
import md.utm.go2web.cli.SearchCommand;
import md.utm.go2web.cli.UrlCommand;
import md.utm.go2web.http.HttpClient;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;

@Command(name = "go2web")
public class Main implements Runnable {

    @Option(names = "-h", description = "Show this help message and exit.")
    boolean help;

    @Option(names = "-u", description = "Fetch a URL and print human-readable content.")
    String url;

    @Option(names = "--accept", description = "Preferred content type: JSON, HTML, ANY (default: ANY).")
    HttpClient.ContentPreference accept;

    @Option(names = "-s", description = "Search the web and print top 10 results.",
            arity = "1..*")
    List<String> searchTerms;

    @Option(names = "--engine", description = "Search engine: DUCKDUCKGO (default), YAHOO.")
    String engine;

    public static void main(String[] args) {
        int exit = new CommandLine(new Main()).execute(args);
        System.exit(exit);
    }

    private static final String RESET  = "\033[0m";
    private static final String BOLD   = "\033[1m";
    private static final String CYAN   = "\033[1;36m";
    private static final String YELLOW = "\033[0;33m";
    private static final String DIM    = "\033[2m";

    private static final String HELP =
            BOLD + "Usage:" + RESET + "\n" +
            "  go2web " + CYAN + "-h" + RESET + "\n" +
            "  go2web " + CYAN + "-u" + RESET + " " + YELLOW + "<URL>" + RESET + " " + DIM + "[--accept JSON|HTML]" + RESET + "\n" +
            "  go2web " + CYAN + "-s" + RESET + " " + YELLOW + "<search term>" + RESET + " " + DIM + "[--engine DUCKDUCKGO|YAHOO]" + RESET + "\n" +
            "\n" +
            "  " + CYAN + "-h" + RESET + "                           Show this help message and exit\n" +
            "  " + CYAN + "-u" + RESET + " " + YELLOW + "<URL>" + RESET + "                     Fetch a URL and print human-readable content\n" +
            "      " + CYAN + "--accept" + RESET + " " + YELLOW + "JSON|HTML" + RESET + "         Preferred content type " + DIM + "(default: ANY)" + RESET + "\n" +
            "  " + CYAN + "-s" + RESET + " " + YELLOW + "<search term>" + RESET + "             Search the web and print top 10 results\n" +
            "      " + CYAN + "--engine" + RESET + " " + YELLOW + "DUCKDUCKGO|YAHOO" + RESET + "  Search engine " + DIM + "(default: DUCKDUCKGO)" + RESET + "\n";

    @Override
    public void run() {
        AppConfig config = new AppConfig();
        if (help) {
            System.out.print(HELP);
        } else if (url != null) {
            HttpClient.ContentPreference resolvedAccept = accept != null ? accept : config.accept();
            new UrlCommand().fetchAndPrint(url, resolvedAccept);
        } else if (searchTerms != null) {
            String resolvedEngine = engine != null ? engine : config.engine().name();
            new SearchCommand().runSearch(searchTerms, resolvedEngine);
        } else {
            System.out.print(HELP);
        }
    }
}
