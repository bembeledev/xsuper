package com.dic.xsuper.lang.ui;
import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.ui.animation.XplKeyframe;
import com.dic.xsuper.lang.ui.animation.XplKeyframeAnimation;
import com.dic.xsuper.lang.ui.helpers.SuperUiEngineUtils;
import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.poo.XplClass;
import com.dic.xsuper.lang.poo.XplInstance;
import com.dic.xsuper.lang.ui.css.*;
import com.dic.xsuper.lang.ui.css.media.JavaFxMediaListener;
import com.dic.xsuper.lang.ui.css.media.XplMediaNode;
import com.dic.xsuper.lang.ui.document.*;
import com.dic.xsuper.lang.ui.event.XplEvent;
import com.dic.xsuper.lang.ui.html.*;
import com.dic.xsuper.lang.ui.window.MainWindow;

import java.util.*;

/**
 * A Super Engine: O coração que bombeia a vida entre a Linguagem XPL e o Renderizador Gráfico (JavaFX).
 * <p>
 * Esta classe gerência o ciclo de vida da UI, a reactividade, a hidratação do DOM,
 * e a comunicação bidirecional entre o XPL e a interface gráfica.
 */

public class SuperUiEngine extends XplInstance implements XplNativeObject {

    // Variável para guardar o motor que está a rodar
    private static SuperUiEngine instance;
    public static  XplClass EVENT_CLASS; ;

    // 1. As Plantas (Classes) Nativas Globais
    public static XplClass ELEMENT_CLASS;
    public static XplClass DOCUMENT_CLASS;
    public static XplClass ENGINE_CLASS;

    // Janela Principal
    MainWindow mainWindow;

    // 1. O Registo Global de Keyframes (Adiciona ao topo da SuperUiEngine)
    private final Map<String, XplKeyframeAnimation> keyframesRegistry = new HashMap<>();

    // ─── Pilares da Engine ──────────────────────────────────────────────────

    private  final Interpreter interpreter;     // O cérebro (Lógica e Memória XPL)
    private XplUiBridge rendererBridge;         // A ponte para o Pintor (JavaFX)
    private final DomEvaluator evaluator;       // O purificador de árvores (@if, @for)

    // ─── Estado da Aplicação ────────────────────────────────────────────────

    private XplNode staticRoot;    // A "Planta" original (com directivas @ intactas)
    private XplNode activeDom;     // A "Casa" construída (árvore limpa, hidratada)

    // ⭐ NOVO: Guarda todos os blocos de CSS carregados
    private final List<String> loadedStyles = new ArrayList<>();

    public XplNode getActiveDom() {
        return activeDom;
    }

    public XplNode getStaticRoot() {
        return staticRoot;
    }

    // ─── O Documento Global (injectado no XPL) ─────────────────────────────
    private final XplDocument document;

    // ─── Registo de funções XPL para eventos da UI ────────────────────────
    public final Map<String, List<XplEventListener>> eventListeners = new HashMap<>();

    // Instância global do Listener
    private final JavaFxMediaListener mediaListener = new JavaFxMediaListener();

    public static XPLModel nativeModel;

    // Mapa que associa o nome da tag personalizada à sua classe XPL
    private final Map<String, XplClass> componentRegistry = new HashMap<>();

