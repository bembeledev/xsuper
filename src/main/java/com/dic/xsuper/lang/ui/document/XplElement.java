package com.dic.xsuper.lang.ui.document;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.poo.XplInstance;
import com.dic.xsuper.lang.ui.SuperUiEngine;
import com.dic.xsuper.lang.ui.event.XplEvent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Classe que replica o comportamento de um HTMLElement no JavaScript.
 * Deve ser tratada como um objeto XPL nativo.
 */
public class XplElement extends XplInstance implements XplNativeObject{

    private static XPLModel nativeModel;


    // ─── Propriedades estruturais (árvore) ────────────────────────────────────
    public String tagName;
    public XplElement parentNode;
    public XplElement parentElement;      // igual a parentNode, mas mais específico
    public List<XplElement> children = new ArrayList<>();
    public Map<String, Object> attributes = new HashMap<>();
    public String textContent = "";

    // Mapa para guardar os códigos XPL em formato de texto vindos do HTML
    // Exemplo: chave="click", valor="login();logout();"
    public Map<String, String> inlineEvents = new HashMap<>();
    public com.dic.xsuper.lang.poo.XplInstance hostComponent = null;

    // ─── Propriedades de navegação (ponteiros) ──────────────────────────────

    public XplElement nextSibling;
    public XplElement previousSibling;
    public XplElement firstChild;
    public XplElement lastChild;

    // ─── Construtores ──────────────────────────────────────────────────────────

    public XplElement(String tagName) {
        super(SuperUiEngine.ELEMENT_CLASS);
        this.tagName = tagName.toLowerCase();
        // =================================================================
        // ⭐ EXPOR MÉTODOS NATIVOS PARA O XPL ⭐
        // =================================================================

        invokeMethod();
    }

    /**
     * Construtor para nós especiais (texto, comentário).
     * Usado por document.createTextNode() e document.createComment()
     */
    public XplElement(String tagName, String data) {
        super(SuperUiEngine.ELEMENT_CLASS);
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

    /**
     * O método que o teu script XPL vai invocar nativamente!
     * Ex: btn.callAction("click");
     * O método chamado pelo teu script XPL.
     */
    public void callAction(String eventName, Interpreter interpreter) {
        String xplCode = inlineEvents.get(eventName);

        if (xplCode != null && interpreter != null) {
            // ⭐ A MAGIA DA METAPROGRAMAÇÃO E AST ⭐
            // Em vez de um eval sujo, usamos o teu próprio motor léxico e sintático
            // para converter o texto "login();logout();" em Statements reais do teu núcleo!


            try {
                Lexer lexer = new Lexer(xplCode, null);
                List<com.dic.xsuper.lang.Token> tokens = lexer.tokenize();
                com.dic.xsuper.lang.Parser parser = new com.dic.xsuper.lang.Parser(tokens);
                List<com.dic.xsuper.lang.Stmt> statements = parser.parse();

                // ⭐ A VERDADEIRA MAGIA: Injetar diretamente no GLOBALS temporariamente ⭐
                // Como é uma execução dinâmica (eval), o interpretador vai procurar aqui!

                com.dic.xsuper.lang.Environment globais = interpreter.globals;
                java.util.List<String> variaveisInjetadas = new java.util.ArrayList<>();

                try {

                    // 1. O PADRÃO ANGULAR/VUE: Escopo Implícito
                    if (this.hostComponent != null) {
                        com.dic.xsuper.lang.poo.XplInstance host = this.hostComponent;

                        // A) Injetamos todos os MÉTODOS da classe no HTML
                        if (host.klass != null && host.klass.model.methods != null) {
                            for (String methodName : host.klass.model.methods.keySet()) {
                                com.dic.xsuper.lang.Token dummyToken = new com.dic.xsuper.lang.Token(com.dic.xsuper.lang.TokenType.IDENTIFIER, methodName, null, 0, 0, null);
                                Object boundMethod = host.get(dummyToken); // O método já vem com o 'this' embutido!

                                globais.defineLet(methodName, boundMethod);
                                variaveisInjetadas.add(methodName);
                            }
                        }

                        // B) Injetamos todas as VARIÁVEIS (fields) do componente no HTML
                        if (host.fields != null) {
                            for (String fieldName : host.fields.keySet()) {
                                globais.defineLet(fieldName, host.fields.get(fieldName));
                                variaveisInjetadas.add(fieldName);
                            }
                        }
                    }



                    // 2. Injetamos a variável mágica "event"
                    // (Ajuste a classe XplEvent para a sua correspondente, se necessário)
                    globais.defineLet("event", new XplEvent(eventName, this, this));
                    variaveisInjetadas.add("event");

                    // 3. Agora sim, executamos! O interpretador VAI encontrar o getProps e o event no globals.
                    interpreter.interpret(statements);

                } finally {
                    // ⭐ LIMPEZA CIRÚRGICA:
                    // Removemos APENAS o que injetámos para não poluir o programa após o clique.
                    for (String var : variaveisInjetadas) {
                        globais.values.remove(var);
                    }
                }

            } catch (Exception e) {
                System.err.println("Erro ao executar ação do evento '" + eventName + "': " + e.getMessage());
            }
        } else {
            // Se for um listener normal (addEventListener), dispara o evento.
            XplEvent event = new XplEvent(eventName, this);
            dispatchEvent(event);
        }
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
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            Object val = entry.getValue();
            if (val != null && !val.toString().isEmpty()) {
                sb.append(" ").append(entry.getKey()).append("=\"").append(val).append("\"");
            }
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
            sb.append(child.getTextContent().concat(" "));
        }
        return sb.toString().trim();
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
        firstChild = children.getFirst();
        lastChild = children.getLast();
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
            return list.isEmpty() ? null : list.getFirst();
        } else {
            List<XplElement> list = getElementsByTagName(selector);
            return list.isEmpty() ? null : list.getFirst();
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

    @Override
    public void invokeMethod() {

        // ─── EXPOR MÉTODOS NATIVOS PARA O XPL ────────────────────────────────

        // callAction (já existente)
        this.fields.put("callAction", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String eventName = intp.evaluate(args.getFirst().expression).toString();
                callAction(eventName, intp);
                return null;
            }
        });

