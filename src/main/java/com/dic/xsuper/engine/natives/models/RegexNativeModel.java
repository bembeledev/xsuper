package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.ast.Stmt;
import com.dic.xsuper.engine.ast.TypeNode;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.core.Token;
import com.dic.xsuper.engine.core.TokenType;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;
import com.dic.xsuper.engine.poo.XplInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexNativeModel {

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Regex", null);
        model.hasBaseImplementation = true;
        Token pubToken = new Token(TokenType.PUBLIC, "pub", null, 0, 0);

        // Campo 'pattern' da Instância (READONLY)
        model.addField(new Stmt.FieldDecl(
                pubToken, false, false, true,
                new Token(TokenType.IDENTIFIER, "pattern", null, 0, 0),
                new TypeNode.Simple(new Token(TokenType.T_STRING, "string", null, 0, 0))
        ));

        XplClass regexClass = new XplClass(model, interpreter.globals);

        // =========================================================
        // 1. INSTANCIAÇÃO: Regex.compile("pattern")
        // =========================================================
        model.staticFields.put("compile", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String patStr = intp.stringify(intp.evaluate(args.getFirst().expression));
                Pattern pattern = Pattern.compile(patStr);

                XplInstance inst = new XplInstance(regexClass);
                inst.fields.put("pattern", patStr);

                // .test(string) -> Devolve true se encontrar o padrão em qualquer lado
                inst.fields.put("test", buildInstanceMethod(1, (i, a) -> {
                    String text = i.stringify(i.evaluate(a.getFirst().expression));
                    return pattern.matcher(text).find();
                }));

                // .match(string) -> Devolve true apenas se a string inteira corresponder
                inst.fields.put("match", buildInstanceMethod(1, (i, a) -> {
                    String text = i.stringify(i.evaluate(a.getFirst().expression));
                    return pattern.matcher(text).matches();
                }));

                // .captures(string) -> Extrai os grupos capturados para um Array
                inst.fields.put("captures", buildInstanceMethod(1, (i, a) -> {
                    String text = i.stringify(i.evaluate(a.getFirst().expression));
                    Matcher m = pattern.matcher(text);
                    List<String> matches = new ArrayList<>();
                    while (m.find()) {
                        matches.add(m.group());
                    }
                    return matches;
                }));

                // .replaceFirst(string, replacement)
                inst.fields.put("replaceFirst", buildInstanceMethod(2, (i, a) -> {
                    String text = i.stringify(i.evaluate(a.get(0).expression));
                    String replacement = i.stringify(i.evaluate(a.get(1).expression));
                    return pattern.matcher(text).replaceFirst(replacement);
                }));

                // .replaceAll(string, replacement)
                inst.fields.put("replaceAll", buildInstanceMethod(2, (i, a) -> {
                    String text = i.stringify(i.evaluate(a.get(0).expression));
                    String replacement = i.stringify(i.evaluate(a.get(1).expression));
                    return pattern.matcher(text).replaceAll(replacement);
                }));

                // .split(string)
                inst.fields.put("split", buildInstanceMethod(1, (i, a) -> {
                    String text = i.stringify(i.evaluate(a.getFirst().expression));
                    return List.of(pattern.split(text));
                }));

                return inst;
            }
        });

        // =========================================================
        // 2. VALIDADORES ESTÁTICOS (camelCase)
        // =========================================================
        addStaticValidator(model, "isEmail", "^[A-Za-z0-9+_.-]+@(.+)$");
        addStaticValidator(model, "isUrl", "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$");
        addStaticValidator(model, "isNumber", "-?\\d+(\\.\\d+)?");
        addStaticValidator(model, "isUuid", "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
        addStaticValidator(model, "isHexColor", "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3}|[A-Fa-f0-9]{8})$");
        addStaticValidator(model, "isIpv4", "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
        addStaticValidator(model, "isBase64", "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$");
        addStaticValidator(model, "isStrongPassword", "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$");

        // =========================================================
        // 3. UTILITÁRIOS DE LIMPEZA ESTÁTICOS (camelCase)
        // =========================================================
        model.staticFields.put("escape", buildStaticUtility(1, (intp, args) ->
                Pattern.quote(intp.stringify(intp.evaluate(args.getFirst().expression)))
        ));

        model.staticFields.put("extractNumbers", buildStaticUtility(1, (intp, args) ->
                intp.stringify(intp.evaluate(args.getFirst().expression)).replaceAll("[^0-9]", "")
        ));

        model.staticFields.put("extractLetters", buildStaticUtility(1, (intp, args) ->
                intp.stringify(intp.evaluate(args.getFirst().expression)).replaceAll("[^a-zA-Z]", "")
        ));

        model.staticFields.put("normalizeSpaces", buildStaticUtility(1, (intp, args) ->
                intp.stringify(intp.evaluate(args.getFirst().expression)).trim().replaceAll("\\s{2,}", " ")
        ));

        model.staticFields.put("slugify", buildStaticUtility(1, (intp, args) -> {
            String text = intp.stringify(intp.evaluate(args.getFirst().expression)).toLowerCase();
            text = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
            return text.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        }));

        // Registar o modelo e a classe no ecossistema
        interpreter.registry_model.put("Regex", model);
        interpreter.environment.defineConst("Regex", regexClass);
    }

    // =========================================================================
    // MÁQUINAS AUXILIARES (Para manter o código limpo)
    // =========================================================================

    private static void addStaticValidator(XPLModel model, String methodName, String regexPattern) {
        Pattern pattern = Pattern.compile(regexPattern);
        model.staticFields.put(methodName, new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String text = intp.stringify(intp.evaluate(args.getFirst().expression));
                return pattern.matcher(text).matches();
            }
        });
    }

    private interface StaticAction {
        Object execute(Interpreter intp, List<Expr.CallArg> args);
    }

    private static XplCallable buildStaticUtility(int arity, StaticAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return action.execute(intp, args);
            }
        };
    }

    private static XplCallable buildInstanceMethod(int arity, StaticAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return action.execute(intp, args);
            }
        };
    }
}