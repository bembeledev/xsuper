package com.dic.xsuper.lang.ui.tags.editor;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ColorPreviewProvider implements GutterProvider {
    @Override
    public Node createGraphic(int lineIndex, String text) {
        Color foundColor = MetadataCache.getInstance().getColorForLine(lineIndex);

        if (foundColor != null) {
            Rectangle colorBox = new Rectangle(12, 12);
            colorBox.setFill(foundColor);
            colorBox.setStroke(Color.GRAY);
            colorBox.setStrokeWidth(1);
            colorBox.getStyleClass().add("superui-color-box");
            return colorBox;
        }
        return null;
    }
}