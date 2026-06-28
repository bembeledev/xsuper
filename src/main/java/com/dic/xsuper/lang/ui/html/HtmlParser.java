package com.dic.xsuper.lang.ui.html;
import com.dic.xsuper.lang.ui.XplNode;
import java.util.List;

public class HtmlParser {
    private final List<HtmlToken> tokens;
    private int current = 0;

    public HtmlParser(List<HtmlToken> tokens) {
        this.tokens = tokens;
    }

    // O ponto de entrada. Retorna o nó RAIZ (ex: um <body> ou <main> invisível que guarda tudo)
    public XplNode parse() {
        XplNode root = new XplNode("root");
        while (!isAtEnd()) {
            if (check(HtmlTokenType.TEXT)) {
                root.textContent += advance().lexeme;
            } else if (check(HtmlTokenType.LT)) {
                root.addChild(parseElement());
            } else {
                advance(); // Ignora tokens perdidos no nível superior
            }
        }
        return root;
    }

    // ⭐ O CORAÇÃO DO PARSER: Lê uma tag completa e os seus filhos!
    private XplNode parseElement() {
        consume(HtmlTokenType.LT, "Esperado '<' para iniciar a tag.");

        HtmlToken tagToken = consume(HtmlTokenType.IDENTIFIER, "Esperado nome da tag após '<'.");
        XplNode node = new XplNode(tagToken.lexeme);

        // 1. EXTRAIR PROPRIEDADES (Atributos, Eventos, Bindings)
        while (!check(HtmlTokenType.GT) && !check(HtmlTokenType.SLASH) && !isAtEnd()) {

            // É um EVENTO? (click)="funcao"
            if (match(HtmlTokenType.LPAREN)) {
                String eventName = consume(HtmlTokenType.IDENTIFIER, "Esperado nome do evento.").lexeme;
                consume(HtmlTokenType.RPAREN, "Esperado ')' após o nome do evento.");
                consume(HtmlTokenType.EQUALS, "Esperado '=' após o evento.");
                String action = consume(HtmlTokenType.STRING, "Esperado ação do evento em aspas.").literal.toString();
                node.events.put(eventName, action);
            }
            // É um DATA BINDING? [value]="var"
            else if (match(HtmlTokenType.LBRACKET)) {
                String bindName = consume(HtmlTokenType.IDENTIFIER, "Esperado nome do binding.").lexeme;
                consume(HtmlTokenType.RBRACKET, "Esperado ']' após o nome do binding.");
                consume(HtmlTokenType.EQUALS, "Esperado '='.");
                String varName = consume(HtmlTokenType.STRING, "Esperado variável do binding em aspas.").literal.toString();
                node.bindings.put(bindName, varName);
            }
            // É um ATRIBUTO NORMAL? id="painel"
            else if (check(HtmlTokenType.IDENTIFIER)) {
                String attrName = advance().lexeme;
                if (match(HtmlTokenType.EQUALS)) {
                    String attrValue = consume(HtmlTokenType.STRING, "Esperado valor do atributo.").literal.toString();

                    // Atalhos JS-like para a raiz do Node
                    if (attrName.equals("id")) node.id = attrValue;
                    else if (attrName.equals("class")) node.className = attrValue;
                    else node.attributes.put(attrName, attrValue);
                } else {
                    node.attributes.put(attrName, "true"); // Atributos booleanos (ex: disabled)
                }
            } else {
                // Se for uma diretiva estrutural nova (@if) dentro da tag (fallback de segurança)
                advance();
            }
        }

        // 2. VERIFICAR AUTO-FECHO (Ex: <input />)
        if (match(HtmlTokenType.SLASH)) {
            consume(HtmlTokenType.GT, "Esperado '>' após '/'.");
            return node;
        }

        consume(HtmlTokenType.GT, "Esperado '>' para fechar a abertura da tag.");

        // 3. LER CONTEÚDO INTERNO E FILHOS (RECURSÃO ATUALIZADA!)
        while (!isAtEnd()) {
            // Se encontrámos '</', significa que a NOSSA tag está a fechar!
            if (check(HtmlTokenType.LT) && checkNext(HtmlTokenType.SLASH)) {
                break;
            }

            // ⭐ CHAMAMOS O ORQUESTRADOR EM VEZ DE APENAS PARSE ELEMENT ⭐
            XplNode child = parseNode();
            if (child != null && (!child.tag.equals("text") || !child.textContent.isEmpty())) {
                node.addChild(child);
            }
        }

        // 4. FECHAR A TAG (Ex: </div>)
        consume(HtmlTokenType.LT, "Esperado '<' para fechar a tag.");
        consume(HtmlTokenType.SLASH, "Esperado '/' no fecho da tag.");
        HtmlToken closeTag = consume(HtmlTokenType.IDENTIFIER, "Esperado nome da tag no fecho.");

        if (!closeTag.lexeme.equals(node.tag)) {
            throw new RuntimeException("Erro na linha " + closeTag.line + ": Fecho de tag incorreto. Esperado '</" + node.tag + ">', mas encontrou '</" + closeTag.lexeme + ">'.");
        }

        consume(HtmlTokenType.GT, "Esperado '>' para finalizar a tag.");

        return node;
    }

