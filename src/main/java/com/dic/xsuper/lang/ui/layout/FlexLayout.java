package com.dic.xsuper.lang.ui.layout;

import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.layout.panes.FlexColumnPane;
import com.dic.xsuper.lang.ui.layout.panes.FlexRowPane;
import javafx.scene.layout.Pane;
import java.util.Map;

public class FlexLayout implements LayoutManager {
    @Override
    public Pane createContainer(NativeTag tag) {
        Map<String, String> styles = tag.getRawStyles();
        String direction = styles.getOrDefault("flex-direction", "row").toLowerCase().trim();

        double gap = 0;
        if (styles.containsKey("gap")) {
            try {
                gap = Double.parseDouble(styles.get("gap").replace("px", "").trim());
            } catch (NumberFormatException ignored) {}
        }

        if (direction.contains("column")) {
            return new FlexColumnPane(styles, gap);
        } else {
            return new FlexRowPane(styles, gap);
        }
    }
}