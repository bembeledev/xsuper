package com.dic.xsuper.lang.poo;

import com.dic.xsuper.lang.ControlFlow;
import com.dic.xsuper.lang.Stmt;
import com.dic.xsuper.lang.Token;
import com.dic.xsuper.lang.XplFunction;

import java.util.HashMap;
import java.util.Map;

public class XplInstance {
    public XplClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    public XplInstance(XplClass klass) {
        this.klass = klass;

        // Inicializa a memória ignorando os campos estáticos!
        for (String fieldName : klass.model.fields.keySet()) {
            Stmt.FieldDecl field = klass.model.fields.get(fieldName);

            if (!field.isStatic) {
                // Se não for estático, vai buscar o valor default (ou null se não existir)
                Object defaultValue = klass.model.defaultInstanceFields.getOrDefault(fieldName, null);
                fields.put(fieldName, defaultValue);
            }
        }
    }

    // Método para LER (Ex: println(animal.nome) ou animal.add())
    public Object get(Token name) {
        // 1. É um Dado? (Property)
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        // ⭐ 2. É um Comportamento? (Method) - SUBSTITUIR AQUI ⭐
        // Antes: Stmt.Function method = klass.model.methods.get(name.lexeme);
        Stmt.Function method = klass.model.findMethod(name.lexeme);

        if (method != null) {
            XPLModel owner = klass.model.getOwnerOfMethod(name.lexeme);
            XplFunction function = new XplFunction(method, klass.closure, owner);
            // Converte a função da AST para uma função invocável e injeta o 'this'
            return function.bind(this);
        }

        throw new ControlFlow.RuntimeError(name,
                "A propriedade ou método '" + name.lexeme + "' não existe no modelo " + klass.model.name + " nem nos seus ascendentes.");
    }

    // Método para ESCREVER (Ex: animal.nome = "Leão")
    public void set(Token name, Object value) {
        // Bloqueamos a criação de propriedades dinâmicas! XPL é estritamente estruturado.
        if (klass.model.fields.containsKey(name.lexeme)) {
            fields.put(name.lexeme, value);
        } else {
            throw new ControlFlow.RuntimeError(name,
                    "Operação Ilegal: O modelo " + klass.model.name + " não possui o campo '" + name.lexeme + "'.");
        }
    }

    @Override
    public String toString() {
        return "<instance " + klass.model.name + ">";
    }
}