    // ─── Construtor ─────────────────────────────────────────────────────────
    public SuperUiEngine(Interpreter interpreter, XplUiBridge rendererBridge) {
        instance = this;
        this.interpreter = interpreter;
        this.rendererBridge = rendererBridge;
        this.evaluator = new DomEvaluator(interpreter, this);

        // No construtor, após criar mediaListener
        mediaListener.setEngineRebuildTrigger(this::renderCycle);
        this.mainWindow = new MainWindow(this);
        this.mediaListener.setEngineRebuildTrigger(this.mainWindow::forceLayoutRebuild);

        if (this.interpreter != null && this.interpreter.globals != null) {

            // =================================================================
            // ⭐ FASE 1: OBTER OS MODELOS NATIVOS REAIS (SEM REPETIR CÓDIGO!)
            // =================================================================
            XPLModel elementModel = com.dic.xsuper.lang.ui.document.XplElement.buildNativeModel();
            XPLModel documentModel = com.dic.xsuper.lang.ui.document.XplDocument.buildNativeModel();
            XPLModel engineModel = com.dic.xsuper.lang.ui.SuperUiEngine.buildNativeModel();

            XPLModel eventModel = XplEvent.buildNativeModel();

            // =================================================================
            // ⭐ FASE 1.5: REGISTAR NO COMPILADOR (Para o 'extends' funcionar)
            // Aqui dizemos ao Resolver: "Ei, estas classes nativas existem!"
            // =================================================================
            this.interpreter.registry_model.put("XplElement", elementModel);
            this.interpreter.registry_model.put("XplDocument", documentModel);
            // Opcional, se precisares de estender a engine nalgum momento:
            this.interpreter.registry_model.put("SuperUiEngine", engineModel);

            interpreter.registry_model.put("XplEvent", eventModel);

            // =================================================================
            // ⭐ FASE 1.8: CRIAR AS CLASSES RUNTIME E REGISTAR NA MEMÓRIA GLOBAL
            // =================================================================
            ELEMENT_CLASS = new com.dic.xsuper.lang.poo.XplClass(elementModel, interpreter.globals);
            DOCUMENT_CLASS = new com.dic.xsuper.lang.poo.XplClass(documentModel, interpreter.globals);
            ENGINE_CLASS = new com.dic.xsuper.lang.poo.XplClass(engineModel, interpreter.globals);
            EVENT_CLASS = new XplClass(eventModel, interpreter.globals);
            // 2. Registar no Runtime (apenas se a classe NÃO estiver lá)
            if (!this.interpreter.globals.values.containsKey("XplElement")) {
                ELEMENT_CLASS = new com.dic.xsuper.lang.poo.XplClass(elementModel, interpreter.globals);
                this.interpreter.globals.defineConst("XplElement", ELEMENT_CLASS);

            }
            if (!this.interpreter.globals.values.containsKey("XplDocument")) {
                DOCUMENT_CLASS = new com.dic.xsuper.lang.poo.XplClass(documentModel, interpreter.globals);
                this.interpreter.globals.defineConst("XplDocument", DOCUMENT_CLASS);
            }
            if (!interpreter.globals.values.containsKey("XplEvent")) {
                interpreter.globals.defineConst("XplEvent", EVENT_CLASS);
            }
            // =================================================================
            // ⭐ FASE 2: A PRÓPRIA ENGINE ASSUME A SUA IDENTIDADE (__ui_engine)
            // =================================================================
            this.klass = ENGINE_CLASS;
            this.invokeMethod(); // Injecta os próprios métodos!
            this.interpreter.globals.defineConst("__ui_engine", this);
        }

        // =================================================================
        // ⭐ FASE 3: SÓ AGORA INSTANCIÁMOS OS OBJECTOS VIVOS (DOM)
        // Agora o 'super(ELEMENT_CLASS)' lá dentro do Java vai encontrar a classe perfeitamente!
        // =================================================================

        XplElement root = new XplElement("html");
        this.document = new XplDocument(root);

        if (this.interpreter != null && this.interpreter.globals != null) {
            this.interpreter.globals.defineConst("document", this.document);
            this.interpreter.globals.defineConst("ui", this.document);
        }

        // canal de comunicação entre o JavaFx, o DOM e o Interpretador para a UI.
        com.dic.xsuper.lang.ui.event.eventbus.UiEventBusSubscriber.register(this);
    }

