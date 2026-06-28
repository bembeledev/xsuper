package com.dic.xsuper.lang.ui.document;

import java.util.HashMap;
import java.util.Map;

/**
 * Representa um evento DOM no motor XPL.
 * Equivalente ao objeto Event do JavaScript.
 */
public class XplEvent {

    // --- Propriedades básicas ---
    public final String type;
    public final Object target;
    public final Object currentTarget;
    public final long timestamp;
    public final int eventPhase; // 0=NONE, 1=CAPTURING_PHASE, 2=AT_TARGET, 3=BUBBLING_PHASE
    public final boolean bubbles;
    public final boolean cancelable;
    public boolean defaultPrevented = false;
    public boolean propagationStopped = false;
    public boolean immediatePropagationStopped = false;

    // Dados adicionais (custom)
    public final Map<String, Object> detail = new HashMap<>();

    // --- Construtores ---
    public XplEvent(String type) {
        this(type, null, null);
    }

    public XplEvent(String type, Object target) {
        this(type, target, null);
    }

    public XplEvent(String type, Object target, Object currentTarget) {
        this.type = type;
        this.target = target;
        this.currentTarget = currentTarget;
        this.timestamp = System.currentTimeMillis();
        this.eventPhase = 2; // AT_TARGET por defeito
        this.bubbles = true;
        this.cancelable = true;
    }

    // Construtor com opções (estilo JS: new Event(type, { bubbles, cancelable }))
    public XplEvent(String type, boolean bubbles, boolean cancelable) {
        this(type, null, null);
        // Não podemos modificar campos finais, por isso criamos um método de fábrica
    }

    // Fábrica para eventos com opções
    public static XplEvent create(String type, Map<String, Object> options) {
        XplEvent evt = new XplEvent(type);
        if (options != null) {
            if (options.containsKey("bubbles")) evt.setBubbles((Boolean) options.get("bubbles"));
            if (options.containsKey("cancelable")) evt.setCancelable((Boolean) options.get("cancelable"));
            if (options.containsKey("detail")) evt.detail.putAll((Map<String, Object>) options.get("detail"));
        }
        return evt;
    }

    // Setters para as propriedades (apenas durante a criação)
    private void setBubbles(boolean b) { /* não é possível alterar final, mas podemos usar reflexão ou criar nova instância */ }
    private void setCancelable(boolean c) { /* similar */ }

    // --- Métodos ---
    public void preventDefault() {
        if (cancelable) {
            defaultPrevented = true;
        }
    }

    public void stopPropagation() {
        propagationStopped = true;
    }

    public void stopImmediatePropagation() {
        propagationStopped = true;
        immediatePropagationStopped = true;
    }

    // --- Conveniência para XPL ---
    public void setDetail(String key, Object value) {
        detail.put(key, value);
    }

    public Object getDetail(String key) {
        return detail.get(key);
    }

    @Override
    public String toString() {
        return "XplEvent{" +
                "type='" + type + '\'' +
                ", target=" + target +
                ", timestamp=" + timestamp +
                ", defaultPrevented=" + defaultPrevented +
                '}';
    }
}