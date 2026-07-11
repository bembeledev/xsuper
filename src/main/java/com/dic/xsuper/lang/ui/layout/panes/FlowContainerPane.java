package com.dic.xsuper.lang.ui.layout.panes;

import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.properties.cssunit.CssContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import java.util.List;
import java.util.Map;

public class FlowContainerPane extends FlowPane implements CustomLayoutPane {

    private final Map<String, String> style;

    public FlowContainerPane(Map<String, String> style, double gap) {
        super(gap, gap); // Aplica o gap tanto na horizontal como na vertical
        this.style = style != null ? style : Map.of();
        setupAlignment();
    }

    private void setupAlignment() {
        String justify = style.getOrDefault("justify-content", "flex-start").toLowerCase().trim();
        String align = style.getOrDefault("align-items", "flex-start").toLowerCase().trim();

        // O FlowPane precisa de uma combinação de X e Y (Pos)
        if (justify.equals("center")) {
            if (align.equals("center")) setAlignment(Pos.CENTER);
            else setAlignment(Pos.TOP_CENTER);
        } else if (justify.equals("flex-end")) {
            if (align.equals("flex-end")) setAlignment(Pos.BOTTOM_RIGHT);
            else setAlignment(Pos.TOP_RIGHT);
        } else {
            // flex-start
            if (align.equals("center")) setAlignment(Pos.CENTER_LEFT);
            else if (align.equals("flex-end")) setAlignment(Pos.BOTTOM_LEFT);
            else setAlignment(Pos.TOP_LEFT);
        }
    }

    @Override
    public void populateChildren(List<NativeTag> children, CssContext context) {
        for (NativeTag child : children) {
            Node fxChild = child.build();
            applyMargins(fxChild, child, context);
            getChildren().add(fxChild);
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
            FlowPane.setMargin(targetNode, m);
        }
    }
}