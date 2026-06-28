package com.dic.xsuper.lang.ui.document;

import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.ui.XplNode;

import java.util.ArrayList;
import java.util.List;

public class DomEvaluator {
    private final Interpreter interpreter; // O cérebro da tua linguagem!

    public DomEvaluator(Interpreter interpreter) {
        this.interpreter = interpreter;
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
            case "@match":
                // Ficará idêntico à lógica do switch, mas extraindo o atributo 'pattern'
                break;
            case "text":
                result.add(cloneNode(node)); // Nós de texto passam direto
                break;
            default:
                // Tag HTML normal (div, button, input)
                XplNode dynamicElement = cloneNode(node);

                // 1. Resolver Bindings Dinâmicos (ex: [disabled]="isCarregando")
                for (String bindKey : node.bindings.keySet()) {
                    String varName = node.bindings.get(bindKey);
                    Object value = evaluateExpressionXPL(varName);
                    dynamicElement.attributes.put(bindKey, String.valueOf(value));
                }

                // 2. Avaliar filhos recursivamente
                for (XplNode child : node.children) {
                    dynamicElement.children.addAll(evaluateNode(child));
                }
                result.add(dynamicElement);
                break;
        }
        return result;
    }

    // =====================================================================
    // 🧬 RESOLUÇÃO DOS BLOCOS ESTRUTURAIS
    // =====================================================================

    private List<XplNode> evaluateIfBlock(XplNode ifNode) {
        List<XplNode> result = new ArrayList<>();
        String conditionCode = ifNode.attributes.get("condition").toString();

        // Pergunta à memória se a condição é verdadeira
        boolean isTrue = isTruthy(evaluateExpressionXPL(conditionCode));

        if (isTrue) {
            for (XplNode child : ifNode.children) {
                result.addAll(evaluateNode(child));
            }
        }
        return result;
    }

    private List<XplNode> evaluateForBlock(XplNode forNode) {
        List<XplNode> result = new ArrayList<>();
        String expr = forNode.attributes.get("expression").toString(); // Ex: "let item of lista"

        // Parse simples da expressão "let X of Y"
        String[] parts = expr.split(" of ");
        if (parts.length != 2) throw new RuntimeException("Expressão @for inválida: " + expr);

        String varName = parts[0].replace("let ", "").trim();
        String listName = parts[1].trim();

        Object listObject = evaluateExpressionXPL(listName);

        // Verificamos se a lista existe e é iterável
        if (listObject instanceof Iterable<?> iterable) {
            boolean hasItems = false;

            for (Object item : iterable) {
                hasItems = true;
                // ⭐ MAGIA DE ESCOPO: Injetamos a variável temporária no ambiente
                interpreter.environment.defineVar(varName, item);

                for (XplNode child : forNode.children) {
                    if (!child.tag.equals("@empty")) {
                        result.addAll(evaluateNode(child));
                    }
                }
            }

            // Se a lista estiver vazia, procuramos o bloco @empty
            if (!hasItems) {
                for (XplNode child : forNode.children) {
                    if (child.tag.equals("@empty")) {
                        for (XplNode emptyChild : child.children) {
                            result.addAll(evaluateNode(emptyChild));
                        }
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
        return clone;
    }

    /**
     * Comunica com o teu Interpretador para resolver strings como "usuario.isLogado()".
     */
    private Object evaluateExpressionXPL(String expressao) {
        // Aqui tu delegarás para o teu Scanner e Parser da linguagem principal!
        // Ex: Expr expr = new Parser(new Scanner(expressao).scanTokens()).parseExpression();
        // return interpreter.evaluate(expr);

        // Placeholder para manter a classe compilável por enquanto
        return null;
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
}