package com.dic.xsuper.lang.ui.tags.interactive;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TabsTag extends NativeTag {

    private VBox rootContainer;
    private HBox headerContainer;
    private StackPane contentContainer;

    private final List<TabHeader> headers = new ArrayList<>();
    private final List<Node> contents = new ArrayList<>();

    // Estilos personalizáveis via atributo 'style' do <tabs>
    private String activeColor = "#3b82f6";    // Azul padrão Web
    private String inactiveColor = "#64748b";  // Cinza
    private String activeBg = "transparent";
    private String inactiveBg = "transparent";
    private String hoverBg = "rgba(0,0,0,0.04)";

    public TabsTag(XplNode sourceNode) {
        super(sourceNode);
        parseCustomStyles();
    }

    private void parseCustomStyles() {
        Map<String, String> styles = getRawStyles();
        if (styles.containsKey("tab-active-color")) activeColor = styles.get("tab-active-color");
        if (styles.containsKey("tab-inactive-color")) inactiveColor = styles.get("tab-inactive-color");
        if (styles.containsKey("tab-active-bg")) activeBg = styles.get("tab-active-bg");
        if (styles.containsKey("tab-inactive-bg")) inactiveBg = styles.get("tab-inactive-bg");
        if (styles.containsKey("tab-hover-bg")) hoverBg = styles.get("tab-hover-bg");
    }

    @Override
    protected Node createNode() {
        rootContainer = new VBox();
        rootContainer.setMaxWidth(Double.MAX_VALUE);

        // O cabeçalho onde os botões das abas vão ficar
        headerContainer = new HBox(5);
        headerContainer.setStyle("-fx-border-color: transparent transparent #e2e8f0 transparent; -fx-border-width: 0 0 1px 0; -fx-padding: 0 10px 0 10px;");
        headerContainer.setAlignment(Pos.BOTTOM_LEFT);

        // A caixa onde o conteúdo vai aparecer (StackPane é perfeito para sobreposição rápida)
        contentContainer = new StackPane();
        contentContainer.setMaxWidth(Double.MAX_VALUE);
        contentContainer.setStyle("-fx-padding: 10px 0 0 0;");

        rootContainer.getChildren().addAll(headerContainer, contentContainer);

        applyCommonStyles();
        return rootContainer;
    }

    @Override
    protected void applyTagSpecificStyles() {}

    @Override
    protected void addChildren() {
        if (rootContainer == null) return;

        int index = 0;
        int activeIndex = 0;

        for (NativeTag child : children) {
            if (child instanceof TabTag tabChild) {
                // 1. Extrai o conteúdo e esconde-o por defeito
                Node contentNode = tabChild.build();
                contentNode.setVisible(false);
                contentNode.setManaged(false); // Remove o espaço físico quando oculto

                contents.add(contentNode);
                contentContainer.getChildren().add(contentNode);

                // 2. Cria o botão do cabeçalho customizado
                TabHeader header = createTabHeader(tabChild.getTitle(), index);
                headers.add(header);
                headerContainer.getChildren().add(header.node);

                // 3. Verifica se tem a flag active="true"
                if (tabChild.isActiveByDefault()) {
                    activeIndex = index;
                }

                index++;
            }
        }

        // Ativa a aba padrão (ou a primeira se nenhuma for marcada)
        if (!headers.isEmpty()) {
            activateTab(activeIndex);
        }
    }

    private TabHeader createTabHeader(String title, int index) {
        HBox box = new HBox();
        Label label = new Label(title);

        box.getChildren().add(label);
        box.setCursor(javafx.scene.Cursor.HAND);

        // Estilo Base Inativo
        String baseStyle = "-fx-background-color: " + inactiveBg + "; -fx-padding: 10px 16px; -fx-border-width: 0 0 2px 0; -fx-border-color: transparent;";
        box.setStyle(baseStyle);
        label.setStyle("-fx-text-fill: " + inactiveColor + "; -fx-font-weight: bold; -fx-font-size: 14px;");

        // Eventos Visuais (Hover e Clique)
        box.setOnMouseEntered(e -> {
            if (!box.getProperties().containsKey("active")) {
                box.setStyle(baseStyle.replace(inactiveBg, hoverBg));
            }
        });
        box.setOnMouseExited(e -> {
            if (!box.getProperties().containsKey("active")) {
                box.setStyle(baseStyle);
            }
        });

        box.setOnMouseClicked(e -> activateTab(index));

        return new TabHeader(box, label);
    }

    private void activateTab(int index) {
        for (int i = 0; i < headers.size(); i++) {
            TabHeader header = headers.get(i);
            Node content = contents.get(i);

            if (i == index) {
                // ATIVAR
                header.node.getProperties().put("active", true);
                header.node.setStyle("-fx-background-color: " + activeBg + "; -fx-padding: 10px 16px; -fx-border-width: 0 0 2px 0; -fx-border-color: " + activeColor + ";");
                header.label.setStyle("-fx-text-fill: " + activeColor + "; -fx-font-weight: bold; -fx-font-size: 14px;");

                content.setVisible(true);
                content.setManaged(true);
            } else {
                // DESATIVAR
                header.node.getProperties().remove("active");
                header.node.setStyle("-fx-background-color: " + inactiveBg + "; -fx-padding: 10px 16px; -fx-border-width: 0 0 2px 0; -fx-border-color: transparent;");
                header.label.setStyle("-fx-text-fill: " + inactiveColor + "; -fx-font-weight: bold; -fx-font-size: 14px;");

                content.setVisible(false);
                content.setManaged(false);
            }
        }
    }

    // Estrutura interna para guardar a referência do botão e do texto
    private static class TabHeader {
        HBox node;
        Label label;
        TabHeader(HBox node, Label label) {
            this.node = node;
            this.label = label;
        }
    }
}