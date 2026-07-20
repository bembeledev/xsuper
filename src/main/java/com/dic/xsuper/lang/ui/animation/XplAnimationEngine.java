package com.dic.xsuper.lang.ui.animation;

import javafx.animation.*;
import javafx.beans.value.WritableValue;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.util.*;

/**
 * Motor que gerencia transições e keyframes para nós JavaFX.
 * Responsável por criar e iniciar Timelines baseadas nas configurações
 * armazenadas nos nós.
 */
public class XplAnimationEngine {
    private static final Map<Node, List<Timeline>> activeTimelines = new HashMap<>();

    /**
     * Aplica uma transição a uma propriedade de um nó, com base no valor antigo e novo.
     * Usa uma Timeline para interpolar entre os valores.
     * @param node O nó JavaFX
     * @param property Nome da propriedade (ex: "opacity", "translateX")
     * @param oldValue Valor antigo (string)
     * @param newValue Valor novo (string)
     * @param transition Configuração da transição
     */
    public static void applyTransition(Node node, String property, String oldValue, String newValue, XplTransition transition) {
        if (node == null || transition == null) return;
        // Verifica se a transição se aplica a esta propriedade
        if (!transition.getProperties().contains("all") && !transition.getProperties().contains(property)) {
            return;
        }

        // Obtém os valores como números (assumimos que são numéricos)
        double oldVal = parseDouble(oldValue);
        double newVal = parseDouble(newValue);
        if (Double.isNaN(oldVal) || Double.isNaN(newVal)) return;

        // Cria uma Timeline para animar a propriedade
        Duration duration = transition.getDuration();
        Duration delay = transition.getDelay();
        Interpolator interpolator = transition.getEasing().getInterpolator();

        Timeline timeline = new Timeline();
        timeline.setDelay(delay);
        timeline.setCycleCount(1);

        // Adiciona um KeyFrame no início (valor antigo) e no fim (valor novo)
        KeyValue keyValueStart = new KeyValue(getPropertyValue(node, property), oldVal);
        KeyValue keyValueEnd = new KeyValue(getPropertyValue(node, property), newVal, interpolator);
        KeyFrame startFrame = new KeyFrame(Duration.ZERO, keyValueStart);
        KeyFrame endFrame = new KeyFrame(duration, keyValueEnd);

        timeline.getKeyFrames().addAll(startFrame, endFrame);
        timeline.play();

        // Gerencia a lista ativa
        activeTimelines.computeIfAbsent(node, k -> new ArrayList<>()).add(timeline);
        // Remove automaticamente quando terminar
        timeline.setOnFinished(e -> {
            List<Timeline> list = activeTimelines.get(node);
            if (list != null) list.remove(timeline);
        });
    }

