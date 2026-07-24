package com.dic.xsuper.lang.ui.document;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XplClass;
import com.dic.xsuper.lang.poo.XplInstance;
import com.dic.xsuper.lang.ui.SuperUiEngine;
import com.dic.xsuper.lang.ui.html.HtmlLexer;
import com.dic.xsuper.lang.ui.html.HtmlParser;
import com.dic.xsuper.lang.ui.html.HtmlTagUtils;
import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.reactivity.XplReactiveState;

import java.util.*;

public class DomEvaluator {
    private final Interpreter interpreter; // O cérebro da tua linguagem!
    private final SuperUiEngine engine;
    // ⭐ NOVO: O Map Reativo que controla os Signals
    private final XplReactiveState reactiveState;
    public DomEvaluator(Interpreter interpreter, SuperUiEngine engine, XplReactiveState reactiveState) {
        this.interpreter = interpreter;
        this.engine = engine;
        this.reactiveState = reactiveState;
    }

    /**
     * Pega no nó raiz (blueprint) e devolve uma árvore resolvida e "limpa" de lógica.
     */
    public XplNode evaluateTree(XplNode rootStatic) {
        XplNode rootDynamic = new XplNode(rootStatic.tag);

        for (XplNode child : rootStatic.children) {
            rootDynamic.children.addAll(evaluateNode(child));
        }
        return rootDynamic;
    }

