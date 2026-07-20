package com.dic.xsuper.lang.ui.animation;

import com.dic.xsuper.lang.ui.html.XplNode;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class XplAnimationManager {

    // ⭐ O registo que saiu da Engine agora vive aqui de forma isolada!
    private final Map<String, XplKeyframeAnimation> keyframesRegistry = new HashMap<>();

    /**
     * O teu método original de extração, agora focado apenas na AST de CSS.
     */
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
                        String position = frameNode.attributes.getOrDefault("selector", "0%").toString();
                        XplKeyframe keyframe = new XplKeyframe(position);

                        for (XplNode propNode : frameNode.children) {
                            String propName = propNode.attributes.getOrDefault("name", "").toString();
                            String propValue = extractDeepValue(propNode);
                            keyframe.addStyle(propName, propValue);
                        }
                        animation.addKeyframe(keyframe);
                    }
                }

                keyframesRegistry.put(animName, animation);
                System.out.println("[AnimationManager] 🎬 Keyframe registado no cofre: " + animName);

                // ⭐ Crucial: Remove da árvore de CSS para não poluir o Matcher do DOM!
                iterator.remove();
            }
        }
    }

    public XplKeyframeAnimation getKeyframe(String name) {
        return keyframesRegistry.get(name);
    }

    private String extractDeepValue(XplNode node) {
        if (node == null) return "";
        if (node.attributes.containsKey("value")) return node.attributes.get("value").toString().trim();
        if (node.textContent != null && !node.textContent.trim().isEmpty()) return node.textContent.trim();

        StringBuilder sb = new StringBuilder();
        if (node.children != null) {
            for (XplNode child : node.children) {
                String childValue = extractDeepValue(child);
                if (!childValue.isEmpty()) sb.append(childValue).append(" ");
            }
        }
        return sb.toString().trim();
    }
}