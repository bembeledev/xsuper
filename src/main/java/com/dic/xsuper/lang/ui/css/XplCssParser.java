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
            } else if (match(XplCssTokenType.AT_MEDIA)) {
                root.addChild(parseMediaRule());
            } else if (match(XplCssTokenType.AT_KEYFRAMES)) {
                root.addChild(parseKeyframesRule());
            } else if (match(XplCssTokenType.AT_IMPORT)) {
                root.addChild(parseAtRule());
            } else {
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

    // 🌐 Media Query: Captura tudo como a condição correta
    private XplNode parseMediaRule() {
        StringBuilder condition = new StringBuilder();

        // Em vez de procurar rigidamente parênteses, lemos tudo até ao início do bloco '{'
        while (!isAtEnd() && !check(XplCssTokenType.LBRACE)) {
            condition.append(advance().lexeme).append(" ");
        }

        XplNode mediaNode = new XplNode("@media");
        mediaNode.attributes.put("condition", condition.toString().trim());

        parseBlock(mediaNode); // O interior do media são regras normais
        return mediaNode;
    }

    // 🎬 Keyframes: Espera o nome da animação (ex: spin) e blocos com percentagens (0%, 100%)
    private XplNode parseKeyframesRule() {
        // O nome da animação é capturado como identificador/seletor
        String animName = previous().lexeme;
        if (check(XplCssTokenType.IDENTIFIER) || check(XplCssTokenType.SELECTOR)) {
            animName = advance().lexeme;
        }

        XplNode keyframesNode = new XplNode("@keyframes");
        keyframesNode.attributes.put("name", animName);

        consume(XplCssTokenType.LBRACE, "Esperado '{' no @keyframes");

        // O interior do keyframes são passos temporais (from, to, 0%, 50%, 100%)
        while (!check(XplCssTokenType.RBRACE) && !isAtEnd()) {
            if (check(XplCssTokenType.SELECTOR) || check(XplCssTokenType.IDENTIFIER) || check(XplCssTokenType.NUMBER)) {
                String frameSelector = parseSelector();
                XplNode frameNode = new XplNode("frame");
                frameNode.attributes.put("step", frameSelector);
                parseBlock(frameNode);
                keyframesNode.addChild(frameNode);
            } else {
                advance();
            }
        }

        consume(XplCssTokenType.RBRACE, "Esperado '}' para fechar o @keyframes");

        return keyframesNode;
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

                // ⭐ Se for 'transition', guardamos diretamente no nó pai (a regra atual)
                if ("transition".equals(propName)) {
                    // Lemos o valor até ';' e criamos um nó literal ou expressão
                    while (!isAtEnd() && !check(XplCssTokenType.SEMICOLON) && !check(XplCssTokenType.RBRACE)) {
                        propNode.addChild(parseValue());
                    }
                    parent.addChild(propNode);
                    match(XplCssTokenType.SEMICOLON);
                    continue;
                }


                // ⭐ NOVO: Um valor CSS pode ser uma mistura! Lemos TUDO até o ';'
                while (!isAtEnd() && !check(XplCssTokenType.SEMICOLON) && !check(XplCssTokenType.RBRACE)) {
                    if (match(XplCssTokenType.AT_IF)) {
                        propNode.addChild(parseIfBlock());
                    } else if (match(XplCssTokenType.AT_SWITCH)) {
                        propNode.addChild(parseSwitchBlock());
                    } else if (match(XplCssTokenType.AT_MATCH)) {
                        propNode.addChild(parseMatchBlock());
                    } else {
                        propNode.addChild(parseValue());
                    }
                }

                parent.addChild(propNode);
                match(XplCssTokenType.SEMICOLON); // Consome o ; no final da linha
            }
            else if (match(XplCssTokenType.VAR_NAME)) {
                String varName = previous().lexeme;
                consume(XplCssTokenType.COLON, "Esperado ':' após variável.");
                XplNode varNode = new XplNode("variable");
                varNode.attributes.put("name", varName);
                // Valor pode ser literal, número, expressão ou diretiva
                if (match(XplCssTokenType.AT_IF)) {
                    varNode.addChild(parseIfBlock());
                } else if (match(XplCssTokenType.AT_SWITCH)) {
                    varNode.addChild(parseSwitchBlock());
                } else {
                    varNode.addChild(parseValue());
                }
                parent.addChild(varNode);
                match(XplCssTokenType.SEMICOLON);
            }
            // 3. Seletor aninhado (regra)
            else if (check(XplCssTokenType.SELECTOR) || check(XplCssTokenType.IDENTIFIER) ||
                    check(XplCssTokenType.PSEUDO_CLASS) || check(XplCssTokenType.STAR) ||
                    check(XplCssTokenType.NUMBER)) {
                // Usamos o método parseSelector() nativo que já entende as variáveis {{...}} e as vírgulas!
                String selector = parseSelector();

                if (selector != null && !selector.isEmpty()) {
                    XplNode childRule = new XplNode("rule");
                    childRule.attributes.put("selector", selector);
                    parseBlock(childRule);
                    parent.addChild(childRule);
                }
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

            // Consome PSEUDO_CLASS (ex: :root)
            if (check(XplCssTokenType.PSEUDO_CLASS)) {
                selector.append(advance().lexeme);
                continue;
            }

            // ⭐ Seletor universal: * { ... }
            if (check(XplCssTokenType.STAR)) {
                selector.append(advance().lexeme);
                // Se o próximo token for '{', para, senão continua (ex: *.classe)
                if (check(XplCssTokenType.LBRACE)) break;
                continue;
            }

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

        // 1. Tratamento isolado para var() no início
        if (match(XplCssTokenType.VAR_FUNC)) {
            valNode.attributes.put("type", "var");
            if (match(XplCssTokenType.VAR_NAME)) {
                valNode.attributes.put("name", previous().lexeme);
                if (match(XplCssTokenType.COMMA)) {
                    StringBuilder fallback = new StringBuilder();
                    while (!isAtEnd() && !check(XplCssTokenType.RPAREN)) {
                        fallback.append(advance().lexeme);
                    }
                    XplNode fallbackNode = new XplNode("literal");
                    fallbackNode.attributes.put("type", "fallback");
                    fallbackNode.attributes.put("value", fallback.toString().trim());
                    valNode.addChild(fallbackNode);
                }
                consume(XplCssTokenType.RPAREN, "Esperado ')' para fechar var()");
            }
            return valNode;
        }

        // 2. Tratamento isolado para calc()
        if (match(XplCssTokenType.CALC_FUNC)) {
            StringBuilder calcExpr = new StringBuilder("calc(");
            int depth = 1;
            while (depth > 0 && !isAtEnd()) {
                XplCssToken token = advance();
                if (token.type == XplCssTokenType.LPAREN) depth++;
                else if (token.type == XplCssTokenType.RPAREN) depth--;

                if (token.type == XplCssTokenType.PLUS || token.type == XplCssTokenType.MINUS ||
                        token.type == XplCssTokenType.STAR || token.type == XplCssTokenType.SLASH) {
                    calcExpr.append(" ").append(token.lexeme).append(" ");
                } else {
                    calcExpr.append(token.lexeme);
                }
            }
            valNode.attributes.put("type", "expression");
            valNode.attributes.put("data", calcExpr.toString().trim());
            return valNode;
        }

        // 3. O SEGREDO: Coleta todos os tokens contíguos até bater numa parede (; ou })
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && !check(XplCssTokenType.SEMICOLON) && !check(XplCssTokenType.RBRACE)) {
            // Se encontrar uma diretiva misturada no valor (ex: border: 3px @if), ele recua para o parseBlock!
            if (check(XplCssTokenType.AT_IF) || check(XplCssTokenType.AT_SWITCH) ||
                    check(XplCssTokenType.AT_FOR) || check(XplCssTokenType.AT_MATCH)) {
                break;
            }

            XplCssToken t = advance();
            if (t.type == XplCssTokenType.STRING) {
                sb.append("\"").append(t.literal != null ? t.literal : t.lexeme).append("\"");
            } else {
                sb.append(t.lexeme);
            }

            // Adiciona espaços lógicos onde é necessário (ignora antes de vírgulas)
            if (!isAtEnd() && !check(XplCssTokenType.SEMICOLON) && !check(XplCssTokenType.RBRACE)) {
                XplCssTokenType nextType = peek().type;
                if (t.type != XplCssTokenType.LPAREN && nextType != XplCssTokenType.RPAREN &&
                        nextType != XplCssTokenType.COMMA && t.type != XplCssTokenType.COMMA) {
                    sb.append(" ");
                }
            }
        }

        String expr = sb.toString().trim();

        // Otimização: Identifica perfeitamente se é uma string pura, um número puro, ou uma expressão complexa
        if (expr.startsWith("\"") && expr.endsWith("\"") && expr.indexOf("\"", 1) == expr.length() - 1) {
            valNode.attributes.put("type", "literal");
            valNode.attributes.put("data", expr.substring(1, expr.length() - 1));
        } else if (expr.matches("^-?\\d+(\\.\\d+)?(%|[a-zA-Z]+)?$")) {
            valNode.attributes.put("type", "number");
            valNode.attributes.put("data", expr);
        } else {
            valNode.attributes.put("type", "expression");
            valNode.attributes.put("data", expr);
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