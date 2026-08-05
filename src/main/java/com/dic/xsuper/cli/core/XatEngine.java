package com.dic.xsuper.cli.core;

import com.dic.xsuper.utils.ConsoleTheme;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * O Mini Compilador e Interpretador da linguagem de automação .XAT
 */
public class XatEngine {

    private final CommandRegistry registry;
    private Path currentDir;
    private final Map<String, Object> memory = new HashMap<>();
    private static final Map<String, Thread> activeAsyncTasks = new ConcurrentHashMap<>();
    public XatEngine(CommandRegistry registry, Path startDir) {
        this.registry = registry;
        this.currentDir = startDir;
    }

    public Path run(String sourceCode) {
        try {
            List<Token> tokens = new Lexer(sourceCode).tokenize();
            List<Stmt> statements = new Parser(tokens).parse();

            System.out.println(ConsoleTheme.HEADER + "========================================" + ConsoleTheme.RESET);
            System.out.println(ConsoleTheme.HEADER + "  MOTOR DE AUTOMAÇÃO XAT ATIVADO" + ConsoleTheme.RESET);
            System.out.println(ConsoleTheme.HEADER + "========================================\n" + ConsoleTheme.RESET);

            for (Stmt stmt : statements) {
                execute(stmt);
            }

            System.out.println(ConsoleTheme.SUCCESS + "\n-> Automação concluída com sucesso!" + ConsoleTheme.RESET);

        } catch (XatException e) {
            // ⭐ TRATAMENTO DE ERROS COM LOCALIZAÇÃO EXATA!
            String loc = e.token != null ? "Linha " + e.token.line + ", Col " + e.token.col + " -> " : "";
            System.out.println(ConsoleTheme.ERROR + "\n[XAT Erro Lógico] " + loc + e.getMessage() + ConsoleTheme.RESET);
        } catch (Exception e) {
            System.out.println(ConsoleTheme.ERROR + "\n[XAT Erro Fatal] " + e.getMessage() + ConsoleTheme.RESET);
        }
        return currentDir;
    }

    // =========================================================================
    // ⭐ 0. EXCEÇÃO CUSTOMIZADA (Com Rastreamento)
    // =========================================================================
    static class XatException extends RuntimeException {
        final Token token;
        XatException(Token token, String message) {
            super(message);
            this.token = token;
        }
    }

