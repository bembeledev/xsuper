package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.execution.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.execution.XplFunction;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;
import java.util.List;

public class AssertNativeModel {

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Assert", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        // Assert.eq(a, b)
        model.staticFields.put("eq", buildCallable(2, (intp, args) -> {
            Object actual = intp.evaluate(args.get(0).expression);
            Object expected = intp.evaluate(args.get(1).expression);
            if (!areEqual(actual, expected)) {
                throw new ControlFlow.RuntimeError(null, String.format("Assert falhou: esperado '%s' mas obtido '%s'", intp.stringify(expected), intp.stringify(actual)));
            }
            return true;
        }));

        // Assert.neq(a, b)
        model.staticFields.put("neq", buildCallable(2, (intp, args) -> {
            Object actual = intp.evaluate(args.get(0).expression);
            Object expected = intp.evaluate(args.get(1).expression);
            if (areEqual(actual, expected)) {
                throw new ControlFlow.RuntimeError(null, String.format("Assert falhou: ambos são estritamente iguais a '%s'", intp.stringify(actual)));
            }
            return true;
        }));

        // Assert.isTrue(cond)
        model.staticFields.put("isTrue", buildCallable(1, (intp, args) -> {
            Object cond = intp.evaluate(args.getFirst().expression);
            if (!isTruthy(cond)) {
                throw new ControlFlow.RuntimeError(null, "Assert falhou: a condição testada é FALSA.");
            }
            return true;
        }));

        // Assert.isFalse(cond)
        model.staticFields.put("isFalse", buildCallable(1, (intp, args) -> {
            Object cond = intp.evaluate(args.getFirst().expression);
            if (isTruthy(cond)) {
                throw new ControlFlow.RuntimeError(null, "Assert falhou: a condição testada é VERDADEIRA.");
            }
            return true;
        }));

        // Assert.throws(() => { ... })
        model.staticFields.put("throws", buildCallable(1, (intp, args) -> {
            Object fnObj = intp.evaluate(args.getFirst().expression);
            if (!(fnObj instanceof XplFunction fn)) {
                throw new ControlFlow.RuntimeError(null, "Assert.throws: argumento deve ser uma função () => { ... }");
            }
            try {
                fn.call(intp, List.of());
                throw new ControlFlow.RuntimeError(null, "Assert.throws falhou: A função executou sem lançar nenhuma exceção/erro.");
            } catch (Exception e) {
                // Sucesso! A função lançou erro como esperado
                return true;
            }
        }));

        interpreter.registry_model.put("Assert", model);
        interpreter.environment.defineConst("Assert", new XplClass(model, interpreter.globals));
    }

    // --- Helpers de Teste ---
    private static boolean areEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        return a.equals(b);
    }

    private static boolean isTruthy(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Boolean) return (Boolean) obj;
        return true;
    }

    private interface NativeAction { Object execute(Interpreter i, List<Expr.CallArg> a); }
    private static XplCallable buildCallable(int arity, NativeAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) { return action.execute(intp, args); }
        };
    }
}