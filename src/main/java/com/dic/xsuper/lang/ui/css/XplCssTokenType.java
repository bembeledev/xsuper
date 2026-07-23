package com.dic.xsuper.lang.ui.css;

public enum XplCssTokenType {
    // Símbolos estruturais
    LPAREN, RPAREN,         // ( )
    LBRACE, RBRACE,         // { }
    LBRACKET, RBRACKET,     // [ ]
    COMMA, DOT, COLON, SEMICOLON,  // , . : ;
    HASH,                   // #

    // Operadores aritméticos e de comparação
    PLUS, MINUS, STAR, SLASH, MODULO,  // + - * / %
    POWER,                  // ** (exponenciação)

    ASSIGN,                 // =
    EQUAL,                  // ==
    NOT_EQUAL,              // !=
    LESS, GREATER,          // < >
    LESS_EQUAL, GREATER_EQUAL, // <= >=

    // Operadores de incremento/decremento
    PLUS_PLUS, MINUS_MINUS, // ++ --

    // Operadores de atribuição compostos
    PLUS_ASSIGN, MINUS_ASSIGN, // += -=
    STAR_ASSIGN, SLASH_ASSIGN, // *= /=
    MODULO_ASSIGN,          // %=
    HASH_ASSIGN,            // #= (caso uses)

    // Valores e identificadores
    IDENTIFIER,             // nomes, seletores, propriedades, variáveis
    STRING,                 // "string"
    NUMBER,                 // 12, 1.5, 100%

    // Diretivas de controlo
    AT_IF, AT_ELSEIF, AT_ELSE,
    AT_FOR,
    AT_SWITCH, AT_CASE, AT_DEFAULT,
    AT_MATCH, AT_ARM, AT_NONE, AT_EMPTY,

    // Especiais CSS
    AT_MEDIA, AT_KEYFRAMES, AT_IMPORT, AT_EXTEND,
    // ... existentes
    PSEUDO_CLASS,      // :root, :hover, etc.
    VAR_NAME,          // --cor-primaria
    VAR_FUNC,          // var(
    CALC_FUNC,          // calc(

    BANG, SELECTOR, PROPERTY, QUESTION_MARK, EOF,
    TILDE, CARET, DOLLAR,   // ~ ^ $
}