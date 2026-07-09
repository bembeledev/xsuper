package com.dic.xsuper.lang.ui.document;

import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.poo.XPLModel;

import java.util.List;

public interface XplNativeObject {
    /**
     * Permite ao interpretador interrogar o objeto para executar um método.
     */
    void invokeMethod();

    /**
     * Permite ao interpretador ler uma propriedade (ex: btn.id).
     */
    Object getProperty(String propertyName);

    static XPLModel buildNativeModel() {
        return null;
    }
}