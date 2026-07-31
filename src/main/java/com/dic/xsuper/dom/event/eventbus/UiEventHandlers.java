package com.dic.xsuper.dom.event.eventbus;

import com.dic.xsuper.dom.event.XplEvent;
import com.dic.xsuper.dom.node.XplElement;
import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.core.SuperUiEngine;
import com.dic.xsuper.render.javafx.event.UiEventPublisher;

public final class UiEventHandlers {
    private UiEventHandlers() {}

    public static void handleDomMutated(SuperUiEngine engine, String targetUid, String property, Object value) {
        if (targetUid == null || targetUid.isEmpty()) return;

        // 1. Actualiza o liveElement silenciosamente
        XplElement el = engine.getDocument().getElementByInternalUid(targetUid);
        if (el != null) {
            el.setAttributeSilently(property, value);
        }

        // 2. Actualiza a árvore virtual usando o novo indexador de Matrícula (UID)
        XplNode node = findNodeByUid(engine.getActiveDom(), targetUid);
        if (node != null) {

            // ⭐ PROTECÇÃO: Não sujar os atributos com propriedades dinâmicas!
            if (!property.equalsIgnoreCase("innerHTML") && !property.equalsIgnoreCase("textContent") && !property.equalsIgnoreCase("innerText")) {
                node.attributes.put(property, value);
            }
            syncSpecialProperties(node, property, value);
        }

        // 3. Encaminha para o EventBus tratar
        if ("innerHTML".equalsIgnoreCase(property)) {
            UiEventPublisher.publishRebuildRequest(targetUid, property, value);
        } else {
            UiEventPublisher.publishUpdateRequest(targetUid, property, value);
        }
    }

    private static void syncSpecialProperties(XplNode node, String property, Object value) {
        String str = value != null ? value.toString() : "";

        switch (property.toLowerCase()) {
            case "id" -> node.id = str;
            case "class", "classname" -> node.className = str;
            case "value" -> node.value = value;
            case "textcontent", "innertext" -> node.textContent = str;
            case "disabled" -> node.disabled = "true".equals(str) || "on".equals(str) || "disabled".equals(str);
            case "hidden" -> node.hidden = "true".equals(str) || "on".equals(str) || "hidden".equals(str);
        }
    }

    // ⭐ AGORA COMPARA MATRÍCULAS E NÃO IDs HTML!
    private static XplNode findNodeByUid(XplNode root, String uid) {
        if (root == null || uid == null) return null;
        if (uid.equals(root._internalUid)) return root; // <- Compara com _internalUid
        for (XplNode child : root.children) {
            XplNode found = findNodeByUid(child, uid);
            if (found != null) return found;
        }
        return null;
    }

    public static void handleUiInteracted(SuperUiEngine engine, String targetId, String property, Object value) {
        if (targetId == null || targetId.isEmpty()) return;

        // O utilizador interagiu com a tela (JavaFX). Vamos actualizar a memória DOM silenciosamente.
        XplElement el = engine.getDocument().getElementById(targetId);
        if (el != null) {
            el.setAttributeSilently(property, value);

            // Dispara os eventos da linguagem XPL (onchange, oninput)
            engine.dispatchEvent("input", value, targetId);
            engine.dispatchEvent("change", value, targetId);
        }
    }

    public static void handleUiEventDispatch(SuperUiEngine engine, String targetId, String scriptCallback, XplEvent event) {
        engine.executeInlineScript(scriptCallback, event);
    }
}