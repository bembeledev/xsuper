package com.dic.xsuper.render.javafx.tags.chart;

import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.chart.*;

import java.util.List;
import java.util.Map;

public class LineChartTag extends ChartBase {

    private boolean symbolsVisible = true;
    private boolean linesVisible = true;
    private boolean smooth = false;

    public LineChartTag(XplNode node) {
        super(node);
    }

    @Override
    protected Chart createChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(xAxisLabel);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yAxisLabel);
        return new LineChart<>(xAxis, yAxis);
    }

    @Override
    protected void applySpecificAttributes() {
        if (sourceNode.attributes.containsKey("symbols")) {
            symbolsVisible = Boolean.parseBoolean(sourceNode.attributes.get("symbols").toString());
        }
        if (sourceNode.attributes.containsKey("lines")) {
            linesVisible = Boolean.parseBoolean(sourceNode.attributes.get("lines").toString());
        }
        if (sourceNode.attributes.containsKey("smooth")) {
            smooth = Boolean.parseBoolean(sourceNode.attributes.get("smooth").toString());
        }
    }

    @Override
    protected void populateData(Object data) {
        if (!(activeChart instanceof LineChart)) return;
        LineChart<String, Number> chart = (LineChart<String, Number>) activeChart;

        // 1. Limpa os dados antigos
        chart.getData().clear();

        // ⭐ 2. Aplica as configurações ANTES de inserir dados para evitar loops internos do JavaFX
        chart.setCreateSymbols(symbolsVisible);

        // 3. Processa a tua Lista XPL
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
            // Insere a série configurada
            chart.getData().add(series);
        }

        // 4. Aplica os estilos CSS aos nós apenas se as linhas estiverem invisíveis ou suaves
        javafx.application.Platform.runLater(() -> {
            if (!linesVisible) {
                chart.getData().forEach(s -> {
                    if (s.getNode() != null) s.getNode().setStyle("-fx-stroke: transparent;");
                });
            }
            if (smooth) {
                chart.getData().forEach(s -> {
                    if (s.getNode() != null) s.getNode().setStyle("-fx-stroke-line-join: round;");
                });
            }
        });
    }
}