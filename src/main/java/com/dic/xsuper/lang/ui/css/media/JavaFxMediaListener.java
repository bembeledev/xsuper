package com.dic.xsuper.lang.ui.css.media;

import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.scene.Scene;

import java.util.Map;

public class JavaFxMediaListener {

    public final Map<String, XplMediaNode> mediaMap = new java.util.LinkedHashMap<>();

    private Runnable engineRebuildTrigger;

    public void setEngineRebuildTrigger(Runnable trigger) {
        this.engineRebuildTrigger = trigger;
    }

    public void addMediaNode(XplMediaNode node) {
        // Se a query já existir na memória, preserva o estado (Ligado/Desligado)
        if (mediaMap.containsKey(node.condition)) {
            node.isActive = mediaMap.get(node.condition).isActive;
        }
        mediaMap.put(node.condition, node);
    }

    public void clear() {
        mediaMap.clear();
    }

    /**
     * Anexa os listeners de redimensionamento à cena.
     * Quando a largura muda, avalia as media queries.
     */
    public void attachToScene(Scene scene) {
        scene.widthProperty().addListener((obs, oldW, newW) -> {
            if (scene.getWindow() == null || !scene.getWindow().isShowing()) return;
            double width = newW.doubleValue();
            if (width <= 0) return;
            evaluateAll(width);
        });

        scene.windowProperty().addListener((obs, oldWin, newWin) -> {
            if (newWin != null) {
                newWin.showingProperty().addListener((o, oldS, isShowing) -> {
                    if (isShowing) evaluateAll(scene.getWidth());
                });
            }
        });
    }

    /**
     * Avalia todas as media queries com a largura atual.
     * Se alguma mudar de estado, dispara a reconstrução da UI.
     */
    private void evaluateAll(double width) {
        boolean stateChanged = false;

        for (XplMediaNode mediaNode : mediaMap.values()) {
            boolean conditionMet = mediaNode.evaluate(width);
            if (conditionMet != mediaNode.isActive) {
                mediaNode.isActive = conditionMet;
                stateChanged = true;
            }
        }

        if (stateChanged && engineRebuildTrigger != null) {
            System.out.println("[MediaListener] 📱 Media queries mudaram. A reconstruir UI...");
            engineRebuildTrigger.run();
        }
    }

    /**
     * Aplica as regras das media queries ativas ao DOM virtual (XplNode).
     * Percorre a árvore e sobrescreve os estilos dos nós que correspondem aos seletores.
     * Este método é chamado a partir do renderCycle da SuperUiEngine.
     */
    public void applyActiveStylesToVirtualDom(XplNode root) {
        if (root == null) return;

        for (XplMediaNode mediaNode : mediaMap.values()) {
            if (!mediaNode.isActive) continue;

            for (Map.Entry<String, Map<String, String>> entry : mediaNode.selectorsAndStyles.entrySet()) {
                String selector = entry.getKey().trim();
                Map<String, String> cssRules = entry.getValue();
                applyRulesToTree(root, selector, cssRules);
            }
        }
    }

    /**
     * Aplica as regras de uma media query a todos os nós que correspondem ao seletor.
     * Percorre recursivamente a árvore.
     */
    private void applyRulesToTree(XplNode node, String selector, Map<String, String> cssRules) {
        if (matchesSelector(node, selector)) {
            // Sobrescreve os estilos no node.style
            for (Map.Entry<String, String> rule : cssRules.entrySet()) {
                node.style.put(rule.getKey().toLowerCase(), rule.getValue());
            }
        }
        for (XplNode child : node.children) {
            applyRulesToTree(child, selector, cssRules);
        }
    }

    /**
     * Verifica se um nó corresponde a um seletor CSS.
     * Suporta seletores de classe (.), ID (#) e tag (nome da tag).
     */
    private boolean matchesSelector(XplNode node, String selector) {
        if (selector.startsWith(".")) {
            return node.className != null && node.className.contains(selector.substring(1));
        } else if (selector.startsWith("#")) {
            return node.id != null && node.id.equals(selector.substring(1));
        } else {
            return node.tag != null && node.tag.equalsIgnoreCase(selector);
        }
    }
}