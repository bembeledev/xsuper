package com.dic.xsuper.lang.ui.properties.style;


import java.util.HashMap;
import java.util.Map;

/**
 * Estilos padrão para elementos HTML (User Agent Stylesheet).
 */
public class UserAgentStyles {

    private static final Map<String, Map<String, String>> defaultStyles = new HashMap<>();

    static {
        // ─── Elementos de bloco ──────────────────────────────────────────
        defaultStyles.put("div", Map.of(
                "display", "block"
        ));
        defaultStyles.put("p", Map.of(
                "display", "block",
                "margin-top", "1em",
                "margin-bottom", "1em"
        ));
        defaultStyles.put("h1", Map.of(
                "display", "block",
                "font-size", "2em",
                "font-weight", "bold",
                "margin-top", "0.67em",
                "margin-bottom", "0.67em"
        ));
        defaultStyles.put("h2", Map.of(
                "display", "block",
                "font-size", "1.5em",
                "font-weight", "bold",
                "margin-top", "0.83em",
                "margin-bottom", "0.83em"
        ));
        defaultStyles.put("h3", Map.of(
                "display", "block",
                "font-size", "1.17em",
                "font-weight", "bold",
                "margin-top", "1em",
                "margin-bottom", "1em"
        ));
        defaultStyles.put("h4", Map.of(
                "display", "block",
                "font-size", "1em",
                "font-weight", "bold",
                "margin-top", "1.33em",
                "margin-bottom", "1.33em"
        ));
        defaultStyles.put("h5", Map.of(
                "display", "block",
                "font-size", "0.83em",
                "font-weight", "bold",
                "margin-top", "1.67em",
                "margin-bottom", "1.67em"
        ));
        defaultStyles.put("h6", Map.of(
                "display", "block",
                "font-size", "0.67em",
                "font-weight", "bold",
                "margin-top", "2.33em",
                "margin-bottom", "2.33em"
        ));
        defaultStyles.put("section", Map.of(
                "display", "block"
        ));
        defaultStyles.put("article", Map.of(
                "display", "block"
        ));
        defaultStyles.put("nav", Map.of(
                "display", "block"
        ));
        defaultStyles.put("header", Map.of(
                "display", "block"
        ));
        defaultStyles.put("footer", Map.of(
                "display", "block"
        ));
        defaultStyles.put("main", Map.of(
                "display", "block"
        ));
        defaultStyles.put("aside", Map.of(
                "display", "block"
        ));
        defaultStyles.put("figure", Map.of(
                "display", "block",
                "margin-top", "1em",
                "margin-bottom", "1em"
        ));
        defaultStyles.put("figcaption", Map.of(
                "display", "block"
        ));
        defaultStyles.put("blockquote", Map.of(
                "display", "block",
                "margin-top", "1em",
                "margin-bottom", "1em",
                "margin-left", "40px",
                "margin-right", "40px"
        ));
        defaultStyles.put("ul", Map.of(
                "display", "block",
                "margin-top", "1em",
                "margin-bottom", "1em",
                "padding-left", "40px",
                "list-style-type", "disc"
        ));
        defaultStyles.put("ol", Map.of(
                "display", "block",
                "margin-top", "1em",
                "margin-bottom", "1em",
                "padding-left", "40px",
                "list-style-type", "decimal"
        ));
        defaultStyles.put("li", Map.of(
                "display", "list-item"
        ));
        defaultStyles.put("table", Map.of(
                "display", "table",
                "border-collapse", "separate",
                "border-spacing", "2px"
        ));
        defaultStyles.put("tr", Map.of(
                "display", "table-row"
        ));
        defaultStyles.put("td", Map.of(
                "display", "table-cell",
                "padding", "1px"
        ));
        defaultStyles.put("th", Map.of(
                "display", "table-cell",
                "padding", "1px",
                "font-weight", "bold",
                "text-align", "center"
        ));

        // ─── Elementos inline ────────────────────────────────────────────
        defaultStyles.put("span", Map.of(
                "display", "inline"
        ));
        defaultStyles.put("a", Map.of(
                "display", "inline",
                "color", "#0000EE",
                "text-decoration", "underline",
                "cursor", "pointer"
        ));
        defaultStyles.put("strong", Map.of(
                "display", "inline",
                "font-weight", "bold"
        ));
        defaultStyles.put("em", Map.of(
                "display", "inline",
                "font-style", "italic"
        ));
        defaultStyles.put("b", Map.of(
                "display", "inline",
                "font-weight", "bold"
        ));
        defaultStyles.put("i", Map.of(
                "display", "inline",
                "font-style", "italic"
        ));
        defaultStyles.put("u", Map.of(
                "display", "inline",
                "text-decoration", "underline"
        ));
        defaultStyles.put("s", Map.of(
                "display", "inline",
                "text-decoration", "line-through"
        ));
        defaultStyles.put("small", Map.of(
                "display", "inline",
                "font-size", "0.83em"
        ));
        defaultStyles.put("sub", Map.of(
                "display", "inline",
                "vertical-align", "sub",
                "font-size", "0.83em"
        ));
        defaultStyles.put("sup", Map.of(
                "display", "inline",
                "vertical-align", "super",
                "font-size", "0.83em"
        ));
        defaultStyles.put("code", Map.of(
                "display", "inline",
                "font-family", "monospace"
        ));
        defaultStyles.put("pre", Map.of(
                "display", "block",
                "font-family", "monospace",
                "white-space", "pre",
                "margin-top", "1em",
                "margin-bottom", "1em"
        ));

        // ─── Elementos de formulário ──────────────────────────────────────
        defaultStyles.put("button", Map.of(
                "display", "inline-block",
                "padding", "2px 6px",
                "border", "2px outset #CCCCCC",
                "background-color", "#F0F0F0",
                "font-size", "13.33px",
                "font-family", "Arial",
                "cursor", "pointer"
        ));
        defaultStyles.put("input", Map.of(
                "display", "inline-block",
                "padding", "1px 2px",
                "border", "2px inset #CCCCCC",
                "background-color", "#FFFFFF",
                "font-size", "13.33px",
                "font-family", "Arial"
        ));
        defaultStyles.put("textarea", Map.of(
                "display", "inline-block",
                "padding", "2px",
                "border", "2px inset #CCCCCC",
                "background-color", "#FFFFFF",
                "font-size", "13.33px",
                "font-family", "monospace",
                "resize", "both"
        ));
        defaultStyles.put("select", Map.of(
                "display", "inline-block",
                "padding", "1px 2px",
                "border", "2px inset #CCCCCC",
                "background-color", "#FFFFFF",
                "font-size", "13.33px",
                "font-family", "Arial"
        ));
        defaultStyles.put("option", Map.of(
                "display", "block",
                "padding", "1px 2px"
        ));
        defaultStyles.put("label", Map.of(
                "display", "inline-block",
                "cursor", "default"
        ));

        // ─── Elementos de mídia ──────────────────────────────────────────
        defaultStyles.put("img", Map.of(
                "display", "inline-block"
        ));
        defaultStyles.put("video", Map.of(
                "display", "inline-block"
        ));
        defaultStyles.put("audio", Map.of(
                "display", "inline-block"
        ));
        defaultStyles.put("canvas", Map.of(
                "display", "inline-block"
        ));
        defaultStyles.put("svg", Map.of(
                "display", "inline-block"
        ));

        // ─── Elementos de agrupamento ────────────────────────────────────
        defaultStyles.put("hr", Map.of(
                "display", "block",
                "border", "1px inset #CCCCCC",
                "margin-top", "0.5em",
                "margin-bottom", "0.5em"
        ));
        defaultStyles.put("br", Map.of(
                "display", "inline"
        ));
        defaultStyles.put("wbr", Map.of(
                "display", "inline"
        ));

        // ─── Elementos de secção ─────────────────────────────────────────
        defaultStyles.put("details", Map.of(
                "display", "block"
        ));
        defaultStyles.put("summary", Map.of(
                "display", "block",
                "cursor", "pointer"
        ));
        defaultStyles.put("dialog", Map.of(
                "display", "none",
                "position", "absolute",
                "top", "50%",
                "left", "50%",
                "transform", "translate(-50%, -50%)"
        ));
    }

    /**
     * Obtém os estilos padrão para uma tag HTML.
     * @param tagName Nome da tag (ex: "div", "span")
     * @return Mapa de propriedades CSS padrão (pode ser vazio se a tag não tiver estilos definidos).
     */
    public static Map<String, String> getDefaultStyles(String tagName) {
        return defaultStyles.getOrDefault(tagName.toLowerCase(), Map.of());
    }


}