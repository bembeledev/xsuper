package com.dic.xsuper.dom.event;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.ast.Stmt;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.core.Token;
import com.dic.xsuper.engine.core.TokenType;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplInstance;
import com.dic.xsuper.render.javafx.core.SuperUiEngine;
import com.dic.xsuper.dom.node.XplElement;
import com.dic.xsuper.dom.node.XplNativeObject;
import com.dic.xsuper.render.javafx.event.XplKeyCode;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Representa um evento DOM no motor XPL unificado.
 * Herda as capacidades nativas do motor XPL e os contratos de eventos (W3C e InputManager).
 */
public class XplEvent extends XplInstance implements XplNativeObject {

    // ─── Modelo nativo (singleton) ──────────────────────────────────────────
    private static XPLModel nativeModel;

    // ─── Propriedades do evento (W3C Standard) ──────────────────────────────
    public final String type;
    public final Object target;
    public final Object currentTarget;
    public final long timestamp;
    public final int eventPhase;        // 0=NONE, 1=CAPTURING, 2=AT_TARGET, 3=BUBBLING
    public final boolean bubbles;
    public final boolean cancelable;
    public boolean defaultPrevented = false;
    public boolean propagationStopped = false;
    public boolean immediatePropagationStopped = false;

    // Mapa dinâmico para dados flexíveis (rato, teclado, custom)
    public final Map<String, Object> detail = new HashMap<>();

    // ─── Construtores ───────────────────────────────────────────────────────
    /**
     * Construtor de Retrocompatibilidade para o XplInputManager antigo e EventBus.
     */
    // ─── Construtor Antigo ──────────────────────────────────────────────────
    public XplEvent(XplEventType eventType, Object target, float mouseX, float mouseY, XplKeyCode keyCode) {
        super(SuperUiEngine.EVENT_CLASS);
        this.type = eventType != null ? eventType.getWebName() : "unknown";
        this.target = target;
        this.currentTarget = target;
        this.timestamp = System.currentTimeMillis();
        this.eventPhase = 2;
        this.bubbles = true;
        this.cancelable = true;

        this.detail.put("clientX", mouseX);
        this.detail.put("clientY", mouseY);

        if (keyCode != null && keyCode != XplKeyCode.KEY_UNKNOWN) {
            this.detail.put("code", keyCode.name());
            this.detail.put("keyCode", keyCode.getValue());
        }

        // ⭐ A PONTE DE MEMÓRIA: Passa o mapa do Java para a gaveta do XPL
        this.fields.put("detail", this.detail);
        this.fields.put("type", this.type);

        invokeMethod();
    }

    // ─── Construtor Moderno ─────────────────────────────────────────────────
    public XplEvent(String type, Object target, Object currentTarget) {
        super(SuperUiEngine.EVENT_CLASS);
        this.type = type;
        this.target = target;
        this.currentTarget = currentTarget;
        this.timestamp = System.currentTimeMillis();
        this.eventPhase = 2;
        this.bubbles = true;
        this.cancelable = true;

        // ⭐ A PONTE DE MEMÓRIA TAMBÉM AQUI (Para os eventos do JavaFX)
        this.fields.put("detail", this.detail);
        this.fields.put("type", this.type);

        invokeMethod();
    }

    public XplEvent(String type) {
        this(type, null, null);
    }

    public XplEvent(String focus, XplElement xplElement) {
        this(focus, null, null);
    }

    // ─── Getters de Retrocompatibilidade (Código Java Antigo) ───────────────

    public XplEventType getTypeEnum() {
        return XplEventType.fromString(this.type);
    }

    public Object getTarget() {
        return this.target;
    }

    public float getMouseX() {
        Object val = detail.get("clientX");
        return val instanceof Number ? ((Number) val).floatValue() : 0f;
    }

    public float getMouseY() {
        Object val = detail.get("clientY");
        return val instanceof Number ? ((Number) val).floatValue() : 0f;
    }

    public XplKeyCode getKeyCode() {
        Object code = detail.get("keyCode");
        if (code instanceof Integer) return XplKeyCode.fromIntValue((Integer) code);
        return XplKeyCode.KEY_UNKNOWN;
    }

    // ─── Métodos de controlo do evento (W3C) ────────────────────────────────

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

    public void setDetail(String key, Object value) {
        detail.put(key, value);
    }

    public Object getDetail(String key) {
        return detail.get(key);
    }

    // ─── Implementação de XplNativeObject (Motor Interpretador) ─────────────

    @Override
    public void invokeMethod() {
        this.fields.put("preventDefault", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                preventDefault(); return null;
            }
        });

        this.fields.put("stopPropagation", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                stopPropagation(); return null;
            }
        });

        this.fields.put("setDetail", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String key = intp.evaluate(args.get(0).expression).toString();
                Object value = intp.evaluate(args.get(1).expression);
                setDetail(key, value);
                return null;
            }
        });

        this.fields.put("getDetail", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String key = intp.evaluate(args.getFirst().expression).toString();
                return getDetail(key);
            }
        });

        // Getters para a linguagem XPL (Scripts)
        this.fields.put("getType", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) { return type; }
        });

        this.fields.put("getTarget", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) { return target; }
        });

        this.fields.put("isDefaultPrevented", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) { return defaultPrevented; }
        });
    }

    @Override
    public Object getProperty(String propertyName) {
        return switch (propertyName) {
            case "type" -> type;
            case "target" -> target;
            case "currentTarget" -> currentTarget;
            case "timestamp" -> timestamp;
            case "eventPhase" -> eventPhase;
            case "bubbles" -> bubbles;
            case "cancelable" -> cancelable;
            case "defaultPrevented" -> defaultPrevented;
            case "detail" -> Collections.unmodifiableMap(detail);
            default -> null;
        };
    }

    // ─── CONSTRUÇÃO DO MODELO NATIVO ────────────────────────────────────────
    public static XPLModel buildNativeModel() {
        if (nativeModel == null) {
            nativeModel = new XPLModel("XplEvent", null);

            String[] fieldNames = {
                    "type", "target", "currentTarget", "timestamp", "eventPhase",
                    "bubbles", "cancelable", "defaultPrevented", "detail"
            };
            for (String f : fieldNames) {
                Token nameToken = new Token(TokenType.IDENTIFIER, f, null, 0, 0);
                Stmt.FieldDecl field = new Stmt.FieldDecl(getAccessModifier(), false, false, false, nameToken, null);
                nativeModel.addField(field);
            }

            nativeModel.addMethod(new Stmt.Function(getAccessModifier(), false, false,
                    new Token(TokenType.IDENTIFIER, "preventDefault", null, 0, 0),
                    Collections.emptyList(), null, null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            nativeModel.addMethod(new Stmt.Function(getAccessModifier(), false, false,
                    new Token(TokenType.IDENTIFIER, "stopPropagation", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
        }
        return nativeModel;
    }

    private static @NotNull Token getAccessModifier() {
        return new Token(TokenType.PUBLIC, "pub", null, 0, 0, null);
    }

    @Override
    public String toString() {
        return "XplEvent{" +
                "type='" + type + '\'' +
                ", target=" + target.toString() +
                ", currentTarget=" + currentTarget.toString() +
                ", timestamp=" + timestamp +
                ", eventPhase=" + eventPhase +
                ", bubbles=" + bubbles +
                ", cancelable=" + cancelable +
                ", defaultPrevented=" + defaultPrevented +
                ", propagationStopped=" + propagationStopped +
                ", immediatePropagationStopped=" + immediatePropagationStopped +
                ", detail=" + detail +
                ", klass=" + klass.model.name +
                ", fields=" + fields +
                '}';
    }
}