    // =========================================================================
    // ⭐ 1. O INTERPRETADOR (Execução da AST)
    // =========================================================================
    // =========================================================================
    // ⭐ 1. O INTERPRETADOR (Execução da AST)
    // =========================================================================
    private void execute(Stmt stmt) {

        // ⭐ CORREÇÃO 2 (SURDEZ): Verifica a cada passo se alguém mandou esta tarefa parar!
        // Se a flag de interrupção estiver ativa, lançamos um erro para destruir a execução imediatamente.
        if (Thread.currentThread().isInterrupted()) {
            throw new RuntimeException(new InterruptedException("Tarefa cancelada"));
        }

        if (stmt instanceof Stmt.Block block) {
            for (Stmt s : block.statements) execute(s);
        }
        else if (stmt instanceof Stmt.Dec dec) {
            memory.put(dec.nameTk.lexeme, evaluate(dec.value));
        }
        else if (stmt instanceof Stmt.Assign assign) {
            if (!memory.containsKey(assign.nameTk.lexeme)) {
                throw new XatException(assign.nameTk, "A variável '" + assign.nameTk.lexeme + "' não foi declarada.");
            }
            memory.put(assign.nameTk.lexeme, evaluate(assign.value));
        }
        else if (stmt instanceof Stmt.InstructionDef inst) {
            memory.put(inst.nameTk.lexeme, inst.body);
        }
        else if (stmt instanceof Stmt.CallInst call) {
            if (!memory.containsKey(call.nameTk.lexeme)) {
                throw new XatException(call.keyword, "A instrução/tarefa '" + call.nameTk.lexeme + "' não existe.");
            }
            Object obj = memory.get(call.nameTk.lexeme);
            if (obj instanceof Stmt.Block b) {
                if (call.isAsync) {
                    String taskName = call.nameTk.lexeme;

                    // Se já houver uma com o mesmo nome a correr, evita duplicados
                    if (activeAsyncTasks.containsKey(taskName)) {
                        System.out.println(ConsoleTheme.WARNING + " ⚠️ [Async] A tarefa '" + taskName + "' já está em execução." + ConsoleTheme.RESET);
                        return;
                    }

                    Thread backgroundThread = new Thread(() -> {
                        try {
                            execute(b);
                        } catch (Exception e) {
                            // Verifica se a causa da falha foi o nosso cancelamento seguro
                            if (Thread.currentThread().isInterrupted() || e.getCause() instanceof InterruptedException || (e.getMessage() != null && e.getMessage().contains("Tarefa cancelada"))) {
                                System.out.println(ConsoleTheme.WARNING + "\n 🛑 [Async] Tarefa '" + taskName + "' foi interrompida com sucesso." + ConsoleTheme.RESET);
                            } else {
                                System.out.println(ConsoleTheme.ERROR + "\n[Async Task Error (" + taskName + ")] " + e.getMessage() + ConsoleTheme.RESET);
                            }
                        } finally {
                            activeAsyncTasks.remove(taskName); // Limpa da memória quando termina
                        }
                    });

                    // ⭐ CORREÇÃO 1 (RACE CONDITION): Regista a Thread na memória ANTES de a iniciar!
                    // Assim, o script principal já a consegue encontrar imediatamente a seguir.
                    activeAsyncTasks.put(taskName, backgroundThread);
                    backgroundThread.setDaemon(true);
                    backgroundThread.start();

                    System.err.println(); // Ajuste visual
                    System.out.println(ConsoleTheme.SUCCESS + " 🚀 [Async] Tarefa '" + taskName + "' disparada em segundo plano." + ConsoleTheme.RESET);
                } else {
                    execute(b);
                }
            } else {
                throw new XatException(call.nameTk, "O identificador '" + call.nameTk.lexeme + "' é uma variável, não um bloco invocável.");
            }
        }
        else if (stmt instanceof Stmt.CancelStmt cancelStmt) {
            String taskName = cancelStmt.nameTk.lexeme;
            Thread targetThread = activeAsyncTasks.get(taskName);

            if (targetThread != null && targetThread.isAlive()) {
                targetThread.interrupt(); // Envia o sinal de interrupção para a thread
                System.out.println(ConsoleTheme.WARNING + " 🛑 A enviar sinal de paragem para a tarefa '" + taskName + "'..." + ConsoleTheme.RESET);
            } else {
                System.out.println(ConsoleTheme.ERROR + " ❌ Não foi encontrada nenhuma tarefa assíncrona ativa com o nome '" + taskName + "'." + ConsoleTheme.RESET);
            }
        }
        else if (stmt instanceof Stmt.Run runStmt) {
            String command = evaluate(runStmt.command).toString();
            System.out.println(ConsoleTheme.DIRECTORY + " ❯ " + ConsoleTheme.TEXT + command + ConsoleTheme.RESET);
            currentDir = registry.executeCommand(command, currentDir);
        }
        else if (stmt instanceof Stmt.Loop loop) {
            while (isTruthy(evaluate(loop.condition))) {
                execute(loop.body);
            }
        }
        // ⭐ EXECUÇÃO DA PAUSA (NATIVA)
        else if (stmt instanceof Stmt.SleepStmt sleepStmt) {
            Object durationObj = evaluate(sleepStmt.duration);
            if (!(durationObj instanceof Long)) {
                throw new XatException(sleepStmt.keyword, "O tempo de espera (wait/delay) deve ser um número inteiro (milissegundos).");
            }

            long millis = (Long) durationObj;
            try {
                Thread.sleep(millis); // 💤 Adormece a thread atual!
            } catch (InterruptedException e) {
                // Se a thread for acordada por um 'cancel', volta a ligar a flag de interrupção
                // e atira o erro para que a tarefa morra imediatamente e de forma limpa!
                Thread.currentThread().interrupt();
                throw new RuntimeException(new InterruptedException("Tarefa cancelada durante a pausa"));
            }
        }
        else if (stmt instanceof Stmt.If ifStmt) {
            if (isTruthy(evaluate(ifStmt.condition))) {
                execute(ifStmt.thenBranch);
            } else {
                boolean matchedElseIs = false;
                for (Stmt.If.ElseIs ei : ifStmt.elseIsBranches) {
                    if (isTruthy(evaluate(ei.condition))) {
                        execute(ei.body);
                        matchedElseIs = true;
                        break;
                    }
                }
                if (!matchedElseIs && ifStmt.elseBranch != null) {
                    execute(ifStmt.elseBranch);
                }
            }
        }
    }

