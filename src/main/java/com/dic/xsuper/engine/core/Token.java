package com.dic.xsuper.engine.core;

public class Token {
    public final TokenType type;
    public final String lexeme; // A string exata que foi lida (ex: "fun" ou "123")
    public final Object literal; // O valor convertido (ex: 123 em Integer)
    public final int line;
    public final int column;
    public String filePath;

    public Token(TokenType type, String lexeme, Object literal, int line, int column, String filePath) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
        this.column = column;
        this.filePath = filePath;
    }
    public Token(TokenType type, String lexeme, Object literal, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
        this.column = column;

    }

    @Override
    public String toString() {
        return String.format("[%s '%s' L:%d C:%d]", type.name(), lexeme, line, column);
    }
}