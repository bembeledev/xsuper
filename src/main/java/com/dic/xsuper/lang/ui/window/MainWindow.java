package com.dic.xsuper.lang.ui.window;

import com.dic.xsuper.lang.debug.XplNodeDebugger;
import com.dic.xsuper.lang.ui.JavaFxRenderer;
import com.dic.xsuper.lang.ui.SuperUiEngine;
import com.dic.xsuper.lang.ui.css.media.JavaFxMediaListener;
import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;

import java.util.concurrent.CountDownLatch;

/**
 * Gestor da janela principal da aplicação.
 * Isola toda a lógica de criação e gestão da cena JavaFX.
 */
public class MainWindow {

    private final SuperUiEngine engine;
    private final JavaFxMediaListener mediaListener;

    // Ponte de renderização (guardada localmente para referência)
    private JavaFxRenderer rendererBridge;

    // Estado da janela
    private Stage primaryStage;
    private Scene currentScene;

    public MainWindow(SuperUiEngine engine) {
        this.engine = engine;
        // O media listener já está na engine; podemos aceder via getter
        this.mediaListener = engine.getMediaListener();
    }

    /**
     * Abre a janela principal com o título e dimensões especificados.
     * Este método é chamado a partir do XPL (via __ui_engine.showWindow).
     */
    public void showWindow(String title, double width, double height) {
        System.out.println("[MainWindow] A abrir janela: " + title);

        // Inicializa o toolkit JavaFX se ainda não estiver
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit já iniciado
        }

        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            // 1. Cria a cena e o contentor raiz
            Stage stage = new Stage();
            this.primaryStage = stage;

            // 2. Obtém o <body> da árvore ativa (já deve estar renderizada)
            XplNode activeDom = engine.getActiveDom();
            XplNode bodyNode = XplNode.findBodyNode(activeDom);
            if (bodyNode == null) {
                bodyNode = activeDom; // fallback
            }

            // 3. Cria o contentor JavaFX que representará o <body>
            javafx.scene.layout.VBox fxBody = new javafx.scene.layout.VBox();
            fxBody.setStyle("-fx-background-color: #f0f2f5; -fx-padding: 0;");

            // 4. Liga a ponte de renderização (JavaFxRenderer)
            this.rendererBridge = new JavaFxRenderer(fxBody);
            engine.setRendererBridge(this.rendererBridge);

            // 5. (Opcional) Debug da árvore
            XplNodeDebugger.debbug(bodyNode);

            // 6. Cria a ScrollPane (para permitir rolagem) e a Scene
            ScrollPane scroll = new ScrollPane(fxBody);
            scroll.setFitToWidth(true);
            Scene scene = new Scene(scroll, width, height);
            this.currentScene = scene;

            // 7. Anexa o listener de media queries à cena
            mediaListener.attachToScene(scene);

            // 8. Configura e mostra a janela
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();

            // 9. Força um ciclo de renderização completo com a largura atual
            //    (garante que as media queries são aplicadas imediatamente)
            engine.renderCycle();

            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Força a reconstrução total do layout (hard reflow).
     * Chamado pelo media listener quando uma media query estrutural muda.
     */
    public void forceLayoutRebuild() {
        System.out.println("[MainWindow] ⚠️ Hard Reflow solicitado pelo MediaListener.");

        // 1. Executa o renderCycle para atualizar a árvore virtual
        engine.renderCycle();

        // 2. Obtém a nova árvore ativa e o <body>
        XplNode activeDom = engine.getActiveDom();
        XplNode bodyNode = XplNode.findBodyNode(activeDom);
        if (bodyNode == null) bodyNode = activeDom;

        // 3. Reconstroi a UI a partir do zero (se a ponte existir)
        if (rendererBridge != null) {
            XplNode finalBodyNode = bodyNode;
            Platform.runLater(() -> rendererBridge.rebuildFullView(finalBodyNode));
        }

    }

    // ─── GETTERS (para a SuperUiEngine poder aceder, se necessário) ────

    public JavaFxRenderer getRendererBridge() {
        return rendererBridge;
    }

    public Scene getCurrentScene() {
        return currentScene;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

}