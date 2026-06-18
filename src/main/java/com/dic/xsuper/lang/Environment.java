package com.dic.xsuper.lang;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Environment enclosing; // Escopo pai (ex: a função onde o if está dentro)
    private final Map<String, Object> values = new HashMap<>();
    private final Map<String, Boolean> isConstant = new HashMap<>();
    private final long depth; // Nível de profundidade (0 = Arquivo/Global)

    public Environment() {
        this.enclosing = null;
        this.depth = 0;
    }

    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
        this.depth = enclosing.depth + 1;
    }

    public void defineVar(String name, Object value) {
        if (depth > 0) {
            throw new RuntimeException("Erro de Sintaxe: 'var' (" + name + ") só pode ser declarado ao nível do arquivo (Escopo Global).");
        }
        values.put(name, value);
        isConstant.put(name, false);
    }

    public void defineLet(String name, Object value) {
        // Pode ser redeclarado no mesmo escopo (sobrescreve) ou em escopos diferentes
        values.put(name, value);
        isConstant.put(name, false);
    }

    public void defineConst(String name, Object value) {
        if (values.containsKey(name)) {
            throw new RuntimeException("Erro: A constante '" + name + "' já está definida neste escopo.");
        }
        values.put(name, value);
        isConstant.put(name, true);
    }

    public void assign(String name, Object value) {
        if (values.containsKey(name)) {
            if (isConstant.get(name)) {
                throw new RuntimeException("Erro: Não podes reatribuir valor à constante '" + name + "'.");
            }
            values.put(name, value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeException("Erro: Variável não definida '" + name + "'.");
    }

    public Object get(String name) {
        if (values.containsKey(name)) {
            return values.get(name);
        }
        if (enclosing != null) {
            return enclosing.get(name);
        }
        throw new RuntimeException("Erro: Variável não definida '" + name + "'.");
    }

    @Override
    public String toString() {
        return "Environment{" +
                "enclosing=" + enclosing +
                ", values=" + values +
                ", isConstant=" + isConstant +
                ", depth=" + depth +
                '}';
    }
}