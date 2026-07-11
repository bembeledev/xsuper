package com.dic.xsuper.lang.ui.layout;

import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.layout.panes.GridContainerPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Pane;
import java.util.Map;

public class GridLayout implements LayoutManager {
    @Override
    public Pane createContainer(NativeTag tag) {
        Map<String, String> styles = tag.getRawStyles();

        // ⭐ Instancia o nosso Pane Especializado
        GridContainerPane grid = new GridContainerPane(styles);

        // 1. GAP (Espaçamento entre células)
        if (styles.containsKey("gap")) {
            try {
                double gap = Double.parseDouble(styles.get("gap").replace("px", "").trim());
                grid.setHgap(gap);
                grid.setVgap(gap);
            } catch (Exception ignored) {}
        }

        // 2. A MATEMÁTICA DO GRID-TEMPLATE-COLUMNS (Restrições percentuais / fr)
        if (styles.containsKey("grid-template-columns")) {
            String[] cols = styles.get("grid-template-columns").trim().split("\\s+");

            double totalFr = 0;
            for (String col : cols) {
                if (col.endsWith("fr")) {
                    try {
                        totalFr += Double.parseDouble(col.replace("fr", "").trim());
                    } catch (NumberFormatException e) {
                        totalFr += 1.0;
                    }
                }
            }

            for (String col : cols) {
                ColumnConstraints constraint = new ColumnConstraints();

                if (col.endsWith("fr")) {
                    double frValue = 1.0;
                    try {
                        frValue = Double.parseDouble(col.replace("fr", "").trim());
                    } catch (NumberFormatException ignored) {}

                    if (totalFr > 0) {
                        constraint.setPercentWidth((frValue / totalFr) * 100);
                    }
                } else if (col.endsWith("%")) {
                    try {
                        constraint.setPercentWidth(Double.parseDouble(col.replace("%", "").trim()));
                    } catch (NumberFormatException ignored) {}
                } else if (col.endsWith("px")) {
                    try {
                        constraint.setPrefWidth(Double.parseDouble(col.replace("px", "").trim()));
                    } catch (NumberFormatException ignored) {}
                }

                grid.getColumnConstraints().add(constraint);
            }
        }

        return grid;
    }
}