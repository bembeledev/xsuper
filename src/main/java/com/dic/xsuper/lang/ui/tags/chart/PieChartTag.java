package com.dic.xsuper.lang.ui.tags.chart;

import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.scene.chart.PieChart;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public class PieChartTag extends ChartBase {

    private double startAngle = 0.0;
    private boolean clockwise = true;
    private boolean donut = false;
    private double innerRadius = 0.5;
    private int labelLineLength = 20;
    private String labelLineColor = "#000000";
    private boolean labelsVisible = true;
    private String customColors = null;

    public PieChartTag(XplNode node) {
        super(node);
    }

    @Override
    protected javafx.scene.chart.Chart createChart() {
        return new PieChart();
    }

    @Override
    protected void applySpecificAttributes() {
        if (sourceNode.attributes.containsKey("start-angle")) {
            try {
                startAngle = Double.parseDouble(sourceNode.attributes.get("start-angle").toString());
            } catch (NumberFormatException ignored) {}
        }
        if (sourceNode.attributes.containsKey("clockwise")) {
            clockwise = Boolean.parseBoolean(sourceNode.attributes.get("clockwise").toString());
        }
        if (sourceNode.attributes.containsKey("donut")) {
            donut = Boolean.parseBoolean(sourceNode.attributes.get("donut").toString());
        }
        if (sourceNode.attributes.containsKey("inner-radius")) {
            try {
                innerRadius = Double.parseDouble(sourceNode.attributes.get("inner-radius").toString());
                if (innerRadius < 0) innerRadius = 0;
                if (innerRadius > 1) innerRadius = 1;
            } catch (NumberFormatException ignored) {}
        }
        if (sourceNode.attributes.containsKey("label-line-length")) {
            try {
                labelLineLength = Integer.parseInt(sourceNode.attributes.get("label-line-length").toString());
            } catch (NumberFormatException ignored) {}
        }
        if (sourceNode.attributes.containsKey("label-line-color")) {
            labelLineColor = sourceNode.attributes.get("label-line-color").toString();
        }
        if (sourceNode.attributes.containsKey("labels")) {
            labelsVisible = Boolean.parseBoolean(sourceNode.attributes.get("labels").toString());
        }
        if (sourceNode.attributes.containsKey("colors")) {
            customColors = sourceNode.attributes.get("colors").toString();
        }
    }

    @Override
    protected void populateData(Object data) {
        if (!(activeChart instanceof PieChart)) return;
        PieChart chart = (PieChart) activeChart;
        chart.getData().clear();

        // Aplicar propriedades (usando métodos existentes)
        chart.setStartAngle(startAngle);
        chart.setClockwise(clockwise);
        chart.setLabelsVisible(labelsVisible);
        chart.setLabelLineLength(labelLineLength);

        // ⭐ A cor da linha do rótulo é aplicada via CSS, não via método setLabelLineColor
        applyLabelLineColor(chart);

        // Donut – via CSS
        if (donut) {
            double radius = innerRadius * 100;
            chart.setStyle("-fx-pie-radius: 100%; -fx-pie-inner-radius: " + radius + "%;");
        }

        // Popular dados
        if (data instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) data;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String label = toString(entry.getKey());
                double value = toDouble(entry.getValue());
                chart.getData().add(new PieChart.Data(label, value));
            }
        } else if (data instanceof List) {
            List<?> list = (List<?>) data;
            for (Object item : list) {
                if (item instanceof Map) {
                    Map<String, Object> point = (Map<String, Object>) item;
                    String label = toString(point.getOrDefault("label", ""));
                    double value = toDouble(point.getOrDefault("value", 0.0));
                    chart.getData().add(new PieChart.Data(label, value));
                }
            }
        }

        // Cores personalizadas
        if (customColors != null && !customColors.isEmpty()) {
            applyCustomColors(chart);
        }
    }

    /**
     * Aplica cor à linha do rótulo via CSS.
     */
    private void applyLabelLineColor(PieChart chart) {
        if (labelLineColor != null && !labelLineColor.isEmpty()) {
            String colorHex = labelLineColor;
            if (!colorHex.startsWith("#")) {
                // Tenta converter nome para hex (simplificado)
                try {
                    Color c = Color.web(colorHex);
                    colorHex = String.format("#%02X%02X%02X",
                            (int) (c.getRed() * 255),
                            (int) (c.getGreen() * 255),
                            (int) (c.getBlue() * 255));
                } catch (Exception ignored) {}
            }
            // Aplica via CSS – o seletor .chart-pie-label-line está no labelLinePath
            chart.setStyle(chart.getStyle() + " .chart-pie-label-line { -fx-stroke: " + colorHex + "; }");
        }
    }

    /**
     * Aplica cores personalizadas aos segmentos.
     */
    private void applyCustomColors(PieChart chart) {
        String[] colorStrings = customColors.split(",");
        int index = 0;
        for (PieChart.Data data : chart.getData()) {
            if (index < colorStrings.length) {
                String colorStr = colorStrings[index].trim();
                try {
                    // Aplica a cor via estilo inline no nó
                    data.getNode().setStyle("-fx-pie-color: " + colorStr + ";");
                } catch (Exception ignored) {}
            }
            index++;
        }
    }

    @Override
    protected void addChildren() {
        // Gráficos não têm filhos DOM
    }
}