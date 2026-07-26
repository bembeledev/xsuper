package com.dic.xsuper.lang.ui.tags.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class ProjectileTag extends GameTagBase {

    private EntityType type = EntityType.BULLET;
    private double speed = 300.0;
    private int damage = 10;
    private double lifetimeSeconds = 3.0;
    private String sprite = "projectile.png";
    private double width = 16, height = 16;
    private String direction = "right";
    private double angleDeg = 0;
    private boolean gravity = false;
    private boolean piercing = false;
    private String onHitScript;
    private String onExpireScript;

    private Entity projectileEntity;
    private Point2D directionVector;

    public ProjectileTag(XplNode sourceNode) {
        super(sourceNode);
        parseAttributes();
    }

    private void parseAttributes() {
        if (sourceNode.attributes.containsKey("type")) {
            type = parseEntityType(sourceNode.attributes.get("type"));
        }
        speed = parseDouble(sourceNode.attributes.get("speed"), 300.0);
        damage = parseInt(sourceNode.attributes.get("damage"), 10);
        if (sourceNode.attributes.containsKey("lifetime")) {
            lifetimeSeconds = parseDuration(sourceNode.attributes.get("lifetime").toString());
        }
        if (sourceNode.attributes.containsKey("sprite")) {
            sprite = sourceNode.attributes.get("sprite").toString();
        }
        width = parseDouble(sourceNode.attributes.get("width"), 16);
        height = parseDouble(sourceNode.attributes.get("height"), 16);
        if (sourceNode.attributes.containsKey("direction")) {
            direction = sourceNode.attributes.get("direction").toString().toLowerCase();
        }
        angleDeg = parseDouble(sourceNode.attributes.get("angle"), 0);
        gravity = parseBoolean(sourceNode.attributes.get("gravity"), false);
        piercing = parseBoolean(sourceNode.attributes.get("piercing"), false);
        if (sourceNode.attributes.containsKey("on-hit")) {
            onHitScript = sourceNode.attributes.get("on-hit").toString();
        }
        if (sourceNode.attributes.containsKey("on-expire")) {
            onExpireScript = sourceNode.attributes.get("on-expire").toString();
        }
    }

    @Override
    protected void onGameReady() {
        double angleRad = 0;
        if (direction.equals("right")) angleRad = 0;
        else if (direction.equals("left")) angleRad = Math.PI;
        else if (direction.equals("up")) angleRad = -Math.PI/2;
        else if (direction.equals("down")) angleRad = Math.PI/2;
        else {
            try { angleRad = Math.toRadians(angleDeg); } catch (Exception e) { angleRad = 0; }
        }
        directionVector = new Point2D(Math.cos(angleRad), Math.sin(angleRad));

        HitBox hitBox = new HitBox(BoundingShape.box(width, height));

        Entity projectile = FXGL.entityBuilder()
                .at(0, 0)
                .view(createFallbackTexture(sprite, Color.YELLOW, width, height))
                .bbox(hitBox)
                .type(type)
                .with(new ProjectileComponent(directionVector, speed, gravity, piercing, damage, lifetimeSeconds))
                .buildAndAttach();

        projectileEntity = projectile;

        double xPos = parseDouble(sourceNode.attributes.get("x"), 0);
        double yPos = parseDouble(sourceNode.attributes.get("y"), 0);
        projectile.setPosition(xPos, yPos);

        // ⭐ Registar colisão com ENEMY (cast desnecessário)
        FXGL.onCollisionBegin(type, EntityType.ENEMY, (p, other) -> onHit(other));

        FXGL.run(() -> {
            if (projectileEntity != null && projectileEntity.isActive()) {
                if (onExpireScript != null) {
                    executeScript(onExpireScript);
                }
                projectileEntity.removeFromWorld();
            }
        }, Duration.seconds(lifetimeSeconds));
    }

    private void onHit(Entity other) {
        if (onHitScript != null) {
            FXGL.getWorldProperties().setValue("target", other);
            executeScript(onHitScript);
        }
        if (!piercing) {
            if (projectileEntity != null && projectileEntity.isActive()) {
                projectileEntity.removeFromWorld();
            }
        }
    }

    private void executeScript(String script) {
        System.out.println("[ProjectileTag] Script executado: " + script);
    }

    private double parseDuration(String value) {
        if (value.endsWith("ms")) {
            return Double.parseDouble(value.replace("ms", "")) / 1000.0;
        } else if (value.endsWith("s")) {
            return Double.parseDouble(value.replace("s", ""));
        }
        return Double.parseDouble(value);
    }

    @Override
    protected Node createNode() {
        return createPlaceholder();
    }

    @Override
    protected void addChildren() {}

    private static class ProjectileComponent extends Component {
        private final Point2D direction;
        private final double speed;
        private final boolean gravity;
        private final boolean piercing;
        private final int damage;
        private final double lifetime;

        public ProjectileComponent(Point2D direction, double speed, boolean gravity,
                                   boolean piercing, int damage, double lifetime) {
            this.direction = direction;
            this.speed = speed;
            this.gravity = gravity;
            this.piercing = piercing;
            this.damage = damage;
            this.lifetime = lifetime;
        }

        @Override
        public void onUpdate(double tpf) {
            double dx = direction.getX() * speed * tpf;
            double dy = direction.getY() * speed * tpf;
            if (gravity) {
                dy += 9.8 * 30 * tpf;
            }
            entity.translateX(dx);
            entity.translateY(dy);
        }
    }
}