        // setAttribute (já existente)
        this.fields.put("setAttribute", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String attrName = intp.evaluate(args.get(0).expression).toString();
                Object attrValue = intp.evaluate(args.get(1).expression);
                setAttribute(attrName, attrValue);
                return null;
            }
        });

// getAttribute
        this.fields.put("getAttribute", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String name = intp.evaluate(args.get(0).expression).toString();
                return getAttribute(name);
            }
        });

// hasAttribute
        this.fields.put("hasAttribute", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String name = intp.evaluate(args.get(0).expression).toString();
                return hasAttribute(name);
            }
        });

// removeAttribute
        this.fields.put("removeAttribute", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String name = intp.evaluate(args.get(0).expression).toString();
                removeAttribute(name);
                return null;
            }
        });

// getAttributes
        this.fields.put("getAttributes", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getAttributes();
            }
        });

// appendChild
        this.fields.put("appendChild", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplElement child = (XplElement) intp.evaluate(args.get(0).expression);
                appendChild(child);
                return null;
            }
        });

        // insertBefore
        this.fields.put("insertBefore", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplElement newNode = (XplElement) intp.evaluate(args.get(0).expression);
                XplElement refNode = (XplElement) intp.evaluate(args.get(1).expression);
                insertBefore(newNode, refNode);
                return null;
            }
        });

// replaceChild
        this.fields.put("replaceChild", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplElement newNode = (XplElement) intp.evaluate(args.get(0).expression);
                XplElement oldNode = (XplElement) intp.evaluate(args.get(1).expression);
                replaceChild(newNode, oldNode);
                return null;
            }
        });

// removeChild
        this.fields.put("removeChild", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplElement child = (XplElement) intp.evaluate(args.get(0).expression);
                removeChild(child);
                return null;
            }
        });

// remove
        this.fields.put("remove", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                remove();
                return null;
            }
        });