    private Object evaluate(Expr expr) {

        if (expr instanceof Expr.ReadInput readExpr) {
            Object promptVal = evaluate(readExpr.prompt);

            if (readExpr.newLine) {
                System.out.println(ConsoleTheme.WARNING + " ❔ " + promptVal.toString() + ConsoleTheme.RESET);
            } else {
                System.out.print(ConsoleTheme.WARNING + " ❔ " + promptVal.toString() + ConsoleTheme.RESET);
            }

            String input = "";
            java.io.Console console = System.console();
            if (console != null) input = console.readLine();
            else {
                @SuppressWarnings("resource")
                java.util.Scanner scanner = new java.util.Scanner(System.in);
                input = scanner.nextLine();
            }

            input = input.trim();
            try { return Long.parseLong(input); } catch (NumberFormatException e) { return input; }
        }

        if (expr instanceof Expr.Unary unary) {
            Object right = evaluate(unary.right);
            if (unary.op.type == TokenType.NOT) return !isTruthy(right);
            if (unary.op.type == TokenType.MINUS) {
                if (right instanceof Long l) return -l;
                throw new XatException(unary.op, "Operador '-' exige um número.");
            }
        }

        if (expr instanceof Expr.Literal lit) return lit.value;
        if (expr instanceof Expr.Variable var) {
            if (!memory.containsKey(var.nameTk.lexeme)) {
                throw new XatException(var.nameTk, "Variável desconhecida: " + var.nameTk.lexeme);
            }
            return memory.get(var.nameTk.lexeme);
        }
        if (expr instanceof Expr.Binary bin) {

            // ⭐ AVALIAÇÃO EM CURTO-CIRCUITO (Para performance e segurança)
            if (bin.op.type == TokenType.OR) {
                Object left = evaluate(bin.left);
                if (isTruthy(left)) return true; // Se o esquerdo for verdade, nem olha para o direito!
                return isTruthy(evaluate(bin.right));
            }
            if (bin.op.type == TokenType.AND) {
                Object left = evaluate(bin.left);
                if (!isTruthy(left)) return false; // Se o esquerdo for falso, aborta logo!
                return isTruthy(evaluate(bin.right));
            }

            // Operações normais (Comparações e Matemática)
            Object left = evaluate(bin.left);
            Object right = evaluate(bin.right);

            if (bin.op.type == TokenType.IS) return left.equals(right);
            if (bin.op.type == TokenType.NOT) return !left.equals(right);
            if (bin.op.type == TokenType.PLUS) {
                if (left instanceof String || right instanceof String) return left.toString() + right.toString();
                return (Long) left + (Long) right;
            }

            long l = (Long) left;
            long r = (Long) right;

            return switch (bin.op.type) {
                case MINUS -> l - r;
                case STAR -> l * r;
                case SLASH -> {
                    if (r == 0) throw new XatException(bin.op, "Divisão por zero não permitida.");
                    yield l / r;
                }
                case LT -> l < r;
                case GT -> l > r;
                default -> throw new XatException(bin.op, "Operador desconhecido.");
            };
        }
        return null;
    }

    private boolean isTruthy(Object obj) {
        if (obj instanceof Boolean b) return b;
        return obj != null;
    }

