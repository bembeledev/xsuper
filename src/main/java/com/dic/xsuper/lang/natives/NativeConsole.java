package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.ControlFlow;
import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;

import java.util.List;
import java.util.Scanner;

public class NativeConsole {

    // Scanner global e estático para não causar conflitos na stream de entrada (System.in)
    private static final Scanner scanner = new Scanner(System.in);

    public static void register(Interpreter interpreter) {

        // =========================================================
        // 1. FUNÇÕES DE LEITURA DIRETA (Apenas travam e leem)
        // =========================================================
        interpreter.globals.defineConst("read_string", createReader(interpreter, "string"));
        //interpreter.globals.defineConst("read_char", createReader(interpreter, "char"));
        interpreter.globals.defineConst("read_int", createReader(interpreter, "long"));
        //interpreter.globals.defineConst("read_long", createReader(interpreter, "long"));
        //interpreter.globals.defineConst("read_short", createReader(interpreter, "short"));
        //interpreter.globals.defineConst("read_byte", createReader(interpreter, "byte"));
        interpreter.globals.defineConst("read_float", createReader(interpreter, "double"));
        //interpreter.globals.defineConst("read_double", createReader(interpreter, "double"));
        interpreter.globals.defineConst("read_bool", createReader(interpreter, "bool"));

        // =========================================================
        // 2. FUNÇÕES DE PROMPT (Imprimem a mensagem e depois leem)
        // =========================================================
        interpreter.globals.defineConst("prompt", createPrompt(interpreter, "string"));
        //interpreter.globals.defineConst("prompt_char", createPrompt(interpreter, "char"));
        interpreter.globals.defineConst("prompt_int", createPrompt(interpreter, "long"));
        //interpreter.globals.defineConst("prompt_long", createPrompt(interpreter, "long"));
        //interpreter.globals.defineConst("prompt_short", createPrompt(interpreter, "short"));
        //interpreter.globals.defineConst("prompt_byte", createPrompt(interpreter, "byte"));
        interpreter.globals.defineConst("prompt_float", createPrompt(interpreter, "double"));
        //interpreter.globals.defineConst("prompt_double", createPrompt(interpreter, "double"));
        interpreter.globals.defineConst("prompt_bool", createPrompt(interpreter, "bool"));
    }

    // --- FÁBRICA 1: Apenas Lê ---
    private static XplCallable createReader(Interpreter interpreter, String type) {
        return new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return parseInput(scanner.nextLine(), type);
            }
        };
    }

    // --- FÁBRICA 2: Imprime e Lê (O Clássico Prompt) ---
    private static XplCallable createPrompt(Interpreter interpreter, String type) {
        return new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                // Imprime a mensagem de ecrã (ex: "Qual é o teu nome? ")
                Object msg = intp.evaluate(args.get(0).expression);
                System.out.print(intp.stringify(msg));
                System.out.flush(); // Garante que a mensagem aparece imediatamente!

                return parseInput(scanner.nextLine(), type);
            }
        };
    }

    // --- O CONVERSOR DE SEGURANÇA ---
    private static Object parseInput(String input, String type) {
        try {
            String clean = input.trim();
            return switch (type) {
                case "string" -> input; // Mantém espaços originais (ex: "Fernando Bembele")
                //case "char"   -> input.isEmpty() ? "" : String.valueOf(input.charAt(0));
                // O motor matemático do XPL roda perfeitamente em Long e Double
                case "int", "long", "short", "byte" -> Long.parseLong(clean);
                case "float", "double" -> Double.parseDouble(clean);
                case "bool"   -> Boolean.parseBoolean(clean) || clean.equalsIgnoreCase("1") || clean.equalsIgnoreCase("sim") || clean.equalsIgnoreCase("yes");
                default -> input;
            };
        } catch (Exception e) {
            // Um erro nativo disparado para o Catch do XPL!
            throw new ControlFlow.RuntimeError(null, "Entrada inválida a partir da consola. Era esperado um valor do tipo: " + type);
        }
    }
}