package com.dic.xsuper.render.javafx.tags.navigation;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;

import java.util.Map;

/**
 * Tag HTML &lt;menuitem&gt; – Item de menu individual.
 * Suporta link, ícone, atalho de teclado e evento de clique.
 * <p>
 * Atributos suportados:
 * - href: URL para navegação
 * - icon: ícone (emoji ou URL de imagem)
 * - shortcut: atalho de teclado (ex: "Ctrl+S")
 * - disabled: desativa o item
 */
public class MenuItemTag extends NativeTag {

    private String label = "";
    private String icon = null;
    private String href = null;
    private String shortcut = null;
    private boolean disabled = false;
    private boolean separator = false;


    public MenuItemTag(XplNode sourceNode) {
        super(sourceNode);
        parseAttributes();
    }

    private void parseAttributes() {
        Map<String, Object> attrs = sourceNode.attributes;
        if (attrs.containsKey("href")) {
            href = attrs.get("href").toString();
        }
        if (attrs.containsKey("icon")) {
            icon = attrs.get("icon").toString();
        }
        if (attrs.containsKey("shortcut")) {
            shortcut = attrs.get("shortcut").toString();
        }
        if (attrs.containsKey("disabled")) {
            disabled = "true".equalsIgnoreCase(attrs.get("disabled").toString()) ||
                    attrs.get("disabled").equals("");
        }
    }

    @Override
    protected Node createNode() {
        // Cria um HBox para alinhar ícone, texto e atalho
        HBox itemBox = new HBox(10);
        itemBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        itemBox.setStyle("-fx-padding: 4px 8px; -fx-cursor: hand;");

        // Ícone
        if (icon != null && !icon.isEmpty()) {
            Label iconLabel = new Label(icon);
            iconLabel.setFont(Font.font(14));
            iconLabel.setStyle("-fx-text-fill: #94a3b8;");
            itemBox.getChildren().add(iconLabel);
        }

        // Texto (principal)
        Label textLabel = new Label();
        if (sourceNode.textContent != null) {
            textLabel.setText(sourceNode.textContent.trim());
        } else {
            textLabel.setText("Item");
        }
        textLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px;");
        itemBox.getChildren().add(textLabel);

        // Atalho (opcional)
        if (shortcut != null && !shortcut.isEmpty()) {
            Label shortcutLabel = new Label(shortcut);
            shortcutLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
            // Empurra o atalho para a direita com um spacer
            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            itemBox.getChildren().add(spacer);
            itemBox.getChildren().add(shortcutLabel);
        }

        // Aplica estilos
        applyCommonStyles();
        applyTagSpecificStyles();

        // Desativa se necessário
        if (disabled) {
            itemBox.setDisable(true);
            itemBox.setStyle(itemBox.getStyle() + "-fx-opacity: 0.4;");
        }

        // Clique (navegação ou evento)
        itemBox.setOnMouseClicked(e -> {
            if (disabled) return;
            if (href != null && !href.isEmpty()) {
                // Navega para o link
                dispatchEvent("navigate", sourceNode.id, href);
            } else {
                // Dispara evento de clique genérico
                dispatchEvent("click", sourceNode.id, sourceNode.textContent);
            }
        });

        return itemBox;
    }

    @Override
    protected void applyTagSpecificStyles() {
        // Estilos específicos para o item de menu
    }

    @Override
    protected void addChildren() {
        // <menuitem> não tem filhos visuais
    }

    @Override
    protected void bindEvents() {
        super.bindEvents();
        // Podes adicionar eventos XPL: onclick, onhover, etc.
    }

    private void dispatchEvent(String eventName, String targetId, Object payload) {
        // Chama o callback da bridge se existir
        System.out.println("[MenuItemTag] Evento: " + eventName + " | ID: " + targetId + " | Payload: " + payload);
        // if (engineCallback != null) engineCallback.onEvent(eventName, targetId, payload);
    }

    // ─── Getters ────────────────────────────────────────────────────────────
    public String getLabel() { return label; }
    public String getIcon() { return icon; }
    public String getHref() { return href; }
    public String getShortcut() { return shortcut; }
    public boolean isDisabled() { return disabled; }
    public boolean isSeparator() { return separator; }
}