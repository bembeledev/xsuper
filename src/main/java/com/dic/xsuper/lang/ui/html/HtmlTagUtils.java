package com.dic.xsuper.lang.ui.html;

import java.util.*;

public final class HtmlTagUtils {
    public static final Map<String, List<String>> TAG_SPECIFIC_ATTRIBUTES = new LinkedHashMap<>();

    public static final Set<String> GLOBAL_ATTRIBUTES = Set.of(
            "id", "class", "style", "title", "tabindex", "hidden", "lang", "dir",
            "accesskey", "draggable", "spellcheck", "contenteditable", "translate",
            "role", "slot", "inert", "popover",
            "fill", "stroke", "stroke-width"
    );

    // ⭐ 1. A NOVA LISTA EXCLUSIVA DE SHAPES SVG
    public static final Set<String> SVG_SHAPES = Set.of(
            "path", "circle", "rect", "line", "polygon", "polyline",
            "ellipse", "arc", "quadcurve", "cubiccurve", "content"
    );

    public static final Set<String> EMPTY_TAGS = Set.of(
            "input", "img", "br", "hr", "meta", "link", "source", "track", "area",
            "col", "base", "embed", "param", "wbr",
            // Os shapes SVG também são vazios por defeito na nossa engine
            "path", "circle", "rect", "line", "polygon", "polyline",
            "ellipse", "arc", "quadcurve", "cubiccurve"
    );

    // ─── Tags nativas HTML ──────────────────────────────────────────────────
    private static final Set<String> NATIVE_TAGS = Set.of(
            // Estrutura
            "html", "head", "body", "title", "base", "link", "meta", "style",
            "script", "noscript", "template", "slot", "dialog", "web", "tabs","tab",

            // Secções
            "section", "nav", "article", "aside", "header", "footer", "main",
            "address", "hgroup", "h1", "h2", "h3", "h4", "h5", "h6",

            // Agrupamento
            "p", "hr", "pre", "blockquote", "ol", "ul", "li", "dl", "dt", "dd",
            "figure", "figcaption", "div",

            // Texto
            "a", "em", "strong", "small", "s", "cite", "q", "dfn", "abbr",
            "ruby", "rt", "rp", "data", "time", "code", "var", "samp", "kbd",
            "sub", "sup", "i", "b", "u", "mark", "bdi", "bdo", "span", "br", "wbr",

            // Mídia
            "img", "picture", "source", "video", "audio", "track", "map", "area",

            // Tabelas
            "table", "caption", "colgroup", "col", "tbody", "thead", "tfoot",
            "tr", "td", "th",

            // Formulários
            "form", "label", "input", "button", "select", "datalist", "optgroup",
            "option", "textarea", "output", "progress", "meter", "fieldset", "legend",

            // Interação
            "details", "summary", "menu", "menuitem",

            // Scripting & Vector (Canvas e SVG integrados)
            "canvas", "svg", "math", "chart",
            "path", "circle", "rect", "line", "polygon", "polyline",
            "ellipse", "arc", "quadcurve", "cubiccurve",

            // Nós especiais (internos)
            "#text", "#comment", "#document-fragment", "root", "content",
            "popup", "tooltip",
            // ─── TAGS DE JOGO (FXGL) ──────────────────────────────────────────
            "game", "physics", "level", "camera", "entity", "player", "enemy",
            "powerup", "projectile", "spawner", "trigger", "animation"
    );

