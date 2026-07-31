package com.dic.xsuper.render.javafx.tags.controls;

import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ImageInputTag extends ButtonInputTag {

    public ImageInputTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        fxButton = new Button();

        if (sourceNode.attributes.containsKey("src")) {
            String src = (String) sourceNode.attributes.get("src");
            try {
                Image img = new Image(src);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(16);
                iv.setFitHeight(16);
                fxButton.setGraphic(iv);
            } catch (Exception e) {
                fxButton.setText(src);
            }
        }

        if (!value.isEmpty()) fxButton.setText(value);

        applyCommonAttributes(fxButton);

        return fxButton;
    }
}