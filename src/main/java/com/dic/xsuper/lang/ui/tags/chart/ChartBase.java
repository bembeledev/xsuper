package com.dic.xsuper.lang.ui.tags.chart;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.Chart;
import javafx.scene.layout.StackPane;

import java.util.List;
import java.util.Map;

public abstract class ChartBase extends NativeTag {

    protected StackPane container;
    protected Chart activeChart;
    protected String title;
    protected String xAxisLabel = "X";
    protected String yAxisLabel = "Y";
    protected boolean animated = true;
    protected boolean legendVisible = true;
    protected Side legendSide = Side.TOP;

    public ChartBase(XplNode node) {
        super(node);
        parseCommonAttributes();
    }

    private void parseCommonAttributes() {
        if (sourceNode.attributes.containsKey("title")) {
            title = sourceNode.attributes.get("title").toString();
        }
        if (sourceNode.attributes.containsKey("x-axis")) {
            xAxisLabel = sourceNode.attributes.get("x-axis").toString();
        }
        if (sourceNode.attributes.containsKey("y-axis")) {
            yAxisLabel = sourceNode.attributes.get("y-axis").toString();
        }
        if (sourceNode.attributes.containsKey("animated")) {
            animated = Boolean.parseBoolean(sourceNode.attributes.get("animated").toString());
        }
        if (sourceNode.attributes.containsKey("legend")) {
            legendVisible = Boolean.parseBoolean(sourceNode.attributes.get("legend").toString());
        }
        if (sourceNode.attributes.containsKey("legend-side")) {
            try {
                legendSide = Side.valueOf(sourceNode.attributes.get("legend-side").toString().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
    }

    @Override
    protected Node createNode() {
        container = new StackPane();
        return container;
    }

    @Override
    protected void applyTagSpecificStyles() {
        container.getChildren().clear();
        activeChart = createChart();
        if (activeChart == null) return;

        if (title != null) activeChart.setTitle(title);
        activeChart.setAnimated(animated);
        activeChart.setLegendVisible(legendVisible);
        activeChart.setLegendSide(legendSide);

        applySpecificAttributes();

        if (sourceNode.attributes.containsKey("data")) {
            populateData(sourceNode.attributes.get("data"));
        }

        container.getChildren().add(activeChart);
    }

    protected abstract Chart createChart();
    protected void applySpecificAttributes() {}
    protected void populateData(Object data) {}

    // ─── Helpers ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> parseSeriesList(Object data) {
        // Se for uma Lista Nativa do Java
        if (data instanceof List) {
            return (List<Map<String, Object>>) data;
        }
        // ⭐ SE O XPL DEVOLVER UM ARRAY DE OBJETOS (Object[])
        else if (data instanceof Object[] arr) {
            List<Map<String, Object>> list = new java.util.ArrayList<>();
            for (Object item : arr) {
                if (item instanceof Map) {
                    list.add((Map<String, Object>) item);
                }
            }
            return list;
        }
        return null;
    }

    protected List<Object> parseList(Object data) {
        if (data instanceof List) {
            return (List<Object>) data;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> parseMap(Object data) {
        if (data instanceof Map) {
            return (Map<String, Object>) data;
        }
        return null;
    }

    protected double toDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try { return Double.parseDouble(obj.toString()); } catch (Exception e) { return 0.0; }
    }

    protected String toString(Object obj) {
        return obj != null ? obj.toString() : "";
    }
}