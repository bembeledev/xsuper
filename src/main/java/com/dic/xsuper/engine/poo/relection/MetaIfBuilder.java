package com.dic.xsuper.engine.poo.relection;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.ast.Stmt;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.execution.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.execution.XplFunction;
import com.dic.xsuper.engine.poo.XplInstance;

public class MetaIfBuilder extends XplInstance {
    private Expr.If rootIfNode;
    private Expr.If currentTail;
    private Interpreter interpreter;

    public MetaIfBuilder(Expr condition, Stmt.Block thenBlock, Interpreter interpreter) {
        super(); // Instância Meta (não tem classe base)
        this.interpreter = interpreter;
        this.rootIfNode = new Expr.If(condition, thenBlock, null);
        this.currentTail = this.rootIfNode;
        bindMethods();
    }

    public Stmt getAST() {
        return new Stmt.ExpressionStmt(this.rootIfNode);
    }

    // O Extrator de AST auxiliar (para podermos abrir as Arrow Functions)
    private Stmt.Block extractToBlock(Object val) {
        if (val instanceof XplFunction xplFunc) return new Stmt.Block(xplFunc.declaration.body);
        throw new ControlFlow.RuntimeError(null, "O Builder exige um bloco encapsulado () => { ... }.");
    }

    private Expr extractExpression(Object val) {
        if (val instanceof XplFunction xplFunc && !xplFunc.declaration.body.isEmpty()) {
            Stmt first = xplFunc.declaration.body.getFirst();
            if (first instanceof Stmt.Return ret) return ret.value;
            if (first instanceof Stmt.ExpressionStmt exprStmt) return exprStmt.expression;
        }
        throw new ControlFlow.RuntimeError(null, "Esperada uma expressão de condição.");
    }

    private void bindMethods() {
        // ⭐ .ElseIf()
        this.fields.put("ElseIf", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, java.util.List<Expr.CallArg> args) {
                Expr cond = extractExpression(intp.evaluate(args.get(0).expression));
                Stmt.Block block = extractToBlock(intp.evaluate(args.get(1).expression));
                Expr.If newIf = new Expr.If(cond, block, null);
                currentTail.elseBranch = new Stmt.ExpressionStmt(newIf);
                currentTail = newIf;
                return MetaIfBuilder.this;
            }
        });

        // ⭐ .Else()
        this.fields.put("Else", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, java.util.List<Expr.CallArg> args) {
                Stmt.Block block = extractToBlock(intp.evaluate(args.getFirst().expression));
                currentTail.elseBranch = block;
                return MetaIfBuilder.this;
            }
        });

        // ⭐ .execute()
        this.fields.put("execute", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, java.util.List<Expr.CallArg> args) {
                intp.execute(new Stmt.ExpressionStmt(rootIfNode));
                return null;
            }
        });
    }
}