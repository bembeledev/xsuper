package com.dic.xsuper.lang.ui.event.eventbus;

import com.dic.xsuper.lang.ui.SuperUiEngine;
import com.dic.xsuper.lang.ui.event.XplEvent;

public final class UiEventBusSubscriber {
    private UiEventBusSubscriber() {}

    public static void register(SuperUiEngine engine) {
        UiEventBus.getInstance().subscribe((topic, targetId, property, value) -> {
            switch (topic) {
                case DOM_MUTATED ->
                        UiEventHandlers.handleDomMutated(engine, targetId, property, value);

                case UI_INTERACTED ->
                        UiEventHandlers.handleUiInteracted(engine, targetId, property, value);

                case UI_EVENT_DISPATCH ->
                        UiEventHandlers.handleUiEventDispatch(engine, targetId, property, (XplEvent) value);

                // ⭐ LIGAÇÃO DIRECTA AO JAVAFX: Actualizar propriedades (ex: cor, width, value)
                case UI_UPDATE_REQUEST -> {
                    if (engine.getRendererBridge() != null) {
                        engine.getRendererBridge().updateProperty(targetId, property, value);
                    }
                }

                // ⭐ LIGAÇÃO DIRECTA À ENGINE: Reconstruir árvore (ex: innerHTML)
                case UI_REBUILD_REQUEST -> {
                    engine.processPartialHtmlUpdate(targetId, value != null ? value.toString() : "");
                }
            }
        });
    }
}