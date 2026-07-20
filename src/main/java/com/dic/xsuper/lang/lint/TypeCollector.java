package com.dic.xsuper.lang.lint;

import com.dic.xsuper.lang.*;

public class TypeCollector implements Stmt.Visitor<Void> {
    private final TypeEnvironment env;

    public TypeCollector(TypeEnvironment env) {
        this.env = env;
    }

    @Override
    public Void visitDeclareDeclStmt(Stmt.DeclareDecl stmt) {
        env.addType(stmt.name.lexeme, null);
        env.addClass(stmt.name.lexeme, stmt.superclass != null ? stmt.superclass.lexeme : null);
        for (Stmt.FieldDecl field : stmt.fields) {
            env.addClassField(stmt.name.lexeme, field);
        }
        return null;
    }

    @Override
    public Void visitImplementDeclStmt(Stmt.ImplementDecl stmt) {
        String className = stmt.aliasName != null ? stmt.aliasName.lexeme : stmt.targetName.lexeme;
        env.addClass(className, null); // pode herdar de declare, mas não temos super aqui
        for (Stmt.Function method : stmt.methods) {
            env.addClassMethod(className, method);
        }
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        env.addGlobalFunction(stmt.name.lexeme, stmt);
        return null;
    }

    // Outros visitantes vazios (não precisamos processar expressões agora)
    @Override public Void visitExpressionStmt(Stmt.ExpressionStmt stmt) { return null; }
    @Override public Void visitVarDeclStmt(Stmt.VarDecl stmt) { return null; }
    @Override public Void visitBlockStmt(Stmt.Block stmt) { return null; }
    @Override public Void visitForInStmt(Stmt.ForIn stmt) { return null; }
    @Override public Void visitForCStyleStmt(Stmt.ForCStyle stmt) { return null; }
    @Override public Void visitDoWhileStmt(Stmt.DoWhile stmt) { return null; }
    @Override public Void visitWhileStmt(Stmt.While stmt) { return null; }
    @Override public Void visitBreakStmt(Stmt.Break stmt) { return null; }
    @Override public Void visitContinueStmt(Stmt.Continue stmt) { return null; }
    @Override public Void visitReturnStmt(Stmt.Return stmt) { return null; }
    @Override public Void visitTryStmt(Stmt.Try stmt) { return null; }
    @Override public Void visitThrowStmt(Stmt.Throw stmt) { return null; }
    @Override public Void visitTypeAliasDecl(Stmt.TypeAliasDecl stmt) { return null; }
    @Override public Void visitDecoratorDeclStmt(Stmt.DecoratorDecl stmt) { return null; }
    @Override public Void visitModuleDeclStmt(Stmt.ModuleDecl stmt) { return null; }
    @Override public Void visitImportDeclStmt(Stmt.ImportDecl stmt) { return null; }
    @Override public Void visitExportDeclStmt(Stmt.ExportDecl stmt) { return null; }
    @Override public Void visitGlobalDeclStmt(Stmt.GlobalDecl stmt) { return null; }
    @Override public Void visitForInRangeStmt(Stmt.ForInRange stmt) { return null; }
    @Override public Void visitEnumStmt(Stmt.Enum stmt) { return null; }
    @Override public Void visitInterfaceDeclStmt(Stmt.InterfaceDecl stmt) { return null; }
}