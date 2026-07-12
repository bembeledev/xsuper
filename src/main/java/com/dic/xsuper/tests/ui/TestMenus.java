package com.dic.xsuper.tests.ui;


import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.scene.layout.VBox;

/**
 * Teste visual para todos os tipos de menus:
 * - Menubar (barra de menu de topo)
 * - Menus e sub‑menus
 * - Menu de contexto (contextmenu)
 * - Barra de navegação (nav)
 */
public class TestMenus {

    /**
     * Cria uma árvore XplNode com todos os exemplos de menus.
     */
    public static XplNode createFullMenuExample() {
        VBox container = new VBox(20);
        container.setStyle("-fx-padding: 20; -fx-background-color: #0f172a;");

        // ─── 1. MENUBAR (Desktop) ────────────────────────────────────────────
        XplNode menubar = createDesktopMenubar();

        // ─── 2. CONTEXT MENU (exemplo anexado a um botão) ──────────────────
        XplNode contextMenuExample = createContextMenuExample();

        // ─── 3. NAV (Web) ──────────────────────────────────────────────────
        XplNode nav = createWebNav();

        // ─── 4. MENU VERTICAL (alternativa) ──────────────────────────────
        XplNode verticalMenu = createVerticalMenu();

        // Adiciona todos ao contentor
        XplNode root = new XplNode("div");
        root.attributes.put("style", "display: flex; flex-direction: column; gap: 20px;");
        root.addChild(menubar);
        root.addChild(contextMenuExample);
        root.addChild(nav);
        root.addChild(verticalMenu);

        return root;
    }

    // ─── 1. MENUBAR ──────────────────────────────────────────────────────────

    private static XplNode createDesktopMenubar() {
        XplNode menubar = new XplNode("menubar");
        menubar.attributes.put("style", "-fx-background-color: #1e293b; -fx-padding: 4px 12px;");

        // Menu File
        XplNode fileMenu = new XplNode("menu");
        fileMenu.attributes.put("label", "File");
        fileMenu.attributes.put("icon", "📂");

        fileMenu.addChild(createMenuItem("New", "📄", "Ctrl+N"));
        fileMenu.addChild(createMenuItem("Open", "📂", "Ctrl+O"));

        // Sub‑menu Recent
        XplNode recentMenu = new XplNode("menu");
        recentMenu.attributes.put("label", "Recent");
        recentMenu.addChild(createMenuItem("Project A", null, null));
        recentMenu.addChild(createMenuItem("Project B", null, null));
        recentMenu.addChild(createMenuItem("Project C", null, null));
        recentMenu.addChild(createSeparator());
        recentMenu.addChild(createMenuItem("Clear Recent", null, null));
        fileMenu.addChild(recentMenu);

        fileMenu.addChild(createSeparator());
        fileMenu.addChild(createMenuItem("Save", "💾", "Ctrl+S"));
        fileMenu.addChild(createMenuItem("Save As...", "💾", "Ctrl+Shift+S"));
        fileMenu.addChild(createSeparator());
        fileMenu.addChild(createMenuItem("Exit", "🚪", "Ctrl+Q"));

        menubar.addChild(fileMenu);

        // Menu Edit
        XplNode editMenu = new XplNode("menu");
        editMenu.attributes.put("label", "Edit");
        editMenu.attributes.put("icon", "✏️");
        editMenu.addChild(createMenuItem("Undo", "↩️", "Ctrl+Z"));
        editMenu.addChild(createMenuItem("Redo", "↪️", "Ctrl+Y"));
        editMenu.addChild(createSeparator());
        editMenu.addChild(createMenuItem("Cut", "✂️", "Ctrl+X"));
        editMenu.addChild(createMenuItem("Copy", "📋", "Ctrl+C"));
        editMenu.addChild(createMenuItem("Paste", "📎", "Ctrl+V"));
        menubar.addChild(editMenu);

        // Menu View
        XplNode viewMenu = new XplNode("menu");
        viewMenu.attributes.put("label", "View");
        viewMenu.attributes.put("icon", "👁️");
        viewMenu.addChild(createMenuItem("Zoom In", "🔍+", "Ctrl++"));
        viewMenu.addChild(createMenuItem("Zoom Out", "🔍-", "Ctrl+-"));
        viewMenu.addChild(createSeparator());
        viewMenu.addChild(createMenuItem("Full Screen", "⛶", "F11"));
        menubar.addChild(viewMenu);

        // Menu Help
        XplNode helpMenu = new XplNode("menu");
        helpMenu.attributes.put("label", "Help");
        helpMenu.attributes.put("icon", "❓");
        helpMenu.addChild(createMenuItem("Documentation", "📖", null));
        helpMenu.addChild(createMenuItem("About", "ℹ️", null));
        menubar.addChild(helpMenu);

        return menubar;
    }

