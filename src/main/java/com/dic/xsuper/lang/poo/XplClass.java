package com.dic.xsuper.lang.poo;

import com.dic.xsuper.lang.Environment;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;
import com.dic.xsuper.lang.XplFunction; // Garante esta importação
import com.dic.xsuper.lang.Stmt;        // Garante esta importação

import java.util.List;

public class XplClass implements XplCallable {
    public final XPLModel model;
    public final Environment closure; // O ambiente onde a classe foi registada

    public XplClass(XPLModel model, Environment closure) {
        this.model = model;
        this.closure = closure;
    }

    @Override
    public int arity() {
        // ⭐ 1. A FILTRAGEM DE ARIDADE DINÂMICA ⭐
        // Usamos o 'findMethod' que criámos para ver se existe um construtor 'init'
        Stmt.Function initializer = model.findMethod("init");

        // Se não houver construtor, o 'new' aceita 0 argumentos.
        if (initializer == null) return 0;

        // Se houver, a classe herda o número de parâmetros exigidos pelo 'init'!
        return initializer.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        // 1. Cria o bloco de memória limpo (Instância)
        XplInstance instance = new XplInstance(this);

        // ⭐ 2. O DESPERTAR DO CONSTRUTOR ⭐
        // Procura se o modelo (ou os seus pais) definiram um método 'init'
        Stmt.Function initializer = model.findMethod("init");

        if (initializer != null) {
            XPLModel owner = model.getOwnerOfMethod("init");
            // Transforma a AST da função num comportamento invocável
            XplFunction constructor = new XplFunction(initializer, closure,owner);

            // Amarramos o context 'this' à nova instância e executamos imediatamente
            // passando os argumentos que vieram do 'new Modelo(args...)'
            constructor.bind(instance).call(interpreter, arguments);
        }

        // 3. Devolve o objeto vivo e totalmente preenchido
        return instance;
    }

    @Override
    public String toString() {
        return "<class " + model.name + ">";
    }
}