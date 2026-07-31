package com.dic.xsuper.render.javafx.tags.shapes;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

public class PolygonTag extends NativeTag {

    public PolygonTag(XplNode sourceNode) { super(sourceNode); }

    @Override
    protected Node createNode() {
        Polygon polygon = new Polygon();
        if (sourceNode.attributes.containsKey("points")) {
            String pointsStr = sourceNode.attributes.get("points").toString().trim();
            String[] coords = pointsStr.split("[\\s,]+");

            // ⭐ CORREÇÃO: Adicionamos diretamente à lista
            for (String coord : coords) {
                try {
                    polygon.getPoints().add(Double.parseDouble(coord));
                } catch (NumberFormatException e) {
                    polygon.getPoints().add(0.0);
                }
            }
        }

        if (sourceNode.attributes.containsKey("fill")) {
            try { polygon.setFill(Color.web(sourceNode.attributes.get("fill").toString())); } catch (Exception ignored) {}
        }
        if (sourceNode.attributes.containsKey("stroke")) {
            try { polygon.setStroke(Color.web(sourceNode.attributes.get("stroke").toString())); } catch (Exception ignored) {}
        }
        if (sourceNode.attributes.containsKey("stroke-width")) {
            try { polygon.setStrokeWidth(Double.parseDouble(sourceNode.attributes.get("stroke-width").toString())); } catch (Exception ignored) {}
        }

        applyCommonStyles();
        return polygon;
    }

    @Override protected void applyTagSpecificStyles() {}
}