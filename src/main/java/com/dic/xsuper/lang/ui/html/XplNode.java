package com.dic.xsuper.lang.ui.html;

import com.dic.xsuper.lang.poo.XplInstance;
import com.dic.xsuper.lang.ui.document.XplElement;

import javax.swing.*;
import java.util.*;

import static com.dic.xsuper.lang.ui.html.HtmlTagUtils.GLOBAL_ATTRIBUTES;

/**
 * A representação Virtual do DOM na memória do XPL.
 * <p>
 * Esta classe é usada pelo parser HTML e pela SuperUiEngine para construir
 * a árvore de elementos estática e a árvore activa (após processamento de
 * directivas como @if, @for).
 */
public class XplNode {

    // ─── IDENTIFICAÇÃO E HIERARQUIA ──────────────────────────────────────────

    public String tag;
    public String id;
    public String className;
    public XplNode parent = null;
    public XplInstance hostComponent = null;

    // ─── MAPAS DE PROPRIEDADES (Atributos, Directivas, Eventos, Bindings) ──

    public Map<String, Object> attributes = new HashMap<>();
    public Map<String, String> directives = new HashMap<>();
    public Map<String, String> events = new HashMap<>();
    public Map<String, String> bindings = new HashMap<>();


    // ─── ESTILO E ESTADO ─────────────────────────────────────────────────────

    public Map<String, Object> style = new HashMap<>();
    public Object value = null;
    public boolean disabled = false;
    public boolean hidden = false;

    // ─── CONTEÚDO ─────────────────────────────────────────────────────────────

    public List<XplNode> children = new ArrayList<>();
    public String textContent = "";

    // ─── REFERÊNCIA PARA O ELEMENTO VIVO (após hidratação) ─────────────────

    public XplElement liveElement = null;

    // ─── CONSTRUTORES ─────────────────────────────────────────────────────────

    public XplNode(String tag) {
        this.tag = tag;
    }

    public XplNode(String tag, String id) {
        this.tag = tag;
        this.id = id;
    }

    // Em XplNode.java
    public static XplNode fromXplElement(XplElement el) {
        XplNode node = new XplNode(el.tagName);
        node.id = el.getId();
        node.className = el.getClassName();
        node.textContent = el.textContent;
        node.attributes.putAll(el.attributes);
        // Copiar filhos
        for (XplElement child : el.getChildren()) {
            node.addChild(fromXplElement(child));
        }
        return node;
    }

    // ─── MANIPULAÇÃO DE FILHOS ───────────────────────────────────────────────

    public void addChild(XplNode child) {
        child.parent = this;
        this.children.add(child);
    }

    public void removeChild(XplNode child) {
        if (this.children.remove(child)) {
            child.parent = null;
        }
    }

    public void insertBefore(XplNode newNode, XplNode referenceNode) {
        if (referenceNode == null) {
            addChild(newNode);
            return;
        }
        int index = this.children.indexOf(referenceNode);
        if (index == -1) return;
        newNode.parent = this;
        this.children.add(index, newNode);
    }

    public void replaceChild(XplNode newNode, XplNode oldNode) {
        int index = this.children.indexOf(oldNode);
        if (index == -1) return;
        newNode.parent = this;
        this.children.set(index, newNode);
        oldNode.parent = null;
    }

    public void clearChildren() {
        for (XplNode child : children) {
            child.parent = null;
        }
        this.children.clear();
    }

    // ─── MÉTODOS DE NAVEGAÇÃO ─────────────────────────────────────────────────

    public List<XplNode> getAncestors() {
        List<XplNode> ancestors = new ArrayList<>();
        XplNode current = parent;
        while (current != null) {
            ancestors.add(current);
            current = current.parent;
        }
        return ancestors;
    }

    public List<XplNode> getSiblings() {
        if (parent == null) return Collections.emptyList();
        List<XplNode> siblings = new ArrayList<>(parent.children);
        siblings.remove(this);
        return siblings;
    }

    // ─── MÉTODOS DE PESQUISA (DOM Core) ──────────────────────────────────────

    public XplNode getElementById(String idToFind) {
        if (idToFind.equals(this.id)) return this;
        for (XplNode child : children) {
            XplNode found = child.getElementById(idToFind);
            if (found != null) return found;
        }
        return null;
    }

    public List<XplNode> getElementsByClassName(String className) {
        List<XplNode> results = new ArrayList<>();
        if (this.className.contains(className)) results.add(this);
        for (XplNode child : children) {
            results.addAll(child.getElementsByClassName(className));
        }
        return results;
    }

    public List<XplNode> getElementsByTagName(String tagName) {
        List<XplNode> results = new ArrayList<>();
        if (this.tag.equalsIgnoreCase(tagName)) results.add(this);
        for (XplNode child : children) {
            results.addAll(child.getElementsByTagName(tagName));
        }
        return results;
    }

