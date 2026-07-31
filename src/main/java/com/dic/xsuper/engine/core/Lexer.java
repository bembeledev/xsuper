package com.dic.xsuper.engine.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private final String filePath; // ⭐ NOVO
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int columnStart = 1; // Coluna onde o token actual começou
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
        keywords.put("while", TokenType.WHILE);
        keywords.put("do", TokenType.DO);
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
        keywords.put("use", TokenType.USE);
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
        keywords.put("pub", TokenType.PUBLIC);
        keywords.put("prot", TokenType.PROTECTED);
        keywords.put("priv", TokenType.PRIVATE);
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
        keywords.put("listener", TokenType.LISTENER);
        keywords.put("module", TokenType.MODULE);
        keywords.put("import", TokenType.IMPORT);
        keywords.put("export", TokenType.EXPORT);
        keywords.put("all", TokenType.ALL);
        keywords.put("prefix", TokenType.PREFIX);
        keywords.put("global", TokenType.GLOBAL);
        keywords.put("sealed", TokenType.SEALED);

    }

    public Lexer(String source, String filePath) {
        this.source = source;
        this.filePath = filePath;
    }

    public List<Token> tokenize() {
        while (!isAtEnd()) {
            start = current;
            columnStart = currentColumn;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line, currentColumn,this.filePath));
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
            case '.':
                // Verifica se os próximos dois caracteres também são pontos
                if (match('.')) {
                    if (match('.')) {
                        addToken(TokenType.SPREAD);
                    } else {
                        // ERRO FATAL: Dois pontos sozinhos não significam nada no XPL
                        //Xpl.error(line, "Token inesperado '..'. Usar '.' para propriedades ou '...' para spread.");
                        System.err.println("Aviso Léxico (L" + line + "): Ficheiro terminou com um bloco de comentário '/*' não fechado.");
                    }
                } else {
                    // É apenas um ponto normal (ex: obj.propriedade)
                    addToken(TokenType.DOT);
                }
                break;
            case ':':
                if (match(':')) {
                    addToken(TokenType.DOUBLE_COLON); // É um ::
                } else {
                    addToken(TokenType.COLON); // É apenas um :
                }
                break;
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
            }
            break;
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
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('*')) {

                    boolean commentClosed = false; // ⭐ A NOSSA FLAG INTELIGENTE

                    while (!isAtEnd()) {
                        if (peek() == '\n') {
                            line++;
                            currentColumn = 0;
                        }

                        if (peek() == '*' && peekNext() == '/') {
                            advance(); // Engole o '*'
                            advance(); // Engole o '/'
                            commentClosed = true; // ⭐ Marca como fechado com sucesso!
                            break;
                        }
                        advance();
                    }

                    // ⭐ Agora só avisa se o ficheiro acabou e a flag continua false!
                    if (!commentClosed) {
                        System.err.println("Aviso Léxico (L" + line + "): Ficheiro terminou com um bloco de comentário '/*' não fechado.");
                    }

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

        String text = source.substring(start, current);
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

    // =========================================================================
    // ⭐ VOLUME 14: STRINGS MULTI-LINHA E INTERPOLAÇÃO MÁGICA (${...}) ⭐
    // =========================================================================
    private void string() {
        // O primeiro '"' já foi consumido pelo switch no scanToken()
        if (match('"')) {
            if (match('"')) {
                parseMultiLineString();
            } else {
                // Era apenas uma string vazia simples ""
                tokens.add(new Token(TokenType.STRING_LITERAL, "", "", line, 0,this.filePath));
            }
        } else {
            parseNormalString();
        }
    }

    private void parseMultiLineString() {
        int startLine = this.line;
        StringBuilder rawContent = new StringBuilder();

        while (!isAtEnd()) {
            // Deteta o fecho com as 3 aspas (""")
            if (current + 2 < source.length() &&
                    source.charAt(current) == '"' &&
                    source.charAt(current + 1) == '"' &&
                    source.charAt(current + 2) == '"') {
                break;
            }
            char c = advance();
            if (c == '\n') this.line++;
            rawContent.append(c);
        }

        if (isAtEnd()) {
            System.err.println("Erro Léxico (L" + line + "): String multi-linha não fechada.");
            return;
        }

        advance(); advance(); advance(); // Consome as 3 aspas de fecho

        // 1. Limpeza de espaços (O Comportamento Genuíno de Text Blocks do Java)
        String cleanedContent = cleanJavaTextBlock(rawContent.toString());

        // 2. Resolve a interpolação e injeta os tokens matemáticos no compilador
        processInterpolation(cleanedContent, startLine);
    }

    private void parseNormalString() {
        int startLine = this.line;
        StringBuilder rawContent = new StringBuilder();

        while (!isAtEnd() && peek() != '"') {
            if (peek() == '\n') line++;

            // Processamento de escapes clássicos
            if (peek() == '\\') {
                advance(); // consome a barra
                if (isAtEnd()) break;
                char c = advance();
                switch (c) {
                    case 'n': rawContent.append('\n'); break;
                    case 't': rawContent.append('\t'); break;
                    case 'r': rawContent.append('\r'); break;
                    case '\\': rawContent.append('\\'); break;
                    case '"': rawContent.append('"'); break;
                    case '$': rawContent.append('$'); break;
                    default: rawContent.append('\\').append(c); break;
                }
            } else {
                rawContent.append(advance());
            }
        }

        if (isAtEnd()) {
            System.err.println("Erro Léxico (L" + line + "): String não fechada.");
            return;
        }

        advance(); // Consome a aspa de fecho (")

        processInterpolation(rawContent.toString(), startLine);
    }

    // ⭐ Lógica Estilo Java: Remove a indentação vazia comum e a primeira quebra de linha
    private String cleanJavaTextBlock(String raw) {
        String[] lines = raw.split("\r?\n", -1);
        int minIndent = Integer.MAX_VALUE;

        for (int i = 0; i < lines.length; i++) {
            String l = lines[i];
            if (i == 0 && l.trim().isEmpty()) continue; // Ignora a 1ª linha vazia

            int indent = 0;
            while (indent < l.length() && (l.charAt(indent) == ' ' || l.charAt(indent) == '\t')) {
                indent++;
            }
            if (!l.trim().isEmpty() || i == lines.length - 1) {
                if (indent < minIndent) minIndent = indent;
            }
        }

        if (minIndent == Integer.MAX_VALUE) minIndent = 0;

        StringBuilder sb = new StringBuilder();
        int startIdx = (lines.length > 0 && lines[0].trim().isEmpty()) ? 1 : 0;

        for (int i = startIdx; i < lines.length; i++) {
            String l = lines[i];
            if (l.length() >= minIndent) {
                sb.append(l.substring(minIndent));
            } else {
                sb.append(l.trim()); // Limpa espaços de linhas 100% vazias
            }
            if (i < lines.length - 1) sb.append("\n");
        }
        return sb.toString();
    }

    // ⭐ O Motor Quântico de Interpolação: Transforma strings em Somas na AST!
    private void processInterpolation(String content, int startLine) {
        boolean firstPart = true;
        StringBuilder currentPart = new StringBuilder();
        int i = 0;

        while (i < content.length()) {
            // Detetou a abertura de interpolação: ${
            if (content.charAt(i) == '$' && i + 1 < content.length() && content.charAt(i + 1) == '{') {

                // Emite a string lida até agora
                if (!firstPart) {
                    tokens.add(new Token(TokenType.PLUS, "+", null, startLine, 0,this.filePath));
                }
                tokens.add(new Token(TokenType.STRING_LITERAL, currentPart.toString(), currentPart.toString(), startLine, 0,this.filePath));
                currentPart.setLength(0);
                firstPart = false;

                // Emite os tokens da SOMA + PARÊNTESIS: "+ ("
                tokens.add(new Token(TokenType.PLUS, "+", null, startLine, 0,this.filePath));
                tokens.add(new Token(TokenType.LPAREN, "(", null, startLine, 0,this.filePath));

                i += 2; // Salta '${'
                int braceDepth = 1;
                int exprStart = i;
                boolean inString = false;

                // Navega até fechar a chave da interpolação, ignorando chaves dentro de strings!
                while (i < content.length() && braceDepth > 0) {
                    char c = content.charAt(i);
                    if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                        inString = !inString;
                    }
                    if (!inString) {
                        if (c == '{') braceDepth++;
                        else if (c == '}') braceDepth--;
                    }
                    if (braceDepth > 0) i++;
                }

                String innerExpr = content.substring(exprStart, i);
                if (i < content.length()) i++; // Salta o '}'

                // INCEPTION: Usa um Lexer clone para extrair os tokens da expressão matemática!
                Lexer innerLexer = new Lexer(innerExpr, filePath);
                innerLexer.line = startLine;
                java.util.List<Token> innerTokens = innerLexer.tokenize();

                for (Token t : innerTokens) {
                    if (t.type != TokenType.EOF) tokens.add(t);
                }

                // Fecha a nossa expressão fantasma: ")"
                tokens.add(new Token(TokenType.RPAREN, ")", null, startLine, 0,this.filePath));
                continue;
            }

            // Letras normais da string
            currentPart.append(content.charAt(i));
            i++;
        }

        // Emite o bloco final da string
        if (!firstPart) {
            if (!currentPart.isEmpty()) {
                tokens.add(new Token(TokenType.PLUS, "+", null, startLine, 0,this.filePath));
                tokens.add(new Token(TokenType.STRING_LITERAL, currentPart.toString(), currentPart.toString(), startLine, 0,this.filePath));
            }
        } else {
            // Se nunca houve interpolação, emite a string inteira, intocável.
            tokens.add(new Token(TokenType.STRING_LITERAL, currentPart.toString(), currentPart.toString(), startLine, 0,this.filePath));
        }
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
        tokens.add(new Token(type, text, literal, line, columnStart,this.filePath));
    }
}