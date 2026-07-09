package com.dic.xsuper.lang.ui.html;

import java.util.ArrayList;
import java.util.List;

public class HtmlLexer {
    private final String source;
    private final List<HtmlToken> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;

    // ⭐ A MÁQUINA DE ESTADOS DO LEXER ⭐
    private boolean isInsideTag = false;


    // No teu HtmlLexer.java, o método auxiliar será algo assim:
    private int line = 1; // Variável global no Lexer que incrementa quando encontrares um '\n'


    public HtmlLexer(String source) {
        this.source = source;
    }

    public List<HtmlToken> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            if (isInsideTag) {
                scanTagContent(); // Se estamos dentro de um < ... >, lemos atributos e bindings
            } else {
                scanOuterContent(); // Se estamos fora, procuramos <, @ ou Texto Livre
            }
        }
        tokens.add(new HtmlToken(HtmlTokenType.EOF, "", null,line));
        return tokens;
    }


    // --- ESTADO 1: Fora das Tags (HTML Texto, <, e Diretivas @) ---
    private void scanOuterContent() {
        char c = advance();

        if (c == '<') {
            isInsideTag = true;
            addToken(HtmlTokenType.LT);
        } else if (c == '@') {
            scanDirective(); // Apanhámos um @if, @for, etc!
        } else if (c == '{') {
            addToken(HtmlTokenType.LBRACE);
        } else if (c == '}') {
            addToken(HtmlTokenType.RBRACE);
        } else if (c == '(' || c == ')') {
            addToken(c == '(' ? HtmlTokenType.LPAREN : HtmlTokenType.RPAREN); // Para ler as condições do @if(...)
        } else if (!isWhitespace(c)) {
            // Se não é nada disto, é texto bruto visível no ecrã!
            scanTextContent();
        }
    }


    // --- ESTADO 2: Dentro das Tags (id, class, [binding], (evento)) ---
    private void scanTagContent() {
        char c = advance();

        switch (c) {
            case '>':
                isInsideTag = false; // Saímos da tag!
                addToken(HtmlTokenType.GT);
                break;
            case '/': addToken(HtmlTokenType.SLASH); break;
            case '=': addToken(HtmlTokenType.EQUALS); break;
            case '"':
            case '\'':
                scanString(c);
                break;
            case '[': addToken(HtmlTokenType.LBRACKET); break;
            case ']': addToken(HtmlTokenType.RBRACKET); break;
            case '(': addToken(HtmlTokenType.LPAREN); break;
            case ')': addToken(HtmlTokenType.RPAREN); break;
            default:
                if (isAlpha(c)) {
                    scanIdentifier(); // Nome da tag ou de um atributo
                }
                break;
        }
    }

      private void scanDirective() {
        while (isAlphaNumeric(peek())) advance();
        String text = source.substring(start + 1, current); // Ignora o '@' no nome

        switch (text) {
            case "if": addToken(HtmlTokenType.AT_IF); break;
            case "elseif": addToken(HtmlTokenType.AT_ELSEIF); break;
            case "else": addToken(HtmlTokenType.AT_ELSE); break;
            case "for": addToken(HtmlTokenType.AT_FOR); break;
            case "switch": addToken(HtmlTokenType.AT_SWITCH); break;
            case "match": addToken(HtmlTokenType.AT_MATCH); break;
            case "case": addToken(HtmlTokenType.AT_CASE); break;
            case "default": addToken(HtmlTokenType.AT_DEFAULT); break;
            case "arm":   addToken(HtmlTokenType.AT_ARM); break;
            case "none":  addToken(HtmlTokenType.AT_NONE); break;
            case "empty":  addToken(HtmlTokenType.AT_EMPTY); break;
            default: throw new RuntimeException("Diretiva desconhecida: @" + text);
        }
    }



    private void addToken(HtmlTokenType type) {
        addToken(type, null);
    }

    private void addToken(HtmlTokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new HtmlToken(type, text, literal, line));
    }

    // =====================================================================
    // ⚙️ ENGRENAGENS INTERNAS DA MÁQUINA DE ESTADOS (UTILITIES)
    // =====================================================================

    /**
     * Verifica se já chegámos ao fim do ficheiro HTML.
     */
    private boolean isAtEnd() {
        return current >= source.length();
    }

    /**
     * Consome o caractere atual e avança a "agulha" de leitura 1 passo para a frente.
     */
    private char advance() {
        return source.charAt(current++);
    }

    /**
     * Espreita o caractere atual SEM avançar a agulha (Lookahead).
     */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    /**
     * Verifica se o caractere é um espaço em branco, tabulação ou quebra de linha.
     * Importante: Atualiza o contador de linhas para o rastreio de erros!
     */
    private boolean isWhitespace(char c) {
        if (c == '\n') {
            line++; // Atualizamos a linha global do teu Lexer
            return true;
        }
        return c == ' ' || c == '\r' || c == '\t';
    }

    /**
     * Verifica se é uma letra ou um 'underscore'.
     */
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    /**
     * Verifica se é um número.
     */
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Verifica se é alfanumérico.
     */
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    // =====================================================================
    // 🔍 EXTRATORES DE TOKENS COMPLEXOS
    // =====================================================================

    /**
     * Lê texto livre do HTML até encontrar uma nova tag '<', diretiva '@' ou bloco '{'.
     */
    private void scanTextContent() {
        // ⭐ CIRURGIA LÉXICA: Adicionámos o '(' e o ')' como paredes de colisão!
        while (!isAtEnd() && peek() != '<' && peek() != '@' && peek() != '{' && peek() != '}' && peek() != '(' && peek() != ')') {
            if (peek() == '\n') line++;
            advance();
        }

        // Extrai o texto capturado
        String text = source.substring(start, current);

        // Só adicionamos o token se não for um monte de espaços em branco vazios
        if (!text.trim().isEmpty()) {
            addToken(HtmlTokenType.TEXT, text.trim());
        }
    }

    /**
     * Lê identificadores (Nomes de Tags, Atributos ou Variáveis).
     * Ex: 'div', 'class', 'meu-componente'.
     */
    private void scanIdentifier() {
        // Em HTML e CSS, os identificadores podem ter traços '-' (ex: data-id="123")
        while (isAlphaNumeric(peek()) || peek() == '-') {
            advance();
        }

        // Guardamos a palavra exata que extraímos
        addToken(HtmlTokenType.IDENTIFIER);
    }

    /**
     * Lê uma string entre aspas (Ex: id="painel" ou src='foto.png').
     */
    private void scanString(char quoteType) {
        while (peek() != quoteType && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            throw new RuntimeException("Erro Lexical na linha " + line + ": Aspas (" + quoteType + ") não fechadas.");
        }

        advance(); // Consome a aspa de fecho

        // Extrai o valor LIMPO, sem as aspas à volta!
        String value = source.substring(start + 1, current - 1);
        addToken(HtmlTokenType.STRING, value);
    }

}