    // =========================================================================
    // ⭐ 2. A AST (Árvore Sintática)
    // =========================================================================
    abstract static class Stmt {
        static class Block extends Stmt { List<Stmt> statements; Block(List<Stmt> s) { statements = s; } }
        static class Dec extends Stmt { Token nameTk; Expr value; Dec(Token n, Expr v) { nameTk = n; value = v; } }
        static class Assign extends Stmt { Token nameTk; Expr value; Assign(Token n, Expr v) { nameTk = n; value = v; } }
        static class Run extends Stmt { Token keyword; Expr command; Run(Token k, Expr c) { keyword = k; command = c; } }
        static class Loop extends Stmt { Token keyword; Expr condition; Block body; Loop(Token k, Expr c, Block b) { keyword = k; condition = c; body = b; } }
        static class InstructionDef extends Stmt { Token nameTk; Block body; InstructionDef(Token n, Block b) { nameTk = n; body = b; } }
        static class CallInst extends Stmt {
            Token keyword;
            Token nameTk;
            boolean isAsync; // 👈 Nova flag
            CallInst(Token k, Token n, boolean async) {
                keyword = k;
                nameTk = n;
                isAsync = async;
            }
        }
        static class If extends Stmt {
            Token keyword; Expr condition; Block thenBranch; List<ElseIs> elseIsBranches; Block elseBranch;
            If(Token k, Expr c, Block t, List<ElseIs> ei, Block e) { keyword = k; condition = c; thenBranch = t; elseIsBranches = ei; elseBranch = e; }
            static class ElseIs { Token keyword; Expr condition; Block body; ElseIs(Token k, Expr c, Block b) { keyword = k; condition = c; body = b; } }
        }
        static class CancelStmt extends Stmt {
            Token keyword;
            Token nameTk;
            CancelStmt(Token k, Token n) { keyword = k; nameTk = n; }
        }
        static class SleepStmt extends Stmt {
            Token keyword;
            Expr duration;
            SleepStmt(Token k, Expr d) { keyword = k; duration = d; }
        }
    }

    abstract static class Expr {
        static class Literal extends Expr { Object value; Literal(Object v) { value = v; } }
        static class Variable extends Expr { Token nameTk; Variable(Token n) { nameTk = n; } }
        static class Binary extends Expr { Expr left; Token op; Expr right; Binary(Expr l, Token o, Expr r) { left = l; op = o; right = r; } }
        static class Unary extends Expr { Token op; Expr right; Unary(Token o, Expr r) { op = o; right = r; } }
        static class ReadInput extends Expr { Token keyword; Expr prompt; boolean newLine;
            ReadInput(Token k, Expr p, boolean nl) { keyword = k; prompt = p; newLine = nl; } }
    }

    // =========================================================================
    // ⭐ 3. O LEXER & PARSER (Agora com coordenadas X,Y)
    // =========================================================================
    enum TokenType { DEC, LOOP, IF, ELSE, IS, RUN, IDENT, STR, NUM, PLUS, MINUS, STAR, SLASH, EQ, LT, GT, LBRACE, RBRACE, EOF, TASK, CALL, SYNC, CANCEL, SLEEP, AND, READ, READL, NOT, OR }

    // ⭐ NOVO: O Token agora sabe onde nasceu (linha e coluna)
    record Token(TokenType type, String lexeme, Object literal, int line, int col) {}

    static class Lexer {
        String src;
        int pos = 0;
        int line = 1;
        int col = 1;

        Lexer(String src) { this.src = src; }

        // Mapeador de posição para atualizar as coordenadas ativamente
        private char advance() {
            char c = src.charAt(pos++);
            if (c == '\n') { line++; col = 1; } else { col++; }
            return c;
        }

        private char peek() {
            if (pos >= src.length()) return '\0';
            return src.charAt(pos);
        }

