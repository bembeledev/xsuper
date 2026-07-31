package com.dic.xsuper.render.javafx.window;

import com.dic.xsuper.dom.debug.XplNodeDebugger;
import com.dic.xsuper.render.javafx.core.JavaFxRenderer;
import com.dic.xsuper.render.javafx.core.SuperUiEngine;
import com.dic.xsuper.render.javafx.css.media.JavaFxMediaListener;
import com.dic.xsuper.dom.node.XplNode;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.Base64;
import java.util.concurrent.CountDownLatch;

/**
 * Gestor da janela principal da aplicação.
 * Isola toda a lógica de criação e gestão da cena JavaFX.
 */
public class MainWindow {

    private final SuperUiEngine engine;
    private final JavaFxMediaListener mediaListener;

    private JavaFxRenderer rendererBridge;
    private Stage primaryStage;
    private Scene currentScene;

    public MainWindow(SuperUiEngine engine) {
        this.engine = engine;
        this.mediaListener = engine.getMediaListener();
    }

    public void showWindow(String title, double requestedWidth, double requestedHeight) {
        System.out.println("[MainWindow] A abrir janela: " + title);

        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit já iniciado
        }

        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            Stage stage = new Stage();
            this.primaryStage = stage;

            XplNode activeDom = engine.getActiveDom();
            XplNode bodyNode = XplNode.findBodyNode(activeDom);
            if (bodyNode == null) bodyNode = activeDom;

            javafx.scene.layout.VBox fxBody = new javafx.scene.layout.VBox();

            // Permite que o VBox cresça infinitamente para baixo para ativar o Scroll!
            fxBody.setFillWidth(true);
            fxBody.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

            this.rendererBridge = new JavaFxRenderer(fxBody);
            engine.setRendererBridge(this.rendererBridge);

            //imprime as dimensoes
            XplNodeDebugger.debugDimension(bodyNode);
            //imprime a arvore DOM
            XplNodeDebugger.debugTreeDOM(bodyNode);



            // ⭐ 1. CRIAÇÃO DO SCROLL PAN COM FUNDO TRANSPARENTE
            ScrollPane scroll = new ScrollPane(fxBody);
            //scroll.setFitToWidth(true); // O conteúdo adapta-se à largura da janela
            // NÃO faças setFitToHeight(true) senão bloqueias o scroll vertical!

            // ⭐ INJEÇÃO DE DEPENDÊNCIA (Provider Pattern)
            // Guardamos o AnimationManager no topo da árvore visual da janela!
            scroll.getProperties().put("xpl_animation_manager", engine.getAnimationManager());

            // ⭐ A CURA: O comportamento Web Real (Top-Down Box Model)
            // O body (<VBox>) terá o tamanho EXATO da área visível do ecrã.
            // Isto impede que nós de texto gulosos expandam a página para o infinito horizontalmente.
            scroll.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
                double viewportWidth = newVal.getWidth();
                double viewportHeight = newVal.getHeight();

                // ⭐ 1. INJEÇÃO TOP-DOWN (O pai dita a regra)
                fxBody.setMinWidth(viewportWidth);
                fxBody.setPrefWidth(viewportWidth);
                fxBody.setMaxWidth(viewportWidth); // O Segredo Mestre: Bloqueia a largura!

                // 2. A altura continua flexível para permitir o scroll vertical,
                // mas com um mínimo para pintar o fundo do ecrã inteiro.
                fxBody.setMinHeight(viewportHeight);
            });

            // Força a barra de scroll horizontal a aparecer apenas quando necessário
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

            scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

            // ⭐ 2. INTELIGÊNCIA DE RESOLUÇÃO (Não deixa a janela estourar o monitor!)
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            double finalWidth = Math.min(requestedWidth, screenBounds.getWidth());
            // Subtraímos 40px para garantir que não fica por baixo da barra de tarefas do Windows/Mac
            double finalHeight = Math.min(requestedHeight, screenBounds.getHeight() - 40);

            Scene scene = new Scene(scroll, finalWidth, finalHeight);

            // ⭐ 3. A MAGIA DO SCROLLBAR MODERNO WEB (Base64 Injection)
            // Aplica um scroll flutuante, escuro, arredondado e sem os botões com setas feios.
            String modernScrollCss =
                    ".scroll-pane { -fx-background-color: transparent; -fx-background: transparent; }\n" +
                            ".scroll-pane > .viewport { -fx-background-color: transparent; }\n" +
                            ".scroll-bar:horizontal, .scroll-bar:vertical { -fx-background-color: transparent; }\n" +
                            ".scroll-bar:vertical { -fx-pref-width: 8px; }\n" +
                            ".scroll-bar:horizontal { -fx-pref-height: 8px; }\n" +
                            ".scroll-bar:horizontal .track, .scroll-bar:vertical .track { -fx-background-color: transparent; -fx-border-color: transparent; }\n" +
                            ".scroll-bar:horizontal .thumb, .scroll-bar:vertical .thumb { -fx-background-color: #475569; -fx-background-radius: 10px; }\n" +
                            ".scroll-bar:horizontal .thumb:hover, .scroll-bar:vertical .thumb:hover { -fx-background-color: #94a3b8; }\n" +
                            ".scroll-bar:horizontal .increment-button, .scroll-bar:horizontal .decrement-button,\n" +
                            ".scroll-bar:vertical .increment-button, .scroll-bar:vertical .decrement-button { -fx-background-color: transparent; -fx-pref-width: 0; -fx-pref-height: 0; -fx-padding: 0; }\n" +
                            ".scroll-bar:horizontal .increment-arrow, .scroll-bar:horizontal .decrement-arrow,\n" +
                            ".scroll-bar:vertical .increment-arrow, .scroll-bar:vertical .decrement-arrow { -fx-shape: \" \"; -fx-padding: 0; }";

            String cssUri = "data:text/css;charset=utf-8;base64," + Base64.getEncoder().encodeToString(modernScrollCss.getBytes());
            scene.getStylesheets().add(cssUri);

            // Fundo escuro padrão caso a tag Body não traga cor
            scene.setFill(javafx.scene.paint.Color.web("#050505"));

            // Listeners de Redimensionamento
            engine.setViewportSize(finalWidth, finalHeight);
            scene.widthProperty().addListener((obs, oldVal, newVal) -> engine.setViewportSize(newVal.doubleValue(), scene.getHeight()));
            scene.heightProperty().addListener((obs, oldVal, newVal) -> engine.setViewportSize(scene.getWidth(), newVal.doubleValue()));

            mediaListener.attachToScene(scene);

            stage.setTitle(title);
            stage.setScene(scene);

            // Se ainda assim for muito grande, maximiza automaticamente
            if (finalWidth >= screenBounds.getWidth() && finalHeight >= screenBounds.getHeight() - 40) {
                stage.setMaximized(true);
            }

            stage.show();
            engine.renderCycle();

            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    public void forceLayoutRebuild() {
        engine.renderCycle();
        if (rendererBridge != null) {
            Platform.runLater(() -> rendererBridge.rebuildFullView(engine.getActiveDom()));
        }
    }

    public JavaFxRenderer getRendererBridge() { return rendererBridge; }
    public Scene getCurrentScene() { return currentScene; }
    public Stage getPrimaryStage() { return primaryStage; }
}