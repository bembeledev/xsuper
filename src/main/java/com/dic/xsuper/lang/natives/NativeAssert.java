package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.ControlFlow;
import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;
import com.dic.xsuper.lang.XplFunction;

import java.util.List;

/**
 * Módulo de asserções para testes unitários.
 * Uso: assert_eq(actual, expected)
 *      assert_neq(actual, expected)
 *      assert_true(cond)
 *      assert_false(cond)
 *      assert_throws(() => { ... }) -> espera que a função lance erro
 */
public class NativeAssert {
    public static void register(Interpreter interpreter) {
        interpreter.globals.defineConst("assert_eq", buildFunc(2, (intp, args) -> {
            Object actual = intp.evaluate(args.get(0).expression);
            Object expected = intp.evaluate(args.get(1).expression);
            if (!areEqual(actual, expected)) {
                throw new ControlFlow.RuntimeError(null,
                        String.format("assert_eq falhou: esperado '%s' mas obtido '%s'",
                                intp.stringify(expected), intp.stringify(actual)));
            }
            return true;
        }));

        interpreter.globals.defineConst("assert_neq", buildFunc(2, (intp, args) -> {
            Object actual = intp.evaluate(args.get(0).expression);
            Object expected = intp.evaluate(args.get(1).expression);
            if (areEqual(actual, expected)) {
                throw new ControlFlow.RuntimeError(null,
                        String.format("assert_neq falhou: ambos são '%s'", intp.stringify(actual)));
            }
            return true;
        }));

        interpreter.globals.defineConst("assert_true", buildFunc(1, (intp, args) -> {
            Object cond = intp.evaluate(args.get(0).expression);
            if (!isTruthy(cond)) {
                throw new ControlFlow.RuntimeError(null, "assert_true falhou: condição é falsa");
            }
            return true;
        }));

        interpreter.globals.defineConst("assert_false", buildFunc(1, (intp, args) -> {
            Object cond = intp.evaluate(args.get(0).expression);
            if (isTruthy(cond)) {
                throw new ControlFlow.RuntimeError(null, "assert_false falhou: condição é verdadeira");
            }
            return true;
        }));

        interpreter.globals.defineConst("assert_throws", buildFunc(1, (intp, args) -> {
            Object fnObj = intp.evaluate(args.get(0).expression);
            if (!(fnObj instanceof XplFunction)) {
                throw new ControlFlow.RuntimeError(null, "assert_throws: argumento deve ser uma função");
            }
            XplFunction fn = (XplFunction) fnObj;
            try {
                fn.call(intp, List.of());
                throw new ControlFlow.RuntimeError(null, "assert_throws falhou: a função não lançou exceção");
            } catch (Exception e) {
                // Esperado - sucesso
                return true;
            }
        }));
    }

    private static boolean areEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        // Para números, compara com tolerância
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

    @FunctionalInterface
    private interface NativeAction {
        Object execute(Interpreter interpreter, List<Expr.CallArg> args);
    }

    private static XplCallable buildFunc(int arity, NativeAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return action.execute(intp, args);
            }
        };
    }
}