package com.dic.xsuper.lang.ui.reactivity;

import com.dic.xsuper.lang.ui.SuperUiEngine;
import com.dic.xsuper.lang.ui.html.XplNode;
import java.util.Map;

/**
 * O Cirurgião de Reatividade (Fine-Grained Reactivity Renderer).
 */
public class XplReactivityRenderer {

    private final SuperUiEngine engine;

    public XplReactivityRenderer(SuperUiEngine engine) {
        this.engine = engine;
    }

    public void patchPartialDom(XplNode node, Map<String, Object> currentState) {
        if (node == null) return;

        // =========================================================================
        // NÍVEL 3 (VIP): TRANSPLANTE ESTRUTURAL (@if, @for, @switch)
        // (Tratado ANTES da verificação de UID, pois estes nós são fantasmas!)
        // =========================================================================
        if (node.tag.startsWith("@")) {
            System.out.println("🧱 [ReactivityRenderer] Mutação na diretiva " + node.tag + ". A acionar Arquitetura (renderCycle)...");

            javafx.application.Platform.runLater(engine::renderCycle);
            return; // Sai imediatamente, o renderCycle trata do resto!
        }

        // 🛡️ Segurança para nós físicos (Têm de ter Matrícula/UID)
        if (node._internalUid == null) return;

        // =========================================================================
        // NÍVEL 1: CIRURGIA DE ATRIBUTOS (ex: class={modalClass})
        // =========================================================================
        if (node.bindings != null && !node.bindings.isEmpty()) {
            for (Map.Entry<String, String> binding : node.bindings.entrySet()) {
                String attrName = binding.getKey();
                String expression = binding.getValue();

                Object newValue = engine.getDomEvaluator().evaluateExpressionXPL(expression);
                String strValue = newValue != null ? String.valueOf(newValue) : "";

                node.setAttribute(attrName, strValue);

                if (engine.getRendererBridge() != null) {
                    engine.getRendererBridge().updateProperty(node._internalUid, attrName, strValue);
                }
            }
        }

        // =========================================================================
        // NÍVEL 2: CIRURGIA DE TEXTO (Interpolação, ex: {{ titulo }})
        // =========================================================================
        if (node.rawTemplate != null && node.rawTemplate.contains("{{")) {
            String newText = engine.getDomEvaluator().resolveTextBindings(node.rawTemplate, node);
            node.textContent = newText;

            if (engine.getRendererBridge() != null) {
                engine.getRendererBridge().updateProperty(node._internalUid, "textContent", newText);
            }
        }
    }
}