package com.dic.xsuper.lang.ui.document.helpers;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.ui.document.XplElement;
import com.dic.xsuper.lang.ui.document.XplEventListener;
import com.dic.xsuper.lang.ui.event.XplEvent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XplElementUtils {
    public static void invokeMethod(XplElement element) {

        // ─── EXPOR MÉTODOS NATIVOS PARA O XPL ────────────────────────────────

        // callAction (já existente)
        element.fields.put("callAction", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String eventName = intp.evaluate(args.getFirst().expression).toString();
                element.callAction(eventName, intp);
                return null;
            }
        });

        // setAttribute (já existente)
        element.fields.put("setAttribute", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String attrName = intp.evaluate(args.get(0).expression).toString();
                Object attrValue = intp.evaluate(args.get(1).expression);
                element.setAttribute(attrName, attrValue);
                return null;
            }
        });

// getAttribute
        element.fields.put("getAttribute", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String name = intp.evaluate(args.getFirst().expression).toString();
                return element.getAttribute(name);
            }
        });

// hasAttribute
        element.fields.put("hasAttribute", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String name = intp.evaluate(args.getFirst().expression).toString();
                return element.hasAttribute(name);
            }
        });

// removeAttribute
        element.fields.put("removeAttribute", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String name = intp.evaluate(args.getFirst().expression).toString();
                element.removeAttribute(name);
                return null;
            }
        });

// getAttributes
        element.fields.put("getAttributes", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return element.getAttributes();
            }
        });

// appendChild
        element.fields.put("appendChild", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplElement child = (XplElement) intp.evaluate(args.getFirst().expression);
                element.appendChild(child);
                return null;
            }
        });

        // insertBefore
        element.fields.put("insertBefore", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplElement newNode = (XplElement) intp.evaluate(args.get(0).expression);
                XplElement refNode = (XplElement) intp.evaluate(args.get(1).expression);
                element.insertBefore(newNode, refNode);
                return null;
            }
        });

// replaceChild
        element.fields.put("replaceChild", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplElement newNode = (XplElement) intp.evaluate(args.get(0).expression);
                XplElement oldNode = (XplElement) intp.evaluate(args.get(1).expression);
                element.replaceChild(newNode, oldNode);
                return null;
            }
        });

// removeChild
        element.fields.put("removeChild", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplElement child = (XplElement) intp.evaluate(args.getFirst().expression);
                element.removeChild(child);
                return null;
            }
        });

// remove
        element.fields.put("remove", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                element.remove();
                return null;
            }
        });

// cloneNode
        element.fields.put("cloneNode", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                boolean deep = (boolean) intp.evaluate(args.getFirst().expression);
                return element.cloneNode(deep);
            }
        });

// getElementById
        element.fields.put("getElementById", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String id = intp.evaluate(args.get(0).expression).toString();
                return element.getElementById(id);
            }
        });

// getElementsByClassName
        element.fields.put("getElementsByClassName", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String cls = intp.evaluate(args.getFirst().expression).toString();
                return element.getElementsByClassName(cls);
            }
        });

// getElementsByTagName
        element.fields.put("getElementsByTagName", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String tag = intp.evaluate(args.getFirst().expression).toString();
                return element.getElementsByTagName(tag);
            }
        });

// getElementsByName
        element.fields.put("getElementsByName", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String name = intp.evaluate(args.getFirst().expression).toString();
                return element.getElementsByName(name);
            }
        });

// querySelector
        element.fields.put("querySelector", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String selector = intp.evaluate(args.getFirst().expression).toString();
                return element.querySelector(selector);
            }
        });

// querySelectorAll
        element.fields.put("querySelectorAll", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String selector = intp.evaluate(args.getFirst().expression).toString();
                return element.querySelectorAll(selector);
            }
        });

// getClosest
        element.fields.put("getClosest", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String selector = intp.evaluate(args.getFirst().expression).toString();
                return element.getClosest(selector);
            }
        });

// matches
        element.fields.put("matches", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String selector = intp.evaluate(args.getFirst().expression).toString();
                return element.matches(selector);
            }
        });

// addClass
        element.fields.put("addClass", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String cls = intp.evaluate(args.getFirst().expression).toString();
                element.addClass(cls);
                return null;
            }
        });

