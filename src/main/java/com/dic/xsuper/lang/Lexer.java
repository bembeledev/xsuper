package com.dic.xsuper.lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int columnStart = 1; // Coluna onde o token atual começou
    private int currentColumn = 1;

    // Tabela de palavras-chave (Mapeia strings para os teus TokenTypes)
    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("let", TokenType.LET);
        keywords.put("var", TokenType.VAR);
        keywords.put("const", TokenType.CONST);
        keywords.put("fun", TokenType.FUN);
        keywords.put("for", TokenType.FOR);
        keywords.put("in", TokenType.IN);
        keywords.put("if", TokenType.IF);
        keywords.put("else", TokenType.ELSE);
        keywords.put("break", TokenType.BREAK);
        keywords.put("continue", TokenType.CONTINUE);
        keywords.put("int", TokenType.T_INT);
        keywords.put("bool", TokenType.T_BOOL);
        keywords.put("float", TokenType.T_FLOAT);
        keywords.put("string", TokenType.T_STRING);
        keywords.put("array", TokenType.T_ARRAY);
        keywords.put("object", TokenType.T_OBJECT);
        keywords.put("enum", TokenType.T_ENUM);
        keywords.put("return", TokenType.RETURN);
        // No local onde inicializas o teu map de keywords
        keywords.put("true", TokenType.TRUE);
        keywords.put("false", TokenType.FALSE);
        keywords.put("null", TokenType.NULL); // ou NIL, dependendo de como chamaste
        keywords.put("new", TokenType.NEW);
        // Estruturas de Orientação a Dados
        keywords.put("interface", TokenType.INTERFACE);
        keywords.put("declare", TokenType.DECLARE);
        keywords.put("implement", TokenType.IMPLEMENT);
        keywords.put("extends", TokenType.EXTENDS);
        keywords.put("this", TokenType.THIS);
        // Modificadores de Encapsulamento
        keywords.put("pub", TokenType.PUB);
        keywords.put("prot", TokenType.PROT);
        keywords.put("priv", TokenType.PRIV);
        keywords.put("as", TokenType.AS);
        keywords.put("abstract", TokenType.ABSTRACT);
        keywords.put("super", TokenType.SUPER);
        keywords.put("static", TokenType.STATIC);
        keywords.put("default", TokenType.DEFAULT);
        keywords.put("try", TokenType.TRY);
        keywords.put("catch", TokenType.CATCH);
        keywords.put("finally", TokenType.FINALLY);
        keywords.put("throw", TokenType.THROW);
        keywords.put("throws", TokenType.THROWS);
        keywords.put("readonly", TokenType.READONLY);
        keywords.put("final", TokenType.FINAL);
        keywords.put("typeof", TokenType.TYPEOF);
        keywords.put("type", TokenType.TYPE);
        keywords.put("instance", TokenType.INSTANCE);
        keywords.put("switch", TokenType.SWITCH);
        keywords.put("case", TokenType.CASE);
        keywords.put("none", TokenType.NONE);
        keywords.put("match", TokenType.MATCH);
        keywords.put("decorator", TokenType.DECORATOR);
        keywords.put("module", TokenType.MODULE);
        keywords.put("import", TokenType.IMPORT);
        keywords.put("export", TokenType.EXPORT);
        keywords.put("all", TokenType.ALL);
        keywords.put("prefix", TokenType.PREFIX);
        keywords.put("global", TokenType.GLOBAL);

    }

    public Lexer(String source) {
        this.source = source;
    }

    public List<Token> tokenize() {
        while (!isAtEnd()) {
            start = current;
            columnStart = currentColumn;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line, currentColumn));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(TokenType.LPAREN); break;
            case ')': addToken(TokenType.RPAREN); break;
            case '{': addToken(TokenType.LBRACE); break;
            case '}': addToken(TokenType.RBRACE); break;
            case '[': addToken(TokenType.LBRACKET); break;
            case ']': addToken(TokenType.RBRACKET); break;
            case ',': addToken(TokenType.COMMA); break;
            case '.': addToken(TokenType.DOT); break;
            case ':': addToken(TokenType.COLON); break;
            case ';': addToken(TokenType.SEMICOLON); break;
            case '&':
                addToken(match('&') ? TokenType.AND : TokenType.BIT_AND);
                break;
            case '@': addToken(TokenType.AT); break;
            case '|':
                addToken(match('|') ? TokenType.OR : TokenType.BIT_OR);
                break;
            case '^':
                addToken(TokenType.BIT_XOR); // Bónus: Ou Exclusivo (XOR) de bits
                break;
            case '-': {
                if (match('=')) addToken(TokenType.MINUS_ASSIGN);
                else if (match('-')) addToken(TokenType.MINUS_MINUS);
                else if (match('>')) addToken(TokenType.ARROW);
                else addToken(TokenType.MINUS);
            }; break;
            case '+': {
               if(match('=')) addToken(TokenType.PLUS_ASSIGN);
               else if (match('+')) addToken(TokenType.PLUS_PLUS);
               else addToken(TokenType.PLUS);
            } break;
            case '*': {
                if (match('=')) addToken(TokenType.STAR_ASSIGN);
                else if (match('*')) addToken(TokenType.POWER);
                else addToken(TokenType.STAR);
            } break;
            case '#': {
                if (match('=')) addToken(TokenType.HASH_ASSIGN);
                else addToken(TokenType.HASH);
            } break;
            case '%': {
                if (match('=')) addToken(TokenType.MODULO_ASSIGN);
                else addToken(TokenType.MODULO);
            } break;
            case '/':
                if (match('/')) { // Comentário de linha (ex: // isto é um comentário)
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('=')) {
                    addToken(TokenType.SLASH_ASSIGN);
                } else {
                    addToken(TokenType.SLASH);
                }
            break;
            case '=': {
                if (match('=')) {
                    // ⭐ Já leu '=='. Será que vem um terceiro '=' para formar '==='?
                    if (match('=')) {
                        addToken(TokenType.STRICT_EQUAL);     // ===
                    } else {
                        addToken(TokenType.EQUAL);            // ==
                    }
                } else if (match('>')) {
                    addToken(TokenType.FAT_ARROW);            // =>
                } else {
                    addToken(TokenType.ASSIGN);               // =
                }
                break;
            }

            case '!': {
                if (match('=')) {
                    // ⭐ Já leu '!='. Será que vem um terceiro '=' para formar '!=='?
                    if (match('=')) {
                        addToken(TokenType.STRICT_NOT_EQUAL); // !==
                    } else {
                        addToken(TokenType.NOT_EQUAL);        // !=
                    }
                } else {
                    addToken(TokenType.BANG);                 // !
                }
                break;
            }
            case '?':
                if (match('?')) addToken(TokenType.QUESTION_QUESTION);
                else if (match('.')) addToken(TokenType.QUESTION_DOT);
                else addToken(TokenType.QUESTION); // O question mark normal dos tipos (?string)
                break;
            case '<':
                if (match('<')) addToken(TokenType.SHIFT_LEFT);
                else if (match('=')) addToken(TokenType.LESS_EQUAL);
                else addToken(TokenType.LESS);
                break;
            case '>':
                if (match('>')) addToken(TokenType.SHIFT_RIGHT);
                else if (match('=')) addToken(TokenType.GREATER_EQUAL);
                else addToken(TokenType.GREATER);
                break;

            // Ignorar espaços e quebras de linha
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line++;
                currentColumn = 1;
                break;

            // Captura de Strings
            case '"': string(); break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    // Erro Léxico
                    System.err.println("Erro Léxico na linha " + line + ", coluna " + currentColumn + ": Caractere inesperado '" + c + "'.");
                    addToken(TokenType.ERROR);
                }
                break;
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring((int) start, (int) current);
        TokenType type = keywords.get(text);
        if (type == null) type = TokenType.IDENTIFIER;

        addToken(type);
    }

    private void number() {
        boolean isFloat = false;
        while (isDigit(peek())) advance();

        // Procura pela parte decimal
        if (peek() == '.' && isDigit(peekNext())) {
            isFloat = true;
            advance(); // Consome o '.'
            while (isDigit(peek())) advance();
        }

        String value = source.substring(start, current);
        if (isFloat) {
            addToken(TokenType.FLOAT_LITERAL, Double.parseDouble(value));
        } else {
            addToken(TokenType.INT_LITERAL, Long.parseLong(value));
        }
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
                currentColumn = 1; // Genial! Mantém o rastreio da coluna perfeito.
            }
            advance();
        }

        if (isAtEnd()) {
            System.err.println("Erro Léxico na linha " + line + ": String não terminada.");
            return;
        }

        advance(); // Consome as aspas de fecho (")

        // 1. Retira as aspas do valor real (Texto cru)
        String value = source.substring(start + 1, current - 1);

        // ⭐ 2. A MAGIA DAS SEQUÊNCIAS DE ESCAPE ⭐
        // Traduz os caracteres literais \ e n para um ENTER de verdade, etc.
        value = value.replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");

        // 3. Guarda o token formatado!
        addToken(TokenType.STRING_LITERAL, value);
    }

    // --- Métodos Auxiliares de Varredura (Iguais aos do Rust) ---

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        currentColumn++;
        return true;
    }

    private char advance() {
        currentColumn++;
        return source.charAt(current++);
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line, columnStart));
    }
}