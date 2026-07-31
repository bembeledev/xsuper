package com.dic.xsuper.engine.poo.relection;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.ast.Stmt;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.poo.XplInstance;

public class MetaForBuilder extends XplInstance {
    private final Stmt.ForCStyle forNode;
    private Interpreter interpreter;

    public MetaForBuilder(Stmt init, Expr cond, Expr inc, Stmt.Block body, Interpreter interpreter) {
        super();
        this.interpreter = interpreter;
        this.forNode = new Stmt.ForCStyle(init, cond, inc, body);
        bindMethods();
    }

    public Stmt getAST() {
        return this.forNode;
    }

    private void bindMethods() {
        // ⭐ .execute()
        this.fields.put("execute", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, java.util.List<Expr.CallArg> args) {
                // Aqui está o truque: Se tens um Stmt, executa-o diretamente!
                // NÃO TENTES AVALIAR O FOR COMO UMA EXPRESSÃO.
                intp.execute(forNode);
                return null;
            }
        });
    }
}