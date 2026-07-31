package com.dic.xsuper.engine.exceptions;

import com.dic.xsuper.engine.core.Token;

public class ControlFlow {

    // Usamos RuntimeException, mas desativamos o StackTrace para ser hiper-rápido (custa zero performance)
    public static class BreakException extends RuntimeException {
        public BreakException() { super(null, null, false, false); }
    }

    public static class ContinueException extends RuntimeException {
        public ContinueException() { super(null, null, false, false); }
    }

    public static class RuntimeError extends RuntimeException {
        public final Token token;
        public final com.dic.xsuper.engine.exceptions.ErrorCode code;
        private final String rawMessage; // A mensagem já processada

        // Construtor Legado
        public RuntimeError(Token token, String message) {
            super(message);
            this.token = token;
            this.code = null;
            this.rawMessage = "Erro de Execução: " + message;
        }

        // Construtor Novo (Blindado)
        public RuntimeError(Token token, com.dic.xsuper.engine.exceptions.ErrorCode code, Object... args) {
            super(code.format(args)); // Passa a mensagem completa para o Java
            this.token = token;
            this.code = code;
            this.rawMessage = code.format(args); // Guarda: "[XPL1200] Erro (POO): Mensagem..."
        }

        // ⭐ O FORMATADOR AUTÓNOMO ⭐
        public String getDisplayMessage() {
            String path = (token != null && token.filePath != null) ? token.filePath : "Nativo/JIT";
            int line = (token != null) ? token.line : 0;
            int col = (token != null) ? token.column : 0;

            return com.dic.xsuper.utils.ConsoleTheme.ERROR +
                    path + ":" + line + ":" + col + ":\n\t" +
                    this.rawMessage +
                    com.dic.xsuper.utils.ConsoleTheme.RESET;
        }
    }

    public static class ReturnException extends RuntimeException {
        public final Object value;
        public ReturnException(Object value) {
            super(null, null, false, false);
            this.value = value;
        }
    }

    public static class ThrowException extends RuntimeException {
        public final Object value;
        public ThrowException(Object value) {
            // Desativamos a stacktrace para máxima performance!
            super(null, null, false, false);
            this.value = value;
        }
    }
}