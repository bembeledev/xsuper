package com.dic.xsuper.render.javafx.tags.navigation;

import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCombination;

public class MenuHelper {

    /**
     * Constrói recursivamente um Menu e todos os seus sub-menus.
     */
    public static Menu buildMenu(XplNode node) {
        String label = (String) node.attributes.getOrDefault("label", "Menu");
        Menu menu = new Menu(label);

        // ⭐ INJEÇÃO DE ÍCONES PARA SUB-MENUS
        String icon = (String) node.attributes.get("icon");
        if (icon != null && !icon.isEmpty()) {
            Label iconLabel = new Label(icon);
            iconLabel.setStyle("-fx-font-size: 14px;");
            menu.setGraphic(iconLabel);
        }

        if (node.attributes.containsKey("disabled") && "true".equalsIgnoreCase(node.attributes.get("disabled").toString())) {
            menu.setDisable(true);
        }

        // Lê os filhos crus (XplNode) sem precisar da TagFactory!
        for (XplNode child : node.children) {
            if ("menuitem".equalsIgnoreCase(child.tag)) {
                menu.getItems().add(buildMenuItem(child));
            } else if ("menu".equalsIgnoreCase(child.tag)) {
                menu.getItems().add(buildMenu(child)); // Recursividade para Sub-menus
            }
        }
        return menu;
    }

    /**
     * Constrói um item clicável com tratamento seguro de atalhos.
     */
    public static MenuItem buildMenuItem(XplNode node) {
        // Suporte para separadores <menuitem separator="true">
        if (node.attributes.containsKey("separator") || "hr".equalsIgnoreCase(node.tag)) {
            return new SeparatorMenuItem();
        }

        // Extrai o texto do atributo ou do corpo da tag
        String label = (String) node.attributes.getOrDefault("label", "Item");
        if (node.textContent != null && !node.textContent.trim().isEmpty()) {
            label = node.textContent.trim();
        }

        MenuItem item = new MenuItem(label);

        // ⭐ INJEÇÃO DE ÍCONES PARA ITENS
        String icon = (String) node.attributes.get("icon");
        if (icon != null && !icon.isEmpty()) {
            Label iconLabel = new Label(icon);
            iconLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;"); // Cor neutra para o ícone
            item.setGraphic(iconLabel);
        }

        // Atalhos de teclado (com normalização segura para o sinal de mais ++)
        if (node.attributes.containsKey("shortcut")) {
            String rawShortcut = node.attributes.get("shortcut").toString().trim();
            try {
                // Normaliza "Ctrl++" para "Ctrl+PLUS" que o JavaFX compreende perfeitamente
                String normalizedShortcut = rawShortcut;
                if (rawShortcut.endsWith("++")) {
                    normalizedShortcut = rawShortcut.substring(0, rawShortcut.length() - 2) + "+PLUS";
                } else if (rawShortcut.equals("Ctrl++") || rawShortcut.equals("Ctrl+ +")) {
                    normalizedShortcut = "Ctrl+PLUS";
                }

                item.setAccelerator(KeyCombination.keyCombination(normalizedShortcut));
            } catch (IllegalArgumentException e) {
                System.err.println("[MenuHelper] Aviso: Não foi possível parsear o atalho '" + rawShortcut + "': " + e.getMessage());
            }
        }

        if (node.attributes.containsKey("disabled") && "true".equalsIgnoreCase(node.attributes.get("disabled").toString())) {
            item.setDisable(true);
        }

        // Eventos e Navegação integrados
        item.setOnAction(e -> {
            String href = (String) node.attributes.get("href");
            String onclick = (String) node.attributes.get("onclick");

            if (onclick != null) {
                System.out.println("[Menu Event] A disparar evento XPL: " + onclick);
            } else if (href != null) {
                System.out.println("[Menu Event] A navegar para vista interna: " + href);
            }
        });

        return item;
    }
}