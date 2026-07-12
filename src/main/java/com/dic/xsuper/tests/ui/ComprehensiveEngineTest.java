package com.dic.xsuper.tests.ui;

import com.dic.xsuper.lang.ui.html.XplNode;

public class ComprehensiveEngineTest {

    /**
     * Cria um contentor com todos os exemplos de teste.
     * @return XplNode raiz (um <div> contentor)
     */
    public static XplNode createExample() {
        // ─── Contentor principal ──────────────────────────────────────────────
        XplNode container = new XplNode("div");
        container.attributes.put("style",
                "display: flex; flex-direction: column; gap: 20px; padding: 20px; " +
                        "background: #0f172a; color: #e2e8f0; font-family: 'Segoe UI', sans-serif;"
        );

        // 1. Scroll com overflow (div com scroll nativo)
        container.addChild(createScrollExample1());

        // 2. Lista com scroll estilizado
        container.addChild(createScrollExample2());

        // 3. Textarea com atributos de formulário
        container.addChild(createTextareaExample());

        // 4. Âncora (<a>)
        container.addChild(createAnchorExample());

        // 5. Figura com legenda
        container.addChild(createFigureExample());

        // 6. Tabs modernas
        container.addChild(createTabsExample());

        // 7. WebView (browser embutido)
        container.addChild(createWebViewExample());

        return container;
    }

    // ─── 1. Overflow scroll ──────────────────────────────────────────────────

    private static XplNode createScrollExample1() {
        XplNode div = new XplNode("div");
        div.attributes.put("style",
                "height: 150px; overflow-y: auto; background: #1e293b; border-radius: 8px; padding: 12px; " +
                        "border: 1px solid #334155;"
        );
        for (int i = 1; i <= 10; i++) {
            XplNode p = new XplNode("p");
            p.textContent = "Linha " + i + " – O JavaFX vai esconder isto até fazeres scroll!";
            p.attributes.put("style", "margin: 4px 0; color: #cbd5e1;");
            div.addChild(p);
        }
        return div;
    }

    // ─── 2. Lista com scroll estilizado ─────────────────────────────────────

    private static XplNode createScrollExample2() {
        XplNode ul = new XplNode("ul");
        ul.attributes.put("style",
                "height: 200px; overflow-y: auto; " +
                        "scrollbar-thumb: #ef4444; scrollbar-hover: #dc2626; scrollbar-width: 10px; " +
                        "background: #1e293b; border-radius: 8px; padding: 12px; " +
                        "border: 1px solid #334155; list-style-type: disc;"
        );
        String[] itens = {"Item 1", "Item 2", "Item 3", "Item 4", "Item 5", "Item 6", "Item 7"};
        for (String texto : itens) {
            XplNode li = new XplNode("li");
            li.textContent = texto;
            li.attributes.put("style", "color: #e2e8f0; padding: 4px 0;");
            ul.addChild(li);
        }
        return ul;
    }

    // ─── 3. Textarea ─────────────────────────────────────────────────────────

    private static XplNode createTextareaExample() {
        XplNode textarea = new XplNode("textarea");
        textarea.textContent = "Texto inicial de teste no Textarea.";
        textarea.attributes.put("rows", "4");
        textarea.attributes.put("cols", "30");
        textarea.attributes.put("name", "comentario");
        textarea.attributes.put("style",
                "width: 100%; padding: 10px; border-radius: 6px; border: 1px solid #475569; " +
                        "background: #0f172a; color: #e2e8f0; font-size: 14px; resize: vertical;"
        );
        return textarea;
    }

    // ─── 4. Âncora ───────────────────────────────────────────────────────────

    private static XplNode createAnchorExample() {
        XplNode a = new XplNode("a");
        a.textContent = "🔗 Visitar Documentação Externa";
        a.attributes.put("href", "https://github.com");
        a.attributes.put("style",
                "color: #60a5fa; text-decoration: underline; cursor: pointer; font-weight: 500; " +
                        "padding: 8px 0; display: inline-block;"
        );
        return a;
    }

    // ─── 5. Figura com legenda ──────────────────────────────────────────────

    private static XplNode createFigureExample() {
        XplNode figure = new XplNode("figure");
        figure.attributes.put("style",
                "display: flex; flex-direction: column; align-items: center; gap: 8px; " +
                        "background: #1e293b; border-radius: 8px; padding: 16px; border: 1px solid #334155;"
        );

        XplNode img = new XplNode("img");
        img.attributes.put("src", "https://via.placeholder.com/150");
        img.attributes.put("alt", "Logo Exemplo");
        img.attributes.put("style", "width: 100px; height: 100px; object-fit: cover; border-radius: 6px;");
        figure.addChild(img);

        XplNode caption = new XplNode("figcaption");
        caption.textContent = "Legenda da Figura";
        caption.attributes.put("style", "color: #94a3b8; font-size: 14px; font-style: italic;");
        figure.addChild(caption);

        return figure;
    }

    // ─── 6. Tabs modernas ──────────────────────────────────────────────────

    private static XplNode createTabsExample() {
        XplNode tabs = new XplNode("tabs");
        tabs.attributes.put("style",
                "tab-active-color: #ef4444; tab-inactive-color: #64748b; " +
                        "display: flex; flex-direction: column; gap: 4px; background: #1e293b; border-radius: 8px; padding: 12px;"
        );

        // Tab 1
        XplNode tab1 = new XplNode("tab");
        tab1.attributes.put("title", "Geral");
        tab1.textContent = "Conteúdo da aba geral.";
        tab1.attributes.put("style", "padding: 8px 12px; background: #334155; border-radius: 6px;");
        tabs.addChild(tab1);

        // Tab 2 (ativa)
        XplNode tab2 = new XplNode("tab");
        tab2.attributes.put("title", "Avançado");
        tab2.attributes.put("active", "true");
        tab2.textContent = "Conteúdo da aba avançada.";
        tab2.attributes.put("style", "padding: 8px 12px; background: #475569; border-radius: 6px;");
        tabs.addChild(tab2);

        return tabs;
    }

    // ─── 7. WebView (browser) ──────────────────────────────────────────────

    private static XplNode createWebViewExample() {
        XplNode web = new XplNode("web");
        web.attributes.put("src", "https://google.com");
        web.attributes.put("style",
                "width: 100%; height: 250px; border-radius: 8px; border: 1px solid #334155; overflow: hidden;"
        );
        return web;
    }

}