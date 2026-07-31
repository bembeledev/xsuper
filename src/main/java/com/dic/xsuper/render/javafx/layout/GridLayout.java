package com.dic.xsuper.render.javafx.layout;

import com.dic.xsuper.render.javafx.tags.NativeTag;
import com.dic.xsuper.render.javafx.layout.panes.GridContainerPane;
import com.dic.xsuper.render.javafx.layout.grid.GridDimension;
import com.dic.xsuper.render.javafx.layout.grid.GridTemplateParser;
import javafx.scene.layout.Pane;
import java.util.List;
import java.util.Map;

public class GridLayout implements LayoutManager {
    @Override
    public Pane createContainer(NativeTag tag) {
        Map<String, String> styles = tag.getRawStyles();

        // Parse das colunas e linhas
        List<GridDimension> columns = parseGridTemplate(styles.get("grid-template-columns"));
        List<GridDimension> rows = parseGridTemplate(styles.get("grid-template-rows"));

        // Cria o GridContainerPane com as dimensões
        GridContainerPane grid = new GridContainerPane(styles, columns, rows);

        // Configurar gap
        String gap = styles.get("gap");
        if (gap != null) {
            try {
                double gapVal = Double.parseDouble(gap.replace("px", "").trim());
                grid.setHgap(gapVal);
                grid.setVgap(gapVal);
            } catch (Exception ignored) {}
        }

        // Configurar alinhamentos (serão aplicados no GridContainerPane)
        // justify-items, align-items, justify-content, align-content

        return grid;
    }

    private List<GridDimension> parseGridTemplate(String value) {
        if (value == null || value.trim().isEmpty()) return List.of();
        return GridTemplateParser.parse(value);
    }
}