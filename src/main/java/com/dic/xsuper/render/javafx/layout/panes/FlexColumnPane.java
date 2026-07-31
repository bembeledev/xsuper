package com.dic.xsuper.render.javafx.layout.panes;

import com.dic.xsuper.render.javafx.tags.NativeTag;
import com.dic.xsuper.dom.properties.cssunit.CssContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.util.List;
import java.util.Map;

public class FlexColumnPane extends VBox implements CustomLayoutPane {

    private final Map<String, String> style;

    public FlexColumnPane(Map<String, String> style, double spacing) {
        super(spacing);
        this.style = style != null ? style : Map.of();
        setupAlignment();
    }

    private void setupAlignment() {
        String align = style.getOrDefault("align-items", "stretch").toLowerCase().trim();
        if (align.equals("center")) setAlignment(Pos.TOP_CENTER);
        else if (align.equals("flex-end")) setAlignment(Pos.TOP_RIGHT);
        else if (align.equals("flex-start")) setAlignment(Pos.TOP_LEFT);
        else setAlignment(Pos.TOP_LEFT);
    }

    @Override
    public void populateChildren(List<NativeTag> children, CssContext context) {
        String justify = style.getOrDefault("justify-content", "").toLowerCase().trim();
        boolean spaceBetween = justify.equals("space-between");
        int size = children.size();

        for (int i = 0; i < size; i++) {
            NativeTag child = children.get(i);
            Node builtNode = child.build();

            if ("100%".equals(child.getRawStyles().get("height"))) {
                VBox.setVgrow(builtNode, Priority.ALWAYS);
            }

            getChildren().add(builtNode);

            if (spaceBetween && i < size - 1) {
                Region spacer = new Region();
                VBox.setVgrow(spacer, Priority.ALWAYS);
                getChildren().add(spacer);
            }

            applyMargins(builtNode, child, context);
        }
    }

    private void applyMargins(Node fxChild, NativeTag child, CssContext context) {
        if (!child.getResolvedStyles().margin.isZero()) {
            Insets m = new Insets(
                    child.getResolvedStyles().margin.getTopPixels(context),
                    child.getResolvedStyles().margin.getRightPixels(context),
                    child.getResolvedStyles().margin.getBottomPixels(context),
                    child.getResolvedStyles().margin.getLeftPixels(context)
            );
            VBox.setMargin(fxChild, m);
        }
    }
}