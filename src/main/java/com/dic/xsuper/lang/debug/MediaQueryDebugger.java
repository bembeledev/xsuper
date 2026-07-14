package com.dic.xsuper.lang.debug;

import com.dic.xsuper.lang.ui.css.media.JavaFxMediaListener;
import com.dic.xsuper.lang.ui.css.media.XplMediaNode;
import com.dic.xsuper.lang.ui.html.XplNode;

import java.util.Map;

/**
 * Ferramenta de depuração para inspecionar o estado das media queries
 * e os estilos finais de um nó ou árvore.
 */
public class MediaQueryDebugger {

    /**
     * Imprime o mapa de estilos de um único nó, indicando se há media query ativa.
     */
    public static void debugNode(XplNode node, JavaFxMediaListener mediaListener) {
        if (node == null) {
            System.out.println("[MediaDebug] Nó é null");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📊 DEBUG NODE: ").append(node.tag);
        if (node.id != null && !node.id.isEmpty()) sb.append("#").append(node.id);
        if (node.className != null && !node.className.isEmpty()) sb.append(".").append(node.className);
        sb.append("\n");

        // Estilos atuais (após todas as aplicações)
        sb.append("  🎨 Estilos atuais no node.style:\n");
        if (node.style.isEmpty()) {
            sb.append("    (vazio)\n");
        } else {
            for (Map.Entry<String, String> entry : node.style.entrySet()) {
                sb.append("    ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        // Verifica se há alguma media query ativa que afete este nó
        if (mediaListener != null) {
            sb.append("  📡 Media queries ativas que afetam este nó:\n");
            boolean found = false;
            for (XplMediaNode mediaNode : mediaListener.mediaMap.values()) {
                if (mediaNode.isActive) {
                    for (String selector : mediaNode.selectorsAndStyles.keySet()) {
                        if (matchesSelector(node, selector)) {
                            found = true;
                            sb.append("    ✅ ").append(mediaNode.condition)
                                    .append(" (seletor: ").append(selector).append(")\n");
                            Map<String, String> rules = mediaNode.selectorsAndStyles.get(selector);
                            for (Map.Entry<String, String> rule : rules.entrySet()) {
                                sb.append("       - ").append(rule.getKey()).append(": ").append(rule.getValue()).append("\n");
                            }
                        }
                    }
                }
            }
            if (!found) {
                sb.append("    (nenhuma media query ativa afeta este nó)\n");
            }
        }

        System.out.println(sb.toString());
    }

    /**
     * Percorre toda a árvore e imprime o estado de cada nó com estilo não vazio.
     */
    public static void debugTree(XplNode root, JavaFxMediaListener mediaListener) {
        if (root == null) {
            System.out.println("[MediaDebug] Árvore é null");
            return;
        }
        debugNodeRecursive(root, mediaListener, 0);
    }

    private static void debugNodeRecursive(XplNode node, JavaFxMediaListener mediaListener, int depth) {
        if (node == null) return;
        String indent = "  ".repeat(depth);
        System.out.println(indent + "🔹 " + node.tag + (node.id != null ? "#" + node.id : "") + (node.className != null ? "." + node.className : ""));
        if (!node.style.isEmpty()) {
            System.out.println(indent + "  style: " + node.style);
        }
        // Verifica se há media query ativa
        if (mediaListener != null) {
            for (XplMediaNode mediaNode : mediaListener.mediaMap.values()) {
                if (mediaNode.isActive) {
                    for (String selector : mediaNode.selectorsAndStyles.keySet()) {
                        if (matchesSelector(node, selector)) {
                            System.out.println(indent + "  📡 ATIVO: " + mediaNode.condition + " -> " + mediaNode.selectorsAndStyles.get(selector));
                        }
                    }
                }
            }
        }
        for (XplNode child : node.children) {
            debugNodeRecursive(child, mediaListener, depth + 1);
        }
    }

    private static boolean matchesSelector(XplNode node, String selector) {
        if (selector.startsWith(".")) {
            return node.className != null && node.className.contains(selector.substring(1));
        } else if (selector.startsWith("#")) {
            return node.id != null && node.id.equals(selector.substring(1));
        } else {
            return node.tag != null && node.tag.equalsIgnoreCase(selector);
        }
    }
}