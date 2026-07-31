package com.dic.xsuper.dom.css;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.dom.reactivity.XplExpressionEvaluator;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * Motor que funde as variáveis da linguagem XPL com a árvore CSS bruta.
 * Resolve diretivas (@if, @for) e interpolação ({{var}}), gerando um CSS AST "plano".
 */
public class XplCssEvaluator {

    /**
     * Avalia a árvore CSS com o estado atual da linguagem.
     * @param rawRoot A raiz "css" bruta gerada pelo Parser.
     * @param xplContext As variáveis globais/locais da tua linguagem (ex: valor=12, isLink=true).
     * @return Uma nova árvore XplNode contendo apenas CSS puro resolvido.
     */
    public static XplNode evaluate(XplNode rawRoot, Map<String, Object> xplContext) {
        XplNode flatRoot = new XplNode("css");
        evaluateChildren(rawRoot.children, flatRoot, xplContext);
        return flatRoot;
    }

    private static void evaluateChildren(List<XplNode> children, XplNode flatParent, Map<String, Object> context) {
        for (XplNode child : children) {
            switch (child.tag) {
                case "@if" -> processIf(child, flatParent, context);
                case "@for" -> processFor(child, flatParent, context);
                case "@switch" -> processSwitch(child, flatParent, context);
                case "rule" -> processRule(child, flatParent, context);
                case "property" -> processProperty(child, flatParent, context);
                case "variable" -> processVariable(child, flatParent, context);
                case "@media", "@keyframes" -> processAtRulePassThrough(child, flatParent, context);
                default -> flatParent.addChild(child.cloneNode(true)); // Mantém outros nós (ex: spread) intactos
            }
        }
    }

    // ─── 1. RESOLUÇÃO DE DIRETIVAS (A Lógica da Linguagem) ───────────────

    private static void processIf(XplNode ifNode, XplNode flatParent, Map<String, Object> context) {
        String condition = ifNode.attributes.get("condition").toString();
        boolean isTrue = evaluateXplCondition(condition, context);

        if (isTrue) {
            // ⭐ Avalia apenas os filhos do bloco principal (excluindo @elseif e @else)
            for (XplNode child : ifNode.children) {
                if (child.tag.equals("@elseif") || child.tag.equals("@else")) continue;
                evaluateSingleNode(child, flatParent, context);
            }
        } else {
            // Procura e avalia o primeiro ramo alternativo verdadeiro
            for (XplNode child : ifNode.children) {
                if (child.tag.equals("@elseif")) {
                    if (evaluateXplCondition(child.attributes.get("condition").toString(), context)) {
                        evaluateChildren(child.children, flatParent, context);
                        break;
                    }
                } else if (child.tag.equals("@else")) {
                    evaluateChildren(child.children, flatParent, context);
                    break;
                }
            }
        }
    }

    private static void processFor(XplNode forNode, XplNode flatParent, Map<String, Object> context) {
        String expr = forNode.attributes.get("expression").toString(); // "let item of items"
        String[] parts = expr.split(" ");

        if (parts.length >= 4 && parts[2].equals("of")) {
            String loopVar = parts[1];
            String arrayVar = parts[3];
            Object listObj = context.get(arrayVar);

            if (listObj instanceof Iterable<?> list) {
                boolean hasItems = false;
                for (Object item : list) {
                    hasItems = true;
                    Map<String, Object> localScope = new HashMap<>(context);
                    localScope.put(loopVar, item);

                    // ⭐ Executa apenas os filhos do loop que NÃO sejam o @empty
                    for (XplNode child : forNode.children) {
                        if (child.tag.equals("@empty")) continue;
                        evaluateSingleNode(child, flatParent, localScope);
                    }
                }

                // ⭐ Só processa o @empty se a lista estiver totalmente vazia
                if (!hasItems) {
                    for (XplNode child : forNode.children) {
                        if (child.tag.equals("@empty")) {
                            evaluateChildren(child.children, flatParent, context);
                        }
                    }
                }
            }
        }
    }

    // Auxiliar para avaliar um único nó isolado
    private static void evaluateSingleNode(XplNode node, XplNode flatParent, Map<String, Object> context) {
        evaluateChildren(List.of(node), flatParent, context);
    }
    // ─── 2. RESOLUÇÃO DE INTERPOLAÇÃO (HTML/CSS Dinâmico) ────────────────

