package com.dic.xsuper.lang.ui.animation;

import javafx.util.Duration;

import java.util.*;

/**
 * Representa uma animação @keyframes, com nome, duração, iterações,
 * direção, fill-mode e lista de frames.
 */
public class XplKeyframeAnimation {
    private final String name;
    private Duration duration = Duration.millis(1000);
    private int iterations = 1; // -1 para infinite
    private String direction = "normal"; // normal, reverse, alternate, alternate-reverse
    private String fillMode = "none"; // none, forwards, backwards, both
    private final List<XplKeyframe> keyframes = new ArrayList<>();

    public XplKeyframeAnimation(String name) {
        this.name = name;
    }

    // Getters/Setters
    public String getName() { return name; }
    public Duration getDuration() { return duration; }
    public void setDuration(Duration duration) { this.duration = duration; }
    public int getIterations() { return iterations; }
    public void setIterations(int iterations) { this.iterations = iterations; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getFillMode() { return fillMode; }
    public void setFillMode(String fillMode) { this.fillMode = fillMode; }
    public List<XplKeyframe> getKeyframes() { return keyframes; }

    public void addKeyframe(XplKeyframe frame) {
        keyframes.add(frame);
    }

    /**
     * Ordena os keyframes por posição.
     */
    public void sortKeyframes() {
        keyframes.sort(Comparator.comparingDouble(XplKeyframe::getPosition));
    }

    /**
     * Constrói um `javafx.animation.KeyFrame` para ser usado no `Timeline`.
     * Este método é chamado pelo motor quando a animação é aplicada.
     */
    public javafx.animation.KeyFrame[] toJavaFXKeyFrames(javafx.scene.Node node) {
        // Implementação será feita no motor, pois precisamos aplicar os estilos.
        // Vamos retornar uma lista vazia por enquanto.
        return new javafx.animation.KeyFrame[0];
    }
}