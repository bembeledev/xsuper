package com.dic.xsuper.dom.html;

/**
 * A unidade fundamental de informação extraída do teu ficheiro HTML/UI.
 */
public class HtmlToken {
    public final HtmlTokenType type; // O tipo (ex: AT_IF, IDENTIFIER, STRING)
    public final String lexeme;      // O texto exato capturado (ex: "div", "@if", "meu-id")
    public final Object literal;     // O valor processado (útil para strings sem as aspas, ex: meu-id)
    public final int line;           // ⭐ CRÍTICO para a XplSystemBridge reportar erros na interface!

    public HtmlToken(HtmlTokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    /**
     * Facilita a depuração visual quando fizeres print da lista de tokens.
     */
    @Override
    public String toString() {
        if (literal != null) {
            return type + " '" + lexeme + "' [" + literal + "]";
        }
        return type + " '" + lexeme + "'";
    }
}