package com.dic.xsuper.engine.analysis;

import com.dic.xsuper.engine.ast.Stmt;
import com.dic.xsuper.engine.ast.TypeNode;
import com.dic.xsuper.engine.core.Token;

import java.util.*;

public class SemanticScope {
    public final SemanticScope enclosing;
    public final Map<String, SymbolInfo> symbols = new HashMap<>();
    private final Map<String, String> knownTypes = new HashMap<>(); // tipo -> "primitive", "class", "interface", "enum", "decorator", "alias"
    private final Map<String, XPLModelInfo> classInfo = new HashMap<>(); // nome da classe -> info
    private final Map<String, Stmt.InterfaceDecl> interfaceInfo = new HashMap<>(); // ⭐ NOVO: nome da interface -> info da interface
    private final Map<String, TypeNode> typeAliases = new HashMap<>(); // alias -> TypeNode original

    public SemanticScope(SemanticScope enclosing) {
        this.enclosing = enclosing;
    }

    // Profundidade do escopo (0 = global)
    public int depth() {
        int d = 0;
        SemanticScope s = this;
        while (s.enclosing != null) {
            d++;
            s = s.enclosing;
        }
        return d;
    }

    // --- Símbolos (variáveis, funções) ---

    public void define(Token name, String type, boolean isMutable, boolean isInitialized) {
        if (symbols.containsKey(name.lexeme)) {
            throw new SemanticError(name, "A variável '" + name.lexeme + "' já foi declarada neste escopo.");
        }
        symbols.put(name.lexeme, new SymbolInfo(type, isMutable, isInitialized, name, null));
    }

    public void defineFunction(Token name, String returnType, List<String> paramTypes) {
        if (symbols.containsKey(name.lexeme)) {
            throw new SemanticError(name, "O identificador '" + name.lexeme + "' já está em uso neste escopo.");
        }
        symbols.put(name.lexeme, new SymbolInfo(returnType, false, true, name, paramTypes));
    }

    public SymbolInfo resolve(Token name) {
        if (symbols.containsKey(name.lexeme)) return symbols.get(name.lexeme);
        if (enclosing != null) return enclosing.resolve(name);
        throw new SemanticError(name, "O identificador '" + name.lexeme + "' não existe ou ainda não foi declarado.");
    }

    public void checkAssignment(Token name) {
        SymbolInfo info = resolve(name);
        if (!info.isMutable) {
            throw new SemanticError(name, "Erro de Segurança: Não podes reatribuir um valor à constante '" + name.lexeme + "'.");
        }
        info.isInitialized = true;
    }

    // --- Tipos (declarações) ---

    public void defineType(Token name, String kind) {
        String lex = name.lexeme;
        if (knownTypes.containsKey(lex) || symbols.containsKey(lex)) {
            throw new SemanticError(name, "O identificador '" + lex + "' já está em uso neste escopo.");
        }
        knownTypes.put(lex, kind);
    }

    public boolean isTypeDefined(String typeName) {
        if (typeName == null) return true;
        String baseName = typeName.startsWith("?") ? typeName.substring(1) : typeName;
        // Tipos primitivos e nativos
        if (baseName.matches("int|float|string|bool|array|object|any|null|void|function|error|class")) return true;
        if (knownTypes.containsKey(baseName)) return true;
        if (typeAliases.containsKey(baseName)) return true;
        if (enclosing != null) return enclosing.isTypeDefined(baseName);
        return false;
    }

    // --- Aliases ---

    public void defineTypeAlias(Token name, TypeNode target) {
        String lex = name.lexeme;
        if (typeAliases.containsKey(lex) || knownTypes.containsKey(lex)) {
            throw new SemanticError(name, "O alias '" + lex + "' já está definido.");
        }
        typeAliases.put(lex, target);
        knownTypes.put(lex, "alias");
    }

    public boolean isTypeAlias(String name) {
        if (typeAliases.containsKey(name)) return true;
        if (enclosing != null) return enclosing.isTypeAlias(name);
        return false;
    }

