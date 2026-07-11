package com.dic.xsuper.lang.ui.tags.interactive;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.tags.TagFactory;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.Map;

/**
 * Tag HTML &lt;details&gt; convertida para JavaFX.
 * Cria um painel expansível com um cabeçalho (summary) e conteúdo.
 * <p>
 * Atributos suportados:
 * - open: define se o details começa aberto (true/false)
 * - summary: texto do cabeçalho (alternativa a &lt;summary&gt;)
 * <p>
 * Suporta &lt;summary&gt; como primeiro filho para definir o cabeçalho.
 */
public class DetailsTag extends NativeTag {

    private VBox rootContainer;
    private ToggleButton toggleButton;
    private VBox contentContainer;
    private boolean isOpen = false;
    private String summaryText = "Detalhes";
    private Node customSummaryNode = null;

    public DetailsTag(XplNode sourceNode) {
        super(sourceNode);
        parseAttributes();
        processChildren();
    }

    private void parseAttributes() {
        Map<String, Object> attrs = sourceNode.attributes;
        if (attrs.containsKey("open")) {
            isOpen = "true".equalsIgnoreCase(attrs.get("open").toString()) ||
                    attrs.get("open").equals("");
        }
        if (attrs.containsKey("summary")) {
            summaryText = attrs.get("summary").toString();
        }
    }

    private void processChildren() {
        // Procura por <summary> como primeiro filho
        if (!sourceNode.children.isEmpty()) {
            XplNode firstChild = sourceNode.children.get(0);
            if ("summary".equalsIgnoreCase(firstChild.tag)) {
                // Cria um nó visual para o summary a partir do XplNode
                customSummaryNode = TagFactory.create(firstChild).build();
                sourceNode.children.remove(0); // Remove o summary do processamento normal
            }
        }
    }

