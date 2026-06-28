package com.dic.xsuper.lang.ui.html;

public enum HtmlTokenType {
    // 1. Estrutura HTML
    LT, GT,             // < e >
    SLASH,              // / (para fechar tags </div> ou <img/>)
    EQUALS,             // =
    STRING,             // "valores entre aspas"
    TEXT,               // Texto puro (ex: "Compilar")
    IDENTIFIER,         // Nomes de tags (div) ou atributos (id, class)

    // 2. Bindings Híbridos (Padrão Angular dentro da Tag)
    LBRACKET, RBRACKET, // [ e ] -> Para Data Binding: [value]="x"
    LPAREN, RPAREN,     // ( e ) -> Para Eventos: (click)="y"

    // 3. Diretivas de Fluxo SuperUI (Padrão Blazor/Angular17)
    AT_IF, AT_ELSEIF, AT_ELSE,
    AT_FOR,
    AT_SWITCH, AT_CASE, AT_DEFAULT, AT_MATCH,

    AT_ARM,AT_NONE,AT_EMPTY,

    // 4. Estrutura de Blocos
    LBRACE, RBRACE,     // { e } -> Para envolver o conteúdo do @if
    EOF
}