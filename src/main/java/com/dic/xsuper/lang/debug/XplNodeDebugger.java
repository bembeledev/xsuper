package com.dic.xsuper.lang.debug;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.tags.TagFactory;
import javafx.scene.Node;
import javafx.scene.layout.Region;

public class XplNodeDebugger {

    // =========================================================================
    // 🐞 DEBUGGER VISUAL DE ÁRVORE DOM
    // =========================================================================
    public static void debugTreeDOM(XplNode node) {
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


    // =========================================================================
    // 🐞 DEBUGGER VISUAL W3C (Raio-X Físico Absoluto via NativeTag)
    // =========================================================================
    public static void debugDimension(XplNode node) {
        System.out.println("\n🌳 ÁRVORE DOM XPL (Modo Raio-X Nativo):");

        // ⭐ A MAGIA: Transforma o DOM virtual (XplNode) numa árvore física JavaFX
        // usando a tua arquitetura de tags para medir tudo de forma precisa na memória!
        NativeTag rootTag = TagFactory.create(node);

        if (rootTag != null) {
            rootTag.build(); // Constrói toda a hierarquia JavaFX em background!
            printTag(rootTag, "", true);
        } else {
            System.err.println("Erro: Não foi possível compilar a tag raiz.");
        }

        System.out.println("====================================================================\n");
    }

    private static void printTag(NativeTag tag, String prefix, boolean isTail) {
        if (tag == null) return;

        XplNode source = tag.getSourceNode();
        StringBuilder nodeInfo = new StringBuilder();

        // 1. Identificação do Nó
        nodeInfo.append("<").append(source.tag);
        if (source.id != null && !source.id.isEmpty()) {
            nodeInfo.append(" id=\"").append(source.id).append("\"");
        }
        if (source.className != null && !source.className.isEmpty()) {
            nodeInfo.append(" class=\"").append(source.className).append("\"");
        }
        nodeInfo.append(">");

        // =====================================================================
        // ⭐ EXTRAÇÃO DIMENSIONAL DIRETO DO JAVAFX (fxNode)
        // =====================================================================
        Node fxNode = tag.getFxNode();

        if (fxNode instanceof Region region) {
            // Forçamos o JavaFX a fazer os cálculos matemáticos na hora (-1 = auto)
            // baseando-se nas fontes reais, paddings e tamanhos dos filhos W3C!
            double w = region.prefWidth(-1);
            double h = region.prefHeight(-1);

            nodeInfo.append(String.format("  [📏 %.1f x %.1fpx]", w, h));

        } else if (fxNode != null) {
            // Shapes (SVG, Canvas, Linhas) ou controlos base
            double w = fxNode.getLayoutBounds().getWidth();
            double h = fxNode.getLayoutBounds().getHeight();
            nodeInfo.append(String.format("  [📏 %.1f x %.1fpx]", w, h));

        } else {
            // Contentores puramente virtuais
            nodeInfo.append("  [👻 Virtual]");
        }

        // 2. Imprime no terminal com a formatação em árvore
        System.out.println(prefix + (isTail ? "└── " : "├── ") + nodeInfo.toString());

        // 3. Mergulha nos filhos recursivamente lendo diretamente da NativeTag!
        if (tag.getChildren() != null && !tag.getChildren().isEmpty()) {
            int size = tag.getChildren().size();
            for (int i = 0; i < size - 1; i++) {
                printTag(tag.getChildren().get(i), prefix + (isTail ? "    " : "│   "), false);
            }
            if (size > 0) {
                printTag(tag.getChildren().get(size - 1), prefix + (isTail ? "    " : "│   "), true);
            }
        }
    }
}
