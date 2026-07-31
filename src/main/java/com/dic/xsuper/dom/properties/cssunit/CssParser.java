package com.dic.xsuper.dom.properties.cssunit;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser para valores CSS, suportando números com unidades, auto, etc.
 */
public class CssParser {

    private static final Pattern VALUE_PATTERN = Pattern.compile(
            "^([+-]?\\d*\\.?\\d+)(px|pt|pc|in|cm|mm|q|em|rem|ex|ch|vw|vh|vmin|vmax|%|fr)?$"
    );

    /**
     * Converte uma string CSS para CssValue.
     * Exemplos:
     *   "10px"   → CssValue(10, PX)
     *   "20%"    → CssValue(20, PERCENT)
     *   "auto"   → CssValue(0, AUTO)
     *   "1.5"    → CssValue(1.5, NONE)
     *   "12pt"   → CssValue(12, PT)
     */
    public static CssValue parseValue(String str) {
        if (str == null) return CssValue.zero();
        String trimmed = str.trim().toLowerCase();
        if (trimmed.isEmpty()) return CssValue.zero();

        // auto
        if (trimmed.equals("auto")) return CssValue.auto();

        // Número sem unidade (ex: "1.5")
        if (trimmed.matches("^[+-]?\\d*\\.?\\d+$")) {
            try {
                double val = Double.parseDouble(trimmed);
                return new CssValue(val, CssUnit.NONE);
            } catch (NumberFormatException e) {
                return CssValue.zero();
            }
        }

        // Número com unidade
        Matcher m = VALUE_PATTERN.matcher(trimmed);
        if (m.matches()) {
            try {
                double val = Double.parseDouble(m.group(1));
                String unitStr = m.group(2);
                CssUnit unit = parseUnit(unitStr);
                return new CssValue(val, unit);
            } catch (NumberFormatException e) {
                return CssValue.zero();
            }
        }

        // Tentar parsear expressões simples tipo "calc(100% - 20px)" - FUTURO
        // Por agora, fallback para zero
        return CssValue.zero();
    }

    /**
     * Parseia uma lista de valores separados por espaços (ex: "10px 20px 30px 40px")
     * Útil para margin e padding.
     */
    public static List<CssValue> parseValueList(String str) {
        List<CssValue> result = new ArrayList<>();
        if (str == null || str.trim().isEmpty()) return result;

        // Divide por espaços, mas respeita valores com hífen (ex: "10px -20px")
        // Simples: split por espaços
        String[] parts = str.trim().split("\\s+");
        for (String part : parts) {
            result.add(parseValue(part));
        }
        return result;
    }

    private static CssUnit parseUnit(String unitStr) {
        if (unitStr == null) return CssUnit.NONE;
        return switch (unitStr) {
            case "px" -> CssUnit.PX;
            case "pt" -> CssUnit.PT;
            case "pc" -> CssUnit.PC;
            case "in" -> CssUnit.IN;
            case "cm" -> CssUnit.CM;
            case "mm" -> CssUnit.MM;
            case "q" -> CssUnit.Q;
            case "em" -> CssUnit.EM;
            case "rem" -> CssUnit.REM;
            case "ex" -> CssUnit.EX;
            case "ch" -> CssUnit.CH;
            case "vw" -> CssUnit.VW;
            case "vh" -> CssUnit.VH;
            case "vmin" -> CssUnit.VMIN;
            case "vmax" -> CssUnit.VMAX;
            case "%" -> CssUnit.PERCENT;
            case "fr" -> CssUnit.FR;
            default -> CssUnit.NONE;
        };
    }
}