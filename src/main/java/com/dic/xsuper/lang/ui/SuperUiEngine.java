package com.dic.xsuper.lang.ui;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.poo.XplClass;
import com.dic.xsuper.lang.poo.XplInstance;
import com.dic.xsuper.lang.ui.document.*;
import com.dic.xsuper.lang.ui.event.XplEvent;
import com.dic.xsuper.lang.ui.html.*;

import java.util.*;

/**
 * A Super Engine: O coração que bombeia a vida entre a Linguagem XPL e o Renderizador Gráfico (JavaFX).
 * <p>
 * Esta classe gerencia o ciclo de vida da UI, a reatividade, a hidratação do DOM,
 * e a comunicação bidirecional entre o XPL e a interface gráfica.
 */
public class SuperUiEngine extends XplInstance implements XplNativeObject {


    public static  XplClass EVENT_CLASS; ;
    // 1. As Plantas (Classes) Nativas Globais
    public static XplClass ELEMENT_CLASS;
    public static XplClass DOCUMENT_CLASS;
    public static XplClass ENGINE_CLASS;


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


    private static XPLModel nativeModel;

    // Mapa que associa o nome da tag personalizada à sua classe XPL
    private final Map<String, XplClass> componentRegistry = new HashMap<>();


    // ─── Construtor ─────────────────────────────────────────────────────────
    public SuperUiEngine(Interpreter interpreter, XplUiBridge rendererBridge) {
        this.interpreter = interpreter;
        this.rendererBridge = rendererBridge;
        this.evaluator = new DomEvaluator(interpreter, this);

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
            this.invokeMethod(); // Injeta os próprios métodos!
            this.interpreter.globals.defineConst("__ui_engine", this);
        }

        // =================================================================
        // ⭐ FASE 3: SÓ AGORA INSTANCIAMOS OS OBJETOS VIVOS (DOM)
        // Agora o 'super(ELEMENT_CLASS)' lá dentro do Java vai encontrar a classe perfeitamente!
        // =================================================================

        XplElement root = new XplElement("html");
        this.document = new XplDocument(root);

        if (this.interpreter != null && this.interpreter.globals != null) {
            this.interpreter.globals.defineConst("document", this.document);
            this.interpreter.globals.defineConst("ui", this.document);
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
        this.staticRoot = normalizeDocumentTree(this.staticRoot);

        registerNodesInXpl(this.staticRoot);

        System.out.println("[Engine] 4. A iniciar o 1º Ciclo de Renderização...");
        renderCycle();
    }


