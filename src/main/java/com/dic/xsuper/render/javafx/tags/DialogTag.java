package com.dic.xsuper.render.javafx.tags;

import com.dic.xsuper.dom.node.XplNode;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.util.Duration;

public class DialogTag extends NativeTag {

    private StackPane overlay;
    private VBox contentArea;
    private boolean isOpen = false;
    private boolean closeOnBackdrop = true;
    private boolean closeOnEscape = true;

    public DialogTag(XplNode sourceNode) {
        super(sourceNode);
        if (sourceNode.attributes.containsKey("close-on-backdrop")) {
            closeOnBackdrop = !"false".equalsIgnoreCase(sourceNode.attributes.get("close-on-backdrop").toString());
        }
        if (sourceNode.attributes.containsKey("close-on-escape")) {
            closeOnEscape = !"false".equalsIgnoreCase(sourceNode.attributes.get("close-on-escape").toString());
        }
    }

    @Override
    protected Node createNode() {
        overlay = new StackPane();
        overlay.setVisible(isOpen);
        overlay.setManaged(false); // Mantém-se fora do fluxo normal (position: absolute/fixed)

        // Liga os listeners para ajustar sempre que a janela for redimensionada
        overlay.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.widthProperty().addListener((o, oldW, newW) -> forceUpdateLayout());
                newScene.heightProperty().addListener((o, oldH, newH) -> forceUpdateLayout());
            }
        });

        contentArea = new VBox();
        contentArea.setAlignment(Pos.CENTER);

        overlay.getChildren().add(contentArea);
        StackPane.setAlignment(contentArea, Pos.CENTER);

        // Eventos de fecho W3C
        overlay.setOnMouseClicked(e -> {
            if (closeOnBackdrop && e.getTarget() == overlay) close();
        });

        overlay.setOnKeyPressed(e -> {
            if (closeOnEscape && e.getCode() == KeyCode.ESCAPE) close();
        });

        return overlay;
    }

    // =========================================================================
    // ⭐ A CURA DO LAYOUT (Simulação de 'position: fixed')
    // =========================================================================
    private void forceUpdateLayout() {
        if (overlay.getScene() != null) {
            double w = overlay.getScene().getWidth();
            double h = overlay.getScene().getHeight();

            if (w > 0 && h > 0) {
                // 1. Estica o fundo negro para ocupar o ecrã inteiro
                overlay.resize(w, h);

                // 2. Anula as restrições físicas do Pai (Centragem Perfeita)
                // Isto garante que se o Dialog estiver dentro de uma div com padding,
                // ele puxa a sua própria posição para o (0,0) absoluto da Janela!
                if (overlay.getParent() != null) {
                    Point2D offset = overlay.getParent().sceneToLocal(0, 0);
                    overlay.setTranslateX(offset.getX());
                    overlay.setTranslateY(offset.getY());
                }

                // 3. Força a caixa interior a re-arranjar-se
                overlay.requestLayout();
            }
        }
    }

    @Override
    protected void addChildren() {
        contentArea.getChildren().clear();

        for (NativeTag child : children) {
            Node childNode = child.build();
            if (childNode instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
            }
            contentArea.getChildren().add(childNode);
        }
    }

    @Override
    protected void applyCommonStyles() {
        Node backup = this.fxNode;
        // Aplica o W3C (.meu-modal-neon) apenas à caixa interior!
        this.fxNode = this.contentArea;
        super.applyCommonStyles();

        this.fxNode = backup;
        // Fundo escuro fixo do backdrop
        this.overlay.setStyle("-fx-background-color: rgba(0,0,0,0.65);");
    }

    @Override
    protected void applyTagSpecificStyles() {}

    // =========================================================================
    // REACTIVIDADE E ANIMAÇÕES
    // =========================================================================
    @Override
    public void onReactiveAttributeChange(String attrName, Object newValue) {
        super.onReactiveAttributeChange(attrName, newValue);

        if ("open".equalsIgnoreCase(attrName) || "show".equalsIgnoreCase(attrName)) {
            boolean shouldOpen = "true".equalsIgnoreCase(String.valueOf(newValue));

            if (shouldOpen && !this.isOpen) {
                showModal();
            } else if (!shouldOpen && this.isOpen) {
                close();
            }
        }
    }

    public void showModal() {
        if (!isOpen) {
            isOpen = true;
            overlay.setVisible(true);
            overlay.toFront();

            // ⭐ A SOLUÇÃO: Força o cálculo das coordenadas ANTES da animação!
            forceUpdateLayout();

            overlay.requestFocus();
            animateIn();

            // Opcional: dispara um evento "open" para a engine se estiveres a ouvir
            // dispatchEvent("open", null);
        }
    }

    public void close() {
        if (isOpen) {
            animateOut(() -> {
                isOpen = false;
                overlay.setVisible(false);
                // dispatchEvent("close", null);
            });
        }
    }

    private void animateIn() {
        FadeTransition fade = new FadeTransition(Duration.millis(200), overlay);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(Duration.millis(200), contentArea);
        scale.setFromX(0.85); scale.setFromY(0.85);
        scale.setToX(1); scale.setToY(1);

        new ParallelTransition(fade, scale).play();
    }

    private void animateOut(Runnable onFinished) {
        FadeTransition fade = new FadeTransition(Duration.millis(150), overlay);
        fade.setFromValue(1);
        fade.setToValue(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(150), contentArea);
        scale.setFromX(1); scale.setFromY(1);
        scale.setToX(0.90); scale.setToY(0.90);

        ParallelTransition pt = new ParallelTransition(fade, scale);
        pt.setOnFinished(e -> onFinished.run());
        pt.play();
    }
}