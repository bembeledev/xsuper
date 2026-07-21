package com.dic.xsuper.lang.ui.document.helpers;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.ui.document.XplDocument;
import com.dic.xsuper.lang.ui.document.XplElement;
import com.dic.xsuper.lang.ui.document.XplEventListener;
import com.dic.xsuper.lang.ui.event.XplEvent;

import java.util.Collections;
import java.util.List;

public class XplDocumentUtils {
    public static XPLModel buildNativeModel() {
        if (XplDocument.nativeModel == null) {
            XplDocument.nativeModel = new XPLModel("XplDocument", null);

            // ─── Campos (propriedades) ──────────────────────────────────────────────
            String[] fieldNames = {
                    "documentElement", "body", "head", "title", "URL", "domain",
                    "referrer", "cookie", "characterSet", "contentType", "compatMode",
                    "designMode", "dir", "lastModified", "location", "readyState",
                    "hidden", "visibilityState", "childElementCount",
                    "children", "firstElementChild", "lastElementChild",
                    "doctype", "documentURI", "origin", "baseURI"
            };
            for (String f : fieldNames) {
                Token nameToken = new Token(TokenType.IDENTIFIER, f, null, 0, 0);
                // FieldDecl(Token modifier, boolean isStatic, boolean isFinal, boolean isReadonly, Token name, TypeNode type)
                Stmt.FieldDecl field = new Stmt.FieldDecl(null, false, false, false, nameToken, null);
                XplDocument.nativeModel.addField(field);
            }

            // ─── Métodos ──────────────────────────────────────────────────────────────

            // createElement(tagName)
            List<Stmt.Param> params1 = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "tagName", null, 0, 0), null, null)
            );
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createElement", null, 0, 0),
                    params1,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // createElementNS(namespaceURI, qualifiedName)
            List<Stmt.Param> params2 = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "namespaceURI", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "qualifiedName", null, 0, 0), null, null)
            );
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createElementNS", null, 0, 0),
                    params2,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // createTextNode(data)
            List<Stmt.Param> paramsData = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "data", null, 0, 0), null, null)
            );
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createTextNode", null, 0, 0),
                    paramsData,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // createComment(data)
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createComment", null, 0, 0),
                    paramsData,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // createDocumentFragment()
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createDocumentFragment", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // createAttribute(name)
            List<Stmt.Param> paramsName = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "name", null, 0, 0), null, null)
            );
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createAttribute", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // createAttributeNS(namespaceURI, qualifiedName)
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createAttributeNS", null, 0, 0),
                    params2,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getElementById(id)
            List<Stmt.Param> paramsId = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "id", null, 0, 0), null, null)
            );
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getElementById", null, 0, 0),
                    paramsId,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getElementsByTagName(tagName)
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getElementsByTagName", null, 0, 0),
                    params1,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getElementsByClassName(className)
            List<Stmt.Param> paramsClass = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "className", null, 0, 0), null, null)
            );
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getElementsByClassName", null, 0, 0),
                    paramsClass,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getElementsByName(name)
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getElementsByName", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // querySelector(selector)
            List<Stmt.Param> paramsSelector = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "selector", null, 0, 0), null, null)
            );
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "querySelector", null, 0, 0),
                    paramsSelector,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // querySelectorAll(selector)
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "querySelectorAll", null, 0, 0),
                    paramsSelector,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getElementByIdNS(namespaceURI, localName)
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getElementByIdNS", null, 0, 0),
                    params2,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getElementsByTagNameNS(namespaceURI, localName)
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getElementsByTagNameNS", null, 0, 0),
                    params2,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // Navegação (sem parâmetros)
            String[] navMethods = {
                    "getRootNode", "getActiveElement", "getAnchors", "getApplets", "getEmbeds",
                    "getForms", "getImages", "getLinks", "getScripts", "getStylesheets"
            };
            for (String m : navMethods) {
                XplDocument.nativeModel.addMethod(new Stmt.Function(
                        null, false, false,
                        new Token(TokenType.IDENTIFIER, m, null, 0, 0),
                        Collections.emptyList(),
                        null,
                        null,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList()
                ));
            }

            // write, writeln
            List<Stmt.Param> paramsWrite = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "text", null, 0, 0), null, null)
            );
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "write", null, 0, 0),
                    paramsWrite,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "writeln", null, 0, 0),
                    paramsWrite,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // Eventos
            List<Stmt.Param> paramsEventListener = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "type", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "listener", null, 0, 0), null, null)
            );
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "addEventListener", null, 0, 0),
                    paramsEventListener,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "removeEventListener", null, 0, 0),
                    paramsEventListener,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            List<Stmt.Param> paramsEvent = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "event", null, 0, 0), null, null)
            );
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "dispatchEvent", null, 0, 0),
                    paramsEvent,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // Utilidades
            List<Stmt.Param> paramsImport = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "node", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "deep", null, 0, 0), null, null)
            );
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "importNode", null, 0, 0),
                    paramsImport,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            List<Stmt.Param> paramsNode = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "node", null, 0, 0), null, null)
            );
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "adoptNode", null, 0, 0),
                    paramsNode,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createRange", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            List<Stmt.Param> paramsExec = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "commandId", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "showUI", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "value", null, 0, 0), null, null)
            );
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "execCommand", null, 0, 0),
                    paramsExec,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            List<Stmt.Param> paramsCmd = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "commandId", null, 0, 0), null, null)
            );
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "queryCommandEnabled", null, 0, 0),
                    paramsCmd,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "queryCommandSupported", null, 0, 0),
                    paramsCmd,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "queryCommandValue", null, 0, 0),
                    paramsCmd,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "close", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            List<Stmt.Param> paramsOpen = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "url", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "name", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "features", null, 0, 0), null, null)
            );
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "open", null, 0, 0),
                    paramsOpen,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // Novos métodos
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "contains", null, 0, 0),
                    paramsNode,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "hasFocus", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getSelection", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            List<Stmt.Param> paramsPoint = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "x", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "y", null, 0, 0), null, null)
            );
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "elementFromPoint", null, 0, 0),
                    paramsPoint,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "elementsFromPoint", null, 0, 0),
                    paramsPoint,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createEvent", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            List<Stmt.Param> paramsIterator = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "root", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "whatToShow", null, 0, 0), null, null)
            );
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createNodeIterator", null, 0, 0),
                    paramsIterator,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createTreeWalker", null, 0, 0),
                    paramsIterator,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // Serialização
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getHTML", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getInnerHTML", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "toObject", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // Getters/Setters
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getTitle", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "setTitle", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getCookie", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "setCookie", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getDesignMode", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "setDesignMode", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getDir", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "setDir", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getLocation", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            XplDocument.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "setLocation", null, 0, 0),
                    paramsName,
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
            XplDocument.nativeModel.addMethod(new Stmt.Function(
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
        return XplDocument.nativeModel;
    }
    public static void invokeMethod( XplDocument document) {
        // ─── Criação de nós ──────────────────────────────────────────────
        document.fields.put("createElement", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String tag = intp.evaluate(args.getFirst().expression).toString();
                return document.createElement(tag);
            }
        });
        document.fields.put("createElementNS", new XplCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String ns = intp.evaluate(args.get(0).expression).toString();
                String qname = intp.evaluate(args.get(1).expression).toString();
                return document.createElementNS(ns, qname);
            }
        });
        document.fields.put("createTextNode", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String data = intp.evaluate(args.getFirst().expression).toString();
                return document.createTextNode(data);
            }
        });
        document.fields.put("createComment", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String data = intp.evaluate(args.getFirst().expression).toString();
                return document.createComment(data);
            }
        });
        document.fields.put("createDocumentFragment", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.createDocumentFragment();
            }
        });
        document.fields.put("createAttribute", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String name = intp.evaluate(args.getFirst().expression).toString();
                return document.createAttribute(name);
            }
        });
        document.fields.put("createAttributeNS", new XplCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String ns = intp.evaluate(args.get(0).expression).toString();
                String qname = intp.evaluate(args.get(1).expression).toString();
                return document.createAttributeNS(ns, qname);
            }
        });

        // ─── Selecção ────────────────────────────────────────────────────
        document.fields.put("getElementById", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String id = intp.evaluate(args.getFirst().expression).toString();
                return document.getElementById(id);
            }
        });
        document.fields.put("getElementsByTagName", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String tag = intp.evaluate(args.getFirst().expression).toString();
                return document.getElementsByTagName(tag);
            }
        });
        document.fields.put("getElementsByClassName", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String cls = intp.evaluate(args.getFirst().expression).toString();
                return document.getElementsByClassName(cls);
            }
        });
        document.fields.put("getElementsByName", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String name = intp.evaluate(args.getFirst().expression).toString();
                return document.getElementsByName(name);
            }
        });
        document.fields.put("querySelector", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String selector = intp.evaluate(args.getFirst().expression).toString();
                return document.querySelector(selector);
            }
        });
        document.fields.put("querySelectorAll", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String selector = intp.evaluate(args.getFirst().expression).toString();
                return document.querySelectorAll(selector);
            }
        });
        document.fields.put("getElementByIdNS", new XplCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String ns = intp.evaluate(args.get(0).expression).toString();
                String local = intp.evaluate(args.get(1).expression).toString();
                return document.getElementByIdNS(ns, local);
            }
        });
        document.fields.put("getElementsByTagNameNS", new XplCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String ns = intp.evaluate(args.get(0).expression).toString();
                String local = intp.evaluate(args.get(1).expression).toString();
                return document.getElementsByTagNameNS(ns, local);
            }
        });

        // ─── Navegação e coleções ────────────────────────────────────────
        document.fields.put("getRootNode", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getRootNode();
            }
        });
        document.fields.put("getActiveElement", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getActiveElement();
            }
        });
        document.fields.put("getAnchors", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getAnchors();
            }
        });
        document.fields.put("getApplets", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getApplets();
            }
        });
        document.fields.put("getEmbeds", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getEmbeds();
            }
        });
        document.fields.put("getForms", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getForms();
            }
        });
        document.fields.put("getImages", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getImages();
            }
        });
        document.fields.put("getLinks", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getLinks();
            }
        });
        document.fields.put("getScripts", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getScripts();
            }
        });
        document.fields.put("getStylesheets", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getStylesheets();
            }
        });

        // ─── Escrita ──────────────────────────────────────────────────────
        document.fields.put("write", new XplCallable() {
            @Override
            public int arity() {
                return -1;
            } // varargs

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String[] texts = args.stream()
                        .map(arg -> intp.evaluate(arg.expression).toString())
                        .toArray(String[]::new);
                document.write(texts);
                return null;
            }
        });
        document.fields.put("writeln", new XplCallable() {
            @Override
            public int arity() {
                return -1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String[] texts = args.stream()
                        .map(arg -> intp.evaluate(arg.expression).toString())
                        .toArray(String[]::new);
                document.writeln(texts);
                return null;
            }
        });

        // ─── Eventos ──────────────────────────────────────────────────────
        document.fields.put("addEventListener", new XplCallable() {
            @Override
            public int arity() {
                return -1;
            } // 2 ou 3

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                if (args.size() < 2 || args.size() > 3) {
                    throw new IllegalArgumentException("addEventListener espera 2 ou 3 argumentos.");
                }
                String type = intp.evaluate(args.get(0).expression).toString();
                Object listenerObj = intp.evaluate(args.get(1).expression);
                if (listenerObj instanceof XplEventListener) {
                    document.addEventListener(type, (XplEventListener) listenerObj);
                } else if (listenerObj instanceof XplFunction) {
                    document.addEventListener(type, (XplFunction) listenerObj, intp);
                } else {
                    throw new IllegalArgumentException("Ouvinte deve ser XplEventListener ou XplFunction.");
                }
                // terceiro argumento (options) ignorado
                return null;
            }
        });
        document.fields.put("removeEventListener", new XplCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String type = intp.evaluate(args.get(0).expression).toString();
                XplEventListener listener = (XplEventListener) intp.evaluate(args.get(1).expression);
                document.removeEventListener(type, listener);
                return null;
            }
        });
        document.fields.put("dispatchEvent", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplEvent event = (XplEvent) intp.evaluate(args.getFirst().expression);
                return document.dispatchEvent(event);
            }
        });

        // ─── Utilidades ──────────────────────────────────────────────────
        document.fields.put("importNode", new XplCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplElement node = (XplElement) intp.evaluate(args.get(0).expression);
                boolean deep = (boolean) intp.evaluate(args.get(1).expression);
                return document.importNode(node, deep);
            }
        });
        document.fields.put("adoptNode", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplElement node = (XplElement) intp.evaluate(args.getFirst().expression);
                return document.adoptNode(node);
            }
        });
        document.fields.put("createRange", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.createRange();
            }
        });
        document.fields.put("execCommand", new XplCallable() {
            @Override
            public int arity() {
                return 3;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String cmd = intp.evaluate(args.get(0).expression).toString();
                boolean ui = (boolean) intp.evaluate(args.get(1).expression);
                String value = intp.evaluate(args.get(2).expression).toString();
                document.execCommand(cmd, ui, value);
                return null;
            }
        });
        document.fields.put("queryCommandEnabled", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String cmd = intp.evaluate(args.getFirst().expression).toString();
                return document.queryCommandEnabled(cmd);
            }
        });
        document.fields.put("queryCommandSupported", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String cmd = intp.evaluate(args.getFirst().expression).toString();
                return document.queryCommandSupported(cmd);
            }
        });
        document.fields.put("queryCommandValue", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String cmd = intp.evaluate(args.getFirst().expression).toString();
                return document.queryCommandValue(cmd);
            }
        });
        document.fields.put("close", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                document.close();
                return null;
            }
        });
        document.fields.put("open", new XplCallable() {
            @Override
            public int arity() {
                return 3;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String url = intp.evaluate(args.get(0).expression).toString();
                String name = intp.evaluate(args.get(1).expression).toString();
                String features = intp.evaluate(args.get(2).expression).toString();
                document.open(url, name, features);
                return null;
            }
        });

        // ─── Serialização ─────────────────────────────────────────────────
        document.fields.put("getHTML", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getHTML();
            }
        });
        document.fields.put("getInnerHTML", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getInnerHTML();
            }
        });
        document.fields.put("getDOMObject", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getDOMObject();
            }
        });

        // ─── Getters e Setters específicos (para propriedades mutáveis) ─
        document.fields.put("getTitle", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getTitle();
            }
        });
        document.fields.put("setTitle", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String t = intp.evaluate(args.getFirst().expression).toString();
                document.setTitle(t);
                return null;
            }
        });
        document.fields.put("getCookie", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getCookie();
            }
        });
        document.fields.put("setCookie", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String c = intp.evaluate(args.getFirst().expression).toString();
                document.setCookie(c);
                return null;
            }
        });
        document.fields.put("getDesignMode", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getDesignMode();
            }
        });
        document.fields.put("setDesignMode", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String mode = intp.evaluate(args.getFirst().expression).toString();
                document.setDesignMode(mode);
                return null;
            }
        });
        document.fields.put("getDir", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getDir();
            }
        });
        document.fields.put("setDir", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String d = intp.evaluate(args.getFirst().expression).toString();
                document.setDir(d);
                return null;
            }
        });
        document.fields.put("getLocation", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getLocation();
            }
        });
        document.fields.put("setLocation", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String loc = intp.evaluate(args.getFirst().expression).toString();
                document.setLocation(loc);
                return null;
            }
        });

        // ─── getProperty como método (opcional) ──────────────────────────
        document.fields.put("getProperty", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String prop = intp.evaluate(args.getFirst().expression).toString();
                return document.getProperty(prop);
            }
        });

        // ─── NOVOS MÉTODOS ─────────────────────────────────────────────────────

