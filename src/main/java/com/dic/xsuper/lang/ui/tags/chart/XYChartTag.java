package com.dic.xsuper.lang.ui.tags.chart;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import eu.hansolo.fx.charts.ChartType;
import eu.hansolo.fx.charts.XYChart;
import eu.hansolo.fx.charts.XYPane;
import eu.hansolo.fx.charts.data.XYChartItem;
import eu.hansolo.fx.charts.series.XYSeries;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XYChartTag extends NativeTag {

    private StackPane container;
    private XYPane<XYChartItem> xyPane;
    private ChartType chartType;
    private final Map<String, XYSeries<XYChartItem>> liveSeries = new HashMap<>();

    public XYChartTag(XplNode node) {
        super(node);
        String modelAttr = sourceNode.attributes.getOrDefault("model", "line").toString().toLowerCase();
        this.chartType = switch (modelAttr) {
            case "bar" -> ChartType.BAR;
            case "area" -> ChartType.AREA;
            case "smooth-line" -> ChartType.SMOOTH_LINE;
            case "smooth-area" -> ChartType.SMOOTH_AREA;
            case "scatter" -> ChartType.SCATTER;
            default -> ChartType.LINE;
        };
    }

    @Override
    protected Node createNode() {
        container = new StackPane();

        // 1. EXTRAI OS DADOS PRIMEIRO (Antes de o gráfico existir fisicamente!)
        if (sourceNode.attributes.containsKey("data")) {
            injectOrUpdateData(sourceNode.attributes.get("data"));
        }

        // 2. INSTANCIA O HANSOLO JÁ ALIMENTADO COM AS SÉRIES (Resolve o ecrã em branco!)
        XYSeries[] seriesArray = liveSeries.values().toArray(new XYSeries[0]);
        xyPane = new XYPane<>(seriesArray);

        XYChart<XYChartItem> chart = new XYChart<>(xyPane);
        container.getChildren().add(chart);

        applyCommonStyles();
        return container;
    }

    @Override
    public void onReactiveAttributeChange(String attrName, Object newValue) {
        super.onReactiveAttributeChange(attrName, newValue);
        if ("data".equalsIgnoreCase(attrName)) {
            Platform.runLater(() -> injectOrUpdateData(newValue));
        }
    }

    @SuppressWarnings("unchecked")
    private void injectOrUpdateData(Object rawData) {
        if (rawData == null) return;

        // 🚨 ESCUDO ANTI-SILÊNCIO: Garante que o XPL envia uma Lista
        if (!(rawData instanceof List)) {
            System.err.println("❌ [XYChartTag] Erro Crítico: O atributo 'data' não é uma Lista! Recebido: " + rawData.getClass().getName());
            return;
        }

        List<Map<String, Object>> seriesList = (List<Map<String, Object>>) rawData;

        for (Map<String, Object> seriesMap : seriesList) {
            String seriesName = seriesMap.getOrDefault("name", "Série").toString();
            List<Map<String, Object>> values = (List<Map<String, Object>>) seriesMap.get("values");
            if (values == null) continue;

            XYSeries<XYChartItem> series = liveSeries.get(seriesName);
            if (series == null) {
                series = new XYSeries<>();
                series.setName(seriesName);
                series.setChartType(chartType);
                liveSeries.put(seriesName, series);

                // Só adicionamos dinamicamente se o painel já existir (em updates futuros)
                if (xyPane != null) xyPane.getListOfSeries().add(series);
            }

            int index = 0;
            List<XYChartItem> newItems = new java.util.ArrayList<>();

            for (Map<String, Object> val : values) {
                Object xObj = val.get("x");
                Object yObj = val.get("y");

                double yVal = (yObj instanceof Number) ? ((Number)yObj).doubleValue() : parseDoubleSeguro(yObj);
                double xVal = (xObj instanceof Number) ? ((Number)xObj).doubleValue() : index;
                String xName = (xObj != null) ? xObj.toString() : "";

                if (index < series.getItems().size()) {
                    XYChartItem item = series.getItems().get(index);
                    item.setX(xVal);
                    item.setY(yVal);
                } else {
                    newItems.add(new XYChartItem(xVal, yVal, xName));
                }
                index++;
            }
            if (!newItems.isEmpty()) series.getItems().addAll(newItems);
        }
    }

    private double parseDoubleSeguro(Object obj) {
        try { return Double.parseDouble(String.valueOf(obj)); } catch (Exception e) { return 0.0; }
    }

    @Override protected void applyTagSpecificStyles() {}
    @Override protected void addChildren() {}
}