package com.dic.xsuper.lang;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Environment {
    private final Environment enclosing; // Escopo pai (ex: a função onde o if está dentro)
    public final Map<String, Object> values = new HashMap<>();
    private final Map<String, Boolean> isConstant = new HashMap<>();
    private final long depth; // Nível de profundidade (0 = Arquivo/Global)

    // ⭐ NOVO: Registo de quem entrou por via de 'import'
    private final java.util.Set<String> importedSymbols = new java.util.HashSet<>();


    // ⭐ MEMÓRIA BLINDADA CONTRA CONCORRÊNCIA ⭐
    public final Map<String, Object> valuesConcurrency = new ConcurrentHashMap<>();
    public final Map<String, String> typeRegistryConcurrency = new ConcurrentHashMap<>();


    // ⭐ NOVO: Método para registar variáveis importadas
    public void defineImported(String name, Object value) {
        values.put(name, value);
        importedSymbols.add(name); // Carimba o passaporte como "Importado"!
    }

    // ⭐ NOVO: Método para a Guilhotina perguntar se o símbolo é importado
    public boolean isImported(String name) {
        return importedSymbols.contains(name);
    }
    public Environment() {
        this.enclosing = null;
        this.depth = 0;
    }

    // ⭐ NOVO: Construtor Quântico para Raízes de Módulos
    public Environment(Environment enclosing, long depth) {
        this.enclosing = enclosing;
        this.depth = depth;
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

    // 1. Adiciona o dicionário de trancas logo abaixo do teu 'values'
    public final java.util.Map<String, String> typeRegistry = new java.util.HashMap<>();

    // 2. Adiciona este método para trancar uma variável a um tipo
    public void lockType(String name, String type) {
        typeRegistry.put(name, type);
    }

    // 3. Adiciona este método para o Interpretador perguntar o tipo
    public String getLockedType(String name) {
        if (typeRegistry.containsKey(name)) return typeRegistry.get(name);

        // Se não estiver neste escopo, procura nos escopos superiores (pai)!
        if (enclosing != null) return enclosing.getLockedType(name);

        return "any"; // Se nunca foi trancado, aceita tudo.
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