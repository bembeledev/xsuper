package com.dic.xsuper.lang.ui.document;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.poo.XplInstance;
import com.dic.xsuper.lang.ui.SuperUiEngine;
import com.dic.xsuper.lang.ui.events.XplEvent;

import java.util.*;

/**
 * Classe que replica o objeto global 'document' do JavaScript.
 * Deve ser injetada no Environment do interpretador XPL como 'document'.
 */
public class XplDocument extends XplInstance implements XplNativeObject {

    // ──────────────────────── Propriedades principais ─────────────────────────

    public XplElement documentElement;
    public XplElement body;
    public XplElement head;
    public String title;
    public String URL;
    public String domain;
    public String referrer;
    public String cookie;
    public String characterSet;
    public String contentType;
    public String compatMode;
    public String designMode;
    public String dir;
    public String lastModified;
    public String location;
    public String readyState; // "loading", "interactive", "complete"
    public boolean hidden;
    public boolean visibilityState;
    public long childElementCount;

    private static XPLModel nativeModel;

    // ─── Índice rápido de elementos por ID ──────────────────────────────────────
    private final Map<String, XplElement> elementsById = new HashMap<>();

    // ─── Construtor ────────────────────────────────────────────────────────────
    private final Map<String, List<XplEventListener>> eventListeners = new HashMap<>();


    /**
     * Regista um elemento (e os seus filhos) no índice de IDs.
     * Deve ser chamado durante a hidratação da árvore DOM.
     *
     * @param element O elemento a registar.
     */
    public void registerElement(XplElement element) {
        if (element == null) return;

        // 1. Regista o próprio elemento, se tiver ID
        String id = element.getId();
        if (id != null && !id.isEmpty()) {
            elementsById.put(id, element);
        }

        // 2. Regista os filhos recursivamente
        for (XplElement child : element.getChildren()) {
            registerElement(child);
        }
    }

    /**
     * Remove um elemento do índice por ID.
     *
     * @param id O ID do elemento a remover.
     */
    public void removeElement(String id) {
        if (id != null && !id.isEmpty()) {
            elementsById.remove(id);
        }
    }

    /**
     * Limpa completamente o índice de elementos.
     * Deve ser chamado antes de um novo ciclo de renderização.
     */
    public void clearIndex() {
        elementsById.clear();
    }


    // ─── Métodos de criação de nós ────────────────────────────────────────────

    public XplElement createElement(String tagName) {
        return new XplElement(tagName);
    }

    public XplElement createElementNS(String namespaceURI, String qualifiedName) {
        return new XplElement(qualifiedName);
    }

    public XplElement createTextNode(String data) {
        return new XplElement("#text", data);
    }

    public XplElement createComment(String data) {
        return new XplElement("#comment", data);
    }

    public XplElement createDocumentFragment() {
        return new XplElement("#document-fragment");
    }

    public XplElement createAttribute(String name) {
        XplElement attr = new XplElement("@attribute");
        attr.setAttribute("name", name);
        return attr;
    }

    public XplElement createAttributeNS(String namespaceURI, String qualifiedName) {
        return createAttribute(qualifiedName);
    }

    // ─── Métodos de selecção ──────────────────────────────────────────────────

    public XplElement getElementById(String id) {
        // 1. Tenta o mapa rápido (O(1))
        if (id != null && elementsById.containsKey(id)) {
            return elementsById.get(id);
        }
        // 2. Fallback: pesquisa na árvore (O(n))
        if (documentElement != null) {
            assert id != null;
            return documentElement.getElementById(id);
        }
        return null;
    }

    public List<XplElement> getElementsByTagName(String tagName) {
        if (documentElement == null) return Collections.emptyList();
        return documentElement.getElementsByTagName(tagName);
    }

    public List<XplElement> getElementsByClassName(String className) {
        if (documentElement == null) return Collections.emptyList();
        return documentElement.getElementsByClassName(className);
    }

