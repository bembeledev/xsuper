package com.dic.xsuper.dom.reactivity;

import com.dic.xsuper.engine.core.Environment;
import com.dic.xsuper.render.javafx.core.SuperUiEngine;

/**
 * Padrão Decorator: Um Listener que envolve o Environment original.
 * Interceta as mutações de variáveis para a UI sem tocar no núcleo do XPL.
 */
public class XplEnvironmentWatcher extends Environment {

    private final Environment coreEnvironment; // O Environment original, intocado!
    private final SuperUiEngine engine;

    // Construtor: Recebemos o Environment e não mexemos no código dele!
    public XplEnvironmentWatcher(Environment coreEnvironment, SuperUiEngine engine) {
        super(); // Chama o construtor vazio da classe mãe
        this.coreEnvironment = coreEnvironment;
        this.engine = engine;
    }

    // =========================================================================
    // ⭐ 1. OS NOSSOS "OUVIDOS" (Intercectamos a escrita)
    // =========================================================================

    @Override
    public void assign(String name, Object value) {
        // 1. O núcleo faz o seu trabalho
        coreEnvironment.assign(name, value);

        // 2. O Listener escuta e notifica a UI
        if (engine != null) {
            engine.updateVariable(name, value);
        }
    }

    @Override
    public void defineLet(String name, Object value) {
        coreEnvironment.defineLet(name, value);
        if (engine != null) {
            engine.updateVariable(name, value);
        }
    }

    @Override
    public void defineVar(String name, Object value) {
        coreEnvironment.defineVar(name, value);
        if (engine != null) {
            engine.updateVariable(name, value);
        }
    }

    @Override
    public void defineConst(String name, Object value) {
        coreEnvironment.defineConst(name, value);
        if (engine != null) {
            engine.updateVariable(name, value);
        }
    }

    // =========================================================================
    // 🛡️ 2. MODO TRANSPARENTE (Delegamos a leitura e tipagem para não quebrar o XPL)
    // =========================================================================

    @Override
    public Object get(String name) {
        // Apenas repassa a leitura para o Environment verdadeiro
        return coreEnvironment.get(name);
    }

    @Override
    public void remove(String name) {
        coreEnvironment.remove(name);
    }

    @Override
    public void lockType(String name, String type) {
        coreEnvironment.lockType(name, type);
    }

    @Override
    public String getLockedType(String name) {
        return coreEnvironment.getLockedType(name);
    }

    @Override
    public void defineImported(String name, Object value) {
        coreEnvironment.defineImported(name, value);
        // Imports geralmente não geram reactividade, mas podes adicionar se quiseres!
    }

    @Override
    public boolean isImported(String name) {
        return coreEnvironment.isImported(name);
    }
}