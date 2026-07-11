package com.dic.xsuper.tests.ui;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.tags.TagFactory;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;

/**
 * Teste para listas HTML (ul e ol) com diferentes estilos.
 */
public class TestLists  {
    public static XplNode createListTestTree() {
        XplNode container = new XplNode("div");
        container.attributes.put("style", """
                padding: 40px;
                max-width: 700px;
                margin: 0 auto;
                background: white;
                border-radius: 16px;
                box-shadow: 0 8px 30px rgba(0,0,0,0.08);
                font-family: 'Segoe UI', system-ui, sans-serif;
            """);

        // ─── Título ──────────────────────────────────────────────────────
        XplNode title = new XplNode("h1");
        title.textContent = "📋 Teste de Listas HTML";
        title.attributes.put("style", "text-align: center; color: #1e293b; font-size: 28px; margin-bottom: 30px;");
        container.addChild(title);

        // ─── SECÇÃO 1: Lista não ordenada (ul) ──────────────────────────
        container.addChild(createSection("Lista Não Ordenada (ul)", createUlExample()));

        // ─── SECÇÃO 2: Lista ordenada (ol) ──────────────────────────────
        container.addChild(createSection("Lista Ordenada (ol)", createOlExample()));

        // ─── SECÇÃO 3: Lista não ordenada com customização ──────────────
        container.addChild(createSection("Lista Não Ordenada (circle)", createUlCircleExample()));

        // ─── SECÇÃO 4: Lista ordenada com romanos ──────────────────────
        container.addChild(createSection("Lista Ordenada (lower-roman)", createOlRomanExample()));

        // ─── SECÇÃO 5: Lista aninhada ──────────────────────────────────
        container.addChild(createSection("Lista Aninhada", createNestedListExample()));

        return container;
    }

    private static XplNode createSection(String title, XplNode content) {
        XplNode section = new XplNode("div");
        section.attributes.put("style", "margin-bottom: 25px;");

        XplNode header = new XplNode("h2");
        header.textContent = title;
        header.attributes.put("style", "font-size: 18px; font-weight: 600; color: #334155; margin-bottom: 10px; border-bottom: 2px solid #e2e8f0; padding-bottom: 6px;");
        section.addChild(header);
        section.addChild(content);

        return section;
    }

    // ─── Exemplo 1: ul padrão ──────────────────────────────────────────

    private static XplNode createUlExample() {
        XplNode ul = new XplNode("ul");
        ul.attributes.put("style", "padding: 16px; background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0;");

        ul.addChild(createLi("Item 1"));
        ul.addChild(createLi("Item 2"));
        ul.addChild(createLi("Item 3"));
        ul.addChild(createLi("Item 4"));

        return ul;
    }

    // ─── Exemplo 2: ol padrão ──────────────────────────────────────────

    private static XplNode createOlExample() {
        XplNode ol = new XplNode("ol");
        ol.attributes.put("style", "padding: 16px; background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0;");

        ol.addChild(createLi("Primeiro"));
        ol.addChild(createLi("Segundo"));
        ol.addChild(createLi("Terceiro"));

        return ol;
    }

    // ─── Exemplo 3: ul com circle ──────────────────────────────────────

    private static XplNode createUlCircleExample() {
        XplNode ul = new XplNode("ul");
        ul.attributes.put("style", "list-style-type: circle; padding: 16px; background: #f0fdf4; border-radius: 8px; border: 1px solid #bbf7d0;");

        ul.addChild(createLi("Maçã"));
        ul.addChild(createLi("Banana"));
        ul.addChild(createLi("Laranja"));
        ul.addChild(createLi("Uva"));

        return ul;
    }

    // ─── Exemplo 4: ol com lower-roman ─────────────────────────────────

    private static XplNode createOlRomanExample() {
        XplNode ol = new XplNode("ol");
        ol.attributes.put("style", "list-style-type: lower-roman; padding: 16px; background: #fefce8; border-radius: 8px; border: 1px solid #fef08a;");

        ol.addChild(createLi("Capítulo I"));
        ol.addChild(createLi("Capítulo II"));
        ol.addChild(createLi("Capítulo III"));
        ol.addChild(createLi("Capítulo IV"));

        return ol;
    }

    // ─── Exemplo 5: lista aninhada ─────────────────────────────────────

    private static XplNode createNestedListExample() {
        XplNode ul = new XplNode("ul");
        ul.attributes.put("style", "padding: 16px; background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0;");

        // Item 1
        XplNode li1 = new XplNode("li");
        li1.textContent = "Frutas";
        ul.addChild(li1);

        // Sublista (ul aninhada)
        XplNode subUl = new XplNode("ul");
        subUl.attributes.put("style", "list-style-type: circle; padding-left: 25px;");
        subUl.addChild(createLi("Maçã"));
        subUl.addChild(createLi("Banana"));
        subUl.addChild(createLi("Laranja"));
        ul.addChild(subUl);

        // Item 2
        XplNode li2 = new XplNode("li");
        li2.textContent = "Legumes";
        ul.addChild(li2);

        // Sublista (ol aninhada)
        XplNode subOl = new XplNode("ol");
        subOl.attributes.put("style", "list-style-type: lower-alpha; padding-left: 25px;");
        subOl.addChild(createLi("Cenoura"));
        subOl.addChild(createLi("Brócolis"));
        subOl.addChild(createLi("Espinafre"));
        ul.addChild(subOl);

        return ul;
    }

    // ─── Utilitário para criar <li> ────────────────────────────────────

    private static XplNode createLi(String text) {
        XplNode li = new XplNode("li");
        li.textContent = text;
        return li;
    }
}