    // =========================================================================
    // 🧹 NORMALIZADOR DO DOM (Padrão W3C Estrito)
    // ⭐ MÉTODO ROBUSTO: Retorna a árvore normalizada para garantir atualização ⭐
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
        //    Usamos uma cópia para não modificar a lista enquanto iteramos
        List<XplNode> rootChildrenCopy = new ArrayList<>(root.children);
        for (XplNode child : rootChildrenCopy) {
            if (child == htmlNode) continue;  // não mexe no html

            root.children.remove(child);       // remove da raiz

            if (child.tag.equalsIgnoreCase("head")) {
                if (headNode == null) {
                    headNode = child;
                    htmlNode.children.addFirst(headNode);
                } else {
                    // funde conteúdos se já existir head
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

        // 4. Garantir que <head> e <body> existam
        if (headNode == null) {
            headNode = new XplNode("head");
            htmlNode.children.addFirst(headNode);
        }
        if (bodyNode == null) {
            bodyNode = new XplNode("body");
            htmlNode.children.add(bodyNode);
        }

        return root;
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



    // ─── Ciclo de Reactividade (O Loop da Magia) ─────────────────────────────

    /**
     * Recalcula os @if e @for, hidrata os nós com ID, e renderiza a UI.
     * Deve ser chamado sempre que uma variável XPL relevante mudar.
     */
    public void renderCycle() {
        if (staticRoot == null) return;

        System.out.println(staticRoot.children.getLast().children);

        // 1. O Evaluator resolve a árvore. (Isto cria uma árvore nova, sem referências antigas)
        this.activeDom = evaluator.evaluateTree(staticRoot);

        // 2. Normalização (Arrumação) na árvore virtual (XplNode)
        // Chamamos isto antes de converter para objetos XplElement
        normalizeDocumentTree(this.activeDom);

        // 3. 💧 Limpeza Total e Hidratação
        // Aqui está o segredo: limpamos o documento antes de hidratar
        this.document.documentElement = null;
        this.document.head = null;
        this.document.body = null;

        // Convertemos a árvore normalizada para Objetos Vivos (XplElement)
        this.document.documentElement = this.activeDom.toXplElement();

        // 4. Conectar os ponteiros rápidos (Sempre a partir do novo documentElement!)
        // Usamos querySelector que, por ser novo, só encontra o que está nesta instância
        this.document.head = this.document.documentElement.querySelector("head");
        this.document.body = this.document.documentElement.querySelector("body");

        // 5. Hidratação final
        hydrateHeadlessDom(this.activeDom);
        registerNodesInXpl(this.activeDom);

        // 6. Enviar para a Ponte Gráfica
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

    // ─── Hidratação: Criar os Objetos Vivos (XplElement) ────────────────────

    private void hydrateHeadlessDom(XplNode node) {
        if (node == null) return;

        // Se o nó tiver um ID, ele ganha vida no mundo XPL
        if (node.id != null && !node.id.isEmpty()) {
            XplElement element = new XplElement(node.tag);
            // ⭐ TRANSFERE A MEMÓRIA DO COMPONENTE!
            element.hostComponent = node.hostComponent;
            // Copia atributos (silenciosamente, para não disparar notificações)
            for (Map.Entry<String, Object> entry : node.attributes.entrySet()) {
                element.setAttribute(entry.getKey(), entry.getValue());
            }


            // ⭐ ADICIONA ESTAS 3 LINHAS (Copiar os eventos inline para a memória) ⭐
            element.inlineEvents.putAll(node.events);


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

    // ─── Acesso ao Documento Global ──────────────────────────────────────────

    public XplDocument getDocument() {
        return this.document;
    }

    // ─── Métodos utilitários para o XPL ─────────────────────────────────────


    @Override
    public void invokeMethod() {
        // ─── MÉTODOS DA ENGINE ──────────────────────────────────────────────

        // loadView(htmlSource)
        this.fields.put("loadView", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String html = intp.evaluate(args.getFirst().expression).toString();
                loadView(html);
                return null;
            }
        });


        // ⭐ O NOVO RENDERIZADOR JSON DE CONSOLA ⭐
        this.fields.put("printDOM", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                Object obj = intp.evaluate(args.getFirst().expression);
                String object = null;
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
                    //System.out.println(mapper.writeValueAsString(obj));
                    object = mapper.writeValueAsString(obj);
                } catch (Exception e) {
                    System.out.println(obj);
                }
                return object;
            }
        });

        // loadView(htmlSource)
        this.fields.put("defineTag", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String tagName = intp.evaluate(args.get(0).expression).toString();
                Object tagElement = intp.evaluate(args.get(1).expression);
                if (HtmlTagUtils.isNativeTag(tagName)) {
                    throw new RuntimeException("Erro de Estrutura: A tag '<" + tagName + ">' não é permitida dentro de um componente customizado.");
                }
                if (!HtmlTagUtils.isValidCustomTagName(tagName)) {
                    throw new RuntimeException(String.format(
                            "Erro de Estrutura: A tag customizada '<%s>' não pode ser usada neste contexto. " +
                                    "Esta secção espera apenas tags nativas da Web (como <div>, <p>, <span>). " +
                                    "Certifique-se de que o componente <%s> está a ser instanciado dentro de um container permitido (como <body>).",
                            tagName, tagName
                    ));
                }
                setXplModel(tagName, tagElement);
                return null;
            }
        });

