package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class NativeUrl {
    public static void register(Interpreter interpreter) {

        define(interpreter, "url_encode", 1, args -> URLEncoder.encode(args.getFirst().toString(), StandardCharsets.UTF_8));
        define(interpreter, "url_decode", 1, args -> URLDecoder.decode(args.getFirst().toString(), StandardCharsets.UTF_8));

        // Extrai o domínio principal de um URL (ex: "https://api.github.com/users" -> "api.github.com")
        define(interpreter, "url_parse_host", 1, args -> {
            try { return URI.create(args.getFirst().toString()).getHost(); }
            catch (Exception e) { return ""; }
        });
    }

    private interface NativeAction { Object execute(List<Object> args); }

    private static void define(Interpreter intp, String name, int arity, NativeAction action) {
        intp.globals.defineConst(name, new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                List<Object> vals = new ArrayList<>();
                for (var arg : args) vals.add(intp.evaluate(arg.expression));
                return action.execute(vals);
            }
        });
    }
}