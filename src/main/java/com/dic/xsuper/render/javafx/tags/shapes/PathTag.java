package com.dic.xsuper.render.javafx.tags.shapes;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.shape.SVGPath;
import javafx.scene.paint.Color;

public class PathTag extends NativeTag {

    public PathTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        SVGPath path = new SVGPath();

        // 1. A Rota do Vetor (O atributo "d" do SVG W3C)
        if (sourceNode.attributes.containsKey("d")) {
            path.setContent(sourceNode.attributes.get("d").toString());
        }

        // 2. Preenchimento (Fill)
        if (sourceNode.attributes.containsKey("fill")) {
            String fill = sourceNode.attributes.get("fill").toString();
            if (!fill.equals("none")) {
                try { path.setFill(Color.web(fill)); } catch (Exception ignored) {}
            } else {
                path.setFill(Color.TRANSPARENT);
            }
        }

        // 3. Contorno (Stroke)
        if (sourceNode.attributes.containsKey("stroke")) {
            try { path.setStroke(Color.web(sourceNode.attributes.get("stroke").toString())); } catch (Exception ignored) {}
        }
        if (sourceNode.attributes.containsKey("stroke-width")) {
            try { path.setStrokeWidth(Double.parseDouble(sourceNode.attributes.get("stroke-width").toString())); } catch (Exception ignored) {}
        }

        applyCommonStyles();
        return path;
    }

    @Override
    protected void applyTagSpecificStyles() {

    }

    @Override
    protected void addChildren() {
        // SVG Paths não têm filhos DOM na nossa implementação direta
    }
}