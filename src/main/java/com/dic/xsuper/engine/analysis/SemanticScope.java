package com.dic.xsuper.engine.analysis;

import com.dic.xsuper.engine.core.Token;
import java.util.HashMap;
import java.util.Map;

public class SemanticScope {
    public final SemanticScope enclosing;

    // Guarda os metadados da variável: O Tipo (String) e se é Mutável (boolean)
    private final Map<String, SymbolInfo> symbols = new HashMap<>();

    public SemanticScope(SemanticScope enclosing) {
        this.enclosing = enclosing;
    }

    public void define(Token name, String type, boolean isMutable) {
        if (symbols.containsKey(name.lexeme)) {
            // Lança erro se tentar redeclarar a mesma variável no mesmo bloco!
            throw new SemanticError(name, "A variável '" + name.lexeme + "' já foi declarada neste escopo.");
        }
        symbols.put(name.lexeme, new SymbolInfo(type, isMutable, name));
    }

    public SymbolInfo resolve(Token name) {
        if (symbols.containsKey(name.lexeme)) {
            return symbols.get(name.lexeme);
        }
        if (enclosing != null) {
            return enclosing.resolve(name);
        }
        throw new SemanticError(name, "A variável '" + name.lexeme + "' não existe ou ainda não foi declarada.");
    }

    // Estrutura interna para guardar os dados da variável
    public static class SymbolInfo {
        public final String type;
        public final boolean isMutable;
        public final Token declarationToken;

        public SymbolInfo(String type, boolean isMutable, Token declarationToken) {
            this.type = type;
            this.isMutable = isMutable;
            this.declarationToken = declarationToken;
        }
    }
}