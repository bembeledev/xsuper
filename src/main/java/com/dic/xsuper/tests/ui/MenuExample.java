package com.dic.xsuper.tests.ui;

import com.dic.xsuper.lang.ui.html.XplNode;

/**
 * Exemplo de construção de navegação (<nav>) e menus (<menu>) com <menuitem>.
 * <p>
 * Corresponde ao HTML:
 * <pre>
 * &lt;!-- Navegação horizontal --&gt;
 * &lt;nav orientation="horizontal" style="margin: 10px;"&gt;
 *     &lt;menuitem icon="🏠" href="/"&gt;Início&lt;/menuitem&gt;
 *     &lt;menuitem icon="📁" href="/docs"&gt;Documentação&lt;/menuitem&gt;
 *     &lt;menuitem icon="👤" href="/profile"&gt;Perfil&lt;/menuitem&gt;
 * &lt;/nav&gt;
 *
 * &lt;!-- Menu vertical com subitens --&gt;
 * &lt;menu orientation="vertical" icon="⚙️"&gt;
 *     &lt;menuitem icon="📊"&gt;Dashboard&lt;/menuitem&gt;
 *     &lt;menuitem icon="📈"&gt;Estatísticas&lt;/menuitem&gt;
 *     &lt;menuitem icon="🔧" shortcut="Ctrl+S"&gt;Definições&lt;/menuitem&gt;
 *     &lt;menuitem disabled&gt;Opção desativada&lt;/menuitem&gt;
 * &lt;/menu&gt;
 * </pre>
 */
public class MenuExample {

    /**
     * Cria um exemplo de navegação horizontal (<nav>).
     */
    public static XplNode createNavExample() {
        XplNode nav = new XplNode("nav");
        nav.attributes.put("orientation", "horizontal");
        nav.attributes.put("style",
                "display: flex; flex-direction: row; gap: 8px; " +
                        "padding: 10px 16px; background: #1e293b; border-radius: 8px; " +
                        "margin: 10px;"
        );

        nav.addChild(createMenuItem("🏠", "/", "Início"));
        nav.addChild(createMenuItem("📁", "/docs", "Documentação"));
        nav.addChild(createMenuItem("👤", "/profile", "Perfil"));

        return nav;
    }

    /**
     * Cria um exemplo de menu vertical (<menu>).
     */
    public static XplNode createMenuExample() {
        XplNode menu = new XplNode("menu");
        menu.attributes.put("orientation", "vertical");
        menu.attributes.put("icon", "⚙️");
        menu.attributes.put("style",
                "display: flex; flex-direction: column; gap: 4px; " +
                        "padding: 12px; background: #1e293b; border-radius: 8px; " +
                        "max-width: 200px; margin: 10px;"
        );

        menu.addChild(createMenuItem("📊", null, "Dashboard"));
        menu.addChild(createMenuItem("📈", null, "Estatísticas"));
        menu.addChild(createMenuItem("🔧", null, "Definições", "Ctrl+S"));
        menu.addChild(createMenuItem(null, null, "Opção desativada", null, true));

        return menu;
    }

    // ─── Métodos auxiliares ──────────────────────────────────────────────────

    private static XplNode createMenuItem(String icon, String href, String label) {
        return createMenuItem(icon, href, label, null, false);
    }

    private static XplNode createMenuItem(String icon, String href, String label, String shortcut) {
        return createMenuItem(icon, href, label, shortcut, false);
    }

    private static XplNode createMenuItem(String icon, String href, String label, String shortcut, boolean disabled) {
        XplNode item = new XplNode("menuitem");
        if (icon != null) item.attributes.put("icon", icon);
        if (href != null) item.attributes.put("href", href);
        if (shortcut != null) item.attributes.put("shortcut", shortcut);
        if (disabled) item.attributes.put("disabled", "true");

        // Monta o texto com ícone e atalho
        StringBuilder displayText = new StringBuilder();
        if (icon != null) displayText.append(icon).append(" ");
        displayText.append(label);
        if (shortcut != null) displayText.append("  (").append(shortcut).append(")");
        item.textContent = displayText.toString();

        // Estilos base para menuitem
        String style = "padding: 8px 12px; border-radius: 4px; cursor: pointer; " +
                "color: #e2e8f0; transition: background 0.15s;";
        if (disabled) {
            style += " opacity: 0.5; cursor: not-allowed;";
        }
        item.attributes.put("style", style);

        return item;
    }

    /**
     * Exemplo combinado: ambos os menus num contentor.
     */
    public static XplNode createCombinedExample() {
        XplNode container = new XplNode("div");
        container.attributes.put("style",
                "display: flex; flex-direction: column; gap: 20px; " +
                        "padding: 20px; background: #0f172a;"
        );
        container.addChild(createNavExample());
        container.addChild(createMenuExample());
        return container;
    }

    /**
     * Exemplo de como usar no motor UI.
     */
    public static void demo() {
        XplNode root = createCombinedExample();
        // __ui_engine.loadView(root);
    }
}