        List<Token> tokenize() {
            List<Token> tokens = new ArrayList<>();
            while (pos < src.length()) {
                int startCol = col;
                int startLine = line;
                char c = advance();

                if (Character.isWhitespace(c)) continue;

                if (c == '#') {
                    while (pos < src.length() && peek() != '\n') advance();
                    continue;
                }
                if (c == '/' && peek() == '/') {
                    while (pos < src.length() && peek() != '\n') advance();
                    continue;
                }

                if (Character.isLetter(c)) {
                    int startPos = pos - 1;
                    while (pos < src.length() && (Character.isLetterOrDigit(peek()) || peek() == '_')) advance();
                    String word = src.substring(startPos, pos);
                    TokenType type = switch (word) {
                        case "dec" -> TokenType.DEC; case "loop" -> TokenType.LOOP;
                        case "if" -> TokenType.IF; case "else" -> TokenType.ELSE;
                        case "is" -> TokenType.IS; case "run" -> TokenType.RUN;
                        case "task" -> TokenType.TASK;
                        case "call" -> TokenType.CALL;
                        case "sync" -> TokenType.SYNC;
                        case "cancel" -> TokenType.CANCEL;
                        case "sleep", "wait", "delay" -> TokenType.SLEEP;
                        case "and" -> TokenType.AND;
                        case "or" -> TokenType.OR;
                        case "read" -> TokenType.READ;
                        case "readl" -> TokenType.READL;
                        case "not" -> TokenType.NOT;
                        default -> TokenType.IDENT;
                    };
                    tokens.add(new Token(type, word, null, startLine, startCol));
                } else if (Character.isDigit(c)) {
                    int startPos = pos - 1;
                    while (pos < src.length() && Character.isDigit(peek())) advance();
                    String numStr = src.substring(startPos, pos);
                    tokens.add(new Token(TokenType.NUM, numStr, Long.parseLong(numStr), startLine, startCol));
                } else if (c == '"') {
                    int startPos = pos;
                    while (pos < src.length() && peek() != '"') advance();
                    String str = src.substring(startPos, pos);
                    if (pos < src.length()) advance(); // consome a aspa final
                    tokens.add(new Token(TokenType.STR, str, str, startLine, startCol));
                } else {
                    TokenType t = switch (c) {
                        case '+' -> TokenType.PLUS; case '-' -> TokenType.MINUS; case '*' -> TokenType.STAR;
                        case '/' -> TokenType.SLASH; case '=' -> TokenType.EQ; case '<' -> TokenType.LT;
                        case '>' -> TokenType.GT; case '{' -> TokenType.LBRACE; case '}' -> TokenType.RBRACE;
                        default -> null;
                    };
                    if (t == null) throw new XatException(new Token(TokenType.EOF, "", null, startLine, startCol), "Carácter inesperado: " + c);
                    tokens.add(new Token(t, String.valueOf(c), null, startLine, startCol));
                }
            }
            tokens.add(new Token(TokenType.EOF, "", null, line, col));
            return tokens;
        }
    }

    static class Parser {
        List<Token> tokens; int pos = 0;
        Parser(List<Token> t) { tokens = t; }

        Token previous() { return tokens.get(pos - 1); }

        List<Stmt> parse() {
            List<Stmt> stmts = new ArrayList<>();
            while (tokens.get(pos).type != TokenType.EOF) stmts.add(statement());
            return stmts;
        }