    public TypeNode resolveTypeAlias(String name) {
        if (typeAliases.containsKey(name)) return typeAliases.get(name);
        if (enclosing != null) return enclosing.resolveTypeAlias(name);
        return null;
    }

    // --- Classes (declare) ---

    public void defineClassInfo(String name, String superclass, List<Stmt.FieldDecl> fields, List<String> interfaces, boolean isSealed, int typeParamCount, List<String> typeParameters) {
        if (classInfo.containsKey(name)) {
            throw new SemanticError(new Token(com.dic.xsuper.engine.core.TokenType.IDENTIFIER, name, null, 0, 0), "Classe '" + name + "' já definida.");
        }
        classInfo.put(name, new XPLModelInfo(name, superclass, fields, interfaces, isSealed, typeParamCount, typeParameters));
        knownTypes.put(name, "class");
    }

    public boolean isClass(String name) {
        if (classInfo.containsKey(name)) return true;
        if (enclosing != null) return enclosing.isClass(name);
        return false;
    }

    public XPLModelInfo getClassInfo(String name) {
        if (classInfo.containsKey(name)) return classInfo.get(name);
        if (enclosing != null) return enclosing.getClassInfo(name);
        return null;
    }

    public List<Stmt.FieldDecl> getClassFields(String name) {
        XPLModelInfo info = getClassInfo(name);
        if (info != null) return info.fields;
        return null;
    }

    public String getSuperclass(String name) {
        XPLModelInfo info = getClassInfo(name);
        return (info != null) ? info.superclass : null;
    }

    public boolean isSubclass(String child, String parent) {
        if (child.equals(parent)) return true;
        String superclass = getSuperclass(child);
        if (superclass == null) return false;
        return isSubclass(superclass, parent);
    }

    // --- ⭐ INTERFACES (Novos métodos em falta) ---

    public void defineInterfaceInfo(Stmt.InterfaceDecl stmt) {
        String name = stmt.name.lexeme;
        if (interfaceInfo.containsKey(name)) {
            throw new SemanticError(stmt.name, "Interface '" + name + "' já definida.");
        }
        interfaceInfo.put(name, stmt);
        knownTypes.put(name, "interface");
    }

    public boolean isInterfaceDefined(String name) {
        if (interfaceInfo.containsKey(name)) return true;
        if (enclosing != null) return enclosing.isInterfaceDefined(name);
        return false;
    }

    public Stmt.InterfaceDecl getInterfaceInfo(String name) {
        if (interfaceInfo.containsKey(name)) return interfaceInfo.get(name);
        if (enclosing != null) return enclosing.getInterfaceInfo(name);
        return null;
    }

    // --- Decorators ---

    // --- Decorators e Listeners ---

    public void defineDecorator(String name) {
        knownTypes.put(name, "decorator");
    }

    public boolean isDecorator(String name) {
        if (knownTypes.getOrDefault(name, "").equals("decorator")) return true;
        if (enclosing != null) return enclosing.isDecorator(name);
        return false;
    }

    // ⭐ A PEÇA EM FALTA: Para o Linter conseguir detetar as matrizes ativas! ⭐
    public void defineListener(String name) {
        knownTypes.put(name, "listener");
    }

    public boolean isListener(String name) {
        if (knownTypes.getOrDefault(name, "").equals("listener")) return true;
        if (enclosing != null) return enclosing.isListener(name);
        return false;
    }