    public void processPartialHtmlUpdate(String targetIdOrUid, String htmlContent) {
        // Tenta achar por UID interno (seguro), senão faz fallback para ID público (para retrocompatibilidade)
        XplElement liveTarget = document.getElementByInternalUid(targetIdOrUid);
        if (liveTarget == null) {
            liveTarget = document.getElementById(targetIdOrUid);
        }

        if (liveTarget != null) {
            // 1. Esvazia os filhos antigos e reseta o texto
            liveTarget.clear();

            try {
                // 2. Lexer e Parser do novo HTML
                HtmlLexer lexer = new HtmlLexer(htmlContent);
                HtmlParser parser = new HtmlParser(lexer.scanTokens());
                XplNode parsedRoot = parser.parse();

                // A CURA DOS TEXTOS FANTASMAS: Funde os nós #text no textContent das tags!
                purifyTree(parsedRoot);

                // 3. Hidrata e anexa à Árvore Viva
                for (XplNode childVirtual : parsedRoot.children) {
                    hydrateHeadlessDom(childVirtual);
                    if (childVirtual.liveElement != null) {
                        liveTarget.appendChild(childVirtual.liveElement);
                    }
                }
            } catch (Exception e) {
                System.err.println("Erro de Parse no innerHTML: " + e.getMessage());
                return;
            }

            // 4. SINCRONIZAR! Recria a árvore virtual a partir da viva (inteira)
            this.activeDom = XplNode.fromXplElement(this.document.documentElement);

            // 4.5 RESGATE: Transfere eventos e componentes perdidos da Árvore Viva
            syncLiveToVirtual(this.activeDom, this.document.documentElement);

            // 5. APLICA A CASCATA CSS (O Segredo que faltava para os botões aparecerem!)
            applyStylesToActiveDom();
            mediaListener.applyActiveStylesToVirtualDom(this.activeDom);
            serializeComputedStyles(this.activeDom);

            // 6. HIDRATAR A ÁRVORE VIVA COM O NOVO CSS
            syncVirtualToLive(this.activeDom, this.document.documentElement);

            // 7. Regista os novos elementos
            document.clearIndex();
            document.registerElement(this.document.documentElement);

            // 7. Extrai a 'Div' virtual com os estilos já injetados e envia ao JavaFX
            XplNode virtualTarget = findVirtualNodeByUid(this.activeDom, liveTarget._internalUid);
            if (virtualTarget == null) {
                virtualTarget = findVirtualNodeById(this.activeDom, liveTarget.getId());
            }

            if (rendererBridge != null && virtualTarget != null) {
                // O Renderer JavaFX usa SEMPRE a matrícula secreta interna
                rendererBridge.rebuildFullView(liveTarget._internalUid, virtualTarget);
            }
        }
    }

    private void syncLiveToVirtual(XplNode vNode, XplElement lNode) {
        if (vNode == null || lNode == null) return;

        // ⭐ RESGATE IMACULADO: A Árvore Virtual recupera tudo o que o "fromXplElement" esqueceu!
        vNode._internalUid = lNode._internalUid;
        vNode.hostComponent = lNode.hostComponent;
        vNode.events.putAll(lNode.inlineEvents); // Devolve o (click)="logout();" à vida!
        vNode.liveElement = lNode;

        for (int i = 0; i < vNode.children.size() && i < lNode.getChildren().size(); i++) {
            syncLiveToVirtual(vNode.children.get(i), lNode.getChildren().get(i));
        }
    }

    // --- Helpers Obrigatórios ---
    private XplNode findVirtualNodeByUid(XplNode root, String uid) {
        if (root == null || uid == null) return null;
        if (uid.equals(root._internalUid)) return root;
        for (XplNode child : root.children) {
            XplNode found = findVirtualNodeByUid(child, uid);
            if (found != null) return found;
        }
        return null;
    }

    private XplNode findVirtualNodeById(XplNode root, String id) {
        if (root == null || id == null) return null;
        if (id.equals(root.id)) return root;
        for (XplNode child : root.children) {
            XplNode found = findVirtualNodeById(child, id);
            if (found != null) return found;
        }
        return null;
    }

    // Em SuperUiEngine.java

    private void syncVirtualToLive(XplNode vNode, XplElement lNode) {
        if (vNode == null || lNode == null) return;

        vNode._internalUid = lNode._internalUid;

        // ✂️ APAGADA A GRAVAÇÃO DO STYLE!
        // A Árvore Viva agora mantém-se pura, apenas com o estilo inline original.

        vNode.liveElement = lNode;

        for (int i = 0; i < vNode.children.size() && i < lNode.getChildren().size(); i++) {
            syncVirtualToLive(vNode.children.get(i), lNode.getChildren().get(i));
        }
    }

    // ─── Ponto de entrada: Carregar Estilos (CSS) ───────────────────────────
    public void loadStyles(String... styles) {
        System.out.println("[Engine] A carregar novos blocos de estilo...");
        for (String style : styles) {
            if (style != null && !style.trim().isEmpty()) {
                this.loadedStyles.add(style);
            }
        }
    }

