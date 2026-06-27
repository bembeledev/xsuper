package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;

import java.util.ArrayList;
import java.util.List;

public class NativeMath {
    public static void register(Interpreter interpreter) {
        // --- Trigonométricas ---
        define(interpreter, "sin", 1, args -> Math.sin(toDouble(args.getFirst())));
        define(interpreter, "cos", 1, args -> Math.cos(toDouble(args.getFirst())));
        define(interpreter, "tan", 1, args -> Math.tan(toDouble(args.getFirst())));
        define(interpreter, "asin", 1, args -> Math.asin(toDouble(args.getFirst())));
        define(interpreter, "acos", 1, args -> Math.acos(toDouble(args.getFirst())));
        define(interpreter, "atan", 1, args -> Math.atan(toDouble(args.getFirst())));
        define(interpreter, "atan2", 2, args -> Math.atan2(toDouble(args.getFirst()), toDouble(args.get(1))));

        // --- Exponenciais e Logarítmicas ---
        define(interpreter, "sqrt", 1, args -> Math.sqrt(toDouble(args.getFirst())));
        define(interpreter, "cbrt", 1, args -> Math.cbrt(toDouble(args.getFirst())));
        define(interpreter, "pow", 2, args -> Math.pow(toDouble(args.getFirst()), toDouble(args.get(1))));
        define(interpreter, "exp", 1, args -> Math.exp(toDouble(args.getFirst())));
        define(interpreter, "ln", 1, args -> Math.log(toDouble(args.getFirst())));
        define(interpreter, "log10", 1, args -> Math.log10(toDouble(args.getFirst())));

        // --- Arredondamento e Utilitários ---
        define(interpreter, "round", 1, args -> (double) Math.round(toDouble(args.getFirst())));
        define(interpreter, "floor", 1, args -> Math.floor(toDouble(args.getFirst())));
        define(interpreter, "ceil", 1, args -> Math.ceil(toDouble(args.getFirst())));
        define(interpreter, "abs", 1, args -> Math.abs(toDouble(args.getFirst())));
        define(interpreter, "sign", 1, args -> (double) Math.signum(toDouble(args.getFirst())));

        // --- Lógica e Validação ---
        define(interpreter, "min", 2, args -> Math.min(toDouble(args.getFirst()), toDouble(args.get(1))));
        define(interpreter, "max", 2, args -> Math.max(toDouble(args.getFirst()), toDouble(args.get(1))));
        define(interpreter, "is_nan", 1, args -> Double.isNaN(toDouble(args.getFirst())));
        define(interpreter, "is_infinite", 1, args -> Double.isInfinite(toDouble(args.getFirst())));

        // --- Aleatoriedade ---
        interpreter.globals.defineConst("random", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) { return Math.random(); }
        });

        interpreter.globals.defineConst("random_range", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                double min = toDouble(intp.evaluate(args.get(0).expression));
                double max = toDouble(intp.evaluate(args.get(1).expression));
                return min + (Math.random() * (max - min));
            }
        });

        // --- Interpolação Linear (lerp) ---
        define(interpreter, "lerp", 3, args -> {
            double a = toDouble(args.get(0));
            double b = toDouble(args.get(1));
            double t = toDouble(args.get(2));
            return a + t * (b - a);
        });
    }

    // Fábrica minimalista
    private static void define(Interpreter intp, String name, int arity, MathAction action) {
        intp.globals.defineConst(name, new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                List<Object> evaluated = new ArrayList<>();
                for (Expr.CallArg arg : args) evaluated.add(intp.evaluate(arg.expression));
                return action.execute(evaluated);
            }
        });
    }

    private interface MathAction { Object execute(List<Object> args); }

    private static double toDouble(Object obj) {
        if (obj instanceof Long l) return (double) l;
        if (obj instanceof Double d) return d;
        return 0.0;
    }
}