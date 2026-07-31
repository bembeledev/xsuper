package com.dic.xsuper.render.javafx.tags.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.physics.BoundingShape;
import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.Node;
import javafx.scene.paint.Color;

public class EnemyTag extends GameTagBase {

    private double x = 0, y = 0;
    private double speed = 80;
    private int hp = 3;
    private String behavior = "patrol";
    private String sprite = "enemy.png";

    public EnemyTag(XplNode sourceNode) {
        super(sourceNode);
        x = parseDouble(sourceNode.attributes.get("x"), 0);
        y = parseDouble(sourceNode.attributes.get("y"), 0);
        speed = parseDouble(sourceNode.attributes.get("speed"), 80);
        hp = parseInt(sourceNode.attributes.get("hp"), 3);
        if (sourceNode.attributes.containsKey("behavior")) {
            behavior = sourceNode.attributes.get("behavior").toString().toLowerCase();
        }
        if (sourceNode.attributes.containsKey("sprite")) {
            sprite = sourceNode.attributes.get("sprite").toString();
        }
    }

    @Override
    protected void onGameReady() {
        HitBox hitBox = new HitBox(BoundingShape.box(32, 32));

        Entity enemy = FXGL.entityBuilder()
                .at(x, y)
                .view(createFallbackTexture(sprite, Color.RED, 32, 32))
                .bbox(hitBox)
                .type(EntityType.ENEMY)
                .with(new EnemyAI(behavior, speed, hp))
                .buildAndAttach();
        System.out.println("[EnemyTag] Inimigo criado em (" + x + ", " + y + ") com comportamento: " + behavior);
    }

    @Override
    protected Node createNode() {
        return createPlaceholder();
    }

    private static class EnemyAI extends Component {
        private final String behavior;
        private final double speed;
        private int hp;
        private double timer = 0;

        public EnemyAI(String behavior, double speed, int hp) {
            this.behavior = behavior;
            this.speed = speed;
            this.hp = hp;
        }

        @Override
        public void onUpdate(double tpf) {
            switch (behavior) {
                case "patrol":
                    timer += tpf;
                    if (timer > 2.0) {
                        timer = 0;
                        entity.translateX((Math.random() - 0.5) * speed * tpf);
                        entity.translateY((Math.random() - 0.5) * speed * tpf);
                    }
                    break;
                case "chase":
                    var player = FXGL.getGameWorld().getSingleton(EntityType.PLAYER);
                    if (player != null) {
                        double dx = player.getX() - entity.getX();
                        double dy = player.getY() - entity.getY();
                        double dist = Math.sqrt(dx*dx + dy*dy);
                        if (dist > 0) {
                            double normX = dx / dist;
                            double normY = dy / dist;
                            entity.translateX(normX * speed * tpf);
                            entity.translateY(normY * speed * tpf);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }
}