    // ─── 2. CONTEXT MENU ────────────────────────────────────────────────────

    private static XplNode createContextMenuExample() {
        // Cria um elemento alvo para o contexto
        XplNode target = new XplNode("div");
        target.attributes.put("id", "myElement");
        target.attributes.put("style",
                "padding: 40px; background: #1e293b; border: 2px dashed #475569; " +
                        "border-radius: 8px; text-align: center; color: #94a3b8; cursor: context-menu;");
        target.textContent = "🖱️ Clique com botão direito aqui";

        // Cria o context menu
        XplNode contextMenu = new XplNode("contextmenu");
        contextMenu.attributes.put("target", "myElement");
        contextMenu.addChild(createMenuItem("Copy", "📋", "Ctrl+C"));
        contextMenu.addChild(createMenuItem("Paste", "📎", "Ctrl+V"));
        contextMenu.addChild(createSeparator());
        contextMenu.addChild(createMenuItem("Delete", "🗑️", "Delete"));
        contextMenu.addChild(createSeparator());
        contextMenu.addChild(createMenuItem("Properties", "⚙️", null));

        // Anexa o context menu ao target (para ser processado pelo motor)
        // Nota: O motor deve ligar o evento de contexto ao elemento com id "myElement"
        // Este é um exemplo estrutural; a ligação será feita pela ponte JavaFX.
        // Para demonstração, colocamos ambos no mesmo container.
        XplNode wrapper = new XplNode("div");
        wrapper.attributes.put("style", "display: flex; flex-direction: column; gap: 8px;");
        wrapper.addChild(target);
        wrapper.addChild(contextMenu);

        return wrapper;
    }

    // ─── 3. NAV (Web) ──────────────────────────────────────────────────────

    private static XplNode createWebNav() {
        XplNode nav = new XplNode("nav");
        nav.attributes.put("orientation", "horizontal");
        nav.attributes.put("style",
                "background: #1e293b; padding: 8px 16px; border-radius: 8px; " +
                        "display: flex; gap: 16px; align-items: center;");

        nav.addChild(createNavItem("🏠", "Início"));
        nav.addChild(createNavItem("📁", "Documentação"));
        nav.addChild(createNavItem("👤", "Perfil"));
        nav.addChild(createNavItem("⚙️", "Definições"));

        return nav;
    }

    private static XplNode createNavItem(String icon, String label) {
        XplNode item = new XplNode("menuitem");
        item.textContent = icon + " " + label;
        item.attributes.put("style",
                "color: #e2e8f0; padding: 6px 12px; border-radius: 4px; " +
                        "cursor: pointer; transition: background 0.2s;");
        return item;
    }

    // ─── 4. MENU VERTICAL ──────────────────────────────────────────────────

    private static XplNode createVerticalMenu() {
        XplNode menu = new XplNode("menu");
        menu.attributes.put("orientation", "vertical");
        menu.attributes.put("style",
                "background: #1e293b; padding: 8px; border-radius: 8px; " +
                        "display: flex; flex-direction: column; gap: 4px; width: 200px;");

        menu.addChild(createMenuItem("Dashboard", "📊", null));
        menu.addChild(createMenuItem("Statistics", "📈", null));
        menu.addChild(createMenuItem("Settings", "🔧", "Ctrl+S"));
        menu.addChild(createMenuItem("Disabled Item", null, null, true));

        return menu;
    }

    // ─── UTILITÁRIOS ────────────────────────────────────────────────────────

    private static XplNode createMenuItem(String label, String icon, String shortcut) {
        return createMenuItem(label, icon, shortcut, false);
    }

    private static XplNode createMenuItem(String label, String icon, String shortcut, boolean disabled) {
        XplNode item = new XplNode("menuitem");
        item.textContent = label;
        if (icon != null) item.attributes.put("icon", icon);
        if (shortcut != null) item.attributes.put("shortcut", shortcut);
        if (disabled) item.attributes.put("disabled", "true");
        return item;
    }

    private static XplNode createSeparator() {
        XplNode sep = new XplNode("menuitem");
        sep.attributes.put("separator", "true");
        return sep;
    }
}