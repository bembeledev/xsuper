package com.dic.xsuper.dom.css;

import com.dic.xsuper.dom.node.XplNode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor de Seletores CSS de Nível Industrial (Estilo jQuery / W3C).
 * Suporta:
 * - Hierarquia: Espaço (descendente), > (filho direto), + (irmão adjacente), ~ (irmãos seguintes)
 * - Atributos: [attr], [attr="val"], [attr^="val"], [attr$="val"], [attr*="val"]
 * - Pseudo-classes: :first-child, :last-child, :nth-child(even|odd), :not(seletor)
 * - Padrões: tag, .classe, #id, *
 */
public class XplSelectorEngine {

    // Regex para extrair blocos de atributos: [name="value"] ou [checked]
    private static final Pattern ATTR_PATTERN = Pattern.compile("\\[([a-zA-Z0-9_-]+)(?:([*^$]?=)[\"']?([^\"\']*)[\"']?)?\\]");

    // Regex para pseudo-classes com ou sem parênteses: :not(.class) ou :nth-child(even)
    private static final Pattern PSEUDO_PATTERN = Pattern.compile(":([a-zA-Z0-9_-]+)(?:\\(([^)]+)\\))?");

    /**
     * Ponto de Entrada: Verifica se um nó DOM corresponde a um seletor CSS complexo.
     */
    public static boolean matches(XplNode node, String selector) {
        if (node == null || selector == null || selector.trim().isEmpty()) return false;

        // Separa os combinadores, respeitando espaços apenas fora de parênteses/colchetes
        // Uma forma simplificada para o nosso motor (suporta "div > p", "ul li", etc)
        String[] parts = selector.trim().split("\\s+(?![^\\[]*])(?![^(]*\\))");

        // Os browsers lêem os seletores da DIREITA para a ESQUERDA! (Bottom-Up)
        // Ex: "div .card > p" -> Testa se é "p". Se sim, testa se o pai é ".card". Se sim, testa se algum avô é "div".
        int currentIndex = parts.length - 1;

        // 1. O nó atual tem de corresponder à última parte do seletor
        if (!matchCompound(node, parts[currentIndex])) {
            return false;
        }

        // 2. Se houver mais partes (hierarquia), navegamos na árvore para cima/lados
        XplNode currentContext = node;
        currentIndex--;

        while (currentIndex >= 0) {
            String part = parts[currentIndex].trim();

            switch (part) {
                case ">" -> {
                    // Filho direto: O pai exato tem de corresponder à próxima regra
                    currentIndex--;
                    currentContext = currentContext.parent;
                    if (currentContext == null || !matchCompound(currentContext, parts[currentIndex])) return false;
                }
                case "+" -> {
                    // Irmão adjacente: O irmão imediatamente anterior
                    currentIndex--;
                    currentContext = getPreviousSibling(currentContext);
                    if (currentContext == null || !matchCompound(currentContext, parts[currentIndex])) return false;
                }
                case "~" -> {
                    // Irmão geral: Procura para trás nos irmãos até encontrar
                    currentIndex--;
                    String targetSiblingSelector = parts[currentIndex];
                    boolean foundSibling = false;
                    currentContext = getPreviousSibling(currentContext);
                    while (currentContext != null) {
                        if (matchCompound(currentContext, targetSiblingSelector)) {
                            foundSibling = true;
                            break;
                        }
                        currentContext = getPreviousSibling(currentContext);
                    }
                    if (!foundSibling) return false;
                }
                default -> {
                    // Espaço (Descendente): Procura na árvore de pais até ao topo
                    boolean foundAncestor = false;
                    currentContext = currentContext.parent;
                    while (currentContext != null) {
                        if (matchCompound(currentContext, part)) {
                            foundAncestor = true;
                            break;
                        }
                        currentContext = currentContext.parent;
                    }
                    if (!foundAncestor) return false;
                }
            }
            currentIndex--;
        }

        return true;
    }

