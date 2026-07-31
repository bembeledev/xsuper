package com.dic.xsuper.render.javafx.tags.chart;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import eu.hansolo.fx.charts.ConcentricRingChart;
import eu.hansolo.fx.charts.SectorChart;
import eu.hansolo.fx.charts.data.ChartItem;
import eu.hansolo.fx.charts.series.ChartItemSeries;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CircularChartTag extends NativeTag {

    private StackPane container;
    private boolean isDonut;
    private ChartItemSeries<ChartItem> series;
    private ConcentricRingChart donutChart;
    private SectorChart pieChart;
    private final Map<String, ChartItem> liveItems = new HashMap<>();

    public CircularChartTag(XplNode node) {
        super(node);
        String donutAttr = sourceNode.attributes.getOrDefault("donut", "false").toString();
        this.isDonut = "true".equalsIgnoreCase(donutAttr);
    }

    @Override
    protected Node createNode() {
        container = new StackPane();

        // 1. CARREGA OS DADOS (Extrai Dicionário XPL e transforma em ChartItems)
        if (sourceNode.attributes.containsKey("data")) {
            injectOrUpdateData(sourceNode.attributes.get("data"));
        }

        // 2. INSTANCIA GRÁFICOS COM ESCALA CALCULADA
        if (isDonut) {
            ChartItem[] items = liveItems.values().toArray(new ChartItem[0]);
            donutChart = new ConcentricRingChart(items);
            container.getChildren().add(donutChart);
        } else {
            series = new ChartItemSeries<>();
            series.setName(sourceNode.attributes.getOrDefault("title", "Pizza").toString());
            series.getItems().addAll(liveItems.values());
            pieChart = new SectorChart(Arrays.asList(series));
            container.getChildren().add(pieChart);
        }

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

        // 🚨 ESCUDO ANTI-SILÊNCIO: Garante que o XPL envia um Mapa (Dicionário)
        if (!(rawData instanceof Map)) {
            System.err.println("❌ [CircularChartTag] Erro Crítico: O atributo 'data' não é um Dicionário/Mapa! Recebido: " + rawData.getClass().getName());
            return;
        }

        Map<String, Object> dataMap = (Map<String, Object>) rawData;
        String colorsStr = sourceNode.attributes.getOrDefault("colors", "#38bdf8,#f43f5e,#10b981,#f59e0b").toString();
        String[] colors = colorsStr.split(",");

        int i = 0;
        java.util.List<ChartItem> newDonutItems = new java.util.ArrayList<>();
        java.util.List<ChartItem> newPieItems = new java.util.ArrayList<>();

        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            String name = entry.getKey();
            double value = 0;
            try { value = Double.parseDouble(String.valueOf(entry.getValue())); } catch (Exception ignored) {}

            String colorHex = colors[i % colors.length].trim();
            Color fxColor;
            try { fxColor = Color.web(colorHex); } catch (Exception e) { fxColor = Color.GRAY; }

            if (liveItems.containsKey(name)) {
                ChartItem item = liveItems.get(name);
                item.setValue(value);
                item.setFill(fxColor);
            } else {
                ChartItem newItem = new ChartItem(name, value, fxColor);
                liveItems.put(name, newItem);

                if (isDonut) newDonutItems.add(newItem);
                else newPieItems.add(newItem);
            }
            i++;
        }

        // Se injetarmos novos dados após a renderização inicial, passamos ao gráfico
        if (isDonut && !newDonutItems.isEmpty() && donutChart != null) donutChart.addItems(newDonutItems);
        else if (!isDonut && !newPieItems.isEmpty() && series != null) series.getItems().addAll(newPieItems);
    }

    @Override protected void applyTagSpecificStyles() {}
    @Override protected void addChildren() {}
}