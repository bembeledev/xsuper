package com.dic.xsuper.lang.poo;

import com.dic.xsuper.lang.Stmt;

import java.util.HashMap;
import java.util.Map;

public class XplInterface {
    public final String name;

    // Guardamos o mapa de assinaturas (o que a interface exige)
    public final Map<String, Stmt.FunctionSig> requiredMethods = new HashMap<>();

    public XplInterface(Stmt.InterfaceDecl stmt) {
        this.name = stmt.name.lexeme;

        // Pega em todas as assinaturas geradas pelo Parser e guarda-as!
        for (Stmt.FunctionSig sig : stmt.methods) {
            requiredMethods.put(sig.name.lexeme, sig);
        }
    }
}