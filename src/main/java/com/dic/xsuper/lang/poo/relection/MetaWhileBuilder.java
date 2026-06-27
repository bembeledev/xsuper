package com.dic.xsuper.lang.poo.relection;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XplInstance;

public class MetaWhileBuilder extends XplInstance {
    private Stmt.While rootWhileNode;
    private Interpreter interpreter;

    public MetaWhileBuilder(Expr condition, Stmt.Block body, Interpreter interpreter) {
        super(null); // Instância JIT pura (sem classe XPL base)
        this.interpreter = interpreter;
        this.rootWhileNode = new Stmt.While(condition, body);
        bindMethods();
    }

    public Stmt getAST() {
        return this.rootWhileNode;
    }

    private void bindMethods() {
        this.fields.put("execute", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, java.util.List<Expr.CallArg> args) {
                try {
                    intp.execute(getAST());
                } catch (ControlFlow.BreakException e) {
                    // Interceção silenciosa: O break cumpriu o seu papel e saiu do loop!
                } catch (ControlFlow.ContinueException e) {}
                return null;
            }
        });
    }
}