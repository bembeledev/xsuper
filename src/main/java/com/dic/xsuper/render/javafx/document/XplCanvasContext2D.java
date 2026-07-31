package com.dic.xsuper.render.javafx.document;

import com.dic.xsuper.dom.node.XplNativeObject;
import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.ast.Stmt;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.core.Token;
import com.dic.xsuper.engine.core.TokenType;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;
import com.dic.xsuper.engine.poo.XplInstance;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.Collections;
import java.util.List;

/**
 * Motor de Desenho Nativo (Ponte entre XPL e JavaFX GraphicsContext)
 */
public class XplCanvasContext2D extends XplInstance implements XplNativeObject {

    private final GraphicsContext gc;

    public XplCanvasContext2D(GraphicsContext gc, XplClass klass) {
        this.klass = klass;
        this.gc = gc;
        this.invokeMethod(); // Injeta os métodos na instância viva!
    }

    // =========================================================================
    // 🎨 COMANDOS DE PINTURA (O embrulho para a linguagem XPL)
    // =========================================================================
    @Override
    public void invokeMethod() {

        this.fields.put("setFillStyle", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String color = intp.evaluate(args.getFirst().expression).toString();
                try { gc.setFill(Color.web(color)); } catch (Exception ignored) {}
                return null;
            }
        });

        this.fields.put("setStrokeStyle", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String color = intp.evaluate(args.getFirst().expression).toString();
                try { gc.setStroke(Color.web(color)); } catch (Exception ignored) {}
                return null;
            }
        });

        this.fields.put("setFont", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String fontStyle = intp.evaluate(args.getFirst().expression).toString();
                try {
                    String[] parts = fontStyle.split("px ");
                    double size = Double.parseDouble(parts[0]);
                    String family = parts[1].replace("'", "").replace("\"", "");
                    gc.setFont(Font.font(family, size));
                } catch (Exception e) {
                    gc.setFont(Font.getDefault());
                }
                return null;
            }
        });

        this.fields.put("fillRect", new XplCallable() {
            @Override public int arity() { return 4; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                double x = parseDouble(intp.evaluate(args.get(0).expression));
                double y = parseDouble(intp.evaluate(args.get(1).expression));
                double w = parseDouble(intp.evaluate(args.get(2).expression));
                double h = parseDouble(intp.evaluate(args.get(3).expression));
                gc.fillRect(x, y, w, h);
                return null;
            }
        });

        this.fields.put("strokeRect", new XplCallable() {
            @Override public int arity() { return 4; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                double x = parseDouble(intp.evaluate(args.get(0).expression));
                double y = parseDouble(intp.evaluate(args.get(1).expression));
                double w = parseDouble(intp.evaluate(args.get(2).expression));
                double h = parseDouble(intp.evaluate(args.get(3).expression));
                gc.strokeRect(x, y, w, h);
                return null;
            }
        });

        this.fields.put("clearRect", new XplCallable() {
            @Override public int arity() { return 4; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                double x = parseDouble(intp.evaluate(args.get(0).expression));
                double y = parseDouble(intp.evaluate(args.get(1).expression));
                double w = parseDouble(intp.evaluate(args.get(2).expression));
                double h = parseDouble(intp.evaluate(args.get(3).expression));
                gc.clearRect(x, y, w, h);
                return null;
            }
        });

        this.fields.put("fillText", new XplCallable() {
            @Override public int arity() { return 3; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String text = intp.evaluate(args.get(0).expression).toString();
                double x = parseDouble(intp.evaluate(args.get(1).expression));
                double y = parseDouble(intp.evaluate(args.get(2).expression));
                gc.fillText(text, x, y);
                return null;
            }
        });

        this.fields.put("beginPath", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) { gc.beginPath(); return null; }
        });

        this.fields.put("closePath", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) { gc.closePath(); return null; }
        });

        this.fields.put("stroke", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) { gc.stroke(); return null; }
        });

        this.fields.put("fill", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) { gc.fill(); return null; }
        });

        this.fields.put("moveTo", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                double x = parseDouble(intp.evaluate(args.get(0).expression));
                double y = parseDouble(intp.evaluate(args.get(1).expression));
                gc.moveTo(x, y); return null;
            }
        });

        this.fields.put("lineTo", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                double x = parseDouble(intp.evaluate(args.get(0).expression));
                double y = parseDouble(intp.evaluate(args.get(1).expression));
                gc.lineTo(x, y); return null;
            }
        });

        this.fields.put("setLineWidth", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, java.util.List<Expr.CallArg> args) {
                double w = parseDouble(intp.evaluate(args.getFirst().expression));
                gc.setLineWidth(w);
                return null;
            }
        });

        this.fields.put("setLineCap", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, java.util.List<Expr.CallArg> args) {
                String cap = intp.evaluate(args.getFirst().expression).toString();
                if (cap.equals("round")) gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
                else if (cap.equals("square")) gc.setLineCap(javafx.scene.shape.StrokeLineCap.SQUARE);
                else gc.setLineCap(javafx.scene.shape.StrokeLineCap.BUTT);
                return null;
            }
        });
    }

    private double parseDouble(Object val) {
        if (val == null) return 0.0;
        try { return Double.parseDouble(val.toString()); } catch (NumberFormatException e) { return 0.0; }
    }

    @Override
    public Object getProperty(String propertyName) {
        if (propertyName.equals("name")) return "CanvasRenderingContext2D";
        return null;
    }

    // =========================================================================
    // ⭐ INTEGRAÇÃO COM A MÁQUINA VIRTUAL XPL (O Modelo Ast Estático)
    // =========================================================================
    public static XPLModel nativeModel;

    public static XPLModel buildNativeModel() {
        if (nativeModel == null) {
            nativeModel = new XPLModel("CanvasRenderingContext2D", null);

            // Helper para injectar a assinatura das funções sem gerar excepções semânticas!
            addVoidMethod(nativeModel, "beginPath", 0);
            addVoidMethod(nativeModel, "closePath", 0);
            addVoidMethod(nativeModel, "stroke", 0);
            addVoidMethod(nativeModel, "fill", 0);
            addVoidMethod(nativeModel, "setFillStyle", 1);
            addVoidMethod(nativeModel, "setStrokeStyle", 1);
            addVoidMethod(nativeModel, "setFont", 1);
            addVoidMethod(nativeModel, "moveTo", 2);
            addVoidMethod(nativeModel, "lineTo", 2);
            addVoidMethod(nativeModel, "fillText", 3);
            addVoidMethod(nativeModel, "fillRect", 4);
            addVoidMethod(nativeModel, "strokeRect", 4);
            addVoidMethod(nativeModel, "clearRect", 4);
        }
        return nativeModel;
    }

    private static void addVoidMethod(XPLModel model, String name, int paramsCount) {
        java.util.List<Stmt.Param> params = new java.util.ArrayList<>();
        for(int i = 0; i < paramsCount; i++) {
            params.add(new Stmt.Param(new Token(TokenType.IDENTIFIER, "arg" + i, null, 0, 0), null, null,false));
        }

        model.addMethod(new Stmt.Function(
                null, false, false,
                new Token(TokenType.IDENTIFIER, name, null, 0, 0),
                params, null, null,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
        ));
    }
}