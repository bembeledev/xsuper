package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.ControlFlow;
import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.List;

/**
 * Módulo JSON: codifica/decodifica dados XPL para JSON.
 * Uso: json_encode(objeto) -> string
 *      json_decode(string) -> objeto (Map/List)
 */
public class NativeJson {
    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static void register(Interpreter interpreter) {
        interpreter.globals.defineConst("json_encode", buildFunc(1, (intp, args) -> {
            Object obj = intp.evaluate(args.get(0).expression);
            try {
                return mapper.writeValueAsString(obj);
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "json_encode: " + e.getMessage());
            }
        }));

        interpreter.globals.defineConst("json_decode", buildFunc(1, (intp, args) -> {
            String json = getString(intp, args, 0);
            try {
                return mapper.readValue(json, Object.class);
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "json_decode: " + e.getMessage());
            }
        }));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private static String getString(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        return interpreter.stringify(val);
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