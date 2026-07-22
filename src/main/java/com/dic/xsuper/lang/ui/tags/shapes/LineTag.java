package com.dic.xsuper.lang.ui.tags.shapes;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class LineTag extends NativeTag {

    public LineTag(XplNode sourceNode) { super(sourceNode); }

    @Override
    protected Node createNode() {
        Line line = new Line();
        if (sourceNode.attributes.containsKey("x1")) line.setStartX(parseDouble("x1"));
        if (sourceNode.attributes.containsKey("y1")) line.setStartY(parseDouble("y1"));
        if (sourceNode.attributes.containsKey("x2")) line.setEndX(parseDouble("x2"));
        if (sourceNode.attributes.containsKey("y2")) line.setEndY(parseDouble("y2"));

        if (sourceNode.attributes.containsKey("stroke")) {
            try { line.setStroke(Color.web(sourceNode.attributes.get("stroke").toString())); } catch (Exception ignored) {}
        }
        if (sourceNode.attributes.containsKey("stroke-width")) {
            try { line.setStrokeWidth(Double.parseDouble(sourceNode.attributes.get("stroke-width").toString())); } catch (Exception ignored) {}
        }

        applyCommonStyles();
        return line;
    }

    private double parseDouble(String attr) {
        try { return Double.parseDouble(sourceNode.attributes.get(attr).toString()); } catch (Exception e) { return 0.0; }
    }

    @Override protected void applyTagSpecificStyles() {}
}