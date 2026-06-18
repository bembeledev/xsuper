package com.dic.xsuper.lang;

import java.util.List;

public class XplFunction implements XplCallable {
    private final Stmt.Function declaration;
    private final Environment closure; // Guarda o escopo onde a função foi criada

    public XplFunction(Stmt.Function declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        // Cria um ambiente novo isolado (O Escopo Local da Função)
        Environment environment = new Environment(closure);

        // Injeta os argumentos passados para dentro das variáveis locais
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.defineLet(declaration.params.get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (ControlFlow.ReturnException returnValue) {
            // Apanha o valor que fizeste "return" e devolve-o ao Java!
            return returnValue.value;
        }

        return null; // Se não houver return, devolve null (void)
    }

    @Override
    public String toString() {
        return "<fun " + declaration.name.lexeme + ">";
    }
}