// cloneNode
        this.fields.put("cloneNode", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                boolean deep = (boolean) intp.evaluate(args.get(0).expression);
                return cloneNode(deep);
            }
        });

// getElementById
        this.fields.put("getElementById", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String id = intp.evaluate(args.get(0).expression).toString();
                return getElementById(id);
            }
        });

// getElementsByClassName
        this.fields.put("getElementsByClassName", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String cls = intp.evaluate(args.get(0).expression).toString();
                return getElementsByClassName(cls);
            }
        });

// getElementsByTagName
        this.fields.put("getElementsByTagName", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String tag = intp.evaluate(args.get(0).expression).toString();
                return getElementsByTagName(tag);
            }
        });

// getElementsByName
        this.fields.put("getElementsByName", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String name = intp.evaluate(args.get(0).expression).toString();
                return getElementsByName(name);
            }
        });

// querySelector
        this.fields.put("querySelector", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String selector = intp.evaluate(args.get(0).expression).toString();
                return querySelector(selector);
            }
        });

// querySelectorAll
        this.fields.put("querySelectorAll", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String selector = intp.evaluate(args.get(0).expression).toString();
                return querySelectorAll(selector);
            }
        });

// getClosest
        this.fields.put("getClosest", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String selector = intp.evaluate(args.get(0).expression).toString();
                return getClosest(selector);
            }
        });

// matches
        this.fields.put("matches", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String selector = intp.evaluate(args.get(0).expression).toString();
                return matches(selector);
            }
        });

// addClass
        this.fields.put("addClass", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String cls = intp.evaluate(args.get(0).expression).toString();
                addClass(cls);
                return null;
            }
        });

// removeClass
        this.fields.put("removeClass", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String cls = intp.evaluate(args.getFirst().expression).toString();
                removeClass(cls);
                return null;
            }
        });

// hasClass
        this.fields.put("hasClass", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String cls = intp.evaluate(args.getFirst().expression).toString();
                return hasClass(cls);
            }
        });

// toggleClass
        this.fields.put("toggleClass", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String cls = intp.evaluate(args.getFirst().expression).toString();
                toggleClass(cls);
                return null;
            }
        });

// setStyle
        this.fields.put("setStyle", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String prop = intp.evaluate(args.get(0).expression).toString();
                String value = intp.evaluate(args.get(1).expression).toString();
                setStyle(prop, value);
                return null;
            }
        });

// getStyle
        this.fields.put("getStyle", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String prop = intp.evaluate(args.getFirst().expression).toString();
                return getStyle(prop);
            }
        });

// setInnerHTML
        this.fields.put("setInnerHTML", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String html = intp.evaluate(args.getFirst().expression).toString();
                setInnerHTML(html);
                return null;
            }
        });

// getInnerHTML
        this.fields.put("getInnerHTML", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getInnerHTML();
            }
        });

// getOuterHTML
        this.fields.put("getOuterHTML", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getOuterHTML();
            }
        });

// getTextContent
        this.fields.put("getTextContent", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getTextContent();
            }
        });

// setTextContent
        this.fields.put("setTextContent", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String text = intp.evaluate(args.getFirst().expression).toString();
                setTextContent(text);
                return null;
            }
        });

// getInnerText
        this.fields.put("getInnerText", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getInnerText();
            }
        });

// getOuterText
        this.fields.put("getOuterText", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getOuterText();
            }
        });

// appendText
        this.fields.put("appendText", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String text = intp.evaluate(args.get(0).expression).toString();
                appendText(text);
                return null;
            }
        });

// prependText
        this.fields.put("prependText", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String text = intp.evaluate(args.get(0).expression).toString();
                prependText(text);
                return null;
            }
        });

// clear
        this.fields.put("clear", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                clear();
                return null;
            }
        });

