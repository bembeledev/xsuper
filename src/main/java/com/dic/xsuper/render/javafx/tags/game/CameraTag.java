package com.dic.xsuper.render.javafx.tags.game;

import com.almasb.fxgl.dsl.FXGL;
import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.Node;

public class CameraTag extends GameTagBase {

    private String target = "player";
    private double zoom = 1.0;
    private double boundsX = 0, boundsY = 0, boundsWidth = 0, boundsHeight = 0;

    public CameraTag(XplNode sourceNode) {
        super(sourceNode);
        if (sourceNode.attributes.containsKey("target")) {
            target = sourceNode.attributes.get("target").toString();
        }
        zoom = parseDouble(sourceNode.attributes.get("zoom"), 1.0);
        boundsX = parseDouble(sourceNode.attributes.get("bounds-x"), 0);
        boundsY = parseDouble(sourceNode.attributes.get("bounds-y"), 0);
        boundsWidth = parseDouble(sourceNode.attributes.get("bounds-width"), 0);
        boundsHeight = parseDouble(sourceNode.attributes.get("bounds-height"), 0);
    }

    @Override
    protected void onGameReady() {
        var viewport = FXGL.getGameScene().getViewport();
        viewport.setZoom(zoom);

        if (boundsWidth > 0 && boundsHeight > 0) {
            viewport.setBounds((int) boundsX, (int) boundsY, (int) boundsWidth, (int) boundsHeight);
        }

        // O binding ao target é feito no PlayerTag
        System.out.println("[CameraTag] Câmara configurada (zoom=" + zoom + ")");
    }

    @Override
    protected Node createNode() {
        return createPlaceholder();
    }
}