        Stmt statement() {
            if (match(TokenType.TASK)) {
                Token name = consume(TokenType.IDENT);
                return new Stmt.InstructionDef(name, block());
            }
            if (match(TokenType.CALL)) {
                Token keyword = previous();
                boolean isAsync = match(TokenType.SYNC);
                Token name = consume(TokenType.IDENT);
                return new Stmt.CallInst(keyword, name, isAsync);
            }
            if (match(TokenType.CANCEL)) {
                Token keyword = previous();
                Token name = consume(TokenType.IDENT);
                return new Stmt.CancelStmt(keyword, name);
            }
            if (match(TokenType.SLEEP)) {
                Token keyword = previous();
                Expr duration = expression();
                return new Stmt.SleepStmt(keyword, duration);
            }
            if (match(TokenType.DEC)) {
                Token name = consume(TokenType.IDENT);
                consume(TokenType.EQ);
                return new Stmt.Dec(name, expression());
            }
            if (match(TokenType.RUN)) {
                Token keyword = previous();
                return new Stmt.Run(keyword, expression());
            }
            if (match(TokenType.LOOP)) {
                Token keyword = previous();
                Expr cond = expression();
                return new Stmt.Loop(keyword, cond, block());
            }
            if (match(TokenType.IF)) {
                Token keyword = previous();
                Expr cond = expression();
                Stmt.Block thenBranch = block();
                List<Stmt.If.ElseIs> elseIs = new ArrayList<>();
                Stmt.Block elseBranch = null;

                while (match(TokenType.ELSE)) {
                    Token elseKeyword = previous();
                    if (match(TokenType.IS)) {
                        elseIs.add(new Stmt.If.ElseIs(elseKeyword, expression(), block()));
                    } else {
                        elseBranch = block();
                        break;
                    }
                }
                return new Stmt.If(keyword, cond, thenBranch, elseIs, elseBranch);
            }

            // Assign
            Token nameTk = consume(TokenType.IDENT);
            consume(TokenType.EQ);
            return new Stmt.Assign(nameTk, expression());
        }

        Stmt.Block block() {
            consume(TokenType.LBRACE);
            List<Stmt> stmts = new ArrayList<>();
            while (tokens.get(pos).type != TokenType.RBRACE) stmts.add(statement());
            consume(TokenType.RBRACE);
            return new Stmt.Block(stmts);
        }

        // ⭐ A HIERARQUIA PURA DO COMPILADOR (A Descer na Árvore de Precedência)

        Expr expression() { return logicalOr(); }

        Expr logicalOr() {
            Expr expr = logicalAnd();
            while (match(TokenType.OR)) {
                Token op = previous();
                expr = new Expr.Binary(expr, op, logicalAnd());
            }
            return expr;
        }

        Expr logicalAnd() {
            Expr expr = comparison();
            while (match(TokenType.AND)) {
                Token op = previous();
                expr = new Expr.Binary(expr, op, comparison());
            }
            return expr;
        }


        Expr comparison() {
            Expr expr = unary();
            // ⭐ ADICIONAMOS O 'NOT' AQUI
            while (match(TokenType.IS, TokenType.NOT, TokenType.LT, TokenType.GT)) {
                Token op = previous();
                expr = new Expr.Binary(expr, op, unary());
            }
            return expr;
        }

        // 2. ADICIONA ESTE NOVO MÉTODO (Trata o not e números negativos)
        Expr unary() {
            if (match(TokenType.NOT, TokenType.MINUS)) {
                Token op = previous();
                return new Expr.Unary(op, unary());
            }
            return term();
        }

        Expr term() {
            Expr expr = factor();
            while (match(TokenType.PLUS, TokenType.MINUS)) {
                Token op = previous();
                expr = new Expr.Binary(expr, op, factor());
            }
            return expr;
        }

        Expr factor() {
            Expr expr = primary();
            while (match(TokenType.STAR, TokenType.SLASH)) {
                Token op = previous();
                expr = new Expr.Binary(expr, op, primary());
            }
            return expr;
        }

        Expr primary() {
            if (match(TokenType.NUM, TokenType.STR)) return new Expr.Literal(previous().literal);
            if (match(TokenType.IDENT)) return new Expr.Variable(previous());
            if (match(TokenType.READ, TokenType.READL)) {
                Token keyword = previous();
                boolean newLine = keyword.type == TokenType.READL;
                Expr prompt = expression();
                return new Expr.ReadInput(keyword, prompt, newLine);
            }
            throw new XatException(tokens.get(pos), "Expressão inválida ao ler o código.");
        }

        boolean match(TokenType... types) {
            for (TokenType t : types) {
                if (tokens.get(pos).type == t) { pos++; return true; }
            }
            return false;
        }

        Token consume(TokenType type) {
            if (tokens.get(pos).type == type) return tokens.get(pos++);
            throw new XatException(tokens.get(pos), "Esperava encontrar '" + type + "', mas encontrou '" + tokens.get(pos).lexeme + "'");
        }
    }
}