package com.dic.xsuper.tests.ui;

import com.dic.xsuper.lang.ui.html.XplNode;

/**
 * Exemplo de construção da estrutura <tabs> com <tab> para testes.
 * <p>
 * A estrutura criada corresponde ao HTML:
 * <pre>
 * &lt;tabs&gt;
 *     &lt;tab title="Perfil" closable="false"&gt;
 *         &lt;p&gt;Conteúdo do perfil do utilizador...&lt;/p&gt;
 *     &lt;/tab&gt;
 *     &lt;tab title="Segurança"&gt;
 *         &lt;input type="password" placeholder="Nova senha"&gt;
 *     &lt;/tab&gt;
 * &lt;/tabs&gt;
 * </pre>
 */
public class TabExample {

    /**
     * Cria a árvore XplNode para a estrutura de tabs.
     * @return o nó raiz (&lt;tabs&gt;)
     */
    public static XplNode createTabsExample() {
        // ─── Nó <tabs> ──────────────────────────────────────────────────────
        XplNode tabs = new XplNode("tabs");
        tabs.attributes.put("style", "display: flex; flex-direction: column; gap: 8px; background: #1e293b; border-radius: 12px; padding: 12px;");

        // ─── Tab 1: Perfil ──────────────────────────────────────────────────
        XplNode tab1 = new XplNode("tab");
        tab1.attributes.put("title", "Perfil");
        tab1.attributes.put("closable", "false");
        tab1.attributes.put("style", "padding: 8px 12px; background: #334155; border-radius: 6px;");

        // Conteúdo da tab 1: <p>
        XplNode p1 = new XplNode("p");
        p1.textContent = "Conteúdo do perfil do utilizador...";
        p1.attributes.put("style", "color: #cbd5e1; margin: 0;");
        tab1.addChild(p1);

        // Adiciona a tab ao container
        tabs.addChild(tab1);

        // ─── Tab 2: Segurança ───────────────────────────────────────────────
        XplNode tab2 = new XplNode("tab");
        tab2.attributes.put("title", "Segurança");
        tab2.attributes.put("style", "padding: 8px 12px; background: #334155; border-radius: 6px;");

        // Conteúdo da tab 2: <input type="password">
        XplNode input = new XplNode("input");
        input.attributes.put("type", "password");
        input.attributes.put("placeholder", "Nova senha");
        input.attributes.put("style", "width: 100%; padding: 8px; border-radius: 4px; border: 1px solid #475569; background: #0f172a; color: #e2e8f0;");
        tab2.addChild(input);

        tabs.addChild(tab2);

        return tabs;
    }

    /**
     * Exemplo de como usar no motor UI.
     */
    public static void demo() {
        // Obtém o nó raiz
        XplNode root = createTabsExample();

        // Renderiza com o motor (exemplo)
        // __ui_engine.loadView(root);
        // Ou, se o motor esperar HTML:
        // String html = HtmlGenerator.generate(root);
        // __ui_engine.loadView(html);
    }
}