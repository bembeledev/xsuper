package com.dic.xsuper.lang.ui.document;

import com.dic.xsuper.lang.ControlFlow;
import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;
import com.dic.xsuper.lang.XplFunction;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Classe que replica o comportamento de um HTMLElement no JavaScript.
 * Deve ser tratada como um objeto XPL nativo.
 */
public class XplElement {

    // ─── Propriedades estruturais (árvore) ────────────────────────────────────

    public String tagName;
    public XplElement parentNode;
    public XplElement parentElement;      // igual a parentNode, mas mais específico
    public List<XplElement> children = new ArrayList<>();
    public Map<String, Object> attributes = new HashMap<>();
    public String textContent = "";

    // ─── Propriedades de navegação (ponteiros) ──────────────────────────────

    public XplElement nextSibling;
    public XplElement previousSibling;
    public XplElement firstChild;
    public XplElement lastChild;

    // ─── Construtores ──────────────────────────────────────────────────────────

    public XplElement(String tagName) {
        this.tagName = tagName.toLowerCase();
    }

    /**
     * Construtor para nós especiais (texto, comentário).
     * Usado por document.createTextNode() e document.createComment()
     */
    public XplElement(String tagName, String data) {
        this.tagName = tagName.toLowerCase();
        this.textContent = data;
    }

    // ─── Getters / Setters (substituem os campos estáticos) ──────────────────

    // ID
    public String getId() {
        return (String) attributes.getOrDefault("id", "");
    }
    public void setId(String id) {
        setAttribute("id", id);
    }

    // Class (name e list)
    public String getClassName() {
        return (String) attributes.getOrDefault("class", "");
    }
    public void setClassName(String className) {
        setAttribute("class", className);
    }
    public List<String> getClassList() {
        String cls = getClassName();
        return cls.isEmpty() ? Collections.emptyList() : Arrays.asList(cls.split(" "));
    }
    public void setClassList(List<String> classes) {
        setClassName(String.join(" ", classes));
    }

    // Conteúdo HTML
    public String getInnerHTML() {
        StringBuilder sb = new StringBuilder();
        for (XplElement child : children) {
            sb.append(child.getOuterHTML());
        }
        return sb.toString();
    }
    public void setInnerHTML(String html) {
        // ⚠️ SIMPLIFICADO: limpa e cria um nó de texto com o HTML bruto
        // Numa implementação real, farias parsing do HTML para criar uma árvore de filhos.
        this.children.clear();
        this.textContent = "";
        // Opcional: adicionar como texto puro se não houver parser
        if (html != null && !html.isEmpty()) {
            XplElement textNode = new XplElement("#text", html);
            appendChild(textNode);
        }
        updateSiblings();
    }

    // Outer HTML (serialização)
    public String getOuterHTML() {
        if ("#text".equals(tagName) || "#comment".equals(tagName)) {
            return textContent;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(tagName);
        for (Map.Entry<String, Object> attr : attributes.entrySet()) {
            sb.append(" ").append(attr.getKey()).append("=\"").append(attr.getValue()).append("\"");
        }
        sb.append(">");
        sb.append(getInnerHTML());
        sb.append("</").append(tagName).append(">");
        return sb.toString();
    }

    // Texto
    public String getTextContent() {
        if ("#text".equals(tagName) || "#comment".equals(tagName)) {
            return textContent;
        }
        // Concatena o texto de todos os filhos (recursivo simplificado)
        StringBuilder sb = new StringBuilder();
        for (XplElement child : children) {
            sb.append(child.getTextContent());
        }
        return sb.toString();
    }
    public void setTextContent(String text) {
        this.textContent = text;
        this.children.clear();
        updateSiblings();
    }

    public String getInnerText() {
        return getTextContent();
    }
    public String getOuterText() {
        return getTextContent();
    }

    // Dimensões (simuladas)
    public int getClientWidth() { return 0; }
    public int getClientHeight() { return 0; }
    public int getOffsetWidth() { return 0; }
    public int getOffsetHeight() { return 0; }
    public int getScrollWidth() { return 0; }
    public int getScrollHeight() { return 0; }
    public int getScrollTop() { return 0; }
    public void setScrollTop(int value) {}
    public int getScrollLeft() { return 0; }
    public void setScrollLeft(int value) {}

    public Map<String, Object> getDataset() {
        Map<String, Object> dataset = new HashMap<>();
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            if (entry.getKey().startsWith("data-")) {
                dataset.put(entry.getKey().substring(5), entry.getValue());
            }
        }
        return dataset;
    }

