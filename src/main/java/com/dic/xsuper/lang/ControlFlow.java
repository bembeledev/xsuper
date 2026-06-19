package com.dic.xsuper.lang;

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
        public RuntimeError(Token token, String message) {
            super(message);
            this.token = token;
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