    // ─── Processador Interno de Cascata ─────────────────────────────────────
    private void applyStylesToActiveDom() {
        if (this.loadedStyles.isEmpty() || this.activeDom == null) return;

        System.out.println("[Engine] 5. A fundir o CSS e aplicar a Cascata...");

        // 1. Unificar todos os estilos
        StringBuilder combinedCss = new StringBuilder();
        for (String style : this.loadedStyles) {
            combinedCss.append(style).append("\n");
        }

        // 2. Extrair variáveis globais do XPL para o CSS
        // Cria um mapa com as variáveis da linguagem para o CSS resolver @if, @for e interpolações
        Map<String, Object> xplContext = new HashMap<>(interpreter.globals.values);

        // 3. Parser e Evaluator
        XplCssLexer lexer = new XplCssLexer(combinedCss.toString());
        XplNode rawCssAst = new XplCssParser(lexer.scanTokens()).parse();
        XplNode flatCssAst = XplCssEvaluator.evaluate(rawCssAst, xplContext);

        // ⭐ NOVO: Alimentar o JavaFxMediaListener com os dados!
        extractMediaQueries(flatCssAst);

        //extrai as animações
        extractKeyframes(flatCssAst);

        // 4. Extrair Tema Global (:root)
        XplCssResolver resolver = new XplCssResolver();
        resolver.extractRootVariables(flatCssAst);

        // 5. Aplicar o CSS Global e Sobrescrever com o CSS Inline
        XplCssMatcher.applyStyles(this.activeDom, flatCssAst, resolver);
        XplCssMatcher.applyInlineStyles(this.activeDom, resolver);
    }

    public static SuperUiEngine getInstance() {
        return instance;
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
        this.staticRoot = normalizeDocumentTree(this.staticRoot);

        registerNodesInXpl(this.staticRoot);

        System.out.println("[Engine] 4. A iniciar o 1º Ciclo de Renderização...");
        renderCycle();
    }

    // =========================================================================
    // 🧹 NORMALIZADOR DO DOM (Padrão W3C Estrito)
    // =========================================================================
    public XplNode normalizeDocumentTree(XplNode root) {

        // 1. Identificar o nó <html> na raiz
        XplNode htmlNode = null;
        for (XplNode child : root.children) {
            if (child.tag.equalsIgnoreCase("html")) {
                htmlNode = child;
                break;
            }
        }

        // Se não houver <html>, criá‑lo
        if (htmlNode == null) {
            htmlNode = new XplNode("html");
            root.addChild(htmlNode);
        }

        // 2. Procurar <head> e <body> dentro do <html>
        XplNode headNode = null;
        XplNode bodyNode = null;
        for (XplNode child : htmlNode.children) {
            if (child.tag.equalsIgnoreCase("head")) headNode = child;
            else if (child.tag.equalsIgnoreCase("body")) bodyNode = child;
        }

        // 3. Mover nós órfãos que estejam na raiz (fora do <html>) para dentro
        List<XplNode> rootChildrenCopy = new ArrayList<>(root.children);
        for (XplNode child : rootChildrenCopy) {
            if (child == htmlNode) continue;  // não mexe no html

            root.children.remove(child);       // remove da raiz

            if (child.tag.equalsIgnoreCase("head")) {
                if (headNode == null) {
                    headNode = child;
                    htmlNode.children.addFirst(headNode);
                } else {
                    headNode.children.addAll(child.children);
                }
            } else if (child.tag.equalsIgnoreCase("body")) {
                if (bodyNode == null) {
                    bodyNode = child;
                    htmlNode.children.add(bodyNode);
                } else {
                    bodyNode.children.addAll(child.children);
                }
            } else {
                // elemento comum → coloca no <body>
                if (bodyNode == null) {
                    bodyNode = new XplNode("body");
                    htmlNode.children.add(bodyNode);
                }
                bodyNode.addChild(child);
            }
        }

        if (headNode == null) htmlNode.children.addFirst(new XplNode("head"));
        if (bodyNode == null) htmlNode.children.add(new XplNode("body"));

        // ⭐ A MAGIA ACONTECE AQUI: Purificar e hidratar a árvore final!
        purifyTree(root);

        return root;
    }

