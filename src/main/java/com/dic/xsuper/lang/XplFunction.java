package com.dic.xsuper.lang;

import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.poo.XplInstance;

import java.util.List;

public class XplFunction implements XplCallable {
    private final Stmt.Function declaration;
    private final Environment closure; // Guarda o escopo onde a função foi criada
    private final XPLModel ownerModel;

    public XplFunction(Stmt.Function declaration, Environment closure, XPLModel ownerModel) {
        this.declaration = declaration;
        this.closure = closure;
        this.ownerModel = ownerModel;
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
            environment.defineLet(declaration.params.get(i).name.lexeme, arguments.get(i));
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

    // ⭐ Cria uma nova versão da função com o 'this' injetado no escopo!
    public XplFunction bind(XplInstance instance) {
        Environment environment = new Environment(closure);
        environment.defineConst("this", instance);
        // ⭐ O SEGREDO DO SUPER: Guardamos em que nível da árvore genealógica estamos!
        if (ownerModel != null) {
            environment.defineConst("__current_model", ownerModel);
        }
        return new XplFunction(declaration, environment, ownerModel);
    }
}