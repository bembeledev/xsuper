package com.dic.xsuper.lang.ui.properties.style;

import java.util.HashMap;
import java.util.Map;

/**
 * Paleta de cores do motor Xplorer.
 * Mapeia nomes CSS, Tailwind e cores personalizadas para valores hex e ARGB.
 * Baseado na paleta definida no motor Rust.
 */
public enum XplColor {

    // ─── CORES BÁSICAS ──────────────────────────────────────────────
    BLACK("black", "#000000"),
    WHITE("white", "#ffffff"),
    RED("red", "#ef4444"),
    GREEN("green", "#22c55e"),
    BLUE("blue", "#3b82f6"),
    YELLOW("yellow", "#eab308"),
    CYAN("cyan", "#06b6d4"),
    MAGENTA("magenta", "#d946ef"),
    GRAY("gray", "#71717a"),
    GREY("grey", "#71717a"),
    ORANGE("orange", "#f97316"),
    PURPLE("purple", "#a855f7"),
    PINK("pink", "#ec4899"),
    LIME("lime", "#84cc16"),
    TEAL("teal", "#14b8a6"),
    INDIGO("indigo", "#6366f1"),
    VIOLET("violet", "#8b5cf6"),
    FUCHSIA("fuchsia", "#d946ef"),
    ROSE("rose", "#f43f5e"),
    SKY("sky", "#0ea5e9"),
    EMERALD("emerald", "#10b981"),
    AMBER("amber", "#f59e0b"),
    BROWN("brown", "#8b5a2b"),
    CORAL("coral", "#ff7f50"),
    CRIMSON("crimson", "#dc143c"),
    GOLD("gold", "#ffd700"),
    SILVER("silver", "#c0c0c0"),
    BRONZE("bronze", "#cd7f32"),
    NAVY("navy", "#000080"),
    MAROON("maroon", "#800000"),
    OLIVE("olive", "#808000"),
    PLUM("plum", "#dda0dd"),
    ORCHID("orchid", "#da70d6"),
    CHOCOLATE("chocolate", "#d2691e"),
    SALMON("salmon", "#fa8072"),
    TOMATO("tomato", "#ff6347"),
    TURQUOISE("turquoise", "#40e0d0"),
    LAVENDER("lavender", "#e6e6fa"),
    BEIGE("beige", "#f5f5dc"),
    IVORY("ivory", "#fffff0"),
    KHAKI("khaki", "#f0e68c"),

    // ─── CORES CLARAS (Light) ──────────────────────────────────────
    LIGHT_RED("light_red", "#fca5a5"),
    LIGHT_GREEN("light_green", "#86efac"),
    LIGHT_BLUE("light_blue", "#93c5fd"),
    LIGHT_YELLOW("light_yellow", "#fef08a"),
    LIGHT_CYAN("light_cyan", "#67e8f9"),
    LIGHT_MAGENTA("light_magenta", "#f0abfc"),
    LIGHT_GRAY("light_gray", "#d4d4d8"),
    LIGHT_GREY("light_grey", "#d4d4d8"),
    LIGHT_ORANGE("light_orange", "#fdba74"),
    LIGHT_PURPLE("light_purple", "#d8b4fe"),
    LIGHT_PINK("light_pink", "#f9a8d4"),
    LIGHT_LIME("light_lime", "#bef264"),
    LIGHT_TEAL("light_teal", "#5eead4"),
    LIGHT_INDIGO("light_indigo", "#a5b4fc"),

    // ─── CORES ESCURAS (Dark) ──────────────────────────────────────
    DARK_RED("dark_red", "#b91c1c"),
    DARK_GREEN("dark_green", "#15803d"),
    DARK_BLUE("dark_blue", "#1d4ed8"),
    DARK_YELLOW("dark_yellow", "#a16207"),
    DARK_CYAN("dark_cyan", "#0e7490"),
    DARK_MAGENTA("dark_magenta", "#a21caf"),
    DARK_GRAY("dark_gray", "#3f3f46"),
    DARK_GREY("dark_grey", "#3f3f46"),
    DARK_ORANGE("dark_orange", "#c2410c"),
    DARK_PURPLE("dark_purple", "#6b21a5"),
    DARK_PINK("dark_pink", "#be185d"),
    DARK_LIME("dark_lime", "#4d7c0f"),
    DARK_TEAL("dark_teal", "#0f766e"),
    DARK_INDIGO("dark_indigo", "#3730a3"),

