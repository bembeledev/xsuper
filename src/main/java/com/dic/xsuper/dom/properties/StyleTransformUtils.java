package com.dic.xsuper.dom.properties;


import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.transform.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitário para aplicar transformações CSS (2D/3D) a nós JavaFX.
 * <p>
 * Suporta propriedades:
 * - translate-x, translate-y, translate-z, translate
 * - rotate, rotate-x, rotate-y, rotate-z, rotate3d
 * - scale, scale-x, scale-y, scale-z
 * - skew, skew-x, skew-y
 * - matrix, matrix3d
 * - transform (string unificada: "translate(10px) rotate(45deg)")
 * - transform-origin (center, top, left, etc. ou valores)
 * - perspective (para o pai)
 * - transform-style (preserve-3d / flat)
 * - backface-visibility (visible / hidden)
 */
public class StyleTransformUtils {

    // ─── Contexto para resolução de unidades relativas ──────────────────────

    public static class TransformContext {
        public double nodeWidth = 0;
        public double nodeHeight = 0;
        public double parentWidth = 0;
        public double parentHeight = 0;
        public double fontSize = 16.0;      // 1em
        public double rootFontSize = 16.0;  // 1rem
        public double viewportWidth = 1920.0;
        public double viewportHeight = 1080.0;

        public TransformContext() {}

        public TransformContext withNodeSize(double w, double h) {
            this.nodeWidth = w; this.nodeHeight = h; return this;
        }
        public TransformContext withParentSize(double w, double h) {
            this.parentWidth = w; this.parentHeight = h; return this;
        }
        public TransformContext withFontSize(double fs) {
            this.fontSize = fs; return this;
        }
        public TransformContext withViewport(double vw, double vh) {
            this.viewportWidth = vw; this.viewportHeight = vh; return this;
        }
    }

    private static final TransformContext DEFAULT_CONTEXT = new TransformContext();

    // ─── Aplicação principal ─────────────────────────────────────────────────

    public static void applyTransforms(Node fxNode, Map<String, Object> style) {
        applyTransforms(fxNode, style, DEFAULT_CONTEXT);
    }

    public static void applyTransforms(Node fxNode, Map<String, Object> style, TransformContext ctx) {
        if (fxNode == null || style == null || style.isEmpty()) return;

        // 1. Limpa transformações anteriores (para evitar acumulação)
        fxNode.getTransforms().clear();
        fxNode.setTranslateX(0);
        fxNode.setTranslateY(0);
        fxNode.setTranslateZ(0);
        fxNode.setRotate(0);
        fxNode.setScaleX(1);
        fxNode.setScaleY(1);
        fxNode.setScaleZ(1);

        // 2. Transform-origin (pivot)
        Point3D origin = parseTransformOrigin(style, ctx);
        double pivotX = origin.getX();
        double pivotY = origin.getY();
        double pivotZ = origin.getZ();

        // 3. Parsing da string 'transform' (unificada)
        if (style.containsKey("transform")) {
            parseAndApplyTransformString(fxNode, style.get("transform").toString(), pivotX, pivotY, pivotZ, ctx);
        }

        // 4. Propriedades individuais (têm precedência sobre a string transform)
        applyIndividualProperties(fxNode, style, pivotX, pivotY, pivotZ, ctx);

        // 5. Propriedades 3D adicionais
        if (style.containsKey("transform-style") && "preserve-3d".equals(style.get("transform-style").toString())) {
            fxNode.setDepthTest(javafx.scene.DepthTest.ENABLE);
        }

        if (style.containsKey("backface-visibility")) {
            String val = style.get("backface-visibility").toString();
            boolean visible = !"hidden".equalsIgnoreCase(val);
            fxNode.setNodeOrientation(javafx.geometry.NodeOrientation.LEFT_TO_RIGHT);
            // No JavaFX, backface visibility é controlada via CacheHint ou 3D transforms
            // Não há método direto; aplicamos via CSS ou usando um efeito.
            // Para simplificar, usamos setCache(true) e setCacheHint(CacheHint.ROTATE)
            if (!visible) {
                fxNode.setCache(true);
                fxNode.setCacheHint(javafx.scene.CacheHint.ROTATE);
            }
        }

        // 6. Perspectiva (aplicada ao pai ou ao próprio nó)
        if (style.containsKey("perspective")) {
            double perspective = parseLength(style.get("perspective").toString(), ctx);
            // JavaFX: podemos criar um efeito de perspectiva usando um transform
            // ou configurar no pai. Para simplificar, aplicamos ao nó.
            if (perspective > 0) {
                // Não há transform direto; usamos um PerspectiveTransform (efeito)
                // Ou aplicamos uma rotação 3D com perspectiva via scene3D.
                // Vamos ignorar por enquanto; o usuário pode usar rotateX/Y.
            }
        }

        // 7. Opacidade (pode ser aplicada aqui também)
        if (style.containsKey("opacity")) {
            double opacity = parseNumber(style.get("opacity").toString());
            fxNode.setOpacity(Math.max(0, Math.min(1, opacity)));
        }
    }

