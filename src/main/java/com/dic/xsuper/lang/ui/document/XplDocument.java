package com.dic.xsuper.lang.ui.document;

import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplFunction;

import java.util.*;

/**
 * Classe que replica o objeto global 'document' do JavaScript.
 * Deve ser injetada no Environment do interpretador XPL como 'document'.
 */
public class XplDocument {

    // ─── Propriedades principais ──────────────────────────────────────────────

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

    // ─── Índice rápido de elementos por ID ──────────────────────────────────────
    private final Map<String, XplElement> elementsById = new HashMap<>();

    // ─── Construtor ────────────────────────────────────────────────────────────

    public XplDocument(XplElement rootElement) {
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
    }


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

    private Map<String, List<XplEventListener>> eventListeners = new HashMap<>();

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

    public Map<String, Object> toObject() {
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
        map.put("documentElement", documentElement != null ? documentElement.toObject() : null);
        map.put("body", body != null ? body.toObject() : null);
        map.put("head", head != null ? head.toObject() : null);
        return map;
    }

    @Override
    public String toString() {
        return "[Document " + URL + "]";
    }

}