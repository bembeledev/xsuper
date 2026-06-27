package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class NativeRegex {
    // Cache de padrões para não recompilar a mesma regex repetidamente
    private static final Map<String, Pattern> patternCache = new HashMap<>();

    public static void register(Interpreter interpreter) {
        // --- Operações Básicas ---
        define(interpreter, "regex_match", 2, args -> getPattern(args.getFirst()).matcher(args.get(1).toString()).matches());
        define(interpreter, "regex_test", 2, args -> getPattern(args.getFirst()).matcher(args.get(1).toString()).find());

        // --- Extração e Substituição ---
        define(interpreter, "regex_replace", 3, args -> getPattern(args.getFirst()).matcher(args.get(1).toString()).replaceFirst(args.get(2).toString()));
        define(interpreter, "regex_replace_all", 3, args -> getPattern(args.getFirst()).matcher(args.get(1).toString()).replaceAll(args.get(2).toString()));
        define(interpreter, "regex_split", 2, args -> List.of(getPattern(args.getFirst()).split(args.get(1).toString())));

        // --- Capturas ---
        define(interpreter, "regex_captures", 2, args -> {
            Matcher m = getPattern(args.get(0)).matcher(args.get(1).toString());
            List<String> caps = new ArrayList<>();
            if (m.find()) {
                for (int i = 0; i <= m.groupCount(); i++) caps.add(m.group(i));
            }
            return caps;
        });

        // --- Validações de Alto Nível (Shortcuts) ---
        define(interpreter, "regex_is_email", 1, args -> Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$").matcher(args.get(0).toString()).matches());
        define(interpreter, "regex_is_url", 1, args -> Pattern.compile("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$").matcher(args.get(0).toString()).matches());
        define(interpreter, "regex_is_number", 1, args -> args.getFirst().toString().matches("-?\\d+(\\.\\d+)?"));

        // --- Utilitários ---
        define(interpreter, "regex_escape", 1, args -> Pattern.quote(args.getFirst().toString()));
    }

    private static Pattern getPattern(Object regexStr) {
        String reg = regexStr.toString();
        return patternCache.computeIfAbsent(reg, Pattern::compile);
    }

    private static void define(Interpreter intp, String name, int arity, NativeAction action) {
        intp.globals.defineConst(name, new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<com.dic.xsuper.lang.Expr.CallArg> args) {
                List<Object> vals = new ArrayList<>();
                for (var arg : args) vals.add(intp.evaluate(arg.expression));
                return action.execute(vals);
            }
        });
    }

    private interface NativeAction { Object execute(List<Object> args); }
}