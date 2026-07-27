package com.dic.xsuper.lang.ui.document;


import com.dic.xsuper.lang.Interpreter;

import java.util.List;

public interface XplNativeObject {
    /**
     * Permite ao interpretador interrogar o objecto para executar um método.
     */
    void invokeMethod();
    default Object invokeMethod(String methodName, List<Object> args, Interpreter interpreter){
        return null;
    };

    /**
     * Permite ao interpretador ler uma propriedade (ex: btn.id).
     */
    Object getProperty(String propertyName);

}