    /**
     * Testa um bloco simples colado (Ex: div#meuId.btn[data-type="x"]:not(.disabled) )
     */
    private static boolean matchCompound(XplNode node, String compound) {
        // Universal
        if (compound.equals("*")) return true;

        String remaining = compound;

        // 1. Extrai e testa Atributos: [type="text"]
        Matcher attrMatcher = ATTR_PATTERN.matcher(remaining);
        while (attrMatcher.find()) {
            String attrName = attrMatcher.group(1);
            String operator = attrMatcher.group(2);
            String expectedVal = attrMatcher.group(3);

            if (!matchAttribute(node, attrName, operator, expectedVal)) return false;
            remaining = remaining.replace(attrMatcher.group(0), ""); // Remove o que já foi testado
        }

        // 2. Extrai e testa Pseudo-classes: :not(.hidden), :nth-child(even)
        Matcher pseudoMatcher = PSEUDO_PATTERN.matcher(remaining);
        while (pseudoMatcher.find()) {
            String pseudoName = pseudoMatcher.group(1);
            String pseudoArg = pseudoMatcher.group(2); // Pode ser null

            if (!matchPseudoClass(node, pseudoName, pseudoArg)) return false;
            remaining = remaining.replace(pseudoMatcher.group(0), "");
        }

        // Se após remover os atributos e pseudo-classes a string ficou vazia, já passámos!
        if (remaining.isEmpty()) return true;

        // 3. Testa Identificadores Base (Tag, Classe, ID)
        // Divide as classes e IDs (ex: "div.card#main" -> ["div", ".card", "#main"])
        String[] bases = remaining.split("(?=[.#])");
        for (String base : bases) {
            if (base.isEmpty()) continue;

            if (base.startsWith(".")) {
                if (node.className == null || !node.className.contains(base.substring(1))) return false;
            } else if (base.startsWith("#")) {
                if (node.id == null || !node.id.equals(base.substring(1))) return false;
            } else {
                if (!node.tag.equalsIgnoreCase(base)) return false;
            }
        }

        return true;
    }

    // --- LÓGICA DE ATRIBUTOS ---
    private static boolean matchAttribute(XplNode node, String attrName, String operator, String expectedVal) {
        Object actualValObj = node.attributes.get(attrName);

        // Se o atributo não existir (ex: input sem 'disabled')
        if (actualValObj == null) return false;

        // Se o operador for null, é apenas verificação de presença (Ex: [disabled])
        if (operator == null) return true;

        String actualVal = actualValObj.toString();

        return switch (operator) {
            case "=" -> actualVal.equals(expectedVal);
            case "^=" -> actualVal.startsWith(expectedVal); // Começa com
            case "$=" -> actualVal.endsWith(expectedVal);   // Termina com
            case "*=" -> actualVal.contains(expectedVal);   // Contém
            default -> false;
        };
    }

    // --- LÓGICA DE PSEUDO-CLASSES ---
    private static boolean matchPseudoClass(XplNode node, String name, String arg) {
        return switch (name.toLowerCase()) {
            case "not" -> {
                // :not(.disabled) -> Se o argumento bater, então FALHA!
                if (arg == null || arg.isEmpty()) yield true;
                yield !matchCompound(node, arg);
            }
            case "first-child" -> isNthChild(node, 1);
            case "last-child" -> {
                if (node.parent == null || node.parent.children == null) yield false;
                yield isNthChild(node, node.parent.children.size());
            }
            case "nth-child" -> {
                if (arg == null) yield false;
                if (arg.equalsIgnoreCase("even")) yield isNthChildEven(node, true);
                if (arg.equalsIgnoreCase("odd")) yield isNthChildEven(node, false);
                try {
                    yield isNthChild(node, Integer.parseInt(arg.trim()));
                } catch (Exception e) { yield false; }
            }
            default -> false; // Pseudo-classes interativas (:hover) são geridas pelo JavaFX nativo
        };
    }

    // --- HELPERS ESTRUTURAIS ---
    private static boolean isNthChild(XplNode node, int expectedIndex) {
        if (node.parent == null || node.parent.children == null) return false;
        int index = node.parent.children.indexOf(node) + 1; // W3C usa índice base 1
        return index == expectedIndex;
    }

    private static boolean isNthChildEven(XplNode node, boolean even) {
        if (node.parent == null || node.parent.children == null) return false;
        int index = node.parent.children.indexOf(node) + 1;
        return even == (index % 2 == 0);
    }

    private static XplNode getPreviousSibling(XplNode node) {
        if (node == null || node.parent == null || node.parent.children == null) return null;
        int index = node.parent.children.indexOf(node);
        return (index > 0) ? node.parent.children.get(index - 1) : null;
    }
}