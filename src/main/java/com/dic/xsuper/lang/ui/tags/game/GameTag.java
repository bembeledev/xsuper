package com.dic.xsuper.lang.ui.tags.game;

import com.almasb.fxgl.app.FXGLPane;
import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class GameTag extends NativeTag {

    private StackPane container;
    private boolean gameReady = false;

    public GameTag(XplNode sourceNode) {
        super(sourceNode);
        // ⭐ Inicia a FXGL (apenas uma vez)
        EmbeddedGameApp.launchGame();
    }

    @Override
    protected Node createNode() {
        container = new StackPane();
        container.setPrefSize(800, 600);
        container.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Aguarda a FXGL estar pronta e adiciona o pane ao contentor
        new Thread(() -> {
            FXGLPane gamePane = EmbeddedGameApp.getGamePane();
            if (gamePane != null) {
                Platform.runLater(() -> {
                    container.getChildren().addFirst(gamePane);
                    gameReady = true;
                    System.out.println("[GameTag] FXGLPane adicionado ao contentor.");
                });
            } else {
                System.err.println("[GameTag] Falha ao obter FXGLPane.");
            }
        }).start();

        return container;
    }

    @Override
    protected void addChildren() {
        // Aguarda a FXGL estar pronta antes de processar os filhos
        new Thread(() -> {
            while (!EmbeddedGameApp.isReady()) {
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            }
            Platform.runLater(() -> {
                for (NativeTag child : children) {
                    Node childNode = child.build();
                    if (childNode != null) {
                        container.getChildren().add(childNode);
                    }
                }
            });
        }).start();
    }

    @Override
    protected void applyTagSpecificStyles() {
        // Nada específico
    }
}