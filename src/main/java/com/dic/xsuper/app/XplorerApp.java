package com.dic.xsuper.app;

import com.dic.xsuper.cli.commands.standard.*;
import com.dic.xsuper.cli.commands.xsuper.*;
import com.dic.xsuper.cli.core.CommandRegistry;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

public class XplorerApp {
    private final CommandRegistry registry;
    private Path currentDirectory;

    public XplorerApp() {
        this.registry = new CommandRegistry();
        this.currentDirectory = Paths.get(System.getProperty("user.dir"));

        registry.register(new TimeCmd(registry));
        registry.register(new WatchCmd(registry));
        registry.register(new RunXplCmd(registry));
        registry.register(new CallCmd(registry));
        registry.register(new InfoCmd(registry));
        registry.register(new ListCmd());
        registry.register(new ChangeDirCmd());
        registry.register(new ConvertCmd());
        registry.register(new ZipCmd());
        registry.register(new UnzipCmd());
        registry.register(new ProcessCmd());
        registry.register(new MakeDirCmd());
        registry.register(new DeleteCmd());
        registry.register(new CopyCmd());
        registry.register(new PropsCmd());
        registry.register(new ReadCmd());
        registry.register(new PwdCmd());
        registry.register(new TouchCmd());
        registry.register(new RenameCmd());
        registry.register(new MoveCmd());
        registry.register(new CatCmd());
        registry.register(new WriteCmd());
        registry.register(new AppendCmd());
        registry.register(new OpenCmd());
        registry.register(new SystemCmd());
        registry.register(new EditCmd());
        registry.register(new SearchCmd());
        registry.register(new FindTextCmd());
        registry.register(new CloseCmd());
        registry.register(new CopyCmd());
        registry.register(new DeleteCmd());
        registry.register(new GrepCmd());
        registry.register(new WcCmd());
        registry.register(new ReplaceCmd());
        registry.register(new DuCmd());
        registry.register(new PsCmd());
        registry.register(new KillCmd());
        registry.register(new FindCmd());
        registry.register(new ClearCmd());
        registry.register(new DiffCmd());
        registry.register(new SortCmd());
        registry.register(new TailCmd());
        registry.register(new LnCmd());
        registry.register(new HistoryCmd());
        registry.register(new AliasCmd());
        registry.register(new EnvCmd());
        registry.register(new CryptCmd());
        registry.register(new JoinfCmd());
        registry.register(new PingCmd());
        registry.register(new DfCmd());
        registry.register(new ChmodCmd());
        registry.register(new TreeCmd());
        registry.register(new SedCmd());
        registry.register(new AwkCmd());
        registry.register(new EchoCmd());
        registry.register(new HttpCmd());
    }

    public void boot() {
        System.out.println(ConsoleTheme.HEADER);
        System.out.println("██╗  ██╗██████╗ ██╗      ██████╗ ██████╗ ███████╗██████╗ ");
        System.out.println("╚██╗██╔╝██╔══██╗██║     ██╔═══██╗██╔══██╗██╔════╝██╔══██╗");
        System.out.println(" ╚███╔╝ ██████╔╝██║     ██║   ██║██████╔╝█████╗  ██████╔╝");
        System.out.println(" ██╔██╗ ██╔═══╝ ██║     ██║   ██║██╔══██╗██╔══╝  ██╔══██╗");
        System.out.println("██╔╝ ██╗██║     ███████╗╚██████╔╝██║  ██║███████╗██║  ██║");
        System.out.println("╚═╝  ╚═╝╚═╝     ╚══════╝ ╚═════╝ ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝");
        System.out.println("                  v2.0 - Core Active                     ");
        System.out.println(ConsoleTheme.RESET);
        Scanner scanner = new Scanner(System.in);
        while (true) {
            // Prompt Minimalista estilo Linux/Unix
            System.out.print(ConsoleTheme.PROMPT + "xplorer ~ " + ConsoleTheme.DIRECTORY + Arrays.stream(currentDirectory.toString().split("\\\\")).toList().getLast() + ConsoleTheme.TEXT + " ❯ " + ConsoleTheme.RESET);
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                System.out.println(ConsoleTheme.TEXT + "Shutting down engine..." + ConsoleTheme.RESET);
                break;
            }
            if (input.equalsIgnoreCase("help")) {
                registry.printHelp();
                continue;
            }
            currentDirectory = registry.executeCommand(input, currentDirectory);
        }
        scanner.close();
    }
}