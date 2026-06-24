package com.dic.xsuper.commands.xsuper;

import com.dic.xsuper.core.Command;
import com.dic.xsuper.core.CommandRegistry;
import com.dic.xsuper.lang.Lexer;
import com.dic.xsuper.lang.Parser;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.Token;
import com.dic.xsuper.lang.Stmt;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class RunXplCmd implements Command {

    private final CommandRegistry registry;

    public RunXplCmd(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() { return "run"; }

    @Override
    public String getDescription() { return "Executa um script XPL. Uso: run <script.xpl>"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 2) {
            System.out.println(ConsoleTheme.WARNING + "Uso correto: run <script.xpl>" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        Path target = currentDirectory.resolve(args[1]).normalize();

        if (!Files.exists(target)) {
            System.out.println(ConsoleTheme.ERROR + "Ficheiro não encontrado: " + target.toAbsolutePath() + ConsoleTheme.RESET);
            return currentDirectory;
        }

        System.out.println(ConsoleTheme.TEXT + ">> 1. A ler ficheiro: " + target.getFileName() + ConsoleTheme.RESET);
        String sourceCode = Files.readString(target);
        System.out.println(ConsoleTheme.TEXT + ">> 2. Tamanho do código: " + sourceCode.length() + " bytes lidos." + ConsoleTheme.RESET);

        try {
            Lexer lexer = new Lexer(sourceCode, target.toAbsolutePath().toString());
            List<Token> tokens = lexer.tokenize();
            /*for (Token a: tokens){
                System.out.println(ConsoleTheme.TEXT + a + ConsoleTheme.RESET);
            }*/
            System.out.println(ConsoleTheme.SUCCESS + ">> 3. Lexer: " + tokens.size() + " tokens extraídos." + ConsoleTheme.RESET);
            Parser parser = new Parser(tokens);

            List<Stmt> statements = parser.parse();
            /*for (Stmt stms : statements) {
                System.out.println(ConsoleTheme.TEXT + stms.toString() + ConsoleTheme.RESET);
            }*/
            System.out.println(ConsoleTheme.SUCCESS + ">> 4. Parser: " + statements.size() + " declarações geradas na AST." + ConsoleTheme.RESET);

            // ⭐ A TUA NOVA MURALHA DE SEGURANÇA!
            if (parser.hasErrors()) {
                System.err.println(ConsoleTheme.ERROR +
                        "\n>> Execução abortada: Foram encontrados " + parser.getErrorCount() +
                        " erro(s) sintático(s) no código fonte. Corrija-os antes de rodar o motor." +
                        ConsoleTheme.RESET);
                return currentDirectory; // 🛑 CORTA AQUI! O interpretador nunca será chamado!
            }

            if (statements.isEmpty()) {
                System.out.println(ConsoleTheme.WARNING + ">> AVISO: A AST está vazia. Não há nada para executar!" + ConsoleTheme.RESET);
            } else {
                System.out.println(ConsoleTheme.TEXT + ">> 5. A iniciar Interpretador..." + ConsoleTheme.RESET);
                Interpreter interpreter = new Interpreter(this.registry, currentDirectory);
                interpreter.interpret(statements);
                System.out.println(ConsoleTheme.SUCCESS + ">> 6. Execução concluída com sucesso!" + ConsoleTheme.RESET);
                return interpreter.currentDirectory;
            }

        } catch (Exception e) {
            System.out.println(ConsoleTheme.ERROR + ">> ERRO FATAL: " + e.toString() + ConsoleTheme.RESET);
            // Isto vai garantir que a StackTrace aparece SEMPRE, mesmo que a mensagem seja nula!
            e.printStackTrace(System.out);
        }

        return currentDirectory;
    }
}