package com.dic.xsuper.lang.ui.tags;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.controls.*;
import com.dic.xsuper.lang.ui.tags.controls.date.DateTimeLocalInputTag;
import com.dic.xsuper.lang.ui.tags.controls.date.MonthInputTag;
import com.dic.xsuper.lang.ui.tags.controls.date.TimeInputTag;
import com.dic.xsuper.lang.ui.tags.controls.date.WeekInputTag;
import com.dic.xsuper.lang.ui.tags.controls.select.SelectTag;
import com.dic.xsuper.lang.ui.tags.interactive.*;
import com.dic.xsuper.lang.ui.tags.list.LiTag;
import com.dic.xsuper.lang.ui.tags.list.OlTag;

import com.dic.xsuper.lang.ui.tags.list.UlTag;
import com.dic.xsuper.lang.ui.tags.media.AudioTag;
import com.dic.xsuper.lang.ui.tags.media.VideoTag;
import com.dic.xsuper.lang.ui.tags.table.TableCellTag;
import com.dic.xsuper.lang.ui.tags.table.TableGroupTag;
import com.dic.xsuper.lang.ui.tags.table.TableRowTag;
import com.dic.xsuper.lang.ui.tags.table.TableTag;

public class TagFactory {

    public static NativeTag create(XplNode node) {
        if (node == null) return null;
        String tag = node.tag.toLowerCase();

        return switch (tag) {
            // ⭐ Contentores W3C Universais (Agora incluem ul, li e form)
            case "div", "main", "section", "article", "header", "footer", "nav", "aside", "form" -> new ContainerTag(node);

            // ⭐ NÓS DE TEXTO PURO (Gerados pelo HtmlParser)
            case "h1", "h2", "h3", "h4", "h5", "h6", "p", "span", "label", "text", "#text", "b", "strong", "i", "em" -> new LabelTag(node);

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

            default -> new ContainerTag(node);
        };
    }
}