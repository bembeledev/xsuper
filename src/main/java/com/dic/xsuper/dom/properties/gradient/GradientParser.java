package com.dic.xsuper.dom.properties.gradient;



import com.dic.xsuper.dom.properties.cssunit.CssParser;
import com.dic.xsuper.dom.properties.cssunit.CssUnit;
import com.dic.xsuper.dom.properties.cssunit.CssValue;
import com.dic.xsuper.dom.properties.style.XplColor;

import java.util.*;

public class GradientParser {

    /**
     * Parseia uma string CSS e devolve uma lista de Gradients.
     * Suporta: linear-gradient, radial-gradient, conic-gradient,
     * repeating-linear-gradient, repeating-radial-gradient e custom-gradient.
     */
    public static List<Gradient> parseGradients(String css) {
        List<Gradient> result = new ArrayList<>();
        if (css == null || css.isEmpty()) return result;

        // Divide por vírgula, mas respeita parênteses
        String[] parts = splitByComma(css);
        for (String part : parts) {
            Gradient g = parseSingle(part.trim());
            if (g != null) result.add(g);
        }
        return result;
    }

    private static Gradient parseSingle(String css) {
        if (css.startsWith("linear-gradient(")) {
            String inner = css.substring("linear-gradient(".length(), css.length() - 1);
            return parseLinearGradient(inner, false);
        }
        if (css.startsWith("repeating-linear-gradient(")) {
            String inner = css.substring("repeating-linear-gradient(".length(), css.length() - 1);
            return parseLinearGradient(inner, true);
        }
        if (css.startsWith("radial-gradient(")) {
            String inner = css.substring("radial-gradient(".length(), css.length() - 1);
            return parseRadialGradient(inner, false);
        }
        if (css.startsWith("repeating-radial-gradient(")) {
            String inner = css.substring("repeating-radial-gradient(".length(), css.length() - 1);
            return parseRadialGradient(inner, true);
        }
        if (css.startsWith("conic-gradient(")) {
            String inner = css.substring("conic-gradient(".length(), css.length() - 1);
            return parseConicGradient(inner);
        }
        if (css.startsWith("custom-gradient(")) {
            String inner = css.substring("custom-gradient(".length(), css.length() - 1);
            return parseCustomGradient(inner);
        }
        return null;
    }

    // ─── Linear ──────────────────────────────────────────────────────────

    private static Gradient parseLinearGradient(String inner, boolean repeating) {
        List<String> parts = splitGradientArgs(inner);
        if (parts.isEmpty()) return null;

        int offset = 0;
        float angle = 0;
        String direction = null;

        // Verifica se primeiro token é direção ou ângulo
        String first = parts.getFirst().trim();
        if (first.startsWith("to ")) {
            direction = first;
            offset = 1;
        } else if (first.endsWith("deg")) {
            angle = Float.parseFloat(first.replace("deg", "").trim());
            offset = 1;
        }

        List<GradientStop> stops = parseStops(parts.subList(offset, parts.size()));
        if (stops.size() < 2) return null;

        Gradient.Builder builder = Gradient.builder()
                .type(repeating ? GradientType.REPEATING_LINEAR : GradientType.LINEAR)
                .stops(stops)
                .angle(angle)
                .direction(direction);

        return builder.build();
    }

    // ─── Radial ──────────────────────────────────────────────────────────

