package com.dic.xsuper.render.javafx.layout.grid;

import java.util.ArrayList;
import java.util.List;

public class GridTemplateParser {
    private final String input;
    private int pos;
    private final int len;

    public GridTemplateParser(String input) {
        this.input = input == null ? "" : input.trim();
        this.pos = 0;
        this.len = this.input.length();
    }

    public List<GridDimension> parse() {
        List<GridDimension> result = new ArrayList<>();
        while (pos < len) {
            skipWhitespace();
            if (pos >= len) break;
            GridDimension dim = parseDimensionUnit();
            if (dim != null) result.add(dim);
            skipWhitespace();
            if (pos < len && input.charAt(pos) == ' ') {
                pos++;
                continue;
            }
            // Se houver algo que não seja espaço, pode ser separador (como espaço) e continuamos
        }
        return result;
    }

    private GridDimension parseDimensionUnit() {
        skipWhitespace();
        if (pos >= len) return null;

        // 1. Palavras-chave: auto, minmax, repeat, fit-content
        if (startsWith("auto")) {
            pos += 4;
            return AutoDimension.INSTANCE;
        }

        if (startsWith("minmax(")) {
            pos += 7; // "minmax("
            GridDimension min = parseDimensionUnit();
            skipWhitespace();
            if (pos < len && input.charAt(pos) == ',') pos++;
            skipWhitespace();
            GridDimension max = parseDimensionUnit();
            skipWhitespace();
            if (pos < len && input.charAt(pos) == ')') pos++;
            return new MinMaxDimension(min, max);
        }

        if (startsWith("fit-content(")) {
            pos += 11; // "fit-content("
            GridDimension value = parseDimensionUnit();
            skipWhitespace();
            if (pos < len && input.charAt(pos) == ')') pos++;
            return new FitContentDimension(value);
        }

        if (startsWith("repeat(")) {
            pos += 7; // "repeat("
            skipWhitespace();

            // Deteta auto-fit / auto-fill ou número
            boolean autoFit = false;
            boolean autoFill = false;
            int count = 1;

            if (startsWith("auto-fit")) {
                pos += 8;
                autoFit = true;
            } else if (startsWith("auto-fill")) {
                pos += 9;
                autoFill = true;
            } else {
                count = readNumber();
            }

            skipWhitespace();
            if (pos < len && input.charAt(pos) == ',') pos++;
            skipWhitespace();

            // Ler as dimensões dentro do repeat até ')'
            List<GridDimension> dims = new ArrayList<>();
            while (pos < len && input.charAt(pos) != ')') {
                skipWhitespace();
                GridDimension d = parseDimensionUnit();
                if (d != null) dims.add(d);
                skipWhitespace();
                if (pos < len && input.charAt(pos) == ',') pos++;
            }
            if (pos < len && input.charAt(pos) == ')') pos++;

            if (autoFit || autoFill) {
                return new RepeatDimension(autoFit, dims);
            } else {
                return new RepeatDimension(count, dims);
            }
        }

        // 2. Número com unidade (px, %, fr, em, etc.)
        return parseNumberUnit();
    }

    private GridDimension parseNumberUnit() {
        int start = pos;
        while (pos < len && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.' || input.charAt(pos) == '-')) {
            pos++;
        }
        if (pos == start) return null;
        String numStr = input.substring(start, pos);
        double num;
        try {
            num = Double.parseDouble(numStr);
        } catch (NumberFormatException e) {
            return null;
        }

        // Ler unidade (px, %, fr, em, etc.)
        int unitStart = pos;
        while (pos < len && Character.isLetter(input.charAt(pos))) pos++;
        String unit = pos > unitStart ? input.substring(unitStart, pos) : "";

        return switch (unit) {
            case "px" -> new FixedDimension(num);
            case "%", "vh" -> new PercentDimension(num);
            case "fr" -> new FrDimension(num);
            case "em" -> // converter para px: usar contexto, mas como não temos, tratamos como fixed aproximado
                    new FixedDimension(num * 16); // estimativa
            case "rem" -> new FixedDimension(num * 16);
            case "vw" -> new PercentDimension(num); // tratamento especial depois
            default -> new FixedDimension(num); // fallback
        };
    }

    private int readNumber() {
        int start = pos;
        while (pos < len && Character.isDigit(input.charAt(pos))) pos++;
        if (pos == start) return 1;
        return Integer.parseInt(input.substring(start, pos));
    }

    private boolean startsWith(String prefix) {
        if (pos + prefix.length() > len) return false;
        return input.substring(pos, pos + prefix.length()).equalsIgnoreCase(prefix);
    }

    private void skipWhitespace() {
        while (pos < len && Character.isWhitespace(input.charAt(pos))) pos++;
    }

    public static List<GridDimension> parse(String input) {
        return new GridTemplateParser(input).parse();
    }
}