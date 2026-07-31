package com.dic.xsuper.render.javafx.tags.shapes;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;

public class ArcTag extends NativeTag {

    public ArcTag(XplNode sourceNode) { super(sourceNode); }

    @Override
    protected Node createNode() {
        Arc arc = new Arc();
        if (sourceNode.attributes.containsKey("cx")) arc.setCenterX(parseDouble("cx"));
        if (sourceNode.attributes.containsKey("cy")) arc.setCenterY(parseDouble("cy"));
        if (sourceNode.attributes.containsKey("rx")) arc.setRadiusX(parseDouble("rx"));
        if (sourceNode.attributes.containsKey("ry")) arc.setRadiusY(parseDouble("ry"));
        if (sourceNode.attributes.containsKey("start")) arc.setStartAngle(parseDouble("start"));
        if (sourceNode.attributes.containsKey("length")) arc.setLength(parseDouble("length"));

        if (sourceNode.attributes.containsKey("type")) {
            String type = sourceNode.attributes.get("type").toString().toLowerCase();
            switch (type) {
                case "round": arc.setType(ArcType.ROUND); break;
                case "open":  arc.setType(ArcType.OPEN); break;
                default:      arc.setType(ArcType.CHORD);
            }
        }

        parseFillStroke(arc);
        applyCommonStyles();
        return arc;
    }

    private void parseFillStroke(javafx.scene.shape.Shape shape) {
        if (sourceNode.attributes.containsKey("fill")) {
            String fill = sourceNode.attributes.get("fill").toString().trim();
            if (fill.equalsIgnoreCase("none")) {
                shape.setFill(javafx.scene.paint.Color.TRANSPARENT);
            } else {
                try { shape.setFill(javafx.scene.paint.Color.web(fill)); } catch (Exception ignored) {}
            }
        }
        if (sourceNode.attributes.containsKey("stroke")) {
            String stroke = sourceNode.attributes.get("stroke").toString().trim();
            if (stroke.equalsIgnoreCase("none")) {
                shape.setStroke(javafx.scene.paint.Color.TRANSPARENT);
            } else {
                try { shape.setStroke(javafx.scene.paint.Color.web(stroke)); } catch (Exception ignored) {}
            }
        }
        if (sourceNode.attributes.containsKey("stroke-width")) {
            try { shape.setStrokeWidth(Double.parseDouble(sourceNode.attributes.get("stroke-width").toString())); } catch (Exception ignored) {}
        }
    }

    private double parseDouble(String attr) {
        try { return Double.parseDouble(sourceNode.attributes.get(attr).toString()); } catch (Exception e) { return 0.0; }
    }

    @Override protected void applyTagSpecificStyles() {}
}