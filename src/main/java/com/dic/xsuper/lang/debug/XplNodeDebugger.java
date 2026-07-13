package com.dic.xsuper.lang.debug;

import com.dic.xsuper.lang.ui.html.XplNode;

public class XplNodeDebugger {

    // =========================================================================
    // 🐞 DEBUGGER VISUAL DE ÁRVORE DOM
    // =========================================================================
    public static void debbug(XplNode node) {
        System.out.println("\n🌳 ÁRVORE DOM XPL:");
        printNode(node, "", true);
        System.out.println("========================================\n");
    }

    private static void printNode(XplNode node, String prefix, boolean isTail) {
        if (node == null) return;

        // 1. Constrói a representação limpa do nó (ex: <div id="app" class="card" style="padding: 10px;">)
        StringBuilder nodeInfo = new StringBuilder();
        nodeInfo.append("<").append(node.tag);

        // Injeta ID
        if (node.id != null && !node.id.isEmpty()) {
            nodeInfo.append(" id=\"").append(node.id).append("\"");
        }

        // Injeta Classe
        if (node.className != null && !node.className.isEmpty()) {
            nodeInfo.append(" class=\"").append(node.className).append("\"");
        }

        // Injeta Estilos (Serializa o mapa style)
        if (node.style != null && !node.style.isEmpty()) {
            nodeInfo.append(" style=\"");
            for (java.util.Map.Entry<String, String> entry : node.style.entrySet()) {
                nodeInfo.append(entry.getKey()).append(": ").append(entry.getValue()).append("; ");
            }
            nodeInfo.append("\"");
        }

        nodeInfo.append(">");

        // 2. Imprime o nó com as ramificações ASCII
        System.out.println(prefix + (isTail ? "└── " : "├── ") + nodeInfo.toString());

        // 3. Mergulha nos filhos recursivamente, ajustando o prefixo da árvore
        if (node.children != null && !node.children.isEmpty()) {
            for (int i = 0; i < node.children.size() - 1; i++) {
                // Filhos intermédios levam ramificação "├── "
                printNode(node.children.get(i), prefix + (isTail ? "    " : "│   "), false);
            }
            // O último filho leva a ramificação de cauda "└── "
            if (!node.children.isEmpty()) {
                printNode(node.children.getLast(), prefix + (isTail ? "    " : "│   "), true);
            }
        }
    }
}
