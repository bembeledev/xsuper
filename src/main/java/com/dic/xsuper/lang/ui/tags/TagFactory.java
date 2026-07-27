package com.dic.xsuper.lang.ui.tags;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.chart.*;
import com.dic.xsuper.lang.ui.tags.controls.*;
import com.dic.xsuper.lang.ui.tags.controls.date.DateTimeLocalInputTag;
import com.dic.xsuper.lang.ui.tags.controls.date.MonthInputTag;
import com.dic.xsuper.lang.ui.tags.controls.date.TimeInputTag;
import com.dic.xsuper.lang.ui.tags.controls.date.WeekInputTag;
import com.dic.xsuper.lang.ui.tags.controls.select.SelectTag;
import com.dic.xsuper.lang.ui.tags.editor.EditorTag;
import com.dic.xsuper.lang.ui.tags.game.*;
import com.dic.xsuper.lang.ui.tags.interactive.*;
import com.dic.xsuper.lang.ui.tags.list.LiTag;
import com.dic.xsuper.lang.ui.tags.list.OlTag;

import com.dic.xsuper.lang.ui.tags.list.UlTag;
import com.dic.xsuper.lang.ui.tags.media.AudioTag;
import com.dic.xsuper.lang.ui.tags.media.VideoTag;
import com.dic.xsuper.lang.ui.tags.navigation.*;
import com.dic.xsuper.lang.ui.tags.shapes.*;
import com.dic.xsuper.lang.ui.tags.table.TableCellTag;
import com.dic.xsuper.lang.ui.tags.table.TableGroupTag;
import com.dic.xsuper.lang.ui.tags.table.TableRowTag;
import com.dic.xsuper.lang.ui.tags.table.TableTag;
import com.dic.xsuper.lang.ui.tags.texts.*;

public class TagFactory {

