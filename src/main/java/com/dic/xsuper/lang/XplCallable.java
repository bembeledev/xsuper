package com.dic.xsuper.lang;

import java.util.List;

public interface XplCallable {
    // Retorna o número de argumentos exigidos (-1 se aceitar número variável, ex: 1 ou 2)
    int arity();

    // O que acontece quando a função é chamada
    Object call(Interpreter interpreter, List<Object> arguments);
}