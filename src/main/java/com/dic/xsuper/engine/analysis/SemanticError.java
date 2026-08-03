package com.dic.xsuper.engine.analysis;

import com.dic.xsuper.engine.core.Token;

public class SemanticError extends RuntimeException {
    public final Token token;

    public SemanticError(Token token, String message) {
        super(message);
        this.token = token;
    }
}