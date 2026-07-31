package com.dic.xsuper.dom.node;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.execution.XplFunction;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplInstance;
import com.dic.xsuper.render.javafx.core.SuperUiEngine;
import com.dic.xsuper.dom.node.helpers.XplDocumentUtils;
import com.dic.xsuper.dom.event.XplEvent;
import com.dic.xsuper.dom.event.XplEventListener;

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
    public static XPLModel nativeModel;

    // ─── Índice rápido de elementos por ID ──────────────────────────────────────
    private final Map<String, XplElement> elementsById = new HashMap<>();

    // ─── Construtor ────────────────────────────────────────────────────────────
    private final Map<String, List<XplEventListener>> eventListeners = new HashMap<>();

    // 1. O índice invencível da Engine
    private final Map<String, XplElement> elementsByInternalUid = new HashMap<>();

    /**
     * Regista um elemento (e os seus filhos) no índice de IDs.
     * Deve ser chamado durante a hidratação da árvore DOM.
     *
     * @param element O elemento a registar.
     */
    public void registerElement(XplElement element) {
        if (element == null) return;

        // Registo para uso da Engine (Pub/Sub)
        elementsByInternalUid.put(element._internalUid, element);

        // Registo para uso do programador XPL (se o elemento tiver a propriedade "id" no HTML)
        String publicId = element.getId();
        if (publicId != null && !publicId.isEmpty()) {
            elementsById.put(publicId, element);
        }

        for (XplElement child : element.getChildren()) {
            registerElement(child);
        }
    }

    // Método vital para o UiEventHandlers usar:
    public XplElement getElementByInternalUid(String uid) {
        return elementsByInternalUid.get(uid);
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
        if (nativeModel == null) return XplDocumentUtils.buildNativeModel();
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
        //Injectar métodos do documentos
        XplDocumentUtils.invokeMethod(this);
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
        return new XplEvent(eventType);
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
        return documentElement.getChildren().isEmpty() ? null : documentElement.getChildren().getFirst();
    }

    public XplElement getLastElementChild() {
        if (documentElement == null) return null;
        List<XplElement> children = documentElement.getChildren();
        return children.isEmpty() ? null : children.getLast();
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