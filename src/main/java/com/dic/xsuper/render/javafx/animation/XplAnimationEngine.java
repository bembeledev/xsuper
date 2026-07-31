package com.dic.xsuper.render.javafx.animation;

import animatefx.animation.*;
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

    // =========================================================================
    // 🎼 O MAESTRO (Blindado contra NullPointerException de Scene)
    // =========================================================================
    public static void playAnimation(Node fxNode, String animName, Map<String, String> overrides, XplAnimationManager manager) {

        // ⭐ PROTEÇÃO CONTRA O NÓ AINDA NÃO ESTAR NA SCENE GRAPH
        // Se a Scene ainda for null (o nó está a nascer), adiamos a animação para o momento em que ele for anexado à janela.
        if (fxNode.getScene() == null) {
            fxNode.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    executeAnimation(fxNode, animName, overrides, manager);
                }
            });
            return;
        }

        executeAnimation(fxNode, animName, overrides, manager);
    }

    private static void executeAnimation(Node fxNode, String animName, Map<String, String> overrides, XplAnimationManager manager) {
        // 1. Tenta encontrar a animação no mega-dicionário do AnimateFX
        AnimationFX fxAnim = getAnimateFX(animName, fxNode);

        if (fxAnim != null) {
            if (overrides.containsKey("iterations")) {
                int cycles = Integer.parseInt(overrides.get("iterations"));
                fxAnim.setCycleCount(cycles == -1 ? javafx.animation.Animation.INDEFINITE : cycles);
            }
            if (overrides.containsKey("duration")) {
                double requestedSeconds = parseDuration(overrides.get("duration"));
                fxAnim.setSpeed(1.0 / requestedSeconds);
            }

            fxAnim.play();
            System.out.println("[Maestro] 🚀 AnimateFX a executar: " + animName);
            return;
        }

        // 2. SE NÃO FOR DO ANIMATEFX, PROCURA NOS @KEYFRAMES DO TEU CSS
        XplKeyframeAnimation customAnim = manager.getKeyframe(animName);
        if (customAnim != null) {
            System.out.println("[Maestro] 🎨 Motor CSS a executar @keyframes: " + animName);
            applyKeyframeAnimation(fxNode, customAnim, overrides);
        } else {
            System.err.println("[Maestro] ⚠️ Animação desconhecida e sem @keyframes: " + animName);
        }
    }

    // =========================================================================
    // 📚 O MEGA-DICIONÁRIO DO ANIMATEFX (Mantém o teu switch gigante aqui)
    // =========================================================================
    private static AnimationFX getAnimateFX(String name, Node node) {
        return switch (name.toLowerCase()) {
            case "bounce" -> new Bounce(node);
            case "flash" -> new Flash(node);
            case "pulse" -> new Pulse(node);
            case "rubberband" -> new RubberBand(node);
            case "shake" -> new Shake(node);
            case "swing" -> new Swing(node);
            case "tada" -> new Tada(node);
            case "wobble" -> new Wobble(node);
            case "jello" -> new Jello(node);
            case "hinge" -> new Hinge(node);
            case "jackinthebox" -> new JackInTheBox(node);

            case "bouncein" -> new BounceIn(node);
            case "bounceindown" -> new BounceInDown(node);
            case "bounceinleft" -> new BounceInLeft(node);
            case "bounceinright" -> new BounceInRight(node);
            case "bounceinup" -> new BounceInUp(node);
            case "bounceout" -> new BounceOut(node);
            case "bounceoutdown" -> new BounceOutDown(node);
            case "bounceoutleft" -> new BounceOutLeft(node);
            case "bounceoutright" -> new BounceOutRight(node);
            case "bounceoutup" -> new BounceOutUp(node);

            case "fadein" -> new FadeIn(node);
            case "fadeindown" -> new FadeInDown(node);
            case "fadeindownbig" -> new FadeInDownBig(node);
            case "fadeinleft" -> new FadeInLeft(node);
            case "fadeinleftbig" -> new FadeInLeftBig(node);
            case "fadeinright" -> new FadeInRight(node);
            case "fadeinrightbig" -> new FadeInRightBig(node);
            case "fadeinup" -> new FadeInUp(node);
            case "fadeinupbig" -> new FadeInUpBig(node);
            case "fadeout" -> new FadeOut(node);
            case "fadeoutdown" -> new FadeOutDown(node);
            case "fadeoutdownbig" -> new FadeOutDownBig(node);
            case "fadeoutleft" -> new FadeOutLeft(node);
            case "fadeoutleftbig" -> new FadeOutLeftBig(node);
            case "fadeoutright" -> new FadeOutRight(node);
            case "fadeoutrightbig" -> new FadeOutRightBig(node);
            case "fadeoutup" -> new FadeOutUp(node);
            case "fadeoutupbig" -> new FadeOutUpBig(node);

            case "flip" -> new Flip(node);
            case "flipinx" -> new FlipInX(node);
            case "flipiny" -> new FlipInY(node);
            case "flipoutx" -> new FlipOutX(node);
            case "flipouty" -> new FlipOutY(node);

            case "lightspeedin" -> new LightSpeedIn(node);
            case "lightspeedout" -> new LightSpeedOut(node);

            case "rollin" -> new RollIn(node);
            case "rollout" -> new RollOut(node);
            case "rotatein" -> new RotateIn(node);
            case "rotateindownleft" -> new RotateInDownLeft(node);
            case "rotateindownright" -> new RotateInDownRight(node);
            case "rotateinupleft" -> new RotateInUpLeft(node);
            case "rotateinupright" -> new RotateInUpRight(node);
            case "rotateout" -> new RotateOut(node);
            case "rotateoutdownleft" -> new RotateOutDownLeft(node);
            case "rotateoutdownright" -> new RotateOutDownRight(node);
            case "rotateoutupleft" -> new RotateOutUpLeft(node);
            case "rotateoutupright" -> new RotateOutUpRight(node);

            case "slideindown" -> new SlideInDown(node);
            case "slideinleft" -> new SlideInLeft(node);
            case "slideinright" -> new SlideInRight(node);
            case "slideinup" -> new SlideInUp(node);
            case "slideoutdown" -> new SlideOutDown(node);
            case "slideoutleft" -> new SlideOutLeft(node);
            case "slideoutright" -> new SlideOutRight(node);
            case "slideoutup" -> new SlideOutUp(node);

            case "zoomin" -> new ZoomIn(node);
            case "zoomindown" -> new ZoomInDown(node);
            case "zoominleft" -> new ZoomInLeft(node);
            case "zoominright" -> new ZoomInRight(node);
            case "zoominup" -> new ZoomInUp(node);
            case "zoomout" -> new ZoomOut(node);
            case "zoomoutdown" -> new ZoomOutDown(node);
            case "zoomoutleft" -> new ZoomOutLeft(node);
            case "zoomoutright" -> new ZoomOutRight(node);
            case "zoomoutup" -> new ZoomOutUp(node);

            default -> null;
        };
    }

    private static double parseDuration(String durationStr) {
        try {
            if (durationStr.endsWith("ms")) {
                return Double.parseDouble(durationStr.replace("ms", "")) / 1000.0;
            } else if (durationStr.endsWith("s")) {
                return Double.parseDouble(durationStr.replace("s", ""));
            }
        } catch (Exception ignored) {}
        return 1.0;
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


        // Recolhe todas as propriedades únicas mencionadas em todos os keyframes (ex: opacity, background-color, etc.)
        Set<String> allProperties = new HashSet<>();
        for (XplKeyframe frame : animation.getKeyframes()) {
            allProperties.addAll(frame.getStyles().keySet());
        }

        // Para cada propriedade, construímos a sua evolução ao longo do tempo na Timeline
        for (String property : allProperties) {
            if (isColorProperty(property)) {
                // 🎨 TRATAMENTO DE CORES (Interpolação de Cores)
                setupColorKeyframes(node, timeline, animation, property, duration);
            } else {
                // 🔢 TRATAMENTO NUMÉRICO (Usa KeyValue nativo do JavaFX)
                setupNumericKeyframes(node, timeline, animation, property, duration);
            }
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

    private static void setupNumericKeyframes(Node node, Timeline timeline, XplKeyframeAnimation animation, String property, Duration totalDuration) {
        String propLower = property.toLowerCase();

        // 1. TRATAMENTO DA PROPRIEDADE COMPOSTA "transform" (Ex: transform: scale(1.1) ou rotate(45deg))
        if (propLower.equals("transform")) {
            for (XplKeyframe frame : animation.getKeyframes()) {
                String valStr = frame.getStyles().get(property);
                if (valStr == null) continue;
                valStr = valStr.trim().toLowerCase();

                Duration time = totalDuration.multiply(frame.getPosition());

                // Extrai funções individuais dentro do transform (ex: scale(1.2), rotate(90), translate(10px))
                if (valStr.contains("scale")) {
                    double num = parseDouble(extractFunctionValue(valStr, "scale"));
                    if (!Double.isNaN(num)) {
                        timeline.getKeyFrames().add(new KeyFrame(time,
                                new KeyValue(node.scaleXProperty(), num),
                                new KeyValue(node.scaleYProperty(), num)
                        ));
                    }
                }
                if (valStr.contains("scalex")) {
                    double num = parseDouble(extractFunctionValue(valStr, "scalex"));
                    if (!Double.isNaN(num)) timeline.getKeyFrames().add(new KeyFrame(time, new KeyValue(node.scaleXProperty(), num)));
                }
                if (valStr.contains("scaley")) {
                    double num = parseDouble(extractFunctionValue(valStr, "scaley"));
                    if (!Double.isNaN(num)) timeline.getKeyFrames().add(new KeyFrame(time, new KeyValue(node.scaleYProperty(), num)));
                }
                if (valStr.contains("rotate")) {
                    double num = parseDouble(extractFunctionValue(valStr, "rotate"));
                    if (!Double.isNaN(num)) timeline.getKeyFrames().add(new KeyFrame(time, new KeyValue(node.rotateProperty(), num)));
                }
                if (valStr.contains("translatex")) {
                    double num = parseDouble(extractFunctionValue(valStr, "translatex"));
                    if (!Double.isNaN(num)) timeline.getKeyFrames().add(new KeyFrame(time, new KeyValue(node.translateXProperty(), num)));
                }
                if (valStr.contains("translatey")) {
                    double num = parseDouble(extractFunctionValue(valStr, "translatey"));
                    if (!Double.isNaN(num)) timeline.getKeyFrames().add(new KeyFrame(time, new KeyValue(node.translateYProperty(), num)));
                }
            }
            return;
        }

        // 2. TRATAMENTO DIRETO PARA "scale" (Aplica a X e Y em simultâneo)
        if (propLower.equals("scale")) {
            for (XplKeyframe frame : animation.getKeyframes()) {
                String valStr = frame.getStyles().get(property);
                if (valStr != null) {
                    double num = parseDouble(valStr);
                    if (!Double.isNaN(num)) {
                        Duration time = totalDuration.multiply(frame.getPosition());
                        timeline.getKeyFrames().add(new KeyFrame(
                                time,
                                new KeyValue(node.scaleXProperty(), num),
                                new KeyValue(node.scaleYProperty(), num)
                        ));
                    }
                }
            }
            return;
        }

        // 3. TRATAMENTO NUMÉRICO PADRÃO (opacity, rotate, translateX, translateY, scaleX, scaleY, etc.)
        WritableValue<Number> target = getPropertyValue(node, property);
        if (target == null) return;

        for (XplKeyframe frame : animation.getKeyframes()) {
            if (frame.getStyles().containsKey(property)) {
                double num = parseDouble(frame.getStyles().get(property));
                if (!Double.isNaN(num)) {
                    KeyFrame kf = new KeyFrame(totalDuration.multiply(frame.getPosition()), new KeyValue(target, num));
                    timeline.getKeyFrames().add(kf);
                }
            }
        }
    }

    // Helper para extrair o valor numérico de funções CSS (ex: scale(1.5) -> 1.5)
    private static String extractFunctionValue(String cssString, String funcName) {
        try {
            int start = cssString.indexOf(funcName + "(");
            if (start == -1) return "";
            start += funcName.length() + 1;
            int end = cssString.indexOf(")", start);
            if (end == -1) return "";
            return cssString.substring(start, end).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static void setupColorKeyframes(Node node, Timeline timeline, XplKeyframeAnimation animation, String property, Duration totalDuration) {
        // Ordena os frames que contêm esta propriedade de cor
        List<XplKeyframe> validFrames = animation.getKeyframes().stream()
                .filter(f -> f.getStyles().containsKey(property))
                .sorted(Comparator.comparingDouble(XplKeyframe::getPosition))
                .toList();

        if (validFrames.size() < 2) return;

        // Cria transições suaves de cor entre cada par de keyframes
        for (int i = 0; i < validFrames.size() - 1; i++) {
            XplKeyframe startFrame = validFrames.get(i);
            XplKeyframe endFrame = validFrames.get(i + 1);

            javafx.scene.paint.Color startColor = parseWebColor(startFrame.getStyles().get(property));
            javafx.scene.paint.Color endColor = parseWebColor(endFrame.getStyles().get(property));

            if (startColor == null || endColor == null) continue;

            double startTime = startFrame.getPosition();
            double endTime = endFrame.getPosition();
            double interval = endTime - startTime;

            // Divide em micropassos para garantir fluidez visual na cor
            int steps = 20;
            for (int s = 1; s <= steps; s++) {
                double progress = (double) s / steps;
                double currentTime = startTime + (interval * progress);
                javafx.scene.paint.Color interpolatedColor = startColor.interpolate(endColor, progress);

                KeyFrame kf = new KeyFrame(totalDuration.multiply(currentTime), e -> applyColorProperty(node, property, interpolatedColor));
                timeline.getKeyFrames().add(kf);
            }
        }
    }

    private static boolean isColorProperty(String property) {
        String p = property.toLowerCase();
        return p.contains("color") || p.contains("background") || p.contains("border");
    }

    private static javafx.scene.paint.Color parseWebColor(String colorStr) {
        try {
            if (colorStr == null || colorStr.trim().isEmpty()) return null;
            return javafx.scene.paint.Color.web(colorStr.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static void applyColorProperty(Node node, String property, javafx.scene.paint.Color color) {
        String hexColor = "#" + color.toString().substring(2);
        String currentStyle = node.getStyle() != null ? node.getStyle() : "";

        // Aplica limpa ou adiciona ao estilo inline do JavaFX
        String cssProperty = switch (property.toLowerCase()) {
            case "background-color" -> "-fx-background-color";
            case "color" -> "-fx-text-fill";
            case "border-color" -> "-fx-border-color";
            default -> null;
        };

        if (cssProperty != null) {
            // Remove a propriedade antiga do estilo inline se existir e adiciona a nova cor interpolada
            currentStyle = currentStyle.replaceAll(cssProperty + "\\s*:[^;]+;", "");
            node.setStyle(currentStyle + " " + cssProperty + ": " + hexColor + ";");
        }
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
        return switch (property.toLowerCase()) {
            case "opacity" -> node.opacityProperty();
            case "translatex" -> node.translateXProperty();
            case "translatey" -> node.translateYProperty();
            case "translatez" -> node.translateZProperty();
            case "scalex" -> node.scaleXProperty();
            case "scaley" -> node.scaleYProperty();
            case "rotate", "rotatez" -> node.rotateProperty();
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