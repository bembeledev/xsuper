package com.dic.xsuper.lang.ui.event.eventbus;
import java.util.ArrayList;
import java.util.List;

/**
 * O Canal de Comunicação Central da SuperUI (Padrão Pub/Sub).
 * Desacopla totalmente o DOM, o JavaFX e a Engine.
 */

public class UiEventBus {

    private static final UiEventBus instance = new UiEventBus();
    public static UiEventBus getInstance() { return instance; }

    // Tipos de Mensagens
    public enum Topic {
        DOM_MUTATED,          // script XPL alterou um atributo (já existe)
        UI_INTERACTED,        // utilizador interagiu com a UI (já existe)
        UI_UPDATE_REQUEST,    // pedido para actualizar uma propriedade no JavaFX
        UI_REBUILD_REQUEST,   // pedido para reconstruir parte da UI (ex: innerHTML)
        UI_EVENT_DISPATCH,    // pedido para disparar um evento XPL (callback)
    }

    public interface MessageListener {
        void onMessage(Topic topic, String targetId, String property, Object value);
    }

    private final List<MessageListener> listeners = new ArrayList<>();

    public void subscribe(MessageListener listener) {
        listeners.add(listener);
    }

    public void publish(Topic topic, String targetId, String property, Object value) {
        if (targetId == null || targetId.isEmpty()) return;
        // Distribui a mensagem a todos os subscritores (A Engine estará a ouvir)
        for (MessageListener listener : listeners) {
            listener.onMessage(topic, targetId, property, value);
        }
    }
}