// addEventListener (com sobrecarga)
        this.fields.put("addEventListener", new XplCallable() {
            @Override public int arity() { return -1; } // aridade variável (2 ou 3)
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                if (args.size() < 2 || args.size() > 3) {
                    throw new IllegalArgumentException("addEventListener espera 2 ou 3 argumentos.");
                }
                String type = intp.evaluate(args.get(0).expression).toString();
                if (args.size() == 2) {
                    // Segundo argumento pode ser XplEventListener ou XplFunction
                    Object listenerObj = intp.evaluate(args.get(1).expression);
                    if (listenerObj instanceof XplEventListener) {
                        addEventListener(type, (XplEventListener) listenerObj);
                    } else if (listenerObj instanceof XplFunction) {
                        addEventListener(type, (XplFunction) listenerObj, intp);
                    } else {
                        throw new IllegalArgumentException("Ouvinte deve ser XplEventListener ou XplFunction.");
                    }
                } else { // 3 argumentos: podemos ignorar o terceiro (options) ou tratar
                    Object listenerObj = intp.evaluate(args.get(1).expression);
                    // options ignorado por simplicidade
                    if (listenerObj instanceof XplEventListener) {
                        addEventListener(type, (XplEventListener) listenerObj);
                    } else if (listenerObj instanceof XplFunction) {
                        addEventListener(type, (XplFunction) listenerObj, intp);
                    } else {
                        throw new IllegalArgumentException("Ouvinte deve ser XplEventListener ou XplFunction.");
                    }
                }
                return null;
            }
        });

// removeEventListener
        this.fields.put("removeEventListener", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String type = intp.evaluate(args.get(0).expression).toString();
                XplEventListener listener = (XplEventListener) intp.evaluate(args.get(1).expression);
                removeEventListener(type, listener);
                return null;
            }
        });

// dispatchEvent
        this.fields.put("dispatchEvent", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplEvent event = (XplEvent) intp.evaluate(args.get(0).expression);
                return dispatchEvent(event);
            }
        });

// toObject
        this.fields.put("toObject", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getDOMObject();
            }
        });

// getDataset
        this.fields.put("getDataset", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getDataset();
            }
        });

// getParent
        this.fields.put("getParent", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getParent();
            }
        });

// getChildren
        this.fields.put("getChildren", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getChildren();
            }
        });

// getAncestors
        this.fields.put("getAncestors", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getAncestors();
            }
        });

// getSiblings
        this.fields.put("getSiblings", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getSiblings();
            }
        });

        // contains
        this.fields.put("contains", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplElement node = (XplElement) intp.evaluate(args.get(0).expression);
                return contains(node);
            }
        });

