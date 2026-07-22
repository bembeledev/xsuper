package com.dic.xsuper.lang.ui.tags.shapes;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;

public class PolylineTag extends NativeTag {

    public PolylineTag(XplNode sourceNode) { super(sourceNode); }

    @Override
    protected Node createNode() {
        Polyline polyline = new Polyline();
        if (sourceNode.attributes.containsKey("points")) {
            String pointsStr = sourceNode.attributes.get("points").toString().trim();
            String[] coords = pointsStr.split("[\\s,]+");

            // ⭐ CORREÇÃO: Adicionamos diretamente à lista, sem usar array double[]
            for (String coord : coords) {
                try {
                    polyline.getPoints().add(Double.parseDouble(coord));
                } catch (NumberFormatException e) {
                    polyline.getPoints().add(0.0);
                }
            }
        }

        if (sourceNode.attributes.containsKey("fill")) {
            try { polyline.setFill(Color.web(sourceNode.attributes.get("fill").toString())); } catch (Exception ignored) {}
        }
        if (sourceNode.attributes.containsKey("stroke")) {
            try { polyline.setStroke(Color.web(sourceNode.attributes.get("stroke").toString())); } catch (Exception ignored) {}
        }
        if (sourceNode.attributes.containsKey("stroke-width")) {
            try { polyline.setStrokeWidth(Double.parseDouble(sourceNode.attributes.get("stroke-width").toString())); } catch (Exception ignored) {}
        }

        applyCommonStyles();
        return polyline;
    }

    @Override protected void applyTagSpecificStyles() {}
}