    public List<XplNode> getElementsByName(String name) {
        List<XplNode> results = new ArrayList<>();
        Object attr = attributes.get("name");
        if (attr != null && attr.toString().equals(name)) results.add(this);
        for (XplNode child : children) {
            results.addAll(child.getElementsByName(name));
        }
        return results;
    }

    public XplNode querySelector(String selector) {
        if (selector.startsWith("#")) {
            return getElementById(selector.substring(1));
        } else if (selector.startsWith(".")) {
            List<XplNode> list = getElementsByClassName(selector.substring(1));
            return list.isEmpty() ? null : list.getFirst();
        } else {
            List<XplNode> list = getElementsByTagName(selector);
            return list.isEmpty() ? null : list.getFirst();
        }
    }

    public List<XplNode> querySelectorAll(String selector) {
        if (selector.startsWith("#")) {
            XplNode el = getElementById(selector.substring(1));
            return el != null ? List.of(el) : Collections.emptyList();
        } else if (selector.startsWith(".")) {
            return getElementsByClassName(selector.substring(1));
        } else {
            return getElementsByTagName(selector);
        }
    }

    public XplNode getClosest(String selector) {
        XplNode current = this;
        while (current != null) {
            if (current.matches(selector)) return current;
            current = current.parent;
        }
        return null;
    }

    public boolean matches(String selector) {
        if (selector.startsWith("#")) {
            return this.id.equals(selector.substring(1));
        } else if (selector.startsWith(".")) {
            return this.className.contains(selector.substring(1));
        } else {
            return this.tag.equalsIgnoreCase(selector);
        }
    }

    // ─── MANIPULAÇÃO DE CLASSES ──────────────────────────────────────────────

    public void addClass(String className) {
        Set<String> classes = new HashSet<>(Arrays.asList(this.className.split(" ")));
        classes.add(className);
        this.className = String.join(" ", classes);
    }

    public void removeClass(String className) {
        Set<String> classes = new HashSet<>(Arrays.asList(this.className.split(" ")));
        classes.remove(className);
        this.className = String.join(" ", classes);
    }

    public boolean hasClass(String className) {
        return Arrays.asList(this.className.split(" ")).contains(className);
    }

    public void toggleClass(String className) {
        if (hasClass(className)) {
            removeClass(className);
        } else {
            addClass(className);
        }
    }

    // ─── MANIPULAÇÃO DE ATRIBUTOS ────────────────────────────────────────────

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
        // Sincroniza propriedades especiais
        if ("id".equals(name)) this.id = value.toString();
        if ("class".equals(name)) this.className = value.toString();
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public boolean hasAttribute(String name) {
        return attributes.containsKey(name);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
        if ("id".equals(name)) this.id = null;
        if ("class".equals(name)) this.className = null;
    }

    // ─── ESTILOS INLINE ──────────────────────────────────────────────────────

    public void setStyle(String property, Object value) {
        style.put(property, value);
    }

    public Object getStyle(String property) {
        return style.get(property);
    }

    // ─── CLONAGEM ─────────────────────────────────────────────────────────────

    public XplNode cloneNode(boolean deep) {
        XplNode clone = new XplNode(this.tag);
        clone.id = this.id;
        clone.className = this.className;
        clone.textContent = this.textContent;
        clone.attributes = new HashMap<>(this.attributes);
        clone.directives = new HashMap<>(this.directives);
        clone.events = new HashMap<>(this.events);
        clone.bindings = new HashMap<>(this.bindings);
        clone.style = new HashMap<>(this.style);
        clone.value = this.value;
        clone.disabled = this.disabled;
        clone.hidden = this.hidden;
        clone.hostComponent = this.hostComponent;
        if (deep) {
            for (XplNode child : children) {
                clone.addChild(child.cloneNode(true));
            }
        }
        return clone;
    }

    // ─── CONVERSÃO PARA XplElement (nativo do XPL) ──────────────────────────

