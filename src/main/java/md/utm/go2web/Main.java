package md.utm.go2web;

import md.utm.go2web.cli.SearchCommand;
import md.utm.go2web.cli.UrlCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "go2web",
        mixinStandardHelpOptions = true,
        version = "go2web 1.0",
        description = "Make HTTP requests and search the web from the command line.",
        subcommands = {UrlCommand.class, SearchCommand.class}
)
public class Main implements Runnable {

    public static void main(String[] args) {
        int exit = new CommandLine(new Main()).execute(args);
        System.exit(exit);
    }

    @Override
    public void run() {
        new CommandLine(this).usage(System.out);
    }
}
