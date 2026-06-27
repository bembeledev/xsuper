package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class NativeNetwork {
    public static void register(Interpreter interpreter) {

        // ping("google.com") -> devolve true ou false
        define(interpreter, "ping", 1, args -> {
            try { return InetAddress.getByName(args.getFirst().toString()).isReachable(3000); }
            catch (Exception e) { return false; }
        });

        // dns_lookup("google.com") -> devolve o IP (ex: "142.250.190.46")
        define(interpreter, "dns_lookup", 1, args -> {
            try { return InetAddress.getByName(args.getFirst().toString()).getHostAddress(); }
            catch (Exception e) { return "Host Desconhecido"; }
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