    // ─── BACKGROUND CORES (para console, mas útil para UI) ────────
    BG_BLACK("bg_black", "#000000"),
    BG_WHITE("bg_white", "#ffffff"),
    BG_RED("bg_red", "#ef4444"),
    BG_GREEN("bg_green", "#22c55e"),
    BG_BLUE("bg_blue", "#3b82f6"),
    BG_YELLOW("bg_yellow", "#eab308"),
    BG_CYAN("bg_cyan", "#06b6d4"),
    BG_MAGENTA("bg_magenta", "#d946ef"),
    BG_GRAY("bg_gray", "#71717a"),
    BG_GREY("bg_grey", "#71717a"),
    BG_ORANGE("bg_orange", "#f97316"),
    BG_PURPLE("bg_purple", "#a855f7"),
    BG_PINK("bg_pink", "#ec4899"),
    BG_LIME("bg_lime", "#84cc16"),
    BG_TEAL("bg_teal", "#14b8a6"),
    BG_INDIGO("bg_indigo", "#6366f1"),
    BG_DARK_RED("bg_dark_red", "#b91c1c"),
    BG_DARK_GREEN("bg_dark_green", "#15803d"),
    BG_DARK_BLUE("bg_dark_blue", "#1d4ed8"),
    BG_LIGHT_RED("bg_light_red", "#fca5a5"),
    BG_LIGHT_GREEN("bg_light_green", "#86efac"),
    BG_LIGHT_BLUE("bg_light_blue", "#93c5fd"),