    @Override
    protected Node createNode() {
        // 1. Container principal com uma borda suave e cantos arredondados
        rootContainer = new VBox(0);
        rootContainer.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e2e8f0; -fx-border-radius: 6px; -fx-background-radius: 6px;");
        rootContainer.setMaxWidth(Double.MAX_VALUE);

        // 2. Cabeçalho (ToggleButton) com fundo cinza claro
        createHeader();

        // 3. Container de conteúdo (Fundo transparente, com recuo elegante na margem esquerda)
        contentContainer = new VBox(8);
        contentContainer.setStyle("-fx-padding: 12px 15px 15px 35px; -fx-background-color: transparent;");
        contentContainer.setMaxWidth(Double.MAX_VALUE);

        for (NativeTag child : children) {
            if (child instanceof SummaryTag) continue;
            Node childNode = child.build();
            contentContainer.getChildren().add(childNode);
        }

        if (contentContainer.getChildren().isEmpty()) {
            Label empty = new Label("(vazio)");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
            contentContainer.getChildren().add(empty);
        }

        rootContainer.getChildren().addAll(toggleButton, contentContainer);

        toggleButton.setSelected(isOpen);
        updateContentVisibility(isOpen);

        applyCommonStyles();
        applyTagSpecificStyles();

        toggleButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            updateContentVisibility(newVal);
            dispatchEvent("toggle", sourceNode.id, newVal);
        });

        return rootContainer;
    }

    private void createHeader() {
        toggleButton = new ToggleButton();
        toggleButton.setMaxWidth(Double.MAX_VALUE);

        // 1. Estilo base cinza claro, cantos arredondados
        String baseStyle = "-fx-background-color: #f1f5f9; -fx-cursor: hand; -fx-alignment: center-left; -fx-padding: 12px 15px; -fx-background-radius: 6px;";
        String hoverStyle = "-fx-background-color: #e2e8f0; -fx-cursor: hand; -fx-alignment: center-left; -fx-padding: 12px 15px; -fx-background-radius: 6px;";

        toggleButton.setStyle(baseStyle);

        // 2. Cores Escuras para o texto (Preto/Cinza Escuro)
        String textColorHex = resolvedStyles.textColor != 0xFF000000 ? toJavaFxCssColor(resolvedStyles.textColor) : "#0f172a";
        String fontSizeStr = (resolvedStyles.fontSize != null && resolvedStyles.fontSize.toPixels(cssContext) > 0)
                ? resolvedStyles.fontSize.toPixels(cssContext) + "px" : "14px";
        String fontWeight = "bold".equals(resolvedStyles.fontWeight) ? "bold" : "normal";

        // 3. O Marcador visual
        Label marker = new Label(isOpen ? "▼ " : "▶ ");
        marker.setStyle("-fx-text-fill: " + textColorHex + "; -fx-font-size: " + fontSizeStr + "; -fx-padding: 0 8px 0 0;");

        HBox graphicContainer = new HBox(marker);
        graphicContainer.setAlignment(Pos.CENTER_LEFT);

        if (customSummaryNode != null) {
            graphicContainer.getChildren().add(customSummaryNode);
        } else {
            String text = summaryText != null && !summaryText.isEmpty() ? summaryText : "Detalhes";
            Label defaultSummary = new Label(text);
            defaultSummary.setStyle("-fx-text-fill: " + textColorHex + "; -fx-font-size: " + fontSizeStr + "; -fx-font-weight: " + fontWeight + ";");
            graphicContainer.getChildren().add(defaultSummary);
        }

        toggleButton.setGraphic(graphicContainer);
        toggleButton.setText("");

        // 4. Hover interativo escurecendo ligeiramente o cinza
        toggleButton.setOnMouseEntered(e -> {
            if (!toggleButton.isSelected()) {
                toggleButton.setStyle(hoverStyle);
            }
        });
        toggleButton.setOnMouseExited(e -> {
            toggleButton.setStyle(baseStyle);
        });

        // 5. Ajustar a borda inferior do cabeçalho quando expandido
        toggleButton.selectedProperty().addListener((obs, old, isSelected) -> {
            marker.setText(isSelected ? "▼ " : "▶ ");
            if (isSelected) {
                // Fica cinza mais escuro e tira o arredondamento inferior
                toggleButton.setStyle(baseStyle + " -fx-background-radius: 6px 6px 0 0; -fx-border-color: transparent transparent #e2e8f0 transparent; -fx-border-width: 0 0 1px 0;");
            } else {
                toggleButton.setStyle(baseStyle);
            }
        });
    }
    private void updateContentVisibility(boolean open) {
        if (contentContainer == null) return;

        // O 'managed' dita se o JavaFX deve reservar espaço físico para este elemento
        contentContainer.setVisible(open);
        contentContainer.setManaged(open);
    }

    private void animateContent(boolean expand) {
        double targetHeight = expand ? contentContainer.prefHeight(-1) : 0;
        // Animação simples usando o ScaleY ou Clip
        // Usamos uma animação de transição para suavizar
        double startHeight = expand ? 0 : contentContainer.getHeight();
        double endHeight = expand ? targetHeight : 0;

        // Simples: usamos a propriedade de opacidade e tamanho via CSS
        // Como alternativa, usamos um Clip com transição
        if (expand) {
            contentContainer.setOpacity(1);
            contentContainer.setScaleY(1);
        } else {
            contentContainer.setOpacity(0);
            contentContainer.setScaleY(0);
        }
        // Para uma animação suave, usamos transição de duração curta.
        // (Código mais avançado com Transition pode ser adicionado depois)
    }

    // ─── Método para abrir/fechar programaticamente ──────────────────────

    public void setOpen(boolean open) {
        if (toggleButton != null) {
            toggleButton.setSelected(open);
        }
    }

    public boolean isOpen() {
        return toggleButton != null && toggleButton.isSelected();
    }

    // ─── Dispacho de eventos para o XPL ──────────────────────────────────

    private void dispatchEvent(String eventName, String targetId, Object payload) {
        // Se tiveres um EngineCallback, chama-o aqui.
        // Por enquanto, apenas log para debug.
        System.out.println("[DetailsTag] Evento: " + eventName + " | ID: " + targetId + " | Payload: " + payload);
        // if (engineCallback != null) engineCallback.onEvent(eventName, targetId, payload);
    }

    @Override
    protected void applyTagSpecificStyles() {
        // Podes adicionar estilos CSS específicos para <details>
        if (rootContainer != null) {
            // Aplica estilo do resolvedStyles ao rootContainer
            // Já aplicado via applyCommonStyles()
        }
    }

    @Override
    protected void addChildren() {
        // Os filhos são adicionados no createNode
    }

    @Override
    protected void bindEvents() {
        super.bindEvents();
        // Adiciona eventos XPL (toggle) se necessário
    }
}