    // =========================================================================
    // 🧬 PURIFICADOR E SINCRONIZADOR DIFÁSICO DE ÁRVORE
    // =========================================================================
    private void purifyTree(XplNode node) {
        if (node == null) return;

        // 1. RESOLUÇÃO CSS: Converter string bruta para o mapa 'style'
        if (node.attributes.containsKey("style")) {
            String rawStyle = node.attributes.get("style").toString();
            String[] declarations = rawStyle.split(";");
            for (String dec : declarations) {
                if (!dec.trim().isEmpty()) {
                    String[] kv = dec.split(":", 2);
                    if (kv.length == 2) {
                        node.style.put(kv[0].trim().toLowerCase(), kv[1].trim());
                    }
                }
            }
        }

        // 2. SINCRONIZAÇÃO DE TEXTO (Mantendo os nós #text vivos!)
        if (node.children != null && !node.children.isEmpty()) {
            StringBuilder combinedText = new StringBuilder();

            for (XplNode child : node.children) {
                if ("#text".equalsIgnoreCase(child.tag)) {
                    // Apanha o texto avaliado (ex: após injectar variáveis) mas NÃO apaga o nó!
                    if (child.textContent != null) {
                        combinedText.append(child.textContent).append(" ");
                    }
                } else {
                    // Continua a purificar as tags normais
                    purifyTree(child);
                }
            }

            // Injecta a soma dos textos no textContent do pai para o JavaFX ler rápido
            String aggregatedText = combinedText.toString().trim();
            if (!aggregatedText.isEmpty()) {
                node.textContent = aggregatedText;
            }

        } else if (node.textContent != null && !node.textContent.trim().isEmpty()) {

            // 3. AUTO-PREENCHIMENTO: Se o nó tem textContent mas perdeu os filhos #text
            node.textContent = node.textContent.trim();

            // Garante que o ecossistema W3C tem o seu filho #text correspondente!
            if (!"#text".equalsIgnoreCase(node.tag)) {
                XplNode textNode = new XplNode("#text");
                textNode.textContent = node.textContent;
                node.addChild(textNode);
            }
        }
    }

    /**
     * Obtém a classe de um componente registado.
     */
    public XplClass getComponent(String tagName) {
        return componentRegistry.get(tagName);
    }

    /**
     * Verifica se uma tag é um componente registado.
     */
    public boolean isComponent(String tagName) {
        return componentRegistry.containsKey(tagName);
    }


    public XplKeyframeAnimation getKeyframe(String name) {
        return keyframesRegistry.get(name);
    }

    // 2. O Extractor de Keyframes (Adiciona perto do extractMediaQueries)
    private void extractKeyframes(XplNode flatCssAst) {
        if (flatCssAst == null || flatCssAst.children == null) return;

        Iterator<XplNode> iterator = flatCssAst.children.iterator();
        while (iterator.hasNext()) {
            XplNode node = iterator.next();

            // Se encontrar um bloco @keyframes
            if ("@keyframes".equalsIgnoreCase(node.tag) || "keyframes".equalsIgnoreCase(node.tag)) {
                String animName = node.attributes.getOrDefault("name", "").toString();
                XplKeyframeAnimation animation = new XplKeyframeAnimation(animName);

                // Varre os frames (ex: "0%", "100%", "from", "to")
                for (XplNode frameNode : node.children) {
                    if ("frame".equalsIgnoreCase(frameNode.tag) || "rule".equalsIgnoreCase(frameNode.tag)) {
                        String position = frameNode.attributes.getOrDefault("selector", "0%").toString();
                        XplKeyframe keyframe = new XplKeyframe(position);

                        // Varre as propriedades de CSS do frame (opacity, transform, etc.)
                        for (XplNode propNode : frameNode.children) {
                            String propName = propNode.attributes.getOrDefault("name", "").toString();
                            String propValue = extractDeepValue(propNode);
                            keyframe.addStyle(propName, propValue);
                        }
                        animation.addKeyframe(keyframe);
                    }
                }

                // Guarda no registo global e APAGA da árvore base para não poluir o CSS
                keyframesRegistry.put(animName, animation);
                System.out.println("[Engine] 🎬 Keyframe registado: " + animName);
                iterator.remove();
            }
        }
    }

