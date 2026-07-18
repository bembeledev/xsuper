package com.dic.xsuper.lang.ui.document;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.poo.XplInstance;
import com.dic.xsuper.lang.ui.SuperUiEngine;
import com.dic.xsuper.lang.ui.document.helpers.XplElementUtils;
import com.dic.xsuper.lang.ui.event.XplEvent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Classe que replica o comportamento de um HTMLElement no JavaScript.
 * Deve ser tratada como um objeto XPL nativo.
 */
public class XplElement extends XplInstance implements XplNativeObject{

    public static XPLModel nativeModel;


    // ─── Propriedades estruturais (árvore) ────────────────────────────────────
    public String tagName;
    public XplElement parentNode;
    public XplElement parentElement;      // igual a parentNode, mas mais específico
    public List<XplElement> children = new ArrayList<>();
    public Map<String, Object> attributes = new HashMap<>();
    public String textContent = "";

    // Flag de bloqueio para impedir Loops Infinitos (JavaFX <-> XPL)
    public boolean isSyncing = false;

    // Mapa para guardar os códigos XPL em formato de texto vindos do HTML
    // Exemplo: chave="click", valor="login();logout();"
    public Map<String, String> inlineEvents = new HashMap<>();
    public com.dic.xsuper.lang.poo.XplInstance hostComponent = null;

    // ─── Propriedades de navegação (ponteiros) ──────────────────────────────

    public XplElement nextSibling;
    public XplElement previousSibling;
    public XplElement firstChild;
    public XplElement lastChild;

    // Assume uma matrícula própria (útil para document.createElement("div"))
    public String _internalUid = com.dic.xsuper.lang.ui.html.XplNode.generateUid();

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

    // ─── NOVAS PROPRIEDADES UNIVERSAIS (value, name, type, etc) ──────────────

    public Object getValue() { return attributes.getOrDefault("value", ""); }
    public void setValue(Object value) { setAttribute("value", value); }

    public String getName() { return (String) attributes.getOrDefault("name", ""); }
    public void setName(String name) { setAttribute("name", name); }

    public String getType() { return (String) attributes.getOrDefault("type", ""); }
    public void setType(String type) { setAttribute("type", type); }

    public boolean getChecked() {
        Object chk = attributes.get("checked");
        return chk != null && (chk.equals(true) || chk.equals("true"));
    }
    public void setChecked(boolean checked) { setAttribute("checked", checked); }

    public String getSrc() { return (String) attributes.getOrDefault("src", ""); }
    public void setSrc(String src) { setAttribute("src", src); }

    public String getHref() { return (String) attributes.getOrDefault("href", ""); }
    public void setHref(String href) { setAttribute("href", href); }

    public boolean isDisabled() { return attributes.containsKey("disabled") && !attributes.get("disabled").equals("false"); }
    public void setDisabled(boolean disabled) {
        if (disabled) setAttribute("disabled", "true");
        else removeAttribute("disabled");
    }

    public boolean isReadOnly() { return attributes.containsKey("readonly") && !attributes.get("readonly").equals("false"); }
    public void setReadOnly(boolean readOnly) {
        if (readOnly) setAttribute("readonly", "true");
        else removeAttribute("readonly");
    }

    public String getPlaceholder() { return (String) attributes.getOrDefault("placeholder", ""); }
    public void setPlaceholder(String placeholder) { setAttribute("placeholder", placeholder); }

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
        if (value == null) return;

        // Protecção contra poluição: Nunca guardamos propriedades virtuais no HTML
        if (name.equalsIgnoreCase("innerHTML") || name.equalsIgnoreCase("textContent") || name.equalsIgnoreCase("innerText")) {
            return;
        }

        if (isSyncing) {
            attributes.put(name, value);
            return;
        }

        Object oldValue = attributes.get(name);
        if (value.equals(oldValue)) return;

        attributes.put(name, value);

        // Dispara reactividade
        com.dic.xsuper.lang.ui.event.eventbus.UiEventPublisher.publishDomMutated(this._internalUid, name, value);
    }


    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public boolean hasAttribute(String name) {
        return attributes.containsKey(name);
    }

    /*
     * Método interno rigoroso: Usado APENAS quando o JavaFX está a atualizar o DOM.
     * Actualiza a memória silenciosamente sem disparar um novo evento para a UI.
     *
     * Actualiza a memória silenciosamente sem disparar um novo evento.
     */
    public void setAttributeSilently(String name, Object value) {
        // ⭐ PROTECÇÃO ABSOLUTA: Nunca poluir os atributos HTML com conteúdo virtual!
        if (name.equalsIgnoreCase("innerHTML") || name.equalsIgnoreCase("textContent") || name.equalsIgnoreCase("innerText")) {
            return;
        }

        isSyncing = true;
        try {
            attributes.put(name, value);
        } finally {
            isSyncing = false;
        }
    }

    public void removeAttribute(String name) {
        if (!attributes.containsKey(name)) return;
        attributes.remove(name);

        if (!isSyncing && getId() != null && !getId().isEmpty()) {
            SuperUiEngine engine = SuperUiEngine.getInstance();
            if (engine != null) {
                //engine.notifyStateChanged(getId(), name, ""); // Envia vazio para apagar no JavaFX
            }
        }
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
        //injeção de metodos acessores e proriedades
        XplElementUtils.invokeMethod(this);
    }

    // Em XplElement.java

    @Override
    public void set(com.dic.xsuper.lang.Token name, Object value) {
        String propName = name.lexeme;

        if (propName.equalsIgnoreCase("innerHTML")) {
            // ⭐ Transporta o _internalUid!
            com.dic.xsuper.lang.ui.event.eventbus.UiEventPublisher.publishDomMutated(this._internalUid, propName, value);
            return;
        }
        if (propName.equalsIgnoreCase("textContent") || propName.equalsIgnoreCase("innerText")) {
            this.textContent = value != null ? value.toString() : "";
            this.children.clear();
            com.dic.xsuper.lang.ui.event.eventbus.UiEventPublisher.publishDomMutated(this._internalUid, "textContent", value);
            return;
        }

        setAttribute(propName, value);
    }



    // ─── ATUALIZAR O GETPROPERTY E GETDOMOBJECT ─────────────────────────────

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
            // 👇 NOVAS PROPRIEDADES 👇
            case "value" -> getValue();
            case "name" -> getName();
            case "type" -> getType();
            case "checked" -> getChecked();
            case "src" -> getSrc();
            case "href" -> getHref();
            case "disabled" -> isDisabled();
            case "readonly" -> isReadOnly();
            case "placeholder" -> getPlaceholder();
            default -> null;
        };
    }


    public static XPLModel buildNativeModel() {
        if (nativeModel == null) return XplElementUtils.buildNativeModel();
        return nativeModel;
    }

    @Override
    public Object get(com.dic.xsuper.lang.Token name) {
        // Tenta resolver nas propriedades da Web W3C (value, innerHTML, etc)
        Object propertyValue = getProperty(name.lexeme);

        // Se a propriedade existir (mesmo que seja uma string vazia ""), devolve-a.
        if (propertyValue != null) {
            return propertyValue;
        }

        // Fallback normal do XPL para métodos e classes
        return super.get(name);
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