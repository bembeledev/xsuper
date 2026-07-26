package com.dic.xsuper.lang.ui.tags.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class EntityTag extends GameTagBase {

    private EntityType type = EntityType.DEFAULT;
    private double x = 0, y = 0;
    private String sprite;
    private double width = 32, height = 32;
    private boolean bbox = true;

    public EntityTag(XplNode sourceNode) {
        super(sourceNode);
        if (sourceNode.attributes.containsKey("type")) {
            type = parseEntityType(sourceNode.attributes.get("type"));
        }
        x = parseDouble(sourceNode.attributes.get("x"), 0);
        y = parseDouble(sourceNode.attributes.get("y"), 0);
        if (sourceNode.attributes.containsKey("sprite")) {
            sprite = sourceNode.attributes.get("sprite").toString();
        }
        width = parseDouble(sourceNode.attributes.get("width"), 32);
        height = parseDouble(sourceNode.attributes.get("height"), 32);
        if (sourceNode.attributes.containsKey("bbox")) {
            bbox = parseBoolean(sourceNode.attributes.get("bbox"), true);
        }
    }

    @Override
    protected void onGameReady() {
        spawnEntity();
    }

    private void spawnEntity() {
        var builder = FXGL.entityBuilder()
                .at(x, y)
                .type(type);

        // ⭐ Usa fallback se a textura não existir
        if (sprite != null && !sprite.isEmpty()) {
            builder.view(createFallbackTexture(sprite, Color.GRAY, width, height));
        } else {
            builder.view(new Rectangle(width, height, Color.GRAY));
        }

        if (bbox) {
            HitBox hitBox = new HitBox(BoundingShape.box(width, height));
            builder.bbox(hitBox);
        }

        Entity entity = builder.buildAndAttach();
        System.out.println("[EntityTag] Entidade criada: " + type + " em (" + x + ", " + y + ")");
    }

    @Override
    protected Node createNode() {
        return createPlaceholder();
    }
}