    // =========================================================================
    // 📱 EXTRACTOR DE MEDIA QUERIES
    // =========================================================================
    private void extractMediaQueries(XplNode flatCssAst) {
        if (flatCssAst == null || flatCssAst.children == null) return;
        // Limpa media queries antigas (evita duplicação em múltiplos renderCycles)
        // Se ainda não tens o método clear() no JavaFxMediaListener, podes criar lá!
        // this.mediaListener.clear();
        for (XplNode node : flatCssAst.children) {
            if ("@media".equalsIgnoreCase(node.tag) || "media_block".equalsIgnoreCase(node.tag)) {

                String condition = node.attributes.getOrDefault("condition", "").toString();
                if (condition.isEmpty() && node.attributes.containsKey("selector")) {
                    condition = node.attributes.get("selector").toString();
                }

                Map<String, Map<String, String>> rules = new HashMap<>();

                for (XplNode ruleNode : node.children) {
                    if ("rule".equalsIgnoreCase(ruleNode.tag)) {
                        String selector = ruleNode.attributes.getOrDefault("selector", "").toString();
                        Map<String, String> properties = new HashMap<>();

                        for (XplNode propNode : ruleNode.children) {
                            if ("property".equalsIgnoreCase(propNode.tag) || "variable".equalsIgnoreCase(propNode.tag)) {
                                String propName = propNode.attributes.getOrDefault("name", "").toString();

                                // A MAGIA ACONTECE AQUI: Extracção invencível!
                                String propValue = extractDeepValue(propNode);

                                properties.put(propName, propValue);
                            }
                        }
                        rules.put(selector, properties);
                    }
                }

                XplMediaNode mediaNode = new XplMediaNode(condition, rules);
                this.mediaListener.addMediaNode(mediaNode);

                System.out.println("[Engine] 📡 Media Query registada: " + condition + " -> " + rules);
            }
        }
    }


    // 🧲 EXTRACTOR PROFUNDO (Garante que nenhum valor de CSS escapa)
    private String extractDeepValue(XplNode node) {
        if (node == null) return "";

        // 1. Tenta encontrar nos atributos directos
        if (node.attributes.containsKey("value")) return node.attributes.get("value").toString().trim();
        if (node.attributes.containsKey("data")) return node.attributes.get("data").toString().trim();

        // 2. Tenta encontrar no texto do nó
        if (node.textContent != null && !node.textContent.trim().isEmpty()) return node.textContent.trim();

        // 3. Se estiver aninhado em nós filhos (ex: <property><value>15px</value></property>)
        StringBuilder sb = new StringBuilder();
        if (node.children != null) {
            for (XplNode child : node.children) {
                String childValue = extractDeepValue(child);
                if (!childValue.isEmpty()) {
                    sb.append(childValue).append(" ");
                }
            }
        }
        return sb.toString().trim();
    }


    // ─── Ciclo de Reactividade (O Loop da Magia) ─────────────────────────────
    /**
     * Recalcula os @if e @for, hidrata os nós com ID, e renderiza a UI.
     * Deve ser chamado sempre que uma variável XPL relevante mudar.
     */
    public void renderCycle() {
        if (staticRoot == null) return;

        // 1. Guardar estado actual
        Map<String, Object> currentValues = new HashMap<>();
        collectCurrentValues(this.activeDom, currentValues);

        // 2. Resolve @if, @for na Árvore Virtual
        this.activeDom = evaluator.evaluateTree(staticRoot);

        // 3. Normaliza (<html>, <head>, <body>)
        normalizeDocumentTree(this.activeDom);

        // 4. Restaurar valores guardados
        restoreValues(this.activeDom, currentValues);

        // 5. Aplica Cascata CSS
        applyStylesToActiveDom();

        // 6. Aplica media queries activas
        mediaListener.applyActiveStylesToVirtualDom(this.activeDom);

        // 7. Serializa estilos
        serializeComputedStyles(this.activeDom);

        // 8. LIMPEZA TOTAL (Evita fantasmas no mapa de IDs)
        this.document.documentElement = null;
        this.document.head = null;
        this.document.body = null;
        this.document.clearIndex();

        // 9. HIDRATAÇÃO (Constrói a verdadeira Árvore Viva)
        hydrateHeadlessDom(this.activeDom);

        // 10. CONECTA A ÁRVORE VIVA AO DOCUMENTO (Acaba com o Split-Brain!)
        this.document.documentElement = this.activeDom.liveElement;
        if (this.document.documentElement != null) {
            this.document.head = this.document.documentElement.querySelector("head");
            this.document.body = this.document.documentElement.querySelector("body");
            this.document.registerElement(this.document.documentElement);
        }

        // 11. Renderização gráfica
        if (rendererBridge != null) {
            rendererBridge.renderView(this.activeDom);
        }
    }