    // ─── Métodos de manipulação de atributos ──────────────────────────────────

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public boolean hasAttribute(String name) {
        return attributes.containsKey(name);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    // ─── Métodos de manipulação da árvore ─────────────────────────────────────

    public void appendChild(XplElement child) {
        if (child == null) return;
        if (child.parentNode != null) {
            child.parentNode.removeChild(child);
        }
        child.parentNode = this;
        child.parentElement = this;
        this.children.add(child);
        updateSiblings();
    }

    public void insertBefore(XplElement newNode, XplElement referenceNode) {
        if (referenceNode == null) {
            appendChild(newNode);
            return;
        }
        int index = this.children.indexOf(referenceNode);
        if (index == -1) return;
        if (newNode.parentNode != null) {
            newNode.parentNode.removeChild(newNode);
        }
        newNode.parentNode = this;
        newNode.parentElement = this;
        this.children.add(index, newNode);
        updateSiblings();
    }

    public void replaceChild(XplElement newNode, XplElement oldNode) {
        int index = this.children.indexOf(oldNode);
        if (index == -1) return;
        if (newNode.parentNode != null) {
            newNode.parentNode.removeChild(newNode);
        }
        newNode.parentNode = this;
        newNode.parentElement = this;
        this.children.set(index, newNode);
        oldNode.parentNode = null;
        oldNode.parentElement = null;
        updateSiblings();
    }

    public void removeChild(XplElement child) {
        if (this.children.remove(child)) {
            child.parentNode = null;
            child.parentElement = null;
            updateSiblings();
        }
    }

    public void remove() {
        if (parentNode != null) {
            parentNode.removeChild(this);
        }
        this.parentNode = null;
        this.parentElement = null;
    }

    public XplElement cloneNode(boolean deep) {
        XplElement clone = new XplElement(this.tagName);
        clone.attributes = new HashMap<>(this.attributes);
        clone.textContent = this.textContent;
        if (deep) {
            for (XplElement child : this.children) {
                clone.appendChild(child.cloneNode(true));
            }
        }
        return clone;
    }

    // ─── Atualização dos ponteiros de navegação ──────────────────────────────

    private void updateSiblings() {
        if (children.isEmpty()) {
            firstChild = null;
            lastChild = null;
            return;
        }
        firstChild = children.get(0);
        lastChild = children.get(children.size() - 1);
        for (int i = 0; i < children.size(); i++) {
            XplElement child = children.get(i);
            child.parentNode = this;
            child.parentElement = this;
            child.nextSibling = (i + 1 < children.size()) ? children.get(i + 1) : null;
            child.previousSibling = (i > 0) ? children.get(i - 1) : null;
        }
    }

    // ─── Navegação e propriedades de posição ──────────────────────────────────

    public List<XplElement> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public XplElement getParent() {
        return parentNode;
    }

    public List<XplElement> getAncestors() {
        List<XplElement> ancestors = new ArrayList<>();
        XplElement current = parentNode;
        while (current != null) {
            ancestors.add(current);
            current = current.parentNode;
        }
        return ancestors;
    }

    public List<XplElement> getSiblings() {
        if (parentNode == null) return Collections.emptyList();
        List<XplElement> siblings = new ArrayList<>(parentNode.children);
        siblings.remove(this);
        return siblings;
    }

    // ─── Métodos de pesquisa (DOM Core) ──────────────────────────────────────

    public XplElement getElementById(String idToFind) {
        if (idToFind.equals(getId())) return this;
        for (XplElement child : children) {
            XplElement found = child.getElementById(idToFind);
            if (found != null) return found;
        }
        return null;
    }

    public List<XplElement> getElementsByClassName(String className) {
        List<XplElement> results = new ArrayList<>();
        if (getClassName().contains(className)) results.add(this);
        for (XplElement child : children) {
            results.addAll(child.getElementsByClassName(className));
        }
        return results;
    }

    public List<XplElement> getElementsByTagName(String tagName) {
        List<XplElement> results = new ArrayList<>();
        if (this.tagName.equalsIgnoreCase(tagName)) results.add(this);
        for (XplElement child : children) {
            results.addAll(child.getElementsByTagName(tagName));
        }
        return results;
    }

    public List<XplElement> getElementsByName(String name) {
        List<XplElement> results = new ArrayList<>();
        Object attr = attributes.get("name");
        if (attr != null && attr.toString().equals(name)) results.add(this);
        for (XplElement child : children) {
            results.addAll(child.getElementsByName(name));
        }
        return results;
    }

    public XplElement querySelector(String selector) {
        // Seletores básicos: #id, .class, tagname
        if (selector.startsWith("#")) {
            return getElementById(selector.substring(1));
        } else if (selector.startsWith(".")) {
            List<XplElement> list = getElementsByClassName(selector.substring(1));
            return list.isEmpty() ? null : list.get(0);
        } else {
            List<XplElement> list = getElementsByTagName(selector);
            return list.isEmpty() ? null : list.get(0);
        }
    }

    public List<XplElement> querySelectorAll(String selector) {
        if (selector.startsWith("#")) {
            XplElement el = getElementById(selector.substring(1));
            return el != null ? List.of(el) : Collections.emptyList();
        } else if (selector.startsWith(".")) {
            return getElementsByClassName(selector.substring(1));
        } else {
            return getElementsByTagName(selector);
        }
    }

    // ─── Métodos de classe CSS ──────────────────────────────────────────────────

    public void addClass(String className) {
        Set<String> classes = new HashSet<>(getClassList());
        classes.add(className);
        setClassName(String.join(" ", classes));
    }

    public void removeClass(String className) {
        Set<String> classes = new HashSet<>(getClassList());
        classes.remove(className);
        setClassName(String.join(" ", classes));
    }

    public boolean hasClass(String className) {
        return getClassList().contains(className);
    }

    public void toggleClass(String className) {
        if (hasClass(className)) {
            removeClass(className);
        } else {
            addClass(className);
        }
    }

    // ─── Estilos (inline) ─────────────────────────────────────────────────────

    public void setStyle(String property, String value) {
        String currentStyle = (String) attributes.getOrDefault("style", "");
        Map<String, String> styles = parseStyle(currentStyle);
        styles.put(property, value);
        attributes.put("style", serializeStyle(styles));
    }

    public String getStyle(String property) {
        String currentStyle = (String) attributes.getOrDefault("style", "");
        Map<String, String> styles = parseStyle(currentStyle);
        return styles.getOrDefault(property, "");
    }

    private Map<String, String> parseStyle(String styleStr) {
        Map<String, String> map = new HashMap<>();
        if (styleStr == null || styleStr.isEmpty()) return map;
        for (String part : styleStr.split(";")) {
            String[] kv = part.trim().split(":", 2);
            if (kv.length == 2) map.put(kv[0].trim(), kv[1].trim());
        }
        return map;
    }

    private String serializeStyle(Map<String, String> styles) {
        return styles.entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(";"));
    }