    public List<XplElement> getElementsByName(String name) {
        if (documentElement == null) return Collections.emptyList();
        return documentElement.getElementsByName(name);
    }

    public XplElement querySelector(String selector) {
        if (documentElement == null) return null;
        return documentElement.querySelector(selector);
    }

    public List<XplElement> querySelectorAll(String selector) {
        if (documentElement == null) return Collections.emptyList();
        return documentElement.querySelectorAll(selector);
    }

    // ─── Navegação e coleções ─────────────────────────────────────────────────

    public XplElement getRootNode() {
        return documentElement;
    }

    public XplElement getActiveElement() {
        return body != null ? body : documentElement;
    }

    public List<XplElement> getAnchors() {
        return querySelectorAll("a[name]");
    }

    public List<XplElement> getApplets() {
        return querySelectorAll("applet");
    }

    public List<XplElement> getEmbeds() {
        return querySelectorAll("embed");
    }

    public List<XplElement> getForms() {
        return querySelectorAll("form");
    }

    public List<XplElement> getImages() {
        return querySelectorAll("img");
    }

    public List<XplElement> getLinks() {
        return querySelectorAll("a[href]");
    }

    public List<XplElement> getScripts() {
        return querySelectorAll("script");
    }

    public List<XplElement> getStylesheets() {
        return querySelectorAll("link[rel=stylesheet], style");
    }

    // ─── Escrita directa (write/writeln) ──────────────────────────────────────

    public void write(String... text) {
        for (String s : text) {
            if (body != null) {
                XplElement textNode = createTextNode(s);
                body.appendChild(textNode);
            }
        }
    }

    public void writeln(String... text) {
        for (String s : text) {
            write(s + "\n");
        }
    }

    // ─── Gestão de eventos (com suporte a funções XPL) ──────────────────────

    public XplDocument(XplElement rootElement) {
        super(SuperUiEngine.DOCUMENT_CLASS);
        this.documentElement = rootElement;
        this.body = rootElement; // simplificação: assumimos que a raiz é <body>
        this.head = new XplElement("head");
        this.title = "";
        this.URL = "http://localhost/";
        this.domain = "localhost";
        this.referrer = "";
        this.cookie = "";
        this.characterSet = "UTF-8";
        this.contentType = "text/html";
        this.compatMode = "CSS1Compat";
        this.designMode = "off";
        this.dir = "ltr";
        this.lastModified = new Date().toString();
        this.location = this.URL;
        this.readyState = "complete";
        this.hidden = false;
        this.visibilityState = true;
        this.childElementCount = 1;

        invokeMethod();
    }

    public void addEventListener(String type, XplEventListener listener) {
        eventListeners.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
    }

    public void removeEventListener(String type, XplEventListener listener) {
        List<XplEventListener> list = eventListeners.get(type);
        if (list != null) list.remove(listener);
    }

    public boolean dispatchEvent(XplEvent event) {
        List<XplEventListener> list = eventListeners.get(event.type);
        if (list != null) {
            for (XplEventListener l : list) {
                l.handleEvent(event);
            }
        }
        return true;
    }

    /**
     * Adiciona um ouvinte de eventos usando uma função XPL.
     * A função receberá o objeto XplEvent como argumento.
     */
    public void addEventListener(String type, XplFunction function, Interpreter interpreter) {
        addEventListener(type, new XplElement.XplFunctionListener(function, interpreter));
    }

    // ─── Utilidades ────────────────────────────────────────────────────────────

    public XplElement importNode(XplElement node, boolean deep) {
        return node.cloneNode(deep);
    }

    public XplElement adoptNode(XplElement node) {
        if (node.parentNode != null) {
            node.parentNode.removeChild(node);
        }
        return node;
    }

    public XplElement createRange() {
        return new XplElement("#range");
    }

    public void execCommand(String commandId, boolean showUI, String value) {
        // Simulação – sem implementação real
    }

    public boolean queryCommandEnabled(String commandId) {
        return false;
    }

    public boolean queryCommandSupported(String commandId) {
        return false;
    }

