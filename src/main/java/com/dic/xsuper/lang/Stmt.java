package com.dic.xsuper.lang;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class Stmt {

    public interface Visitor<R> {
        R visitExpressionStmt(ExpressionStmt stmt);
        R visitVarDeclStmt(VarDecl stmt);
        R visitBlockStmt(Block stmt);
        R visitForCStyleStmt(ForCStyle stmt);
        R visitFunctionStmt(Function stmt);
        R visitBreakStmt(Break stmt);
        R visitContinueStmt(Continue stmt);
        R visitReturnStmt(Return aReturn);
        R visitForInRangeStmt(ForInRange forInRange);
        R visitForInStmt(ForIn forIn);
        R visitEnumStmt(Enum stmt);
        R visitDeclareDeclStmt(DeclareDecl declareDecl);
        R visitInterfaceDeclStmt(InterfaceDecl interfaceDecl);
        R visitImplementDeclStmt(ImplementDecl stmt);
        R visitTryStmt(Try stmt);
        R visitThrowStmt(Throw stmt);
        R visitTypeAliasDecl(TypeAliasDecl typeAliasDecl);
        R visitDecoratorDeclStmt(DecoratorDecl decoratorDecl);
        R visitModuleDeclStmt(ModuleDecl moduleDecl);
        R visitImportDeclStmt(ImportDecl importDecl);
        R visitExportDeclStmt(ExportDecl exportDecl);
        R visitGlobalDeclStmt(GlobalDecl globalDecl);
        R visitDoWhileStmt(DoWhile doWhile);
        R visitWhileStmt(While aWhile);
    }

    public abstract <R> R accept(Visitor<R> visitor);

    // --- Tipos de Declarações ---

    public static class ExpressionStmt extends Stmt {
        public final Expr expression;

        public ExpressionStmt(Expr expression) {
            this.expression = expression;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitExpressionStmt(this); }

        @Override
        public String toString() {
            return "ExpressionStmt{" +
                    "expression=" + expression +
                    '}';
        }
    }

    public static class VarDecl extends Stmt {
        public final Token keyword;
        public final Token name;
        public final TypeNode typeAnnotation; // ⭐ A EVOLUÇÃO: Agora usa a árvore de tipos!
        public final Expr initializer;
        public final java.util.List<DecoratorNode> decorators;
        public final List<Stmt.DecoratorNode> listeners;  // Os reativos (&)

        public VarDecl(Token keyword, Token name, TypeNode typeAnnotation, Expr initializer, List<DecoratorNode> decorators, List<DecoratorNode> listeners) {
            this.keyword = keyword;
            this.name = name;
            this.typeAnnotation = typeAnnotation;
            this.initializer = initializer;
            this.decorators = decorators;
            this.listeners = listeners;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitVarDeclStmt(this); }

        @Override
        public String toString() {
            return "VarDecl{" +
                    "keyword=" + keyword +
                    ", name=" + name +
                    ", typeAnnotation=" + typeAnnotation +
                    ", initializer=" + initializer +
                    ", decorators=" + decorators +
                    ", listeners=" + listeners +
                    '}';
        }
    }

    public static class Block extends Stmt {
        public final List<Stmt> statements;

        public Block(List<Stmt> statements) {
            this.statements = statements;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitBlockStmt(this); }

        @Override
        public String toString() {
            return "Block{" +
                    "statements=" + statements +
                    '}';
        }
    }

    public static class ForIn extends Stmt {
        public final Token loopVariable; // O 'a' no teu "for a in [1,2,3]"
        public final Expr iterable;      // A array [1,2,3]
        public final Stmt body;          // O bloco {}

        public ForIn(Token loopVariable, Expr iterable, Stmt body) {
            this.loopVariable = loopVariable;
            this.iterable = iterable;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitForInStmt(this); }

        @Override
        public String toString() {
            return "ForIn{" +
                    "loopVariable=" + loopVariable +
                    ", iterable=" + iterable +
                    ", body=" + body +
                    '}';
        }
    }

    public static class ForCStyle extends Stmt {
        public final Stmt init; // O 'a' no teu "for (a=0; a<=12; a=a + 1 ){}"
        public final Expr condition;      // i<1; iz=12
        public final Expr increment;      // i++, i--,
        public final Stmt body;          // O bloco {}

        public ForCStyle(Stmt init, Expr condition, Expr increment, Stmt body) {
            this.init = init;
            this.condition = condition;
            this.increment = increment;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitForCStyleStmt(this); }

        @Override
        public String toString() {
            return "ForCStyle{" +
                    "init=" + init +
                    ", condition=" + condition +
                    ", increment=" + increment +
                    ", body=" + body +
                    '}';
        }
    }

    // ⭐ LAÇO DO-WHILE
    public static class DoWhile extends Stmt {
        public final Stmt body;
        public final Expr condition;

        public DoWhile(Stmt body, Expr condition) {
            this.body = body;
            this.condition = condition;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitDoWhileStmt(this);
        }
    }

    public static class ForInRange extends Stmt {
        public final Token loopVariable; // O 'a' no teu "for a in [1,2,3]"
        public final Expr start;      // i<1; iz=12
        public final Expr end;      // i<1; iz=12
        public final Optional<Expr> jump;      // i<1; iz=12
        public final Stmt body;          // O bloco {}

        public ForInRange(Token loopVariable, Expr start, Expr end, Optional<Expr> jump, Stmt body) {
            this.loopVariable = loopVariable;
            this.start = start;
            this.end = end;
            this.jump = jump;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitForInRangeStmt(this); }

        @Override
        public String toString() {
            String jump = this.jump.map(Object::toString).orElse(null);
            return "ForInRange{" +
                    "loopVariable=" + loopVariable +
                    ", start=" + start +
                    ", end=" + end +
                    ", jump=" + jump +
                    ", body=" + body +
                    '}';
        }
    }

    public static class Function extends Stmt {
        public final Token accessModifier; // Guarda o 'pub' ou 'priv' (pode ser null)
        public final boolean isStatic;
        public final boolean isAbstract;   // Verdadeiro se for um método abstrato
        public final Token name;
        public final List<Param> params;
        public final TypeNode returnType;
        public final List<Token> thrownExceptions;
        public final List<Stmt> body;      // Será 'null' se isAbstract for verdadeiro!
        public final java.util.List<DecoratorNode> decorators;
        public final List<Stmt.DecoratorNode> listeners;  // Os reativos (&)

        // Atualiza o construtor com os novos campos
        public Function(Token accessModifier, boolean isStatic, boolean isAbstract, Token name,
                        List<Param> params, TypeNode returnType, List<Token> thrownExceptions, List<Stmt> body, List<DecoratorNode> decorators, List<DecoratorNode> listeners) {
            this.accessModifier = accessModifier;
            this.isStatic = isStatic;
            this.isAbstract = isAbstract;
            this.name = name;
            this.params = params;
            this.returnType = returnType;
            this.thrownExceptions = thrownExceptions;
            this.body = body;
            this.decorators = decorators;
            this.listeners = listeners;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitFunctionStmt(this);
        }

        @Override
        public String toString() {
            return "Function{" +
                    "accessModifier=" + accessModifier +
                    ", isStatic=" + isStatic +
                    ", isAbstract=" + isAbstract +
                    ", name=" + name +
                    ", params=" + params +
                    ", returnType=" + returnType +
                    ", thrownExceptions=" + thrownExceptions +
                    ", body=" + body +
                    ", decorators=" + decorators +
                    ", listeners=" + listeners +
                    '}';
        }
    }

    public static class Break extends Stmt {
        public final Token keyword;

        public Break(Token keyword) {
            this.keyword = keyword;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitBreakStmt(this); }

        @Override
        public String toString() {
            return "Break{" +
                    "keyword=" + keyword +
                    '}';
        }
    }

    public static class Continue extends Stmt {
        public final Token keyword;

        public Continue(Token keyword) {
            this.keyword = keyword;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitContinueStmt(this); }

        @Override
        public String toString() {
            return "Continue{" +
                    "keyword=" + keyword +
                    '}';
        }
    }

    public static class Return extends Stmt {
        public final Token keyword;
        public final Expr value; // O que vai ser retornado (pode ser null)

        public Return(Token keyword, Expr value) {
            this.keyword = keyword;
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitReturnStmt(this); }

        @Override
        public String toString() {
            return "Return{" +
                    "keyword=" + keyword +
                    ", value=" + value +
                    '}';
        }
    }

    // No fundo do ficheiro Stmt.java:
    public static class Enum extends Stmt {
        public final Token name;
        public final List<Token> constants;

        public Enum(Token name, List<Token> constants) {
            this.name = name;
            this.constants = constants;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitEnumStmt(this); }

        @Override
        public String toString() {
            return "Enum{" +
                    "name=" + name +
                    ", constants=" + constants +
                    '}';
        }
    }

    // Representa um atributo/campo: "pub nome: string;"
    public static class FieldDecl {
        public final Token modifier;
        public final boolean isStatic; // ⭐ NOVO
        public final boolean isFinal;    // ⭐ NOVO
        public final boolean isReadonly; // ⭐ NOVO
        public final Token name;
        public final TypeNode type;

        public FieldDecl(Token modifier, boolean isStatic, boolean isFinal, boolean isReadonly, Token name, TypeNode type) {
            this.modifier = modifier;
            this.isStatic = isStatic;
            this.isFinal = isFinal;
            this.isReadonly = isReadonly;
            this.name = name;
            this.type = type;
        }

        @Override
        public String toString() {
            return "FieldDecl{" +
                    "modifier=" + modifier +
                    ", isStatic=" + isStatic +
                    ", isFinal=" + isFinal +
                    ", isReadonly=" + isReadonly +
                    ", name=" + name +
                    ", type=" + type +
                    '}';
        }
    }

    // Representa uma assinatura de método: "prot escrever(): string;"
    public static class FunctionSig {
        public final Token modifier; // PUB, PROT, PRIV
        public final Token name;
        // public final List<Token> parameters; (Simplificado para o exemplo)
        public final TypeNode returnType;
        public final List<Stmt.Param> parameters;

        public FunctionSig(Token modifier, Token name, List<Stmt.Param> parameters, TypeNode returnType) {
            this.modifier = modifier;
            this.name = name;
            this.parameters = parameters;
            this.returnType = returnType;
        }

        @Override
        public String toString() {
            return "FunctionSig{" +
                    "modifier=" + modifier +
                    ", name=" + name +
                    ", returnType=" + returnType +
                    ", parameters=" + parameters +
                    '}';
        }
    }

    public static class DeclareDecl extends Stmt {
        public final boolean isSealed;
        public final Token name;
        public final Token superclass;
        public final java.util.List<FieldDecl> fields;
        // ⭐ A NOVA RANHURA DA AST: Guarda os parâmetros de tipo (ex: [T, U])
        public final java.util.List<Token> typeParameters;
        public final List<Stmt.DecoratorNode> decorators; // Os metadados (@)
        public final List<Stmt.DecoratorNode> listeners;  // Os reativos (&)

        public DeclareDecl(boolean isSealed, Token name, Token superclass, java.util.List<FieldDecl> fields, List<Token> typeParameters, List<DecoratorNode> decorators, List<DecoratorNode> listeners) {
            this.isSealed = isSealed;
            this.name = name;
            this.superclass = superclass;
            this.fields = fields;
            this.typeParameters = typeParameters;
            this.decorators = decorators;
            this.listeners = listeners;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitDeclareDeclStmt(this); }

        @Override
        public String toString() {
            return "DeclareDecl{" +
                    "isSealed=" + isSealed +
                    ", name=" + name +
                    ", superclass=" + superclass +
                    ", fields=" + fields +
                    ", typeParameters=" + typeParameters +
                    ", decorators=" + decorators +
                    ", listeners=" + listeners +
                    '}';
        }
    }

    public static class InterfaceDecl extends Stmt {
        public final Token name;
        public final java.util.List<FunctionSig> methods;
        public final List<Stmt.DecoratorNode> decorators; // Os metadados (@)
        public final List<Stmt.DecoratorNode> listeners;  // Os reativos (&)
        public InterfaceDecl(Token name, java.util.List<FunctionSig> methods, List<DecoratorNode> decorators, List<DecoratorNode> listeners) {
            this.name = name;
            this.methods = methods;
            this.decorators = decorators;
            this.listeners = listeners;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitInterfaceDeclStmt(this);
        }

        @Override
        public String toString() {
            return "InterfaceDecl{" +
                    "name=" + name +
                    ", methods=" + methods +
                    ", decorators=" + decorators +
                    ", listeners=" + listeners +
                    '}';
        }
    }

    public static class ImplementDecl extends Stmt {
        public final Token targetName;  // O alvo base (Ex: Mamifero)
        public final Token aliasName;   // A variante opcional (Ex: Mam1 - pode ser null)
        public final java.util.Map<String, Expr> defaultState;
        public final java.util.List<Token> interfaces; // Os contratos (Ex: [CRUD, EXEC])
        public final java.util.List<Stmt.Function> methods; // As funções reais com corpo { ... }
        public final boolean isAbstract;
        // ⭐ NOVA RANHURA: Guarda os parâmetros genéricos do implement (ex: [T])
        public final java.util.List<Token> typeParameters;
        public final List<Stmt.DecoratorNode> decorators; // Os metadados (@)
        public final List<Stmt.DecoratorNode> listeners;  // Os reativos (&)
        public ImplementDecl(boolean isAbstract, Token targetName, Token aliasName,
                             List<Token> interfaces, Map<String, Expr> defaultState, List<Stmt.Function> methods, List<Token> typeParameters, List<DecoratorNode> decorators, List<DecoratorNode> listeners) {
            this.isAbstract = isAbstract;
            this.targetName = targetName;
            this.aliasName = aliasName;
            this.defaultState = defaultState;
            this.interfaces = interfaces;
            this.methods = methods;
            this.typeParameters = typeParameters;
            this.decorators = decorators;
            this.listeners = listeners;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitImplementDeclStmt(this);
        }

        @Override
        public String toString() {
            return "ImplementDecl{" +
                    "targetName=" + targetName +
                    ", aliasName=" + aliasName +
                    ", defaultState=" + defaultState +
                    ", interfaces=" + interfaces +
                    ", methods=" + methods +
                    ", isAbstract=" + isAbstract +
                    ", typeParameters=" + typeParameters +
                    ", decorators=" + decorators +
                    ", listeners=" + listeners +
                    '}';
        }
    }

    // Representa um parâmetro fortemente tipado: nome: tipo
    // ⭐ A EVOLUÇÃO PARA O PRESENTE:
    public static class Param {
        public final Token name;
        public final TypeNode typeNode; // Promovido!
        public final Expr defaultValue;

        public Param(Token name, TypeNode typeNode, Expr defaultValue) {
            this.name = name;
            this.typeNode = typeNode;
            this.defaultValue = defaultValue;
        }

        @Override
        public String toString() {
            return "Param{" +
                    "name=" + name +
                    ", typeNode=" + typeNode +
                    ", defaultValue=" + defaultValue +
                    '}';
        }
    }

    public static class Throw extends Stmt {
        public final Token keyword;
        public final Expr value;

        public Throw(Token keyword, Expr value) {
            this.keyword = keyword;
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitThrowStmt(this); }

        @Override
        public String toString() {
            return "Throw{" +
                    "keyword=" + keyword +
                    ", value=" + value +
                    '}';
        }
    }

    // ⭐ A NOVA CLÁUSULA CATCH ⭐
    public static class CatchClause {
        public final Token name;      // Ex: 'e'
        public final TypeNode type;   // Ex: 'NumberError' ou 'string'
        public final Stmt.Block body; // O código { ... }

        public CatchClause(Token name, TypeNode type, Stmt.Block body) {
            this.name = name;
            this.type = type;
            this.body = body;
        }

        @Override
        public String toString() {
            return "CatchClause{" +
                    "name=" + name +
                    ", type=" + type +
                    ", body=" + body +
                    '}';
        }
    }

    public static class Try extends Stmt {
        public final Stmt tryBlock;
        public final java.util.List<CatchClause> catchClauses; // Agora é uma LISTA de catches!
        public final Stmt finallyBlock;

        public Try(Stmt tryBlock, java.util.List<CatchClause> catchClauses, Stmt finallyBlock) {
            this.tryBlock = tryBlock;
            this.catchClauses = catchClauses;
            this.finallyBlock = finallyBlock;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitTryStmt(this); }

        @Override
        public String toString() {
            return "Try{" +
                    "tryBlock=" + tryBlock +
                    ", catchClauses=" + catchClauses +
                    ", finallyBlock=" + finallyBlock +
                    '}';
        }
    }

    // =========================================================================
    // ⭐ DECLARAÇÃO DE SINÓNIMO DE TIPO ( type Apelido = Alvo; )
    // =========================================================================
    public static class TypeAliasDecl extends Stmt {
        public final Token name;
        public final TypeNode targetType;
        public final List<Stmt.DecoratorNode> decorators; // Os metadados (@)
        public final List<Stmt.DecoratorNode> listeners;  // Os reativos (&)

        public TypeAliasDecl(Token name, TypeNode targetType, List<DecoratorNode> decorators, List<DecoratorNode> listeners) {
            this.name = name;
            this.targetType = targetType;
            this.decorators = decorators;
            this.listeners = listeners;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitTypeAliasDecl(this);
        }

        @Override
        public String toString() {
            return "TypeAliasDecl{" +
                    "name=" + name +
                    ", targetType=" + targetType +
                    ", decorators=" + decorators +
                    ", listeners=" + listeners +
                    '}';
        }
    }

    // ⭐ 1. A DECLARAÇÃO DO DECORADOR NA AST (O layout de memória)
    public static class DecoratorDecl extends Stmt {
        public final Token name;
        public final java.util.List<Stmt.FieldDecl> fields;
        public final java.util.List<Stmt.Function> methods;

        public DecoratorDecl(Token name, java.util.List<Stmt.FieldDecl> fields, List<Function> methods) {
            this.name = name;
            this.fields = fields;
            this.methods = methods;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitDecoratorDeclStmt(this);
        }

        @Override
        public String toString() {
            return "DecoratorDecl{" +
                    "name=" + name +
                    ", fields=" + fields +
                    ", methods=" + methods +
                    '}';
        }
    }

    // ⭐ 2. O AUTOCOLANTE DE METADADOS (A aplicação do Decorador)
    public static class DecoratorNode {
        public final Token name; // O Token "Logging"
        public final java.util.List<Expr.CallArg> arguments; // A lista bivalente (id: 12, nome: "...")

        public DecoratorNode(Token name, java.util.List<Expr.CallArg> arguments) {
            this.name = name;
            this.arguments = arguments;
        }

        @Override
        public String toString() {
            return "@" + name.lexeme + arguments;
        }
    }


    // =========================================================================
    // ⭐ NÓS DE MODULARIDADE ⭐
    // =========================================================================

    // 1. A Declaração do Módulo (Ex: module banco.modelos;)
    public static class ModuleDecl extends Stmt {
        public final String modulePath; // Ex: "banco.modelos"
        public final Token keyword;     // O token 'module' para reportar erros de linha

        public ModuleDecl(Token keyword, String modulePath) {
            this.keyword = keyword;
            this.modulePath = modulePath;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitModuleDeclStmt(this);
        }

        @Override
        public String toString() {
            return "ModuleDecl{" +
                    "modulePath='" + modulePath + '\'' +
                    ", keyword=" + keyword +
                    '}';
        }
    }

    // Estrutura Auxiliar para Imports com Alias (Ex: Cliente as Pessoa)
    public static class ImportSymbol {
        public final Token originalName;
        public final Token aliasName; // Pode ser null se não usar 'as'

        public ImportSymbol(Token originalName, Token aliasName) {
            this.originalName = originalName;
            this.aliasName = aliasName;
        }
    }

    // 2. A Declaração de Import (Ex: import banco.modelos.{Cliente as C};)
    public static class ImportDecl extends Stmt {
        public final String modulePath; // Ex: "banco.modelos"
        public final java.util.List<ImportSymbol> symbols; // Lista de símbolos. Vazia se for wildcard '*'
        public final boolean isWildcard; // true se for import pacote.*

        public final Token prefix;

        public ImportDecl(String modulePath, java.util.List<ImportSymbol> symbols, boolean isWildcard, Token prefix) {
            this.modulePath = modulePath;
            this.symbols = symbols;
            this.isWildcard = isWildcard;
            this.prefix = prefix;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitImportDeclStmt(this);
        }

        @Override
        public String toString() {
            return "ImportDecl{" +
                    "modulePath='" + modulePath + '\'' +
                    ", symbols=" + symbols +
                    ", isWildcard=" + isWildcard +
                    ", prefix=" + prefix +
                    '}';
        }
    }

    // 3. A Declaração de Export (Polimórfica)
    public static class ExportDecl extends Stmt {
        public final Stmt declaration; // Se for: export declare Pessoa {...}
        public final java.util.List<Token> inlineSymbols; // Se for: export Cliente, Pessoa;
        public final boolean isExportAll; // Se for: export all;

        // Construtor A: Exporta uma declaração embutida
        public ExportDecl(Stmt declaration) {
            this.declaration = declaration;
            this.inlineSymbols = null;
            this.isExportAll = false;
        }

        // Construtor B: Exporta uma lista de nomes ou 'all'
        public ExportDecl(java.util.List<Token> inlineSymbols, boolean isExportAll) {
            this.declaration = null;
            this.inlineSymbols = inlineSymbols;
            this.isExportAll = isExportAll;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitExportDeclStmt(this);
        }

        @Override
        public String toString() {
            return "ExportDecl{" +
                    "declaration=" + declaration +
                    ", inlineSymbols=" + inlineSymbols +
                    ", isExportAll=" + isExportAll +
                    '}';
        }
    }

    // =========================================================================
    // ⭐ NOVO: DECLARAÇÃO DE SUPERGLOBAL (Ex: global $_VERSION = "1.10";)
    // =========================================================================
    public static class GlobalDecl extends Stmt {
        public final Token name;
        public final TypeNode typeAnnotation; // Pode ser nulo
        public final Expr initializer;

        public GlobalDecl(Token name, TypeNode typeAnnotation, Expr initializer) {
            this.name = name;
            this.typeAnnotation = typeAnnotation;
            this.initializer = initializer;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitGlobalDeclStmt(this);
        }

        @Override
        public String toString() {
            return "GlobalDecl{" +
                    "name=" + name +
                    ", typeAnnotation=" + typeAnnotation +
                    ", initializer=" + initializer +
                    '}';
        }
    }

    public static class While extends Stmt {
        public final Expr condition;
        public final Stmt body; // Usamos o Block como tu bem desenhaste!

        public While(Expr condition, Stmt body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitWhileStmt(this);
        }

        @Override
        public String toString() {
            return "While{" +
                    "condition=" + condition +
                    ", body=" + body +
                    '}';
        }
    }
}