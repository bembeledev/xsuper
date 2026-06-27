package com.dic.xsuper.lang.poo.relection;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XplInstance;

public class MetaSwitchBuilder extends XplInstance {
    private Expr target;
    private java.util.List<Expr.SwitchCase> cases = new java.util.ArrayList<>();
    private Stmt defaultBranch = null;
    private Interpreter interpreter;

    public MetaSwitchBuilder(Expr target, Interpreter interpreter) {
        super(null);
        this.interpreter = interpreter;
        this.target = target;
        bindMethods();
    }

    public Stmt getAST() {
        return new Stmt.ExpressionStmt(new Expr.Switch(this.target, this.cases, this.defaultBranch));
    }

    private Stmt.Block extractToBlock(Object val) {
        if (val instanceof XplFunction xplFunc) return new Stmt.Block(xplFunc.declaration.body);
        throw new ControlFlow.RuntimeError(null, "O Builder exige um bloco encapsulado () => { ... }.");
    }

    private void bindMethods() {
        this.fields.put("Case", new XplCallable() {
            @Override public int arity() { return -1; } // Aceita 2 ou 3 argumentos (com CONTROL.BREAK)
            @Override public Object call(Interpreter intp, java.util.List<Expr.CallArg> args) {
                Object rawVal = intp.evaluate(args.get(0).expression);
                java.util.List<Expr> caseValues = new java.util.ArrayList<>();

                if (rawVal instanceof java.util.List<?> list) {
                    for (Object item : list) caseValues.add(new Expr.Literal(item));
                } else {
                    caseValues.add(new Expr.Literal(rawVal));
                }

                Stmt.Block bodyBlock = extractToBlock(intp.evaluate(args.get(1).expression));
                java.util.List<Stmt> stmts = new java.util.ArrayList<>(bodyBlock.statements);

                // Se o utilizador passou CONTROL.BREAK no 3º argumento, anexamos o salto à AST:
                if (args.size() >= 3) {
                    Object ctrl = intp.evaluate(args.get(2).expression);
                    if ("break".equals(ctrl)) {
                        stmts.add(new Stmt.Break(new Token(TokenType.BREAK, "break", null, 0, 0)));
                    }
                }

                cases.add(new Expr.SwitchCase(caseValues, new Stmt.Block(stmts)));
                return MetaSwitchBuilder.this;
            }
        });

        this.fields.put("Default", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, java.util.List<Expr.CallArg> args) {
                defaultBranch = extractToBlock(intp.evaluate(args.get(0).expression));
                return MetaSwitchBuilder.this;
            }
        });

        this.fields.put("execute", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, java.util.List<Expr.CallArg> args) {
                try {
                    intp.execute(getAST());
                } catch (ControlFlow.BreakException e) {
                    // Captura perfeita da luva de basebol!
                }
                return null;
            }
        });
    }
}