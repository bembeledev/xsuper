package com.dic.xsuper.tests.ui;

import com.dic.xsuper.dom.node.XplNode;

/**
 * Exemplo moderno de construção da estrutura <tabs> com estilos personalizados.
 * <p>
 * Corresponde ao HTML:
 * <pre>
 * &lt;tabs style="tab-active-color: #e63946; tab-inactive-color: #a8dadc; tab-active-bg: #f1faee; tab-hover-bg: #f1faee;"&gt;
 *     &lt;tab title="Resumo" style="padding: 20px; display: flex; flex-direction: column;"&gt;
 *         &lt;h2 style="color: #1d3557;"&gt;Estatísticas&lt;/h2&gt;
 *         &lt;p&gt;Bem-vindo ao teu painel principal!&lt;/p&gt;
 *     &lt;/tab&gt;
 *     &lt;tab title="Definições" active="true" style="padding: 20px; background-color: #f8f9fa;"&gt;
 *         &lt;input type="text" placeholder="Nome de utilizador"&gt;
 *         &lt;button&gt;Guardar&lt;/button&gt;
 *     &lt;/tab&gt;
 * &lt;/tabs&gt;
 * </pre>
 */
public class TabExampleModern {

    /**
     * Cria a árvore XplNode para as tabs modernas com estilos personalizados.
     * @return o nó raiz (&lt;tabs&gt;)
     */
    public static XplNode createModernTabs() {
        // ─── Nó <tabs> com estilos de personalização ─────────────────────────
        XplNode tabs = new XplNode("tabs");
        tabs.attributes.put("style",
                "tab-active-color: #e63946; " +
                        "tab-inactive-color: #a8dadc; " +
                        "tab-active-bg: #f1faee; " +
                        "tab-hover-bg: #f1faee; " +
                        "display: flex; " +
                        "flex-direction: column; " +
                        "gap: 4px; " +
                        "background: #ffffff; " +
                        "border-radius: 12px; " +
                        "padding: 16px; " +
                        "box-shadow: 0 4px 12px rgba(0,0,0,0.05);"
        );

        // ─── Tab 1: Resumo ──────────────────────────────────────────────────
        XplNode tabResumo = new XplNode("tab");
        tabResumo.attributes.put("title", "Resumo");
        tabResumo.attributes.put("style",
                "padding: 20px; " +
                        "display: flex; " +
                        "flex-direction: column; " +
                        "gap: 12px; " +
                        "background: #f8f9fa; " +
                        "border-radius: 8px; " +
                        "border-left: 4px solid #e63946;"
        );

        // Conteúdo da tab Resumo
        XplNode h2 = new XplNode("h2");
        h2.textContent = "Estatísticas";
        h2.attributes.put("style", "color: #1d3557; margin: 0; font-size: 24px; font-weight: 700;");
        tabResumo.addChild(h2);

        XplNode p = new XplNode("p");
        p.textContent = "Bem-vindo ao teu painel principal!";
        p.attributes.put("style", "color: #457b9d; margin: 0; font-size: 16px;");
        tabResumo.addChild(p);

        // Adiciona a tab ao container
        tabs.addChild(tabResumo);

        // ─── Tab 2: Definições (ativa) ──────────────────────────────────────
        XplNode tabDefinicoes = new XplNode("tab");
        tabDefinicoes.attributes.put("title", "Definições");
        tabDefinicoes.attributes.put("active", "true");  // Começa aberta
        tabDefinicoes.attributes.put("style",
                "padding: 20px; " +
                        "display: flex; " +
                        "flex-direction: column; " +
                        "gap: 12px; " +
                        "background-color: #f8f9fa; " +
                        "border-radius: 8px; " +
                        "border-left: 4px solid #e63946;"
        );

        // Conteúdo da tab Definições
        XplNode input = new XplNode("input");
        input.attributes.put("type", "text");
        input.attributes.put("placeholder", "Nome de utilizador");
        input.attributes.put("style",
                "padding: 10px 14px; " +
                        "border: 1px solid #ced4da; " +
                        "border-radius: 6px; " +
                        "font-size: 14px; " +
                        "background: #ffffff; " +
                        "color: #212529;"
        );
        tabDefinicoes.addChild(input);

        XplNode button = new XplNode("button");
        button.textContent = "Guardar";
        button.attributes.put("style",
                "padding: 10px 20px; " +
                        "background: #e63946; " +
                        "color: #ffffff; " +
                        "border: none; " +
                        "border-radius: 6px; " +
                        "font-weight: 600; " +
                        "font-size: 14px; " +
                        "cursor: pointer; " +
                        "transition: background 0.2s;"
        );
        tabDefinicoes.addChild(button);

        tabs.addChild(tabDefinicoes);

        return tabs;
    }

    /**
     * Exemplo de como usar no motor UI.
     */
    public static void demo() {
        XplNode root = createModernTabs();
        // Renderiza com o motor (exemplo)
        // __ui_engine.loadView(root);
        // Ou, se o motor esperar HTML:
        // String html = HtmlGenerator.generate(root);
        // __ui_engine.loadView(html);
    }
}