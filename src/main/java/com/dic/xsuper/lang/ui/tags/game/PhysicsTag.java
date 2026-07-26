package com.dic.xsuper.lang.ui.tags.game;

import com.almasb.fxgl.dsl.FXGL;
import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.scene.Node;

public class PhysicsTag extends GameTagBase {

    private double gravityX = 0;
    private double gravityY = 9.8;
    private int velocityIterations = 8;
    private int positionIterations = 3;

    public PhysicsTag(XplNode sourceNode) {
        super(sourceNode);
        gravityX = parseDouble(sourceNode.attributes.get("gravity-x"), 0);
        gravityY = parseDouble(sourceNode.attributes.get("gravity-y"), 9.8);
        velocityIterations = parseInt(sourceNode.attributes.get("velocity-iterations"), 8);
        positionIterations = parseInt(sourceNode.attributes.get("position-iterations"), 3);
    }

    @Override
    protected void onGameReady() {
        if (FXGL.getPhysicsWorld() != null) {
            FXGL.getPhysicsWorld().setGravity(gravityX, gravityY);
            System.out.println("[PhysicsTag] Gravidade definida: (" + gravityX + ", " + gravityY + ")");
        }
    }

    @Override
    protected Node createNode() {
        return createPlaceholder();
    }
}