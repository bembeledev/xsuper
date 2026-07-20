package com.dic.xsuper.lang.lint;

import com.dic.xsuper.lang.Token;

public class LintIssue {
    public enum Severity { WARNING, ERROR, SUGGESTION }

    public final Severity severity;
    public final String message;
    public final int line;
    public final int column;
    public final String filePath;
    public final String suggestion; // pode ser null

    public LintIssue(Severity severity, String message, Token token, String suggestion) {
        this.severity = severity;
        this.message = message;
        this.line = token != null ? token.line : 0;
        this.column = token != null ? token.column : 0;
        this.filePath = token != null ? token.filePath : "desconhecido";
        this.suggestion = suggestion;
    }

    public LintIssue(Severity severity, String message, int line, int column, String filePath, String suggestion) {
        this.severity = severity;
        this.message = message;
        this.line = line;
        this.column = column;
        this.filePath = filePath;
        this.suggestion = suggestion;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s:%d:%d – %s%s",
                severity, filePath, line, column, message,
                suggestion != null ? " (sugestão: " + suggestion + ")" : "");
    }
}