    public static NativeTag create(XplNode node) {
        if (node == null) return null;
        String tag = node.tag.toLowerCase();

        return switch (tag) {
            // ⭐ Contentores W3C Universais (Agora incluem ul, li e form)
            case "div", "main", "section", "article", "header", "footer", "aside" -> new ContainerTag(node);

            // ⭐ TIPOGRAFIA E TEXTOS W3C DESCENTRALIZADOS
            case "p" -> new ParagraphTag(node);
            case "h1", "h2", "h3", "h4", "h5", "h6" -> new HeadingTag(node);
            case "span", "label" -> new SpanTag(node);

            // Junta o small e o mark aqui:
            case "b", "strong", "i", "em", "u", "s", "strike", "small", "mark" -> new FormattingTag(node);

            case "#text" -> new TextNodeTag(node);
            // ⭐ Tabelas
            case "table" -> new TableTag(node);
            case "thead", "tbody", "tfoot" -> new TableGroupTag(node);
            case "tr" -> new TableRowTag(node);
            case "td", "th" -> new TableCellTag(node);

            case "ul" -> new UlTag(node);
            case "ol" -> new OlTag(node);
            case "li" -> new LiTag(node);

            // Adiciona "tabs" para chamar a classe nova:
            case "tabs" -> new TabsTag(node);
            case "tab" -> new TabTag(node);

            case "audio" -> new AudioTag(node);
            case "video" -> new VideoTag(node);

            case "details" -> new DetailsTag(node);
            case "summary" -> new SummaryTag(node);

            // Outros componentes
            case "button" -> new ButtonTag(node);
            // Dentro do teu TagFactory.java

            case "input"-> {
                String type = (String) node.attributes.getOrDefault("type", "text");
                yield switch (type) {
                    // Especializados
                    case "checkbox" -> new CheckboxInputTag(node);
                    case "radio" -> new RadioInputTag(node);
                    case "range" -> new RangeInputTag(node);
                    case "file" -> new FileInputTag(node);
                    // Tipos de texto
                    case "text" -> new TextInputTag(node);
                    case "password" -> new PasswordInputTag(node);
                    case "number" -> new NumberInputTag(node);
                    case "email" -> new EmailInputTag(node);
                    case "tel" -> new TelInputTag(node);
                    case "url" -> new UrlInputTag(node);
                    case "search" -> new SearchInputTag(node);
                    case "color" -> new ColorInputTag(node);
                    // Botões
                    case "submit" -> new SubmitInputTag(node);
                    case "reset" -> new ResetInputTag(node);
                    case "button" -> new ButtonInputTag(node);
                    case "image" -> new ImageInputTag(node);
                    // Data/Hora
                    case "datetime-local" -> new DateTimeLocalInputTag(node);
                    case "time" -> new TimeInputTag(node);
                    case "month" -> new MonthInputTag(node);
                    case "week" -> new WeekInputTag(node);
                    // Fallback genérico
                    default -> new TextInputTag(node);
                };
            }

            case "textarea" -> new TextareaTag(node);
            case "a" -> new AnchorTag(node);
            case "img" -> new ImageTag(node);
            case "select" -> new SelectTag(node);

            case "figure" -> new FigureTag(node);
            case "figcaption" -> new FigcaptionTag(node);
            case "web", "iframe" -> new WebTag(node); // Funciona como iframe!
            // No TagFactory.java:
            case "form" -> new FormTag(node);

            case "nav" -> new NavTag(node);
            case "menu" -> new MenuTag(node);
            case "menuitem" -> new MenuItemTag(node);
            case "menubar" -> new MenubarTag(node);
            case "contextmenu" -> new ContextMenuTag(node);

            // Scripting
            case "canvas" -> new CanvasTag(node); // ⭐ INJETADO AQUI
            // ─── Contentores Vetoriais (SVG) ───
            case "svg" -> new SvgTag(node);

            // ─── Shapes W3C ───
            case "path" -> new PathTag(node);
            case "circle" -> new CircleTag(node);
            case "rect" -> new RectangleTag(node);
            case "line" -> new LineTag(node);
            case "polygon" -> new PolygonTag(node);
            case "polyline" -> new PolylineTag(node);
            case "ellipse" -> new EllipseTag(node);
            case "arc" -> new ArcTag(node);
            case "quadcurve" -> new QuadCurveTag(node);
            case "cubiccurve" -> new CubicCurveTag(node);
            case "content" -> new TextTag(node);
            case "chart" -> {
                String type = node.attributes.getOrDefault("type", "line").toString().toLowerCase();
                yield switch (type) {
                    // ─── MOTORES JAVAFX ORIGINAIS ───
                    case "bar" -> new BarChartTag(node);
                    case "area" -> new AreaChartTag(node);
                    case "pie" -> new PieChartTag(node);
                    case "scatter" -> new ScatterChartTag(node);
                    case "bubble" -> new BubbleChartTag(node);
                    case "stacked-bar" -> new StackedBarChartTag(node);
                    case "stacked-area" -> new StackedAreaChartTag(node);
                    case "math" -> new MathChartTag(node);

                    // ─── MOTORES HANSOLO (Nova Geração) ───
                    case "donut" -> new CircularChartTag(node);
                    case "xy" -> new XYChartTag(node); // Delega para o motor Hansolo XY

                    default -> new LineChartTag(node);
                };
            }

            // Em TagFactory.java
            case "popup", "tooltip" -> new PopupTag(node);

            case "hr" -> new HrTag(node);
            case "dialog" -> new DialogTag(node);


            // ─── TAGS DE JOGO ──────────────────────────────────────────────────
            case "game" -> new GameTag(node);
            case "physics" -> new PhysicsTag(node);
            case "level" -> new LevelTag(node);
            case "camera" -> new CameraTag(node);
            case "entity" -> new EntityTag(node);
            case "player" -> new PlayerTag(node);
            case "enemy" -> new EnemyTag(node);
            case "powerup" -> new PowerupTag(node);
            case "projectile" -> new ProjectileTag(node);
            case "spawner" -> new SpawnerTag(node);
            case "trigger" -> new TriggerTag(node);
            case "animation" -> new AnimationTag(node);
            case "editor" -> new EditorTag(node);

            default -> new ContainerTag(node);
        };
    }
}