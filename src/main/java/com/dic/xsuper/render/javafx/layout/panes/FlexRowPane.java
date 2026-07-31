package com.dic.xsuper.render.javafx.layout.panes;

import com.dic.xsuper.render.javafx.tags.NativeTag;
import com.dic.xsuper.dom.properties.cssunit.CssContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import java.util.List;
import java.util.Map;

public class FlexRowPane extends HBox implements CustomLayoutPane {

    private final Map<String, String> style;

    public FlexRowPane(Map<String, String> style, double spacing) {
        super(spacing);
        this.style = style != null ? style : Map.of();
        setupAlignment();
    }

    private void setupAlignment() {
        String align = style.getOrDefault("align-items", "center").toLowerCase().trim();
        switch (align) {
            case "center" -> setAlignment(Pos.CENTER_LEFT);
            case "flex-end" -> setAlignment(Pos.BOTTOM_LEFT);
            case "flex-start" -> setAlignment(Pos.TOP_LEFT);
            default -> setAlignment(Pos.CENTER_LEFT);
        }
    }

    @Override
    public void populateChildren(List<NativeTag> children, CssContext context) {
        String justify = style.getOrDefault("justify-content", "").toLowerCase().trim();
        boolean spaceBetween = justify.equals("space-between");
        int size = children.size();

        for (int i = 0; i < size; i++) {
            NativeTag child = children.get(i);
            Node fxChild = child.build();

            if ("100%".equals(child.getRawStyles().get("width"))) {
                HBox.setHgrow(fxChild, Priority.ALWAYS);
            }

            getChildren().add(fxChild);

            if (spaceBetween && i < size - 1) {
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                getChildren().add(spacer);
            }

            applyMargins(fxChild, child, context);
        }
    }

    private void applyMargins(Node targetNode, NativeTag child, CssContext context) {
        if (!child.getResolvedStyles().margin.isZero()) {
            Insets m = new Insets(
                    child.getResolvedStyles().margin.getTopPixels(context),
                    child.getResolvedStyles().margin.getRightPixels(context),
                    child.getResolvedStyles().margin.getBottomPixels(context),
                    child.getResolvedStyles().margin.getLeftPixels(context)
            );
            HBox.setMargin(targetNode, m);
        }
    }
}