    // =====================================================================
    // ⭐ O ORQUESTRADOR CENTRAL (Substitui o antigo conteúdo do while)
    // =====================================================================

    // 2. O ORQUESTRADOR CENTRAL (A triagem que faltava)
    private XplNode parseNode() {
        if (isAtEnd()) return null;

        // Triagem baseada no tipo de token
        if (check(HtmlTokenType.AT_IF)) return parseIfBlock();
        if (check(HtmlTokenType.AT_FOR)) return parseForBlock();
        if (check(HtmlTokenType.AT_SWITCH)) return parseSwitchBlock();
        if (check(HtmlTokenType.AT_MATCH)) return parseMatchBlock();
        if (check(HtmlTokenType.LT)) return parseElement();

        // Se for texto ou parênteses isolados (restos de condições do @if)
        if (check(HtmlTokenType.TEXT) || check(HtmlTokenType.LPAREN) || check(HtmlTokenType.RPAREN)) {
            XplNode textNode = new XplNode("text");
            textNode.textContent = advance().lexeme;
            return textNode;
        }

        // Se não soubermos o que é, avançamos para não bloquear
        advance();
        return null;
    }

    // =====================================================================
    // 🧬 BLOCO ESTRUTURAL: O @match (Padrão de Pattern Matching)
    // =====================================================================
    private XplNode parseMatchBlock() {
        consume(HtmlTokenType.AT_MATCH, "Esperado @match.");
        consume(HtmlTokenType.LPAREN, "Esperado '(' após @match.");
        String condition = captureExpression();
        consume(HtmlTokenType.RPAREN, "Esperado ')'.");

        XplNode matchNode = new XplNode("@match");
        matchNode.attributes.put("condition", condition);

        consume(HtmlTokenType.LBRACE, "Esperado '{'.");
        while (!check(HtmlTokenType.RBRACE) && !isAtEnd()) {
            if (check(HtmlTokenType.AT_ARM)) {
                matchNode.addChild(parseArmBlock());
            } else if (check(HtmlTokenType.AT_NONE)) {
                matchNode.addChild(parseNoneBlock());
            } else {
                advance(); // Ignora lixo ou espaços
            }
        }
        consume(HtmlTokenType.RBRACE, "Esperado '}'.");
        return matchNode;
    }

    private XplNode parseArmBlock() {
        consume(HtmlTokenType.AT_ARM, "Esperado @arm.");
        consume(HtmlTokenType.LPAREN, "Esperado '(' para o padrão do @arm.");
        String pattern = captureExpression();
        consume(HtmlTokenType.RPAREN, "Esperado ')'.");

        XplNode armNode = new XplNode("@arm");
        armNode.attributes.put("pattern", pattern);

        parseBlockContent(armNode);
        return armNode;
    }

    private XplNode parseNoneBlock() {
        consume(HtmlTokenType.AT_NONE, "Esperado @none.");

        XplNode noneNode = new XplNode("@none");

        parseBlockContent(noneNode);
        return noneNode;
    }

    // =====================================================================
    // 🧬 BLOCO ESTRUTURAL 1: O @if (Padrão Angular 17+)
    // =====================================================================
    private XplNode parseIfBlock() {
        consume(HtmlTokenType.AT_IF, "Esperado @if.");
        consume(HtmlTokenType.LPAREN, "Esperado '(' após @if.");

        // Captura a condição respeitando parênteses aninhados
        StringBuilder condition = new StringBuilder();
        int depth = 1;
        while (depth > 0 && !isAtEnd()) {
            if (check(HtmlTokenType.RPAREN) && depth == 1) break;
            HtmlToken token = advance();
            if (token.type == HtmlTokenType.LPAREN) depth++;
            else if (token.type == HtmlTokenType.RPAREN) depth--;
            condition.append(token.lexeme);
        }
        consume(HtmlTokenType.RPAREN, "Esperado ')' após a condição do @if.");

        XplNode ifNode = new XplNode("@if");
        ifNode.attributes.put("condition", condition.toString().trim());

        // ⭐ CORREÇÃO: O helper trata das chavetas { e } sozinho!
        parseBlockContent(ifNode);

        return ifNode;
    }

