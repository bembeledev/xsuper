package com.dic.xsuper.lang.poo.relection;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XplInstance;

public class MetaMatchBuilder extends XplInstance {
    private Expr target;
    private java.util.List<Expr.MatchArm> arms = new java.util.ArrayList<>();
    private Stmt defaultBranch = null;
    private Interpreter interpreter;

    public MetaMatchBuilder(Expr target, Interpreter interpreter) {
        super(null);
        this.interpreter = interpreter;
        this.target = target;
        bindMethods();
    }

    public Stmt getAST() {
        return new Stmt.ExpressionStmt(new Expr.Match(this.target, this.arms, this.defaultBranch));
    }

    private Stmt.Block extractToBlock(Object val) {
        if (val instanceof XplFunction xplFunc) return new Stmt.Block(xplFunc.declaration.body);
        throw new ControlFlow.RuntimeError(null, "O Builder exige um bloco encapsulado () => { ... }.");
    }

    private Expr extractExpression(Object val) {
        if (val instanceof XplFunction xplFunc && !xplFunc.declaration.body.isEmpty()) {
            Stmt first = xplFunc.declaration.body.get(0);
            if (first instanceof Stmt.Return ret) return ret.value;
            if (first instanceof Stmt.ExpressionStmt exprStmt) return exprStmt.expression;
        }
        return new Expr.Literal(val);
    }

    private void bindMethods() {
        this.fields.put("Arm", new XplCallable() {
            @Override public int arity() { return -1; } // Suporta 2 (valor, bloco) ou 3 (valor, guarda, bloco)
            @Override public Object call(Interpreter intp, java.util.List<Expr.CallArg> args) {
                Expr valueTest = new Expr.Literal(intp.evaluate(args.get(0).expression));
                Expr guard = null;
                Stmt.Block body = null;

                if (args.size() == 2) {
                    body = extractToBlock(intp.evaluate(args.get(1).expression));
                } else if (args.size() >= 3) {
                    guard = extractExpression(intp.evaluate(args.get(1).expression));
                    body = extractToBlock(intp.evaluate(args.get(2).expression));
                }

                arms.add(new Expr.MatchArm(null, valueTest, guard, body));
                return MetaMatchBuilder.this;
            }
        });

        this.fields.put("Default", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, java.util.List<Expr.CallArg> args) {
                defaultBranch = extractToBlock(intp.evaluate(args.get(0).expression));
                return MetaMatchBuilder.this;
            }
        });

        this.fields.put("execute", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, java.util.List<Expr.CallArg> args) {
                intp.execute(getAST());
                return null;
            }
        });
    }
}