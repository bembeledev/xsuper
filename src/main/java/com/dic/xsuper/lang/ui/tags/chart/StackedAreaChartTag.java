package com.dic.xsuper.lang.ui.tags.chart;

import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.scene.chart.*;

import java.util.List;
import java.util.Map;

public class StackedAreaChartTag extends ChartBase {

    private boolean smooth = false;

    public StackedAreaChartTag(XplNode node) {
        super(node);
    }

    @Override
    protected Chart createChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(xAxisLabel);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yAxisLabel);
        return new StackedAreaChart<>(xAxis, yAxis);
    }

    @Override
    protected void applySpecificAttributes() {
        if (sourceNode.attributes.containsKey("smooth")) {
            smooth = Boolean.parseBoolean(sourceNode.attributes.get("smooth").toString());
        }
    }

    @Override
    protected void populateData(Object data) {
        if (!(activeChart instanceof StackedAreaChart)) return;
        StackedAreaChart<String, Number> chart = (StackedAreaChart<String, Number>) activeChart;
        chart.getData().clear();

        if (smooth) {
            chart.getData().forEach(s -> s.getNode().setStyle("-fx-stroke-line-join: round;"));
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