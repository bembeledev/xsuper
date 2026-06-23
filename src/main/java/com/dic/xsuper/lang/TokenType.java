package com.dic.xsuper.lang;

public enum TokenType {
    // Palavras-chave (Keywords)
    LET, VAR, CONST, FUN,
    FOR, IN, IF, ELSE, BREAK, CONTINUE,
    RETURN,

    // Tipos de Dados
    T_INT, T_FLOAT, T_STRING, T_ARRAY, T_OBJECT, T_ENUM,

    // Operadores e Símbolos (Single & Double character)
    LPAREN, RPAREN,      // ( )
    LBRACE, RBRACE,      // { }
    LBRACKET, RBRACKET,  // [ ]
    COMMA, DOT, COLON, SEMICOLON, // , . : ;

    PLUS, MINUS, STAR, SLASH,     // + - * /
    ASSIGN, EQUAL, NOT_EQUAL,     // = == !=
    LESS, GREATER, LESS_EQUAL, GREATER_EQUAL, // < > <= >=

    PLUS_PLUS, MINUS_MINUS, HASH, // ++, --, #
    PLUS_ASSIGN, MINUS_ASSIGN, HASH_ASSIGN, // +=, -=, #=,
    STAR_ASSIGN, SLASH_ASSIGN, // *=, /=

    POWER, // x*x
    MODULO, MODULO_ASSIGN, // %, %=
    FAT_ARROW, // =>
    // Literais e Identificadores
    IDENTIFIER,
    INT_LITERAL,
    FLOAT_LITERAL,
    STRING_LITERAL,
    TRUE,
    FALSE,
    NEW,
    // Controlo Interno
    EOF, NULL, ERROR,

    // --- Estrutura de Dados e Contratos ---
    INTERFACE,
    DECLARE,
    IMPLEMENT,
    EXTENDS,

    // --- Modificadores de Acesso ---
    PUB,
    PROTECTED,
    PRIVATE, AS, THIS,
    ABSTRACT, SUPER,
    STATIC, DEFAULT, TRY, CATCH, FINALLY, THROW,
    THROWS, READONLY, FINAL,
    BANG, TYPEOF, TYPE, INSTANCE, AND, BIT_AND,
    OR, BIT_OR, BIT_XOR, SHIFT_LEFT, SHIFT_RIGHT,
    STRICT_NOT_EQUAL, STRICT_EQUAL, T_BOOL,
    CASE, SWITCH, MATCH, NONE, QUESTION_QUESTION, QUESTION_DOT, QUESTION, DECORATOR,
    AT, ARROW, MODULE, IMPORT, EXPORT, ALL,
    GLOBAL, PREFIX,

}