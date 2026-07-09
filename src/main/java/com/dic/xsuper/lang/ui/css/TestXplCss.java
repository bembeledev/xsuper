package com.dic.xsuper.lang.ui.css;

import com.dic.xsuper.lang.ui.html.XplNode;

import java.util.List;

public class TestXplCss {

    public static void main(String[] args) {
        // Bloco CSS de exemplo com diretivas (exatamente como no enunciado)
        String css = """
        .button {
            color: @if(valor == 12) { "red" } @else { "blue" };
            @if(isLink) {
                a {
                    text-decoration: "none";
                    color: @switch(status) {
                        @case("primary") { "blue" }
                        @case("warning") { "yellow" }
                        @case("danger") { "red" }
                        @default { "secondary" }
                    };
                }
            }
            @for(let item of items) {
                .item-{{item}} {
                    // Agora podes usar expressões matemáticas livremente!
                    background: @if(item % 2 == 0) { "lightgray" } @else { "white" };
                }
            } @empty {
                .empty-message {
                    color: "gray";
                }
            }
        }
        """;

        System.out.println("=== TESTE DO PARSER CSS (APENAS ESTRUTURA) ===\n");

        // 1. Lexer
        System.out.println("-- TOKENS --");
        XplCssLexer lexer = new XplCssLexer(css);
        List<XplCssToken> tokens = lexer.scanTokens();
        for (XplCssToken token : tokens) {
            System.out.println(token);
        }

        // 2. Parser
        System.out.println("\n-- ÁRVORE (XplNode) --");
        XplCssParser parser = new XplCssParser(tokens);
        XplNode root = parser.parse();
        System.out.println("Root children count: " + root.children.size());
        System.out.println("Root attributes: " + root.attributes);
        // 3. Imprimir a árvore (apenas estrutura, sem avaliação)
        printTree(root, 0);
    }

    private static void printTree(XplNode node, int indent) {
        if (node == null) return;
        String pad = "  ".repeat(indent);
        System.out.print(pad + "[" + node.tag);
        if (!node.attributes.isEmpty()) {
            System.out.print(" " + node.attributes);
        }
        if (node.textContent != null && !node.textContent.trim().isEmpty()) {
            System.out.print(" text=\"" + node.textContent.trim() + "\"");
        }
        System.out.println("]");

        for (XplNode child : node.children) {
            printTree(child, indent + 1);
        }
    }
}