// removeClass
        element.fields.put("removeClass", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String cls = intp.evaluate(args.getFirst().expression).toString();
                element.removeClass(cls);
                return null;
            }
        });

// hasClass
        element.fields.put("hasClass", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String cls = intp.evaluate(args.getFirst().expression).toString();
                return element.hasClass(cls);
            }
        });

// toggleClass
        element.fields.put("toggleClass", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String cls = intp.evaluate(args.getFirst().expression).toString();
                element.toggleClass(cls);
                return null;
            }
        });

// setStyle
        element.fields.put("setStyle", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String prop = intp.evaluate(args.get(0).expression).toString();
                String value = intp.evaluate(args.get(1).expression).toString();
                element.setStyle(prop, value);
                return null;
            }
        });

// getStyle
        element.fields.put("getStyle", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String prop = intp.evaluate(args.getFirst().expression).toString();
                return element.getStyle(prop);
            }
        });

// setInnerHTML
        element.fields.put("setInnerHTML", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String html = intp.evaluate(args.getFirst().expression).toString();
                element.setInnerHTML(html);
                return null;
            }
        });

// getInnerHTML
        element.fields.put("getInnerHTML", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return element.getInnerHTML();
            }
        });

// getOuterHTML
        element.fields.put("getOuterHTML", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return element.getOuterHTML();
            }
        });

// getTextContent
        element.fields.put("getTextContent", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return element.getTextContent();
            }
        });

// setTextContent
        element.fields.put("setTextContent", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String text = intp.evaluate(args.getFirst().expression).toString();
                element.setTextContent(text);
                return null;
            }
        });

// getInnerText
        element.fields.put("getInnerText", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return element.getInnerText();
            }
        });

// getOuterText
        element.fields.put("getOuterText", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return element.getOuterText();
            }
        });

// appendText
        element.fields.put("appendText", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String text = intp.evaluate(args.getFirst().expression).toString();
                element.appendText(text);
                return null;
            }
        });

// prependText
        element.fields.put("prependText", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String text = intp.evaluate(args.getFirst().expression).toString();
                element.prependText(text);
                return null;
            }
        });

// clear
        element.fields.put("clear", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                element. clear();
                return null;
            }
        });

// addEventListener (com sobrecarga)
        element.fields.put("addEventListener", new XplCallable() {
            @Override public int arity() { return -1; } // aridade variável (2 ou 3)
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                if (args.size() < 2 || args.size() > 3) {
                    throw new IllegalArgumentException("addEventListener espera 2 ou 3 argumentos.");
                }
                String type = intp.evaluate(args.get(0).expression).toString();
                if (args.size() == 2) {
                    // Segundo argumento pode ser XplEventListener ou XplFunction
                    Object listenerObj = intp.evaluate(args.get(1).expression);
                    if (listenerObj instanceof XplEventListener) {
                        element.addEventListener(type, (XplEventListener) listenerObj);
                    } else if (listenerObj instanceof XplFunction) {
                        element.addEventListener(type, (XplFunction) listenerObj, intp);
                    } else {
                        throw new IllegalArgumentException("Ouvinte deve ser XplEventListener ou XplFunction.");
                    }
                } else { // 3 argumentos: podemos ignorar o terceiro (options) ou tratar
                    Object listenerObj = intp.evaluate(args.get(1).expression);
                    // options ignorado por simplicidade
                    if (listenerObj instanceof XplEventListener) {
                        element.addEventListener(type, (XplEventListener) listenerObj);
                    } else if (listenerObj instanceof XplFunction) {
                        element.addEventListener(type, (XplFunction) listenerObj, intp);
                    } else {
                        throw new IllegalArgumentException("Ouvinte deve ser XplEventListener ou XplFunction.");
                    }
                }
                return null;
            }
        });

// removeEventListener
        element.fields.put("removeEventListener", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String type = intp.evaluate(args.get(0).expression).toString();
                XplEventListener listener = (XplEventListener) intp.evaluate(args.get(1).expression);
                element.removeEventListener(type, listener);
                return null;
            }
        });

// dispatchEvent
        element.fields.put("dispatchEvent", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplEvent event = (XplEvent) intp.evaluate(args.getFirst().expression);
                return element.dispatchEvent(event);
            }
        });

// toObject
        element.fields.put("toObject", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return element.getDOMObject();
            }
        });

