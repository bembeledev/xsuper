package com.dic.xsuper.render.javafx.animation;
import javafx.util.Duration;
import java.util.*;

/**
 * Representa uma animação @keyframes, com nome, duração, iterações,
 * direcção, fill-mode e lista de frames.
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

    /**
     * Lê as partes da string de 'animation' e tenta descobrir o que é duração, fill-mode, iterações, etc.
     */
    private Map<String, String> extractAnimationOverrides(String[] parts) {
        Map<String, String> overrides = new java.util.HashMap<>();

        for (int i = 1; i < parts.length; i++) {
            String p = parts[i].toLowerCase();
            // É tempo? (Duração)
            if (p.endsWith("ms") || p.endsWith("s")) {
                // Se já tivermos duração, poderíamos assumir delay, mas vamos focar na duração principal
                if (!overrides.containsKey("duration")) overrides.put("duration", p);
            }
            // É fill-mode?
            else if (p.equals("forwards") || p.equals("backwards") || p.equals("both") || p.equals("none")) {
                overrides.put("fill-mode", p);
            }
            // É direção?
            else if (p.equals("normal") || p.equals("reverse") || p.equals("alternate") || p.equals("alternate-reverse")) {
                overrides.put("direction", p);
            }
            // É iterações?
            else if (p.equals("infinite")) {
                overrides.put("iterations", "-1");
            } else if (p.matches("\\d+")) {
                overrides.put("iterations", p);
            }
        }
        return overrides;
    }
}