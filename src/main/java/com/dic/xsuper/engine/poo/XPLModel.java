package com.dic.xsuper.engine.poo;

import com.dic.xsuper.engine.ast.Stmt;
import com.dic.xsuper.engine.core.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XPLModel {
    public final String name; // Ex: "Mam1" ou "Animal"
    public final XPLModel superclass; // Para lidar com o 'extends'
    public boolean isSealed = false;
    public XPLModel baseModel; // referência ao modelo original (para variantes)
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


    // ⭐ NOVO: Árvore de Contratos (Interfaces)
    public final java.util.List<String> implementedInterfaces = new java.util.ArrayList<>();

    // ⭐ NOVO: Armazém de Decoradores (Para a Metaprogramação)
    public final java.util.List<String> appliedDecorators = new java.util.ArrayList<>();


    public final List<String> variantAliases = new ArrayList<>();
    public List<Stmt.DecoratorNode> decoratorNodes = new ArrayList<>();

    // ⭐ NOVO: Validadores de Estado (O "Carimbo de Aprovação")
    public boolean canBeInstantiated() {
        return !isAbstract && hasBaseImplementation;
    }


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


    // ⭐ A VERIFICAÇÃO DE CONTRATOS (INTERFACES) ⭐
    public boolean implementsInterface(String targetType) {
        // 1. Eu implemento esta interface diretamente no meu bloco 'implement'?
        if (implementedInterfaces.contains(targetType)) return true;

        // 2. Se não, será que o meu pai (extends superclass) implementou?
        if (superclass != null) {
            if (superclass.implementsInterface(targetType)) return true;
        }

        // 3. Se eu for uma Variante (as Mam1), o meu modelo Base assinou a interface?
        if (baseModel != null) {
            if (baseModel.implementsInterface(targetType)) return true;
        }

        // Não encontrou a interface em lado nenhum do ADN!
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