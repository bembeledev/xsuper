package com.dic.xsuper.lang.ui.events;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.poo.XplInstance;
import com.dic.xsuper.lang.ui.SuperUiEngine;
import com.dic.xsuper.lang.ui.document.XplNativeObject;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Representa um evento DOM no motor XPL.
 * Equivalente ao objeto Event do JavaScript.
 * Agora é uma classe nativa do XPL, instanciável e com métodos expostos.
 */
public class XplEvent extends XplInstance implements XplNativeObject {

    // ─── Modelo nativo (singleton) ──────────────────────────────────────────
    private static XPLModel nativeModel;

    // ─── Propriedades do evento ─────────────────────────────────────────────
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

    // Dados adicionais (custom)
    public final Map<String, Object> detail = new HashMap<>();

    // ─── Construtores ────────────────────────────────────────────────────────

    public XplEvent(String type) {
        this(type, null, null);
    }

    public XplEvent(String type, Object target) {
        this(type, target, null);
    }

    public XplEvent(String type, Object target, Object currentTarget) {
        super(SuperUiEngine.EVENT_CLASS);   // ⭐ classe nativa registada
        this.type = type;
        this.target = target;
        this.currentTarget = currentTarget;
        this.timestamp = System.currentTimeMillis();
        this.eventPhase = 2;                // AT_TARGET
        this.bubbles = true;
        this.cancelable = true;

        invokeMethod();                     // ⭐ expor métodos ao XPL
    }

    // ─── Métodos de controlo do evento ──────────────────────────────────────

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

    // ─── Implementação de XplNativeObject ──────────────────────────────────

    @Override
    public void invokeMethod() {
        // ─── EXPOR MÉTODOS ────────────────────────────────────────────────

        this.fields.put("preventDefault", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                preventDefault();
                return null;
            }
        });

        this.fields.put("stopPropagation", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                stopPropagation();
                return null;
            }
        });

        this.fields.put("stopImmediatePropagation", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                stopImmediatePropagation();
                return null;
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

        // ─── PROPRIEDADES COMO GETTERS ────────────────────────────────────
        this.fields.put("getType", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return type;
            }
        });

        this.fields.put("getTarget", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return target;
            }
        });

        this.fields.put("getCurrentTarget", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return currentTarget;
            }
        });

        this.fields.put("getTimestamp", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return timestamp;
            }
        });

        this.fields.put("isDefaultPrevented", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return defaultPrevented;
            }
        });

        this.fields.put("isPropagationStopped", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return propagationStopped;
            }
        });

        this.fields.put("getDetailMap", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return Collections.unmodifiableMap(detail);
            }
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
            case "propagationStopped" -> propagationStopped;
            case "immediatePropagationStopped" -> immediatePropagationStopped;
            case "detail" -> Collections.unmodifiableMap(detail);
            default -> null;
        };
    }

    // ─── CONSTRUÇÃO DO MODELO NATIVO ──────────────────────────────────────

    public static XPLModel buildNativeModel() {
        if (nativeModel == null) {
            nativeModel = new XPLModel("XplEvent", null);

            // ─── Campos (propriedades) ──────────────────────────────────────
            String[] fieldNames = {
                    "type", "target", "currentTarget", "timestamp", "eventPhase",
                    "bubbles", "cancelable", "defaultPrevented",
                    "propagationStopped", "immediatePropagationStopped", "detail"
            };
            for (String f : fieldNames) {
                Token nameToken = new Token(TokenType.IDENTIFIER, f, null, 0, 0);
                Stmt.FieldDecl field = new Stmt.FieldDecl(
                        getAccessModifier(),
                        false, false, false, nameToken, null
                );
                nativeModel.addField(field);
            }

            // ─── Métodos ──────────────────────────────────────────────────
            // preventDefault()
            nativeModel.addMethod(new Stmt.Function(
                    getAccessModifier(), false, false,
                    new Token(TokenType.IDENTIFIER, "preventDefault", null, 0, 0),
                    Collections.emptyList(),
                    null, null, Collections.emptyList(), Collections.emptyList()
            ));

            // stopPropagation()
            nativeModel.addMethod(new Stmt.Function(
                    getAccessModifier(), false, false,
                    new Token(TokenType.IDENTIFIER, "stopPropagation", null, 0, 0),
                    Collections.emptyList(),
                    null, null, Collections.emptyList(), Collections.emptyList()
            ));

            // stopImmediatePropagation()
            nativeModel.addMethod(new Stmt.Function(
                    getAccessModifier(), false, false,
                    new Token(TokenType.IDENTIFIER, "stopImmediatePropagation", null, 0, 0),
                    Collections.emptyList(),
                    null, null, Collections.emptyList(), Collections.emptyList()
            ));

            // setDetail(key, value)
            List<Stmt.Param> paramsSetDetail = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "key", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "value", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    getAccessModifier(), false, false,
                    new Token(TokenType.IDENTIFIER, "setDetail", null, 0, 0),
                    paramsSetDetail,
                    null, null, Collections.emptyList(), Collections.emptyList()
            ));

            // getDetail(key)
            List<Stmt.Param> paramsGetDetail = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "key", null, 0, 0), null, null)
            );
            nativeModel.addMethod(new Stmt.Function(
                    getAccessModifier(), false, false,
                    new Token(TokenType.IDENTIFIER, "getDetail", null, 0, 0),
                    paramsGetDetail,
                    null, null, Collections.emptyList(), Collections.emptyList()
            ));

            // getters para propriedades (opcionais, mas úteis)
            nativeModel.addMethod(new Stmt.Function(
                    getAccessModifier(), false, false,
                    new Token(TokenType.IDENTIFIER, "getType", null, 0, 0),
                    Collections.emptyList(),
                    null, null, Collections.emptyList(), Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    getAccessModifier(), false, false,
                    new Token(TokenType.IDENTIFIER, "getTarget", null, 0, 0),
                    Collections.emptyList(),
                    null, null, Collections.emptyList(), Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    getAccessModifier(), false, false,
                    new Token(TokenType.IDENTIFIER, "getCurrentTarget", null, 0, 0),
                    Collections.emptyList(),
                    null, null, Collections.emptyList(), Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    getAccessModifier(), false, false,
                    new Token(TokenType.IDENTIFIER, "getTimestamp", null, 0, 0),
                    Collections.emptyList(),
                    null, null, Collections.emptyList(), Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    getAccessModifier(), false, false,
                    new Token(TokenType.IDENTIFIER, "isDefaultPrevented", null, 0, 0),
                    Collections.emptyList(),
                    null, null, Collections.emptyList(), Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    getAccessModifier(), false, false,
                    new Token(TokenType.IDENTIFIER, "isPropagationStopped", null, 0, 0),
                    Collections.emptyList(),
                    null, null, Collections.emptyList(), Collections.emptyList()
            ));
            nativeModel.addMethod(new Stmt.Function(
                    getAccessModifier(), false, false,
                    new Token(TokenType.IDENTIFIER, "getDetailMap", null, 0, 0),
                    Collections.emptyList(),
                    null, null, Collections.emptyList(), Collections.emptyList()
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
                ", target=" + target +
                ", timestamp=" + timestamp +
                ", defaultPrevented=" + defaultPrevented +
                '}';
    }
}