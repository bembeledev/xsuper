package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.execution.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MathNativeModel {

    private static final Random random = new Random();

    public static void Registry(Interpreter interpreter) {
        // 1. Modelo estático
        XPLModel model = new XPLModel("Math", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        // 2. Constantes
        model.staticFields.put("PI", Math.PI);
        model.staticFields.put("E", Math.E);
        model.staticFields.put("INFINITY", Double.POSITIVE_INFINITY);
        model.staticFields.put("NAN", Double.NaN);
        model.staticFields.put("EPSILON", 2.220446049250313e-16);
        model.staticFields.put("MAX_INT", Long.MAX_VALUE);
        model.staticFields.put("MIN_INT", Long.MIN_VALUE);


        // 3. Funções matemáticas
        define(model, "sin", 1, args -> Math.sin(toDouble(args.getFirst())));
        define(model, "cos", 1, args -> Math.cos(toDouble(args.getFirst())));
        define(model, "tan", 1, args -> Math.tan(toDouble(args.getFirst())));
        define(model, "asin", 1, args -> Math.asin(toDouble(args.getFirst())));
        define(model, "acos", 1, args -> Math.acos(toDouble(args.getFirst())));
        define(model, "atan", 1, args -> Math.atan(toDouble(args.getFirst())));
        define(model, "atan2", 2, args -> Math.atan2(toDouble(args.getFirst()), toDouble(args.get(1))));

        define(model, "sinh", 1, args -> Math.sinh(toDouble(args.getFirst())));
        define(model, "cosh", 1, args -> Math.cosh(toDouble(args.getFirst())));
        define(model, "tanh", 1, args -> Math.tanh(toDouble(args.getFirst())));

        define(model, "toRadians", 1, args -> Math.toRadians(toDouble(args.getFirst())));
        define(model, "toDegrees", 1, args -> Math.toDegrees(toDouble(args.getFirst())));

        define(model, "exp", 1, args -> Math.exp(toDouble(args.getFirst())));
        define(model, "expm1", 1, args -> Math.expm1(toDouble(args.getFirst())));
        define(model, "log", 1, args -> Math.log(toDouble(args.getFirst())));
        define(model, "log10", 1, args -> Math.log10(toDouble(args.getFirst())));
        define(model, "log1p", 1, args -> Math.log1p(toDouble(args.getFirst())));
        define(model, "pow", 2, args -> Math.pow(toDouble(args.getFirst()), toDouble(args.get(1))));
        define(model, "sqrt", 1, args -> Math.sqrt(toDouble(args.getFirst())));
        define(model, "cbrt", 1, args -> Math.cbrt(toDouble(args.getFirst())));
        define(model, "hypot", 2, args -> Math.hypot(toDouble(args.getFirst()), toDouble(args.get(1))));

        define(model, "abs", 1, args -> Math.abs(toDouble(args.getFirst())));
        define(model, "ceil", 1, args -> Math.ceil(toDouble(args.getFirst())));
        define(model, "floor", 1, args -> Math.floor(toDouble(args.getFirst())));
        define(model, "round", 1, args -> (double) Math.round(toDouble(args.getFirst())));
        define(model, "rint", 1, args -> Math.rint(toDouble(args.getFirst())));
        define(model, "signum", 1, args -> Math.signum(toDouble(args.getFirst())));
        define(model, "nextUp", 1, args -> Math.nextUp(toDouble(args.getFirst())));
        define(model, "nextDown", 1, args -> Math.nextDown(toDouble(args.getFirst())));

        define(model, "min", 2, args -> Math.min(toDouble(args.getFirst()), toDouble(args.get(1))));
        define(model, "max", 2, args -> Math.max(toDouble(args.getFirst()), toDouble(args.get(1))));
        define(model, "clamp", 3, args -> {
            double v = toDouble(args.get(0));
            double min = toDouble(args.get(1));
            double max = toDouble(args.get(2));
            if (v < min) return min;
            if (v > max) return max;
            return v;
        });
        define(model, "lerp", 3, args -> {
            double a = toDouble(args.get(0));
            double b = toDouble(args.get(1));
            double t = toDouble(args.get(2));
            return a + t * (b - a);
        });
        define(model, "isFinite", 1, args -> Double.isFinite(toDouble(args.getFirst())));
        define(model, "isNaN", 1, args -> Double.isNaN(toDouble(args.getFirst())));
        define(model, "isInfinite", 1, args -> Double.isInfinite(toDouble(args.getFirst())));

        define(model, "copySign", 2, args -> Math.copySign(toDouble(args.getFirst()), toDouble(args.get(1))));
        define(model, "scalb", 2, args -> Math.scalb(toDouble(args.getFirst()), (int) toDouble(args.get(1))));
        define(model, "ulp", 1, args -> Math.ulp(toDouble(args.getFirst())));

        // 4. Aleatoriedade
        define(model, "random", 0, args -> Math.random());
        define(model, "randomInt", 2, args -> {
            long min = (long) toDouble(args.get(0));
            long max = (long) toDouble(args.get(1));
            if (min > max) throw new ControlFlow.RuntimeError(null, "randomInt: min deve ser <= max");
            return min + (long) (Math.random() * (max - min + 1));
        });
        define(model, "randomDouble", 2, args -> {
            double min = toDouble(args.get(0));
            double max = toDouble(args.get(1));
            if (min > max) throw new ControlFlow.RuntimeError(null, "randomDouble: min deve ser <= max");
            return min + Math.random() * (max - min);
        });
        define(model, "randomGaussian", 0, args -> random.nextGaussian());
        define(model, "randomGaussian", 2, args -> {
            double mean = toDouble(args.get(0));
            double stddev = toDouble(args.get(1));
            return mean + stddev * random.nextGaussian();
        });

        // 5. Estatísticas sobre coleções
        define(model, "sum", 1, args -> sumCollection(args.getFirst()));
        define(model, "average", 1, args -> averageCollection(args.getFirst()));
        define(model, "minOf", 1, args -> minOfCollection(args.getFirst()));
        define(model, "maxOf", 1, args -> maxOfCollection(args.getFirst()));
        define(model, "variance", 1, args -> varianceCollection(args.getFirst()));
        define(model, "stddev", 1, args -> stddevCollection(args.getFirst()));

        // 6. Registar a classe Math
        XplClass mathClass = new XplClass(model, interpreter.globals);
        interpreter.registry_model.put("Math", model);
        interpreter.environment.defineConst("Math", mathClass);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private static void define(XPLModel model, String name, int arity, MathAction action) {
        model.staticFields.put(name, new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                List<Object> evaluated = new ArrayList<>();
                for (Expr.CallArg arg : args) evaluated.add(intp.evaluate(arg.expression));
                return action.execute(evaluated);
            }
        });
    }

    @FunctionalInterface
    private interface MathAction {
        Object execute(List<Object> args);
    }

    private static double toDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        if (obj instanceof String) {
            try { return Double.parseDouble((String) obj); } catch (NumberFormatException e) { /* ignore */ }
        }
        return 0.0;
    }

    // ─── Estatísticas sobre coleções ─────────────────────────────────────

    private static List<Double> extractDoubles(Object obj) {
        if (obj instanceof List) {
            List<Double> result = new ArrayList<>();
            for (Object item : (List<?>) obj) {
                if (item instanceof Number) {
                    result.add(((Number) item).doubleValue());
                } else if (item instanceof String) {
                    try { result.add(Double.parseDouble((String) item)); } catch (NumberFormatException ignored) {}
                }
            }
            return result;
        }
        throw new ControlFlow.RuntimeError(null, "Esperado uma lista/array de números.");
    }

    private static Object sumCollection(Object obj) {
        List<Double> nums = extractDoubles(obj);
        return nums.stream().mapToDouble(Double::doubleValue).sum();
    }

    private static Object averageCollection(Object obj) {
        List<Double> nums = extractDoubles(obj);
        if (nums.isEmpty()) return Double.NaN;
        return nums.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    private static Object minOfCollection(Object obj) {
        List<Double> nums = extractDoubles(obj);
        if (nums.isEmpty()) return Double.NaN;
        return nums.stream().mapToDouble(Double::doubleValue).min().orElse(Double.NaN);
    }

    private static Object maxOfCollection(Object obj) {
        List<Double> nums = extractDoubles(obj);
        if (nums.isEmpty()) return Double.NaN;
        return nums.stream().mapToDouble(Double::doubleValue).max().orElse(Double.NaN);
    }

    private static Object varianceCollection(Object obj) {
        List<Double> nums = extractDoubles(obj);
        if (nums.size() < 2) return Double.NaN;
        double mean = nums.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return nums.stream().mapToDouble(d -> (d - mean) * (d - mean)).average().orElse(0);
    }

    private static Object stddevCollection(Object obj) {
        double var = (double) varianceCollection(obj);
        return Math.sqrt(var);
    }
}