    // ─── Aplicação de propriedades individuais ──────────────────────────────

    private static void applyIndividualProperties(Node fxNode, Map<String, Object> style,
                                                  double pivotX, double pivotY, double pivotZ,
                                                  TransformContext ctx) {
        List<Transform> transforms = new ArrayList<>();

        // Translate
        if (style.containsKey("translate-x") || style.containsKey("translateX") ||
                style.containsKey("translate-y") || style.containsKey("translateY") ||
                style.containsKey("translate-z") || style.containsKey("translateZ") ||
                style.containsKey("translate")) {

            double tx = 0, ty = 0, tz = 0;
            Object txObj = style.getOrDefault("translate-x", style.get("translateX"));
            Object tyObj = style.getOrDefault("translate-y", style.get("translateY"));
            Object tzObj = style.getOrDefault("translate-z", style.get("translateZ"));
            Object tObj = style.get("translate");

            if (txObj != null) tx = parseLength(txObj.toString(), ctx);
            if (tyObj != null) ty = parseLength(tyObj.toString(), ctx);
            if (tzObj != null) tz = parseLength(tzObj.toString(), ctx);
            if (tObj != null) {
                // translate pode ter um ou dois valores
                String[] parts = tObj.toString().trim().split("\\s+");
                if (parts.length >= 1) tx = parseLength(parts[0], ctx);
                if (parts.length >= 2) ty = parseLength(parts[1], ctx);
                if (parts.length >= 3) tz = parseLength(parts[2], ctx);
            }
            transforms.add(new Translate(tx, ty, tz));
        }

        // Rotate (individual)
        if (style.containsKey("rotate")) {
            double angle = parseAngle(style.get("rotate").toString());
            transforms.add(new Rotate(angle, pivotX, pivotY, pivotZ));
        }
        if (style.containsKey("rotate-x") || style.containsKey("rotateX")) {
            double angle = parseAngle(style.getOrDefault("rotate-x", style.get("rotateX")).toString());
            transforms.add(new Rotate(angle, pivotX, pivotY, pivotZ, Rotate.X_AXIS));
        }
        if (style.containsKey("rotate-y") || style.containsKey("rotateY")) {
            double angle = parseAngle(style.getOrDefault("rotate-y", style.get("rotateY")).toString());
            transforms.add(new Rotate(angle, pivotX, pivotY, pivotZ, Rotate.Y_AXIS));
        }
        if (style.containsKey("rotate-z") || style.containsKey("rotateZ")) {
            double angle = parseAngle(style.getOrDefault("rotate-z", style.get("rotateZ")).toString());
            transforms.add(new Rotate(angle, pivotX, pivotY, pivotZ, Rotate.Z_AXIS));
        }
        if (style.containsKey("rotate3d")) {
            String[] parts = style.get("rotate3d").toString().trim().split("\\s+");
            if (parts.length == 4) {
                double x = parseNumber(parts[0]);
                double y = parseNumber(parts[1]);
                double z = parseNumber(parts[2]);
                double angle = parseAngle(parts[3]);
                transforms.add(new Rotate(angle, pivotX, pivotY, pivotZ, new Point3D(x, y, z)));
            }
        }

        // Scale
        if (style.containsKey("scale")) {
            double s = parseNumber(style.get("scale").toString());
            transforms.add(new Scale(s, s, s, pivotX, pivotY, pivotZ));
        }
        if (style.containsKey("scale-x") || style.containsKey("scaleX")) {
            double sx = parseNumber(style.getOrDefault("scale-x", style.get("scaleX")).toString());
            transforms.add(new Scale(sx, 1, 1, pivotX, pivotY, pivotZ));
        }
        if (style.containsKey("scale-y") || style.containsKey("scaleY")) {
            double sy = parseNumber(style.getOrDefault("scale-y", style.get("scaleY")).toString());
            transforms.add(new Scale(1, sy, 1, pivotX, pivotY, pivotZ));
        }
        if (style.containsKey("scale-z") || style.containsKey("scaleZ")) {
            double sz = parseNumber(style.getOrDefault("scale-z", style.get("scaleZ")).toString());
            transforms.add(new Scale(1, 1, sz, pivotX, pivotY, pivotZ));
        }

        // Skew
        if (style.containsKey("skew")) {
            // skew(x, y)
            String[] parts = style.get("skew").toString().trim().split("\\s+");
            double sx = 0, sy = 0;
            if (parts.length >= 1) sx = parseAngle(parts[0]);
            if (parts.length >= 2) sy = parseAngle(parts[1]);
            transforms.add(new Shear(Math.tan(Math.toRadians(sx)), Math.tan(Math.toRadians(sy)), pivotX, pivotY));
        }
        if (style.containsKey("skew-x") || style.containsKey("skewX")) {
            double sx = parseAngle(style.getOrDefault("skew-x", style.get("skewX")).toString());
            transforms.add(new Shear(Math.tan(Math.toRadians(sx)), 0, pivotX, pivotY));
        }
        if (style.containsKey("skew-y") || style.containsKey("skewY")) {
            double sy = parseAngle(style.getOrDefault("skew-y", style.get("skewY")).toString());
            transforms.add(new Shear(0, Math.tan(Math.toRadians(sy)), pivotX, pivotY));
        }

        // Matrix (2D)
        if (style.containsKey("matrix")) {
            String[] parts = style.get("matrix").toString().trim().split("\\s+");
            if (parts.length == 6) {
                double m00 = parseNumber(parts[0]);
                double m01 = parseNumber(parts[1]);
                double m10 = parseNumber(parts[2]);
                double m11 = parseNumber(parts[3]);
                double m02 = parseNumber(parts[4]);
                double m12 = parseNumber(parts[5]);
                transforms.add(new Affine(m00, m01, 0, m02, m10, m11, 0, m12, 0, 0, 1, 0));
            }
        }

        // Matrix 3D
        if (style.containsKey("matrix3d")) {
            String[] parts = style.get("matrix3d").toString().trim().split("\\s+");
            if (parts.length == 16) {
                double[] m = new double[16];
                for (int i = 0; i < 16; i++) m[i] = parseNumber(parts[i]);
                Affine affine = new Affine();
                affine.setMxx(m[0]); affine.setMxy(m[1]); affine.setMxz(m[2]); affine.setTx(m[3]);  // Mudou setMxt -> setTx
                affine.setMyx(m[4]); affine.setMyy(m[5]); affine.setMyz(m[6]); affine.setTy(m[7]);  // Mudou setMyt -> setTy
                affine.setMzx(m[8]); affine.setMzy(m[9]); affine.setMzz(m[10]); affine.setTz(m[11]); // Mudou setMzt -> setTz
                // As últimas 4 linhas são para homografia, ignoramos no JavaFX.
                transforms.add(affine);
            }
        }

        // Aplica todas as transformações
        if (!transforms.isEmpty()) {
            fxNode.getTransforms().addAll(transforms);
        }
    }

