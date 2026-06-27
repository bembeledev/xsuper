package com.dic.xsuper.lang.poo.relection;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XplInstance;

public class MetaDoBuilder extends XplInstance {
    private Stmt.Block body;
    private Expr condition;
    private Interpreter interpreter;

    public MetaDoBuilder(Stmt.Block body, Interpreter interpreter) {
        super(null);
        this.interpreter = interpreter;
        this.body = body;
        bindMethods();
    }

    public Stmt getAST() {
        if (this.condition == null) {
            throw new ControlFlow.RuntimeError(null, "O construtor Do() exige uma chamada encadeada .While(condicao).");
        }
        return new Stmt.DoWhile(this.body, this.condition);
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
        this.fields.put("While", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, java.util.List<Expr.CallArg> args) {
                Object val = intp.evaluate(args.get(0).expression);
                MetaDoBuilder.this.condition = extractExpression(val);
                return MetaDoBuilder.this;
            }
        });

        this.fields.put("execute", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, java.util.List<Expr.CallArg> args) {
                try {
                    intp.execute(getAST());
                } catch (ControlFlow.BreakException e) {}
                return null;
            }
        });
    }
}