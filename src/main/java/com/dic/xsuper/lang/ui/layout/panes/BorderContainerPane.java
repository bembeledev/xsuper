package com.dic.xsuper.lang.ui.layout.panes;

import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.properties.cssunit.CssContext;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import java.util.List;

public class BorderContainerPane extends BorderPane implements CustomLayoutPane {

    @Override
    public void populateChildren(List<NativeTag> children, CssContext context) {
        for (NativeTag child : children) {
            Node fxChild = child.build();

            // Lê a região pretendida no CSS (padrão é center)
            String region = child.getRawStyles().getOrDefault("region", "center").toLowerCase().trim();

            switch (region) {
                case "top" -> setTop(fxChild);
                case "bottom" -> setBottom(fxChild);
                case "left" -> setLeft(fxChild);
                case "right" -> setRight(fxChild);
                default -> setCenter(fxChild);
            }
        }
    }
}