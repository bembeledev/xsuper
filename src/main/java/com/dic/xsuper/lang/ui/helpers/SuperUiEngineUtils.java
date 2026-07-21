package com.dic.xsuper.lang.ui.helpers;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.ui.SuperUiEngine;
import com.dic.xsuper.lang.ui.document.XplEventListener;
import com.dic.xsuper.lang.ui.html.HtmlTagUtils;

import java.util.Collections;
import java.util.List;

public class SuperUiEngineUtils {

    public static void buildMethods(SuperUiEngine instance){
            // ─── MÉTODOS DA ENGINE ──────────────────────────────────────────────
        if (instance==null) return;
            // loadView(htmlSource)
        instance.fields.put("loadView", new XplCallable() {
                @Override public int arity() { return 1; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    String html = intp.evaluate(args.getFirst().expression).toString();
                    instance.loadView(html);
                    return null;
                }
            });

            // loadStyles(cssSource)
        instance.fields.put("loadStyles", new XplCallable() {
                @Override public int arity() { return 1; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    String css = intp.evaluate(args.getFirst().expression).toString();
                    instance.loadStyles(css);
                    return null;
                }
            });


            // ⭐ O NOVO RENDERIZADOR JSON DE CONSOLA ⭐
            instance.fields.put("printDOM", new XplCallable() {
                @Override public int arity() { return 1; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    Object obj = intp.evaluate(args.getFirst().expression);
                    String object = null;
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
                        //System.out.println(mapper.writeValueAsString(obj));
                        object = mapper.writeValueAsString(obj);
                    } catch (Exception e) {
                        System.out.println(obj);
                    }
                    return object;
                }
            });

            // showWindow(title, width, height)
            instance.fields.put("showWindow", new XplCallable() {
                @Override public int arity() { return 3; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    String title = intp.evaluate(args.get(0).expression).toString();
                    double width = Double.parseDouble(intp.evaluate(args.get(1).expression).toString());
                    double height = Double.parseDouble(intp.evaluate(args.get(2).expression).toString());

                    instance.showWindow(title, width, height);
                    return null;
                }
            });

            // loadView(htmlSource)
            instance.fields.put("defineTag", new XplCallable() {
                @Override public int arity() { return 2; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    String tagName = intp.evaluate(args.get(0).expression).toString();
                    Object tagElement = intp.evaluate(args.get(1).expression);
                    if (HtmlTagUtils.isNativeTag(tagName)) {
                        throw new RuntimeException("Erro de Estrutura: A tag '<" + tagName + ">' não é permitida dentro de um componente customizado.");
                    }
                    if (!HtmlTagUtils.isValidCustomTagName(tagName)) {
                        throw new RuntimeException(String.format(
                                "Erro de Estrutura: A tag customizada '<%s>' não pode ser usada neste contexto. " +
                                        "Esta secção espera apenas tags nativas da Web (como <div>, <p>, <span>). " +
                                        "Certifique-se de que o componente <%s> está a ser instanciado dentro de um container permitido (como <body>).",
                                tagName, tagName
                        ));
                    }
                    instance.setXplModel(tagName, tagElement);
                    return null;
                }
            });

