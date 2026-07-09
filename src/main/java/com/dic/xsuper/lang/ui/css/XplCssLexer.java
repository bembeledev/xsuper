package com.dic.xsuper.lang.ui.css;

import java.util.ArrayList;
import java.util.List;

public class XplCssLexer {
    private final String source;
    private final List<XplCssToken> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private boolean isValueMode = false;

    public XplCssLexer(String source) {
        this.source = source;
    }

    public List<XplCssToken> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new XplCssToken(XplCssTokenType.EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();

        switch (c) {
            // ─── Símbolos estruturais ────────────────────────────────────
            case '(': addToken(XplCssTokenType.LPAREN); break;
            case ')': addToken(XplCssTokenType.RPAREN); break;
            case '{': addToken(XplCssTokenType.LBRACE); isValueMode = false; break;
            case '}': addToken(XplCssTokenType.RBRACE); isValueMode = false; break;
            case '[': addToken(XplCssTokenType.LBRACKET); break;
            case ']': addToken(XplCssTokenType.RBRACKET); break;
            case ',': addToken(XplCssTokenType.COMMA); break;
            case ':': addToken(XplCssTokenType.COLON); isValueMode = true; break;
            case ';': addToken(XplCssTokenType.SEMICOLON); isValueMode = false; break;
            case '.':
            case '#':
                // Consome o ponto/cardinal e o identificador seguinte
                while (isAlphaNumeric(peek()) || peek() == '-' || peek() == '_') advance();
                addToken(XplCssTokenType.SELECTOR, source.substring(start, current));
                break;

            // ─── Operadores de dois caracteres ──────────────────────────
            case '=':
                if (match('=')) {
                    addToken(XplCssTokenType.EQUAL);
                } else {
                    addToken(XplCssTokenType.ASSIGN);
                }
                break;
            case '!':
                if (match('=')) {
                    addToken(XplCssTokenType.NOT_EQUAL);
                } else {
                    addToken(XplCssTokenType.BANG); // se quiseres ! como operador lógico
                }
                break;
            case '<':
                if (match('=')) {
                    addToken(XplCssTokenType.LESS_EQUAL);
                } else {
                    addToken(XplCssTokenType.LESS);
                }
                break;
            case '>':
                if (match('=')) {
                    addToken(XplCssTokenType.GREATER_EQUAL);
                } else {
                    addToken(XplCssTokenType.GREATER);
                }
                break;
            case '+':
                if (match('+')) {
                    addToken(XplCssTokenType.PLUS_PLUS);
                } else if (match('=')) {
                    addToken(XplCssTokenType.PLUS_ASSIGN);
                } else {
                    addToken(XplCssTokenType.PLUS);
                }
                break;
            case '-':
                if (match('-')) {
                    addToken(XplCssTokenType.MINUS_MINUS);
                } else if (match('=')) {
                    addToken(XplCssTokenType.MINUS_ASSIGN);
                } else {
                    addToken(XplCssTokenType.MINUS);
                }
                break;
            case '*':
                if (match('*')) {
                    addToken(XplCssTokenType.POWER);
                } else if (match('=')) {
                    addToken(XplCssTokenType.STAR_ASSIGN);
                } else {
                    addToken(XplCssTokenType.STAR);
                }
                break;
            case '/':
                if (match('/')) {
                    scanLineComment();
                } else if (match('*')) {
                    scanBlockComment();
                } else if (match('=')) {
                    addToken(XplCssTokenType.SLASH_ASSIGN);
                } else {
                    addToken(XplCssTokenType.SLASH);
                }
                break;
            case '%':
                if (match('=')) {
                    addToken(XplCssTokenType.MODULO_ASSIGN);
                } else {
                    addToken(XplCssTokenType.MODULO);
                }
                break;

            // ─── Strings ─────────────────────────────────────────────────
            case '"':
            case '\'':
                scanString(c);
                break;

            // ─── Directivas @ ──────────────────────────────────────────
            case '@':
                scanDirective();
                break;

            // ─── Números e identificadores ─────────────────────────────
            default:
                if (Character.isWhitespace(c)) {
                    // ignora
                } else if (Character.isDigit(c)) {
                    scanNumber();
                } else {
                    if (isAlpha(c) || c == '_') {
                        scanWord(); // <- agora chama scanWord() em vez de scanIdentifier()
                    } else {
                        throw new RuntimeException("Caractere inesperado na linha " + line + ": " + c);
                    }
                }
                break;
        }
    }
    // ---- Métodos auxiliares ----


