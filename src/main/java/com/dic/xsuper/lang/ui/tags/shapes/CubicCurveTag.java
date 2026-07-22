package com.dic.xsuper.lang.ui.tags.shapes;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;

public class CubicCurveTag extends NativeTag {

    public CubicCurveTag(XplNode sourceNode) { super(sourceNode); }

    @Override
    protected Node createNode() {
        CubicCurve curve = new CubicCurve();
        if (sourceNode.attributes.containsKey("startX")) curve.setStartX(parseDouble("startX"));
        if (sourceNode.attributes.containsKey("startY")) curve.setStartY(parseDouble("startY"));
        if (sourceNode.attributes.containsKey("controlX1")) curve.setControlX1(parseDouble("controlX1"));
        if (sourceNode.attributes.containsKey("controlY1")) curve.setControlY1(parseDouble("controlY1"));
        if (sourceNode.attributes.containsKey("controlX2")) curve.setControlX2(parseDouble("controlX2"));
        if (sourceNode.attributes.containsKey("controlY2")) curve.setControlY2(parseDouble("controlY2"));
        if (sourceNode.attributes.containsKey("endX")) curve.setEndX(parseDouble("endX"));
        if (sourceNode.attributes.containsKey("endY")) curve.setEndY(parseDouble("endY"));

        parseFillStroke(curve);
        applyCommonStyles();
        return curve;
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