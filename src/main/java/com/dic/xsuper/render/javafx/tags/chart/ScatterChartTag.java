package com.dic.xsuper.render.javafx.tags.chart;

import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.chart.Chart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;

import java.util.List;
import java.util.Map;

public class ScatterChartTag extends ChartBase {

    private String symbolType = "circle"; // circle, cross, etc.

    public ScatterChartTag(XplNode node) {
        super(node);
    }

    @Override
    protected Chart createChart() {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel(xAxisLabel);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yAxisLabel);
        return new ScatterChart<>(xAxis, yAxis);
    }

    @Override
    protected void applySpecificAttributes() {
        if (sourceNode.attributes.containsKey("symbol")) {
            symbolType = sourceNode.attributes.get("symbol").toString().toLowerCase();
        }
    }

    @Override
    protected void populateData(Object data) {
        if (!(activeChart instanceof ScatterChart)) return;
        ScatterChart<Number, Number> chart = (ScatterChart<Number, Number>) activeChart;
        chart.getData().clear();

        // Aplicar símbolo (via CSS)
        String shape = switch (symbolType) {
            case "cross" -> "CROSS";
            case "square" -> "SQUARE";
            case "diamond" -> "DIAMOND";
            default -> "CIRCLE";
        };
        chart.setStyle("-fx-symbol: " + shape + ";");

        List<Map<String, Object>> seriesList = parseSeriesList(data);
        if (seriesList == null) return;

        for (Map<String, Object> seriesMap : seriesList) {
            String seriesName = toString(seriesMap.get("name"));
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(seriesName);

            Object valuesObj = seriesMap.get("values");
            if (valuesObj instanceof List) {
                for (Object item : (List<?>) valuesObj) {
                    if (item instanceof Map) {
                        Map<String, Object> point = (Map<String, Object>) item;
                        double x = toDouble(point.get("x"));
                        double y = toDouble(point.get("y"));
                        series.getData().add(new XYChart.Data<>(x, y));
                    }
                }
            }
            chart.getData().add(series);
        }
    }
}