    // =========================================================================
    // ⭐ PONTE DE IMPORTAÇÃO DE MÓDULOS (LINTER)
    // =========================================================================
    public void importFrom(SemanticScope sourceScope, String prefix, List<Stmt.ImportSymbol> importSymbols, boolean isWildcard) {
        if (isWildcard) {
            for (Map.Entry<String, String> entry : sourceScope.knownTypes.entrySet()) {
                String targetName = prefix + entry.getKey();
                this.knownTypes.put(targetName, entry.getValue());

                switch (entry.getValue()) {
                    case "class" ->
                            this.classInfo.put(targetName, deepCopyModel(sourceScope.classInfo.get(entry.getKey()), targetName));
                    case "interface" ->
                            this.interfaceInfo.put(targetName, sourceScope.interfaceInfo.get(entry.getKey()));
                    case "alias" -> this.typeAliases.put(targetName, sourceScope.typeAliases.get(entry.getKey()));
                }
            }

            for (Map.Entry<String, SymbolInfo> entry : sourceScope.symbols.entrySet()) {
                String targetName = prefix + entry.getKey();
                SymbolInfo info = entry.getValue();
                this.symbols.put(targetName, new SymbolInfo(info.type, info.isMutable, info.isInitialized, info.declarationToken, info.paramTypes));
            }
        } else if (importSymbols != null) {
            for (Stmt.ImportSymbol sym : importSymbols) {
                String original = sym.originalName.lexeme;
                String target = prefix + ((sym.aliasName != null) ? sym.aliasName.lexeme : original);

                if (sourceScope.knownTypes.containsKey(original)) {
                    String kind = sourceScope.knownTypes.get(original);
                    this.knownTypes.put(target, kind);

                    switch (kind) {
                        case "class" ->
                                this.classInfo.put(target, deepCopyModel(sourceScope.classInfo.get(original), target));
                        case "interface" -> this.interfaceInfo.put(target, sourceScope.interfaceInfo.get(original));
                        case "alias" -> this.typeAliases.put(target, sourceScope.typeAliases.get(original));
                    }
                }

                if (sourceScope.symbols.containsKey(original)) {
                    SymbolInfo info = sourceScope.symbols.get(original);
                    this.symbols.put(target, new SymbolInfo(info.type, info.isMutable, info.isInitialized, info.declarationToken, info.paramTypes));
                }
            }
        }
    }

    // =========================================================================
    // ⭐ UTILITÁRIO: DEEP COPY PARA MODELOS (Evita corrupção por variantes)
    // =========================================================================
    private XPLModelInfo deepCopyModel(XPLModelInfo original, String newName) {
        if (original == null) return null;
        XPLModelInfo clone = new XPLModelInfo(newName, original.superclass, original.fields, original.interfaces, original.isSealed, original.typeParamCount, original.typeParameters);
        clone.initParamTypes = new java.util.ArrayList<>(original.initParamTypes);
        clone.methods.addAll(original.methods);
        clone.isAbstract = original.isAbstract;
        return clone;
    }
    // --- Informações sobre símbolos ---

    public static class SymbolInfo {
        public final String type;
        public final boolean isMutable;
        public boolean isInitialized;
        public final Token declarationToken;
        public final List<String> paramTypes;

        public SymbolInfo(String type, boolean isMutable, boolean isInitialized, Token declarationToken, List<String> paramTypes) {
            this.type = type;
            this.isMutable = isMutable;
            this.isInitialized = isInitialized;
            this.declarationToken = declarationToken;
            this.paramTypes = paramTypes;
        }
    }

    public static class XPLModelInfo {
        public final String name;
        public final String superclass;
        public final List<Stmt.FieldDecl> fields;
        public final List<String> interfaces;
        public final boolean isSealed;
        public final int typeParamCount;
        public final List<String> typeParameters;
        public List<String> initParamTypes = new ArrayList<>();
        public final List<Stmt.Function> methods = new ArrayList<>();

        public boolean isAbstract = false;

        public XPLModelInfo(String name, String superclass, List<Stmt.FieldDecl> fields, List<String> interfaces, boolean isSealed, int typeParamCount, List<String> typeParameters) {
            this.name = name;
            this.superclass = superclass;
            this.fields = fields != null ? fields : Collections.emptyList();
            this.interfaces = interfaces != null ? interfaces : Collections.emptyList();
            this.isSealed = isSealed;
            this.typeParamCount = typeParamCount;
            this.typeParameters = typeParameters != null ? typeParameters : new ArrayList<>();
        }
    }
}