// insertAdjacentHTML
        this.fields.put("insertAdjacentHTML", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String position = intp.evaluate(args.get(0).expression).toString();
                String html = intp.evaluate(args.get(1).expression).toString();
                insertAdjacentHTML(position, html);
                return null;
            }
        });

        // focus
        this.fields.put("focus", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                focus(intp);
                return null;
            }
        });

        properties();
    }


    public void properties() {
        // ─── PROPRIEDADES (GETTERS, ARIDADE 0) ────────────────────────────────

        // Básicas
        this.fields.put("tagName", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return tagName;
            }
        });
        this.fields.put("id", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getId();
            }
        });
        this.fields.put("className", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getClassName();
            }
        });
        this.fields.put("textContent", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getTextContent();
            }
        });
        this.fields.put("innerHTML", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getInnerHTML();
            }
        });
        this.fields.put("outerHTML", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getOuterHTML();
            }
        });
        this.fields.put("innerText", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getInnerText();
            }
        });
        this.fields.put("outerText", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getOuterText();
            }
        });

        // Navegação (árvore)
        this.fields.put("parentNode", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return parentNode;
            }
        });
        this.fields.put("parentElement", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return parentElement;
            }
        });
        this.fields.put("nextSibling", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return nextSibling;
            }
        });
        this.fields.put("previousSibling", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return previousSibling;
            }
        });
        this.fields.put("firstChild", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return firstChild;
            }
        });
        this.fields.put("lastChild", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return lastChild;
            }
        });
        this.fields.put("children", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getChildren();
            }
        });
        this.fields.put("childElementCount", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return children.size();
            }
        });
        // alias para childCount (se quiser)
        this.fields.put("childCount", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return children.size();
            }
        });

        // Atributos e estilos
        this.fields.put("attributes", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getAttributes();
            }
        });
        this.fields.put("style", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return attributes.get("style");
            }
        });
        this.fields.put("dataset", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getDataset();
            }
        });

        // Dimensões (retornam 0 por enquanto, mas podem ser sobrescritas)
        this.fields.put("clientWidth", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getClientWidth();
            }
        });
        this.fields.put("clientHeight", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getClientHeight();
            }
        });
        this.fields.put("offsetWidth", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getOffsetWidth();
            }
        });
        this.fields.put("offsetHeight", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getOffsetHeight();
            }
        });
        this.fields.put("scrollWidth", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getScrollWidth();
            }
        });
        this.fields.put("scrollHeight", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getScrollHeight();
            }
        });
        this.fields.put("scrollTop", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getScrollTop();
            }
        });
        this.fields.put("scrollLeft", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getScrollLeft();
            }
        });

        // ─── SETTERS PARA PROPRIEDADES MUTÁVEIS (ARIDADE 1) ──────────────────

        this.fields.put("setId", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String val = intp.evaluate(args.getFirst().expression).toString();
                setId(val);
                return null;
            }
        });
        this.fields.put("setClassName", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String val = intp.evaluate(args.getFirst().expression).toString();
                setClassName(val);
                return null;
            }
        });
        this.fields.put("setTextContent", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String val = intp.evaluate(args.getFirst().expression).toString();
                setTextContent(val);
                return null;
            }
        });
        this.fields.put("setInnerHTML", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String val = intp.evaluate(args.getFirst().expression).toString();
                setInnerHTML(val);
                return null;
            }
        });
        this.fields.put("setStyle", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String prop = intp.evaluate(args.get(0).expression).toString();
                String val = intp.evaluate(args.get(1).expression).toString();
                setStyle(prop, val);
                return null;
            }
        });
        this.fields.put("setScrollTop", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                int val = (int) intp.evaluate(args.getFirst().expression);
                setScrollTop(val);
                return null;
            }
        });
        this.fields.put("setScrollLeft", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                int val = (int) intp.evaluate(args.getFirst().expression);
                setScrollLeft(val);
                return null;
            }
        });

        // ─── MÉTODOS DE NAVEGAÇÃO E UTILIDADE ──────────────────────────────────

        // Já existem em invokeMethod, mas podemos adicionar atalhos
        this.fields.put("getParent", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getParent();
            }
        });
        this.fields.put("getChildren", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getChildren();
            }
        });
        this.fields.put("getAncestors", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getAncestors();
            }
        });
        this.fields.put("getSiblings", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getSiblings();
            }
        });
        this.fields.put("getFirstChild", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return firstChild;
            }
        });
        this.fields.put("getLastChild", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return lastChild;
            }
        });
        this.fields.put("getNextSibling", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return nextSibling;
            }
        });
        this.fields.put("getPreviousSibling", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return previousSibling;
            }
        });

        // ─── MÉTODOS DE AÇÃO ────────────────────────────────────────────────────

        // getBoundingClientRect - retorna um mapa com coordenadas
        this.fields.put("getBoundingClientRect", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                Map<String, Object> rect = new LinkedHashMap<>();
                rect.put("left", 0);
                rect.put("top", 0);
                rect.put("right", 0);
                rect.put("bottom", 0);
                rect.put("width", 0);
                rect.put("height", 0);
                return rect;
            }
        });

        // scrollIntoView (sem argumentos por simplicidade)
        this.fields.put("scrollIntoView", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                // Simulação
                return null;
            }
        });

        // focus
        this.fields.put("focus", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                // Simulação
                return null;
            }
        });

        // blur
        this.fields.put("blur", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                // Simulação
                return null;
            }
        });

        // click - dispara evento de clique e executa inline se existir
        this.fields.put("click", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                // Disparar evento listeners
                XplEvent clickEvent = new XplEvent("click", XplElement.this);
                dispatchEvent(clickEvent);
                // Executar código inline, se houver
                String inline = inlineEvents.get("click");
                if (inline != null && intp != null) {
                    try {
                        Lexer lexer = new Lexer(inline, null);
                        List<Token> tokens = lexer.tokenize();
                        Parser parser = new Parser(tokens);
                        List<Stmt> stmts = parser.parse();
                        intp.interpret(stmts);
                    } catch (Exception e) {
                        System.err.println("Erro no evento click inline: " + e.getMessage());
                    }
                }
                return null;
            }
        });

        // ─── ALIAS PARA "class" (já que é palavra reservada no Java) ────────────
        // No XPL, pode-se usar 'class' como propriedade
        this.fields.put("class", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getClassName();
            }
        });
        // Setter para 'class'
        this.fields.put("setClass", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String val = intp.evaluate(args.getFirst().expression).toString();
                setClassName(val);
                return null;
            }
        });

        // ─── MÉTODO GENÉRICO getProperty (já existe, mas pode ser mantido) ──────
        this.fields.put("getProperty", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String propName = intp.evaluate(args.getFirst().expression).toString();
                return getProperty(propName);
            }
        });
    }


    @Override
    public Object getProperty(String propertyName) {
        return switch (propertyName) {
            case "tagName" -> tagName;
            case "id" -> getId();
            case "className" -> getClassName();
            case "textContent" -> getTextContent();
            case "innerHTML" -> getInnerHTML();
            case "outerHTML" -> getOuterHTML();
            case "innerText" -> getInnerText();
            case "outerText" -> getOuterText();
            case "parentNode" -> parentNode;
            case "parentElement" -> parentElement;
            case "nextSibling" -> nextSibling;
            case "previousSibling" -> previousSibling;
            case "firstChild" -> firstChild;
            case "lastChild" -> lastChild;
            case "children" -> getChildren();
            case "childCount" -> children.size();
            case "attributes" -> getAttributes();
            case "style" -> attributes.get("style");
            case "dataset" -> getDataset();
            case "clientWidth" -> getClientWidth();
            case "clientHeight" -> getClientHeight();
            case "offsetWidth" -> getOffsetWidth();
            case "offsetHeight" -> getOffsetHeight();
            case "scrollWidth" -> getScrollWidth();
            case "scrollHeight" -> getScrollHeight();
            case "scrollTop" -> getScrollTop();
            case "scrollLeft" -> getScrollLeft();
            default -> null;
        };
    }


    public static XPLModel buildNativeModel() {
        if (nativeModel == null) {
            nativeModel = new XPLModel("XplElement", null);

            // ─── Campos (propriedades) ──────────────────────────────────────────────
            String[] fieldNames = {
                    "tagName", "id", "className", "textContent", "innerHTML", "outerHTML",
                    "innerText", "outerText", "parentNode", "parentElement", "nextSibling",
                    "previousSibling", "firstChild", "lastChild", "children", "childCount",
                    "attributes", "style", "dataset", "clientWidth", "clientHeight",
                    "offsetWidth", "offsetHeight", "scrollWidth", "scrollHeight",
                    "scrollTop", "scrollLeft"
            };
            for (String f : fieldNames) {
                Token nameToken = new Token(TokenType.IDENTIFIER, f, null, 0, 0);
                // FieldDecl(Token modifier, boolean isStatic, boolean isFinal, boolean isReadonly, Token name, TypeNode type)
                Stmt.FieldDecl field = new Stmt.FieldDecl(new Token(TokenType.PUBLIC,"pub",null,0,0,null), false, false, false, nameToken, null);
                nativeModel.addField(field);
            }

            // ─── Métodos ──────────────────────────────────────────────────────────────

            // setAttribute(name, value)
            List<Stmt.Param> paramsNameVal = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "name", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "value", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "setAttribute", null, 0, 0),
                    paramsNameVal,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getAttribute(name)
            List<Stmt.Param> paramsName = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "name", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getAttribute", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // hasAttribute(name)
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "hasAttribute", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // removeAttribute(name)
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "removeAttribute", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getAttributes()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getAttributes", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // ─── Árvore ───────────────────────────────────────────────────────────────

            // appendChild(child)
            List<Stmt.Param> paramsChild = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "child", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "appendChild", null, 0, 0),
                    paramsChild,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // insertBefore(newNode, refNode)
            List<Stmt.Param> paramsNewRef = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "newNode", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "refNode", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "insertBefore", null, 0, 0),
                    paramsNewRef,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // replaceChild(newNode, oldNode)
            List<Stmt.Param> paramsNewOld = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "newNode", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "oldNode", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "replaceChild", null, 0, 0),
                    paramsNewOld,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // removeChild(child)
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "removeChild", null, 0, 0),
                    paramsChild,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // remove()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "remove", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // cloneNode(deep)
            List<Stmt.Param> paramsDeep = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "deep", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "cloneNode", null, 0, 0),
                    paramsDeep,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // ─── Pesquisa ─────────────────────────────────────────────────────────────

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

            // getElementsByTagName(tagName)
            List<Stmt.Param> paramsTag = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "tagName", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getElementsByTagName", null, 0, 0),
                    paramsTag,
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

            // getClosest(selector)
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getClosest", null, 0, 0),
                    paramsSelector,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // matches(selector)
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "matches", null, 0, 0),
                    paramsSelector,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // ─── Classes CSS ────────────────────────────────────────────────────────

            // addClass(className)
            List<Stmt.Param> paramsCls = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "className", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "addClass", null, 0, 0),
                    paramsCls,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // removeClass(className)
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "removeClass", null, 0, 0),
                    paramsCls,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // hasClass(className)
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "hasClass", null, 0, 0),
                    paramsCls,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // toggleClass(className)
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "toggleClass", null, 0, 0),
                    paramsCls,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // ─── Estilos ─────────────────────────────────────────────────────────────

            // setStyle(property, value)
            List<Stmt.Param> paramsStyle = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "property", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "value", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "setStyle", null, 0, 0),
                    paramsStyle,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getStyle(property)
            List<Stmt.Param> paramsProp = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "property", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getStyle", null, 0, 0),
                    paramsProp,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // ─── Conteúdo ────────────────────────────────────────────────────────────

            // setInnerHTML(html)
            List<Stmt.Param> paramsHTML = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "html", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "setInnerHTML", null, 0, 0),
                    paramsHTML,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getInnerHTML()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getInnerHTML", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getOuterHTML()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getOuterHTML", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getTextContent()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getTextContent", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // setTextContent(text)
            List<Stmt.Param> paramsText = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "text", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "setTextContent", null, 0, 0),
                    paramsText,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getInnerText()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getInnerText", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getOuterText()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getOuterText", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // appendText(text)
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "appendText", null, 0, 0),
                    paramsText,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // prependText(text)
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "prependText", null, 0, 0),
                    paramsText,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // clear()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "clear", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // ─── Eventos ─────────────────────────────────────────────────────────────

            // addEventListener(type, listener)
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

            // removeEventListener(type, listener)
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "removeEventListener", null, 0, 0),
                    paramsEventListener,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // dispatchEvent(event)
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

            // callAction(eventName)
            List<Stmt.Param> paramsEvtName = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "eventName", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "callAction", null, 0, 0),
                    paramsEvtName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // ─── Utilitários ────────────────────────────────────────────────────────

            // toObject()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "toObject", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getDataset()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getDataset", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getParent()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getParent", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getChildren()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getChildren", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getAncestors()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getAncestors", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getSiblings()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getSiblings", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // contains(child)
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "contains", null, 0, 0),
                    paramsChild,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // focus()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "focus", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // blur()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "blur", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // click()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "click", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // insertAdjacentHTML(position, html)
            List<Stmt.Param> paramsPosHTML = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "position", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "html", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "insertAdjacentHTML", null, 0, 0),
                    paramsPosHTML,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getProperty(propName)
            List<Stmt.Param> paramsPropName = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "propName", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getProperty", null, 0, 0),
                    paramsPropName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
        }
        return nativeModel;
    }


    public void focus(Interpreter intp) {
        XplEvent focusEvent = new XplEvent("focus", this);
        dispatchEvent(focusEvent);
        String inline = inlineEvents.get("focus");
        if (inline != null && intp != null) {
            try {
                Lexer lexer = new Lexer(inline, null);
                List<Token> tokens = lexer.tokenize();
                Parser parser = new Parser(tokens);
                List<Stmt> stmts = parser.parse();
                intp.interpret(stmts);
            } catch (Exception e) {
                System.err.println("Erro no evento focus inline: " + e.getMessage());
            }
        }
    }

    /**
     * Insere HTML em uma posição relativa a este elemento.
     * @param position "beforebegin", "afterbegin", "beforeend", "afterend"
     * @param html Texto HTML a ser inserido
     */
    public void insertAdjacentHTML(String position, String html) {
        if (html == null || html.isEmpty()) return;
        // Para simplificar, criamos um nó de texto com o HTML bruto.
        // Numa implementação real, faríamos parse do HTML para criar nós.
        XplElement fragment = new XplElement("#document-fragment");
        XplElement textNode = new XplElement("#text", html);
        fragment.appendChild(textNode);

        switch (position.toLowerCase()) {
            case "beforebegin":
                if (parentNode != null) {
                    parentNode.insertBefore(fragment, this);
                }
                break;
            case "afterbegin":
                if (children.isEmpty()) {
                    appendChild(fragment);
                } else {
                    insertBefore(fragment, children.getFirst());
                }
                break;
            case "beforeend":
                appendChild(fragment);
                break;
            case "afterend":
                if (parentNode != null) {
                    parentNode.insertBefore(fragment, nextSibling);
                }
                break;
            default:
                throw new IllegalArgumentException("Posição inválida: " + position);
        }
    }


    /**
     * Verifica se este elemento contém o nó especificado.
     * Um nó contém a si próprio (returns true se node == this).
     */
    public boolean contains(XplElement node) {
        if (node == null) return false;
        if (node == this) return true;
        XplElement current = node.parentNode;
        while (current != null) {
            if (current == this) return true;
            current = current.parentNode;
        }
        return false;
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
            insertBefore(textNode, children.getFirst());
        }
    }

    public void clear() {
        this.children.clear();
        this.textContent = "";
        updateSiblings();
    }

    // ─── Conversão para dicionário XPL ──────────────────────────────────────

    public Map<String, Object> getDOMObject() {
        Map<String, Object> map = new LinkedHashMap<>();

        // Propriedades básicas
        map.put("tagName", tagName);
        map.put("id", getId());
        map.put("className", getClassName());
        map.put("classList", getClassList()); // lista de classes
        map.put("textContent", getTextContent());
        map.put("innerText", getInnerText());
        map.put("outerText", getOuterText());
        map.put("innerHTML", getInnerHTML());
        map.put("outerHTML", getOuterHTML());

        // Estilos (como mapa)
        String styleStr = (String) attributes.getOrDefault("style", "");
        map.put("style", parseStyle(styleStr)); // converte para mapa

        // Dataset (atributos data-*)
        map.put("dataset", getDataset());

        // Atributos completos (como mapa)
        map.put("attributes", attributes);

        // Dimensões (se houver valores reais, senão 0)
        map.put("clientWidth", getClientWidth());
        map.put("clientHeight", getClientHeight());
        map.put("offsetWidth", getOffsetWidth());
        map.put("offsetHeight", getOffsetHeight());
        map.put("scrollWidth", getScrollWidth());
        map.put("scrollHeight", getScrollHeight());
        map.put("scrollTop", getScrollTop());
        map.put("scrollLeft", getScrollLeft());

        // Navegação (apenas contagem, não referências para evitar ciclos)
        map.put("childCount", children.size());
        map.put("hasChildren", !children.isEmpty());

        // Filhos (serialização recursiva)
        map.put("children", children.stream().map(XplElement::getDOMObject).collect(Collectors.toList()));


        return map;
    }

    @Override
    public String toString() {
        if ("#text".equals(tagName) || "#comment".equals(tagName)) {
            return textContent;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(tagName);
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value != null && !value.toString().isEmpty()) {
                sb.append(' ').append(key).append('"').append(value).append('"');
            }
        }
        sb.append('>');
        return sb.toString();
    }
}