package com.dic.xsuper.lang.natives.models;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.poo.XplClass;
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
            Object obj = intp.evaluate(args.get(0).expression);
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
            String jsonStr = intp.stringify(intp.evaluate(args.get(0).expression));
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