    /**
     * Aplica uma animação de keyframes a um nó.
     * @param node O nó JavaFX
     * @param animation Objeto com a animação configurada
     * @param overrides Mapa opcional de propriedades a sobrepor (ex: animation-duration)
     */
    public static void applyKeyframeAnimation(Node node, XplKeyframeAnimation animation, Map<String, String> overrides) {
        if (node == null || animation == null || animation.getKeyframes().isEmpty()) return;

        // Ordena os keyframes
        animation.sortKeyframes();

        // Cria a Timeline
        Timeline timeline = new Timeline();
        Duration duration = overrides != null && overrides.containsKey("duration") ?
                Duration.millis(parseTime(overrides.get("duration"))) : animation.getDuration();
        int iterations = overrides != null && overrides.containsKey("iterations") ?
                Integer.parseInt(overrides.get("iterations")) : animation.getIterations();
        String direction = overrides != null && overrides.containsKey("direction") ?
                overrides.get("direction") : animation.getDirection();
        String fillMode = overrides != null && overrides.containsKey("fill-mode") ?
                overrides.get("fill-mode") : animation.getFillMode();

        timeline.setCycleCount(iterations == -1 ? Timeline.INDEFINITE : iterations);
        timeline.setAutoReverse("alternate".equals(direction) || "alternate-reverse".equals(direction));

        // Constrói os KeyFrames com base nos keyframes da animação
        for (XplKeyframe frame : animation.getKeyframes()) {
            double pos = frame.getPosition();
            KeyFrame keyFrame = new KeyFrame(
                    duration.multiply(pos),
                    createKeyValues(node, frame.getStyles())
            );
            timeline.getKeyFrames().add(keyFrame);
        }

        // Aplica fill-mode (se "forwards" ou "both", manter o último estado)
        if ("forwards".equals(fillMode) || "both".equals(fillMode)) {
            timeline.setOnFinished(e -> {
                // Aplica o último keyframe (o de posição 1.0)
                XplKeyframe last = animation.getKeyframes().stream()
                        .max(Comparator.comparingDouble(XplKeyframe::getPosition))
                        .orElse(null);
                if (last != null) {
                    applyStyles(node, last.getStyles());
                }
            });
        }

        timeline.play();

        // Gerencia a lista ativa
        activeTimelines.computeIfAbsent(node, k -> new ArrayList<>()).add(timeline);
        timeline.setOnFinished(e -> {
            List<Timeline> list = activeTimelines.get(node);
            if (list != null) list.remove(timeline);
        });
    }

    /**
     * Para todas as animações ativas num nó.
     */
    public static void stopAll(Node node) {
        List<Timeline> timelines = activeTimelines.get(node);
        if (timelines != null) {
            for (Timeline t : timelines) {
                t.stop();
            }
            timelines.clear();
        }
    }

    // Métodos auxiliares (parciais)

    private static double parseDouble(String value) {
        if (value == null) return Double.NaN;
        value = value.trim();
        // Remove unidades (px, em, %)
        value = value.replaceAll("[^0-9.\\-]", "");
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static double parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return 0;

        // Verifica a unidade ANTES de limpar a string!
        boolean isMs = timeStr.toLowerCase().contains("ms");

        // Limpa todas as letras para ficar só o número
        String cleanStr = timeStr.replaceAll("[a-zA-Z]", "").trim();
        try {
            double val = Double.parseDouble(cleanStr);
            return isMs ? val : val * 1000; // Se não tiver 'ms', assumimos segundos e convertemos para millis
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static WritableValue<Number> getPropertyValue(Node node, String property) {
        // Mapeia propriedades CSS para propriedades JavaFX nativas
        return switch (property.toLowerCase()) {
            case "opacity" -> node.opacityProperty();
            case "translatex" -> node.translateXProperty();
            case "translatey" -> node.translateYProperty();
            case "scalex", "scale" -> node.scaleXProperty();
            case "scaley" -> node.scaleYProperty();
            case "rotate" -> node.rotateProperty();

            // ⭐ EXTRAS IMPORTANTES: Animar largura e altura se for um contentor!
            case "width" -> node instanceof Region r ? r.prefWidthProperty() : null;
            case "height" -> node instanceof Region r ? r.prefHeightProperty() : null;

            default -> null;
        };
    }

    private static KeyValue[] createKeyValues(Node node, Map<String, String> styles) {
        List<KeyValue> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : styles.entrySet()) {
            String prop = entry.getKey();
            String val = entry.getValue();
            WritableValue<Number> target = getPropertyValue(node, prop);
            if (target != null) {
                double num = parseDouble(val);
                if (!Double.isNaN(num)) {
                    list.add(new KeyValue(target, num));
                }
            }
        }
        return list.toArray(new KeyValue[0]);
    }

    private static void applyStyles(Node node, Map<String, String> styles) {
        for (Map.Entry<String, String> entry : styles.entrySet()) {
            String prop = entry.getKey();
            String val = entry.getValue();
            WritableValue<Number> target = getPropertyValue(node, prop);
            if (target != null) {
                double num = parseDouble(val);
                if (!Double.isNaN(num)) {
                    target.setValue(num);
                }
            }
        }
    }
}