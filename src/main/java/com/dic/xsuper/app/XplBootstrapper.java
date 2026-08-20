package com.dic.xsuper.app;

import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.natives.models.ModelNativeRegistry;
import com.dic.xsuper.engine.natives.variables.NativeVariables;

public class XplBootstrapper {

    /**
     * Equipa o Interpretador "nu" com todas as ferramentas da Standard Library.
     **/
    public static void bootstrap(Interpreter interpreter) {

        // 1. Injecta as Funções Globais (println, shell, If, For, etc.)
        NativeVariables.registry(interpreter);

        // 2. Injecta os Modelos de Dados (Math, Json, File, Http, Crypto, etc.)
        ModelNativeRegistry.InjectRegistry(interpreter);

    }

}