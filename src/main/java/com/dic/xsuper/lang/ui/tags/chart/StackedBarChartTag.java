package com.dic.xsuper.lang.ui.tags.chart;

import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.scene.chart.*;

import java.util.List;
import java.util.Map;

public class StackedBarChartTag extends ChartBase {

    public StackedBarChartTag(XplNode node) {
        super(node);
    }

    @Override
    protected Chart createChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(xAxisLabel);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yAxisLabel);
        return new StackedBarChart<>(xAxis, yAxis);
    }

    @Override
    protected void populateData(Object data) {
        if (!(activeChart instanceof StackedBarChart)) return;
        StackedBarChart<String, Number> chart = (StackedBarChart<String, Number>) activeChart;
        chart.getData().clear();

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