package com.dic.xsuper.lang.ui.tags.navigation;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.tags.TagFactory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.scene.control.Separator;

import java.util.Map;

/**
 * Tag HTML &lt;nav&gt; – Contêiner de navegação principal.
 * Suporta orientação horizontal (padrão) ou vertical.
 * Pode conter itens de navegação, submenus e links.
 * <p>
 * Atributos suportados:
 * - orientation: "horizontal" | "vertical" (padrão horizontal)
 * - role: "navigation" | "tablist" | "menu"
 */
public class NavTag extends NativeTag {

    private String orientation = "horizontal";
    private String role = "navigation";

    public NavTag(XplNode sourceNode) {
        super(sourceNode);
        parseAttributes();
    }

    private void parseAttributes() {
        Map<String, Object> attrs = sourceNode.attributes;
        if (attrs.containsKey("orientation")) {
            orientation = attrs.get("orientation").toString().toLowerCase();
        }
        if (attrs.containsKey("role")) {
            role = attrs.get("role").toString().toLowerCase();
        }
    }

    @Override
    protected Node createNode() {
        Pane container;
        if ("vertical".equals(orientation)) {
            VBox vbox = new VBox(4);
            vbox.setPadding(new Insets(8));
            vbox.setAlignment(Pos.TOP_LEFT);
            container = vbox;
        } else {
            HBox hbox = new HBox(8);
            hbox.setPadding(new Insets(8, 12, 8, 12));
            hbox.setAlignment(Pos.CENTER_LEFT);
            container = hbox;
        }

        // Aplica estilos CSS
        applyCommonStyles();
        container.setStyle(container.getStyle() +
                "-fx-background-color: #1e293b; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; " +
                "-fx-border-color: #334155; " +
                "-fx-border-width: 1px;"
        );

        // Adiciona filhos
        for (NativeTag child : children) {
            Node childNode = child.build();
            if (container instanceof HBox hbox) {
                hbox.getChildren().add(childNode);
                // Adiciona separador entre itens (exceto no último)
                if (children.indexOf(child) < children.size() - 1) {
                    Separator sep = new Separator();
                    sep.setStyle("-fx-background-color: #475569; -fx-pref-height: 24px;");
                    hbox.getChildren().add(sep);
                }
            } else if (container instanceof VBox vbox) {
                vbox.getChildren().add(childNode);
                // Adiciona separador entre itens
                if (children.indexOf(child) < children.size() - 1) {
                    Separator sep = new Separator();
                    sep.setStyle("-fx-background-color: #475569; -fx-pref-width: 24px;");
                    vbox.getChildren().add(sep);
                }
            }
        }

        // Se estiver vazio, adiciona placeholder
        if (container.getChildren().isEmpty()) {
            Label empty = new Label("(navegação vazia)");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic;");
            container.getChildren().add(empty);
        }

        return container;
    }

    @Override
    protected void applyTagSpecificStyles() {
        // Estilos específicos podem ser aplicados aqui
    }

    @Override
    protected void addChildren() {
        // Os filhos são adicionados no createNode para controlo de separadores
    }

    @Override
    protected void bindEvents() {
        super.bindEvents();
        // Eventos para navegação (ex: onselect)
    }
}