            // renderCycle()
            instance.fields.put("renderCycle", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    instance.renderCycle();
                    return null;
                }
            });

            // dispatchEvent(eventName, payload, targetId)
            instance.fields.put("dispatchEvent", new XplCallable() {
                @Override public int arity() { return 3; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    String eventName = intp.evaluate(args.get(0).expression).toString();
                    Object payload = intp.evaluate(args.get(1).expression);
                    String targetId = intp.evaluate(args.get(2).expression).toString();
                    instance.dispatchEvent(eventName, payload, targetId);
                    return null;
                }
            });

            // addEventListener(type, listener) – suporta XplFunction ou XplEventListener
            instance.fields.put("addEventListener", new XplCallable() {
                @Override public int arity() { return -1; } // 2 ou 3 argumentos
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    if (args.size() < 2 || args.size() > 3) {
                        throw new IllegalArgumentException("addEventListener espera 2 ou 3 argumentos.");
                    }
                    String type = intp.evaluate(args.get(0).expression).toString();
                    Object listenerObj = intp.evaluate(args.get(1).expression);
                    if (listenerObj instanceof XplEventListener) {
                        instance.addEventListener(type, (XplEventListener) listenerObj);
                    } else if (listenerObj instanceof XplFunction) {
                        instance.addEventListener(type, (XplFunction) listenerObj);
                    } else {
                        throw new IllegalArgumentException("Ouvinte deve ser XplEventListener ou XplFunction.");
                    }
                    // Opções ignoradas
                    return null;
                }
            });

            // removeEventListener(type, listener)
            instance.fields.put("removeEventListener", new XplCallable() {
                @Override public int arity() { return 2; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    String type = intp.evaluate(args.get(0).expression).toString();
                    XplEventListener listener = (XplEventListener) intp.evaluate(args.get(1).expression);
                    // Para remover, precisamos ter uma lista de listeners; faremos uma busca
                    List<XplEventListener> list = instance.eventListeners.get(type);
                    if (list != null) {
                        list.remove(listener);
                    }
                    return null;
                }
            });

            // notifyStateChanged(id, name, value) – geralmente chamado internamente, mas pode ser exposto
            instance.fields.put("notifyStateChanged", new XplCallable() {
                @Override public int arity() { return 3; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    String id = intp.evaluate(args.get(0).expression).toString();
                    String name = intp.evaluate(args.get(1).expression).toString();
                    Object value = intp.evaluate(args.get(2).expression);
                    return null;
                }
            });

            // getDocument()
            instance.fields.put("getDocument", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    return instance.getDocument();
                }
            });

            // ─── PROPRIEDADES COMO MÉTODOS (GETTERS) ────────────────────────────

            // Versão da engine (exemplo)
            instance.fields.put("getVersion", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    return "1.0.0";
                }
            });

            // Nome da engine
            instance.fields.put("getName", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    return "SuperUiEngine";
                }
            });
        }
        
     public static XPLModel buildNativeModel() {
        if (SuperUiEngine.nativeModel == null) {
            SuperUiEngine.nativeModel = new XPLModel("SuperUiEngine", null);

            // ─── Campos (propriedades) ──────────────────────────────────────────────
            String[] fieldNames = {"document", "ui"};
            for (String f : fieldNames) {
                Token nameToken = new Token(TokenType.IDENTIFIER, f, null, 0, 0);
                // FieldDecl(Token modifier, boolean isStatic, boolean isFinal, boolean isReadonly, Token name, TypeNode type)
                Stmt.FieldDecl field = new Stmt.FieldDecl(null, false, false, false, nameToken, null);
                SuperUiEngine.nativeModel.addField(field);
            }

            // ─── Métodos ──────────────────────────────────────────────────────────────

            // loadView(html)
            List<Stmt.Param> paramsHTML = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "html", null, 0, 0), null, null)
            );
            SuperUiEngine.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "loadView", null, 0, 0),
                    paramsHTML,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // renderCycle()
            SuperUiEngine.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "renderCycle", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // dispatchEvent(eventName, payload, targetId)
            List<Stmt.Param> paramsEvent = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "eventName", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "payload", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "targetId", null, 0, 0), null, null)
            );
            SuperUiEngine.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "dispatchEvent", null, 0, 0),
                    paramsEvent,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // addEventListener(type, listener)
            List<Stmt.Param> paramsListener = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "type", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "listener", null, 0, 0), null, null)
            );
            SuperUiEngine.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "addEventListener", null, 0, 0),
                    paramsListener,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // removeEventListener(type, listener)
            SuperUiEngine.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "removeEventListener", null, 0, 0),
                    paramsListener,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // notifyStateChanged(id, name, value)
            List<Stmt.Param> paramsState = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "id", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "name", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "value", null, 0, 0), null, null)
            );
            SuperUiEngine.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "notifyStateChanged", null, 0, 0),
                    paramsState,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getDocument()
            SuperUiEngine.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getDocument", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getVersion()
            SuperUiEngine.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getVersion", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getName()
            SuperUiEngine.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getName", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getProperty(propName)
            List<Stmt.Param> paramsProp = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "propName", null, 0, 0), null, null)
            );
            SuperUiEngine.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getProperty", null, 0, 0),
                    paramsProp,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
        }
        return SuperUiEngine.nativeModel;
    }

}
