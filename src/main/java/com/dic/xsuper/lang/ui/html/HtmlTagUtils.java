package com.dic.xsuper.lang.ui.html;

import java.util.*;

public final class HtmlTagUtils {
    public static final Map<String, List<String>> TAG_SPECIFIC_ATTRIBUTES = new LinkedHashMap<>();
    public static final Set<String> GLOBAL_ATTRIBUTES = Set.of(
            "id", "class", "style", "title", "tabindex", "hidden", "lang", "dir",
            "accesskey", "draggable", "spellcheck", "contenteditable", "translate",
            "role", "slot", "inert", "popover"
    );
    public static final Set<String> EMPTY_TAGS = Set.of(
            "input", "img", "br", "hr", "meta", "link", "source", "track", "area",
            "col", "base", "embed", "param", "wbr"
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

            // Scripting
            "canvas", "svg", "math",

            // Nós especiais (internos)
            "#text", "#comment", "#document-fragment", "root"
    );

    static {
        // ─── Estrutura ──────────────────────────────────────────────────────
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
        TAG_SPECIFIC_ATTRIBUTES.put("dialog", List.of("open"));

        // ─── Secções ────────────────────────────────────────────────────────
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

        // ─── Agrupamento de conteúdo ──────────────────────────────────────
        TAG_SPECIFIC_ATTRIBUTES.put("p", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("hr", List.of());
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

        // ─── Texto ──────────────────────────────────────────────────────────
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

        // ─── Mídia embutida ────────────────────────────────────────────────
        TAG_SPECIFIC_ATTRIBUTES.put("img", List.of("src", "alt", "width", "height", "srcset", "sizes", "crossorigin", "loading", "decoding", "referrerpolicy"));
        TAG_SPECIFIC_ATTRIBUTES.put("picture", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("source", List.of("src", "srcset", "sizes", "type", "media", "width", "height"));
        TAG_SPECIFIC_ATTRIBUTES.put("video", List.of("src", "controls", "autoplay", "loop", "muted", "preload", "poster", "playsinline", "width", "height"));
        TAG_SPECIFIC_ATTRIBUTES.put("audio", List.of("src", "controls", "autoplay", "loop", "muted", "preload"));
        TAG_SPECIFIC_ATTRIBUTES.put("track", List.of("src", "kind", "srclang", "label", "default"));
        TAG_SPECIFIC_ATTRIBUTES.put("map", List.of("name"));
        TAG_SPECIFIC_ATTRIBUTES.put("area", List.of("alt", "coords", "shape", "href", "target", "download", "rel", "hreflang", "type"));

        // ─── Tabelas ────────────────────────────────────────────────────────
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

        // ─── Formulários ────────────────────────────────────────────────────
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

        // ─── Interação ──────────────────────────────────────────────────────
        TAG_SPECIFIC_ATTRIBUTES.put("details", List.of("open"));
        TAG_SPECIFIC_ATTRIBUTES.put("summary", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("menu", List.of("type", "label"));
        TAG_SPECIFIC_ATTRIBUTES.put("menuitem", List.of("type", "label", "icon", "disabled", "checked", "radiogroup", "command"));

        // ─── Scripting ──────────────────────────────────────────────────────
        TAG_SPECIFIC_ATTRIBUTES.put("canvas", List.of("width", "height"));
        TAG_SPECIFIC_ATTRIBUTES.put("svg", List.of("width", "height", "viewBox"));
        TAG_SPECIFIC_ATTRIBUTES.put("math", List.of());

        // ─── Obsoletas / menos usadas (algumas) ────────────────────────────
        TAG_SPECIFIC_ATTRIBUTES.put("center", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("font", List.of("color", "face", "size"));
        TAG_SPECIFIC_ATTRIBUTES.put("marquee", List.of("behavior", "direction", "scrollamount", "scrolldelay", "loop", "width", "height"));
        TAG_SPECIFIC_ATTRIBUTES.put("frame", List.of("src", "name", "scrolling", "marginwidth", "marginheight", "noresize", "frameborder"));
        TAG_SPECIFIC_ATTRIBUTES.put("frameset", List.of("cols", "rows", "border"));
        TAG_SPECIFIC_ATTRIBUTES.put("iframe", List.of("src", "srcdoc", "name", "width", "height", "sandbox", "loading", "allow", "allowfullscreen", "referrerpolicy"));

        // ─── Nós especiais (usados internamente) ──────────────────────────
        TAG_SPECIFIC_ATTRIBUTES.put("text", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("#text", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("#comment", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("#document-fragment", List.of());
        TAG_SPECIFIC_ATTRIBUTES.put("root", List.of());
    }

    private HtmlTagUtils() {
        // Construtor privado para evitar instanciação
    }

    /**
     * Retorna a lista de atributos específicos para uma tag HTML.
     *
     * @param tagName Nome da tag (case-insensitive)
     * @return Lista de atributos permitidos (pode ser vazia)
     */
    public static List<String> getSpecificAttributes(String tagName) {
        if (tagName == null) return Collections.emptyList();
        return TAG_SPECIFIC_ATTRIBUTES.getOrDefault(tagName.toLowerCase(), Collections.emptyList());
    }

    /**
     * Verifica se um atributo é específico de uma determinada tag.
     *
     * @param tagName  Nome da tag
     * @param attrName Nome do atributo
     * @return true se o atributo é válido para a tag
     */
    public static boolean isSpecificAttribute(String tagName, String attrName) {
        if (tagName == null || attrName == null) return false;
        List<String> attrs = getSpecificAttributes(tagName);
        return attrs.contains(attrName);
    }

    /**
     * Retorna o mapa completo (para uso em depuração ou extensão).
     */
    public static Map<String, List<String>> getAttributeMap() {
        return Collections.unmodifiableMap(TAG_SPECIFIC_ATTRIBUTES);
    }

    /**
     * Verifica se uma tag é nativa do HTML.
     */
    public static boolean isNativeTag(String tagName) {
        return tagName != null && NATIVE_TAGS.contains(tagName.toLowerCase());
    }

    /**
     * Verifica se uma tag personalizada tem um nome válido.
     * Permite PascalCase (ex: MeuBotao) ou kebab-case (ex: meu-botao).
     */
    public static boolean isValidCustomTagName(String tagName) {
        if (tagName == null || tagName.isEmpty()) return false;
        // PascalCase: começa com maiúscula, seguido de letras/dígitos
        if (tagName.matches("^[A-Z][a-zA-Z0-9]*$")) {
            return true;
        }
        // kebab-case: minúsculas separadas por hífen
        return tagName.matches("^[a-z]+(-[a-z]+)*$");
    }

    /**
     * Verifica se uma tag é válida (nativa ou personalizada com nome válido).
     */
    public static boolean isValidTag(String tagName) {
        return isNativeTag(tagName) || isValidCustomTagName(tagName);
    }

    /**
     * Retorna a lista de tags nativas (para uso externo, se necessário).
     */
    public static Set<String> getNativeTags() {
        return NATIVE_TAGS;
    }

    /**
     * Método recursivo que percorre a árvore de nós para garantir a integridade estrutural.
     *
     * @param node O nó atual a verificar
     */
    public static void validateForbiddenTags(Set<String> FORBIDDEN_TAGS_IN_COMPONENTS, XplNode node) {
        if (node == null) return;

        // 1. Verifica o nó atual
        if (FORBIDDEN_TAGS_IN_COMPONENTS.contains(node.tag.toLowerCase())) {
            throw new RuntimeException(String.format(
                    "Erro de Arquitetura: A tag '<%s>' é proibida dentro de componentes customizados. " +
                            "Apenas pode ser definida na raiz do documento principal.",
                    node.tag
            ));
        }

        // 2. Recursividade: Percorre todos os filhos (se existirem)
        if (node.children != null) {
            for (XplNode child : node.children) {
                validateForbiddenTags(FORBIDDEN_TAGS_IN_COMPONENTS, child);
            }
        }
    }
}