    // ─── Preservação de estado da UI ───────────────────────────────
    /**
     * Percorre a árvore activa e guarda os valores dos atributos importantes
     * de nós que tenham ID.
     * @param node Nó actual
     * @param values Mapa onde guardar (key = id, value = mapa de atributos)
     */
    private void collectCurrentValues(XplNode node, Map<String, Object> values) {
        if (node == null) return;

        // Se o nó tem ID, guardamos os atributos que nos interessam
        if (node.id != null && !node.id.isEmpty()) {
            Map<String, Object> attrs = new HashMap<>();
            // Guardar atributos que são afectados por inputs
            if (node.attributes.containsKey("value")) {
                attrs.put("value", node.attributes.get("value"));
            }
            if (node.attributes.containsKey("checked")) {
                attrs.put("checked", node.attributes.get("checked"));
            }
            if (node.attributes.containsKey("selected")) {
                attrs.put("selected", node.attributes.get("selected"));
            }
            if (node.textContent != null && !node.textContent.isEmpty()) {
                attrs.put("textContent", node.textContent);
            }
            values.put(node.id, attrs);
        }

        // Recursão para os filhos
        for (XplNode child : node.children) {
            collectCurrentValues(child, values);
        }
    }

    /**
     * Restaura os valores guardados nos nós recriados que tenham ID.
     * @param node Nó actual (da nova árvore)
     * @param values Mapa com os valores guardados anteriormente
     */
    private void restoreValues(XplNode node, Map<String, Object> values) {
        if (node == null) return;

        if (node.id != null && !node.id.isEmpty()) {
            Object stored = values.get(node.id);
            if (stored instanceof Map<?, ?> attrs) {
                // Restaurar atributos
                for (Map.Entry<?, ?> entry : ((Map<String, Object>) attrs).entrySet()) {
                    String key = (String) entry.getKey();
                    Object val = entry.getValue();
                    node.attributes.put(key, val);
                    // Se for 'value', também actualiza o liveElement se existir
                    if ("value".equals(key) && node.liveElement != null) {
                        node.liveElement.setAttributeSilently(key, val);
                    }
                    // Se for 'textContent', actualiza o texto
                    if ("textContent".equals(key)) {
                        node.textContent = (String) val;
                    }
                }
                // Notificar a bridge para actualizar o nó JavaFX (se já existir)
                if (rendererBridge != null && node.liveElement != null) {
                    // Actualiza propriedades específicas no JavaFX
                    rendererBridge.updateProperty(node.id, "value", node.attributes.get("value"));
                    rendererBridge.updateProperty(node.id, "checked", node.attributes.get("checked"));
                    rendererBridge.updateProperty(node.id, "style", node.attributes.get("style"));
                    rendererBridge.updateProperty(node.id, "textContent", node.textContent);
                }
            }
        }

        // Recursão para os filhos
        for (XplNode child : node.children) {
            restoreValues(child, values);
        }
    }

    // =========================================================================
    // 🎨 SERIALIZADOR DE ESTILOS COMPUTADOS
    // =========================================================================
    private void serializeComputedStyles(XplNode node) {
        if (node == null) return;

        if (!node.style.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : node.style.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("; ");
            }
            node.attributes.put("style", sb.toString().trim());
        }

        for (XplNode child : node.children) {
            serializeComputedStyles(child);
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
        //renderCycle();
    }

