package com.dic.xsuper.dom.html;
import com.dic.xsuper.dom.node.XplNode;

import java.util.ArrayList;
import java.util.List;

import static com.dic.xsuper.dom.html.HtmlTagUtils.EMPTY_TAGS;

public class HtmlParser {
    private final List<HtmlToken> tokens;
    private int current = 0;

    // ⭐ ESTADO DO DOCUMENTO ⭐
    private boolean htmlFound = false;
    private boolean headFound = false;
    private boolean bodyFound = false;

    public HtmlParser(List<HtmlToken> tokens) {
        this.tokens = tokens;
    }

    // O ponto de entrada. Retorna o nó RAIZ (ex: um <body> ou <main> invisível que guarda tudo)
    // O ponto de entrada. Retorna apenas um Fragmento Invisível (<root>)
    public XplNode parse() {
        XplNode root = new XplNode("root");
        while (!isAtEnd()) {
            XplNode child = parseNode(); // Usa o teu excelente orquestrador!
            if (child != null) {
                root.addChild(child);
            }
        }
        return root;
    }

    // ⭐ O CORAÇÃO DO PARSER: Lê uma tag completa e os seus filhos!
    private XplNode parseElement() {
        consume(HtmlTokenType.LT, "Esperado '<' para iniciar a tag.");

        HtmlToken tagToken = consume(HtmlTokenType.IDENTIFIER, "Esperado nome da tag após '<'.");
        String tagName = tagToken.lexeme;
        XplNode node;

        // ⭐ LÓGICA DE VALIDAÇÃO ESTRICTA ⭐
        switch (tagName) {
            case "html" -> {
                if (htmlFound) throw new RuntimeException("Erro: Tag <html> já foi declarada neste documento.");
                htmlFound = true;
            }
            case "head" -> {
                if (headFound) throw new RuntimeException("Erro: Tag <head> já foi declarada neste documento.");
                headFound = true;
            }
            case "body" -> {
                if (bodyFound) throw new RuntimeException("Erro: Tag <body> já foi declarada neste documento.");
                bodyFound = true;
            }
        }

        if (HtmlTagUtils.isNativeTag(tagName)) {
            node = new XplNode(tagName);
        } else if (HtmlTagUtils.isValidCustomTagName(tagName)) {
            node = new XplNode("@component");
            node.attributes.put("componentName", tagName);
        } else {
            throw new RuntimeException("Tag personalizada inválida: '" + tagName +
                    "'. Deve usar PascalCase (ex: MeuBotao) ou kebab-case (ex: meu-botao).");
        }

        // 1. EXTRAIR PROPRIEDADES (Atributos, Eventos, Bindings)
        while (!check(HtmlTokenType.GT) && !check(HtmlTokenType.SLASH) && !isAtEnd()) {

            // ⭐ EVENTO SINTAXE ANGULAR: (click)="funcao"
            if (match(HtmlTokenType.LPAREN)) {
                String eventName = consume(HtmlTokenType.IDENTIFIER, "Esperado nome do evento.").lexeme;
                consume(HtmlTokenType.RPAREN, "Esperado ')' após o nome do evento.");
                consume(HtmlTokenType.EQUALS, "Esperado '=' após o evento.");
                String action = consume(HtmlTokenType.STRING, "Esperado ação do evento em aspas.").literal.toString();

                // Guarda diretamente (ex: "click")
                node.events.put(eventName.toLowerCase(), action);
            }
            // É um DATA BINDING? [value]="var"
            else if (match(HtmlTokenType.LBRACKET)) {
                String bindName = consume(HtmlTokenType.IDENTIFIER, "Esperado nome do binding.").lexeme;
                consume(HtmlTokenType.RBRACKET, "Esperado ']' após o nome do binding.");
                consume(HtmlTokenType.EQUALS, "Esperado '='.");
                String varName = consume(HtmlTokenType.STRING, "Esperado variável do binding em aspas.").literal.toString();
                node.bindings.put(bindName, varName);
            }
            // ⭐ ATRIBUTO NORMAL, EVENTO OU BINDING REACT: id="painel" ou class={minhaVar}
            else if (check(HtmlTokenType.IDENTIFIER)) {
                String attrName = advance().lexeme;
                if (match(HtmlTokenType.EQUALS)) {

                    // =============================================================
                    // ⚛️ MODO REACT: Suporta attr={variavel} ou attr={{variavel}}
                    // =============================================================
                    if (match(HtmlTokenType.LBRACE)) {
                        boolean isDouble = match(HtmlTokenType.LBRACE);

                        StringBuilder sb = new StringBuilder();
                        // Lê tudo até encontrar a chave de fecho
                        while (!isAtEnd() && !check(HtmlTokenType.RBRACE)) {
                            sb.append(advance().lexeme);
                        }
                        String bindValue = sb.toString().trim();

                        consume(HtmlTokenType.RBRACE, "Esperado '}' no fecho do binding React.");
                        if (isDouble) consume(HtmlTokenType.RBRACE, "Esperado segundo '}' no fecho do binding duplo.");

                        // ⭐ A JOGADA DE MESTRE: Transformamos isto num Data Binding Dinâmico!
                        // A Engine vai avaliá-lo sempre no renderCycle() sem sofrer amnésia.
                        node.bindings.put(attrName, bindValue);
                    }
                    // =============================================================
                    // 📜 MODO STRING W3C: Suporta " ", ' ' e ` `
                    // =============================================================
                    else {
                        String attrValue = consume(HtmlTokenType.STRING, "Esperado valor em aspas ou binding {...}.").literal.toString();

                        if (attrName.toLowerCase().startsWith("on") && attrName.length() > 2) {
                            String eventName = attrName.substring(2).toLowerCase();
                            node.events.put(eventName, attrValue);
                        } else if (attrName.equals("id")) {
                            node.id = attrValue;
                        } else if (attrName.equals("class")) {
                            node.className = attrValue;
                        } else {
                            node.attributes.put(attrName, attrValue);
                        }
                    }
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

        List<XplNode> children = new ArrayList<>();

        // 3. LER CONTEÚDO INTERNO E FILHOS (RECURSÃO ATUALIZADA!)
        while (!isAtEnd()) {
            // Se encontrámos '</', significa que a NOSSA tag está a fechar!
            if (check(HtmlTokenType.LT) && checkNext(HtmlTokenType.SLASH)) {
                break;
            }

            // ⭐ CHAMAMOS O ORQUESTRADOR EM VEZ DE APENAS PARSE ELEMENT ⭐
            XplNode child = parseNode();
            if (child != null && (!child.tag.equals("#text") || (child.textContent != null && !child.textContent.isEmpty()))) {
                children.add(child);
            }
        }
        // ⭐ Validar se a tag permite filhos
        if (EMPTY_TAGS.contains(node.tag)) {
            if (!children.isEmpty()) {
                System.err.println("[Aviso] A tag <" + node.tag + "> não suporta elementos filhos. Os filhos serão ignorados.");
                // Não adicionar os filhos ao nó
            }
        } else {
            // Adicionar filhos ao nó
            for (XplNode child : children) {
                node.addChild(child);
            }
        }

        // Para tags que são sempre vazias, ignorar qualquer texto
        if (EMPTY_TAGS.contains(node.tag) && !node.textContent.isEmpty()) {
            System.err.println("[Aviso] A tag <" + node.tag + "> não suporta texto. O texto '" + node.textContent + "' será ignorado.");
            node.textContent = "";
        }

        // 4. FECHAR A TAG (Ex: </div> ou </Element>)
        consume(HtmlTokenType.LT, "Esperado '<' para fechar a tag.");
        consume(HtmlTokenType.SLASH, "Esperado '/' no fecho da tag.");
        HtmlToken closeTag = consume(HtmlTokenType.IDENTIFIER, "Esperado nome da tag no fecho.");

        // ⭐ A CORREÇÃO DE MESTRE: Qual é o verdadeiro nome esperado? ⭐
        String expectedCloseTag = node.tag.equals("@component") ?
                node.attributes.get("componentName").toString() :
                node.tag;

        if (!closeTag.lexeme.equals(expectedCloseTag)) {
            throw new RuntimeException("Erro na linha " + closeTag.line + ": Fecho de tag incorreto. Esperado '</" + expectedCloseTag + ">', mas encontrou '</" + closeTag.lexeme + ">'.");
        }

        consume(HtmlTokenType.GT, "Esperado '>' para finalizar a tag.");

        return node;
    }


    // =====================================================================
    // ⭐ O ORQUESTRADOR CENTRAL (A triagem inteligente)
    // =====================================================================
    private XplNode parseNode() {
        if (isAtEnd()) return null;

        // 1. É uma diretiva estrutural?
        if (check(HtmlTokenType.AT_IF)) return parseIfBlock();
        if (check(HtmlTokenType.AT_FOR)) return parseForBlock();
        if (check(HtmlTokenType.AT_SWITCH)) return parseSwitchBlock();
        if (check(HtmlTokenType.AT_MATCH)) return parseMatchBlock();

        // 2. É uma nova Tag HTML? (ex: <div>)
        if (check(HtmlTokenType.LT)) {
            // Se for o fecho de uma tag (ex: </div>), saímos para o pai lidar com isso!
            if (checkNext(HtmlTokenType.SLASH)) {
                return null;
            }
            return parseElement();
        }

        // 3. É um '}' solitário a fechar um bloco @for ou @if?
        if (check(HtmlTokenType.RBRACE)) {
            return null; // Deixa o parseBlockContent fechar o bloco
        }

        // 4. Se não é nada do que está acima, SÓ PODE SER TEXTO (incluindo {{ bindings }})!
        return parseTextNode();
    }

    // =====================================================================
    // 📝 O CAPTURADOR DE TEXTO (Apanha tudo até encontrar uma Tag ou Diretiva)
    // =====================================================================
    private XplNode parseTextNode() {
        XplNode textNode = new XplNode("#text");
        StringBuilder sb = new StringBuilder();

        // Consome tokens até bater numa parede (Tag, Diretiva ou Fim de Bloco)
        while (!isAtEnd()) {
            HtmlTokenType type = peek().type;

            // Bateu numa parede? (Início de tag ou diretiva)
            if (type == HtmlTokenType.LT || type == HtmlTokenType.AT_IF ||
                    type == HtmlTokenType.AT_FOR || type == HtmlTokenType.AT_SWITCH ||
                    type == HtmlTokenType.AT_MATCH || type == HtmlTokenType.AT_EMPTY ||
                    type == HtmlTokenType.AT_ELSE || type == HtmlTokenType.AT_ELSEIF) {
                break;
            }

            // O nosso Lexer pode ver as chavetas como blocos. Temos de tratar disso:
            if (type == HtmlTokenType.RBRACE) {
                // Se o próximo também é '}', então é um fecho de binding '}}'!
                if (checkNext(HtmlTokenType.RBRACE)) {
                    sb.append(advance().lexeme);
                    sb.append(advance().lexeme);
                    continue; // Continua a ler o resto do texto
                } else {
                    // É um '}' solitário. Significa que um bloco @for ou @if acabou.
                    break;
                }
            }

            // Vai acumulando o texto (identificadores, espaços, '{', etc)
            sb.append(advance().lexeme);
        }

        textNode.textContent = sb.toString().trim();
        return textNode;
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
    // =====================================================================
    // 🧬 BLOCO ESTRUTURAL 1: O @if (Com suporte a @else if e @else)
    // =====================================================================
    private XplNode parseIfBlock() {
        consume(HtmlTokenType.AT_IF, "Esperado @if.");
        consume(HtmlTokenType.LPAREN, "Esperado '(' após @if.");

        String condition = captureExpression();
        consume(HtmlTokenType.RPAREN, "Esperado ')' após a condição do @if.");

        XplNode ifNode = new XplNode("@if");
        ifNode.attributes.put("condition", condition);

        // Parse do bloco { ... } principal
        parseBlockContent(ifNode);

        // ⭐ NOVO: Suporte encadeado para @else if e @else ⭐
        while (check(HtmlTokenType.AT_ELSE) || check(HtmlTokenType.AT_ELSEIF)) {

            // Se for @else if (ou equivalente no teu lexer)
            if (check(HtmlTokenType.AT_ELSEIF)) {
                consume(HtmlTokenType.AT_ELSEIF, "Esperado @else if.");
                consume(HtmlTokenType.LPAREN, "Esperado '('.");
                String elseIfCond = captureExpression();
                consume(HtmlTokenType.RPAREN, "Esperado ')'.");

                XplNode elseIfNode = new XplNode("@elseif");
                elseIfNode.attributes.put("condition", elseIfCond);
                parseBlockContent(elseIfNode);
                ifNode.addChild(elseIfNode); // Guarda dentro do nó IF principal
            }
            // Se for apenas @else
            else if (check(HtmlTokenType.AT_ELSE)) {
                consume(HtmlTokenType.AT_ELSE, "Esperado @else.");

                XplNode elseNode = new XplNode("@else");
                parseBlockContent(elseNode);
                ifNode.addChild(elseNode); // Guarda dentro do nó IF principal
                break; // O @else simples fecha obrigatoriamente a cadeia!
            }
        }

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