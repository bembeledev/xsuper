package com.dic.xsuper.tests.ui;

import com.dic.xsuper.dom.node.XplNode;

public class DetailsExample {

    /**
     * Cria um nó <details> com conteúdo, estilos e um <summary> personalizado.
     */
    public static XplNode createDetailsExample() {
        // ─── Nó <details> ──────────────────────────────────────────────────
        XplNode details = new XplNode("details");
        details.attributes.put("open", "true");
        details.attributes.put("summary", "📖 Clique para expandir");
        //details.attributes.put("style", "margin: 10px; background: #dddddd; border-radius: 8px;");

        // ─── Nó <summary> ──────────────────────────────────────────────────
        XplNode summary = new XplNode("summary");
        summary.textContent = "📖 Clique para expandir";
        // Podes adicionar estilos ao summary se necessário:
        // summary.attributes.put("style", "font-weight: bold; color: #e2e8f0;");
        details.addChild(summary);

        // ─── Nó <div> (conteúdo) ──────────────────────────────────────────
        XplNode contentDiv = new XplNode("div");
        contentDiv.attributes.put("style", "padding: 8px 12px;");

        // ─── Nó <p> ────────────────────────────────────────────────────────
        XplNode paragraph = new XplNode("p");
        paragraph.textContent = "Este é o conteúdo escondido do details.";
        contentDiv.addChild(paragraph);

        // ─── Nó <ul> ──────────────────────────────────────────────────────
        XplNode ul = new XplNode("ul");
        // Se quiseres estilos para a lista, adiciona aqui:
        ul.attributes.put("style", "list-style-type: disc; background-color:#FFFFFF");


        // ─── Itens <li> ──────────────────────────────────────────────────
        XplNode li1 = new XplNode("li");
        li1.textContent = "Item 1";

        ul.addChild(li1);

        XplNode li2 = new XplNode("li");
        li2.textContent = "Item 2";
        ul.addChild(li2);

        contentDiv.addChild(ul);

        // Adiciona o div ao details
        details.addChild(contentDiv);

        return details;
    }

    /**
     * Exemplo de como usar no motor UI.
     */
    public static void demo() {
        // Cria o nó details
        XplNode detailsNode = createDetailsExample();

        // Renderiza usando o motor (exemplo)
        // __ui_engine.loadView(detailsNode);
        // ou se esperar HTML:
        // String html = HtmlGenerator.generate(detailsNode);
        // __ui_engine.loadView(html);
    }
}