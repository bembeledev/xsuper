package com.dic.xsuper.lang.poo;

import com.dic.xsuper.lang.Stmt;
import com.dic.xsuper.lang.Token;

import java.util.HashMap;
import java.util.Map;

public class XPLModel {
    public final String name; // Ex: "Mam1" ou "Animal"
    public final XPLModel superclass; // Para lidar com o 'extends'

    // Os dados vêm do 'declare'
    public final Map<String, Stmt.FieldDecl> fields = new HashMap<>();

    // Os comportamentos vêm do 'implement'
    public final Map<String, Stmt.Function> methods = new HashMap<>();

    // ⭐ A NOVA MEMÓRIA VIVA ⭐
    public final Map<String, Object> staticFields = new HashMap<>();
    public final Map<String, Object> defaultInstanceFields = new HashMap<>();

    // ⭐ AS NOVAS TRAVAS DE ESTADO ⭐
    public boolean hasBaseImplementation = false; // Fica true quando houver um 'implement Nome'
    public boolean isAbstract = false;             // Fica true se for 'abstract implement Nome'

    public boolean canBeInstantiated = false; // Bloqueado por defeito

    // =========================================================================
    // ⭐ COORDENADAS DE METAPROGRAMAÇÃO (Decoradores) ⭐
    // =========================================================================
    public boolean isDecorator = false;

    // Guardam o nome real dos métodos que o programador marcou com @(Context.X)
    public String metaInitHook = null;
    public String metaGetHook  = null;
    public String metaSetHook  = null;
    public String metaEndHook  = null;

    // Injeta estas duas ranhuras na tua classe XPLModel:
    public boolean isGenericBlueprint = false;
    public java.util.List<Token> typeParameters = new java.util.ArrayList<>();


    // ⭐ GUARDA A CÁBULA GENÉTICA DOS CLONES (Ex: { "T": "int", "U": "string" }) ⭐
    public final java.util.Map<String, String> resolvedGenericMap = new java.util.HashMap<>();


    public XPLModel(String name, XPLModel superclass) {
        this.name = name;
        this.superclass = superclass;
    }

    public void addField(Stmt.FieldDecl field) {
        fields.put(field.name.lexeme, field);
    }

    public void addMethod(Stmt.Function method) {
        methods.put(method.name.lexeme, method);
    }

    // ⭐ A MAGIA DA HERANÇA DE COMPORTAMENTO ⭐
    public Stmt.Function findMethod(String methodName) {
        // 1. Procura primeiro no próprio objeto (Prioridade para o Overriding)
        if (methods.containsKey(methodName)) {
            return methods.get(methodName);
        }

        // 2. Se não tem, mas tem um pai, sobe na árvore genealógica!
        if (superclass != null) {
            return superclass.findMethod(methodName);
        }

        // 3. Se chegou ao topo (sem pai) e não encontrou nada, devolve null
        return null;
    }

    // Devolve o modelo exato onde este método foi escrito originalmente
    public XPLModel getOwnerOfMethod(String methodName) {
        if (methods.containsKey(methodName)) return this;
        if (superclass != null) return superclass.getOwnerOfMethod(methodName);
        return null;
    }

    // ⭐ VALIDAÇÃO DE ÁRVORE GENEALÓGICA PARA O CATCH ⭐
    public boolean isSubclassOf(String typeName) {
        if (this.name.equals(typeName)) return true; // É a própria classe!
        if (this.superclass != null) return this.superclass.isSubclassOf(typeName); // Pergunta ao pai!
        return false;
    }

    @Override
    public String toString() {
        return "XPLModel{" +
                "name='" + name + '\'' +
                ", superclass=" + superclass +
                ", fields=" + fields +
                ", methods=" + methods +
                ", hasBaseImplementation=" + hasBaseImplementation +
                ", isAbstract=" + isAbstract +
                '}';
    }
}