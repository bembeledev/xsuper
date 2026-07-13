package com.dic.xsuper.lang.ui.css.media;

import javafx.scene.Node;
import javafx.scene.Scene;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JavaFxMediaListener {

    // ⭐ OBRIGATÓRIO: LinkedHashMap garante que o motor lê o CSS de cima para baixo!
    private final Map<String, XplMediaNode> mediaMap = new java.util.LinkedHashMap<>();

    public void addMediaNode(XplMediaNode node) {
        mediaMap.put(node.condition, node);
    }

    public void clear() {
        mediaMap.clear();
    }

    public void attachToScene(Scene scene) {
        // 1. Ouve redimensionamentos, MAS SÓ se a janela já estiver fisicamente visível no ecrã!
        scene.widthProperty().addListener((obs, oldW, newW) -> {
            if (scene.getWindow() == null || !scene.getWindow().isShowing()) return; // Bloqueia o fantasma!

            double width = newW.doubleValue();
            if (width <= 0) return;
            evaluateAll(scene, width);
        });

        // 2. A avaliação inicial perfeita: Dispara apenas no momento exato em que a janela aparece.
        scene.windowProperty().addListener((obs, oldWin, newWin) -> {
            if (newWin != null) {
                newWin.showingProperty().addListener((o, oldS, isShowing) -> {
                    if (isShowing) {
                        evaluateAll(scene, scene.getWidth());
                    }
                });
            }
        });
    }

    /**
     * Motor inteligente: Apenas verifica quem liga/desliga.
     * Se houver alteração real, aciona o Repaint.
     */
    private void evaluateAll(Scene scene, double width) {
        boolean stateChanged = false;

        for (XplMediaNode mediaNode : mediaMap.values()) {
            boolean conditionMet = mediaNode.evaluate(width);

            // Se cruzou o limite (para ligar ou para desligar)
            if (conditionMet != mediaNode.isActive) {
                mediaNode.isActive = conditionMet;
                stateChanged = true;
            }
        }

        // Se pelo menos um @media mudou de estado, re-calculamos a cascata!
        if (stateChanged) {
            repaintCascade(scene);
        }
    }

    /**
     * A MAGIA DO CHROME: Limpa a tela e aplica os estilos na ordem correta.
     */
    private void repaintCascade(Scene scene) {
        Set<Node> affectedNodes = new HashSet<>();

        // 1. GATHERING: Identificar todos os nós afetados por QUALQUER media query
        for (XplMediaNode mediaNode : mediaMap.values()) {
            for (String selector : mediaNode.selectorsAndStyles.keySet()) {
                for (Node fxNode : scene.getRoot().lookupAll(selector)) {
                    // Garante que a foto original (sem CSS de media queries) está guardada
                    if (!fxNode.getProperties().containsKey("base_style")) {
                        fxNode.getProperties().put("base_style", fxNode.getStyle() != null ? fxNode.getStyle() : "");
                    }
                    affectedNodes.add(fxNode);
                }
            }
        }

        // 2. RESET: Reverter todos os nós afetados para a sua forma pura original
        for (Node fxNode : affectedNodes) {
            String baseStyle = (String) fxNode.getProperties().get("base_style");
            fxNode.setStyle(baseStyle);
        }

        // 3. APPLY: Reaplicar as regras ativas de cima para baixo.
        // Como é um LinkedHashMap, o último CSS (min-width: 400px) vai sobrepor o primeiro se houver conflito!
        for (XplMediaNode mediaNode : mediaMap.values()) {
            if (mediaNode.isActive) {
                System.out.println("[MediaListener] 🎨 Pintando Cascata: " + mediaNode.condition);

                for (Map.Entry<String, Map<String, String>> entry : mediaNode.selectorsAndStyles.entrySet()) {
                    String selector = entry.getKey();
                    Map<String, String> cssRules = entry.getValue();

                    for (Node fxNode : scene.getRoot().lookupAll(selector)) {
                        String currentStyle = fxNode.getStyle() != null ? fxNode.getStyle().trim() : "";
                        if (!currentStyle.isEmpty() && !currentStyle.endsWith(";")) {
                            currentStyle += ";";
                        }

                        StringBuilder mediaStyle = new StringBuilder(currentStyle);

                        for (Map.Entry<String, String> rule : cssRules.entrySet()) {
                            String propName = rule.getKey().toLowerCase();
                            String value = rule.getValue() != null ? rule.getValue().trim() : "";
                            if (value.isEmpty()) continue;

                            // 🛠️ TRADUTOR DE FLEXBOX PARA JAVAFX
                            if (propName.equals("flex-direction")) {
                                String orientation = value.equals("column") ? "vertical" : "horizontal";
                                mediaStyle.append(" -fx-orientation: ").append(orientation).append(";");
                                continue;
                            }
                            if (propName.equals("display")) {
                                // Ignoramos o 'display: flex' puro no CSS do JavaFX para não gerar warnings
                                continue;
                            }

                            String fxProp = convertToFxProp(propName);
                            mediaStyle.append(" ").append(fxProp).append(": ").append(value).append(";");
                        }

                        fxNode.setStyle(mediaStyle.toString());
                    }
                }
            }
        }
    }

    private String convertToFxProp(String cssProp) {
        return switch (cssProp.toLowerCase()) {
            case "background", "background-color" -> "-fx-background-color";
            case "color" -> "-fx-text-fill";
            case "font-size" -> "-fx-font-size";
            case "padding" -> "-fx-padding";
            case "border-radius" -> "-fx-background-radius";
            default -> "-fx-" + cssProp;
        };
    }
}