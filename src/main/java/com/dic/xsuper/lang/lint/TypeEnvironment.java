package com.dic.xsuper.lang.lint;

import com.dic.xsuper.lang.Stmt;
import com.dic.xsuper.lang.TypeNode;

import java.util.*;

public class TypeEnvironment {
    // Tipos declarados (declare)
    public final Map<String, TypeNode> declaredTypes = new HashMap<>();
    // Funções globais (fun ...)
    public final Map<String, Stmt.Function> globalFunctions = new HashMap<>();
    // Classes: nome -> campos (nome -> FieldDecl)
    public final Map<String, Map<String, Stmt.FieldDecl>> classFields = new HashMap<>();
    // Classes: nome -> métodos (nome -> Function)
    public final Map<String, Map<String, Stmt.Function>> classMethods = new HashMap<>();
    // Superclasse: nome -> nome da super
    public final Map<String, String> classSuper = new HashMap<>();

    public void addType(String name, TypeNode type) {
        declaredTypes.put(name, type);
    }

    public void addGlobalFunction(String name, Stmt.Function func) {
        globalFunctions.put(name, func);
    }

    public void addClass(String name, String superName) {
        classFields.putIfAbsent(name, new HashMap<>());
        classMethods.putIfAbsent(name, new HashMap<>());
        if (superName != null) classSuper.put(name, superName);
    }

    public void addClassField(String className, Stmt.FieldDecl field) {
        classFields.computeIfAbsent(className, k -> new HashMap<>()).put(field.name.lexeme, field);
    }

    public void addClassMethod(String className, Stmt.Function method) {
        classMethods.computeIfAbsent(className, k -> new HashMap<>()).put(method.name.lexeme, method);
    }

    public Stmt.Function getFunction(String name) {
        return globalFunctions.get(name);
    }

    public Stmt.FieldDecl getField(String className, String fieldName) {
        Map<String, Stmt.FieldDecl> fields = classFields.get(className);
        if (fields != null && fields.containsKey(fieldName)) return fields.get(fieldName);
        // Procurar na superclasse
        String superName = classSuper.get(className);
        while (superName != null) {
            fields = classFields.get(superName);
            if (fields != null && fields.containsKey(fieldName)) return fields.get(fieldName);
            superName = classSuper.get(superName);
        }
        return null;
    }

    public Stmt.Function getMethod(String className, String methodName) {
        Map<String, Stmt.Function> methods = classMethods.get(className);
        if (methods != null && methods.containsKey(methodName)) return methods.get(methodName);
        String superName = classSuper.get(className);
        while (superName != null) {
            methods = classMethods.get(superName);
            if (methods != null && methods.containsKey(methodName)) return methods.get(methodName);
            superName = classSuper.get(superName);
        }
        return null;
    }

    public boolean isSubclass(String child, String parent) {
        if (child.equals(parent)) return true;
        String superName = classSuper.get(child);
        while (superName != null) {
            if (superName.equals(parent)) return true;
            superName = classSuper.get(superName);
        }
        return false;
    }
}