package com.dic.xsuper.lang.ui.tags.media;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.net.URI;
import java.util.Map;

public class AudioTag extends NativeTag {

    private MediaPlayer mediaPlayer;
    private MediaControlBase controls;
    private boolean controlsVisible = true;
    private boolean autoplay = false;
    private boolean loop = false;
    private boolean muted = false;
    private double volume = 1.0;

    public AudioTag(XplNode sourceNode) {
        super(sourceNode);
        parseAttributes();
    }

    private void parseAttributes() {
        Map<String, Object> attrs = sourceNode.attributes;
        if (attrs.containsKey("controls")) {
            controlsVisible = !"false".equalsIgnoreCase(attrs.get("controls").toString());
        }
        if (attrs.containsKey("autoplay")) {
            autoplay = "true".equalsIgnoreCase(attrs.get("autoplay").toString()) || attrs.get("autoplay").equals("");
        }
        if (attrs.containsKey("loop")) {
            loop = "true".equalsIgnoreCase(attrs.get("loop").toString()) || attrs.get("loop").equals("");
        }
        if (attrs.containsKey("muted")) {
            muted = "true".equalsIgnoreCase(attrs.get("muted").toString()) || attrs.get("muted").equals("");
        }
        if (attrs.containsKey("volume")) {
            try { volume = Double.parseDouble(attrs.get("volume").toString()); } catch (Exception ignored) {}
            volume = Math.max(0, Math.min(1, volume));
        }
    }

    @Override
    protected Node createNode() {
        String src = getBestSource();
        VBox container = new VBox(10);
        container.setStyle("-fx-padding: 15px; -fx-background-color: #1a1a2e; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-alignment: center;");

        if (src == null || src.isEmpty()) {
            showError(container, "Nenhuma fonte de áudio especificada.");
            return container;
        }

        try {
            String mediaUrl = resolveMediaUrl(src);
            if (mediaUrl == null) {
                showError(container, "Ficheiro não encontrado: " + src);
                return container;
            }

            Media media = new Media(mediaUrl);
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(volume);
            mediaPlayer.setMute(muted);
            mediaPlayer.setCycleCount(loop ? MediaPlayer.INDEFINITE : 1);

            // Criar controlos
            if (controlsVisible) {
                controls = new MediaControlBase(mediaPlayer);
                // Personalizações (cores, etc.) podem ser aplicadas via CSS
                controls.getContainer().setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-background-radius: 8px;");
                container.getChildren().add(controls.getContainer());
            } else {
                Label icon = new Label("🔊");
                icon.setStyle("-fx-font-size: 48px; -fx-text-fill: #3b82f6;");
                container.getChildren().add(icon);
            }

            // Se autoplay
            if (autoplay) {
                mediaPlayer.setOnReady(() -> mediaPlayer.play());
            }

            // Evento de fim para loop
            mediaPlayer.setOnEndOfMedia(() -> {
                if (loop) mediaPlayer.play();
            });

        } catch (Exception e) {
            showError(container, "Erro ao carregar áudio: " + e.getMessage());
        }

        return container;
    }

    private String getBestSource() {
        String src = (String) sourceNode.attributes.get("src");
        if (src != null && !src.isEmpty()) return src;
        for (XplNode child : sourceNode.children) {
            if ("source".equalsIgnoreCase(child.tag)) {
                String childSrc = (String) child.attributes.get("src");
                if (childSrc != null && !childSrc.isEmpty()) return childSrc;
            }
        }
        return null;
    }

    private String resolveMediaUrl(String src) {
        if (src.startsWith("http://") || src.startsWith("https://") || src.startsWith("file:/")) {
            return src;
        }
        File file = new File(src);
        if (!file.exists()) {
            file = new File("/" + src);
        }
        if (!file.exists()) {
            file = new File(System.getProperty("user.dir"), src);
        }
        if (file.exists()) {
            try {
                return file.toURI().toURL().toString();
            } catch (Exception e) {
                try {
                    return new URI("file", null, file.getAbsolutePath(), null).toString();
                } catch (Exception ex) {
                    return null;
                }
            }
        }
        return null;
    }

    private void showError(VBox container, String message) {
        Label error = new Label("⚠️ " + message);
        error.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 14px; -fx-padding: 10px;");
        container.getChildren().add(error);
    }

    @Override
    protected void applyTagSpecificStyles() {}

    @Override
    protected void addChildren() {}

    @Override
    protected void bindEvents() {
        super.bindEvents();
    }
}