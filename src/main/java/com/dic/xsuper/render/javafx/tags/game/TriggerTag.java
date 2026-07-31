package com.dic.xsuper.render.javafx.tags.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.entity.components.BoundingBoxComponent;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class TriggerTag extends GameTagBase {

    private double x = 0, y = 0;
    private double width = 32, height = 32;
    private EntityType targetType = EntityType.PLAYER;
    private String onEnterScript;
    private String onExitScript;
    private boolean repeat = true;
    private double delaySeconds = 0;
    private boolean active = true;
    private String color = "rgba(255,0,0,0.3)";
    private boolean triggered = false;

    private Entity triggerEntity;

    public TriggerTag(XplNode sourceNode) {
        super(sourceNode);
        parseAttributes();
    }

    private void parseAttributes() {
        x = parseDouble(sourceNode.attributes.get("x"), 0);
        y = parseDouble(sourceNode.attributes.get("y"), 0);
        width = parseDouble(sourceNode.attributes.get("width"), 32);
        height = parseDouble(sourceNode.attributes.get("height"), 32);

        if (sourceNode.attributes.containsKey("target-type")) {
            String typeStr = sourceNode.attributes.get("target-type").toString().toUpperCase();
            try {
                targetType = EntityType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                System.err.println("[TriggerTag] Tipo de entidade inválido: " + typeStr);
            }
        }

        if (sourceNode.attributes.containsKey("on-enter")) {
            onEnterScript = sourceNode.attributes.get("on-enter").toString();
        }
        if (sourceNode.attributes.containsKey("on-exit")) {
            onExitScript = sourceNode.attributes.get("on-exit").toString();
        }
        repeat = parseBoolean(sourceNode.attributes.get("repeat"), true);
        if (sourceNode.attributes.containsKey("delay")) {
            delaySeconds = parseDuration(sourceNode.attributes.get("delay").toString());
        }
        active = parseBoolean(sourceNode.attributes.get("active"), true);
        if (sourceNode.attributes.containsKey("color")) {
            color = sourceNode.attributes.get("color").toString();
        }
    }

    @Override
    protected void onGameReady() {
        // Cria a entidade trigger (invisível, com hitbox)
        triggerEntity = FXGL.entityBuilder()
                .at(x, y)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .type(EntityType.TRIGGER)
                .with(new TriggerComponent(targetType, onEnterScript, onExitScript, repeat, delaySeconds))
                .buildAndAttach();

        // Se active for false, desativa o trigger
        if (!active) {
            TriggerComponent comp = triggerEntity.getComponent(TriggerComponent.class);
            if (comp != null) {
                comp.setActive(false);
            }
        }

        // Opcional: visualizar a área em modo debug
        if (FXGL.getSettings().isDeveloperMenuEnabled()) {
            Rectangle rect = new Rectangle(width, height);
            rect.setFill(Color.web(color));
            rect.setStroke(Color.RED);
            rect.setStrokeWidth(1);
            rect.setMouseTransparent(true);
            FXGL.addUINode(rect, x, y);
        }

        System.out.println("[TriggerTag] Trigger criado em (" + x + ", " + y + ")");
    }

    @Override
    protected Node createNode() {
        return createPlaceholder();
    }

    @Override
    protected void addChildren() {
        // Sem filhos
    }

    private double parseDuration(String value) {
        if (value.endsWith("ms")) {
            return Double.parseDouble(value.replace("ms", "")) / 1000.0;
        } else if (value.endsWith("s")) {
            return Double.parseDouble(value.replace("s", ""));
        }
        return Double.parseDouble(value);
    }

    // ─── Componente do trigger ────────────────────────────────────────────

    private static class TriggerComponent extends Component {
        private final EntityType targetType;
        private final String onEnterScript;
        private final String onExitScript;
        private final boolean repeat;
        private final double delaySeconds;
        private boolean active = true;
        private boolean triggered = false;

        // Variáveis para controlar se já está dentro
        private boolean isInside = false;

        public TriggerComponent(EntityType targetType, String onEnter, String onExit,
                                boolean repeat, double delay) {
            this.targetType = targetType;
            this.onEnterScript = onEnter;
            this.onExitScript = onExit;
            this.repeat = repeat;
            this.delaySeconds = delay;
        }

        @Override
        public void onUpdate(double tpf) {
            if (!active) return;

            // Verifica se alguma entidade do tipo alvo está dentro do trigger
            boolean inside = false;
            var entities = FXGL.getGameWorld().getEntitiesByType(targetType);

            for (Entity e : entities) {
                if (e.getComponent(BoundingBoxComponent.class).isCollidingWith(entity.getBoundingBoxComponent())) {
                   inside = true;
                        break;
                }
            }

            // Transição: entrou
            if (inside && !isInside) {
                isInside = true;
                if (onEnterScript != null && (!triggered || repeat)) {
                    if (delaySeconds > 0) {
                        FXGL.run(() -> executeScript(onEnterScript), Duration.seconds(delaySeconds));
                    } else {
                        executeScript(onEnterScript);
                    }
                    if (!repeat) {
                        triggered = true;
                    }
                }
            }

            // Transição: saiu
            if (!inside && isInside) {
                isInside = false;
                if (onExitScript != null && (!triggered || repeat)) {
                    if (delaySeconds > 0) {
                        FXGL.run(() -> executeScript(onExitScript), Duration.seconds(delaySeconds));
                    } else {
                        executeScript(onExitScript);
                    }
                    if (!repeat) {
                        triggered = true;
                    }
                }
            }
        }

        private void executeScript(String script) {
            System.out.println("[Trigger] Script executado: " + script);
            // SuperUiEngine.getInstance().executeInlineScript(script, null);
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}