package com.dic.xsuper.lang.ui.tags.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.scene.Node;
import javafx.scene.paint.Color;

public class PowerupTag extends GameTagBase {

    private double x = 0, y = 0;
    private String type = "health";
    private String sprite = "powerup.png";

    public PowerupTag(XplNode sourceNode) {
        super(sourceNode);
        x = parseDouble(sourceNode.attributes.get("x"), 0);
        y = parseDouble(sourceNode.attributes.get("y"), 0);
        if (sourceNode.attributes.containsKey("type")) {
            type = sourceNode.attributes.get("type").toString();
        }
        if (sourceNode.attributes.containsKey("sprite")) {
            sprite = sourceNode.attributes.get("sprite").toString();
        }
    }

    @Override
    protected void onGameReady() {
        Entity powerup = FXGL.entityBuilder()
                .at(x, y)
                .view(createFallbackTexture(sprite, Color.GREEN, 24, 24))
                .bbox(new HitBox(BoundingShape.box(24, 24)))
                .type(EntityType.POWERUP)
                .with(new PowerupComponent(type))
                .buildAndAttach();
        System.out.println("[PowerupTag] Power-up criado: " + type + " em (" + x + ", " + y + ")");
    }

    @Override
    protected Node createNode() {
        return createPlaceholder();
    }

    private static class PowerupComponent extends com.almasb.fxgl.entity.component.Component {
        private final String type;
        public PowerupComponent(String type) { this.type = type; }
    }
}