    // =====================================================================
    // 🔁 BLOCO ESTRUTURAL 2: O @for (Padrão Angular 17+)
    // =====================================================================
    private XplNode parseForBlock() {
        consume(HtmlTokenType.AT_FOR, "Esperado @for.");
        consume(HtmlTokenType.LPAREN, "Esperado '(' após @for.");
        String expr = captureExpression();
        consume(HtmlTokenType.RPAREN, "Esperado ')'.");

        XplNode forNode = new XplNode("@for");
        forNode.attributes.put("expression", expr);

        // ⭐ CORREÇÃO: Removemos os consumos manuais das chavetas!
        parseBlockContent(forNode);

        // Suporte ao @empty
        if (check(HtmlTokenType.AT_EMPTY)) {
            consume(HtmlTokenType.AT_EMPTY, "Esperado @empty.");
            XplNode emptyNode = new XplNode("@empty");

            // ⭐ CORREÇÃO: O helper trata das chavetas do emptyNode também!
            parseBlockContent(emptyNode);

            forNode.addChild(emptyNode);
        }
        return forNode;
    }

    // =====================================================================
    // 🧬 BLOCO ESTRUTURAL: O @default (O refúgio do Switch)
    // =====================================================================
    private XplNode parseDefaultBlock() {
        // 1. Consumimos a diretiva @default
        consume(HtmlTokenType.AT_DEFAULT, "Esperado @default.");

        // 2. Criamos o nó para este bloco
        XplNode defaultNode = new XplNode("@default");

        // 3. Reutilizamos o nosso helper 'parseBlockContent' para ler o que está dentro das chavetas {}
        // Isto garante que o default suporta div, botões, ou até blocos aninhados!
        parseBlockContent(defaultNode);

        return defaultNode;
    }

    private XplNode parseSwitchBlock() {
        consume(HtmlTokenType.AT_SWITCH, "Esperado @switch.");
        consume(HtmlTokenType.LPAREN, "Esperado '(' após @switch.");
        String condition = captureExpression(); // Reutiliza a lógica de capturar expressões
        consume(HtmlTokenType.RPAREN, "Esperado ')'.");

        XplNode switchNode = new XplNode("@switch");
        switchNode.attributes.put("condition", condition);

        consume(HtmlTokenType.LBRACE, "Esperado '{'.");
        while (!check(HtmlTokenType.RBRACE) && !isAtEnd()) {
            if (check(HtmlTokenType.AT_CASE)) {
                switchNode.addChild(parseCaseBlock());
            } else if (check(HtmlTokenType.AT_DEFAULT)) {
                switchNode.addChild(parseDefaultBlock());
            } else {
                advance(); // Ignora lixo
            }
        }
        consume(HtmlTokenType.RBRACE, "Esperado '}'.");
        return switchNode;
    }

    private XplNode parseCaseBlock() {
        consume(HtmlTokenType.AT_CASE, "Esperado @case.");
        consume(HtmlTokenType.LPAREN, "Esperado '('.");
        String val = captureExpression();
        consume(HtmlTokenType.RPAREN, "Esperado ')'.");

        XplNode caseNode = new XplNode("@case");
        caseNode.attributes.put("value", val);

        parseBlockContent(caseNode); // Método auxiliar que criaremos abaixo
        return caseNode;
    }

    // Captura expressões entre parênteses como "a == b" ou "let i of items"
    private String captureExpression() {
        StringBuilder expr = new StringBuilder();
        int depth = 1;
        while (depth > 0 && !isAtEnd()) {
            if (check(HtmlTokenType.RPAREN) && depth == 1) break;
            HtmlToken token = advance();
            if (token.type == HtmlTokenType.LPAREN) depth++;
            else if (token.type == HtmlTokenType.RPAREN) depth--;
            expr.append(token.lexeme).append(" ");
        }
        return expr.toString().trim();
    }

    // Preenche um nó pai com o conteúdo dentro de { ... }
    private void parseBlockContent(XplNode parent) {
        consume(HtmlTokenType.LBRACE, "Esperado '{'.");
        while (!check(HtmlTokenType.RBRACE) && !isAtEnd()) {
            XplNode child = parseNode();
            if (child != null) parent.addChild(child);
        }
        consume(HtmlTokenType.RBRACE, "Esperado '}'.");
    }


    // =====================================================================
    // ⚙️ FERRAMENTAS DE CONSUMO (IDÊNTICAS AO TEU PARSER XPL)
    // =====================================================================
    private boolean match(HtmlTokenType... types) {
        for (HtmlTokenType type : types) {
            if (check(type)) { advance(); return true; }
        }
        return false;
    }
    private boolean check(HtmlTokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }
    private boolean checkNext(HtmlTokenType type) {
        if (current + 1 >= tokens.size()) return false;
        return tokens.get(current + 1).type == type;
    }
    private HtmlToken advance() {
        if (!isAtEnd()) current++;
        return previous();
    }
    private boolean isAtEnd() { return peek().type == HtmlTokenType.EOF; }
    private HtmlToken peek() { return tokens.get(current); }
    private HtmlToken previous() { return tokens.get(current - 1); }
    private HtmlToken consume(HtmlTokenType type, String message) {
        if (check(type)) return advance();
        throw new RuntimeException("Erro Lexical (Linha " + peek().line + "): " + message);
    }
}