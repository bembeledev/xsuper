package com.dic.xsuper.engine.poo;

import com.dic.xsuper.engine.core.Environment;
import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.execution.XplFunction;
import com.dic.xsuper.engine.ast.Stmt;

import java.util.List;

public class XplClass implements XplCallable {
    public final XPLModel model;
    public final Environment closure;

    public XplClass(XPLModel model, Environment closure) {
        this.model = model;
        this.closure = closure;
    }

    @Override
    public int arity() {
        Stmt.Function initializer = model.findMethod("init");
        return initializer == null ? 0 : initializer.params.size();
    }

    // ⭐ A TUA ASSINATURA SOBERANA:
    @Override
    public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
        XplInstance instance = new XplInstance(this);
        Stmt.Function initializer = model.findMethod("init");

        if (initializer != null) {
            XPLModel owner = model.getOwnerOfMethod("init");
            XplFunction constructor = new XplFunction(initializer, closure, owner);

            // Passagem direta: Entregamos os CallArgs crus na mão da função init!
            constructor.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }

    @Override public String toString() { return "<class " + model.name + ">"; }
}