    private static Gradient parseRadialGradient(String inner, boolean repeating) {
        List<String> parts = splitGradientArgs(inner);
        if (parts.isEmpty()) return null;

        int offset = 0;
        float cx = 50, cy = 50;
        float rx = 50, ry = 50;
        boolean isCircle = false;

        // Verifica se o primeiro token contém shape/size/position
        String first = parts.get(0).trim().toLowerCase();
        if (first.startsWith("circle") || first.startsWith("ellipse") || first.startsWith("at")) {
            // Pode ser "circle at 30% 40%" ou "ellipse at 30% 40%"
            String[] tokens = first.split("\\s+");
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].equals("circle")) {
                    isCircle = true;
                } else if (tokens[i].equals("ellipse")) {
                    isCircle = false;
                } else if (tokens[i].equals("at") && i + 1 < tokens.length) {
                    // Extrai a posição
                    String[] pos = tokens[i+1].split("\\s+");
                    if (pos.length >= 2) {
                        cx = parsePercent(pos[0]);
                        cy = parsePercent(pos[1]);
                    }
                    break;
                }
            }
            offset = 1;
        }

        List<GradientStop> stops = parseStops(parts.subList(offset, parts.size()));
        if (stops.size() < 2) return null;

        Gradient.Builder builder = Gradient.builder()
                .type(repeating ? GradientType.REPEATING_RADIAL : GradientType.RADIAL)
                .stops(stops)
                .center(cx, cy)
                .radius(rx, ry);
        if (isCircle) builder.circle();
        else builder.ellipse();

        return builder.build();
    }

    // ─── Conic ───────────────────────────────────────────────────────────

    private static Gradient parseConicGradient(String inner) {
        // Suporte básico: from angle, at position
        // Ex: conic-gradient(from 0deg at 50% 50%, red, blue)
        List<String> parts = splitGradientArgs(inner);
        if (parts.isEmpty()) return null;

        float fromAngle = 0;
        float cx = 50, cy = 50;
        int offset = 0;

        for (String part : parts) {
            if (part.contains("from")) {
                String[] tokens = part.split("\\s+");
                for (int i = 0; i < tokens.length; i++) {
                    if (tokens[i].equals("from") && i+1 < tokens.length) {
                        fromAngle = Float.parseFloat(tokens[i+1].replace("deg", ""));
                        offset++;
                    }
                }
            } else if (part.contains("at")) {
                String[] tokens = part.split("\\s+");
                for (int i = 0; i < tokens.length; i++) {
                    if (tokens[i].equals("at") && i+1 < tokens.length) {
                        String[] pos = tokens[i+1].split("\\s+");
                        if (pos.length >= 2) {
                            cx = parsePercent(pos[0]);
                            cy = parsePercent(pos[1]);
                        }
                        offset++;
                    }
                }
            } else {
                break;
            }
        }

        List<GradientStop> stops = parseStops(parts.subList(offset, parts.size()));
        if (stops.size() < 2) return null;

        // Conic não tem suporte nativo no Skia; usaremos um builder com tipo CONIC
        return Gradient.builder()
                .type(GradientType.CONIC)
                .stops(stops)
                .center(cx, cy)
                .angle(fromAngle)
                .build();
    }

    // ─── Custom ──────────────────────────────────────────────────────────

    private static Gradient parseCustomGradient(String inner) {
        List<GradientStop> stops = new ArrayList<>();

        // Regex para capturar tudo o que está dentro das tuplas ( )
        // Ex: De "(0%, 0%, red, 0.9, 100, 150, circle)", extrai "0%, 0%, red, 0.9, 100, 150, circle"
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\(([^)]+)\\)").matcher(inner);

        while (m.find()) {
            String tupleContent = m.group(1);
            String[] parts = tupleContent.split(",");

            if (parts.length >= 3) {
                // O .trim() é obrigatório para limpar espaços antes e depois da vírgula
                float x = parsePercent(parts[0].trim());
                float y = parsePercent(parts[1].trim());
                int color = parseColor(parts[2].trim());

                float opacity = parts.length > 3 ? Float.parseFloat(parts[3].trim()) : 1.0f;
                float blur = parts.length > 4 ? Float.parseFloat(parts[4].trim()) : 0f;
                float radius = parts.length > 5 ? Float.parseFloat(parts[5].trim()) : 50f;
                String shapeStr = parts.length > 6 ? parts[6].trim() : "circle";

                GradientStop.Shape shape = parseShape(shapeStr);

                // Cria o stop (A posição linear de 0% é mockada aqui, a renderização lerá x e y customizados)
                stops.add(new GradientStop(color, CssValue.percent(0), opacity, blur, radius, shape));
            }
        }

        if (stops.size() < 2) return null;

        return Gradient.builder()
                .type(GradientType.CUSTOM)
                .stops(stops)
                .center(50, 50)
                .radius(50, 50)
                .build();
    }

    // ─── Utilitários de parsing ─────────────────────────────────────────

    private static List<String> splitGradientArgs(String inner) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (char c : inner.toCharArray()) {
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) {
                parts.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        if (!current.isEmpty()) parts.add(current.toString().trim());
        return parts;
    }

    private static List<GradientStop> parseStops(List<String> stopStrings) {
        List<GradientStop> stops = new ArrayList<>();
        for (String s : stopStrings) {
            String[] parts = s.trim().split("\\s+");
            if (parts.length == 1) {
                // só cor → posição 0%
                int color = parseColor(parts[0]);
                stops.add(new GradientStop(color, CssValue.percent(0)));
            } else if (parts.length >= 2) {
                int color = parseColor(parts[0]);
                CssValue pos = parseCssValue(parts[1]);
                stops.add(new GradientStop(color, pos));
            }
        }
        return stops;
    }

    private static int parseColor(String colorStr) {
        if (colorStr.startsWith("rgba") || colorStr.startsWith("rgb")) {
            // parse rgb/rgba
            // Implementação simplificada
            return 0xFF000000;
        }
        XplColor color = XplColor.fromWebName(colorStr);
        if (color != null) return color.toArgb();
        // fallback para hex
        return parseHexColor(colorStr, 0xFF000000);
    }

    private static int parseHexColor(String hex, int defaultColor) {
        try {
            String clean = hex.replace("#", "").trim();
            if (clean.length() == 6) clean = "FF" + clean;
            return (int) Long.parseLong(clean, 16);
        } catch (NumberFormatException e) { return defaultColor; }
    }

    private static float parsePercent(String str) {
        str = str.trim();
        if (str.endsWith("%")) return Float.parseFloat(str.replace("%", ""));
        return Float.parseFloat(str);
    }

    private static CssValue parseCssValue(String str) {
        if (str.endsWith("%")) {
            return new CssValue(parsePercent(str), CssUnit.PERCENT);
        }
        return CssParser.parseValue(str);
    }

    private static GradientStop.Shape parseShape(String str) {
        str = str.toLowerCase().trim();
        return switch (str) {
            case "circle" -> GradientStop.Shape.CIRCLE;
            case "ellipse" -> GradientStop.Shape.ELLIPSE;
            case "polygon" -> GradientStop.Shape.POLYGON;
            default -> GradientStop.Shape.CIRCLE;
        };
    }

    private static String[] splitByComma(String input) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) {
                parts.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        if (!current.isEmpty()) parts.add(current.toString().trim());
        return parts.toArray(new String[0]);
    }
}