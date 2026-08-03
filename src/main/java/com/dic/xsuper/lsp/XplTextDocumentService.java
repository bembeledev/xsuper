package com.dic.xsuper.lsp;

import com.dic.xsuper.engine.core.Lexer;
import com.dic.xsuper.engine.core.Parser;
import com.dic.xsuper.engine.core.Token;
import com.dic.xsuper.engine.ast.Stmt;
import com.dic.xsuper.engine.exceptions.ControlFlow;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class XplTextDocumentService implements TextDocumentService {

    private LanguageClient client;

    public void setClient(LanguageClient client) {
        this.client = client;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        analyzeCode(params.getTextDocument().getUri(), params.getTextDocument().getText());
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        // O utilizador digitou algo. Analisamos o texto completo instantaneamente.
        analyzeCode(params.getTextDocument().getUri(), params.getContentChanges().get(0).getText());
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        // Limpa os erros quando o ficheiro é fechado
        client.publishDiagnostics(new PublishDiagnosticsParams(params.getTextDocument().getUri(), new ArrayList<>()));
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) { }

    // =====================================================================
    // ⭐ A MAGIA DO LINTER: CONECTADO AO TEU MOTOR REAL ⭐
    // =====================================================================
    private void analyzeCode(String fileUri, String sourceCode) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        try {
            // 1. ANÁLISE LÉXICA (Exatamente como no teu XplRuntime)
            Lexer lexer = new Lexer(sourceCode, fileUri);
            List<Token> tokens = lexer.tokenize();

            // 2. ANÁLISE SINTÁTICA
            Parser parser = new Parser(tokens);
            List<Stmt> statements = parser.parse();

            // 3. CAPTURA DE ERROS SINTÁTICOS (Linhas Vermelhas no VSCode)
            if (parser.hasErrors()) {
                // Se o teu parser tiver um método para devolver a lista de erros:
                // (Adiciona este getter no Parser se ainda não existir)

                for (ControlFlow.RuntimeError error : parser.getErrors()) {
                    diagnostics.add(createDiagnostic(error));
                }

                // Se não quiseres alterar o Parser agora, podes enviar um erro genérico:
                /*diagnostics.add(new Diagnostic(
                        new Range(new Position(0, 0), new Position(0, 1)),
                        "O compilador encontrou " + parser.getErrorCount() + " erro(s) de sintaxe. Verifica o teu código.",
                        DiagnosticSeverity.Error,
                        "xpl-parser"
                ));*/
            }

        } catch (ControlFlow.RuntimeError e) {
            // Erros fatais que interrompam o Lexer ou Parser
            diagnostics.add(createDiagnostic(e));

        } catch (Exception e) {
            // Falhas Críticas do Sistema Java
            diagnostics.add(new Diagnostic(
                    new Range(new Position(0, 0), new Position(0, 1)),
                    "Falha Crítica no Compilador: " + e.getMessage(),
                    DiagnosticSeverity.Error,
                    "xpl-core"
            ));
        }

        // ⭐ ENVIA PARA O VSCODE DESENHAR AS LINHAS! ⭐
        if (client != null) {
            client.publishDiagnostics(new PublishDiagnosticsParams(fileUri, diagnostics));
        }
    }

    // Método Auxiliar para converter os teus Erros XPL em Alertas do VSCode
    private Diagnostic createDiagnostic(ControlFlow.RuntimeError e) {
        int line = e.token != null ? Math.max(0, e.token.line - 1) : 0;
        int col = e.token != null ? Math.max(0, e.token.column - 1) : 0;
        int length = (e.token != null && e.token.lexeme != null) ? e.token.lexeme.length() : 1;

        Range range = new Range(
                new Position(line, col),
                new Position(line, col + length)
        );

        return new Diagnostic(range, e.getMessage(), DiagnosticSeverity.Error, "xpl-linter");
    }
}