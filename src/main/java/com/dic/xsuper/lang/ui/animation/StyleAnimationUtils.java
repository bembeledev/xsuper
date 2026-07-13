package com.dic.xsuper.lang.ui.animation;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;
import java.util.Map;

/**
 * Utilitário para aplicar animações CSS (keyframes nativos) ao JavaFX.
 * Suporta: animation: [name] [duration] [timing-function] [iteration-count] [direction]
 */
public class StyleAnimationUtils {

    public static void applyAnimations(Node fxNode, Map<String, String> style) {
        if (fxNode == null || style == null) return;

        // Se o desenvolvedor definiu uma animação no estilo
        if (style.containsKey("animation")) {
            String animStr = style.get("animation");
            parseAndPlayAnimation(fxNode, animStr);
        }
    }

    private static void parseAndPlayAnimation(Node fxNode, String animStr) {
        // Exemplo: "spin 2s linear infinite alternate"
        String[] parts = animStr.trim().split("\\s+");
        if (parts.length < 2) return; // Exige pelo menos nome e duração

        String name = parts[0].toLowerCase();
        Duration duration = parseDuration(parts[1]);

        Interpolator interpolator = Interpolator.EASE_BOTH;
        int cycleCount = 1;
        boolean autoReverse = false;
        Duration delay = Duration.ZERO;

        // Faz o parse do resto dos argumentos (ordem flexível como no CSS)
        for (int i = 2; i < parts.length; i++) {
            String p = parts[i].toLowerCase();
            if (p.equals("infinite")) {
                cycleCount = Animation.INDEFINITE;
            } else if (p.equals("linear")) {
                interpolator = Interpolator.LINEAR;
            } else if (p.equals("ease-in")) {
                interpolator = Interpolator.EASE_IN;
            } else if (p.equals("ease-out")) {
                interpolator = Interpolator.EASE_OUT;
            } else if (p.equals("ease-in-out") || p.equals("ease")) {
                interpolator = Interpolator.EASE_BOTH;
            } else if (p.equals("alternate")) {
                autoReverse = true;
            } else if (p.endsWith("ms") || p.endsWith("s")) {
                delay = parseDuration(p); // Se houver outro tempo, assume-se que é o delay
            } else if (p.matches("\\d+")) {
                cycleCount = Integer.parseInt(p); // Número exato de repetições
            }
        }

        Animation animation = buildAnimation(fxNode, name, duration, interpolator);

        if (animation != null) {
            animation.setCycleCount(cycleCount);
            animation.setAutoReverse(autoReverse);
            animation.setDelay(delay);
            animation.play(); // Dispara a animação na Thread do JavaFX!
        }
    }

    /**
     * O teu armazém de "@keyframes" nativos.
     */
    private static Animation buildAnimation(Node node, String name, Duration duration, Interpolator interpolator) {
        switch (name) {
            case "spin":
                RotateTransition rt = new RotateTransition(duration, node);
                rt.setByAngle(360);
                rt.setInterpolator(interpolator);
                return rt;

            case "pulse":
                ScaleTransition st = new ScaleTransition(duration, node);
                st.setByX(0.1); // Cresce 10%
                st.setByY(0.1);
                st.setInterpolator(interpolator);
                return st;

            case "fade-in":
                FadeTransition ftIn = new FadeTransition(duration, node);
                ftIn.setFromValue(0.0);
                ftIn.setToValue(1.0);
                ftIn.setInterpolator(interpolator);
                return ftIn;

            case "fade-out":
                FadeTransition ftOut = new FadeTransition(duration, node);
                ftOut.setFromValue(1.0);
                ftOut.setToValue(0.0);
                ftOut.setInterpolator(interpolator);
                return ftOut;

            case "bounce":
                TranslateTransition tt = new TranslateTransition(duration, node);
                tt.setByY(-20f); // Salta 20px para cima
                tt.setInterpolator(interpolator);
                return tt;

            case "slide-in-right":
                TranslateTransition slide = new TranslateTransition(duration, node);
                slide.setFromX(200f);
                slide.setToX(0f);
                slide.setInterpolator(interpolator);
                return slide;

            default:
                System.err.println("[StyleAnimationUtils] Keyframe não suportado: " + name);
                return null;
        }
    }

    private static Duration parseDuration(String val) {
        if (val.endsWith("ms")) return Duration.millis(Double.parseDouble(val.replace("ms", "")));
        if (val.endsWith("s")) return Duration.seconds(Double.parseDouble(val.replace("s", "")));
        return Duration.millis(500); // fallback padrão
    }
}