    private static void processRule(XplNode ruleNode, XplNode flatParent, Map<String, Object> context) {
        XplNode flatRule = new XplNode("rule");
        String selector = ruleNode.attributes.get("selector").toString();

        // Interpola variáveis: ex ".item-{{item}}" vira ".item-1"
        flatRule.attributes.put("selector", interpolate(selector, context));

        // Avalia as propriedades ou sub-regras lá dentro
        evaluateChildren(ruleNode.children, flatRule, context);

        // Só adiciona se a regra ficou com conteúdo (para limpar CSS inútil)
        if (!flatRule.children.isEmpty()) {
            flatParent.addChild(flatRule);
        }
    }

    private static void processProperty(XplNode propNode, XplNode flatParent, Map<String, Object> context) {
        XplNode flatProp = new XplNode("property");
        flatProp.attributes.put("name", propNode.attributes.get("name"));

        // A propriedade pode ter um valor direto (value, literal) ou ter lógica (@if, @switch)
        // Criamos um contentor temporário para deixar a lógica correr
        XplNode tempValContainer = new XplNode("temp");
        evaluateChildren(propNode.children, tempValContainer, context);

        // O resultado da avaliação (ex: o [literal {value=red}]) transita para a flatProp
        flatProp.children.addAll(tempValContainer.children);

        if (!flatProp.children.isEmpty()) {
            flatParent.addChild(flatProp);
        }
    }

    private static void processVariable(XplNode varNode, XplNode flatParent, Map<String, Object> context) {
        // Semelhante à property, mas preservamos a tag "variable" para extração posterior
        XplNode flatVar = new XplNode("variable");
        flatVar.attributes.put("name", varNode.attributes.get("name"));

        XplNode tempContainer = new XplNode("temp");
        evaluateChildren(varNode.children, tempContainer, context);
        flatVar.children.addAll(tempContainer.children);

        flatParent.addChild(flatVar);
    }

    private static void processAtRulePassThrough(XplNode atNode, XplNode flatParent, Map<String, Object> context) {
        // @media e @keyframes transitam estruturalmente, mas o seu conteúdo é avaliado
        XplNode flatAtRule = new XplNode(atNode.tag);
        flatAtRule.attributes.putAll(atNode.attributes);
        evaluateChildren(atNode.children, flatAtRule, context);
        flatParent.addChild(flatAtRule);
    }

    // ─── 3. FERRAMENTAS DA LINGUAGEM ────────────────────────────────────

    private static void processSwitch(XplNode switchNode, XplNode flatParent, Map<String, Object> context) {
        // Extrai e avalia a variável de teste
        String condition = switchNode.attributes.get("condition").toString();
        Object testValue = context.get(condition); // ex: devolve "danger"

        for (XplNode child : switchNode.children) {
            if (child.tag.equals("@case")) {
                String caseVal = child.attributes.get("value").toString().replace("\"", "");
                if (caseVal.equals(String.valueOf(testValue))) {
                    evaluateChildren(child.children, flatParent, context);
                    return; // Entra apenas no case correspondente
                }
            } else if (child.tag.equals("@default")) {
                evaluateChildren(child.children, flatParent, context);
                return;
            }
        }
    }

    /**
     * Interpola strings, substituindo {{chave}} pelo valor do contexto.
     */
    private static String interpolate(String text, Map<String, Object> context) {
        // Usa o avaliador de texto livre porque os seletores CSS usam a sintaxe {{ var }}
        return XplExpressionEvaluator.evaluateText(text, context);
    }

    /**
     * Ponto de ancoragem para o teu interpretador XPL nativo.
     */
    private static boolean evaluateXplCondition(String condition, Map<String, Object> context) {
        // Aqui conectas o Parser da TUA linguagem!
        // Exemplo simplista (mock) apenas para testar a árvore:
        if (condition.contains("valor == 12")) return (int) context.getOrDefault("valor", 0) == 12;
        if (condition.contains("isLink")) return (boolean) context.getOrDefault("isLink", false);
        if (condition.contains("item % 2 == 0")) return (int) context.getOrDefault("item", 1) % 2 == 0;
        return false;
    }
}