package com.dic.xsuper.dom.event.eventbus;

import com.dic.xsuper.engine.ast.Stmt;
import com.dic.xsuper.engine.core.*;
import com.dic.xsuper.render.javafx.core.SuperUiEngine;
import com.dic.xsuper.dom.node.XplElement;
import com.dic.xsuper.dom.event.XplEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilitário para executar scripts XPL inline provenientes de eventos HTML.
 * Isola a lógica de preparação do ambiente, execução e limpeza.
 */
public final class InlineScriptExecutor {

    private final Interpreter interpreter;
    private final SuperUiEngine engine;

    public InlineScriptExecutor(Interpreter interpreter, SuperUiEngine engine) {
        this.interpreter = interpreter;
        this.engine = engine;
    }

    /**
     * Executa um script XPL inline (ex: "area(12,45); values();").
     * Injecção automática da variável 'event' e das propriedades do componente host.
     *
     * @param scriptCallback Código XPL a executar (ex: "login(); logout();")
     * @param event          Objecto XplEvent com os detalhes do evento (target, payload, etc.)
     */
    public void execute(String scriptCallback, XplEvent event) {
        if (scriptCallback == null || scriptCallback.trim().isEmpty()) {
            System.err.println("[InlineScript] Script vazio ou nulo.");
            return;
        }

        // 1. Prepara o script com ponto-e-vírgula final
        String finalScript = scriptCallback.trim();
        if (!finalScript.endsWith(";")) {
            finalScript += ";";
        }

        // 2. Injectar a variável 'event' no ambiente global (temporariamente)
        interpreter.globals.defineVar("event", event);

        // 3. Se houver um elemento alvo no evento, injectar as propriedades do host (componente)
        List<String> injectedVars = new ArrayList<>();
        if (event.target instanceof XplElement targetElement) {

            // Lemos o hostComponent DIRECTAMENTE do XplElement!
            if (targetElement.hostComponent != null) {
                var host = targetElement.hostComponent;

                // Injectar métodos do componente
                if (host.klass != null) {
                    for (String methodName : host.klass.model.methods.keySet()) {
                        Token dummyToken = new Token(TokenType.IDENTIFIER, methodName, null, 0, 0, null);
                        Object boundMethod = host.get(dummyToken);
                        interpreter.globals.defineLet(methodName, boundMethod);
                        injectedVars.add(methodName);
                    }
                }

                // Injectar campos do componente (variáveis)
                for (String fieldName : host.fields.keySet()) {
                    interpreter.globals.defineLet(fieldName, host.fields.get(fieldName));
                    injectedVars.add(fieldName);
                }
            }
        }

        // 4. Executar o script
        try {
            Lexer lexer = new Lexer(finalScript, "Inline_Event");
            List<Token> tokens = lexer.tokenize();
            Parser parser = new Parser(tokens);
            List<Stmt> statements = parser.parse();

            for (Stmt stmt : statements) {
                if (stmt instanceof Stmt.ExpressionStmt exprStmt) {
                    interpreter.evaluate(exprStmt.expression);
                } else {
                    interpreter.execute(stmt);
                }
            }
        } catch (Exception e) {
            System.err.println("[InlineScript] Erro ao executar: " + scriptCallback);
            e.printStackTrace();
        } finally {
            // 5. Limpeza: remover variáveis injectadas
            interpreter.globals.values.remove("event");
            for (String var : injectedVars) {
                interpreter.globals.values.remove(var);
            }
        }
    }

    /**
     * Overload que aceita um callback e um ID para construir o evento automaticamente.
     */
    public void execute(String scriptCallback, String targetId, Object payload) {
        XplEvent event = new XplEvent("custom", engine.getDocument().getElementById(targetId));
        event.detail.put("payload", payload);
        event.detail.put("targetId", targetId);
        execute(scriptCallback, event);
    }
}