    /**
     * O orquestrador que decide como avaliar cada tipo de nó.
     * Retorna uma Lista porque um @for pode gerar múltiplos nós irmãos.
     */
    private List<XplNode> evaluateNode(XplNode node) {
        List<XplNode> result = new ArrayList<>();

        switch (node.tag) {
            case "@if":
                result.addAll(evaluateIfBlock(node));
                break;
            case "@for":
                result.addAll(evaluateForBlock(node));
                break;
            case "@switch":
                result.addAll(evaluateSwitchBlock(node));
                break;
            case "@component": {
                result.addAll(evaluateComponent(node));
                break;
            }
            case "#text":
            case "text":
                XplNode textNode = cloneNode(node);
                textNode.rawTemplate = node.textContent;
                textNode.textContent = resolveTextBindings(node.textContent, textNode);
                result.add(textNode);
                break;

            default:
                XplNode dynamicElement = cloneNode(node);

                // ════════════════════════════════════════════════════════════════
                // 🔥 PASSO 0: Interpolação nas propriedades fixas (Class e ID)
                // ════════════════════════════════════════════════════════════════
                if (dynamicElement.className != null && dynamicElement.className.contains("{")) {
                    // Usa o avaliador de texto para substituir as variáveis sem apagar o texto à volta (ex: "btn {tipo}")
                    dynamicElement.className = resolveAttributeBindings(dynamicElement.className, dynamicElement);
                }

                if (dynamicElement.id != null && dynamicElement.id.contains("{")) {
                    dynamicElement.id = resolveAttributeBindings(dynamicElement.id, dynamicElement);
                }

                // ════════════════════════════════════════════════════════════════
                // 🔥 PASSO 1: Interpolação em atributos normais (PRESERVA OBJETOS!)
                // ════════════════════════════════════════════════════════════════
                for (Map.Entry<String, Object> attrEntry : node.attributes.entrySet()) {
                    String key = attrEntry.getKey();
                    Object value = attrEntry.getValue();

                    if (value instanceof String strValue && strValue.contains("{")) {
                        // ⭐ A CURA: Usamos o método que preserva o tipo real (List/Map)
                        // em vez de forçar a conversão para String!
                        Object resolved = resolveAttributeValue(strValue, dynamicElement);
                        // Se não for nulo, guarda o objeto real. Se for nulo, guarda vazio.
                        dynamicElement.setAttribute(key, resolved != null ? resolved : "");
                    }
                }

                // ════════════════════════════════════════════════════════════════
                // 🔥 PASSO 2: Interpolação no mapa `style` (propriedades individuais)
                // ════════════════════════════════════════════════════════════════
                for (Map.Entry<String, String> styleEntry : dynamicElement.style.entrySet()) {
                    String prop = styleEntry.getKey();
                    String val = styleEntry.getValue();
                    if (val != null && val.contains("{")) {
                        // Estilos CSS são SEMPRE strings, logo aqui usamos o avaliador de texto normal
                        String resolved = resolveAttributeBindings(val, dynamicElement);
                        dynamicElement.style.put(prop, resolved != null ? resolved : "");
                    }
                }

                // ════════════════════════════════════════════════════════════════
                // 🔥 PASSO 3: Sincronizar o mapa `style` com o atributo "style"
                // ════════════════════════════════════════════════════════════════
                if (!dynamicElement.style.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, String> e : dynamicElement.style.entrySet()) {
                        sb.append(e.getKey()).append(": ").append(e.getValue()).append("; ");
                    }
                    dynamicElement.setAttribute("style", sb.toString().trim());
                }

                // ─── PASSO 4: Bindings com colchetes (já existente) ────────────
                for (String bindKey : node.bindings.keySet()) {
                    String varName = node.bindings.get(bindKey);
                    if (reactiveState != null) {
                        reactiveState.track(varName, dynamicElement);
                    }
                    Object value = evaluateExpressionXPL(varName);

                    dynamicElement.setAttribute(bindKey, value);
                }

                // ─── PASSO 5: Interpolação em textContent (já existente) ──────
                if (dynamicElement.textContent != null && dynamicElement.textContent.contains("{{")) {
                    dynamicElement.rawTemplate = dynamicElement.textContent;
                    // Usa a função dedicada para Texto Duplo
                    dynamicElement.textContent = resolveTextBindings(dynamicElement.textContent, dynamicElement);
                    if (dynamicElement.textContent == null) dynamicElement.textContent = "";
                }

                // ─── PASSO 6: Avaliar filhos recursivamente ────────────────────
                for (XplNode child : node.children) {
                    dynamicElement.children.addAll(evaluateNode(child));
                }
                result.add(dynamicElement);
                break;
        }
        return result;
    }
    /**
     * Processa um nó componente, instanciando a classe XPL correspondente e
     * substituindo o nó pela árvore renderizada.
     */
    // =========================================================================
    // ⚙️ AVALIADOR DE COMPONENTES
    // =========================================================================


    private List<XplNode> evaluateComponent(XplNode componentNode) {
        // 1. Obter o nome do componente
        String componentName = (String) componentNode.attributes.get("componentName");
        if (componentName == null) {
            throw new RuntimeException("Nó @component sem nome de componente.");
        }

        // 2. Obter a classe XPL do componente (a partir da engine)
        SuperUiEngine engine = this.engine;
        XplClass componentClass = engine.getComponent(componentName);
        if (componentClass == null) {
            throw new RuntimeException("Componente não registado: " + componentName);
        }

        // 3. Preparar as props (PRESERVANDO OBJETOS!)
        Map<String, Object> props = new HashMap<>();
        for (Map.Entry<String, Object> entry : componentNode.attributes.entrySet()) {
            String key = entry.getKey();
            if (key.equals("componentName") || key.equals("props")) continue;

            // ⭐ MAGIA: Resolver mantendo a tipagem real do XPL!
            Object resolvedValue = resolveAttributeValue(entry.getValue().toString(),componentNode);
            props.put(key, resolvedValue);
        }

        // 4. O SEGREDO DA ARIDADE (Inteligência do Motor)
        List<Expr.CallArg> args = new ArrayList<>();
        // Pergunta à classe se o seu construtor (init) pede argumentos
        if (componentClass.arity() > 0) {
            // Se o init pedir o mapa de props, enviamos!
            args.add(new Expr.CallArg(null, new Expr.Literal(props)));
        }

        // 5. Instanciar o componente e Injetar Propriedades
        Object componentInstance;
        try {
            componentInstance = componentClass.call(interpreter, args);

            // ⭐ A MAGIA DA INJEÇÃO W3C ⭐
            if (componentInstance instanceof XplInstance inst) {
                for (Map.Entry<String, Object> entry : props.entrySet()) {
                    inst.set(new Token(TokenType.IDENTIFIER, entry.getKey(), null, 0, 0, null), entry.getValue());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao instanciar componente '" + componentName + "': " + e.getMessage(), e);
        }

        // 6. Chamar o método render() do componente
        try {
            if (componentInstance instanceof XplInstance inst) {

                // Procurar o método render
                Object renderMethod = inst.get(new Token(TokenType.IDENTIFIER, "render", null, 0, 0, null));

                if (renderMethod instanceof XplCallable callable) {
                    Object result = callable.call(interpreter, Collections.emptyList());

                    if (result instanceof String html) {
                        HtmlLexer lexer = new HtmlLexer(html);
                        HtmlParser parser = new HtmlParser(lexer.scanTokens());
                        XplNode templateRoot = parser.parse(); // Retorna o <root> invisível

                        // 1. O "Polícia do DOM"
                        HtmlTagUtils.validateForbiddenTags(Set.of("html", "head", "body"), templateRoot);

                        // ⭐ 2. O "Polícia do SVG" (Evita Shapes soltos que partem o layout do JavaFX)
                        //HtmlTagUtils.validateSvgStructure(templateRoot, false);

                        // ⭐ 2. A MAGIA DA PROJEÇÃO DE CONTEÚDO (SLOTS) ⭐
                        processSlots(templateRoot, componentNode.children);

                        // 3. Desempacotar e Avaliar o <root>!
                        List<XplNode> evaluatedChildren = new ArrayList<>();
                        for (XplNode child : templateRoot.children) {
                            List<XplNode> evaluated = evaluateNode(child);
                            evaluatedChildren.addAll(evaluated);
                            for (XplNode node : evaluated) {
                                bindHostComponent(node, inst);
                            }
                        }
                        return evaluatedChildren;
                    }
                } else {
                    throw new RuntimeException("Componente '" + componentName + "' não tem método 'render'. O XPL precisa do 'pub fun render()'.");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao renderizar componente '" + componentName + "': " + e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    // Carimba recursivamente os nós com o componente que os gerou
    private void bindHostComponent(XplNode node, XplInstance host) {
        node.hostComponent = host;

        for (XplNode child : node.children) {
            bindHostComponent(child, host);
        }
    }

    // =========================================================================
    // 🧪 RESOLVEDOR DE ATRIBUTOS (Preserva Objetos Reais!)
    // =========================================================================
    private Object resolveAttributeValue(String text, XplNode componentNode) {
        if (text == null) return null;

        // ⭐ A CURA: Agora procura por uma única chaveta e corta apenas 1 caractere!
        if (text.startsWith("{") && text.endsWith("}") && !text.startsWith("{{")) {

            // Corta 1 no início (o '{') e 1 no fim (o '}')
            String expr = text.substring(1, text.length() - 1).trim();

            if (reactiveState != null && componentNode != null) {
                reactiveState.track(expr, componentNode);
            }

            // Devolve o Objeto PURO (List, Map, Boolean) direto do Interpretador!
            return evaluateExpressionXPL(expr);
        }

        // Se for string misturada (ex: "id-{index}"), cai no motor de strings que tu viste
        return resolveAttributeBindings(text, componentNode);
    }

    // =====================================================================
    // 🧬 RESOLUÇÃO DO @if / @elseif / @else
    // =====================================================================
    private List<XplNode> evaluateIfBlock(XplNode ifNode) {
        List<XplNode> result = new ArrayList<>();
        String conditionCode = ifNode.attributes.get("condition").toString();

        // ⭐ CORREÇÃO DE ARQUITETURA: Track + Evaluate isolados!
        if (reactiveState != null) {
            reactiveState.track(conditionCode, ifNode);
        }
        Object conditionResult = evaluateExpressionXPL(conditionCode);

        // 1. Testa a condição do @if principal
        boolean isTrue = isTruthy(conditionResult);

        if (isTrue) {
            for (XplNode child : ifNode.children) {
                if (!child.tag.equals("@else") && !child.tag.equals("@elseif")) {
                    result.addAll(evaluateNode(child));
                }
            }
            return result;
        }

        // 2. Se o IF falhou, procura pelos @elseif
        for (XplNode child : ifNode.children) {
            if (child.tag.equals("@elseif")) {
                String elseIfCond = child.attributes.get("condition").toString();

                // Track no elseif também!
                if (reactiveState != null) reactiveState.track(elseIfCond, child);

                if (isTruthy(evaluateExpressionXPL(elseIfCond))) {
                    for (XplNode elseIfChild : child.children) {
                        result.addAll(evaluateNode(elseIfChild));
                    }
                    return result;
                }
            }
        }

        // 3. Procura a rota de fuga (@else)
        for (XplNode child : ifNode.children) {
            if (child.tag.equals("@else")) {
                for (XplNode elseChild : child.children) {
                    result.addAll(evaluateNode(elseChild));
                }
                return result;
            }
        }

        return result;
    }

    private List<XplNode> evaluateForBlock(XplNode forNode) {
        List<XplNode> result = new ArrayList<>();
        String expr = forNode.attributes.get("expression").toString();

        String[] parts = expr.split(" of ");
        if (parts.length != 2) throw new RuntimeException("Expressão @for inválida: " + expr);

        String varName = parts[0].replace("let ", "").trim();
        String listName = parts[1].trim();

        // ⭐ CORREÇÃO DE ARQUITETURA: Track + Evaluate isolados!
        if (reactiveState != null) {
            reactiveState.track(listName, forNode);
        }
        Object listObject = evaluateExpressionXPL(listName);

        List<Object> items = new ArrayList<>();
        if (listObject instanceof Iterable<?> iterable) {
            for (Object o : iterable) items.add(o);
        } else if (listObject instanceof Object[] arr) {
            items.addAll(java.util.Arrays.asList(arr));
        }

        if (!items.isEmpty()) {
            for (Object item : items) {
                interpreter.environment.defineLet(varName, item);

                for (XplNode child : forNode.children) {
                    if (!child.tag.equals("@empty")) {
                        result.addAll(evaluateNode(child));
                    }
                }
            }
        } else {
            for (XplNode child : forNode.children) {
                if (child.tag.equals("@empty")) {
                    for (XplNode emptyChild : child.children) {
                        result.addAll(evaluateNode(emptyChild));
                    }
                }
            }
        }

        return result;
    }

    private List<XplNode> evaluateSwitchBlock(XplNode switchNode) {
        List<XplNode> result = new ArrayList<>();
        Object switchValue = evaluateExpressionXPL(switchNode.attributes.get("condition").toString());
        boolean matched = false;

        for (XplNode child : switchNode.children) {
            if (child.tag.equals("@case")) {
                Object caseValue = evaluateExpressionXPL(child.attributes.get("value").toString());
                if (isEqual(switchValue, caseValue)) {
                    for (XplNode caseChild : child.children) {
                        result.addAll(evaluateNode(caseChild));
                    }
                    matched = true;
                    break; // Sai do switch após o primeiro match
                }
            }
        }

        // Se nenhum @case bateu, executa o @default
        if (!matched) {
            for (XplNode child : switchNode.children) {
                if (child.tag.equals("@default")) {
                    for (XplNode defaultChild : child.children) {
                        result.addAll(evaluateNode(defaultChild));
                    }
                }
            }
        }
        return result;
    }

    // =====================================================================
    // 🛠️ MÉTODOS AUXILIARES E PONTE COM O NÚCLEO
    // =====================================================================

    /**
     * Clona o nó sem os filhos, garantindo que não mutamos a "planta" original.
     */
    private XplNode cloneNode(XplNode original) {
        XplNode clone = new XplNode(original.tag);
        clone.id = original.id;
        clone.className = original.className;
        clone.textContent = original.textContent;
        clone.attributes.putAll(original.attributes);
        clone.events.putAll(original.events);

        // ⭐ A CURA DA AMNÉSIA: O clone herda a identidade e os poderes reativos do original!
        clone._internalUid = original._internalUid;
        clone.bindings.putAll(original.bindings);
        clone.rawTemplate = original.rawTemplate;
        clone.hostComponent = original.hostComponent;

        return clone;
    }

    /**
     * Comunica com o teu Interpretador para resolver strings como "usuario.isLogado()".
     */
    // =====================================================================
    // 🧠 A PONTE QUÂNTICA ENTRE O DOM E O XPL
    // =====================================================================
    public Object evaluateExpressionXPL(String expressao) {
        if (expressao == null || expressao.trim().isEmpty()) return null;

        try {
            // ⭐ A CURA DAS ASPAS SIMPLES:
            // O HTML usa frequentemente aspas simples. Como o XPL Core exige aspas duplas,
            // substituímos silenciosamente antes de entregar ao Interpretador!
            String codigoInjetado = expressao.trim().replace("'", "\"");

            if (!codigoInjetado.endsWith(";")) {
                codigoInjetado += ";";
            }

            com.dic.xsuper.lang.Lexer lexer = new com.dic.xsuper.lang.Lexer(codigoInjetado, "DOM_Binding");
            List<com.dic.xsuper.lang.Token> tokens = lexer.tokenize();

            com.dic.xsuper.lang.Parser parser = new com.dic.xsuper.lang.Parser(tokens);
            List<com.dic.xsuper.lang.Stmt> statements = parser.parse();

            if (!statements.isEmpty()) {
                com.dic.xsuper.lang.Stmt primeiroStmt = statements.getFirst();

                if (primeiroStmt instanceof com.dic.xsuper.lang.Stmt.ExpressionStmt exprStmt) {
                    return interpreter.evaluate(exprStmt.expression);
                } else if (primeiroStmt instanceof com.dic.xsuper.lang.Stmt.Return retStmt) {
                    return interpreter.evaluate(retStmt.value);
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("[DomEvaluator] Erro ao avaliar a expressão '" + expressao + "': " + e.getMessage());
            return null;
        }
    }
    // =========================================================================
    // 🧩 PROCESSADOR DE SLOTS (Content Projection - W3C Web Components)
    // =========================================================================
    private void processSlots(XplNode templateNode, List<XplNode> projectedContent) {
        if (templateNode == null || templateNode.children.isEmpty()) return;

        List<XplNode> newChildren = new ArrayList<>();

        for (XplNode child : templateNode.children) {
            if (child.tag.equalsIgnoreCase("slot")) {
                // É um slot! Vamos procurar o que injetar nele.
                // Se não tiver nome, é o slot "default"
                String slotName = child.attributes.containsKey("name") ? child.attributes.get("name").toString() : "default";
                boolean foundContent = false;

                for (XplNode projectedNode : projectedContent) {
                    // Descobre para que slot este nó quer ir (se não tiver atributo slot, vai para o default)
                    String targetSlot = projectedNode.attributes.containsKey("slot") ? projectedNode.attributes.get("slot").toString() : "default";

                    if (slotName.equals(targetSlot)) {
                        // Clonamos o nó para não poluir a Árvore Estática do Parser!
                        XplNode nodeToInject = cloneNode(projectedNode);

                        // Removemos o atributo 'slot' para não aparecer no HTML final limpo
                        nodeToInject.attributes.remove("slot");

                        newChildren.add(nodeToInject);
                        foundContent = true;
                    }
                }

                // Se o utilizador não passou nada para este slot, usamos o Fallback (conteúdo padrão do slot)
                if (!foundContent) {
                    processSlots(child, projectedContent); // Processa slots aninhados, se houver
                    newChildren.addAll(child.children);
                }
            } else {
                // Não é slot. Continua a busca recursiva no template!
                processSlots(child, projectedContent);
                newChildren.add(child);
            }
        }

        // Atualiza a árvore do template com os novos nós projetados
        templateNode.children = newChildren;
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    // =========================================================================
    // 🧪 MOTORES DE BINDING (ATRIBUTOS { } vs TEXTO {{ }})
    // =========================================================================

    public String resolveTextBindings(String text, XplNode targetNode) {
        if (text == null || !text.contains("{{")) return text;

        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{\\{([^}]+)\\}\\}").matcher(text);
        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            String expr = m.group(1).trim();
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(processExpression(expr, targetNode)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public String resolveAttributeBindings(String text, XplNode targetNode) {
        if (text == null || !text.contains("{")) return text;

        // Regex cega o {{ }} para não haver colisões
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?<!\\{)\\{([^}]+)\\}(?!\\})").matcher(text);
        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            String expr = m.group(1).trim();
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(processExpression(expr, targetNode)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * O Cérebro Unificado: Analisa a string da expressão, atrela os Signals
     * de Reatividade e pergunta ao Interpretador XPL o resultado final.
     */
    private String processExpression(String expr, XplNode targetNode) {
        // ⭐ É UM TERNÁRIO? (Ex: isNotificationVisible ? 'eu.png' : 'ele.png')
        if (expr.contains("?")) {
            String[] parts = expr.split("\\?", 2);
            String condition = parts[0].trim();

            String[] branches = parts[1].split(":", 2);
            String trueBranch = branches[0].trim().replace("\"", "").replace("'", "");
            String falseBranch = branches.length > 1 ? branches[1].trim().replace("\"", "").replace("'", "") : "";

            // 1. Cola os "ouvidos" da reatividade apenas na condição do ternário!
            if (reactiveState != null && targetNode != null) {
                reactiveState.track(condition, targetNode);
            }

            // 2. Avalia a condição no XPL
            Object val = evaluateExpressionXPL(condition);
            return isTruthy(val) ? trueBranch : falseBranch;
        }

        // ⭐ É UMA VARIÁVEL SIMPLES? (Ex: index)
        else {
            if (reactiveState != null && targetNode != null) {
                reactiveState.track(expr, targetNode);
            }
            Object val = evaluateExpressionXPL(expr);
            return val != null ? String.valueOf(val) : "";
        }
    }
}