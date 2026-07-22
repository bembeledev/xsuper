package com.dic.xsuper.lang.ui.tags.shapes;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class RectangleTag extends NativeTag {

    public RectangleTag(XplNode sourceNode) { super(sourceNode); }

    @Override
    protected Node createNode() {
        Rectangle rect = new Rectangle();
        parseDimension(rect);
        parseFillStroke(rect);
        applyCommonStyles();
        return rect;
    }

    private void parseDimension(Rectangle rect) {
        if (sourceNode.attributes.containsKey("x")) rect.setX(parseDouble("x"));
        if (sourceNode.attributes.containsKey("y")) rect.setY(parseDouble("y"));
        if (sourceNode.attributes.containsKey("width")) rect.setWidth(parseDouble("width"));
        if (sourceNode.attributes.containsKey("height")) rect.setHeight(parseDouble("height"));
        if (sourceNode.attributes.containsKey("rx")) rect.setArcWidth(parseDouble("rx"));
        if (sourceNode.attributes.containsKey("ry")) rect.setArcHeight(parseDouble("ry"));
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