    // ─── CORES CSS/WEB ─────────────────────────────────────────────
    ALICEBLUE("aliceblue", "#f0f8ff"),
    ANTIQUEWHITE("antiquewhite", "#faebd7"),
    AQUA("aqua", "#00ffff"),
    AQUAMARINE("aquamarine", "#7fffd4"),
    AZURE("azure", "#f0ffff"),
    BISQUE("bisque", "#ffe4c4"),
    BLANCHEDALMOND("blanchedalmond", "#ffebcd"),
    BLUEVIOLET("blueviolet", "#8a2be2"),
    BURLYWOOD("burlywood", "#deb887"),
    CADETBLUE("cadetblue", "#5f9ea0"),
    CHARTREUSE("chartreuse", "#7fff00"),
    CORNFLOWERBLUE("cornflowerblue", "#6495ed"),
    CORNSILK("cornsilk", "#fff8dc"),
    DARKBLUE("darkblue", "#00008b"),
    DARKCYAN("darkcyan", "#008b8b"),
    DARKGOLDENROD("darkgoldenrod", "#b8860b"),
    DARKGRAY("darkgray", "#a9a9a9"),
    DARKGREEN("darkgreen", "#006400"),
    DARKKHAKI("darkkhaki", "#bdb76b"),
    DARKMAGENTA("darkmagenta", "#8b008b"),
    DARKOLIVEGREEN("darkolivegreen", "#556b2f"),
    DARKORANGE("darkorange", "#ff8c00"),
    DARKORCHID("darkorchid", "#9932cc"),
    DARKRED("darkred", "#8b0000"),
    DARKSALMON("darksalmon", "#e9967a"),
    DARKSEAGREEN("darkseagreen", "#8fbc8f"),
    DARKSLATEBLUE("darkslateblue", "#483d8b"),
    DARKSLATEGRAY("darkslategray", "#2f4f4f"),
    DARKTURQUOISE("darkturquoise", "#00ced1"),
    DARKVIOLET("darkviolet", "#9400d3"),
    DEEPPINK("deeppink", "#ff1493"),
    DEEPSKYBLUE("deepskyblue", "#00bfff"),
    DIMGRAY("dimgray", "#696969"),
    DODGERBLUE("dodgerblue", "#1e90ff"),
    FIREBRICK("firebrick", "#b22222"),
    FLORALWHITE("floralwhite", "#fffaf0"),
    FORESTGREEN("forestgreen", "#228b22"),
    GAINSBORO("gainsboro", "#dcdcdc"),
    GHOSTWHITE("ghostwhite", "#f8f8ff"),
    GOLDENROD("goldenrod", "#daa520"),
    GREENYELLOW("greenyellow", "#adff2f"),
    HONEYDEW("honeydew", "#f0fff0"),
    HOTPINK("hotpink", "#ff69b4"),
    INDIANRED("indianred", "#cd5c5c"),
    LAVENDERBLUSH("lavenderblush", "#fff0f5"),
    LAWNGREEN("lawngreen", "#7cfc00"),
    LEMONCHIFFON("lemonchiffon", "#fffacd"),
    LIGHTBLUE("lightblue", "#add8e6"),
    LIGHTCORAL("lightcoral", "#f08080"),
    LIGHTCYAN("lightcyan", "#e0ffff"),
    LIGHTGOLDENRODYELLOW("lightgoldenrodyellow", "#fafad2"),
    LIGHTGREEN("lightgreen", "#90ee90"),
    LIGHTPINK("lightpink", "#ffb6c1"),
    LIGHTSALMON("lightsalmon", "#ffa07a"),
    LIGHTSEAGREEN("lightseagreen", "#20b2aa"),
    LIGHTSKYBLUE("lightskyblue", "#87cefa"),
    LIGHTSLATEGRAY("lightslategray", "#778899"),
    LIGHTSTEELBLUE("lightsteelblue", "#b0c4de"),
    LIGHTYELLOW("lightyellow", "#ffffe0"),
    LIMEGREEN("limegreen", "#32cd32"),
    LINEN("linen", "#faf0e6"),
    MEDIUMAQUAMARINE("mediumaquamarine", "#66cdaa"),
    MEDIUMBLUE("mediumblue", "#0000cd"),
    MEDIUMORCHID("mediumorchid", "#ba55d3"),
    MEDIUMPURPLE("mediumpurple", "#9370db"),
    MEDIUMSEAGREEN("mediumseagreen", "#3cb371"),
    MEDIUMSLATEBLUE("mediumslateblue", "#7b68ee"),
    MEDIUMSPRINGGREEN("mediumspringgreen", "#00fa9a"),
    MEDIUMTURQUOISE("mediumturquoise", "#48d1cc"),
    MEDIUMVIOLETRED("mediumvioletred", "#c71585"),
    MIDNIGHTBLUE("midnightblue", "#191970"),
    MINTCREAM("mintcream", "#f5fffa"),
    MISTYROSE("mistyrose", "#ffe4e1"),
    MOCCASIN("moccasin", "#ffe4b5"),
    NAVAJOWHITE("navajowhite", "#ffdead"),
    OLDLACE("oldlace", "#fdf5e6"),
    OLIVEDRAB("olivedrab", "#6b8e23"),
    ORANGERED("orangered", "#ff4500"),
    PALEGOLDENROD("palegoldenrod", "#eee8aa"),
    PALEGREEN("palegreen", "#98fb98"),
    PALETURQUOISE("paleturquoise", "#afeeee"),
    PALEVIOLETRED("palevioletred", "#db7093"),
    PAPAYAWHIP("papayawhip", "#ffefd5"),
    PEACHPUFF("peachpuff", "#ffdab9"),
    PERU("peru", "#cd853f"),
    POWDERBLUE("powderblue", "#b0e0e6"),
    ROSYBROWN("rosybrown", "#bc8f8f"),
    ROYALBLUE("royalblue", "#4169e1"),
    SADDLEBROWN("saddlebrown", "#8b4513"),
    SANDYBROWN("sandybrown", "#f4a460"),
    SEAGREEN("seagreen", "#2e8b57"),
    SEASHELL("seashell", "#fff5ee"),
    SIENNA("sienna", "#a0522d"),
    SKYBLUE("skyblue", "#87ceeb"),
    SLATEBLUE("slateblue", "#6a5acd"),
    SLATEGRAY("slategray", "#708090"),
    SNOW("snow", "#fffafa"),
    SPRINGGREEN("springgreen", "#00ff7f"),
    STEELBLUE("steelblue", "#4682b4"),
    TAN("tan", "#d2b48c"),
    THISTLE("thistle", "#d8bfd8"),
    WHEAT("wheat", "#f5deb3"),
    WHITESMOKE("whitesmoke", "#f5f5f5"),
    YELLOWGREEN("yellowgreen", "#9acd32"),

