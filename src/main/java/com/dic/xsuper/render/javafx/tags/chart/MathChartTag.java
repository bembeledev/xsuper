package com.dic.xsuper.render.javafx.tags.chart;

import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

public class MathChartTag extends ChartBase {

    // Arrays para suportar múltiplos gráficos no mesmo eixo
    private String[] expressions = {"x"};
    private double minX = -10.0;
    private double maxX = 10.0;
    private double step = 0.1;
    // Paleta padrão caso o utilizador não defina cores suficientes
    private String[] colors = {"#3b82f6", "#ef4444", "#10b981", "#f59e0b", "#8b5cf6", "#ec4899"};

    public MathChartTag(XplNode node) {
        super(node);
    }

    @Override
    protected Chart createChart() {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel(xAxisLabel);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yAxisLabel);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setCreateSymbols(false); // Esconde as bolinhas para parecer uma linha contínua pura
        return chart;
    }

    @Override
    protected void applySpecificAttributes() {
        // ⭐ SUPORTE MÚLTIPLO: Aceita vírgula ou ponto-e-vírgula como separador
        if (sourceNode.attributes.containsKey("expressions")) {
            expressions = sourceNode.attributes.get("expressions").toString().split("[,;]");
        } else if (sourceNode.attributes.containsKey("expression")) {
            expressions = new String[]{ sourceNode.attributes.get("expression").toString() };
        }

        // ⭐ CORES MÚLTIPLAS
        if (sourceNode.attributes.containsKey("colors")) {
            colors = sourceNode.attributes.get("colors").toString().split("[,;]");
        } else if (sourceNode.attributes.containsKey("color")) {
            colors = new String[]{ sourceNode.attributes.get("color").toString() };
        }

        if (sourceNode.attributes.containsKey("min")) {
            minX = toDouble(sourceNode.attributes.get("min"));
        }
        if (sourceNode.attributes.containsKey("max")) {
            maxX = toDouble(sourceNode.attributes.get("max"));
        }
        if (sourceNode.attributes.containsKey("step")) {
            step = toDouble(sourceNode.attributes.get("step"));
        }

        plotFunctions();
    }

    private void plotFunctions() {
        if (!(activeChart instanceof LineChart)) return;
        LineChart<Number, Number> chart = (LineChart<Number, Number>) activeChart;
        chart.getData().clear();

        // Fazemos o loop por cada expressão que o utilizador definiu!
        for (int i = 0; i < expressions.length; i++) {
            String expr = expressions[i].trim().toLowerCase();
            if (expr.isEmpty()) continue;

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName("f(x) = " + expr);

            // Calcula os pontos no S.C.O
            for (double x = minX; x <= maxX; x += step) {
                try {
                    double y = evalMath(expr, x);
                    if (!Double.isInfinite(y) && !Double.isNaN(y)) {
                        series.getData().add(new XYChart.Data<>(x, y));
                    }
                } catch (Exception e) {
                    System.err.println("[MathChartTag] Erro ao avaliar expressão '" + expr + "': " + e.getMessage());
                    break;
                }
            }

            chart.getData().add(series);

            // ⭐ ASSINALAR CORES DISTINTAS PARA CADA LINHA
            // Se houver mais equações do que cores fornecidas, ele recomeça do início da paleta (usando o %)
            final int colorIndex = i;
            javafx.application.Platform.runLater(() -> {
                if (series.getNode() != null) {
                    String color = colors[colorIndex % colors.length].trim();
                    series.getNode().setStyle("-fx-stroke: " + color + "; -fx-stroke-width: 2px; -fx-stroke-line-join: round;");
                }
            });
        }
    }

    // =========================================================================
    // 🧠 MOTOR DE AVALIAÇÃO MATEMÁTICA (Recursive Descent Parser)
    // Suporta: x, +, -, *, /, ^, sqrt, sin, cos, tan, abs
    // =========================================================================
    private double evalMath(final String str, final double xValue) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Caractere inesperado: " + (char)ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); // Adição
                    else if (eat('-')) x -= parseTerm(); // Subtração
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor(); // Multiplicação
                    else if (eat('/')) x /= parseFactor(); // Divisão
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor(); // Mais unário
                if (eat('-')) return -parseFactor(); // Menos unário

                double x;
                int startPos = this.pos;
                if (eat('(')) { // Parênteses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // Números
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') { // Funções e Variáveis
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    if (func.equals("x")) {
                        x = xValue;
                    } else {
                        x = parseFactor();
                        switch (func) {
                            case "sqrt" -> x = Math.sqrt(x);
                            case "sin" -> x = Math.sin(x);
                            case "cos" -> x = Math.cos(x);
                            case "tan" -> x = Math.tan(x);
                            case "abs" -> x = Math.abs(x);
                            default -> throw new RuntimeException("Função desconhecida: " + func);
                        }
                    }
                } else {
                    throw new RuntimeException("Caractere inesperado: " + (char)ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // Potência
                return x;
            }
        }.parse();
    }
}