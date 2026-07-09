package com.dic.xsuper.lang.ui.css;

import com.dic.xsuper.lang.ui.html.XplNode;

import java.util.List;

public class XplCssParser {
    private final List<XplCssToken> tokens;
    private int current = 0;

    public XplCssParser(List<XplCssToken> tokens) {
        this.tokens = tokens;
    }

    public XplNode parse() {
        XplNode root = new XplNode("css");
        while (!isAtEnd()) {
            if (match(XplCssTokenType.AT_IF)) {
                root.addChild(parseIfBlock());
            } else if (match(XplCssTokenType.AT_FOR)) {
                root.addChild(parseForBlock());
            } else if (match(XplCssTokenType.AT_SWITCH)) {
                root.addChild(parseSwitchBlock());
            } else if (match(XplCssTokenType.AT_MATCH)) {
                root.addChild(parseMatchBlock());
            } else if (match(XplCssTokenType.AT_MEDIA) || match(XplCssTokenType.AT_KEYFRAMES) || match(XplCssTokenType.AT_IMPORT)) {
                root.addChild(parseAtRule());
            } else {
                // Regra normal: seletor + bloco
                String selector = parseSelector();
                if (selector != null && !selector.isEmpty()) {
                    XplNode rule = new XplNode("rule");
                    rule.attributes.put("selector", selector);
                    parseBlock(rule);
                    root.addChild(rule);
                } else {
                    advance();
                }
            }
        }
        return root;
    }

    // ---- Parsing de blocos estruturais ----

    private XplNode parseIfBlock() {
        consume(XplCssTokenType.LPAREN, "Esperado '(' após @if");
        String condition = captureExpression();
        consume(XplCssTokenType.RPAREN, "Esperado ')'");

        XplNode ifNode = new XplNode("@if");
        ifNode.attributes.put("condition", condition);
        parseBlock(ifNode);

        while (match(XplCssTokenType.AT_ELSEIF)) {
            consume(XplCssTokenType.LPAREN, "Esperado '('");
            String elseIfCond = captureExpression();
            consume(XplCssTokenType.RPAREN, "Esperado ')'");
            XplNode elseIfNode = new XplNode("@elseif");
            elseIfNode.attributes.put("condition", elseIfCond);
            parseBlock(elseIfNode);
            ifNode.addChild(elseIfNode);
        }

        if (match(XplCssTokenType.AT_ELSE)) {
            XplNode elseNode = new XplNode("@else");
            parseBlock(elseNode);
            ifNode.addChild(elseNode);
        }

        return ifNode;
    }

    private XplNode parseForBlock() {
        consume(XplCssTokenType.LPAREN, "Esperado '(' após @for");
        // Captura a expressão, que deve conter "let item of items" (tokens IDENTIFIER/SELECTOR)
        String expr = captureExpression();
        consume(XplCssTokenType.RPAREN, "Esperado ')'");

        XplNode forNode = new XplNode("@for");
        forNode.attributes.put("expression", expr);

        // Consome o bloco { ... } do @for
        parseBlock(forNode);

        if (match(XplCssTokenType.AT_EMPTY)) {
            XplNode emptyNode = new XplNode("@empty");
            parseBlock(emptyNode);
            forNode.addChild(emptyNode);
        }
        return forNode;
    }

    private XplNode parseSwitchBlock() {
        consume(XplCssTokenType.LPAREN, "Esperado '(' após @switch");
        String condition = captureExpression();
        consume(XplCssTokenType.RPAREN, "Esperado ')'");

        XplNode switchNode = new XplNode("@switch");
        switchNode.attributes.put("condition", condition);
        consume(XplCssTokenType.LBRACE, "Esperado '{'");

        while (!check(XplCssTokenType.RBRACE) && !isAtEnd()) {
            if (match(XplCssTokenType.AT_CASE)) {
                switchNode.addChild(parseCaseBlock());
            } else if (match(XplCssTokenType.AT_DEFAULT)) {
                switchNode.addChild(parseDefaultBlock());
            } else {
                advance();
            }
        }
        consume(XplCssTokenType.RBRACE, "Esperado '}'");
        return switchNode;
    }

    private XplNode parseCaseBlock() {
        consume(XplCssTokenType.LPAREN, "Esperado '('");
        String value = captureExpression();
        consume(XplCssTokenType.RPAREN, "Esperado ')'");
        XplNode caseNode = new XplNode("@case");
        caseNode.attributes.put("value", value);
        parseBlock(caseNode);
        return caseNode;
    }

    private XplNode parseDefaultBlock() {
        XplNode defaultNode = new XplNode("@default");
        parseBlock(defaultNode);
        return defaultNode;
    }

