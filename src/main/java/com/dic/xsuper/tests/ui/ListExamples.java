package com.dic.xsuper.tests.ui;

import com.dic.xsuper.lang.ui.html.XplNode;

public class ListExamples {
    public static XplNode create() {
        // Contentor para todas as listas
        XplNode container = new XplNode("div");
        container.attributes.put("style",
                "display: flex; flex-direction: column; gap: 30px; padding: 40px; " +
                        "background: #f8fafc; font-family: 'Segoe UI', sans-serif; max-width: 600px;"
        );

        // 1. Lista com ícone personalizado (emoji) via atributo icon-type="text"
        XplNode ul1 = createCustomList(
                "text",           // icon-type
                "🚀",             // icon
                null,             // icon-url
                null,             // icon-svg
                "padding: 10px; background: #f0f0f0; border-radius: 8px;",
                "Lançar foguete", "Órbita", "Pouso"
        );
        container.addChild(ul1);

        // 2. Lista com ícone por item (data-icon) e list-style-type="custom"
        XplNode ul2 = new XplNode("ul");
        ul2.attributes.put("style", "padding: 10px; background: #ffffff; border-radius: 8px;");
        ul2.attributes.put("list-style-type", "custom");
        ul2.addChild(createLi("Item importante", "★"));
        ul2.addChild(createLi("Item secundário", "♦"));
        ul2.addChild(createLi("Item concluído", "✓"));
        container.addChild(ul2);

        // 3. Lista com imagem (URL)
        XplNode ul3 = new XplNode("ul");
        ul3.attributes.put("style", "padding: 10px; background: #ffffff; border-radius: 8px;");
        ul3.attributes.put("icon-type", "image");
        ul3.attributes.put("icon-url", "https://cdn.jsdelivr.net/npm/feather-icons/dist/icons/check.svg");
        ul3.addChild(createLiWithImage("Item com imagem", "https://cdn.jsdelivr.net/npm/feather-icons/dist/icons/check.svg"));
        container.addChild(ul3);

        // 4. Lista com SVG (path inline)
        XplNode ul4 = new XplNode("ul");
        ul4.attributes.put("style", "padding: 10px; background: #ffffff; border-radius: 8px;");
        ul4.attributes.put("icon-type", "svg");
        ul4.attributes.put("icon-svg", "M10 10 L20 20 M10 20 L20 10");
        ul4.addChild(createLi("Item com SVG", null));  // Herda o SVG do pai
        container.addChild(ul4);

        return container;
    }

// ─── MÉTODOS AUXILIARES ───────────────────────────────────────────────────────

    /**
     * Cria uma lista com ícone único (mesmo ícone para todos os itens).
     * @param iconType Tipo de ícone ("text", "image", "svg")
     * @param icon     Valor do ícone (texto, URL, ou path SVG)
     * @param iconUrl  URL da imagem (se iconType for "image")
     * @param iconSvg  Path SVG (se iconType for "svg")
     * @param style    Estilos CSS inline
     * @param items    Lista de textos dos itens
     * @return XplNode da <ul>
     */
    private static XplNode createCustomList(String iconType, String icon, String iconUrl, String iconSvg, String style, String... items) {
        XplNode ul = new XplNode("ul");
        if (style != null && !style.isEmpty()) {
            ul.attributes.put("style", style);
        }
        if (iconType != null) {
            ul.attributes.put("icon-type", iconType);
        }
        if (icon != null) {
            ul.attributes.put("icon", icon);
        }
        if (iconUrl != null) {
            ul.attributes.put("icon-url", iconUrl);
        }
        if (iconSvg != null) {
            ul.attributes.put("icon-svg", iconSvg);
        }

        for (String itemText : items) {
            XplNode li = new XplNode("li");
            li.textContent = itemText;
            // Se houver ícone definido, repassa para o <li> (data-icon ou img)
            if (icon != null) {
                li.attributes.put("data-icon", icon);
            }
            if (iconUrl != null) {
                XplNode img = new XplNode("img");
                img.attributes.put("src", iconUrl);
                li.addChild(img);
            }
            if (iconSvg != null) {
                li.attributes.put("data-icon-svg", iconSvg);
            }
            ul.addChild(li);
        }
        return ul;
    }

    /**
     * Cria um <li> com texto e ícone individual (via data-icon)
     */
    private static XplNode createLi(String text, String iconChar) {
        XplNode li = new XplNode("li");
        li.textContent = text;
        if (iconChar != null) {
            li.attributes.put("data-icon", iconChar);
        }
        return li;
    }

    /**
     * Cria um <li> com texto e imagem (via <img> como primeiro filho)
     */
    private static XplNode createLiWithImage(String text, String imageUrl) {
        XplNode li = new XplNode("li");
        XplNode img = new XplNode("img");
        img.attributes.put("src", imageUrl);
        img.attributes.put("style", "width: 20px; height: 20px; margin-right: 8px;");
        li.addChild(img);
        li.textContent = text;
        return li;
    }
}