package com.dic.xsuper.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * O Parser (Analisador Sintático) é o "Cérebro Estrutural".
 * Ele lê a lista de Tokens linear (1D) e transforma-a numa Árvore Sintática (AST) (2D/3D).
 * Usa a técnica "Recursive Descent", o que significa que começa pelas regras mais amplas (Programas/Declarações)
 * e vai descendo até às mais específicas (Expressões Matemáticas e Números).
 */
public class Parser {
    private final List<Token> tokens;
    private int current = 0; // O "Ponteiro" que indica em que token estamos no momento

    // Rastreador de profundidade para aplicar a regra rigorosa do 'var'
    private int scopeDepth = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * PONTO DE ENTRADA DO PARSER.
     * O ficheiro é apenas uma lista de declarações. Este método faz um loop até
     * encontrar o fim do ficheiro (EOF) e tenta analisar uma declaração de cada vez.
     */
    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            try {
                statements.add(declaration());
            } catch (ParseException e) {
                // Se der erro, ele "sincroniza" (salta os tokens inválidos até ao próximo ';')
                // para não crashar tudo e conseguir mostrar mais erros no resto do ficheiro.
                synchronize();
            }
        }
        return statements;
    }

    // ==========================================
    // DECLARAÇÕES (O topo da hierarquia)
    // Uma "Declaração" (Declaration) introduz um novo nome no sistema (variável, função, classe, import).
    // Se não for uma declaração, cai para um "Statement" (Comando de execução normal).
    // ==========================================

    private Stmt declaration() {
        if (match(TokenType.FUN)) return functionDeclaration();
        if (match(TokenType.VAR, TokenType.LET, TokenType.CONST)) return varDeclaration();

        // ESQUELETOS FUTUROS QUE PODES IMPLEMENTAR:
        // Se a tua linguagem tiver módulos/imports: import "ficheiro.xpl";
        // if (match(TokenType.IMPORT)) return importDeclaration();

        // Se decidires implementar enums: enum Cor { RED, BLUE }
        // if (match(TokenType.ENUM)) return enumDeclaration();

        return statement();
    }

    private Stmt functionDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Esperado nome da função.");
        consume(TokenType.LPAREN, "Esperado '(' após o nome da função.");

        List<Token> parameters = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                parameters.add(consume(TokenType.IDENTIFIER, "Esperado nome do parâmetro."));
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RPAREN, "Esperado ')' após os parâmetros.");

        Token returnType = null;
        if (match(TokenType.COLON)) {
            if (match(TokenType.T_INT, TokenType.T_FLOAT, TokenType.T_STRING, TokenType.T_ARRAY, TokenType.T_OBJECT, TokenType.T_ENUM)) {
                returnType = previous();
            } else {
                throw error(peek(), "Esperado tipo de retorno válido (int, float, string...).");
            }
        }

        consume(TokenType.LBRACE, "Esperado '{' antes do corpo da função.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, returnType, body);
    }

    private Stmt varDeclaration() {
        Token keyword = previous(); // Pode ser LET, VAR ou CONST

        if (keyword.type == TokenType.VAR && scopeDepth > 0) {
            throw error(keyword, "Erro de Escopo: A palavra-chave 'var' só pode ser usada ao nível do arquivo global.");
        }

        Token name = consume(TokenType.IDENTIFIER, "Esperado nome da variável.");

        Token typeAnnotation = null;
        if (match(TokenType.COLON)) {
            if (match(TokenType.T_INT, TokenType.T_FLOAT, TokenType.T_STRING, TokenType.T_ARRAY, TokenType.T_OBJECT, TokenType.T_ENUM)) {
                typeAnnotation = previous();
            } else {
                throw error(peek(), "Esperado tipo válido após ':'.");
            }
        }

        Expr initializer = null;
        if (match(TokenType.ASSIGN)) {
            initializer = expression();
        }

        if (keyword.type == TokenType.CONST && initializer == null) {
            throw error(name, "Uma constante ('const') precisa ser inicializada com um valor.");
        }

        consume(TokenType.SEMICOLON, "Esperado ';' após a declaração da variável.");
        return new Stmt.VarDecl(keyword, name, typeAnnotation, initializer);
    }

    // ==========================================
    // STATEMENTS (Comandos de Ação)
    // Comandos que não criam variáveis globais, mas executam lógicas (If, For, Print, Atribuições).
    // ==========================================

    private Stmt statement() {
        // Redirecionamento inteligente: Dependendo da palavra-chave inicial, escolhe a regra certa.
        if (match(TokenType.FOR)) return forDispatcher(); // Mudei o nome para organizares melhor os teus For Loops
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.BREAK)) return breakStatement();
        if (match(TokenType.CONTINUE)) return continueStatement();
        if (match(TokenType.RETURN)) return returnStatement();

        // ESQUELETOS FUTUROS:
        // if (match(TokenType.WHILE)) return whileStatement();

        // Se abrir chavetas soltas, cria um escopo (bloco) isolado.
        if (match(TokenType.LBRACE)) return new Stmt.Block(block());

        // Se não for nada disso, assume que é uma expressão a tentar calcular algo (ex: a = 10; ou println("ola");)
        return expressionStatement();
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(TokenType.SEMICOLON)) {
            value = expression();
        }
        consume(TokenType.SEMICOLON, "Esperado ';' após o valor de retorno.");
        return new Stmt.Return(keyword, value);
    }

    /**
     * Mestre Dispatcher dos FOR Loops.
     * Como pretendes ter for-in, for(1,10) e for(int i=0; i<10; i++),
     * este método "espia" os tokens seguintes para descobrir qual for loop deves chamar!
     */
    private Stmt forDispatcher() {
        // Aqui tu deves usar o check() ou peek() para adivinhar o formato e redirecionar
        // TODO: Implementa a tua lógica de "adivinhação" de qual 'for' é este.
        // Se for o for-in: return forInStatement();
        // Se for o for-c: return forCStyleStatement();
        // Se for range for(1, 10): return forRangeStatement();

        if (check(TokenType.LPAREN)){
            return  forCStyleStatement();
        }

        // Exemplo temporário para não quebrar o código atual:
        return forInStatement();
    }

    private Stmt forInStatement() {
        // Ex: for a in [1, 2, 3] { ... }
        Token loopVar = consume(TokenType.IDENTIFIER, "Esperado nome da variável após 'for'.");
        consume(TokenType.IN, "Esperado 'in' após a variável do loop.");

        if (check(TokenType.LPAREN)){
            return forInRangeStatement(loopVar);
        }
        Expr iterable = expression();
        consume(TokenType.LBRACE, "Esperado '{' após a expressão do for-in.");
        Stmt body = new Stmt.Block(block());
        return new Stmt.ForIn(loopVar, iterable, body);
    }

    private Stmt forCStyleStatement() {
        // TODO: Criar o nó AST Stmt.ForCStyle no ficheiro Stmt.java e processar aqui!
        // Ex: for (let i = 0; i < 10; i = i + 1) { ... }
        consume(TokenType.LPAREN,"Esperado '(' após a definição do loop for-c-style.");


        Token keyword = consume(TokenType.LET,"Erro de declaração: Apenas a palavra chave 'let' é suportada para o loop for-c-style.");

        Token name = consume(TokenType.IDENTIFIER, "Esperado nome da variável.");

        Token typeAnnotation = null;
        if (match(TokenType.COLON)){
            if (match(TokenType.T_INT, TokenType.T_FLOAT)) {
                typeAnnotation = previous();
            } else {
                throw error(peek(), "Esperado numerico tipo válido após ':' int ou float.");
            }
        }else {
            throw error(keyword, "Esperado ':' após o nome da variável.");
        }
        Expr initializer = null;
        if (match(TokenType.ASSIGN)) {
            initializer = expression();
        } else {
            throw error(keyword, "Esperado '=' após o tipo da variável.");
        }
        consume(TokenType.SEMICOLON, "Esperado ';' após a declaração da variável.");

        Expr condition = expression();
        consume(TokenType.SEMICOLON, "Esperado ';' após a codição do for-c-style.");
        Expr incrementExp = expression();

        consume(TokenType.RPAREN, "Esperado ')' o incremento do loop for-c-style.");
        consume(TokenType.LBRACE, "Esperado '{' após a expressão do for-c-style.");

        Stmt init = new Stmt.VarDecl(keyword, name, typeAnnotation, initializer);
        Stmt increment = new Stmt.ExpressionStmt(incrementExp);
        Stmt body = new Stmt.Block(block());
        return new Stmt.ForCStyle(init,condition,increment,body);
    }

    // ESQUELETO FUTURO: for a in (1, 10, 2)
    private Stmt forInRangeStatement(Token loopVar) {
        // TODO: Criar a lógica de range loop baseado na tua especificação.
        consume(TokenType.LPAREN,"Esperado '(' após a variável do loop.");

        Expr start = expression();
        consume(TokenType.COMMA,"Esperado ',' após a variável do loop.");
        Expr end = expression();

        Optional<Expr> jump = Optional.empty();
        if (match(TokenType.COMMA)){
            jump = Optional.ofNullable(expression());
        }

        consume(TokenType.RPAREN, "Esperado ')' para fechar o range.");
        consume(TokenType.LBRACE, "Esperado '{' antes do corpo do loop.");
        Stmt body = new Stmt.Block(block());
        return new Stmt.ForInRange(loopVar,start,end,jump,body);
    }

    // ESQUELETO FUTURO: while (condicao) { ... }
    private Stmt whileStatement() {
        // TODO: Criar nó Stmt.While
        return null;
    }

    private Stmt ifStatement() {
        Expr condition = expression();
        consume(TokenType.LBRACE, "Esperado '{' após a condição do if.");
        Stmt thenBranch = new Stmt.Block(block());
        Stmt elseBranch = null;

        // Suporte a IF ELSE encadeado
        if (match(TokenType.ELSE)) {
            if (match(TokenType.IF)) {
                elseBranch = ifStatement(); // Recursão genial para resolver o "else if"
            } else {
                consume(TokenType.LBRACE, "Esperado '{' após 'else'.");
                elseBranch = new Stmt.Block(block());
            }
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt breakStatement() {
        Token keyword = previous();
        consume(TokenType.SEMICOLON, "Esperado ';' após 'break'.");
        return new Stmt.Break(keyword);
    }

    private Stmt continueStatement() {
        Token keyword = previous();
        consume(TokenType.SEMICOLON, "Esperado ';' após 'continue'.");
        return new Stmt.Continue(keyword);
    }

    /**
     * O bloco de código. O scopeDepth é manipulado aqui para proteger a regra do 'var'.
     */
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        scopeDepth++; // Entra num novo mundo (escopo)

        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(TokenType.RBRACE, "Esperado '}' para fechar o bloco.");
        scopeDepth--; // Sai do mundo (escopo)
        return statements;
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Esperado ';' após a expressão.");
        return new Stmt.ExpressionStmt(expr);
    }

    // ==========================================
    // EXPRESSÕES (Cálculos de Valores)
    // A ESCADA DA PRECEDÊNCIA MATEMÁTICA E LÓGICA
    // Começamos na prioridade mais baixa (atribuição '=') e descemos
    // até à prioridade máxima (parêntesis '()' e literais como '10').
    // ==========================================

    private Expr expression() {
        return assignment(); // Inicia a escada
    }

    private Expr assignment() {
        Expr expr = equality();

        // 1. Atribuição Simples (=)
        if (match(TokenType.ASSIGN)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }
            throw error(equals, "Alvo de atribuição inválido.");
        }

        // 2. Atribuição Composta (+=, -=, *=, /=, %=, #=)
        if (match(TokenType.PLUS_ASSIGN, TokenType.MINUS_ASSIGN,
                TokenType.STAR_ASSIGN, TokenType.SLASH_ASSIGN,
                TokenType.MODULO_ASSIGN, TokenType.HASH_ASSIGN)) {

            Token operator = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.CompoundAssign(name, operator, value);
            }
            throw error(operator, "Alvo de atribuição composta inválido.");
        }

        // 3. Incremento e Decremento (++, --)
        if (match(TokenType.PLUS_PLUS, TokenType.MINUS_MINUS)) {
            Token operator = previous();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                // isPrefix = false porque o operador veio DEPOIS do nome (ex: a++)
                return new Expr.Update(name, operator, false);
            }
            throw error(operator, "Alvo inválido para incremento/decremento.");
        }

        return expr;
    }
    private Expr equality() {
        Expr expr = comparison();
        // Resolve os == e != (Esquerda para a direita)
        while (match(TokenType.NOT_EQUAL, TokenType.EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        // Resolve os <, >, <=, >=
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        // Resolve Somas e Subtrações
        while (match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
        // Resolve Multiplicações e Divisões (Precedência mais alta que a soma!)
        while (match(TokenType.SLASH, TokenType.STAR, TokenType.POWER, TokenType.MODULO, TokenType.HASH)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        // Resolve operadores prefixados matemáticos (ex: -10)
        if (match(TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return callExpression();
    }

    /**
     * O Motor de Invocações Moderno.
     * Analisa coisas como função() ou funçãoRetornaFunção()()
     */
    private Expr callExpression() {
        Expr expr = primary();

        while (true) {
            if (match(TokenType.LPAREN)) { // Achou um '(' logo depois da expressão? É função!
                expr = finishCall(expr);
            }
            // ESQUELETO FUTURO: Se achou um '[' logo depois, é acesso a array! ex: lista[0]
            // else if (match(TokenType.LBRACKET)) { expr = finishArrayAccess(expr); }

            // ESQUELETO FUTURO: Se achou um '.' logo depois, é acesso a objeto! ex: obj.nome
            // else if (match(TokenType.DOT)) { expr = finishObjectAccess(expr); }
            else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                arguments.add(expression());
            } while (match(TokenType.COMMA));
        }

        Token paren = consume(TokenType.RPAREN, "Esperado ')' após os argumentos da função.");
        return new Expr.Call(callee, paren, arguments);
    }

    /**
     * AS FOLHAS DA ÁRVORE SINTÁTICA.
     * Este é o fundo da "Recursive Descent". Aqui nós deixamos de tentar procurar operações
     * e consumimos apenas os valores base puros (Números, Strings, Arrays, Identificadores).
     */
    private Expr primary() {
        if (match(TokenType.INT_LITERAL, TokenType.FLOAT_LITERAL, TokenType.STRING_LITERAL)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous()); // Acesso a uma variável na memória
        }

        if (match(TokenType.LBRACKET)) {
            // Array Literal ex: [1, 2, 3]
            List<Expr> elements = new ArrayList<>();
            if (!check(TokenType.RBRACKET)) {
                do {
                    elements.add(expression());
                } while (match(TokenType.COMMA));
            }
            consume(TokenType.RBRACKET, "Esperado ']' após os elementos do array.");
            return new Expr.ArrayLiteral(elements);
        }

        // ESQUELETO FUTURO: Construção de Objetos JavaScript-style: { "nome": "Fernando", "idade": 30 }
        if (match(TokenType.LBRACE)) {
            return objectLiteral();
        }

        if (match(TokenType.LPAREN)) {
            // O uso de parêntesis agrupa matemática (força precedência máxima).
            Expr expr = expression();
            consume(TokenType.RPAREN, "Esperado ')' após a expressão.");
            return expr;
        }

        throw error(peek(), "Expressão inesperada.");
    }

    // ESQUELETO FUTURO: Analisador de objetos
    private Expr objectLiteral() {
        // TODO: Ler pares chave: valor separados por vírgula até fechar o }
        // Exemplo: HashMap de Expr para Expr, ou String para Expr, e colocar no Expr.ObjectLiteral
        consume(TokenType.RBRACE, "Esperado '}' após o corpo do objeto.");
        return null;
    }

    // ==========================================
    // "MÁQUINA DE COMER" (MÉTODOS UTILITÁRIOS DO PARSER)
    // Estes métodos "avançam" no array de Tokens passo a passo.
    // ==========================================

    /** Se o token atual for de um dos tipos solicitados, avança um passo e devolve true. */
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    /** Vê o token atual sem avançar (espia o que vem a seguir). */
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    /** Avança para o próximo Token (o "Come Come"). */
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous(); // Retorna o token que acabou de ser consumido
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    /** Exige obrigatoriamente que um token esteja presente, senão lança um Erro Sintático (Crash Limpo). */
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    /** Gera o aviso visual de erro e cria a Exceção. */
    private ParseException error(Token token, String message) {
        System.err.println("Erro Sintático (L" + token.line + ":C" + token.column + "): " + message);
        return new ParseException();
    }

    /** * RECUPERAÇÃO DE PÂNICO.
     * Quando o parser choca num erro de sintaxe, ele não desiste do ficheiro todo.
     * Este método avança cegamente pelos tokens até achar o próximo Ponto e Vírgula (;) ou início de bloco,
     * para tentar continuar a encontrar mais erros e avisar o programador de tudo de uma vez.
     */
    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return;
            switch (peek().type) {
                case FUN: case VAR: case LET: case CONST:
                case FOR: case IF: return;
                default: advance();
            }
        }
    }

    private static class ParseException extends RuntimeException {}
}