// contains
        document.fields.put("contains", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplElement node = (XplElement) intp.evaluate(args.getFirst().expression);
                return document.contains(node);
            }
        });

// hasFocus
        document.fields.put("hasFocus", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.hasFocus();
            }
        });

// getSelection
        document.fields.put("getSelection", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getSelection();
            }
        });

// elementFromPoint
        document.fields.put("elementFromPoint", new XplCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                int x = (int) intp.evaluate(args.get(0).expression);
                int y = (int) intp.evaluate(args.get(1).expression);
                return document.elementFromPoint(x, y);
            }
        });

// elementsFromPoint
        document.fields.put("elementsFromPoint", new XplCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                int x = (int) intp.evaluate(args.get(0).expression);
                int y = (int) intp.evaluate(args.get(1).expression);
                return document.elementsFromPoint(x, y);
            }
        });

// createEvent
        document.fields.put("createEvent", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String type = intp.evaluate(args.getFirst().expression).toString();
                return document.createEvent(type);
            }
        });

// createNodeIterator
        document.fields.put("createNodeIterator", new XplCallable() {
            @Override
            public int arity() {
                return 2;
            } // simplificado, aceita 2 args (root, whatToShow)

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplElement root = (XplElement) intp.evaluate(args.get(0).expression);
                int whatToShow = (int) intp.evaluate(args.get(1).expression);
                return document.createNodeIterator(root, whatToShow, null);
            }
        });

// createTreeWalker
        document.fields.put("createTreeWalker", new XplCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplElement root = (XplElement) intp.evaluate(args.get(0).expression);
                int whatToShow = (int) intp.evaluate(args.get(1).expression);
                return document.createTreeWalker(root, whatToShow, null);
            }
        });

// ─── GETTERS PARA AS NOVAS PROPRIEDADES (como método) ──────────────
        document.fields.put("getChildren", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getChildren();
            }
        });
        document.fields.put("getFirstElementChild", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getFirstElementChild();
            }
        });
        document.fields.put("getLastElementChild", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getLastElementChild();
            }
        });
        document.fields.put("getDoctype", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getDoctype();
            }
        });
        document.fields.put("getDocumentURI", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getDocumentURI();
            }
        });
        document.fields.put("getOrigin", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getOrigin();
            }
        });
        document.fields.put("getBaseURI", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return document.getBaseURI();
            }
        });
    }
}
