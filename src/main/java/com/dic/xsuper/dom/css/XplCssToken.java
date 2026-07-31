package com.dic.xsuper.dom.css;

public class XplCssToken {
    public final XplCssTokenType type;
    public final String lexeme;
    public final Object literal;
    public final int line;

    public XplCssToken(XplCssTokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    @Override
    public String toString() {
        if (literal != null) {
            return type + " '" + lexeme + "' [" + literal + "]";
        }
        return type + " '" + lexeme + "'";
    }
}