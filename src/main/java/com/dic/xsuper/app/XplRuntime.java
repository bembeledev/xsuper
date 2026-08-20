package com.dic.xsuper.app;

import com.dic.xsuper.cli.core.CommandRegistry;
import com.dic.xsuper.engine.analysis.SemanticAnalyzer;
import com.dic.xsuper.engine.analysis.SemanticError;
import com.dic.xsuper.engine.core.Lexer;
import com.dic.xsuper.engine.core.Parser;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.core.Token;
import com.dic.xsuper.engine.ast.Stmt;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class XplRuntime {

     static void main(String[] args) {
        // O Explorer vai passar o caminho do ficheiro como o primeiro argumento (args[0])
        if (args.length == 0) {
            System.err.println(ConsoleTheme.ERROR + "Erro: Nenhum ficheiro XPL especificado." + ConsoleTheme.RESET);
            System.err.println("Uso: java XplRuntime <caminho_para_ficheiro.xpl>");
            return;
        }

        // 1. Captura e normaliza o caminho absoluto enviado pelo Windows
        Path target = Paths.get(args[0]).normalize().toAbsolutePath();

        if (!Files.exists(target)) {
            System.err.println(ConsoleTheme.ERROR + "Ficheiro não encontrado: " + target + ConsoleTheme.RESET);
            return;
        }

        try {
            String sourceCode = Files.readString(target);

            // 2. Lexer & Parser (Aproveitando a tua lógica original)
            Lexer lexer = new Lexer(sourceCode, target.toString());
            List<Token> tokens = lexer.tokenize();

            Parser parser = new Parser(tokens);
            List<Stmt> statements = parser.parse();

            // Muralha de Segurança
            if (parser.hasErrors()) {
                System.err.println(ConsoleTheme.ERROR +
                        "\n>> Execução abortada: Foram encontrados " + parser.getErrorCount() +
                        " erro(s) sintático(s) no código fonte." + ConsoleTheme.RESET);
                return;
            }

            if (statements.isEmpty()) {
                System.out.println(ConsoleTheme.WARNING + ">> AVISO: A AST está vazia. Não há nada para executar!" + ConsoleTheme.RESET);
                return;
            }

            // 3. Preparar o Ambiente de Execução
            CommandRegistry registry = new CommandRegistry();

            // ⭐ O DETECTIVE DA RAIZ DO PROJECTO ⭐
            // Precisamos de encontrar a pasta onde está o sdm.lock (a raiz do projecto FileManager),
            // e não apenas a pasta onde o script main.xpl está guardado.
            Path scriptDir = target.getParent();
            if (scriptDir == null) scriptDir = Paths.get("");

            Path projectRoot = scriptDir;
            while (projectRoot != null) {
                if (Files.exists(projectRoot.resolve("sdm.lock")) || Files.exists(projectRoot.resolve("package.spm"))) {
                    break; // Encontrou a raiz do projecto (onde está o cofre)!
                }
                projectRoot = projectRoot.getParent();
            }

            // Se for um script avulso sem projeto, usa a pasta onde o terminal foi aberto
            if (projectRoot == null) {
                projectRoot = Paths.get("").toAbsolutePath().normalize();
            }

            // Exemplo de como capturar no teu RunXplCmd.java
            List<Integer> breakpoints = new ArrayList<>();

            for (String arg : args) {
                if (arg.startsWith("--breakpoints=")) {
                    // Corta o prefixo e apanha apenas "6,15,42"
                    String numbersStr = arg.substring("--breakpoints=".length());

                    // Divide pela vírgula e converte para Inteiros
                    for (String num : numbersStr.split(",")) {
                        try {
                            breakpoints.add(Integer.parseInt(num.trim()));
                        } catch (NumberFormatException e) {
                            // Ignora lixo ou formatação errada
                        }
                    }
                }
            }

            Interpreter interpreter = new Interpreter(registry, projectRoot);
            interpreter.activeBreakpoints.addAll(breakpoints);
            XplBootstrapper.bootstrap(interpreter);

            // =========================================================================
            // ⭐ A NOVA MURALHA DE SEGURANÇA 2: LIMPEZA SEMÂNTICA MINUCIOSA
            // =========================================================================
            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(interpreter);
            List<SemanticError> semanticErrors = semanticAnalyzer.analyze(statements);

            if (!semanticErrors.isEmpty()) {
                System.err.println(ConsoleTheme.ERROR + "\n>> Execução abortada: Falha na Análise Semântica (" + semanticErrors.size() + " erro(s) encontrados)." + ConsoleTheme.RESET);

                for (SemanticError err : semanticErrors) {
                    String path = err.token.filePath != null ? err.token.filePath : target.toString();
                    System.err.println(path + ":" + err.token.line + ":" + err.token.column + ":\n\tErro Lógico: " + err.getMessage());
                }
                return; // Bloqueia a execução antes do Interpretador arrancar!
            }
            // =========================================================================

            // 5. Correr o código!
            interpreter.interpret(statements);

        } catch (Exception e) {
            System.err.println(ConsoleTheme.ERROR + ">> ERRO FATAL: " + e + ConsoleTheme.RESET);
            e.printStackTrace(System.out);
        }
    }
}