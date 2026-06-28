package com.dic.xsuper.lang.ui;

import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;
import com.dic.xsuper.lang.XplFunction;
import com.dic.xsuper.lang.ui.document.*;
import com.dic.xsuper.lang.ui.html.HtmlLexer;
import com.dic.xsuper.lang.ui.html.HtmlParser;
import com.dic.xsuper.lang.ui.html.HtmlToken;

import java.util.*;

/**
 * A Super Engine: O coração que bombeia a vida entre a Linguagem XPL e o Renderizador Gráfico (JavaFX).
 * <p>
 * Esta classe gerencia o ciclo de vida da UI, a reatividade, a hidratação do DOM,
 * e a comunicação bidirecional entre o XPL e a interface gráfica.
 */
public class SuperUiEngine {

    // ─── Pilares da Engine ──────────────────────────────────────────────────

    private final Interpreter interpreter;    // O cérebro (Lógica e Memória XPL)
    private final XplUiBridge rendererBridge; // A ponte para o Pintor (JavaFX)
    private final DomEvaluator evaluator;     // O purificador de árvores (@if, @for)

    // ─── Estado da Aplicação ────────────────────────────────────────────────

    private XplNode staticRoot;    // A "Planta" original (com diretivas @ intactas)
    private XplNode activeDom;     // A "Casa" construída (árvore limpa, hidratada)

    // ─── O Documento Global (injetado no XPL) ─────────────────────────────

    private final XplDocument document;

    // ─── Registro de funções XPL para eventos da UI ────────────────────────

    private final Map<String, List<XplEventListener>> eventListeners = new HashMap<>();

    // ─── Construtor ─────────────────────────────────────────────────────────

    public SuperUiEngine(Interpreter interpreter, XplUiBridge rendererBridge) {
        this.interpreter = interpreter;
        this.rendererBridge = rendererBridge;
        this.evaluator = new DomEvaluator(interpreter);

        // Cria o documento global com um elemento raiz <html>
        XplElement root = new XplElement("html");
        this.document = new XplDocument(root);
        // ⭐ AQUI ESTÁ A CORREÇÃO: Verificamos se o interpretador não é nulo antes de injetar
        if (this.interpreter != null && this.interpreter.globals != null) {
            // Injeta o documento no ambiente XPL como 'document' (ou 'ui' se preferires)
            this.interpreter.globals.defineConst("document", this.document);
            this.interpreter.globals.defineConst("ui", this.document); // alias

            // Injeta a própria engine (para funções como ui.render())
            this.interpreter.globals.defineConst("__ui_engine", this);
        }
    }

    // ─── Ponto de entrada: Carregar a View ──────────────────────────────────

    public void loadView(String htmlSource) {
        System.out.println("[Engine] 1. A ler o HTML e a extrair Tokens...");
        HtmlLexer lexer = new HtmlLexer(htmlSource);
        List<HtmlToken> tokens = lexer.scanTokens();

        System.out.println("[Engine] 2. A construir a Planta Estática (Static VDOM)...");
        HtmlParser parser = new HtmlParser(tokens);
        this.staticRoot = parser.parse();

        System.out.println("[Engine] 3. A registar os Elementos no Interpretador XPL...");
        registerNodesInXpl(this.staticRoot);

        System.out.println("[Engine] 4. A iniciar o 1º Ciclo de Renderização...");
        renderCycle();
    }

    // ─── Ciclo de Reactividade (O Loop da Magia) ─────────────────────────────

    /**
     * Recalcula os @if e @for, hidrata os nós com ID, e renderiza a UI.
     * Deve ser chamado sempre que uma variável XPL relevante mudar.
     */
    public void renderCycle() {
        if (staticRoot == null) return;

        // 1. Limpar a memória do documento antigo (re-render total)
        this.document.documentElement.clear();
        this.document.body = null; // será recriado

        // 2. O Evaluator resolve os @if e multiplica os @for
        this.activeDom = evaluator.evaluateTree(staticRoot);

        // 3. 💧 Hidratação: criar os objetos vivos (XplElement) para os IDs
        hydrateHeadlessDom(this.activeDom);


        // 4. Atualizar as propriedades de navegação do documento
        if (this.activeDom != null) {
            this.document.documentElement = this.activeDom.toXplElement();

            // ─── BUSCA DEFENSIVA DO BODY ────────────────────────────────────────
            XplNode bodyNode = this.activeDom.querySelector("body");
            XplElement body;

            if (bodyNode != null) {
                body = bodyNode.toXplElement();
            } else {
                // Cria um body se não existir no HTML bruto
                body = new XplElement("body");
                this.document.documentElement.appendChild(body);
            }
            this.document.body = body;

            // ─── BUSCA DEFENSIVA DO HEAD ────────────────────────────────────────
            XplNode headNode = this.activeDom.querySelector("head");
            XplElement head;

            if (headNode != null) {
                head = headNode.toXplElement();
            } else {
                // Cria um head se não existir
                head = new XplElement("head");
                this.document.documentElement.insertBefore(head, this.document.documentElement.firstChild);
            }
            this.document.head = head;
        }

        // 5. Enviar para a Ponte Gráfica (se existir)
        if (rendererBridge != null) {
            rendererBridge.renderView(this.activeDom);
        }
    }

    // ─── Comunicação UI → Engine → XPL ──────────────────────────────────────

