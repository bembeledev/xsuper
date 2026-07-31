package com.dic.xsuper.render.javafx.tags.shapes;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;

public class CircleTag extends NativeTag {

    public CircleTag(XplNode sourceNode) { super(sourceNode); }

    @Override
    protected Node createNode() {
        Circle circle = new Circle();

        // Coordenadas e Raio do padrão W3C
        if (sourceNode.attributes.containsKey("cx")) circle.setCenterX(parseDouble("cx"));
        if (sourceNode.attributes.containsKey("cy")) circle.setCenterY(parseDouble("cy"));
        if (sourceNode.attributes.containsKey("r")) circle.setRadius(parseDouble("r"));

        // Pintura
        if (sourceNode.attributes.containsKey("fill")) {
            try { circle.setFill(Color.web(sourceNode.attributes.get("fill").toString())); } catch (Exception ignored) {}
        }

        applyCommonStyles();
        return circle;
    }

    @Override
    protected void applyTagSpecificStyles() {

    }

    private double parseDouble(String attr) {
        try { return Double.parseDouble(sourceNode.attributes.get(attr).toString()); } catch (Exception e) { return 0.0; }
    }
}