    private XplNode parseMatchBlock() {
        consume(XplCssTokenType.LPAREN, "Esperado '('");
        String condition = captureExpression();
        consume(XplCssTokenType.RPAREN, "Esperado ')'");
        XplNode matchNode = new XplNode("@match");
        matchNode.attributes.put("condition", condition);
        consume(XplCssTokenType.LBRACE, "Esperado '{'");

        while (!check(XplCssTokenType.RBRACE) && !isAtEnd()) {
            if (match(XplCssTokenType.AT_ARM)) {
                matchNode.addChild(parseArmBlock());
            } else if (match(XplCssTokenType.AT_NONE)) {
                matchNode.addChild(parseNoneBlock());
            } else {
                advance();
            }
        }
        consume(XplCssTokenType.RBRACE, "Esperado '}'");
        return matchNode;
    }

    private XplNode parseArmBlock() {
        consume(XplCssTokenType.LPAREN, "Esperado '('");
        String pattern = captureExpression();
        consume(XplCssTokenType.RPAREN, "Esperado ')'");
        XplNode armNode = new XplNode("@arm");
        armNode.attributes.put("pattern", pattern);
        parseBlock(armNode);
        return armNode;
    }

    private XplNode parseNoneBlock() {
        XplNode noneNode = new XplNode("@none");
        parseBlock(noneNode);
        return noneNode;
    }

    // ---- Utilitários para parsing de CSS ----

    // parseBlock: processa o conteúdo dentro de { ... }
    private void parseBlock(XplNode parent) {
        consume(XplCssTokenType.LBRACE, "Esperado '{' após o seletor ou diretiva.");

        while (!check(XplCssTokenType.RBRACE) && !isAtEnd()) {
            // 1. Diretivas estruturais
            if (match(XplCssTokenType.AT_IF)) {
                parent.addChild(parseIfBlock());
            } else if (match(XplCssTokenType.AT_FOR)) {
                parent.addChild(parseForBlock());
            } else if (match(XplCssTokenType.AT_SWITCH)) {
                parent.addChild(parseSwitchBlock());
            } else if (match(XplCssTokenType.AT_MATCH)) {
                parent.addChild(parseMatchBlock());
            }
            // 2. Propriedade CSS
            else if (match(XplCssTokenType.PROPERTY)) {
                String propName = previous().lexeme;
                consume(XplCssTokenType.COLON, "Esperado ':' após propriedade.");
                XplNode propNode = new XplNode("property");
                propNode.attributes.put("name", propName);

                if (match(XplCssTokenType.AT_IF)) {
                    propNode.addChild(parseIfBlock());
                } else if (match(XplCssTokenType.AT_SWITCH)) {
                    propNode.addChild(parseSwitchBlock());
                } else if (match(XplCssTokenType.AT_MATCH)) {
                    propNode.addChild(parseMatchBlock());
                } else {
                    propNode.addChild(parseValue());
                }

                parent.addChild(propNode);
                match(XplCssTokenType.SEMICOLON);
            }
            // 3. Seletor aninhado (regra)
            else if (match(XplCssTokenType.SELECTOR) || check(XplCssTokenType.IDENTIFIER)) {
                // Para suportar seletores compostos, consumimos todos os SELECTOR/IDENTIFIER/COMMA consecutivos
                StringBuilder selector = new StringBuilder();
                selector.append(previous().lexeme);
                while (check(XplCssTokenType.SELECTOR) || check(XplCssTokenType.IDENTIFIER) || check(XplCssTokenType.COMMA)) {
                    selector.append(advance().lexeme);
                }
                XplNode childRule = new XplNode("rule");
                childRule.attributes.put("selector", selector.toString().trim());
                parseBlock(childRule);
                parent.addChild(childRule);
            }
            // 4. Literais (STRING, NUMBER) – consumir e criar nó literal
            else if (match(XplCssTokenType.STRING)) {
                XplNode litNode = new XplNode("literal");
                litNode.attributes.put("type", "string");
                litNode.attributes.put("value", previous().literal);
                parent.addChild(litNode);
            } else if (match(XplCssTokenType.NUMBER)) {
                XplNode litNode = new XplNode("literal");
                litNode.attributes.put("type", "number");
                litNode.attributes.put("value", previous().literal != null ? previous().literal : previous().lexeme);
                parent.addChild(litNode);
            }
            // 5. Spread Operator (...var)
            else if (check(XplCssTokenType.IDENTIFIER) && peek().lexeme.startsWith("...")) {
                String spreadLexeme = advance().lexeme;
                XplNode spreadNode = new XplNode("spread");
                spreadNode.attributes.put("var", spreadLexeme.substring(3));
                parent.addChild(spreadNode);
                match(XplCssTokenType.SEMICOLON);
            }
            // 6. Outros tokens: avançar para evitar loop
            else {
                advance();
            }
        }

        consume(XplCssTokenType.RBRACE, "Esperado '}' para fechar o bloco.");
    }

