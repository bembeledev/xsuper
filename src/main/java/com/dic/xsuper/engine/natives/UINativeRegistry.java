package com.dic.xsuper.engine.natives;

import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.poo.XplClass;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.dom.node.XplElement;
import com.dic.xsuper.dom.node.XplDocument;
import com.dic.xsuper.dom.event.XplEvent;
import com.dic.xsuper.render.javafx.core.SuperUiEngine;
// Importa o teu XplEvent também

public class UINativeRegistry {

    // Cache global das classes base (para uso interno do motor Java)
    public static XplClass ELEMENT_CLASS;
    public static XplClass DOCUMENT_CLASS;
    public static XplClass ENGINE_CLASS;
    public static XplClass EVENT_CLASS;

    public static void inject(Interpreter interpreter) {
        if (interpreter == null || interpreter.globals == null) return;

        // =================================================================
        // ⭐ FASE 1: OBTER OS MODELOS NATIVOS
        // =================================================================
        XPLModel elementModel = XplElement.buildNativeModel();
        XPLModel documentModel = XplDocument.buildNativeModel();
        XPLModel engineModel = SuperUiEngine.buildNativeModel();
        XPLModel eventModel = XplEvent.buildNativeModel();

        // =================================================================
        // ⭐ FASE 2: REGISTAR NO COMPILADOR (Para o 'extends' funcionar)
        // =================================================================
        interpreter.registry_model.put("XplElement", elementModel);
        interpreter.registry_model.put("XplDocument", documentModel);
        interpreter.registry_model.put("SuperUiEngine", engineModel);
        interpreter.registry_model.put("XplEvent", eventModel);

        // =================================================================
        // ⭐ FASE 3: CRIAR AS CLASSES RUNTIME E REGISTAR NA MEMÓRIA GLOBAL
        // =================================================================
        if (!interpreter.globals.values.containsKey("XplElement")) {
            ELEMENT_CLASS = new XplClass(elementModel, interpreter.globals);
            interpreter.globals.defineConst("XplElement", ELEMENT_CLASS);
        }
        if (!interpreter.globals.values.containsKey("XplDocument")) {
            DOCUMENT_CLASS = new XplClass(documentModel, interpreter.globals);
            interpreter.globals.defineConst("XplDocument", DOCUMENT_CLASS);
        }
        if (!interpreter.globals.values.containsKey("SuperUiEngine")) {
            ENGINE_CLASS = new XplClass(engineModel, interpreter.globals);
            // Pode ou não ser injetado no globals, dependendo se queres que o utilizador instancie motores!
        }
        if (!interpreter.globals.values.containsKey("XplEvent")) {
            EVENT_CLASS = new XplClass(eventModel, interpreter.globals);
            interpreter.globals.defineConst("XplEvent", EVENT_CLASS);
        }
    }
}