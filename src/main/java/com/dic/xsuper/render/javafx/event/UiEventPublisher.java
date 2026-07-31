package com.dic.xsuper.render.javafx.event;

import com.dic.xsuper.dom.event.XplEvent;
import com.dic.xsuper.dom.event.eventbus.UiEventBus;

/**
 * Utilitário para publicar eventos de forma mais concisa e segura.
 */
public final class UiEventPublisher {

    private UiEventPublisher() {}

    public static void publishDomMutated(String targetId, String property, Object value) {
        UiEventBus.getInstance().publish(UiEventBus.Topic.DOM_MUTATED, targetId, property, value);
    }

    public static void publishUiInteracted(String targetId, String property, Object value) {
        UiEventBus.getInstance().publish(UiEventBus.Topic.UI_INTERACTED, targetId, property, value);
    }

    public static void publishEventDispatch(String targetId, String callback, XplEvent event) {
        UiEventBus.getInstance().publish(UiEventBus.Topic.UI_EVENT_DISPATCH, targetId, callback, event);
    }

    public static void publishUpdateRequest(String targetId, String property, Object value) {
        UiEventBus.getInstance().publish(UiEventBus.Topic.UI_UPDATE_REQUEST, targetId, property, value);
    }

    public static void publishRebuildRequest(String targetId, String property, Object value) {
        UiEventBus.getInstance().publish(UiEventBus.Topic.UI_REBUILD_REQUEST, targetId, property, value);
    }
}