    static {
        // ... (O teu bloco static gigante de atributos mantém-se inalterado) ...
        TAG_SPECIFIC_ATTRIBUTES.put("html", List.of("lang", "xmlns"));
        TAG_SPECIFIC_ATTRIBUTES.put("head", List.of("profile"));
        TAG_SPECIFIC_ATTRIBUTES.put("title", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("base", List.of("href", "target"));
        TAG_SPECIFIC_ATTRIBUTES.put("link", List.of("rel", "href", "type", "media", "sizes", "crossorigin", "integrity"));
        TAG_SPECIFIC_ATTRIBUTES.put("meta", List.of("charset", "name", "http-equiv", "content"));
        TAG_SPECIFIC_ATTRIBUTES.put("style", List.of("type", "media", "scoped"));
        TAG_SPECIFIC_ATTRIBUTES.put("script", List.of("src", "type", "async", "defer", "crossorigin", "integrity"));
        TAG_SPECIFIC_ATTRIBUTES.put("noscript", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("template", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("slot", List.of("name"));
        TAG_SPECIFIC_ATTRIBUTES.put("section", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("nav", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("article", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("aside", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("header", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("footer", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("main", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("address", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("hgroup", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("h1", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("h2", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("h3", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("h4", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("h5", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("h6", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("p", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("pre", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("blockquote", List.of("cite"));
        TAG_SPECIFIC_ATTRIBUTES.put("ol", List.of("reversed", "start", "type"));
        TAG_SPECIFIC_ATTRIBUTES.put("ul", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("li", List.of("value"));
        TAG_SPECIFIC_ATTRIBUTES.put("dl", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("dt", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("dd", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("figure", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("figcaption", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("div", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("a", List.of("href", "target", "download", "rel", "hreflang", "type", "ping", "referrerpolicy"));
        TAG_SPECIFIC_ATTRIBUTES.put("em", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("strong", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("small", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("s", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("cite", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("q", List.of("cite"));
        TAG_SPECIFIC_ATTRIBUTES.put("dfn", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("abbr", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("ruby", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("rt", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("rp", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("data", List.of("value"));
        TAG_SPECIFIC_ATTRIBUTES.put("time", List.of("datetime"));
        TAG_SPECIFIC_ATTRIBUTES.put("code", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("var", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("samp", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("kbd", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("sub", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("sup", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("i", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("b", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("u", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("mark", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("bdi", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("bdo", List.of("dir"));
        TAG_SPECIFIC_ATTRIBUTES.put("span", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("br", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("wbr", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("img", List.of("src", "alt", "width", "height", "srcset", "sizes", "crossorigin", "loading", "decoding", "referrerpolicy"));
        TAG_SPECIFIC_ATTRIBUTES.put("picture", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("source", List.of("src", "srcset", "sizes", "type", "media", "width", "height"));
        TAG_SPECIFIC_ATTRIBUTES.put("video", List.of("src", "controls", "autoplay", "loop", "muted", "preload", "poster", "playsinline", "width", "height"));
        TAG_SPECIFIC_ATTRIBUTES.put("audio", List.of("src", "controls", "autoplay", "loop", "muted", "preload"));
        TAG_SPECIFIC_ATTRIBUTES.put("track", List.of("src", "kind", "srclang", "label", "default"));
        TAG_SPECIFIC_ATTRIBUTES.put("map", List.of("name"));
        TAG_SPECIFIC_ATTRIBUTES.put("area", List.of("alt", "coords", "shape", "href", "target", "download", "rel", "hreflang", "type"));
        TAG_SPECIFIC_ATTRIBUTES.put("table", List.of("border", "cellpadding", "cellspacing", "summary"));
        TAG_SPECIFIC_ATTRIBUTES.put("caption", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("colgroup", List.of("span"));
        TAG_SPECIFIC_ATTRIBUTES.put("col", List.of("span"));
        TAG_SPECIFIC_ATTRIBUTES.put("thead", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("tbody", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("tfoot", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("tr", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("td", List.of("colspan", "rowspan", "headers"));
        TAG_SPECIFIC_ATTRIBUTES.put("th", List.of("colspan", "rowspan", "headers", "scope", "abbr"));
        TAG_SPECIFIC_ATTRIBUTES.put("form", List.of("action", "method", "enctype", "target", "autocomplete", "novalidate", "accept-charset"));
        TAG_SPECIFIC_ATTRIBUTES.put("label", List.of("for"));
        TAG_SPECIFIC_ATTRIBUTES.put("input", List.of("type", "name", "value", "placeholder", "required", "disabled", "readonly",
                "maxlength", "min", "max", "step", "pattern", "autocomplete", "autofocus", "multiple",
                "accept", "checked", "src", "alt", "width", "height", "form", "formaction", "formenctype",
                "formmethod", "formnovalidate", "formtarget", "list", "dirname"));
        TAG_SPECIFIC_ATTRIBUTES.put("button", List.of("type", "name", "value", "disabled", "autofocus", "form", "formaction",
                "formenctype", "formmethod", "formnovalidate", "formtarget"));
        TAG_SPECIFIC_ATTRIBUTES.put("select", List.of("name", "multiple", "size", "disabled", "required", "autofocus", "form"));
        TAG_SPECIFIC_ATTRIBUTES.put("datalist", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("optgroup", List.of("label", "disabled"));
        TAG_SPECIFIC_ATTRIBUTES.put("option", List.of("value", "label", "selected", "disabled"));
        TAG_SPECIFIC_ATTRIBUTES.put("textarea", List.of("name", "rows", "cols", "maxlength", "placeholder", "required", "disabled",
                "readonly", "autofocus", "form", "wrap", "dirname"));
        TAG_SPECIFIC_ATTRIBUTES.put("output", List.of("for", "name", "form"));
        TAG_SPECIFIC_ATTRIBUTES.put("progress", List.of("value", "max"));
        TAG_SPECIFIC_ATTRIBUTES.put("meter", List.of("value", "min", "max", "low", "high", "optimum"));
        TAG_SPECIFIC_ATTRIBUTES.put("fieldset", List.of("disabled", "form", "name"));
        TAG_SPECIFIC_ATTRIBUTES.put("legend", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("details", List.of("open"));
        TAG_SPECIFIC_ATTRIBUTES.put("summary", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("menu", List.of("type", "label"));
        TAG_SPECIFIC_ATTRIBUTES.put("menuitem", List.of("type", "label", "icon", "disabled", "checked", "radiogroup", "command"));
        TAG_SPECIFIC_ATTRIBUTES.put("canvas", List.of("width", "height"));
        TAG_SPECIFIC_ATTRIBUTES.put("svg", List.of("width", "height", "viewBox"));
        TAG_SPECIFIC_ATTRIBUTES.put("math", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("center", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("font", List.of("color", "face", "size"));
        TAG_SPECIFIC_ATTRIBUTES.put("marquee", List.of("behavior", "direction", "scrollamount", "scrolldelay", "loop", "width", "height"));
        TAG_SPECIFIC_ATTRIBUTES.put("frame", List.of("src", "name", "scrolling", "marginwidth", "marginheight", "noresize", "frameborder"));
        TAG_SPECIFIC_ATTRIBUTES.put("frameset", List.of("cols", "rows", "border"));
        TAG_SPECIFIC_ATTRIBUTES.put("iframe", List.of("src", "srcdoc", "name", "width", "height", "sandbox", "loading", "allow", "allowfullscreen", "referrerpolicy"));
        TAG_SPECIFIC_ATTRIBUTES.put("#text", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("text", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("#comment", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("#document-fragment", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("root", List.of());
        // ─── Atributos Vetoriais SVG (Para passarem no Filtro de Segurança do DOM) ───
        TAG_SPECIFIC_ATTRIBUTES.put("path", List.of("d", "fill", "stroke", "stroke-width"));
        TAG_SPECIFIC_ATTRIBUTES.put("circle", List.of("cx", "cy", "r", "fill", "stroke", "stroke-width"));
        TAG_SPECIFIC_ATTRIBUTES.put("rect", List.of("x", "y", "width", "height", "rx", "ry", "fill", "stroke", "stroke-width"));
        TAG_SPECIFIC_ATTRIBUTES.put("line", List.of("x1", "y1", "x2", "y2", "stroke", "stroke-width"));
        TAG_SPECIFIC_ATTRIBUTES.put("polygon", List.of("points", "fill", "stroke", "stroke-width"));
        TAG_SPECIFIC_ATTRIBUTES.put("polyline", List.of("points", "fill", "stroke", "stroke-width"));
        TAG_SPECIFIC_ATTRIBUTES.put("ellipse", List.of("cx", "cy", "rx", "ry", "fill", "stroke", "stroke-width"));
        TAG_SPECIFIC_ATTRIBUTES.put("arc", List.of("cx", "cy", "rx", "ry", "start", "length", "type", "fill", "stroke", "stroke-width"));
        TAG_SPECIFIC_ATTRIBUTES.put("quadcurve", List.of("startX", "startY", "controlX", "controlY", "endX", "endY", "fill", "stroke", "stroke-width"));
        TAG_SPECIFIC_ATTRIBUTES.put("cubiccurve", List.of("startX", "startY", "controlX1", "controlY1", "controlX2", "controlY2", "endX", "endY", "fill", "stroke", "stroke-width"));
        TAG_SPECIFIC_ATTRIBUTES.put("content", List.of("x", "y", "dx", "dy", "text-anchor", "font-size", "font-family", "font-weight", "fill", "stroke", "stroke-width", "rotate", "length-adjust"));
        TAG_SPECIFIC_ATTRIBUTES.put("chart", List.of(
                "type",          // tipo de gráfico (bar, line, pie, etc.)
                "data",          // dados (pode ser um JSON string)
                "options",       // opções de configuração (JSON)
                "width", "height", "model",
                "theme",         // claro/escuro
                "responsive",    // boolean
                "title",         // título do gráfico
                "colors",        // paleta de cores
                "animation",     // boolean ou objeto
                "tooltip",       // boolean ou objeto
                "legend"         // boolean ou objeto
        ));
        TAG_SPECIFIC_ATTRIBUTES.put("hr", List.of("orientation"));
        TAG_SPECIFIC_ATTRIBUTES.put("dialog", List.of(
                // ─── Atributos W3C padrão ──────────────────────────────────────────────
                "open",           // Indica que o diálogo está aberto (booleano)
                "returnvalue",    // Valor de retorno definido ao fechar (string)

                // ─── Atributos estendidos da SuperUI ──────────────────────────────────
                "close-on-backdrop",   // Fecha ao clicar no fundo (backdrop)
                "close-on-escape",     // Fecha com a tecla ESC
                "modal",               // True para overlay modal (padrão true)
                "show-close-button",   // Mostra o botão ✕ no canto superior direito
                "width",               // Largura da caixa do diálogo (ex: "400px")
                "max-width",           // Largura máxima (ex: "600px")
                "title",               // Título (pode ser usado como cabeçalho)
                "message"              // Mensagem textual (útil para diálogos simples)
        ));
        TAG_SPECIFIC_ATTRIBUTES.put("popup", List.of(
                "anchor",    // O ID do elemento alvo onde o balão vai flutuar
                "show",      // A variável reativa booleana (true/false)
                "position",  // A direção do balão (top, bottom, left, right)
                "title",     // Opcional, caso queiras dar um título ao popup no futuro));
                "timeout", // ⭐ NOVO: Tempo em milissegundos
                "onclose"  // ⭐ NOVO: Evento para avisar o XPL que fechou
        ));

        TAG_SPECIFIC_ATTRIBUTES.put("tooltip", List.of(
                "anchor",    // O ID do elemento alvo onde o balão vai flutuar
                "show",      // A variável reativa booleana (true/false)
                "position",  // A direção do balão (top, bottom, left, right)
                "text"
        ));

        // ─── TAGS DE JOGO (FXGL) ──────────────────────────────────────────────────
        TAG_SPECIFIC_ATTRIBUTES.put("game", List.of(
                "width", "height", "title", "fps", "debug", "id", "class", "style"
        ));

        TAG_SPECIFIC_ATTRIBUTES.put("physics", List.of(
                "gravity-x", "gravity-y", "velocity-iterations", "position-iterations"
        ));

        TAG_SPECIFIC_ATTRIBUTES.put("level", List.of(
                "src", "background"
        ));

        TAG_SPECIFIC_ATTRIBUTES.put("camera", List.of(
                "target", "zoom", "bounds-x", "bounds-y", "bounds-width", "bounds-height"
        ));

        TAG_SPECIFIC_ATTRIBUTES.put("entity", List.of(
                "type", "x", "y", "sprite", "width", "height", "bbox", "id", "class", "style"
        ));

        TAG_SPECIFIC_ATTRIBUTES.put("player", List.of(
                "x", "y", "speed", "jump-force", "sprite", "controls", "id", "class", "style"
        ));

        TAG_SPECIFIC_ATTRIBUTES.put("enemy", List.of(
                "x", "y", "speed", "hp", "behavior", "sprite", "id", "class", "style"
        ));

        TAG_SPECIFIC_ATTRIBUTES.put("powerup", List.of(
                "x", "y", "type", "sprite", "id", "class", "style"
        ));

        TAG_SPECIFIC_ATTRIBUTES.put("projectile", List.of(
                "type", "x", "y", "speed", "damage", "lifetime", "sprite", "width", "height",
                "direction", "angle", "gravity", "piercing", "on-hit", "on-expire",
                "id", "class", "style"
        ));

        TAG_SPECIFIC_ATTRIBUTES.put("spawner", List.of(
                "x", "y", "interval", "max-entities", "entity-type", "id", "class", "style"
        ));

        TAG_SPECIFIC_ATTRIBUTES.put("trigger", List.of(
                "x", "y", "width", "height", "target-type", "on-enter", "on-exit",
                "repeat", "delay", "active", "color", "id", "class", "style"
        ));

        TAG_SPECIFIC_ATTRIBUTES.put("animation", List.of(
                "id", "sprite", "frames", "frame-width", "frame-height", "columns", "rows",
                "start-frame", "end-frame", "duration", "loop", "auto-play", "on-complete"
        ));
    }

    private HtmlTagUtils() {
        // Construtor privado para evitar instanciação
    }

    public static List<String> getSpecificAttributes(String tagName) {
        if (tagName == null) return Collections.emptyList();
        return TAG_SPECIFIC_ATTRIBUTES.getOrDefault(tagName.toLowerCase(), Collections.emptyList());
    }

    public static boolean isSpecificAttribute(String tagName, String attrName) {
        if (tagName == null || attrName == null) return false;
        List<String> attrs = getSpecificAttributes(tagName);
        return attrs.contains(attrName);
    }

    public static Map<String, List<String>> getAttributeMap() {
        return Collections.unmodifiableMap(TAG_SPECIFIC_ATTRIBUTES);
    }

    public static boolean isNativeTag(String tagName) {
        return tagName != null && NATIVE_TAGS.contains(tagName.toLowerCase());
    }

    public static boolean isValidCustomTagName(String tagName) {
        if (tagName == null || tagName.isEmpty()) return false;
        if (tagName.matches("^[A-Z][a-zA-Z0-9]*$")) {
            return true;
        }
        return tagName.matches("^[a-z]+(-[a-z]+)*$");
    }

    public static boolean isValidTag(String tagName) {
        return isNativeTag(tagName) || isValidCustomTagName(tagName);
    }

    public static Set<String> getNativeTags() {
        return NATIVE_TAGS;
    }

    public static void validateForbiddenTags(Set<String> FORBIDDEN_TAGS_IN_COMPONENTS, XplNode node) {
        if (node == null) return;
        if (FORBIDDEN_TAGS_IN_COMPONENTS.contains(node.tag.toLowerCase())) {
            throw new RuntimeException(String.format(
                    "Erro de Arquitetura: A tag '<%s>' é proibida dentro de componentes customizados. " +
                            "Apenas pode ser definida na raiz do documento principal.",
                    node.tag
            ));
        }
        if (node.children != null) {
            for (XplNode child : node.children) {
                validateForbiddenTags(FORBIDDEN_TAGS_IN_COMPONENTS, child);
            }
        }
    }

    // ⭐ 2. O POLÍCIA DO SVG: Verifica recursivamente se um shape está perdido no DOM
    /**
     * Garante que as formas vetoriais (Shapes SVG) só são usadas dentro de uma tag <svg>.
     * O 'isInsideSvg' inicial deve ser 'false' quando chamado a partir do nó raiz (root).
     */
    public static void validateSvgStructure(XplNode node, boolean isInsideSvg) {
        if (node == null) return;

        String lowerTag = node.tag.toLowerCase();

        // Informa se este nó atual, ou qualquer dos seus pais, é a tela <svg>
        boolean currentlyInSvg = isInsideSvg || lowerTag.equals("svg");

        // Se encontrou uma tag de shape e NÃO estamos dentro de uma <svg>, rebenta!
        if (SVG_SHAPES.contains(lowerTag) && !currentlyInSvg) {
            throw new RuntimeException(String.format(
                    "Erro de Layout: A tag vetorial '<%s>' não pode ser usada solta no DOM. " +
                            "Deve estar encapsulada obrigatoriamente dentro de um contentor <svg> para não quebrar a grelha/flexbox.",
                    node.tag
            ));
        }

        // Continua a procurar nos filhos
        if (node.children != null) {
            for (XplNode child : node.children) {
                validateSvgStructure(child, currentlyInSvg);
            }
        }
    }
}