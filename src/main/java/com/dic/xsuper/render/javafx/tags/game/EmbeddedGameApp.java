package com.dic.xsuper.render.javafx.tags.game;

import com.almasb.fxgl.app.FXGLPane;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import javafx.application.Platform;
import javafx.stage.StageStyle;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class EmbeddedGameApp extends GameApplication {

    private static FXGLPane gamePane;
    private static volatile boolean ready = false;
    private static final CountDownLatch latch = new CountDownLatch(1);
    private static final AtomicReference<Throwable> error = new AtomicReference<>();
    private static volatile boolean initializing = false;
    private static volatile boolean launched = false;

    public static synchronized void launchGame() {
        if (launched) return;
        if (initializing) return;
        initializing = true;
        launched = true;

        new Thread(() -> {
            try {
                getGamePane();
            } catch (Exception e) {
                error.set(e);
                latch.countDown();
            } finally {
                initializing = false;
            }
        }).start();
    }

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(800);
        settings.setHeight(600);
        settings.setTitle("SuperUI Game");
        settings.setFullScreenAllowed(false);
        settings.setIntroEnabled(false);
        settings.setMainMenuEnabled(false);
        settings.setGameMenuEnabled(false);
        settings.setDeveloperMenuEnabled(false);
        settings.setProfilingEnabled(false);
        settings.setStageStyle(StageStyle.TRANSPARENT);
    }

    public static FXGLPane getGamePane() {
        if (ready) return gamePane;

        if (Platform.isFxApplicationThread()) {
            initGamePane();
        } else {
            try {
                Platform.runLater(EmbeddedGameApp::initGamePane);
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        if (error.get() != null) {
            error.get().printStackTrace();
            return null;
        }
        return gamePane;
    }

    private static synchronized void initGamePane() {
        if (ready) return; // Já inicializado
        try {
            EmbeddedGameApp app = new EmbeddedGameApp();
            gamePane = GameApplication.embeddedLaunch(app);
            ready = true;
            latch.countDown();
            System.out.println("[EmbeddedGameApp] FXGL embedded iniciado com sucesso!");
        } catch (Throwable t) {
            error.set(t);
            latch.countDown();
        }
    }

    public static void shutdown() {
        if (ready && gamePane != null) {
            gamePane.getParent().getChildrenUnmodifiable().remove(gamePane);
            GameApplication.embeddedShutdown();
            ready = false;
            gamePane = null;
            launched = false;
        }
    }

    public static boolean isReady() {
        return ready;
    }
}