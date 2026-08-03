package com.dic.xsuper.cli.commands.xsuper;

import com.dic.xsuper.app.XplBootstrapper;
import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.cli.core.CommandRegistry;

import com.dic.xsuper.engine.analysis.SemanticAnalyzer;
import com.dic.xsuper.engine.analysis.SemanticError;
import com.dic.xsuper.engine.core.Lexer;
import com.dic.xsuper.engine.core.Parser;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.core.Token;
import com.dic.xsuper.engine.ast.Stmt;
import com.dic.xsuper.render.javafx.core.SuperUiEngine;
import com.dic.xsuper.render.javafx.core.XplUiBridge;
import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

        //System.out.println(ConsoleTheme.TEXT + ">> 1. A ler ficheiro: " + target.getFileName() + ConsoleTheme.RESET);
        String sourceCode = Files.readString(target);
        //System.out.println(ConsoleTheme.TEXT + ">> 2. Tamanho do código: " + sourceCode.length() + " bytes lidos." + ConsoleTheme.RESET);

        try {
            Lexer lexer = new Lexer(sourceCode, target.toAbsolutePath().toString());
            List<Token> tokens = lexer.tokenize();
            /*for (Token a: tokens){
                System.out.println(ConsoleTheme.TEXT + a + ConsoleTheme.RESET);
            }*/
            //System.out.println(ConsoleTheme.SUCCESS + ">> 3. Lexer: " + tokens.size() + " tokens extraídos." + ConsoleTheme.RESET);
            Parser parser = new Parser(tokens);

            List<Stmt> statements = parser.parse();
            /*for (Stmt stms : statements) {
                System.out.println(ConsoleTheme.TEXT + stms.toString() + ConsoleTheme.RESET);
            }*/
            //System.out.println(ConsoleTheme.SUCCESS + ">> 4. Parser: " + statements.size() + " declarações geradas na AST." + ConsoleTheme.RESET);

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



                //System.out.println(ConsoleTheme.TEXT + ">> 5. A iniciar Interpretador..." + ConsoleTheme.RESET);
                Interpreter interpreter = new Interpreter(this.registry, currentDirectory);
                interpreter.activeBreakpoints.addAll(breakpoints);

                XplBootstrapper.bootstrap(interpreter);


                // ─── ADICIONA ESTAS LINHAS ──────────────────────────────────────────────
                // 2. Cria uma ponte silenciosa (Headless Bridge) para o terminal não crashar
                XplUiBridge headlessBridge = new XplUiBridge() {
                    @Override public void renderView(XplNode root) {}
                    @Override public void updateProperty(String id, String prop, Object val) {
                    }
                    @Override public void setEngineCallback(EngineCallback callback) {}

                    @Override
                    public void rebuildFullView(String targetId, XplNode virtualNode) {}

                    @Override
                    public void invokeMethodOnNode(String targetId, String methodName, Object[] args) {
                    }

                    @Override public void reportError(String message) {
                        System.err.println("Erro UI: " + message);
                    }
                };

                // 3. Instancia a Engine passando o interpretador que vai correr o ficheiro.
                // Isto vai automaticamente injetar '__ui_engine', 'document' e 'ui' nas globais!
                SuperUiEngine uiEngine = new SuperUiEngine(interpreter, headlessBridge);
                // ────────────────────────────────────────────────────────────────────────


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
                    return currentDirectory; // Bloqueia a execução antes do Interpretador arrancar!
                }
                // =========================================================================

                interpreter.interpret(statements);
                //System.out.println(ConsoleTheme.SUCCESS + ">> 6. Execução concluída com sucesso!" + ConsoleTheme.RESET);
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