    // ─── Parsing da string 'transform' ──────────────────────────────────────

    private static void parseAndApplyTransformString(Node fxNode, String transformStr,
                                                     double pivotX, double pivotY, double pivotZ,
                                                     TransformContext ctx) {
        Pattern pattern = Pattern.compile("([a-zA-Z0-9]+)\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(transformStr);
        List<Transform> transforms = new ArrayList<>();

        while (matcher.find()) {
            String func = matcher.group(1).toLowerCase();
            String[] args = matcher.group(2).split(",");
            for (int i = 0; i < args.length; i++) args[i] = args[i].trim();

            switch (func) {
                case "translate", "translate3d" -> {
                    double tx = args.length > 0 ? parseLength(args[0], ctx) : 0;
                    double ty = args.length > 1 ? parseLength(args[1], ctx) : 0;
                    double tz = args.length > 2 ? parseLength(args[2], ctx) : 0;
                    transforms.add(new Translate(tx, ty, tz));
                }
                case "translatex" -> transforms.add(new Translate(parseLength(args[0], ctx), 0, 0));
                case "translatey" -> transforms.add(new Translate(0, parseLength(args[0], ctx), 0));
                case "translatez" -> transforms.add(new Translate(0, 0, parseLength(args[0], ctx)));
                case "rotate" -> {
                    double angle = parseAngle(args[0]);
                    transforms.add(new Rotate(angle, pivotX, pivotY, pivotZ));
                }
                case "rotatex" -> transforms.add(new Rotate(parseAngle(args[0]), pivotX, pivotY, pivotZ, Rotate.X_AXIS));
                case "rotatey" -> transforms.add(new Rotate(parseAngle(args[0]), pivotX, pivotY, pivotZ, Rotate.Y_AXIS));
                case "rotatez" -> transforms.add(new Rotate(parseAngle(args[0]), pivotX, pivotY, pivotZ, Rotate.Z_AXIS));
                case "rotate3d" -> {
                    if (args.length == 4) {
                        double x = parseNumber(args[0]);
                        double y = parseNumber(args[1]);
                        double z = parseNumber(args[2]);
                        double angle = parseAngle(args[3]);
                        transforms.add(new Rotate(angle, pivotX, pivotY, pivotZ, new Point3D(x, y, z)));
                    }
                }
                case "scale" -> {
                    double s = parseNumber(args[0]);
                    transforms.add(new Scale(s, s, 1, pivotX, pivotY, pivotZ));
                }
                case "scale3d" -> {
                    double sx = args.length > 0 ? parseNumber(args[0]) : 1;
                    double sy = args.length > 1 ? parseNumber(args[1]) : 1;
                    double sz = args.length > 2 ? parseNumber(args[2]) : 1;
                    transforms.add(new Scale(sx, sy, sz, pivotX, pivotY, pivotZ));
                }
                case "scalex" -> transforms.add(new Scale(parseNumber(args[0]), 1, 1, pivotX, pivotY, pivotZ));
                case "scaley" -> transforms.add(new Scale(1, parseNumber(args[0]), 1, pivotX, pivotY, pivotZ));
                case "scalez" -> transforms.add(new Scale(1, 1, parseNumber(args[0]), pivotX, pivotY, pivotZ));
                case "skew" -> {
                    double sx = args.length > 0 ? parseAngle(args[0]) : 0;
                    double sy = args.length > 1 ? parseAngle(args[1]) : 0;
                    transforms.add(new Shear(Math.tan(Math.toRadians(sx)), Math.tan(Math.toRadians(sy)), pivotX, pivotY));
                }
                case "skewx" -> {
                    double sx = parseAngle(args[0]);
                    transforms.add(new Shear(Math.tan(Math.toRadians(sx)), 0, pivotX, pivotY));
                }
                case "skewy" -> {
                    double sy = parseAngle(args[0]);
                    transforms.add(new Shear(0, Math.tan(Math.toRadians(sy)), pivotX, pivotY));
                }
                case "matrix" -> {
                    if (args.length == 6) {
                        double m00 = parseNumber(args[0]);
                        double m01 = parseNumber(args[1]);
                        double m10 = parseNumber(args[2]);
                        double m11 = parseNumber(args[3]);
                        double m02 = parseNumber(args[4]);
                        double m12 = parseNumber(args[5]);
                        transforms.add(new Affine(m00, m01, 0, m02, m10, m11, 0, m12, 0, 0, 1, 0));
                    }
                }
                case "matrix3d" -> {
                    if (args.length == 16) {
                        double[] m = new double[16];
                        for (int i = 0; i < 16; i++) m[i] = parseNumber(args[i]);
                        Affine affine = new Affine();
                        affine.setMxx(m[0]); affine.setMxy(m[1]); affine.setMxz(m[2]); affine.setTx(m[3]);
                        affine.setMyx(m[4]); affine.setMyy(m[5]); affine.setMyz(m[6]); affine.setTy(m[7]);
                        affine.setMzx(m[8]); affine.setMzy(m[9]); affine.setMzz(m[10]); affine.setTz(m[11]);
                        transforms.add(affine);
                    }
                }
                default -> {
                    // Função não suportada
                }
            }
        }

        if (!transforms.isEmpty()) {
            fxNode.getTransforms().addAll(transforms);
        }
    }

    // ─── Transform-origin ──────────────────────────────────────────────────

    private static Point3D parseTransformOrigin(Map<String, Object> style, TransformContext ctx) {
        if (!style.containsKey("transform-origin")) {
            return new Point3D(0, 0, 0); // centro do nó (relativo a translate/rotate)
        }
        String val = style.get("transform-origin").toString().trim().toLowerCase();
        String[] parts = val.split("\\s+");
        double x = 0, y = 0, z = 0;

        // Palavras‑chave: center, top, left, bottom, right
        if (parts.length == 1) {
            switch (parts[0]) {
                case "center": x = 50; y = 50; break;
                case "top": y = 0; break;
                case "bottom": y = 100; break;
                case "left": x = 0; break;
                case "right": x = 100; break;
                default: // valor numérico
                    if (parts[0].endsWith("%")) {
                        double pct = parseNumber(parts[0].replace("%", ""));
                        x = pct;
                        y = pct;
                    } else {
                        x = parseLength(parts[0], ctx);
                        y = parseLength(parts[0], ctx);
                    }
            }
        } else if (parts.length == 2) {
            // x y
            x = parseOriginValue(parts[0], ctx);
            y = parseOriginValue(parts[1], ctx);
        } else if (parts.length == 3) {
            x = parseOriginValue(parts[0], ctx);
            y = parseOriginValue(parts[1], ctx);
            z = parseLength(parts[2], ctx);
        }

        // Converte percentagem para pixel (relativo ao tamanho do nó)
        if (style.containsKey("width") && x > 0 && x < 100) {
            double w = ctx.nodeWidth > 0 ? ctx.nodeWidth : 100;
            x = x / 100.0 * w;
        }
        if (style.containsKey("height") && y > 0 && y < 100) {
            double h = ctx.nodeHeight > 0 ? ctx.nodeHeight : 100;
            y = y / 100.0 * h;
        }

        return new Point3D(x, y, z);
    }

    private static double parseOriginValue(String token, TransformContext ctx) {
        token = token.trim().toLowerCase();
        return switch (token) {
            case "center" -> 50.0;
            case "top", "left" -> 0.0;
            case "bottom", "right" -> 100.0;
            default -> parseLength(token, ctx);
        };
    }

    // ─── Parsing de unidades ─────────────────────────────────────────────────

    private static double parseLength(String val, TransformContext ctx) {
        if (val == null) return 0;
        val = val.trim().toLowerCase();

        // Percentagem
        if (val.endsWith("%")) {
            double pct = parseNumber(val.replace("%", ""));
            return (ctx.nodeWidth > 0) ? pct / 100.0 * ctx.nodeWidth : pct;
        }

        // Unidades
        if (val.endsWith("em")) {
            double num = parseNumber(val.replace("em", ""));
            return num * ctx.fontSize;
        }
        if (val.endsWith("rem")) {
            double num = parseNumber(val.replace("rem", ""));
            return num * ctx.rootFontSize;
        }
        if (val.endsWith("vh")) {
            double num = parseNumber(val.replace("vh", ""));
            return num / 100.0 * ctx.viewportHeight;
        }
        if (val.endsWith("vw")) {
            double num = parseNumber(val.replace("vw", ""));
            return num / 100.0 * ctx.viewportWidth;
        }
        if (val.endsWith("px") || val.endsWith("pt") || val.endsWith("pc") || val.endsWith("in") || val.endsWith("cm") || val.endsWith("mm")) {
            // Remove unidade e parseia
            String numStr = val.replaceAll("[a-zA-Z]+$", "").trim();
            return parseNumber(numStr);
        }
        // Número puro (assumimos pixels)
        return parseNumber(val);
    }

    private static double parseAngle(String val) {
        if (val == null) return 0;
        val = val.trim().toLowerCase().replace("deg", "").replace("rad", "").replace("grad", "").replace("turn", "");
        try {
            double num = Double.parseDouble(val);
            // Se for turn, converte para graus (1 turn = 360deg)
            if (val.contains("turn")) return num * 360;
            return num;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double parseNumber(String val) {
        if (val == null) return 0;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}