// getDataset
        element.fields.put("getDataset", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return element.getDataset();
            }
        });

// getParent
        element.fields.put("getParent", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return element.getParent();
            }
        });

// getChildren
        element.fields.put("getChildren", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return element.getChildren();
            }
        });

// getAncestors
        element.fields.put("getAncestors", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return element.getAncestors();
            }
        });

// getSiblings
        element.fields.put("getSiblings", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return element.getSiblings();
            }
        });

        // contains
        element.fields.put("contains", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                XplElement node = (XplElement) intp.evaluate(args.getFirst().expression);
                return element.contains(node);
            }
        });

// insertAdjacentHTML
        element.fields.put("insertAdjacentHTML", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String position = intp.evaluate(args.get(0).expression).toString();
                String html = intp.evaluate(args.get(1).expression).toString();
                element.insertAdjacentHTML(position, html);
                return null;
            }
        });

        // focus
        element.fields.put("focus", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                element.focus(intp);
                return null;
            }
        });
        
        properties(element);
    }

    public static void properties(XplElement element) {
        // ─── PROPRIEDADES (GETTERS, ARIDADE 0) ────────────────────────────────

        // Básicas
        element.fields.put("tagName", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.tagName;
            }
        });
        element.fields.put("id", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getId();
            }
        });
        element.fields.put("className", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getClassName();
            }
        });
        element.fields.put("textContent", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getTextContent();
            }
        });
        element.fields.put("innerHTML", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {

                return  element.getInnerHTML();
            }
        });
        element.fields.put("outerHTML", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getOuterHTML();
            }
        });
        element.fields.put("innerText", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getInnerText();
            }
        });
        element.fields.put("outerText", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getOuterText();
            }
        });

        // Navegação (árvore)
        element.fields.put("parentNode", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.parentNode;
            }
        });
        element.fields.put("parentElement", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.parentElement;
            }
        });
        element.fields.put("nextSibling", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.nextSibling;
            }
        });
        element.fields.put("previousSibling", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return element.previousSibling;
            }
        });
        element.fields.put("firstChild", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.firstChild;
            }
        });
        element.fields.put("lastChild", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.lastChild;
            }
        });
        element.fields.put("children", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getChildren();
            }
        });
        element.fields.put("childElementCount", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.children.size();
            }
        });
        // alias para childCount (se quiser)
        element.fields.put("childCount", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return element.children.size();
            }
        });

        // Atributos e estilos
        element.fields.put("attributes", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getAttributes();
            }
        });
        element.fields.put("style", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.attributes.get("style");
            }
        });
        element.fields.put("dataset", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getDataset();
            }
        });

        // Dimensões (retornam 0 por enquanto, mas podem ser sobrescritas)
        element.fields.put("clientWidth", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getClientWidth();
            }
        });
        element.fields.put("clientHeight", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getClientHeight();
            }
        });
        element.fields.put("offsetWidth", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getOffsetWidth();
            }
        });
        element.fields.put("offsetHeight", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getOffsetHeight();
            }
        });
        element.fields.put("scrollWidth", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getScrollWidth();
            }
        });
        element.fields.put("scrollHeight", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getScrollHeight();
            }
        });
        element.fields.put("scrollTop", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getScrollTop();
            }
        });
        element.fields.put("scrollLeft", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getScrollLeft();
            }
        });

        // ─── SETTERS PARA PROPRIEDADES MUTÁVEIS (ARIDADE 1) ──────────────────

        element.fields.put("setId", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String val = intp.evaluate(args.getFirst().expression).toString();
                element.setId(val);
                return null;
            }
        });
        element.fields.put("setClassName", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String val = intp.evaluate(args.getFirst().expression).toString();
                element.setClassName(val);
                return null;
            }
        });
        element.fields.put("setTextContent", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String val = intp.evaluate(args.getFirst().expression).toString();
                element.setTextContent(val);
                return null;
            }
        });
        element.fields.put("setInnerHTML", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String val = intp.evaluate(args.getFirst().expression).toString();
                element.setInnerHTML(val);
                return null;
            }
        });
        element.fields.put("setStyle", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String prop = intp.evaluate(args.get(0).expression).toString();
                String val = intp.evaluate(args.get(1).expression).toString();
                element.setStyle(prop, val);
                return null;
            }
        });
        element.fields.put("setScrollTop", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                int val = (int) intp.evaluate(args.getFirst().expression);
                element.setScrollTop(val);
                return null;
            }
        });
        element.fields.put("setScrollLeft", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                int val = (int) intp.evaluate(args.getFirst().expression);
                element.setScrollLeft(val);
                return null;
            }
        });

        // ─── MÉTODOS DE NAVEGAÇÃO E UTILIDADE ──────────────────────────────────

        // Já existem em invokeMethod, mas podemos adicionar atalhos
        element.fields.put("getParent", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getParent();
            }
        });
        element.fields.put("getChildren", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getChildren();
            }
        });
        element.fields.put("getAncestors", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getAncestors();
            }
        });
        element.fields.put("getSiblings", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getSiblings();
            }
        });
        element.fields.put("getFirstChild", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.firstChild;
            }
        });
        element.fields.put("getLastChild", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.lastChild;
            }
        });
        element.fields.put("getNextSibling", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.nextSibling;
            }
        });
        element.fields.put("getPreviousSibling", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.previousSibling;
            }
        });

        // ─── MÉTODOS DE AÇÃO ────────────────────────────────────────────────────

        // getBoundingClientRect - retorna um mapa com coordenadas
        element.fields.put("getBoundingClientRect", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                Map<String, Object> rect = new LinkedHashMap<>();
                rect.put("left", 0);
                rect.put("top", 0);
                rect.put("right", 0);
                rect.put("bottom", 0);
                rect.put("width", 0);
                rect.put("height", 0);
                return rect;
            }
        });

        // scrollIntoView (sem argumentos por simplicidade)
        element.fields.put("scrollIntoView", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                // Simulação
                return null;
            }
        });

        // focus
        element.fields.put("focus", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                // Simulação
                return null;
            }
        });

        // blur
        element.fields.put("blur", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                // Simulação
                return null;
            }
        });

        // click - dispara evento de clique e executa inline se existir
        element.fields.put("click", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                // Disparar evento listeners
                XplEvent clickEvent = new XplEvent("click", element);
                element.dispatchEvent(clickEvent);
                // Executar código inline, se houver
                String inline =  element.inlineEvents.get("click");
                if (inline != null && intp != null) {
                    try {
                        Lexer lexer = new Lexer(inline, null);
                        List<Token> tokens = lexer.tokenize();
                        Parser parser = new Parser(tokens);
                        List<Stmt> stmts = parser.parse();
                        intp.interpret(stmts);
                    } catch (Exception e) {
                        System.err.println("Erro no evento click inline: " + e.getMessage());
                    }
                }
                return null;
            }
        });

        // ─── ALIAS PARA "class" (já que é palavra reservada no Java) ────────────
        // No XPL, pode-se usar 'class' como propriedade
        element.fields.put("class", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return  element.getClassName();
            }
        });
        // Setter para 'class'
        element.fields.put("setClass", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String val = intp.evaluate(args.getFirst().expression).toString();
                element.setClassName(val);
                return null;
            }
        });

        // ─── MÉTODO GENÉRICO getProperty (já existe, mas pode ser mantido) ──────
        element.fields.put("getProperty", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String propName = intp.evaluate(args.getFirst().expression).toString();
                return  element.getProperty(propName);
            }
        });

        // Em XplElementUtils.java -> dentro do método properties()

        // ─── GETTERS UNIVERSAIS (value, name, checked, etc) ──────────────────
        String[] simpleProps = {"value", "name", "type", "src", "href", "placeholder"};
        for (String prop : simpleProps) {
            element.fields.put(prop, new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    return element.getProperty(prop); // Usa o switch que atualizamos
                }
            });
        }

        element.fields.put("checked", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) { return element.getChecked(); }
        });
        element.fields.put("disabled", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) { return element.isDisabled(); }
        });
        element.fields.put("readonly", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) { return element.isReadOnly(); }
        });

        // ─── SETTERS UNIVERSAIS ───────────────────────────────────────────────

        element.fields.put("setValue", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                element.setValue(intp.evaluate(args.getFirst().expression));
                return null;
            }
        });

        element.fields.put("setName", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                element.setName(intp.evaluate(args.getFirst().expression).toString());
                return null;
            }
        });

        element.fields.put("setChecked", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                Object val = intp.evaluate(args.getFirst().expression);
                element.setChecked(val != null && (val.equals(true) || val.toString().equals("true")));
                return null;
            }
        });

        element.fields.put("setDisabled", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                Object val = intp.evaluate(args.getFirst().expression);
                element.setDisabled(val != null && (val.equals(true) || val.toString().equals("true")));
                return null;
            }
        });

        // getContext
        element.fields.put("getContext", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String type = intp.evaluate(args.getFirst().expression).toString();
                // A chamada direta para o teu XplElement (camada core) agnóstico
                return element.getContext(type);
            }
        });
    }

    public static XPLModel buildNativeModel() {
        if (XplElement.nativeModel == null) {
            XplElement.nativeModel = new XPLModel("XplElement", null);

            // ─── Campos (propriedades) ──────────────────────────────────────────────
            String[] fieldNames = {
                    "tagName", "id", "className", "textContent", "innerHTML", "outerHTML",
                    "innerText", "outerText", "parentNode", "parentElement", "nextSibling",
                    "previousSibling", "firstChild", "lastChild", "children", "childCount",
                    "attributes", "style", "dataset", "clientWidth", "clientHeight",
                    "offsetWidth", "offsetHeight", "scrollWidth", "scrollHeight",
                    "scrollTop", "scrollLeft","value", "name", "type", "checked", "src", "href", "disabled", "readonly", "placeholder"
            };
            for (String f : fieldNames) {
                Token nameToken = new Token(TokenType.IDENTIFIER, f, null, 0, 0);
                // FieldDecl(Token modifier, boolean isStatic, boolean isFinal, boolean isReadonly, Token name, TypeNode type)
                Stmt.FieldDecl field = new Stmt.FieldDecl(new Token(TokenType.PUBLIC,"pub",null,0,0,null), false, false, false, nameToken, null);
                XplElement.nativeModel.addField(field);
            }

            // ─── Métodos ──────────────────────────────────────────────────────────────

            // setAttribute(name, value)
            List<Stmt.Param> paramsNameVal = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "name", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "value", null, 0, 0), null, null)
            );
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "setAttribute", null, 0, 0),
                    paramsNameVal,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getAttribute(name)
            List<Stmt.Param> paramsName = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "name", null, 0, 0), null, null)
            );
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getAttribute", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // hasAttribute(name)
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "hasAttribute", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // removeAttribute(name)
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "removeAttribute", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getAttributes()
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getAttributes", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // ─── Árvore ───────────────────────────────────────────────────────────────

            // appendChild(child)
            List<Stmt.Param> paramsChild = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "child", null, 0, 0), null, null)
            );
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "appendChild", null, 0, 0),
                    paramsChild,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // insertBefore(newNode, refNode)
            List<Stmt.Param> paramsNewRef = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "newNode", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "refNode", null, 0, 0), null, null)
            );
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "insertBefore", null, 0, 0),
                    paramsNewRef,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // replaceChild(newNode, oldNode)
            List<Stmt.Param> paramsNewOld = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "newNode", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "oldNode", null, 0, 0), null, null)
            );
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "replaceChild", null, 0, 0),
                    paramsNewOld,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // removeChild(child)
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "removeChild", null, 0, 0),
                    paramsChild,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // remove()
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "remove", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // cloneNode(deep)
            List<Stmt.Param> paramsDeep = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "deep", null, 0, 0), null, null)
            );
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "cloneNode", null, 0, 0),
                    paramsDeep,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // ─── Pesquisa ─────────────────────────────────────────────────────────────

            // getElementById(id)
            List<Stmt.Param> paramsId = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "id", null, 0, 0), null, null)
            );
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getElementById", null, 0, 0),
                    paramsId,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getElementsByClassName(className)
            List<Stmt.Param> paramsClass = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "className", null, 0, 0), null, null)
            );
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getElementsByClassName", null, 0, 0),
                    paramsClass,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getElementsByTagName(tagName)
            List<Stmt.Param> paramsTag = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "tagName", null, 0, 0), null, null)
            );
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getElementsByTagName", null, 0, 0),
                    paramsTag,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getElementsByName(name)
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getElementsByName", null, 0, 0),
                    paramsName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // querySelector(selector)
            List<Stmt.Param> paramsSelector = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "selector", null, 0, 0), null, null)
            );
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "querySelector", null, 0, 0),
                    paramsSelector,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // querySelectorAll(selector)
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "querySelectorAll", null, 0, 0),
                    paramsSelector,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getClosest(selector)
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getClosest", null, 0, 0),
                    paramsSelector,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // matches(selector)
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "matches", null, 0, 0),
                    paramsSelector,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // ─── Classes CSS ────────────────────────────────────────────────────────

            // addClass(className)
            List<Stmt.Param> paramsCls = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "className", null, 0, 0), null, null)
            );
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "addClass", null, 0, 0),
                    paramsCls,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // removeClass(className)
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "removeClass", null, 0, 0),
                    paramsCls,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // hasClass(className)
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "hasClass", null, 0, 0),
                    paramsCls,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // toggleClass(className)
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "toggleClass", null, 0, 0),
                    paramsCls,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // ─── Estilos ─────────────────────────────────────────────────────────────

            // setStyle(property, value)
            List<Stmt.Param> paramsStyle = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "property", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "value", null, 0, 0), null, null)
            );
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "setStyle", null, 0, 0),
                    paramsStyle,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getStyle(property)
            List<Stmt.Param> paramsProp = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "property", null, 0, 0), null, null)
            );
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getStyle", null, 0, 0),
                    paramsProp,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // ─── Conteúdo ────────────────────────────────────────────────────────────

            // setInnerHTML(html)
            List<Stmt.Param> paramsHTML = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "html", null, 0, 0), null, null)
            );
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "setInnerHTML", null, 0, 0),
                    paramsHTML,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getInnerHTML()
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getInnerHTML", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getOuterHTML()
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getOuterHTML", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getTextContent()
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getTextContent", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // setTextContent(text)
            List<Stmt.Param> paramsText = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "text", null, 0, 0), null, null)
            );
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "setTextContent", null, 0, 0),
                    paramsText,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getInnerText()
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getInnerText", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getOuterText()
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getOuterText", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // appendText(text)
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "appendText", null, 0, 0),
                    paramsText,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // prependText(text)
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "prependText", null, 0, 0),
                    paramsText,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // clear()
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "clear", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // ─── Eventos ─────────────────────────────────────────────────────────────

            // addEventListener(type, listener)
            List<Stmt.Param> paramsEventListener = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "type", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "listener", null, 0, 0), null, null)
            );
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "addEventListener", null, 0, 0),
                    paramsEventListener,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // removeEventListener(type, listener)
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "removeEventListener", null, 0, 0),
                    paramsEventListener,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // dispatchEvent(event)
            List<Stmt.Param> paramsEvent = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "event", null, 0, 0), null, null)
            );
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "dispatchEvent", null, 0, 0),
                    paramsEvent,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // callAction(eventName)
            List<Stmt.Param> paramsEvtName = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "eventName", null, 0, 0), null, null)
            );
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "callAction", null, 0, 0),
                    paramsEvtName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // ─── Utilitários ────────────────────────────────────────────────────────

            // toObject()
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "toObject", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getDataset()
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getDataset", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getParent()
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getParent", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getChildren()
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getChildren", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getAncestors()
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getAncestors", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getSiblings()
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getSiblings", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // contains(child)
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "contains", null, 0, 0),
                    paramsChild,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // focus()
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "focus", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // blur()
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "blur", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // click()
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "click", null, 0, 0),
                    Collections.emptyList(),
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // insertAdjacentHTML(position, html)
            List<Stmt.Param> paramsPosHTML = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "position", null, 0, 0), null, null),
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "html", null, 0, 0), null, null)
            );
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "insertAdjacentHTML", null, 0, 0),
                    paramsPosHTML,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getProperty(propName)
            List<Stmt.Param> paramsPropName = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "propName", null, 0, 0), null, null)
            );
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getProperty", null, 0, 0),
                    paramsPropName,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));

            // getContext(type)
            List<Stmt.Param> paramsType = List.of(
                    new Stmt.Param(new Token(TokenType.IDENTIFIER, "type", null, 0, 0), null, null)
            );
            XplElement.nativeModel.addMethod(new Stmt.Function(
                    null, false, false,
                    new Token(TokenType.IDENTIFIER, "getContext", null, 0, 0),
                    paramsType,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
        }
        return XplElement.nativeModel;
    }
}
