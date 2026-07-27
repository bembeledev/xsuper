package com.dic.xsuper.lang.ui.document;

import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.poo.XplClass;
// O teu utilitário habitual


public class ContextNativeRegistry {

    // A classe em memória (RAM) que o CanvasTag/XplElement vai procurar!
    public static XplClass CANVAS_CTX_CLASS;

    public static void inject(Interpreter interpreter) {
        if (interpreter == null || interpreter.globals == null) return;

        // =================================================================
        // ⭐ FASE 1: OBTER O MODELO NATIVO DO CANVAS
        // =================================================================
        XPLModel canvasCtxModel = XplCanvasContext2D.buildNativeModel();

        // =================================================================
        // ⭐ FASE 2: REGISTAR NO COMPILADOR (Para type checking e auto-complete)
        // =================================================================
        interpreter.registry_model.put("CanvasRenderingContext2D", canvasCtxModel);

        // =================================================================
        // ⭐ FASE 3: INJECTAR NA MEMÓRIA GLOBAL
        // =================================================================
        if (!interpreter.globals.values.containsKey("CanvasRenderingContext2D")) {
            CANVAS_CTX_CLASS = new XplClass(canvasCtxModel, interpreter.globals);
            interpreter.globals.defineConst("CanvasRenderingContext2D", CANVAS_CTX_CLASS);
        }
    }
}