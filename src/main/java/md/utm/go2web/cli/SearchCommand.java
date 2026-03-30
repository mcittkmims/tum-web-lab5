package md.utm.go2web.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;

@Command(name = "-s", description = "Search the web and print top 10 results")
public class SearchCommand implements Runnable {

    @Parameters(index = "0..*", description = "Search terms")
    private List<String> terms;

    @Override
    public void run() {
        System.err.println("Search not yet implemented");
    }
}