    // ─── CORES METÁLICAS ────────────────────────────────────────────
    // (GOLD, SILVER, BRONZE já definidos)
    COPPER("copper", "#b87333"),
    BRASS("brass", "#b5a642"),
    STEEL("steel", "#4682b4"),
    CHROME("chrome", "#dbdbdb"),
    NICKEL("nickel", "#727472"),
    TITANIUM("titanium", "#878681"),
    PLATINUM("platinum", "#e5e4e2"),

    // ─── CORES PASTEL ──────────────────────────────────────────────
    PASTEL_RED("pastel_red", "#ffb3ba"),
    PASTEL_GREEN("pastel_green", "#baffc9"),
    PASTEL_BLUE("pastel_blue", "#bae1ff"),
    PASTEL_YELLOW("pastel_yellow", "#ffffba"),
    PASTEL_CYAN("pastel_cyan", "#bae1ff"),
    PASTEL_MAGENTA("pastel_magenta", "#ffb3ba"),
    PASTEL_ORANGE("pastel_orange", "#ffd1b3"),
    PASTEL_PURPLE("pastel_purple", "#e0bbff"),
    PASTEL_PINK("pastel_pink", "#ffd1dc"),
    PASTEL_LIME("pastel_lime", "#d0f0c0"),
    PASTEL_TEAL("pastel_teal", "#b3d9d9"),
    PASTEL_LAVENDER("pastel_lavender", "#e6e6fa"),

    // ─── CORES VIBRANTES ────────────────────────────────────────────
    VIBRANT_RED("vibrant_red", "#ff0000"),
    VIBRANT_GREEN("vibrant_green", "#00ff00"),
    VIBRANT_BLUE("vibrant_blue", "#0000ff"),
    VIBRANT_YELLOW("vibrant_yellow", "#ffff00"),
    VIBRANT_CYAN("vibrant_cyan", "#00ffff"),
    VIBRANT_MAGENTA("vibrant_magenta", "#ff00ff"),
    VIBRANT_ORANGE("vibrant_orange", "#ff6600"),
    VIBRANT_PURPLE("vibrant_purple", "#aa00ff"),
    VIBRANT_PINK("vibrant_pink", "#ff1493"),
    VIBRANT_LIME("vibrant_lime", "#32cd32"),

    // ─── CORES SISTEMA ─────────────────────────────────────────────
    ERROR("error", "#ef4444"),
    SUCCESS("success", "#22c55e"),
    WARNING("warning", "#eab308"),
    INFO("info", "#3b82f6"),
    DEBUG("debug", "#71717a"),
    TRACE("trace", "#a855f7"),
    FATAL("fatal", "#dc2626"),

    // ─── CORES TRANSPARENTES ───────────────────────────────────────
    TRANSPARENT("transparent", "rgba(0,0,0,0)"),
    SEMITRANSPARENT_BLACK("semitransparent_black", "rgba(0,0,0,0.5)"),
    SEMITRANSPARENT_WHITE("semitransparent_white", "rgba(255,255,255,0.5)"),

    // ─── PALETA TAILWIND ───────────────────────────────────────────
    SLATE_500("slate-500", "#64748b"),
    RED_500("red-500", "#ef4444"),
    ORANGE_500("orange-500", "#f97316"),
    AMBER_500("amber-500", "#f59e0b"),
    EMERALD_500("emerald-500", "#10b981"),
    SKY_500("sky-500", "#0ea5e9"),
    INDIGO_500("indigo-500", "#6366f1"),
    VIOLET_500("violet-500", "#8b5cf6"),
    FUCHSIA_500("fuchsia-500", "#d946ef"),
    ROSE_500("rose-500", "#f43f5e");