        // renderCycle()
        this.fields.put("renderCycle", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                renderCycle();
                return null;
            }
        });

        // dispatchEvent(eventName, payload, targetId)
        this.fields.put("dispatchEvent", new XplCallable() {
            @Override public int arity() { return 3; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String eventName = intp.evaluate(args.get(0).expression).toString();
                Object payload = intp.evaluate(args.get(1).expression);
                String targetId = intp.evaluate(args.get(2).expression).toString();
                dispatchEvent(eventName, payload, targetId);
                return null;
            }
        });

        // addEventListener(type, listener) – suporta XplFunction ou XplEventListener
        this.fields.put("addEventListener", new XplCallable() {
            @Override public int arity() { return -1; } // 2 ou 3 argumentos
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                if (args.size() < 2 || args.size() > 3) {
                    throw new IllegalArgumentException("addEventListener espera 2 ou 3 argumentos.");
                }
                String type = intp.evaluate(args.get(0).expression).toString();
                Object listenerObj = intp.evaluate(args.get(1).expression);
                if (listenerObj instanceof XplEventListener) {
                    addEventListener(type, (XplEventListener) listenerObj);
                } else if (listenerObj instanceof XplFunction) {
                    addEventListener(type, (XplFunction) listenerObj);
                } else {
                    throw new IllegalArgumentException("Ouvinte deve ser XplEventListener ou XplFunction.");
                }
                // Opções ignoradas
                return null;
            }
        });

        // removeEventListener(type, listener)
        this.fields.put("removeEventListener", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String type = intp.evaluate(args.get(0).expression).toString();
                XplEventListener listener = (XplEventListener) intp.evaluate(args.get(1).expression);
                // Para remover, precisamos ter uma lista de listeners; faremos uma busca
                List<XplEventListener> list = eventListeners.get(type);
                if (list != null) {
                    list.remove(listener);
                }
                return null;
            }
        });

        // notifyStateChanged(id, name, value) – geralmente chamado internamente, mas pode ser exposto
        this.fields.put("notifyStateChanged", new XplCallable() {
            @Override public int arity() { return 3; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String id = intp.evaluate(args.get(0).expression).toString();
                String name = intp.evaluate(args.get(1).expression).toString();
                Object value = intp.evaluate(args.get(2).expression);
                notifyStateChanged(id, name, value);
                return null;
            }
        });

        // getDocument()
        this.fields.put("getDocument", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return getDocument();
            }
        });

        // ─── PROPRIEDADES COMO MÉTODOS (GETTERS) ────────────────────────────

        // Versão da engine (exemplo)
        this.fields.put("getVersion", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return "1.0.0";
            }
        });

        // Nome da engine
        this.fields.put("getName", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return "SuperUiEngine";
            }
        });
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
        if (nativeModel == null) {
            nativeModel = new XPLModel("SuperUiEngine", null);

            // ─── Campos (propriedades) ──────────────────────────────────────────────
            String[] fieldNames = {"document", "ui"};
            for (String f : fieldNames) {
                Token nameToken = new Token(TokenType.IDENTIFIER, f, null, 0, 0);
                // FieldDecl(Token modifier, boolean isStatic, boolean isFinal, boolean isReadonly, Token name, TypeNode type)
                Stmt.FieldDecl field = new Stmt.FieldDecl(null, false, false, false, nameToken, null);
                nativeModel.addField(field);
            }

            // ─── Métodos ──────────────────────────────────────────────────────────────

            // loadView(html)
            List<Stmt.Param> paramsHTML = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "html", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "loadView", null, 0, 0),
                    paramsHTML,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // renderCycle()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "renderCycle", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // dispatchEvent(eventName, payload, targetId)
            List<Stmt.Param> paramsEvent = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "eventName", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "payload", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "targetId", null, 0, 0), null, null)
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

            // addEventListener(type, listener)
            List<Stmt.Param> paramsListener = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "type", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "listener", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "addEventListener", null, 0, 0),
                    paramsListener,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // removeEventListener(type, listener)
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "removeEventListener", null, 0, 0),
                    paramsListener,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // notifyStateChanged(id, name, value)
            List<Stmt.Param> paramsState = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "id", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "name", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "value", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "notifyStateChanged", null, 0, 0),
                    paramsState,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getDocument()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getDocument", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getVersion()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getVersion", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getName()
            nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getName", null, 0, 0),
                    Collections.emptyList(),
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

}