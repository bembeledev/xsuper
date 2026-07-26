package com.dic.xsuper.lang.ui.tags.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.texture.Texture;
import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public abstract class GameTagBase extends NativeTag {

    protected boolean gameReady = false;

    public GameTagBase(XplNode sourceNode) {
        super(sourceNode);
        waitForGame();
    }

    private void waitForGame() {
        new Thread(() -> {
            int attempts = 0;
            while (attempts < 200) {
                try {
                    FXGL.getPhysicsWorld();
                    Platform.runLater(() -> {
                        gameReady = true;
                        onGameReady();
                    });
                    return;
                } catch (Exception e) {
                    // Ignora e continua
                }
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                attempts++;
            }
            System.err.println("[GameTagBase] FXGL não iniciou a tempo.");
        }).start();
    }

    protected void onGameReady() {}

    protected Pane createPlaceholder() {
        Pane placeholder = new Pane();
        placeholder.setManaged(false);
        placeholder.setVisible(false);
        return placeholder;
    }

    protected double parseDouble(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        try { return Double.parseDouble(value.toString()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    protected int parseInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        try { return Integer.parseInt(value.toString()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    protected boolean parseBoolean(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        return "true".equalsIgnoreCase(value.toString());
    }

    protected EntityType parseEntityType(Object value) {
        if (value == null) return EntityType.DEFAULT;
        if (value instanceof EntityType) return (EntityType) value;
        try {
            return EntityType.valueOf(value.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            return EntityType.DEFAULT;
        }
    }

    /**
     * Tenta carregar uma textura a partir do nome do ficheiro.
     * Se falhar, devolve uma forma geométrica colorida como fallback.
     *
     * @param spritePath Caminho da textura (ex: "player.png")
     * @param fallbackColor Cor a usar no fallback
     * @param width Largura da forma de fallback
     * @param height Altura da forma de fallback
     * @return Um Node (Texture ou Rectangle) para usar no view()
     */
    protected Node createFallbackTexture(String spritePath, Color fallbackColor, double width, double height) {
        if (spritePath == null || spritePath.isEmpty()) {
            return new Rectangle(width, height, fallbackColor);
        }

        try {
            return FXGL.texture(spritePath);
        } catch (Exception e) {
            System.out.println("[GameTagBase] Textura não encontrada: " + spritePath + " – usando fallback.");
            return new Rectangle(width, height, fallbackColor);
        }
    }

    @Override
    protected Node createNode() {
        return createPlaceholder();
    }

    @Override
    protected void applyTagSpecificStyles() {}

    @Override
    protected void addChildren() {}
}