    // ─── CAMPOS ──────────────────────────────────────────────────────

    private final String webName;
    private final String hex;
    private final int argb;

    // ─── CONSTRUTOR ──────────────────────────────────────────────────
    XplColor(String webName, String hex) {
        this.webName = webName;
        this.hex = hex;
        this.argb = parseHexToArgb(hex);
    }

    // ─── GETTERS ────────────────────────────────────────────────────
    public String getWebName() {
        return webName;
    }

    public String toHex() {
        return hex;
    }

    /**
     * Retorna o valor ARGB nativo para o Skia.
     */
    public int toArgb() {
        return argb;
    }

    // ─── MÉTODOS ESTÁTICOS ──────────────────────────────────────────

    private static final Map<String, XplColor> BY_WEB_NAME = new HashMap<>();
    private static final Map<Integer, XplColor> BY_ARGB = new HashMap<>();

    static {
        for (XplColor color : values()) {
            BY_WEB_NAME.put(color.webName, color);
            BY_ARGB.put(color.argb, color);
        }
    }

    /**
     * Obtém uma cor a partir do nome CSS (ex: "red", "light_red", "bg_black").
     * Suporta espaços convertidos para underscore (ex: "light red" → "light_red").
     */
    public static XplColor fromWebName(String name) {
        if (name == null) return null;
        String normalized = name.trim().toLowerCase().replace(' ', '_');
        return BY_WEB_NAME.get(normalized);
    }

    /**
     * Obtém uma cor a partir do valor ARGB.
     */
    public static XplColor fromArgb(int argb) {
        return BY_ARGB.get(argb);
    }

    /**
     * Obtém uma cor a partir do código hex (ex: "#FF0000" ou "FF0000").
     * Suporta RGB, RGBA, RRGGBB, AARRGGBB.
     */
    public static XplColor fromHex(String hex) {
        if (hex == null) return null;
        try {
            int argb = parseHexToArgb(hex);
            return fromArgb(argb);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Converte uma string hex (com ou sem #) para ARGB.
     * Suporta: #RRGGBB, #AARRGGBB, #RGB, #ARGB, RRGGBB, etc.
     */
    private static int parseHexToArgb(String hex) {
        if (hex == null) return 0xFF000000;
        String clean = hex.trim();
        if (clean.startsWith("#")) {
            clean = clean.substring(1);
        }
        if (clean.startsWith("rgba(") || clean.startsWith("rgb(")) {
            // Fallback: se for rgba/rgb, retorna transparente ou tenta parse (simplificado)
            return 0x00000000;
        }
        if (clean.length() == 3) {
            // #RGB → #RRGGBB
            char r = clean.charAt(0);
            char g = clean.charAt(1);
            char b = clean.charAt(2);
            clean = "" + r + r + g + g + b + b;
        } else if (clean.length() == 4) {
            // #ARGB → #AARRGGBB
            char a = clean.charAt(0);
            char r = clean.charAt(1);
            char g = clean.charAt(2);
            char b = clean.charAt(3);
            clean = "" + a + a + r + r + g + g + b + b;
        }
        if (clean.length() == 6) {
            clean = "FF" + clean; // opaco
        }
        if (clean.length() == 8) {
            return (int) Long.parseLong(clean, 16);
        }
        return 0xFF000000; // fallback
    }

    // ─── MÉTODO DE RESOLUÇÃO (estilo Rust) ─────────────────────────

    /**
     * Resolve uma cor a partir de uma string (nome, hex, rgb).
     * Útil para o CSSStyleEngine ou para aplicar estilos dinâmicos.
     */
    public static int resolveColor(String input) {
        if (input == null || input.trim().isEmpty()) return 0xFF000000;

        // 1. Tenta pelo nome da paleta
        XplColor color = fromWebName(input);
        if (color != null) return color.toArgb();

        // 2. Tenta parsear como hex
        try {
            return parseHexToArgb(input);
        } catch (Exception e) {
            return 0xFF000000;
        }
    }
}