    public String queryCommandValue(String commandId) {
        return "";
    }

    public void close() {
        // Não faz nada no contexto Java
    }

    public void open(String url, String name, String features) {
        // Simulação
    }

    public XplElement getElementByIdNS(String namespaceURI, String localName) {
        return getElementById(localName);
    }

    public List<XplElement> getElementsByTagNameNS(String namespaceURI, String localName) {
        return getElementsByTagName(localName);
    }

    // ─── Propriedades getter / setter (estilo JS) ────────────────────────────



    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getURL() { return URL; }
    public String getDomain() { return domain; }
    public String getReferrer() { return referrer; }
    public String getCookie() { return cookie; }
    public void setCookie(String cookie) { this.cookie = cookie; }
    public String getCharacterSet() { return characterSet; }
    public String getContentType() { return contentType; }
    public String getCompatMode() { return compatMode; }
    public String getDesignMode() { return designMode; }
    public void setDesignMode(String mode) { this.designMode = mode; }
    public String getDir() { return dir; }
    public void setDir(String dir) { this.dir = dir; }
    public String getLastModified() { return lastModified; }
    public String getLocation() { return location; }
    public void setLocation(String loc) { this.location = loc; this.URL = loc; }
    public String getReadyState() { return readyState; }
    public boolean isHidden() { return hidden; }
    public boolean getVisibilityState() { return visibilityState; }
    public long getChildElementCount() { return childElementCount; }

    // ─── Métodos de conversão / serialização ─────────────────────────────────

    public String getHTML() {
        if (documentElement == null) return "";
        return documentElement.getOuterHTML();
    }

    public String getInnerHTML() {
        if (documentElement == null) return "";
        return documentElement.getInnerHTML();
    }

    // ─── Métodos de inspecção (para a reflexão XPL) ───────────────────────────

