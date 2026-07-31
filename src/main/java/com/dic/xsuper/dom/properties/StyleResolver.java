package com.dic.xsuper.dom.properties;

import com.dic.xsuper.dom.properties.borderunit.BorderRadius;
import com.dic.xsuper.dom.properties.cssunit.CssValue;
import com.dic.xsuper.dom.properties.gradient.GradientParser;
import com.dic.xsuper.dom.properties.size.BoxSize;
import com.dic.xsuper.dom.properties.size.Dimension;
import com.dic.xsuper.dom.properties.style.UserAgentStyles;
import com.dic.xsuper.dom.properties.style.XplColor;

import java.util.*;

public class StyleResolver {

    /**
     * Resolve todos os estilos para um elemento, combinando:
     * - estilos padrão (UserAgent)
     * - estilos inline (do atributo style)
     * - estilos de classes (futuro)
     */
    public static ResolvedStyles resolve(String tagName, Map<String, String> inlineStyles) {



        // 1. Começa com os estilos padrão do user agent
        Map<String, String> defaultStyles = UserAgentStyles.getDefaultStyles(tagName);
        Map<String, String> allStyles = new LinkedHashMap<>(defaultStyles);

        // 2. Sobrescreve com os inline
        if (inlineStyles != null) {
            allStyles.putAll(inlineStyles);
        }

        // 3. Converte para ResolvedStyles
        ResolvedStyles resolved = new ResolvedStyles();

        // BoxSize (width, height, min/max)
        resolved.boxSize = parseBoxSize(allStyles);

        // ⭐ 1. PADDING (Lê o atalho e sobrescreve lados específicos)
        CssValue pt = CssValue.zero(), pr = CssValue.zero(), pb = CssValue.zero(), pl = CssValue.zero();
        String paddingStr = allStyles.get("padding");

        if (paddingStr != null) {
            Padding p = Padding.parse(paddingStr);
            pt = p.getTop(); pr = p.getRight(); pb = p.getBottom(); pl = p.getLeft();
        }
        if (allStyles.containsKey("padding-top")) pt = CssValue.parse(allStyles.get("padding-top"));
        if (allStyles.containsKey("padding-right")) pr = CssValue.parse(allStyles.get("padding-right"));
        if (allStyles.containsKey("padding-bottom")) pb = CssValue.parse(allStyles.get("padding-bottom"));
        if (allStyles.containsKey("padding-left")) pl = CssValue.parse(allStyles.get("padding-left"));

        resolved.padding = new Padding(pt, pr, pb, pl);

        // ⭐ 2. MARGIN (Lê o atalho e sobrescreve lados específicos)
        CssValue mt = CssValue.zero(), mr = CssValue.zero(), mb = CssValue.zero(), ml = CssValue.zero();
        String marginStr = allStyles.get("margin");


        if (marginStr != null) {
            Margin m = Margin.parse(marginStr);
            mt = m.getTop(); mr = m.getRight(); mb = m.getBottom(); ml = m.getLeft();
            if (tagName.equals("button")){
                System.out.println(m);
            }
        }



        if (allStyles.containsKey("margin-top")) mt = CssValue.parse(allStyles.get("margin-top"));
        if (allStyles.containsKey("margin-right")) mr = CssValue.parse(allStyles.get("margin-right"));
        if (allStyles.containsKey("margin-bottom")) mb = CssValue.parse(allStyles.get("margin-bottom"));
        if (allStyles.containsKey("margin-left")) ml = CssValue.parse(allStyles.get("margin-left"));

        resolved.margin = new Margin(mt, mr, mb, ml);

        // Border
        String borderStr = allStyles.get("border");
        if (borderStr != null) {
            // Divide "1px solid #ccc" nas suas 3 partes!
            String[] parts = borderStr.trim().split("\\s+");
            String bw = parts.length > 0 ? parts[0] : "0px";
            String bs = parts.length > 1 ? parts[1] : "solid";
            String bc = parts.length > 2 ? parts[2] : "black";
            resolved.border = Border.parse(bw, bc, bs, "0");
        } else {
            // Tenta parsing individual (border-width, border-color, border-style)
            String bw = allStyles.getOrDefault("border-width", "0");
            String bc = allStyles.getOrDefault("border-color", "black");
            String bs = allStyles.getOrDefault("border-style", "none");
            resolved.border = Border.parse(bw, bc, bs, "0");
        }

        // BorderRadius
        String brStr = allStyles.get("border-radius");
        resolved.borderRadius = brStr != null ? BorderRadius.parse(brStr) : BorderRadius.zero();

        // BoxShadow
        String shadowStr = allStyles.get("box-shadow");
        if (shadowStr != null) {
            resolved.boxShadows = BoxShadow.parseList(shadowStr);
        }

        // Gradientes (background)
        String bgStr = allStyles.get("background");
        if (bgStr != null) {
            // Se tiver gradiente, parseia; senão, é cor sólida
            if (bgStr.contains("gradient")) {
                resolved.gradients = GradientParser.parseGradients(bgStr);
            } else {
                resolved.backgroundColor = XplColor.resolveColor(bgStr);
            }
        }

        // Cor do texto
        String colorStr = allStyles.get("color");
        if (colorStr != null) {
            resolved.textColor = XplColor.resolveColor(colorStr);
        }

        // Font
        resolved.fontFamily = allStyles.get("font-family");
        if (allStyles.containsKey("font-size")) {
            resolved.fontSize = CssValue.parse(allStyles.get("font-size"));
        }
        if (allStyles.containsKey("font-weight")) {
            resolved.fontWeight = allStyles.get("font-weight");
        }

        // Display
        resolved.display = allStyles.getOrDefault("display", "block");

        return resolved;
    }

    private static BoxSize parseBoxSize(Map<String, String> styles) {
        Dimension w = parseDimension(styles.get("width"));
        Dimension h = parseDimension(styles.get("height"));
        Dimension minW = parseDimension(styles.get("min-width"));
        Dimension maxW = parseDimension(styles.get("max-width"));
        Dimension minH = parseDimension(styles.get("min-height"));
        Dimension maxH = parseDimension(styles.get("max-height"));
        return new BoxSize(w, h, minW, maxW, minH, maxH);
    }

    private static Dimension parseDimension(String str) {
        return str != null ? Dimension.parse(str) : Dimension.none();
    }
}