    // parseSelector: recolhe o seletor até encontrar '{' ou diretiva
    private String parseSelector() {
        StringBuilder selector = new StringBuilder();

        while (!isAtEnd()) {
            // Se encontrarmos '{{', capturamos o conteúdo (bind)
            if (check(XplCssTokenType.LBRACE) && checkAhead(XplCssTokenType.LBRACE)) {
                selector.append(advance().lexeme); // {
                selector.append(advance().lexeme); // {
                while (!isAtEnd() && !(check(XplCssTokenType.RBRACE) && checkAhead(XplCssTokenType.RBRACE))) {
                    selector.append(advance().lexeme);
                }
                if (!isAtEnd()) {
                    selector.append(advance().lexeme); // }
                    selector.append(advance().lexeme); // }
                }
                continue;
            }

            // Condição de paragem: início do bloco '{' ou de uma diretiva de controlo
            if (check(XplCssTokenType.LBRACE) || check(XplCssTokenType.AT_IF) ||
                    check(XplCssTokenType.AT_FOR) || check(XplCssTokenType.AT_SWITCH) ||
                    check(XplCssTokenType.AT_MATCH)) {
                break;
            }

            // Consome tokens (SELECTOR, IDENTIFIER, etc.) e adiciona ao seletor
            selector.append(advance().lexeme);
        }
        return selector.toString().trim();
    }

    // parseValue: processa o valor de uma propriedade
    private XplNode parseValue() {
        XplNode valNode = new XplNode("value");

        if (match(XplCssTokenType.STRING)) {
            valNode.attributes.put("type", "literal");
            valNode.attributes.put("data", previous().literal);
        } else if (match(XplCssTokenType.NUMBER)) {
            valNode.attributes.put("type", "number");
            valNode.attributes.put("data", previous().literal != null ? previous().literal : previous().lexeme);
        } else {
            // Expressão composta (ex: 12px, calc(...))
            StringBuilder sb = new StringBuilder();
            while (!isAtEnd() && !check(XplCssTokenType.SEMICOLON) && !check(XplCssTokenType.RBRACE)) {
                if (check(XplCssTokenType.AT_IF) || check(XplCssTokenType.AT_SWITCH) ||
                        check(XplCssTokenType.AT_FOR) || check(XplCssTokenType.AT_MATCH)) {
                    break;
                }
                sb.append(advance().lexeme);
            }
            valNode.attributes.put("type", "expression");
            valNode.attributes.put("data", sb.toString().trim());
        }
        return valNode;
    }

    // parseAtRule: processa @media, @keyframes, etc.
    private XplNode parseAtRule() {
        String directive = previous().lexeme;
        StringBuilder content = new StringBuilder();
        while (!isAtEnd() && !check(XplCssTokenType.LBRACE) && !check(XplCssTokenType.SEMICOLON)) {
            content.append(advance().lexeme);
        }
        XplNode atNode = new XplNode("@rule");
        atNode.attributes.put("directive", directive);
        atNode.attributes.put("content", content.toString().trim());
        if (match(XplCssTokenType.LBRACE)) {
            parseBlock(atNode);
        } else {
            consume(XplCssTokenType.SEMICOLON, "Esperado ';' ou '{'");
        }
        return atNode;
    }

    // captureExpression: captura tokens até encontrar ')' (usado para condições)
    private String captureExpression() {
        StringBuilder expr = new StringBuilder();
        int depth = 1;
        while (depth > 0 && !isAtEnd()) {
            if (check(XplCssTokenType.RPAREN) && depth == 1) break;
            XplCssToken token = advance();
            if (token.type == XplCssTokenType.LPAREN) depth++;
            else if (token.type == XplCssTokenType.RPAREN) depth--;
            expr.append(token.lexeme).append(" ");
        }
        return expr.toString().trim();
    }

    // ---- Ferramentas de parsing ----

    private boolean match(XplCssTokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean check(XplCssTokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private boolean checkAhead(XplCssTokenType type) {
        if (current + 1 >= tokens.size()) return false;
        return tokens.get(current + 1).type == type;
    }

    private XplCssToken advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == XplCssTokenType.EOF;
    }

    private XplCssToken peek() {
        return tokens.get(current);
    }

    private XplCssToken previous() {
        return tokens.get(current - 1);
    }

    private XplCssToken consume(XplCssTokenType type, String message) {
        if (check(type)) return advance();
        throw new RuntimeException("Erro na linha " + peek().line + ": " + message);
    }
}