package com.dic.xsuper.engine.exceptions;

public enum ErrorCategory {
    LEXER("Erro Léxico"),
    PARSER("Erro Sintático"),
    RUNTIME("Erro de Execução"),
    POO("Erro de Estrutura (POO)"),
    UI("Erro Gráfico (SuperUI)"),
    FATAL("Erro Fatal Não Capturado");

    public final String label;

    ErrorCategory(String label) {
        this.label = label;
    }
}