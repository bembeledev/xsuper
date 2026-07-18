package com.dic.xsuper.lang.ui.event.eventbus;


import com.dic.xsuper.lang.ui.event.XplEvent;

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