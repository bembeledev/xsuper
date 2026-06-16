package com.dic.xsuper.app;

import com.dic.xsuper.commands.standard.*;
import com.dic.xsuper.commands.xsuper.*;
import com.dic.xsuper.core.CommandRegistry;
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

        // Registar Comandos Standard
        registry.register(new ListCmd());
        registry.register(new ChangeDirCmd());

        // Registar Superpoderes
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
        // Em Categoria 1 (Manipulação) & Categoria 3 (Info)
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


        // Adiciona as novas ferramentas no construtor
        registry.register(new TimeCmd(this.registry));
        registry.register(new WatchCmd(this.registry));
        registry.register(new CryptCmd());
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
            System.out.print(ConsoleTheme.PROMPT + "xplorer ~" + ConsoleTheme.DIRECTORY + Arrays.stream(currentDirectory.toString().split("\\\\")).toList().getLast() + ConsoleTheme.TEXT + " ❯ " + ConsoleTheme.RESET);
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