    /**
     * Converte este nó (e os seus filhos) numa árvore de objetos XplElement,
     * que são injetáveis no interpretador XPL.
     */
    public XplElement toXplElement() {
        XplElement el = new XplElement(tag);

        if (id != null && !id.isEmpty()) el.setId(id);
        if (className != null && !className.isEmpty()) el.setClassName(className);
        el.textContent = textContent;

        // ─── Injeção de atributos com validação ──────────────────────────
        for (Map.Entry<String, Object> attr : attributes.entrySet()) {
            String key = attr.getKey();
            Object value = attr.getValue();
            // Verifica se o atributo é permitido para esta tag
            if (isAttributeValidForTag(tag, key)) {
                el.setAttribute(key, value);
            } else {
                // Opcional: logar aviso (apenas em modo debug)
                System.out.println("[Aviso] Atributo ignorado para <" + tag + ">: " + key);
            }
        }

        // ─── Eventos inline ────────────────────────────────────────────────
        el.inlineEvents.putAll(events);

        // ─── Estilos inline ────────────────────────────────────────────────
        if (!style.isEmpty()) {
            StringBuilder styleStr = new StringBuilder();
            for (Map.Entry<String, Object> entry : style.entrySet()) {
                styleStr.append(entry.getKey()).append(":").append(entry.getValue()).append(";");
            }
            el.setAttribute("style", styleStr.toString());
        }

        // ==========================================================
        // ⭐ A PONTE QUE FALTAVA: Transferir o cérebro para o Nó Real
        // ==========================================================
        el.hostComponent = this.hostComponent;
        el.inlineEvents.putAll(this.events);
        // ==========================================================

        // ─── Propriedades de estado ──────────────────────────────────────
        if (value != null) el.setAttribute("value", value);
        if (disabled) el.setAttribute("disabled", "true");
        if (hidden) el.setAttribute("hidden", "true");

        // ─── Filhos (recursivo) ──────────────────────────────────────────
        for (XplNode child : children) {
            el.appendChild(child.toXplElement());
        }

        return el;
    }

    // ─── CONVERSÃO PARA OBJECTO XPL (para reflexão) ──────────────────────────

    public Map<String, Object> toXplObject() {
        Map<String, Object> xplObj = new LinkedHashMap<>();

        // ─── Identificação e hierarquia ──────────────────────────────────
        xplObj.put("tag", this.tag);
        xplObj.put("id", this.id);
        xplObj.put("className", this.className);
        xplObj.put("parentId", (this.parent != null && this.parent.id != null) ? this.parent.id : null);

        // ─── Conteúdo e estado ────────────────────────────────────────────
        xplObj.put("textContent", this.textContent != null ? this.textContent : "");
        xplObj.put("value", this.value);
        xplObj.put("disabled", this.disabled);
        xplObj.put("hidden", this.hidden);

        // ─── Mapas dinâmicos ──────────────────────────────────────────────
        xplObj.put("attributes", new LinkedHashMap<>(this.attributes));
        xplObj.put("style", new LinkedHashMap<>(this.style));
        xplObj.put("directives", new LinkedHashMap<>(this.directives));
        xplObj.put("events", new LinkedHashMap<>(this.events));
        xplObj.put("bindings", new LinkedHashMap<>(this.bindings));

        // ─── Dataset (extraído dos atributos data-*) ─────────────────────
        Map<String, Object> dataset = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            if (entry.getKey().startsWith("data-")) {
                dataset.put(entry.getKey().substring(5), entry.getValue());
            }
        }
        xplObj.put("dataset", dataset);

        // ─── Navegação (apenas referências simples, para evitar ciclos) ──
        if (this.parent != null) {
            xplObj.put("parentTag", this.parent.tag);
        }
        if (!children.isEmpty()) {
            xplObj.put("firstChildTag", children.getFirst().tag);
            xplObj.put("lastChildTag", children.getLast().tag);
        }

        // ─── Filhos (recursivo) ───────────────────────────────────────────
        List<Map<String, Object>> xplChildren = new ArrayList<>();
        for (XplNode child : children) {
            xplChildren.add(child.toXplObject());
        }
        xplObj.put("children", xplChildren);
        xplObj.put("childCount", children.size());

        // ─── Informações adicionais (úteis para depuração) ──────────────
        if (liveElement != null) {
            xplObj.put("hasLiveElement", true);
            xplObj.put("liveElementId", liveElement.getId());
        } else {
            xplObj.put("hasLiveElement", false);
        }

        return xplObj;
    }

    // ─── MÉTODO DE INSPEÇÃO ──────────────────────────────────────────────────

    @Override
    public String toString() {
        // Tratamento para nós especiais
        if ("#text".equals(tag) || "#comment".equals(tag)) {
            return textContent != null ? textContent : "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('<').append(tag);

        // Exibe todos os atributos que tenham valor definido
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value != null) {
                String strValue = value.toString();
                if (!strValue.isEmpty()) {
                    sb.append(' ').append(key).append('=').append('"').append(strValue).append('"');
                }
            }
        }

        sb.append("> (").append(children.size()).append(" filhos)");
        return sb.toString();
    }

    // No XplNode.java
    private boolean isAttributeValidForTag(String tag, String attrName) {
        // 1. Atributos globais são sempre válidos
        if (GLOBAL_ATTRIBUTES.contains(attrName)) return true;
        // 2. Atributos data-* e aria-* são sempre válidos
        if (attrName.startsWith("data-") || attrName.startsWith("aria-")) return true;
        // 3. Verifica se o atributo está na lista específica da tag
        return HtmlTagUtils.isSpecificAttribute(tag, attrName);
    }

}