    public static XPLModel buildNativeModel() {
        if (nativeModel == null) {
            nativeModel = new XPLModel("XplDocument", null);

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
                nativeModel.addField(field);
            }

            // ─── Métodos ──────────────────────────────────────────────────────────────

            // createElement(tagName)
            List<Stmt.Param> params1 = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "tagName", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createElement", null, 0, 0),
                    params1,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // createElementNS(namespaceURI, qualifiedName)
            List<Stmt.Param> params2 = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "namespaceURI", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "qualifiedName", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createElementNS", null, 0, 0),
                    params2,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // createTextNode(data)
            List<Stmt.Param> paramsData = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "data", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createTextNode", null, 0, 0),
                    paramsData,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // createComment(data)
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createComment", null, 0, 0),
                    paramsData,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // createDocumentFragment()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createDocumentFragment", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // createAttribute(name)
            List<Stmt.Param> paramsName = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "name", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createAttribute", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // createAttributeNS(namespaceURI, qualifiedName)
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createAttributeNS", null, 0, 0),
                    params2,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getElementById(id)
            List<Stmt.Param> paramsId = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "id", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getElementById", null, 0, 0),
                    paramsId,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getElementsByTagName(tagName)
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getElementsByTagName", null, 0, 0),
                    params1,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getElementsByClassName(className)
            List<Stmt.Param> paramsClass = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "className", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getElementsByClassName", null, 0, 0),
                    paramsClass,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getElementsByName(name)
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getElementsByName", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // querySelector(selector)
            List<Stmt.Param> paramsSelector = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "selector", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "querySelector", null, 0, 0),
                    paramsSelector,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // querySelectorAll(selector)
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "querySelectorAll", null, 0, 0),
                    paramsSelector,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getElementByIdNS(namespaceURI, localName)
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getElementByIdNS", null, 0, 0),
                    params2,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getElementsByTagNameNS(namespaceURI, localName)
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getElementsByTagNameNS", null, 0, 0),
                    params2,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // Navegação (sem parâmetros)
            String[] navMethods = {
                    "getRootNode", "getActiveElement", "getAnchors", "getApplets", "getEmbeds",
                    "getForms", "getImages", "getLinks", "getScripts", "getStylesheets"
            };
            for (String m : navMethods) {
                nativeModel.addMethod(new Stmt.Function(
                        null, false, false,
                        new Token(TokenType.IDENTIFIER, m, null, 0, 0),
                        Collections.emptyList(),
                        null,
                        null,
                        Collections.emptyList(),
                        Collections.emptyList()
                ));
            }

            // write, writeln
            List<Stmt.Param> paramsWrite = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "text", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "write", null, 0, 0),
                    paramsWrite,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "writeln", null, 0, 0),
                    paramsWrite,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // Eventos
            List<Stmt.Param> paramsEventListener = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "type", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "listener", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "addEventListener", null, 0, 0),
                    paramsEventListener,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "removeEventListener", null, 0, 0),
                    paramsEventListener,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            List<Stmt.Param> paramsEvent = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "event", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "dispatchEvent", null, 0, 0),
                    paramsEvent,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // Utilidades
            List<Stmt.Param> paramsImport = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "node", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "deep", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "importNode", null, 0, 0),
                    paramsImport,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            List<Stmt.Param> paramsNode = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "node", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "adoptNode", null, 0, 0),
                    paramsNode,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createRange", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            List<Stmt.Param> paramsExec = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "commandId", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "showUI", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "value", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "execCommand", null, 0, 0),
                    paramsExec,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            List<Stmt.Param> paramsCmd = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "commandId", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "queryCommandEnabled", null, 0, 0),
                    paramsCmd,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "queryCommandSupported", null, 0, 0),
                    paramsCmd,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "queryCommandValue", null, 0, 0),
                    paramsCmd,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "close", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            List<Stmt.Param> paramsOpen = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "url", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "name", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "features", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "open", null, 0, 0),
                    paramsOpen,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // Novos métodos
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "contains", null, 0, 0),
                    paramsNode,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "hasFocus", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getSelection", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            List<Stmt.Param> paramsPoint = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "x", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "y", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "elementFromPoint", null, 0, 0),
                    paramsPoint,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "elementsFromPoint", null, 0, 0),
                    paramsPoint,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createEvent", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            List<Stmt.Param> paramsIterator = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "root", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "whatToShow", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createNodeIterator", null, 0, 0),
                    paramsIterator,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "createTreeWalker", null, 0, 0),
                    paramsIterator,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // Serialização
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getHTML", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getInnerHTML", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "toObject", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // Getters/Setters
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getTitle", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "setTitle", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getCookie", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "setCookie", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getDesignMode", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "setDesignMode", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getDir", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "setDir", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getLocation", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "setLocation", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getProperty(propName)
            List<Stmt.Param> paramsProp = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "propName", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getProperty", null, 0, 0),
                    paramsProp,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
        }
        return nativeModel;
    }

    @Override
    public String toString() {
        return "[Document " + URL + "]";
    }

    public Map<String, Object> getDOMObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", title);
        map.put("URL", URL);
        map.put("domain", domain);
        map.put("referrer", referrer);
        map.put("cookie", cookie);
        map.put("characterSet", characterSet);
        map.put("contentType", contentType);
        map.put("compatMode", compatMode);
        map.put("designMode", designMode);
        map.put("dir", dir);
        map.put("lastModified", lastModified);
        map.put("location", location);
        map.put("readyState", readyState);
        map.put("hidden", hidden);
        map.put("visibilityState", visibilityState);
        map.put("childElementCount", childElementCount);

        // Novas propriedades (coerentes com getProperty)
        map.put("children", getChildren());
        map.put("firstElementChild", getFirstElementChild());
        map.put("lastElementChild", getLastElementChild());
        map.put("doctype", getDoctype());
        map.put("documentURI", getDocumentURI());
        map.put("origin", getOrigin());
        map.put("baseURI", getBaseURI());

        // Elementos estruturais (já existentes)
        map.put("documentElement", documentElement != null ? documentElement.getDOMObject() : null);
        map.put("body", body != null ? body.getDOMObject() : null);
        map.put("head", head != null ? head.getDOMObject() : null);

        return map;
    }

    // ─── Novos métodos ────────────────────────────────────────────────────────────

    @Override
    public void invokeMethod() {
        // ─── Criação de nós ──────────────────────────────────────────────
        this.fields.put("createElement", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String tag = intp.evaluate(args.getFirst().expression).toString();
                return createElement(tag);
            }
        });
        this.fields.put("createElementNS", new XplCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String ns = intp.evaluate(args.get(0).expression).toString();
                String qname = intp.evaluate(args.get(1).expression).toString();
                return createElementNS(ns, qname);
            }
        });
        this.fields.put("createTextNode", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String data = intp.evaluate(args.getFirst().expression).toString();
                return createTextNode(data);
            }
        });
        this.fields.put("createComment", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String data = intp.evaluate(args.getFirst().expression).toString();
                return createComment(data);
            }
        });
        this.fields.put("createDocumentFragment", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return createDocumentFragment();
            }
        });
        this.fields.put("createAttribute", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String name = intp.evaluate(args.getFirst().expression).toString();
                return createAttribute(name);
            }
        });
        this.fields.put("createAttributeNS", new XplCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String ns = intp.evaluate(args.get(0).expression).toString();
                String qname = intp.evaluate(args.get(1).expression).toString();
                return createAttributeNS(ns, qname);
            }
        });

        // ─── Selecção ────────────────────────────────────────────────────
        this.fields.put("getElementById", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String id = intp.evaluate(args.getFirst().expression).toString();
                return getElementById(id);
            }
        });
        this.fields.put("getElementsByTagName", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String tag = intp.evaluate(args.getFirst().expression).toString();
                return getElementsByTagName(tag);
            }
        });
        this.fields.put("getElementsByClassName", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String cls = intp.evaluate(args.getFirst().expression).toString();
                return getElementsByClassName(cls);
            }
        });
        this.fields.put("getElementsByName", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String name = intp.evaluate(args.getFirst().expression).toString();
                return getElementsByName(name);
            }
        });
        this.fields.put("querySelector", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String selector = intp.evaluate(args.getFirst().expression).toString();
                return querySelector(selector);
            }
        });
        this.fields.put("querySelectorAll", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String selector = intp.evaluate(args.getFirst().expression).toString();
                return querySelectorAll(selector);
            }
        });
        this.fields.put("getElementByIdNS", new XplCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String ns = intp.evaluate(args.get(0).expression).toString();
                String local = intp.evaluate(args.get(1).expression).toString();
                return getElementByIdNS(ns, local);
            }
        });
        this.fields.put("getElementsByTagNameNS", new XplCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String ns = intp.evaluate(args.get(0).expression).toString();
                String local = intp.evaluate(args.get(1).expression).toString();
                return getElementsByTagNameNS(ns, local);
            }
        });

        // ─── Navegação e coleções ────────────────────────────────────────
        this.fields.put("getRootNode", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getRootNode();
            }
        });
        this.fields.put("getActiveElement", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getActiveElement();
            }
        });
        this.fields.put("getAnchors", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getAnchors();
            }
        });
        this.fields.put("getApplets", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getApplets();
            }
        });
        this.fields.put("getEmbeds", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getEmbeds();
            }
        });
        this.fields.put("getForms", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getForms();
            }
        });
        this.fields.put("getImages", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getImages();
            }
        });
        this.fields.put("getLinks", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getLinks();
            }
        });
        this.fields.put("getScripts", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getScripts();
            }
        });
        this.fields.put("getStylesheets", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getStylesheets();
            }
        });

        // ─── Escrita ──────────────────────────────────────────────────────
        this.fields.put("write", new XplCallable() {
            @Override
            public int arity() {
                return -1;
            } // varargs

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String[] texts = args.stream()
                        .map(arg -> intp.evaluate(arg.expression).toString())
                        .toArray(String[]::new);
                write(texts);
                return null;
            }
        });
        this.fields.put("writeln", new XplCallable() {
            @Override
            public int arity() {
                return -1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String[] texts = args.stream()
                        .map(arg -> intp.evaluate(arg.expression).toString())
                        .toArray(String[]::new);
                writeln(texts);
                return null;
            }
        });

        // ─── Eventos ──────────────────────────────────────────────────────
        this.fields.put("addEventListener", new XplCallable() {
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
                    addEventListener(type, (XplEventListener) listenerObj);
                } else if (listenerObj instanceof XplFunction) {
                    addEventListener(type, (XplFunction) listenerObj, intp);
                } else {
                    throw new IllegalArgumentException("Ouvinte deve ser XplEventListener ou XplFunction.");
                }
                // terceiro argumento (options) ignorado
                return null;
            }
        });
        this.fields.put("removeEventListener", new XplCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String type = intp.evaluate(args.get(0).expression).toString();
                XplEventListener listener = (XplEventListener) intp.evaluate(args.get(1).expression);
                removeEventListener(type, listener);
                return null;
            }
        });
        this.fields.put("dispatchEvent", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplEvent event = (XplEvent) intp.evaluate(args.getFirst().expression);
                return dispatchEvent(event);
            }
        });

        // ─── Utilidades ──────────────────────────────────────────────────
        this.fields.put("importNode", new XplCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplElement node = (XplElement) intp.evaluate(args.get(0).expression);
                boolean deep = (boolean) intp.evaluate(args.get(1).expression);
                return importNode(node, deep);
            }
        });
        this.fields.put("adoptNode", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplElement node = (XplElement) intp.evaluate(args.getFirst().expression);
                return adoptNode(node);
            }
        });
        this.fields.put("createRange", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return createRange();
            }
        });
        this.fields.put("execCommand", new XplCallable() {
            @Override
            public int arity() {
                return 3;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String cmd = intp.evaluate(args.get(0).expression).toString();
                boolean ui = (boolean) intp.evaluate(args.get(1).expression);
                String value = intp.evaluate(args.get(2).expression).toString();
                execCommand(cmd, ui, value);
                return null;
            }
        });
        this.fields.put("queryCommandEnabled", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String cmd = intp.evaluate(args.getFirst().expression).toString();
                return queryCommandEnabled(cmd);
            }
        });
        this.fields.put("queryCommandSupported", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String cmd = intp.evaluate(args.getFirst().expression).toString();
                return queryCommandSupported(cmd);
            }
        });
        this.fields.put("queryCommandValue", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String cmd = intp.evaluate(args.getFirst().expression).toString();
                return queryCommandValue(cmd);
            }
        });
        this.fields.put("close", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                close();
                return null;
            }
        });
        this.fields.put("open", new XplCallable() {
            @Override
            public int arity() {
                return 3;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String url = intp.evaluate(args.get(0).expression).toString();
                String name = intp.evaluate(args.get(1).expression).toString();
                String features = intp.evaluate(args.get(2).expression).toString();
                open(url, name, features);
                return null;
            }
        });

        // ─── Serialização ─────────────────────────────────────────────────
        this.fields.put("getHTML", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getHTML();
            }
        });
        this.fields.put("getInnerHTML", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getInnerHTML();
            }
        });
        this.fields.put("getDOMObject", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getDOMObject();
            }
        });

        // ─── Getters e Setters específicos (para propriedades mutáveis) ─
        this.fields.put("getTitle", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getTitle();
            }
        });
        this.fields.put("setTitle", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String t = intp.evaluate(args.getFirst().expression).toString();
                setTitle(t);
                return null;
            }
        });
        this.fields.put("getCookie", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getCookie();
            }
        });
        this.fields.put("setCookie", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String c = intp.evaluate(args.getFirst().expression).toString();
                setCookie(c);
                return null;
            }
        });
        this.fields.put("getDesignMode", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getDesignMode();
            }
        });
        this.fields.put("setDesignMode", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String mode = intp.evaluate(args.getFirst().expression).toString();
                setDesignMode(mode);
                return null;
            }
        });
        this.fields.put("getDir", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getDir();
            }
        });
        this.fields.put("setDir", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String d = intp.evaluate(args.getFirst().expression).toString();
                setDir(d);
                return null;
            }
        });
        this.fields.put("getLocation", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getLocation();
            }
        });
        this.fields.put("setLocation", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String loc = intp.evaluate(args.getFirst().expression).toString();
                setLocation(loc);
                return null;
            }
        });

        // ─── getProperty como método (opcional) ──────────────────────────
        this.fields.put("getProperty", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String prop = intp.evaluate(args.getFirst().expression).toString();
                return getProperty(prop);
            }
        });

        // ─── NOVOS MÉTODOS ─────────────────────────────────────────────────────

