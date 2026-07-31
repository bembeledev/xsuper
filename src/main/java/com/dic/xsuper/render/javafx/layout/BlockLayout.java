package com.dic.xsuper.render.javafx.layout;

import com.dic.xsuper.render.javafx.tags.NativeTag;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.Map;

public class BlockLayout implements LayoutManager {
    @Override
    public Pane createContainer(NativeTag tag) {
        VBox vbox = new VBox();
        Map<String, String> rawStyles = tag.getRawStyles();

        // Blocos padrão empilham na vertical
        if (rawStyles.containsKey("gap")) {
            try {
                double gap = Double.parseDouble(rawStyles.get("gap").replace("px", "").trim());
                vbox.setSpacing(gap);
            } catch (NumberFormatException ignored) {}
        }

        return vbox;
    }
}
