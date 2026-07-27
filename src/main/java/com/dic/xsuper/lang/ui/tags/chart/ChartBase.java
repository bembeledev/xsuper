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

        // ⭐ A CURA DOS GRÁFICOS TEIMOSOS:
        // Retira a reserva de espaço gigante de fábrica (500x400) do JavaFX!
        activeChart.setMinSize(0, 0);
        activeChart.setPrefSize(10, 10); // Aceita ser minúsculo e deixa o CSS esticá-lo!
        activeChart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

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

    @Override
    public void onReactiveAttributeChange(String attrName, Object newValue) {
        super.onReactiveAttributeChange(attrName, newValue);

        // Se o programador XPL mudar a variável de dados reativamente
        if ("data".equalsIgnoreCase(attrName)) {

            boolean isStreaming = sourceNode.attributes.containsKey("streaming");

            javafx.application.Platform.runLater(() -> {

                // ⭐ 1. BLINDAGEM GLOBAL: Desliga a animação principal do Gráfico
                if (activeChart != null) {
                    activeChart.setAnimated(false);
                }

                // ⭐ 2. BLINDAGEM DOS EIXOS (A Cura para o Index Out of Bounds!)
                // O JavaFX esconde os eixos dentro do XYChart, temos de ir lá buscá-los.
                if (activeChart instanceof javafx.scene.chart.XYChart) {
                    javafx.scene.chart.XYChart<?, ?> xyChart = (javafx.scene.chart.XYChart<?, ?>) activeChart;
                    xyChart.getXAxis().setAnimated(false);
                    xyChart.getYAxis().setAnimated(false);

                    // Se for Streaming, desliza o eixo!
                    if (isStreaming) {
                        int maxWindow = 10;
                        try { maxWindow = Integer.parseInt(sourceNode.attributes.get("streaming").toString()); }
                        catch (Exception ignored) {}

                        applyStreamingData(newValue, maxWindow);
                        return; // Sai cedo para não re-desenhar tudo!
                    }
                }

                // ⭐ 3. GRÁFICOS NORMAIS (Ex: PieChart da RAM ou Scatter da Rede)
                // Como as animações agora estão totalmente desligadas, o JavaFX não
                // vai estourar quando a função abaixo fizer chart.getData().clear()
                populateData(newValue);
            });
        }

        // Se mudar o título reativamente
        if ("title".equalsIgnoreCase(attrName) && activeChart != null) {
            activeChart.setTitle(String.valueOf(newValue));
        }
    }

    // =========================================================================
    // ⭐ MOTOR FIFO (A ILUSÃO DE DESLIZAMENTO TEMPO REAL)
    // =========================================================================
    @SuppressWarnings("unchecked")
    protected void applyStreamingData(Object data, int maxWindow) {
        javafx.scene.chart.XYChart<String, Number> chart = (javafx.scene.chart.XYChart<String, Number>) activeChart;
        java.util.List<Map<String, Object>> seriesList = parseSeriesList(data);
        if (seriesList == null) return;

        for (Map<String, Object> seriesMap : seriesList) {
            String seriesName = toString(seriesMap.get("name"));

            // 1. Procura a série (linha) já existente no gráfico
            javafx.scene.chart.XYChart.Series<String, Number> targetSeries = null;
            for (javafx.scene.chart.XYChart.Series<String, Number> s : chart.getData()) {
                if (seriesName.equals(s.getName())) {
                    targetSeries = s;
                    break;
                }
            }

            // 2. Se a série não existir, cria-a pela primeira vez!
            if (targetSeries == null) {
                targetSeries = new javafx.scene.chart.XYChart.Series<>();
                targetSeries.setName(seriesName);
                chart.getData().add(targetSeries);
            }

            // 3. ADICIONA APENAS OS NOVOS PONTOS (O Delta)
            Object valuesObj = seriesMap.get("values");
            if (valuesObj instanceof java.util.List) {
                for (Object item : (java.util.List<?>) valuesObj) {
                    if (item instanceof Map) {
                        Map<String, Object> point = (Map<String, Object>) item;
                        String x = toString(point.get("x"));
                        double y = toDouble(point.get("y"));
                        targetSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(x, y));
                    }
                }
            }

            // 4. ⭐ A MAGIA DO TASK MANAGER: Remove os pontos mais velhos!
            // Isto empurra o eixo X e faz o gráfico deslizar visualmente
            while (targetSeries.getData().size() > maxWindow) {
                targetSeries.getData().remove(0);
            }
        }
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