package com.dic.xsuper.lang;

import com.dic.xsuper.lang.poo.relection.*;
import com.dic.xsuper.utils.ConsoleTheme;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class NativeVariables {
    public static void registry(Interpreter interpreter){
        // Função Nativa: println
        interpreter.globals.defineConst("println", new XplCallable() {
            @Override public int arity() { return -1; }

            @Override
            public Object call(Interpreter interpreter, java.util.List<Expr.CallArg> arguments) {

                // ⭐ 1. O FOGÃO: Cozinhamos os nós da AST transformando-os em matéria real!
                java.util.List<Object> argsCozinhados = Interpreter.unpackNativeArgs(interpreter,arguments);
                /*for (Expr.CallArg arg : arguments) {
                    // É ESTA INVOCACÃO QUE FAZ A SOMA DO "Nome: " + m.nome ACONTECER:
                    argsCozinhados.add(interpreter.evaluate(arg.expression));
                }*/

                if (argsCozinhados.isEmpty() || argsCozinhados.size() > 2) {
                    throw new RuntimeException("println espera 1 ou 2 argumentos.");
                }

                String texto = interpreter.stringify(argsCozinhados.get(0));

                if (argsCozinhados.size() == 2) {
                    System.out.println(interpreter.hexToAnsi(interpreter.stringify(argsCozinhados.get(1))) + texto + ConsoleTheme.RESET);
                } else {
                    System.out.println(ConsoleTheme.TEXT + texto + ConsoleTheme.RESET);
                }
                return null;
            }
        });

        // Função Nativa: print
        interpreter.globals.defineConst("print", new XplCallable() {
            @Override public int arity() { return -1; }

            @Override
            public Object call(Interpreter interpreter, java.util.List<Expr.CallArg> arguments) {

                java.util.List<Object> argsCozinhados = Interpreter.unpackNativeArgs(interpreter,arguments);
                /*for (Expr.CallArg arg : arguments) {
                    argsCozinhados.add(interpreter.evaluate(arg.expression));
                }*/
                if (argsCozinhados.isEmpty() || argsCozinhados.size() > 2) {
                    throw new RuntimeException("print espera 1 ou 2 argumentos.");
                }

                String texto = interpreter.stringify(argsCozinhados.getFirst());
                System.out.print(texto);
                System.out.flush();
                return null;
            }
        });

        // Função Nativa: shell
        interpreter.globals.defineConst("shell", new XplCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, java.util.List<Expr.CallArg> arguments) {
                // ⭐ A VACINA: Cozinha o CallArg transformando-o na string real ("ps", "ls", etc.)
                java.util.List<Object> argsCozinhados = Interpreter.unpackNativeArgs(interpreter, arguments);
                String commandStr = interpreter.stringify(argsCozinhados.getFirst());

                PrintStream originalOut = System.out;
                ByteArrayOutputStream memoryStream = new ByteArrayOutputStream();
                try (PrintStream captureOut = new PrintStream(memoryStream, true, StandardCharsets.UTF_8)) {
                    System.setOut(captureOut);
                    interpreter.currentDirectory = interpreter.registry.executeCommand(commandStr, interpreter.currentDirectory);
                } catch (Exception e) {
                    return "Erro no shell: " + e.getMessage();
                } finally {
                    System.setOut(originalOut);
                }
                return memoryStream.toString(StandardCharsets.UTF_8).trim();
            }
        });

        // =====================================================================
        // ⭐ CONSTRUTORES DA API FLUIDA JIT (If, For, While, Do, Switch)
        // =====================================================================

        interpreter.globals.defineConst("If", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter interpreter, java.util.List<Expr.CallArg> args) {
                Expr cond = Interpreter.extractExpression(interpreter.evaluate(args.get(0).expression));
                Stmt.Block thenBlock = Interpreter.extractToBlock(interpreter.evaluate(args.get(1).expression));
                // Chama a classe externa que criaste na pasta meta!
                return new MetaIfBuilder(cond, thenBlock, interpreter);
            }
        });

        interpreter.globals.defineConst("For", new XplCallable() {
            @Override public int arity() { return 4; }
            @Override public Object call(Interpreter interpreter, java.util.List<Expr.CallArg> args) {
                Stmt init = Interpreter.extractFirstStatement(interpreter.evaluate(args.get(0).expression));
                Expr cond = Interpreter.extractExpression(interpreter.evaluate(args.get(1).expression));
                Expr inc = Interpreter.extractExpression(interpreter.evaluate(args.get(2).expression));
                Stmt.Block body = Interpreter.extractToBlock(interpreter.evaluate(args.get(3).expression));
                // Chama a classe externa!
                return new MetaForBuilder(init, cond, inc, body, interpreter);
            }
        });

        // =====================================================================
        // ⭐ CONSTRUTORES DE LOOPS E SWITCHES (API Fluida)
        // =====================================================================

        interpreter.globals.defineConst("While", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter interpreter, java.util.List<Expr.CallArg> args) {
                Expr cond = Interpreter.extractExpression(interpreter.evaluate(args.get(0).expression));
                Stmt.Block body = Interpreter.extractToBlock(interpreter.evaluate(args.get(1).expression));
                return new MetaWhileBuilder(cond, body, interpreter);
            }
        });

        interpreter.globals.defineConst("Do", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter interpreter, java.util.List<Expr.CallArg> args) {
                Stmt.Block body = Interpreter.extractToBlock(interpreter.evaluate(args.getFirst().expression));
                return new MetaDoBuilder(body, interpreter);
            }
        });

        interpreter.globals.defineConst("Switch", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter interpreter, java.util.List<Expr.CallArg> args) {
                Expr target = Interpreter.extractExpression(interpreter.evaluate(args.getFirst().expression));
                return new MetaSwitchBuilder(target, interpreter);
            }
        });

        interpreter.globals.defineConst("Match", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter interpreter, java.util.List<Expr.CallArg> args) {
                Expr target = Interpreter.extractExpression(interpreter.evaluate(args.getFirst().expression));
                return new MetaMatchBuilder(target, interpreter);
            }
        });

        // =====================================================================
        // ⭐ FÁBRICAS DE METAPROGRAMAÇÃO JIT (AST BUILDERS) ⭐
        // =====================================================================

        // 1. Param("nome", TYPES.INT) -> Constrói um Stmt.Param nativo da linguagem
        interpreter.globals.defineConst("Param", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter interpreter, java.util.List<Expr.CallArg> args) {
                String nome = (String) interpreter.evaluate(args.get(0).expression);
                String tipo = (String) interpreter.evaluate(args.get(1).expression);
                return new Stmt.Param(
                        new Token(TokenType.IDENTIFIER, nome, null, 0, 0),
                        new TypeNode.Simple(new Token(TokenType.IDENTIFIER, tipo, null, 0, 0)),
                        null
                );
            }
        });

        // 2. Func("nome", VISIBILITY.PUB, [Params], TYPES.VOID, corpo) -> Constrói a Função para injetar
        interpreter.globals.defineConst("Func", new XplCallable() {
            @Override public int arity() { return 5; }
            @Override public Object call(Interpreter interpreter, java.util.List<Expr.CallArg> args) {
                String nome = (String) interpreter.evaluate(args.get(0).expression);
                String visibilidade = (String) interpreter.evaluate(args.get(1).expression);
                java.util.List<?> rawParams = (java.util.List<?>) interpreter.evaluate(args.get(2).expression);
                String retorno = (String) interpreter.evaluate(args.get(3).expression);

                // Extrai o bloco de dentro da Arrow Function () => { ... }
                Object blocoVal = interpreter.evaluate(args.get(4).expression);
                java.util.List<Stmt> corpoReal = new java.util.ArrayList<>();
                if (blocoVal instanceof XplFunction xf) {
                    corpoReal = xf.declaration.body;
                }

                // Converte a lista do XPL para a lista do Java
                java.util.List<Stmt.Param> astParams = new java.util.ArrayList<>();
                if (rawParams != null) {
                    for (Object rp : rawParams) {
                        if (rp instanceof Stmt.Param p) astParams.add(p);
                    }
                }

                TokenType visType = visibilidade.equals("pub") ? TokenType.PUBLIC : TokenType.PRIVATE;
                TypeNode retNode = retorno.equals("void") ? null : new TypeNode.Simple(new Token(TokenType.IDENTIFIER, retorno, null, 0, 0));

                // Devolve a Árvore Sintática da Função pronta a ser injetada pela Reflexão (::injectMethod)
                return new Stmt.Function(
                        new Token(visType, visibilidade, null, 0, 0),
                        false, false,
                        new Token(TokenType.IDENTIFIER, nome, null, 0, 0),
                        astParams, retNode, new java.util.ArrayList<>(),
                        corpoReal, new java.util.ArrayList<>()
                );
            }
        });



        // =====================================================================
        // ⭐ ENUMS NATIVOS DA LINGUAGEM (Matriz Exaustiva de Metaprogramação) ⭐
        // =====================================================================

        // 1. Tipos de Dados (Cobre todos os aliases do getPrimitiveTokenType)
        Map<String, Object> typesEnum = new java.util.LinkedHashMap<>();
        // Numéricos Inteiros
        typesEnum.put("INT", "long");
        //typesEnum.put("LONG", "long");
        //typesEnum.put("SHORT", "short");
        //typesEnum.put("BYTE", "byte");
        // Numéricos Decimais
        typesEnum.put("FLOAT", "double");
        //typesEnum.put("DOUBLE", "double");
        //typesEnum.put("NUMBER", "number");
        // Textuais e Lógicos
        typesEnum.put("STRING", "string");
        //typesEnum.put("CHAR", "char");
        typesEnum.put("BOOL", "bool");
        //typesEnum.put("BOOLEAN", "boolean");
        // Estruturas de Dados
        typesEnum.put("ARRAY", "array");
        typesEnum.put("LIST", "list");
        typesEnum.put("OBJECT", "object");
        typesEnum.put("MAP", "map");
        //typesEnum.put("DICT", "dict");
        // Especiais / Vácuo
        typesEnum.put("VOID", "void");
        typesEnum.put("ANY", "any");

        interpreter.globals.defineConst("TYPES", java.util.Collections.unmodifiableMap(typesEnum));

        // 2. Modificadores de Visibilidade
        Map<String, Object> visibilityEnum = new java.util.LinkedHashMap<>();
        visibilityEnum.put("PUB", "pub");
        visibilityEnum.put("PRIV", "priv");
        visibilityEnum.put("PROT", "prot");

        interpreter.globals.defineConst("VISIBILITY", java.util.Collections.unmodifiableMap(visibilityEnum));

        // 3. Modificadores de Comportamento (Para Metaprogramação JIT Avançada)
        Map<String, Object> modifiersEnum = new java.util.LinkedHashMap<>();
        modifiersEnum.put("STATIC", "static");
        modifiersEnum.put("FINAL", "final");
        modifiersEnum.put("READONLY", "readonly");
        modifiersEnum.put("ABSTRACT", "abstract");
        modifiersEnum.put("SEALED", "sealed");
        interpreter.globals.defineConst("MODIFIERS", java.util.Collections.unmodifiableMap(modifiersEnum));


        java.util.Map<String, Object> controlEnum = new java.util.LinkedHashMap<>();
        controlEnum.put("BREAK", "break");
        controlEnum.put("CONTINUE", "continue");
        interpreter.globals.defineConst("CONTROL", java.util.Collections.unmodifiableMap(controlEnum));


        // ⭐ CONSTANTES DE REDE E HTTP ⭐
        java.util.Map<String, String> httpConsts = new java.util.LinkedHashMap<>();
        httpConsts.put("GET", "GET");
        httpConsts.put("POST", "POST");
        httpConsts.put("PUT", "PUT");
        httpConsts.put("DELETE", "DELETE");
        httpConsts.put("PATCH", "PATCH");
        httpConsts.put("HEAD", "HEAD");
        httpConsts.put("OPTIONS", "OPTIONS");
        interpreter.globals.defineConst("HTTP", java.util.Collections.unmodifiableMap(httpConsts));

    }



}