    /**
     * Quando o utilizador interage com a UI (ex: clica num botão), o JavaFX chama este método.
     * O evento é despachado para os ouvintes XPL registados.
     */
    public void dispatchEvent(String eventName, Object payload, String targetId) {
        System.out.println("[Engine] Evento recebido da UI: " + eventName + " (alvo: " + targetId + ")");

        // 1. Criar o objeto XplEvent
        XplEvent event = new XplEvent(eventName);
        event.detail.put("payload", payload);
        event.detail.put("targetId", targetId);

        // 2. Disparar para os ouvintes do documento
        for (XplEventListener listener : eventListeners.getOrDefault(eventName, Collections.emptyList())) {
            listener.handleEvent(event);
        }

        // 3. Se houver um elemento específico com aquele ID, disparar também nele
        if (targetId != null) {
            XplElement element = document.getElementById(targetId);
            if (element != null) {
                element.dispatchEvent(event);
            }
        }

        // 4. Após o evento, re-renderizar (se as funções XPL alteraram variáveis)
        renderCycle();
    }

    // ─── Registar funções XPL como ouvintes de eventos ──────────────────────

    /**
     * Regista uma função XPL para ser chamada quando um evento ocorrer no documento.
     */
    public void addEventListener(String type, XplFunction function) {
        addEventListener(type, new XplElement.XplFunctionListener(function, interpreter));
    }

    /**
     * Regista um listener Java nativo.
     */
    public void addEventListener(String type, XplEventListener listener) {
        eventListeners.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
    }

    // ─── Sistema Nervoso Central (Reatividade Cirúrgica) ────────────────────

    /**
     * Invocado pelo UiElement (Headless DOM) sempre que o código XPL altera um atributo.
     * Ex: O XPL executou `botao.setAttribute("disabled", true)`.
     */
    public void notifyStateChanged(String id, String name, Object value) {
        System.out.println("[Engine] ⚡ Mutação detetada no ID '" + id + "': [" + name + "] = " + value);

        // 1. Atualiza a Árvore Virtual Limpa (activeDom) para manter a coerência da memória
        syncActiveDomState(activeDom, id, name, value);

        // 2. Avisar a Interface Gráfica (SE ELA EXISTIR!)
        if (rendererBridge != null) {
            rendererBridge.updateProperty(id, name, value);
        }
    }

    // ─── Sincronização da Árvore Virtual ────────────────────────────────────

    private void syncActiveDomState(XplNode node, String targetId, String attrName, Object newValue) {
        if (node == null) return;
        if (targetId.equals(node.id)) {
            if (attrName.equals("class")) {
                node.className = String.valueOf(newValue);
            } else if (attrName.equals("id")) {
                node.id = String.valueOf(newValue);
            } else {
                node.attributes.put(attrName, String.valueOf(newValue));
            }
            return;
        }
        for (XplNode child : node.children) {
            syncActiveDomState(child, targetId, attrName, newValue);
        }
    }

    // ─── Hidratação: Criar os Objetos Vivos (XplElement) ────────────────────

    private void hydrateHeadlessDom(XplNode node) {
        if (node == null) return;

        // Se o nó tiver um ID, ele ganha vida no mundo XPL
        if (node.id != null && !node.id.isEmpty()) {
            XplElement element = new XplElement(node.tag);

            // Copia atributos (silenciosamente, para não disparar notificações)
            for (Map.Entry<String, Object> entry : node.attributes.entrySet()) {
                element.setAttribute(entry.getKey(), entry.getValue());
            }
            element.setId(node.id);
            element.setClassName(node.className);
            element.textContent = node.textContent;

            // Regista no documento global
            document.registerElement(element);

            // Guarda a referência no nó para futuras sincronizações
            node.liveElement = element;

            System.out.println("[Engine] 💧 Nó Hidratado para XPL: " + node.id + " (" + node.tag + ")");
        }

        // Recursão para os filhos
        for (XplNode child : node.children) {
            hydrateHeadlessDom(child);
            // Se o pai tem um elemento vivo, anexa os filhos hidratados à árvore DOM viva
            if (node.liveElement != null && child.liveElement != null) {
                node.liveElement.appendChild(child.liveElement);
            }
        }
    }

    // ─── Registar nós na tabela de símbolos do XPL ──────────────────────────

    private void registerNodesInXpl(XplNode node) {
        if (!node.id.isEmpty()) {
            // Aqui podes injetar diretamente no ambiente, se preferires
            // interpreter.environment.defineLet(node.id, node.liveElement);
        }
        for (XplNode child : node.children) {
            registerNodesInXpl(child);
        }
    }

    // ─── Acesso ao Documento Global ──────────────────────────────────────────

    public XplDocument getDocument() {
        return this.document;
    }

    // ─── Métodos utilitários para o XPL ─────────────────────────────────────

    /**
     * Função nativa XPL: ui.render() – força um re-render.
     */
    public void render() {
        renderCycle();
    }

    /**
     * Função nativa XPL: ui.getElementById(id) – atalho para o documento.
     */
    public XplElement getElementById(String id) {
        return document.getElementById(id);
    }

    /**
     * Função nativa XPL: ui.querySelector(selector) – atalho para o documento.
     */
    public XplElement querySelector(String selector) {
        return document.querySelector(selector);
    }

    /**
     * Função nativa XPL: ui.querySelectorAll(selector) – atalho para o documento.
     */
    public List<XplElement> querySelectorAll(String selector) {
        return document.querySelectorAll(selector);
    }

    // ─── Registar as funções nativas da engine no XPL ──────────────────────

    public void registerNativeFunctions() {
        interpreter.globals.defineConst("ui_render", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                render();
                return null;
            }
        });

        interpreter.globals.defineConst("ui_getElementById", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String id = (String) intp.evaluate(args.get(0).expression);
                return getElementById(id);
            }
        });

        interpreter.globals.defineConst("ui_querySelector", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String selector = (String) intp.evaluate(args.get(0).expression);
                return querySelector(selector);
            }
        });
    }

}