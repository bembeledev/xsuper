package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.execution.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;
import java.util.List;
import java.util.Scanner;

public class ConsoleNativeModel {

    // Scanner global estático para não causar conflitos na stream de entrada (System.in)
    private static final Scanner scanner = new Scanner(System.in);

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Console", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false; // 100% estático

        // =========================================================
        // 1. LEITURA DIRETA (Console.readLine, Console.readInt...)
        // =========================================================
        model.staticFields.put("readLine", createReader("string"));
        model.staticFields.put("readInt", createReader("long"));
        model.staticFields.put("readFloat", createReader("double"));
        model.staticFields.put("readBool", createReader("bool"));

        // =========================================================
        // 2. PROMPT (Imprime mensagem e lê na mesma linha)
        // =========================================================
        model.staticFields.put("prompt", createPrompt("string"));
        model.staticFields.put("promptInt", createPrompt("long"));
        model.staticFields.put("promptFloat", createPrompt("double"));
        model.staticFields.put("promptBool", createPrompt("bool"));

        // =========================================================
        // 3. UTILITÁRIOS (Opcional, mas dá jeito para limpar terminal)
        // =========================================================
        model.staticFields.put("clear", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                System.out.print("\033[H\033[2J");
                System.out.flush();
                return null;
            }
        });

        interpreter.registry_model.put("Console", model);
        interpreter.environment.defineConst("Console", new XplClass(model, interpreter.globals));
    }

    // --- MÁQUINAS AUXILIARES DO CONSOLE ---

    private static XplCallable createReader(String type) {
        return new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return parseInput(scanner.nextLine(), type);
            }
        };
    }

    private static XplCallable createPrompt(String type) {
        return new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                Object msg = intp.evaluate(args.getFirst().expression);
                System.out.print(intp.stringify(msg));
                System.out.flush();
                return parseInput(scanner.nextLine(), type);
            }
        };
    }

    private static Object parseInput(String input, String type) {
        try {
            String clean = input.trim();
            return switch (type) {
                case "string" -> input;
                case "long" -> Long.parseLong(clean);
                case "double" -> Double.parseDouble(clean);
                case "bool" -> Boolean.parseBoolean(clean) || clean.equalsIgnoreCase("1") || clean.equalsIgnoreCase("sim") || clean.equalsIgnoreCase("yes");
                default -> input;
            };
        } catch (Exception e) {
            throw new ControlFlow.RuntimeError(null, "Entrada inválida a partir da consola. Era esperado um valor do tipo: " + type);
        }
    }
}