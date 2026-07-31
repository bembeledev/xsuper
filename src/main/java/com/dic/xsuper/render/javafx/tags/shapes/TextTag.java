package com.dic.xsuper.render.javafx.tags.shapes;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.dom.properties.style.XplColor;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.Map;

/**
 * Tag SVG <text> – Renderiza texto dentro de um contentor SVG.
 * Suporta atributos: x, y, dx, dy, text-anchor, font-size, font-family,
 * font-weight, fill, stroke, stroke-width, rotate.
 */
public class TextTag extends NativeTag {

    private Text fxText;

    public TextTag(XplNode node) {
        super(node);
    }

    @Override
    protected Node createNode() {
        fxText = new Text();
        applyAttributes();
        applyCommonStyles();
        return fxText;
    }

    private void applyAttributes() {
        Map<String, Object> attrs = sourceNode.attributes;

        // Conteúdo do texto (vem do textContent ou do atributo 'text')
        String textContent = sourceNode.textContent != null ? sourceNode.textContent : "";
        if (attrs.containsKey("text")) {
            textContent = attrs.get("text").toString();
        }
        fxText.setText(textContent);

        // Posicionamento
        if (attrs.containsKey("x")) {
            try { fxText.setX(Double.parseDouble(attrs.get("x").toString())); } catch (NumberFormatException ignored) {}
        }
        if (attrs.containsKey("y")) {
            try { fxText.setY(Double.parseDouble(attrs.get("y").toString())); } catch (NumberFormatException ignored) {}
        }
        if (attrs.containsKey("dx")) {
            try { fxText.setX(fxText.getX() + Double.parseDouble(attrs.get("dx").toString())); } catch (NumberFormatException ignored) {}
        }
        if (attrs.containsKey("dy")) {
            try { fxText.setY(fxText.getY() + Double.parseDouble(attrs.get("dy").toString())); } catch (NumberFormatException ignored) {}
        }

        // Alinhamento horizontal (text-anchor)
        String textAnchor = attrs.containsKey("text-anchor") ? attrs.get("text-anchor").toString() : "start";
        switch (textAnchor.toLowerCase()) {
            case "middle":
                fxText.setTextAlignment(TextAlignment.CENTER);
                fxText.setX(fxText.getX() - fxText.getLayoutBounds().getWidth() / 2);
                break;
            case "end":
                fxText.setTextAlignment(TextAlignment.RIGHT);
                fxText.setX(fxText.getX() - fxText.getLayoutBounds().getWidth());
                break;
            default:
                fxText.setTextAlignment(TextAlignment.LEFT);
                break;
        }

        // Fonte
        String fontFamily = attrs.containsKey("font-family") ? attrs.get("font-family").toString() : "System";
        double fontSize = 12;
        if (attrs.containsKey("font-size")) {
            try { fontSize = Double.parseDouble(attrs.get("font-size").toString()); } catch (NumberFormatException ignored) {}
        }
        FontWeight fontWeight = FontWeight.NORMAL;
        if (attrs.containsKey("font-weight")) {
            String fw = attrs.get("font-weight").toString().toLowerCase();
            if (fw.equals("bold")) fontWeight = FontWeight.BOLD;
        }
        fxText.setFont(Font.font(fontFamily, fontWeight, fontSize));

        // Cor (fill)
        if (attrs.containsKey("fill")) {
            int color = XplColor.resolveColor(attrs.get("fill").toString());
            fxText.setFill(javafx.scene.paint.Color.rgb(
                    (color >> 16) & 0xFF,
                    (color >> 8) & 0xFF,
                    color & 0xFF,
                    ((color >> 24) & 0xFF) / 255.0
            ));
        } else {
            // Padrão SVG: fill preto
            fxText.setFill(javafx.scene.paint.Color.BLACK);
        }

        // Traço (stroke)
        if (attrs.containsKey("stroke")) {
            int color = XplColor.resolveColor(attrs.get("stroke").toString());
            fxText.setStroke(javafx.scene.paint.Color.rgb(
                    (color >> 16) & 0xFF,
                    (color >> 8) & 0xFF,
                    color & 0xFF,
                    ((color >> 24) & 0xFF) / 255.0
            ));
        }
        if (attrs.containsKey("stroke-width")) {
            try {
                fxText.setStrokeWidth(Double.parseDouble(attrs.get("stroke-width").toString()));
            } catch (NumberFormatException ignored) {}
        }

        // Rotação (rotate) – aplica uma rotação ao texto
        if (attrs.containsKey("rotate")) {
            try {
                double angle = Double.parseDouble(attrs.get("rotate").toString());
                fxText.setRotate(angle);
            } catch (NumberFormatException ignored) {}
        }

        // text-anchor também pode ser aplicado via setTextAlignment e ajuste de x
        // Já foi feito acima.
    }

    @Override
    protected void applyTagSpecificStyles() {
        // Nada específico, pois já aplicamos no createNode
    }

    @Override
    protected void addChildren() {
        // A tag <text> não tem filhos (apenas texto)
    }
}