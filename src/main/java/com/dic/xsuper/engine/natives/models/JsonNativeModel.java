package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.execution.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.List;

public class JsonNativeModel {

    // Instância única e global do Mapper para alta performance
    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Json", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false; // Modelo 100% estático

        // =========================================================
        // Json.encode(obj) -> String
        // =========================================================
        model.staticFields.put("encode", buildCallable(1, (intp, args) -> {
            Object obj = intp.evaluate(args.getFirst().expression);
            try {
                return mapper.writeValueAsString(obj);
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "Falha no Json.encode: " + e.getMessage());
            }
        }));

        // =========================================================
        // Json.decode(string) -> Object/Map/Array
        // =========================================================
        model.staticFields.put("decode", buildCallable(1, (intp, args) -> {
            String jsonStr = intp.stringify(intp.evaluate(args.getFirst().expression));
            try {
                return mapper.readValue(jsonStr, Object.class);
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "Falha no Json.decode: " + e.getMessage());
            }
        }));

        interpreter.registry_model.put("Json", model);
        interpreter.environment.defineConst("Json", new XplClass(model, interpreter.globals));
    }

    // --- Helper ---
    private interface NativeAction { Object execute(Interpreter i, List<Expr.CallArg> a); }
    private static XplCallable buildCallable(int arity, NativeAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) { return action.execute(intp, args); }
        };
    }
}