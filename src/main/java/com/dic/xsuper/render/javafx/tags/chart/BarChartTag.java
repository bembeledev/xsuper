package com.dic.xsuper.render.javafx.tags.chart;

import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.chart.*;

import java.util.List;
import java.util.Map;

public class BarChartTag extends ChartBase {

    private double gap = 10.0;
    private double barWidth = -1; // -1 = automático

    public BarChartTag(XplNode node) {
        super(node);
    }

    @Override
    protected Chart createChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(xAxisLabel);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yAxisLabel);
        return new BarChart<>(xAxis, yAxis);
    }

    @Override
    protected void applySpecificAttributes() {
        if (sourceNode.attributes.containsKey("gap")) {
            try { gap = Double.parseDouble(sourceNode.attributes.get("gap").toString()); } catch (Exception ignored) {}
        }
        if (sourceNode.attributes.containsKey("bar-width")) {
            try { barWidth = Double.parseDouble(sourceNode.attributes.get("bar-width").toString()); } catch (Exception ignored) {}
        }
    }

    @Override
    protected void populateData(Object data) {
        if (!(activeChart instanceof BarChart)) return;
        BarChart<String, Number> chart = (BarChart<String, Number>) activeChart;
        chart.getData().clear();

        // Aplicar gap e bar-width via CSS (JavaFX não tem método directo)
        String css = "";
        if (gap >= 0) {
            css += "-fx-bar-gap: " + gap + "; ";
        }
        if (barWidth > 0) {
            css += "-fx-bar-width: " + barWidth + "; ";
        }
        if (!css.isEmpty()) {
            chart.setStyle(css);
        }

        List<Map<String, Object>> seriesList = parseSeriesList(data);
        if (seriesList == null) return;

        for (Map<String, Object> seriesMap : seriesList) {
            String seriesName = toString(seriesMap.get("name"));
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(seriesName);

            Object valuesObj = seriesMap.get("values");
            if (valuesObj instanceof List) {
                for (Object item : (List<?>) valuesObj) {
                    if (item instanceof Map) {
                        Map<String, Object> point = (Map<String, Object>) item;
                        String x = toString(point.get("x"));
                        double y = toDouble(point.get("y"));
                        series.getData().add(new XYChart.Data<>(x, y));
                    }
                }
            }
            chart.getData().add(series);
        }
    }
}