    // ─── Eventos (nativos Java) ──────────────────────────────────────────────

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

    // ─── Ponte para funções XPL como ouvintes ────────────────────────────────

    /**
     * Adaptador que converte uma função XPL (XplFunction) num XplEventListener.
     */
    public static class XplFunctionListener implements XplEventListener {
        private final XplFunction function;
        private final Interpreter interpreter;

        public XplFunctionListener(XplFunction function, Interpreter interpreter) {
            this.function = function;
            this.interpreter = interpreter;
        }

        @Override
        public void handleEvent(XplEvent event) {
            try {
                // Cria um CallArg com o evento
                List<Expr.CallArg> args = new ArrayList<>();
                args.add(new Expr.CallArg(null, new Expr.Literal(event)));
                function.call(interpreter, args);
            } catch (Exception e) {
                // Silencia ou loga
                System.err.println("[XPL Event] Erro ao executar listener: " + e.getMessage());
            }
        }
    }

    public void addEventListener(String type, XplFunction function, Interpreter interpreter) {
        addEventListener(type, new XplFunctionListener(function, interpreter));
    }

    // ─── Métodos utilitários ──────────────────────────────────────────────────

    public XplElement getClosest(String selector) {
        XplElement current = this;
        while (current != null) {
            if (current.matches(selector)) return current;
            current = current.parentNode;
        }
        return null;
    }

    public boolean matches(String selector) {
        if (selector.startsWith("#")) {
            return getId().equals(selector.substring(1));
        } else if (selector.startsWith(".")) {
            return hasClass(selector.substring(1));
        } else {
            return this.tagName.equalsIgnoreCase(selector);
        }
    }

    public void appendText(String text) {
        XplElement textNode = new XplElement("#text", text);
        appendChild(textNode);
    }

    public void prependText(String text) {
        XplElement textNode = new XplElement("#text", text);
        if (children.isEmpty()) {
            appendChild(textNode);
        } else {
            insertBefore(textNode, children.get(0));
        }
    }

    public void clear() {
        this.children.clear();
        this.textContent = "";
        updateSiblings();
    }

    // ─── Conversão para dicionário XPL ──────────────────────────────────────

    public Map<String, Object> toObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("tagName", tagName);
        map.put("id", getId());
        map.put("className", getClassName());
        map.put("textContent", getTextContent());
        map.put("innerHTML", getInnerHTML());
        map.put("outerHTML", getOuterHTML());
        map.put("childCount", children.size());
        map.put("attributes", attributes);
        map.put("children", children.stream().map(XplElement::toObject).collect(Collectors.toList()));
        return map;
    }

    @Override
    public String toString() {
        if ("#text".equals(tagName) || "#comment".equals(tagName)) {
            return textContent;
        }
        return "<" + tagName + " id=\"" + getId() + "\" class=\"" + getClassName() + "\">";
    }
}