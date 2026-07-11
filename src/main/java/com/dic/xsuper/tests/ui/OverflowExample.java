package com.dic.xsuper.tests.ui;

import com.dic.xsuper.lang.ui.html.XplNode;

/**
 * Exemplos de uso de overflow (scroll) com personalização.
 * <p>
 * Corresponde ao HTML:
 * <pre>
 * &lt;!-- Exemplo 1: Simples --&gt;
 * &lt;div style="height: 200px; overflow-y: auto;"&gt;
 *     &lt;p&gt;O teu texto...&lt;/p&gt;
 *     &lt;p&gt;Mais texto...&lt;/p&gt;
 *     &lt;p&gt;O JavaFX vai esconder isto até fazeres scroll!&lt;/p&gt;
 * &lt;/div&gt;
 *
 * &lt;!-- Exemplo 2: Personalização Absoluta --&gt;
 * &lt;ul style="height: 300px; overflow: auto; scrollbar-thumb: #ef4444; scrollbar-hover: #dc2626; scrollbar-width: 10px;"&gt;
 *     &lt;li&gt;Item 1&lt;/li&gt;
 *     &lt;li&gt;Item 2&lt;/li&gt;
 * &lt;/ul&gt;
 * </pre>
 */
public class OverflowExample {

    /**
     * Cria o exemplo 1: div com overflow-y: auto.
     */
    public static XplNode createScrollExample1() {
        XplNode div = new XplNode("div");
        div.attributes.put("style",
                "height: 200px; " +
                        "overflow-y: auto; " +
                        "background: #f8f9fa; " +
                        "border-radius: 8px; " +
                        "padding: 16px; " +
                        "border: 1px solid #e9ecef;"
        );

        // Adiciona parágrafos para encher o conteúdo
        String[] textos = {
                "O teu texto...",
                "Mais texto...",
                "O JavaFX vai esconder isto até fazeres scroll!",
                "Linha 4: Mais conteúdo para forçar scroll.",
                "Linha 5: Outra linha para encher."
        };
        for (String texto : textos) {
            XplNode p = new XplNode("p");
            p.textContent = texto;
            p.attributes.put("style", "margin: 2px 0; color: #212529;");
            div.addChild(p);
        }

        return div;
    }

    /**
     * Cria o exemplo 2: ul com overflow: auto e personalização de scrollbar.
     */
    public static XplNode createScrollExample2() {
        XplNode ul = new XplNode("ul");
        ul.attributes.put("style",
                "height: 300px; " +
                        "overflow: auto; " +
                        "scrollbar-thumb: #ef4444; " +
                        "scrollbar-hover: #dc2626; " +
                        "scrollbar-width: 10px; " +
                        "background: #ffffff; " +
                        "border-radius: 8px; " +
                        "padding: 12px; " +
                        "border: 1px solid #dee2e6; " +
                        "list-style-type: none;"
        );

        // Adiciona vários itens para forçar scroll
        for (int i = 1; i <= 20; i++) {
            XplNode li = new XplNode("li");
            li.textContent = "Item " + i;
            li.attributes.put("style",
                    "padding: 8px 12px; " +
                            "margin: 4px 0; " +
                            "background: #f8f9fa; " +
                            "border-radius: 4px; " +
                            "border-left: 3px solid #ef4444;"
            );

            ul.addChild(li);
        }

        return ul;
    }

    /**
     * Cria um container com ambos os exemplos lado a lado.
     */
    public static XplNode createOverflowDemo() {
        XplNode container = new XplNode("div");
        container.attributes.put("style",
                "display: flex; " +
                        "gap: 20px; " +
                        "padding: 20px; " +
                        "background: #f0f2f5; " +
                        "flex-wrap: wrap; " +
                        "align-items: flex-start;"
        );

        // Exemplo 1
        XplNode divScroll = createScrollExample1();
        divScroll.attributes.put("style", divScroll.attributes.get("style") + " flex: 1; min-width: 200px;");

        // Exemplo 2
        XplNode ulScroll = createScrollExample2();
        ulScroll.attributes.put("style", ulScroll.attributes.get("style") + " flex: 1; min-width: 200px;");

        container.addChild(divScroll);
        container.addChild(ulScroll);

        return container;
    }

    /**
     * Exemplo de uso no motor UI.
     */
    public static void demo() {
        XplNode root = createOverflowDemo();
        // Renderiza com o motor
        // __ui_engine.loadView(root);
    }
}