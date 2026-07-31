package com.dic.xsuper.render.javafx.tags;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.event.UiEventPublisher;
import io.github.hugoquinn2.fxpopup.controller.FxPopup;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class PopupTag extends NativeTag {

    private StackPane contentWrapper;
    private FxPopup popup;
    private Node ghostNode;

    // Os dois cronómetros da vida do Popup
    private PauseTransition hideTimer;
    private PauseTransition delayTimer; // ⭐ NOVO: Timer para abertura automática

    private boolean isContentAdded = false;

    public PopupTag(XplNode node) {
        super(node);
    }

    @Override
    protected Node createNode() {
        contentWrapper = new StackPane();
        contentWrapper.setStyle("-fx-background-color: transparent;");
        // A Cura da Altura: Obriga a caixa a encolher ao tamanho W3C
        contentWrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        popup = new FxPopup();

        ghostNode = new StackPane();
        ghostNode.setManaged(false);
        ghostNode.setVisible(false);

        return ghostNode;
    }

    @Override
    protected void addChildren() {
        for (NativeTag child : children) {
            Node childNode = child.build();
            if (childNode != null) {
                contentWrapper.getChildren().add(childNode);
            }
        }
    }

    @Override
    protected void bindEvents() {
        super.bindEvents();

        // ⭐ A MÁGICA DO TIMEOUT DE ABERTURA (Notificações automáticas)
        if (sourceNode.attributes.containsKey("delay")) {
            try {
                int delayMs = Integer.parseInt(sourceNode.attributes.get("delay").toString());

                // Só inicia a contagem quando o nó for efetivamente colado na janela gráfica
                ghostNode.sceneProperty().addListener((obs, oldScene, newScene) -> {
                    if (newScene != null) {
                        delayTimer = new PauseTransition(Duration.millis(delayMs));
                        delayTimer.setOnFinished(e -> mostrarPopup());
                        delayTimer.playFromStart();
                    }
                });

            } catch (NumberFormatException e) {
                System.err.println("[PopupTag] Erro: O atributo delay deve ser um número inteiro (ms).");
            }
        }
    }

    @Override
    public void onReactiveAttributeChange(String attrName, Object newValue) {
        super.onReactiveAttributeChange(attrName, newValue);

        if ("show".equalsIgnoreCase(attrName)) {
            boolean isVisible = Boolean.parseBoolean(String.valueOf(newValue));
            Platform.runLater(() -> {
                if (isVisible) {
                    mostrarPopup();
                } else {
                    esconderPopupNativo();
                }
            });
        }
    }

    private void esconderPopupNativo() {
        if (contentWrapper != null && contentWrapper.getParent() instanceof Pane pai) {
            pai.getChildren().remove(contentWrapper);
        }
    }

    private void mostrarPopup() {
        esconderPopupNativo(); // Prevenção de duplicados

        if (!isContentAdded) {
            popup.add(contentWrapper);
            isContentAdded = true;
        }

        String anchorId = sourceNode.attributes.getOrDefault("anchor", "").toString();

        if (!anchorId.isEmpty()) {
            XplNode anchorXplNode = findNodeByIdLocal(anchorId);

            if (anchorXplNode != null && anchorXplNode.nativeNode instanceof Node fxAnchor) {
                popup.show(Pos.TOP_LEFT, contentWrapper);

                Platform.runLater(() -> {
                    Bounds bounds = fxAnchor.localToScene(fxAnchor.getBoundsInLocal());
                    if (bounds != null) {
                        contentWrapper.setTranslateX(bounds.getMinX());
                        contentWrapper.setTranslateY(bounds.getMaxY() + 5);
                    }
                });

                iniciarTimeout();
                return;
            }
        }

        // Fallback: Centro do ecrã
        if (ghostNode.getScene() != null && ghostNode.getScene().getWindow() != null) {
            popup.show(contentWrapper);
            iniciarTimeout();
        }
    }

    private void iniciarTimeout() {
        Object timeoutAttr = sourceNode.attributes.get("timeout");
        if (timeoutAttr == null) return;

        try {
            int millis = Integer.parseInt(timeoutAttr.toString());

            if (hideTimer != null) {
                hideTimer.stop();
            }

            hideTimer = new PauseTransition(Duration.millis(millis));
            hideTimer.setOnFinished(e -> {
                // ⭐ NOVO: Remove da vista nativa imediatamente, mesmo se o motor XPL estiver ocupado
                esconderPopupNativo();

                if (getId() != null) {
                    UiEventPublisher.publishUiInteracted(getId(), "close", null);
                }
            });
            hideTimer.playFromStart();

        } catch (NumberFormatException e) {
            System.err.println("[PopupTag] Erro: O atributo timeout deve ser um número inteiro (ms).");
        }
    }

    @Override protected void applyCommonStyles() {}
    @Override protected void applyTagSpecificStyles() {}
}