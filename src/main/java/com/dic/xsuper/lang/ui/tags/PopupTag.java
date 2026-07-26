package com.dic.xsuper.lang.ui.tags;

import com.dic.xsuper.lang.ui.html.XplNode;
import io.github.hugoquinn2.fxpopup.controller.FxPopup;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class PopupTag extends NativeTag {

    private StackPane contentWrapper;
    private FxPopup popup;
    private Node ghostNode;
    private PauseTransition hideTimer;

    // ⭐ NOVO: Flag para garantir que só adicionamos o conteúdo quando for seguro
    private boolean isContentAdded = false;

    public PopupTag(XplNode node) {
        super(node);
    }

    @Override
    protected Node createNode() {
        contentWrapper = new StackPane();
        contentWrapper.setStyle("-fx-background-color: transparent;");

        popup = new FxPopup();

        // ⭐ REMOVIDO DAQUI: popup.add(contentWrapper);
        // Evita que a biblioteca tente inicializar-se assincronamente antes da UI estar pronta.

        ghostNode = new StackPane();
        ghostNode.setManaged(false);
        ghostNode.setVisible(false);

        return ghostNode;
    }

    @Override
    protected void addChildren() {
        for (NativeTag child : children) {
            // ⭐ CORREÇÃO 1: Construir o filho ativamente (build) em vez de o pedir passivamente (getNode)
            Node childNode = child.build();
            if (childNode != null) {
                contentWrapper.getChildren().add(childNode);
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
                }
            });
        }
    }

    private void mostrarPopup() {
        // ⭐ CORREÇÃO 2: Lazy Loading. Injetamos os teus nós W3C na biblioteca
        // apenas na primeira vez que o balão é disparado!
        if (!isContentAdded) {
            popup.add(contentWrapper);
            isContentAdded = true;
        }

        String anchorId = sourceNode.attributes.getOrDefault("anchor", "").toString();

        if (!anchorId.isEmpty()) {
            com.dic.xsuper.lang.ui.SuperUiEngine engine = com.dic.xsuper.lang.ui.SuperUiEngine.getInstance();
            if (engine != null && engine.getActiveDom() != null) {
                com.dic.xsuper.lang.ui.html.XplNode anchorXplNode = engine.getActiveDom().getElementById(anchorId);

                if (anchorXplNode != null && anchorXplNode.nativeNode instanceof Node fxAnchor) {
                    popup.show(fxAnchor);
                    iniciarTimeout();
                    return;
                }
            }
        }

        // Fallback: Centro do ecrã
        if (ghostNode.getScene() != null && ghostNode.getScene().getWindow() != null) {
            popup.show(ghostNode);
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

                if (getId() != null) {
                    com.dic.xsuper.lang.ui.SuperUiEngine.getInstance()
                            .dispatchEvent("close", null, getId());
                }
            });
            hideTimer.playFromStart();

        } catch (NumberFormatException e) {
            System.err.println("[PopupTag] Erro: O atributo timeout deve ser um número inteiro (milissegundos).");
        }
    }

    @Override protected void applyCommonStyles() {}
    @Override protected void applyTagSpecificStyles() {}
}