// contains
        this.fields.put("contains", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplElement node = (XplElement) intp.evaluate(args.getFirst().expression);
                return contains(node);
            }
        });

// hasFocus
        this.fields.put("hasFocus", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return hasFocus();
            }
        });

// getSelection
        this.fields.put("getSelection", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getSelection();
            }
        });

// elementFromPoint
        this.fields.put("elementFromPoint", new XplCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                int x = (int) intp.evaluate(args.get(0).expression);
                int y = (int) intp.evaluate(args.get(1).expression);
                return elementFromPoint(x, y);
            }
        });

// elementsFromPoint
        this.fields.put("elementsFromPoint", new XplCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                int x = (int) intp.evaluate(args.get(0).expression);
                int y = (int) intp.evaluate(args.get(1).expression);
                return elementsFromPoint(x, y);
            }
        });

// createEvent
        this.fields.put("createEvent", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String type = intp.evaluate(args.getFirst().expression).toString();
                return createEvent(type);
            }
        });

// createNodeIterator
        this.fields.put("createNodeIterator", new XplCallable() {
            @Override
            public int arity() {
                return 2;
            } // simplificado, aceita 2 args (root, whatToShow)

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplElement root = (XplElement) intp.evaluate(args.get(0).expression);
                int whatToShow = (int) intp.evaluate(args.get(1).expression);
                return createNodeIterator(root, whatToShow, null);
            }
        });

