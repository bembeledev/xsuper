package com.dic.xsuper.lang.ui.tests;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.html.HtmlLexer;
import com.dic.xsuper.lang.ui.html.HtmlParser;
import com.dic.xsuper.lang.ui.html.HtmlToken;

import java.util.List;

public class TestSuperHTML {
    public static void main(String[] args) {
        // 1. O Código HTML Híbrido de Teste
        String sourceHtml = """
                <div id="main-container" class="dark-mode-panel">
                    <h1 id="titulo">Painel DIC Moz Solutions</h1>
                    <button (click)="compilarTudo()" [disabled]="isCompiling">
                        Iniciar Compilação
                    </button>
                    <input type="text" [value]="caminhoFicheiro" />
                    <div class="painel">
                        @if (usuario.isLogado()) {
                            <button class="btn">Sair</button>
                        }
                        @for (let item of lista) {
                            <span>Item carregado!</span>
                        }
                    </div>
                    @switch(estado) {
                         @case(1) { <button>Estado: Iniciado</button> }
                         @case(2) { <button>Estado: Pausado</button> }
                         @default { <button>Estado: Erro</button> }
                    }
                    @match(tipoUsuario) {
                         @arm("admin") { <span>Bem-vindo Admin</span> }
                         @arm("editor") { <span>Bem-vindo Editor</span> }
                         @none { <span>Convidado</span> }
                    }
                    @for(let item of lista) {
                         <li>{{item}}</li>
                    } @empty {
                         <p>Nenhum item encontrado.</p>
                    }
                </div>
                """;

        System.out.println("=========================================");
        System.out.println(" 🔍 INICIANDO O LEXER DE HTML");
        System.out.println("=========================================");

        HtmlLexer lexer = new HtmlLexer(sourceHtml);
        List<HtmlToken> tokens = lexer.scanTokens();

        for (HtmlToken token : tokens) {
            System.out.println(token);
        }

        System.out.println("\n=========================================");
        System.out.println(" 🌳 INICIANDO O PARSER (ÁRVORE VDOM)");
        System.out.println("=========================================");

        HtmlParser parser = new HtmlParser(tokens);
        XplNode root = parser.parse();

        // Imprime a árvore a partir do primeiro filho real (ignorando a tag virtual 'root' do parser)
        for (XplNode child : root.children) {
            printNode(child, "");
        }
    }

    /**
     * Motor de Depuração Visual para a Árvore VDOM
     */
    private static void printNode(XplNode node, String indent) {
        System.out.println(indent + "📦 Tag: <" + node.tag + ">");

        // Atalhos Nativos
        if (!node.id.isEmpty()) {
            System.out.println(indent + "   ├── ID: " + node.id);
        }
        if (!node.className.isEmpty()) {
            System.out.println(indent + "   ├── Class: " + node.className);
        }
        if (!node.textContent.trim().isEmpty()) {
            System.out.println(indent + "   ├── Texto: '" + node.textContent.trim() + "'");
        }

        // Mapas de Configuração
        if (!node.attributes.isEmpty()) {
            System.out.println(indent + "   ├── Atributos: " + node.attributes);
        }
        if (!node.events.isEmpty()) {
            System.out.println(indent + "   ├── Eventos: " + node.events);
        }
        if (!node.bindings.isEmpty()) {
            System.out.println(indent + "   ├── Bindings: " + node.bindings);
        }

        // Navegação Hierárquica
        if (node.parent != null && !node.parent.tag.equals("root")) {
            System.out.println(indent + "   ├── Pai: <" + node.parent.tag + ">");
        }

        // Filhos Recursivos
        if (!node.children.isEmpty()) {
            System.out.println(indent + "   └── Filhos (" + node.children.size() + "):");
            for (XplNode child : node.children) {
                printNode(child, indent + "       ");
            }
        } else {
            System.out.println(indent + "   └── Filhos (0)");
        }
        //System.out.println(); // Espaçamento entre nós na consola
    }
}