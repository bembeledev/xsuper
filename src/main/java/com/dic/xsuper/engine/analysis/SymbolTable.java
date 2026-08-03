package com.dic.xsuper.engine.analysis;

import com.dic.xsuper.engine.core.Token;
import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    // O escopo pai (para suportar variáveis globais vs locais)
    public final SymbolTable enclosing;

    // A nossa "memória" só guarda os metadados da variável!
    private final Map<String, VariableInfo> symbols = new HashMap<>();

    public SymbolTable(SymbolTable enclosing) {
        this.enclosing = enclosing;
    }

    // Regista uma nova variável no escopo (O nosso antigo 'defineLet')
    public void define(String name, String type, boolean isConst, boolean isInitialized) {
        symbols.put(name, new VariableInfo(type, isConst, isInitialized));
    }

    // Verifica se a variável existe e devolve o TIPO dela
    public String getType(Token name) {
        if (symbols.containsKey(name.lexeme)) {
            return symbols.get(name.lexeme).type;
        }

        if (enclosing != null) {
            return enclosing.getType(name);
        }

        // Se não existir, reportamos erro semântico!
        // (De momento devolvemos "any" para o analisador não crashar o Java)
        return null;
    }

    // Tenta reatribuir um valor. Usado para bloquear alterações a 'const'!
    public boolean checkAssignment(Token name) {
        if (symbols.containsKey(name.lexeme)) {
            VariableInfo info = symbols.get(name.lexeme);
            if (info.isConst) {
                return false; // Erro! Tentou escrever num const!
            }
            info.isInitialized = true;
            return true;
        }

        if (enclosing != null) {
            return enclosing.checkAssignment(name);
        }

        return false; // Variável não encontrada
    }

    // Estrutura interna para guardar a "alma" da variável
    public static class VariableInfo {
        public String type;
        public boolean isConst;
        public boolean isInitialized;

        public VariableInfo(String type, boolean isConst, boolean isInitialized) {
            this.type = type;
            this.isConst = isConst;
            this.isInitialized = isInitialized;
        }
    }
}