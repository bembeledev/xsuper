package com.dic.xsuper.render.javafx.layout;

import com.dic.xsuper.render.javafx.tags.NativeTag;
import com.dic.xsuper.render.javafx.layout.panes.FlowContainerPane;
import javafx.scene.layout.Pane;
import java.util.Map;

public class FlowLayout implements LayoutManager {
    @Override
    public Pane createContainer(NativeTag tag) {
        Map<String, String> styles = tag.getRawStyles();

        // Puxa o gap geral, se existir
        double gap = 0;
        if (styles.containsKey("gap")) {
            try {
                gap = Double.parseDouble(styles.get("gap").replace("px", "").trim());
            } catch (NumberFormatException ignored) {}
        }

        return new FlowContainerPane(styles, gap);
    }
}