// createTreeWalker
        this.fields.put("createTreeWalker", new XplCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplElement root = (XplElement) intp.evaluate(args.get(0).expression);
                int whatToShow = (int) intp.evaluate(args.get(1).expression);
                return createTreeWalker(root, whatToShow, null);
            }
        });

// ─── GETTERS PARA AS NOVAS PROPRIEDADES (como método) ──────────────
        this.fields.put("getChildren", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getChildren();
            }
        });
        this.fields.put("getFirstElementChild", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getFirstElementChild();
            }
        });
        this.fields.put("getLastElementChild", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getLastElementChild();
            }
        });
        this.fields.put("getDoctype", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getDoctype();
            }
        });
        this.fields.put("getDocumentURI", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getDocumentURI();
            }
        });
        this.fields.put("getOrigin", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getOrigin();
            }
        });
        this.fields.put("getBaseURI", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getBaseURI();
            }
        });
    }

    /**
     * Verifica se o documento contém o nó especificado.
     * Um documento contém todos os seus descendentes.
     */
    public boolean contains(XplElement node) {
        if (node == null) return false;
        // Verifica se o nó é descendente do documentElement
        XplElement current = node;
        while (current != null) {
            if (current == this.documentElement) return true;
            current = current.parentNode;
        }
        return false;
    }

    /**
     * Retorna verdadeiro se o documento (ou algum elemento dentro dele) tiver foco.
     */
    public boolean hasFocus() {
        // Simulação: por padrão, assumimos que o body tem foco se existir
        return body != null;
    }

    /**
     * Retorna a seleção actual (simplificada).
     */
    public Map<String, Object> getSelection() {
        Map<String, Object> sel = new LinkedHashMap<>();
        sel.put("type", "None");
        sel.put("anchorNode", null);
        sel.put("focusNode", null);
        sel.put("anchorOffset", 0);
        sel.put("focusOffset", 0);
        sel.put("isCollapsed", true);
        sel.put("rangeCount", 0);
        return sel;
    }

    /**
     * Retorna o elemento na posição (x, y) relativamente à viewport.
     */
    public XplElement elementFromPoint(int x, int y) {
        // Simulação: retorna o body se estiver dentro, senão null
        // Aqui poderíamos fazer uma busca por coordenadas se tivéssemos layout
        return body;
    }

    /**
     * Retorna todos os elementos na posição (x, y).
     */
    public List<XplElement> elementsFromPoint(int x, int y) {
        List<XplElement> list = new ArrayList<>();
        if (body != null) list.add(body);
        return list;
    }

    /**
     * Cria um evento genérico (simplificado).
     */
    public XplEvent createEvent(String eventType) {
        return new XplEvent(eventType, null);
    }

    /**
     * Cria um NodeIterator (simplificado).
     */
    public Object createNodeIterator(XplElement root, int whatToShow, Object filter) {
        // Retorna um objeto simples com métodos para navegação
        Map<String, Object> iterator = new LinkedHashMap<>();
        iterator.put("root", root);
        iterator.put("whatToShow", whatToShow);
        iterator.put("filter", filter);
        iterator.put("nextNode", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                // Simulação: retorna o primeiro filho do root
                return root.getChildren().isEmpty() ? null : root.getChildren().getFirst();
            }
        });
        iterator.put("previousNode", new XplCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return null;
            }
        });
        return iterator;
    }

