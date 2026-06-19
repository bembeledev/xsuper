package com.dic.xsuper.lang.poo;

import com.dic.xsuper.lang.Stmt;

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