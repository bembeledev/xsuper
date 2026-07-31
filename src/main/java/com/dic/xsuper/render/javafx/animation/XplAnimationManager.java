package com.dic.xsuper.render.javafx.animation;

import com.dic.xsuper.dom.node.XplNode;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class XplAnimationManager {

    private final Map<String, XplKeyframeAnimation> keyframesRegistry = new HashMap<>();

    public void extractKeyframes(XplNode flatCssAst) {
        if (flatCssAst == null || flatCssAst.children == null) return;

        Iterator<XplNode> iterator = flatCssAst.children.iterator();
        while (iterator.hasNext()) {
            XplNode node = iterator.next();

            if ("@keyframes".equalsIgnoreCase(node.tag) || "keyframes".equalsIgnoreCase(node.tag)) {
                String animName = node.attributes.getOrDefault("name", "").toString();
                XplKeyframeAnimation animation = new XplKeyframeAnimation(animName);

                for (XplNode frameNode : node.children) {
                    if ("frame".equalsIgnoreCase(frameNode.tag) || "rule".equalsIgnoreCase(frameNode.tag)) {

                        // Lê o atributo "step" gerado pelo teu XplCssParser
                        String rawStep = frameNode.attributes.getOrDefault("step", "0%").toString();

                        // ⭐ A CURA DO "0, 100": Separa os frames por vírgula!
                        String[] positions = rawStep.split(",");

                        for (String pos : positions) {
                            String cleanPos = pos.trim().toLowerCase();

                            // Traduz W3C para números
                            if (cleanPos.equals("from")) cleanPos = "0";
                            else if (cleanPos.equals("to")) cleanPos = "100";

                            // Limpa o % de forma segura
                            cleanPos = cleanPos.replace("%", "").trim();
                            if (cleanPos.isEmpty()) continue;

                            XplKeyframe keyframe = new XplKeyframe(cleanPos);

                            for (XplNode propNode : frameNode.children) {
                                if ("property".equalsIgnoreCase(propNode.tag) || "variable".equalsIgnoreCase(propNode.tag) || propNode.attributes.containsKey("name")) {
                                    String propName = propNode.attributes.getOrDefault("name", "").toString();
                                    String propValue = extractDeepValue(propNode);

                                    if (!propName.isEmpty() && !propValue.isEmpty()) {
                                        keyframe.addStyle(propName, propValue);
                                    }
                                }
                            }
                            animation.addKeyframe(keyframe);
                        }
                    }
                }

                System.out.println("[AnimationManager] Frames extraídos para " + animName + ": " + animation.getKeyframes().size());
                // Ajustado para imprimir o getPosition ou getPercentage dependendo de como o chamaste
                for (XplKeyframe f : animation.getKeyframes()) {
                    System.out.println("   " + f.getPosition() + "% -> " + f.getStyles());
                }

                keyframesRegistry.put(animName, animation);
                System.out.println("[AnimationManager] 🎬 Keyframe registado no cofre: " + animName);

                iterator.remove(); // Remove da árvore de CSS
            }
        }
    }

    public XplKeyframeAnimation getKeyframe(String name) {
        return keyframesRegistry.get(name);
    }

    // ⭐ O Extrator invencível (Idêntico ao que usaste nas Media Queries)
    private String extractDeepValue(XplNode node) {
        if (node == null) return "";

        // Agora verifica a chave "value" e a chave "data"!
        if (node.attributes.containsKey("value")) return node.attributes.get("value").toString().trim();
        if (node.attributes.containsKey("data")) return node.attributes.get("data").toString().trim();

        if (node.textContent != null && !node.textContent.trim().isEmpty()) return node.textContent.trim();

        StringBuilder sb = new StringBuilder();
        if (node.children != null) {
            for (XplNode child : node.children) {
                String childValue = extractDeepValue(child);
                if (!childValue.isEmpty()) {
                    sb.append(childValue).append(" ");
                }
            }
        }
        return sb.toString().trim();
    }
}