    public XplUiBridge getRendererBridge() {
        return rendererBridge;
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

    // ─── Sistema Nervoso Central (Reactividade Cirúrgica) ────────────────────
    /**
     * Invocado pelo UiElement (Headless DOM) sempre que o código XPL altera um atributo.
     * Ex: O XPL executou `botao.setAttribute("disabled", true)`.

    public void notifyStateChanged(String id, String name, Object value) {
        System.out.println("[Engine] ⚡ Mutação detectada no ID '" + id + "': [" + name + "] = " + value);

        // 1. Actualiza a Árvore Virtual Limpa (activeDom) para manter a coerência da memória
        syncActiveDomState(activeDom, id, name, value);

        // 2. Avisar a Interface Gráfica (SE ELA EXISTIR!)
        if (rendererBridge != null) {
            rendererBridge.updateProperty(id, name, value);
        }
    }*/

    // ─── Sincronização da Árvore Virtual ────────────────────────────────────
    private void syncActiveDomState(XplNode node, String targetId, String attrName, Object newValue) {
        if (node == null) return;
        if (targetId != null && targetId.equals(node.id)) {
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

    // ─── Hidratação: Criar os Objectos Vivos (XplElement) ────────────────────
    private void hydrateHeadlessDom(XplNode node) {
        if (node == null) return;

        // ⭐ CORRECÇÃO: TODOS os nós precisam de existir no DOM Vivo (com ou sem ID)
        XplElement element = new XplElement(node.tag);
        // ⭐ A PASSAGEM DE TESTEMUNHO! O DOM Vivo herda a matrícula da Planta Virtual
        element._internalUid = node._internalUid;
        element.hostComponent = node.hostComponent;

        // Copia atributos silenciosamente (Incluindo os estilos compilados)
        for (Map.Entry<String, Object> entry : node.attributes.entrySet()) {
            element.setAttributeSilently(entry.getKey(), entry.getValue());
        }

        element.isSyncing = false;
        element.inlineEvents.putAll(node.events);

        // Se tiver ID ou Classe, actualiza propriedades de atalho
        if (node.id != null) element.setId(node.id);
        if (node.className != null) element.setClassName(node.className);
        if (node.textContent != null) element.textContent = node.textContent;

        // Regista no documento
        document.registerElement(element);
        node.liveElement = element;

        // Recursão
        for (XplNode child : node.children) {
            hydrateHeadlessDom(child);
            if (child.liveElement != null) {
                element.appendChild(child.liveElement);
            }
        }
    }

    // ─── Registar nós na tabela de símbolos do XPL ──────────────────────────
    private void registerNodesInXpl(XplNode node) {
        if (node.id != null && !node.id.isEmpty()) {
            interpreter.environment.defineLet(node.id, node.liveElement);
        }
        for (XplNode child : node.children) {
            registerNodesInXpl(child);
        }
    }

    public void setXplModel(String tagName, Object tagElement){
        this.interpreter.registry_model.put(tagName,((XplClass)tagElement).model);
        interpreter.globals.defineConst(tagName, tagElement);
        componentRegistry.put(tagName, (XplClass)tagElement);
    }

    /**
     * O programador chamou isto do XPL!
     * Agora sim, acordamos o JavaFX, criamos a janela e renderizamos.
     */
    public void showWindow(String title, double width, double height) {
        if (mainWindow == null) {
            mainWindow = new MainWindow(this);
        }
        mainWindow.showWindow(title, width, height);
    }

    // ─── Acesso ao Documento Global ──────────────────────────────────────────
    public XplDocument getDocument() {
        return this.document;
    }

    // ─── Métodos utilitários para o XPL ─────────────────────────────────────
    @Override
    public void invokeMethod() {
        //Métodos de sicronização para a acessar o nosso objecto
        SuperUiEngineUtils.buildMethods(this);
    }

    @Override
    public Object getProperty(String propertyName) {
        return switch (propertyName) {
            case "document" -> document;
            case "version" -> "1.0.0";
            case "name" -> "SuperUiEngine";
            default -> null;
        };
    }

    public static XPLModel buildNativeModel() {
        if (nativeModel == null) SuperUiEngineUtils.buildNativeModel();
        return nativeModel;
    }

    public void setRendererBridge(JavaFxRenderer rendererBridge) {
        this.rendererBridge = rendererBridge;
    }

    public JavaFxMediaListener getMediaListener() {
        return mediaListener;
    }

    public void executeInlineScript(String scriptCallback, XplEvent event) {
        // Instancia o executor isolado e delega a execução do script XPL
        new com.dic.xsuper.lang.ui.event.eventbus.InlineScriptExecutor(this.interpreter, this)
                .execute(scriptCallback, event);
    }
}