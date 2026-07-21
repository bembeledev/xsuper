package com.dic.xsuper.lang;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Environment {
    private final Environment enclosing; // Escopo pai (ex: a função onde o if está dentro)
    public final Map<String, Object> values = Collections.synchronizedMap(new java.util.HashMap<>());
    private final Map<String, Boolean> isConstant = Collections.synchronizedMap(new java.util.HashMap<>());
    private final long depth; // Nível de profundidade (0 = Arquivo/Global)

    public final java.util.Map<String, String> typeRegistry = Collections.synchronizedMap(new java.util.HashMap<>());
    // Registo de quem entrou por via de 'import'
    private final java.util.Set<String> importedSymbols = Collections.synchronizedSet(new java.util.HashSet<>());

    // ⭐ MEMÓRIA BLINDADA CONTRA CONCORRÊNCIA ⭐
    public final Map<String, Object> valuesConcurrency = new ConcurrentHashMap<>();
    public final Map<String, String> typeRegistryConcurrency = new ConcurrentHashMap<>();

    // =========================================================================
    // ⭐ 1. A INTERFACE DO NOVO SISTEMA NERVOSO GERAL (Múltiplos Eventos)
    // =========================================================================
    public interface XplEnvironmentListener {
        void onVariableDeclared(String name, Object value, String scopeType); // scopeType: "let", "var", "const"
        void onVariableMutated(String name, Object oldValue, Object newValue);
        void onVariableRead(String name, Object value);
        void onVariableRemove(String name, Object value);
    }

    // Lista estática e Thread-Safe para múltiplos ouvintes globais (UI, Debugger, Profiler...)
    private static final List<XplEnvironmentListener> globalListeners = new CopyOnWriteArrayList<>();

    public static void addListener(XplEnvironmentListener listener) {
        if (!globalListeners.contains(listener)) {
            globalListeners.add(listener);
        }
    }

    public static void removeListener(XplEnvironmentListener listener) {
        globalListeners.remove(listener);
    }

    // ⭐ Método para registar variáveis importadas
    public void defineImported(String name, Object value) {
        values.put(name, value);
        importedSymbols.add(name); // Carimba o passaporte como "Importado"!
    }

    // ⭐ Método para a Guilhotina perguntar se o símbolo é importado
    public boolean isImported(String name) {
        return importedSymbols.contains(name);
    }
    public Environment() {
        this.enclosing = null;
        this.depth = 0;
    }

    // ⭐ Construtor Quântico para Raízes de Módulos
    public Environment(Environment enclosing, long depth) {
        this.enclosing = enclosing;
        this.depth = depth;
    }

    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
        this.depth = enclosing.depth + 1;
    }



    // =========================================================================
    // 📢 NOTIFICADORES INTERNOS (Alta Performance)
    // =========================================================================
    private void notifyDeclared(String name, Object value, String scopeType) {
        if (globalListeners.isEmpty()) return;
        for (XplEnvironmentListener l : globalListeners) l.onVariableDeclared(name, value, scopeType);
    }

    private void notifyMutated(String name, Object oldValue, Object newValue) {
        if (globalListeners.isEmpty()) return;
        for (XplEnvironmentListener l : globalListeners) l.onVariableMutated(name, oldValue, newValue);
    }

    private void notifyRead(String name, Object value) {
        if (globalListeners.isEmpty()) return;
        for (XplEnvironmentListener l : globalListeners) l.onVariableRead(name, value);
    }

    public void defineLet(String name, Object value) {
        // Pode ser re-declarado no mesmo escopo (sobrescreve) ou em escopos diferentes
        values.put(name, value);
        isConstant.put(name, false);
        notifyDeclared(name, value, "let");
    }

    public void defineConst(String name, Object value) {
        if (values.containsKey(name)) {
            throw new RuntimeException("Erro: A constante '" + name + "' já está definida neste escopo.");
        }
        values.put(name, value);
        isConstant.put(name, true);
        notifyDeclared(name, value, "const");
    }

    public void defineVar(String name, Object value) {
        if (depth > 0) {
            throw new RuntimeException("Erro de Sintaxe: 'var' (" + name + ") só pode ser declarado ao nível do arquivo (Escopo Global).");
        }
        values.put(name, value);
        isConstant.put(name, false);
        notifyDeclared(name, value, "var");
    }

    public void assign(String name, Object value) {
        if (values.containsKey(name)) {
            if (isConstant.get(name)) {
                throw new RuntimeException("Erro: Não podes reatribuir valor à constante '" + name + "'.");
            }
            Object oldValue = values.get(name);
            values.put(name, value);
            notifyMutated(name, oldValue, value);
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
            Object val = values.get(name);
            notifyRead(name, val); // Gatilho de Leitura!
            return val;
        }
        if (enclosing != null) {
            return enclosing.get(name);
        }
        throw new RuntimeException("Erro: Variável não definida '" + name + "'.");
    }

    /**
     * Remove completamente uma variável do escopo actual.
     * Útil para limpar variáveis temporárias injetadas pelo motor (ex: 'event').
     */
    public void remove(String name) {
        // 1. Remove o valor principal
        values.remove(name);

        // 2. Remove o registo de constante (para evitar bloqueios futuros se a variável for recriada)
        isConstant.remove(name);

        // 3. Remove as trancas de tipo, caso a variável tenha sido tipada
        typeRegistry.remove(name);

        // 4. Se por acaso foi importada, limpa também esse carimbo
        importedSymbols.remove(name);
    }

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