    // ⭐ 4. ADICIONA ESTES 2 NOVOS MÉTODOS MÁGICOS NO FUNDO DA CLASSE:
    private void scanWord() {
        while (isAlphaNumeric(peek()) || peek() == '-' || peek() == '_') {
            advance();
        }
        String text = source.substring(start, current);

        // Se estamos em modo valor (após ':'), é IDENTIFIER (variável)
        if (isValueMode) {
            addToken(XplCssTokenType.IDENTIFIER, text);
            return;
        }

        // Fora do modo valor: verifica o próximo token (ignorando espaços)
        int temp = current;
        while (temp < source.length() && Character.isWhitespace(source.charAt(temp))) temp++;
        if (temp < source.length() && source.charAt(temp) == ':') {
            addToken(XplCssTokenType.PROPERTY, text);
        } else {
            // Se começar com '.' ou '#', é seletor; mas o scanIdentifier já pode incluir esses
            // Para simplificar, consideramos SELECTOR
            addToken(XplCssTokenType.SELECTOR, text);
        }
    }

    // O "Raio-X": Verifica se a declaração termina num Bloco ou num Ponto e Vírgula
    private boolean isHeadingTowardsBrace(int fromIndex) {
        for (int i = fromIndex; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') return true;
            if (c == ';' || c == '}') return false;
        }
        return false;
    }

    private void scanDirective() {
        while (isAlphaNumeric(peek()) || peek() == '-') advance();
        String text = source.substring(start + 1, current); // sem o '@'

        XplCssTokenType type;
        switch (text) {
            case "if": type = XplCssTokenType.AT_IF; break;
            case "elseif": type = XplCssTokenType.AT_ELSEIF; break;
            case "else": type = XplCssTokenType.AT_ELSE; break;
            case "for": type = XplCssTokenType.AT_FOR; break;
            case "switch": type = XplCssTokenType.AT_SWITCH; break;
            case "case": type = XplCssTokenType.AT_CASE; break;
            case "default": type = XplCssTokenType.AT_DEFAULT; break;
            case "match": type = XplCssTokenType.AT_MATCH; break;
            case "arm": type = XplCssTokenType.AT_ARM; break;
            case "none": type = XplCssTokenType.AT_NONE; break;
            case "empty": type = XplCssTokenType.AT_EMPTY; break;
            // Suporte a futuras diretivas CSS nativas
            case "media": type = XplCssTokenType.AT_MEDIA; break;
            case "keyframes": type = XplCssTokenType.AT_KEYFRAMES; break;
            case "import": type = XplCssTokenType.AT_IMPORT; break;
            case "extend": type = XplCssTokenType.AT_EXTEND; break;
            default:
                throw new RuntimeException("Diretiva CSS desconhecida na linha " + line + ": @" + text);
        }
        addToken(type);
    }

    private void scanString(char quote) {
        while (peek() != quote && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }
        if (isAtEnd()) {
            throw new RuntimeException("String não fechada na linha " + line);
        }
        advance(); // consome a aspa de fecho
        String value = source.substring(start + 1, current - 1);
        addToken(XplCssTokenType.STRING, value);
    }

    private void scanNumber() {
        while (Character.isDigit(peek())) advance();
        if (peek() == '.' && Character.isDigit(peekNext())) {
            advance();
            while (Character.isDigit(peek())) advance();
        }
        // Suporte a unidades (px, em, %, etc.)
        StringBuilder unit = new StringBuilder();
        while (isAlpha(peek()) || peek() == '%') {
            unit.append(advance());
        }
        String number = source.substring(start, current);
        addToken(XplCssTokenType.NUMBER, number);
    }

    private void scanIdentifier() {
        while (isAlphaNumeric(peek()) || peek() == '-' || peek() == '_' || peek() == '.') {
            advance();
        }
        String text = source.substring(start, current);
        addToken(XplCssTokenType.IDENTIFIER, text);
    }

    private void scanLineComment() {
        while (peek() != '\n' && !isAtEnd()) advance();
    }

    private void scanBlockComment() {
        while (!(peek() == '*' && peekNext() == '/') && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }
        if (isAtEnd()) {
            throw new RuntimeException("Comentário de bloco não fechado na linha " + line);
        }
        advance(); // '*'
        advance(); // '/'
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private char advance() {
        if (isAtEnd()) return '\0';
        char c = source.charAt(current);
        current++;
        if (c == '\n') line++;
        return c;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c) || c == '.';
    }

    private void addToken(XplCssTokenType type) {
        addToken(type, null);
    }

    private void addToken(XplCssTokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new XplCssToken(type, text, literal, line));
    }
}