// ─── Getters para as novas propriedades ─────────────────────────────

    /**
     * Cria um TreeWalker (simplificado).
     */
    public Object createTreeWalker(XplElement root, int whatToShow, Object filter) {
        return createNodeIterator(root, whatToShow, filter);
    }

    public XplElement getFirstElementChild() {
        if (documentElement == null) return null;
        return documentElement.getChildren().isEmpty() ? null : documentElement.getChildren().get(0);
    }

    public XplElement getLastElementChild() {
        if (documentElement == null) return null;
        List<XplElement> children = documentElement.getChildren();
        return children.isEmpty() ? null : children.get(children.size() - 1);
    }

    public List<XplElement> getChildren() {
        if (documentElement == null) return Collections.emptyList();
        return documentElement.getChildren();
    }

    public String getDoctype() {
        return "<!DOCTYPE html>"; // simulação
    }

    public String getDocumentURI() {
        return URL;
    }

    public String getOrigin() {
        // Extrai origem da URL
        try {
            java.net.URL url = new java.net.URL(URL);
            return url.getProtocol() + "://" + url.getHost() + (url.getPort() == -1 ? "" : ":" + url.getPort());
        } catch (Exception e) {
            return "null";
        }
    }

    public String getBaseURI() {
        return URL;
    }

    @Override
    public Object getProperty(String propertyName) {
        return switch (propertyName) {
            case "documentElement" -> documentElement;
            case "body" -> body;
            case "head" -> head;
            case "title" -> title;
            case "URL" -> URL;
            case "domain" -> domain;
            case "referrer" -> referrer;
            case "cookie" -> cookie;
            case "characterSet" -> characterSet;
            case "contentType" -> contentType;
            case "compatMode" -> compatMode;
            case "designMode" -> designMode;
            case "dir" -> dir;
            case "lastModified" -> lastModified;
            case "location" -> location;
            case "readyState" -> readyState;
            case "hidden" -> hidden;
            case "visibilityState" -> visibilityState;
            case "childElementCount" -> childElementCount;
            // NOVAS PROPRIEDADES
            case "children" -> getChildren();
            case "firstElementChild" -> getFirstElementChild();
            case "lastElementChild" -> getLastElementChild();
            case "doctype" -> getDoctype();
            case "documentURI" -> getDocumentURI();
            case "origin" -> getOrigin();
            case "baseURI" -> getBaseURI();
            default -> null;
        };
    }
}