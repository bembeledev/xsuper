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

    private Stmt enumDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Esperado nome do Enum.");
        consume(TokenType.LBRACE, "Esperado '{' antes do corpo do Enum.");

        List<Token> constants = new ArrayList<>();

        if (!check(TokenType.RBRACE)) {
            do {
                constants.add(consume(TokenType.IDENTIFIER, "Esperado nome da constante do Enum."));
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RBRACE, "Esperado '}' após o corpo do Enum.");
        return new Stmt.Enum(name, constants);
    }


    private Stmt declaration() {
        // Se for uma declaração, consome-a. Se falhar, sincroniza.
        if (match(TokenType.TYPE)) return typeAliasDeclaration();
        if (check(TokenType.FUN)) return functionDeclaration();
        if (match(TokenType.VAR, TokenType.LET, TokenType.CONST)) return varDeclaration();
        if (match(TokenType.INTERFACE)) return interfaceDeclaration();
        if (match(TokenType.DECLARE))   return declareDeclaration();
        //if (match(TokenType.IMPLEMENT)) return implementDeclaration();

        // ⭐ A NOVA BIFURCAÇÃO DA ALMA (IMPLEMENT) ⭐
        if (match(TokenType.ABSTRACT)) {
            consume(TokenType.IMPLEMENT, "Esperado 'implement' após a palavra 'abstract'.");
            return implementDeclaration(true); // Passa 'true' porque é uma implementação abstrata!
        }
        if (match(TokenType.IMPLEMENT)) {
            return implementDeclaration(false); // É uma implementação normal!
        }

        if (match(TokenType.T_ENUM)) return enumDeclaration();

        // Se NÃO é uma declaração, é um statement (comando)
        return statement();
    }

    // ⭐ O CONSTRUTOR SINTÁTICO DO ALIAS ⭐
    private Stmt typeAliasDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Esperado identificador para o nome do Alias.");
        consume(TokenType.ASSIGN, "Esperado '=' após o nome do Alias.");

        // Reutilizamos a nossa coroa de ouro: a leitura fractal de tipos!
        TypeNode target = parseTypeAnnotation();

        consume(TokenType.SEMICOLON, "Esperado ';' após a definição do sinónimo de tipo.");

        return new Stmt.TypeAliasDecl(name, target);
    }

    private Stmt declareDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Esperado nome do modelo de dados (declare).");

        Token superclass = null;
        if (match(TokenType.EXTENDS)) {
            superclass = consume(TokenType.IDENTIFIER, "Esperado nome do modelo pai após 'extends'.");
        }

        consume(TokenType.LBRACE, "Esperado '{' antes do corpo do declare.");

        java.util.List<Stmt.FieldDecl> fields = new java.util.ArrayList<>();

        // Removida a lista de methods! O declare só guarda variáveis.

        // Percorre tudo até fechar a chaveta
        while (!check(TokenType.RBRACE) && !isAtEnd()) {

            // ⭐1. O Colecionador de Modificadores ⭐
            Token accessModifier = null;
            boolean isStatic = false, isFinal = false, isReadonly = false;

            while (match(TokenType.PUB, TokenType.PRIV, TokenType.PROT, TokenType.STATIC, TokenType.FINAL, TokenType.READONLY)) {
                Token t = previous();
                switch (t.type) {
                    case PUB: case PRIV: case PROT:
                        if (accessModifier != null) throw error(t, "Apenas podes usar um modificador de acesso (pub, priv, prot).");
                        accessModifier = t;
                        break;
                    case STATIC: isStatic = true; break;
                    case FINAL: isFinal = true; break;
                    case READONLY: isReadonly = true; break;
                }
            }

            // Se o programador não escreveu pub/priv/prot, o padrão por segurança é PRIV!
            if (accessModifier == null) {
                accessModifier = new Token(TokenType.PRIV, "priv", null, peek().line, peek().column);
            }

            Token memberName = consume(TokenType.IDENTIFIER, "Esperado nome da propriedade.");
            consume(TokenType.COLON, "Esperado ':' após a propriedade.");
            TypeNode type = parseTypeAnnotation();
            consume(TokenType.SEMICOLON, "Esperado ';' no final da declaração.");

            fields.add(new Stmt.FieldDecl(accessModifier, isStatic, isFinal, isReadonly, memberName, type));
        }

        consume(TokenType.RBRACE, "Esperado '}' após o corpo do declare.");

        // ⭐ NOTA: Atualiza a tua classe Stmt.DeclareDecl para deixar de pedir a lista de methods!
        return new Stmt.DeclareDecl(name, superclass, fields);
    }

    private Stmt interfaceDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Esperado nome da interface.");
        consume(TokenType.LBRACE, "Esperado '{' antes do corpo da interface.");

        // ⭐ 1. MUDANÇA: A lista passa a ser de FunctionSig
        java.util.List<Stmt.FunctionSig> methods = new java.util.ArrayList<>();

        while (!check(TokenType.RBRACE) && !isAtEnd()) {

            // ⭐ 1. Capturar o Modificador (Opcional na interface, mas suportado!)
            Token modifier = null;
            if (match(TokenType.PUB, TokenType.PRIV, TokenType.PROT)) { // Garante que PROT está no teu Lexer!
                modifier = previous();
            }

            consume(TokenType.FUN, "Esperada a palavra-chave 'fun' para definir um método na interface.");
            Token methodName = consume(TokenType.IDENTIFIER, "Esperado nome do método.");

            // 2. Parâmetros (Mantém-se igual, mesmo que o tenhas simplificado no teu comentário)
            consume(TokenType.LPAREN, "Esperado '(' após o nome do método.");
            java.util.List<Stmt.Param> parameters = new java.util.ArrayList<>();
            if (!check(TokenType.RPAREN)) {
                do {
                    Token paramName = consume(TokenType.IDENTIFIER, "Esperado nome do parâmetro.");
                    consume(TokenType.COLON, "Esperado ':' após o nome do parâmetro.");

                    // ⭐ ADEUS TRATOR CEGO. Entra a Árvore Sintática:
                    TypeNode paramTypeNode = parseTypeAnnotation();

                    parameters.add(new Stmt.Param(paramName, paramTypeNode));

                } while (match(TokenType.COMMA));
            }
            consume(TokenType.RPAREN, "Esperado ')' após parâmetros.");

            // ⭐ 3. Embrulhar o Retorno no novo TypeNode!
            TypeNode returnTypeNode = null;
            if (match(TokenType.COLON)) {
                if (match(TokenType.IDENTIFIER, TokenType.T_INT, TokenType.T_FLOAT, TokenType.T_STRING, TokenType.T_ARRAY, TokenType.T_OBJECT, TokenType.T_ENUM)) {
                    returnTypeNode = new TypeNode.Simple(previous()); // Cria o TypeNode.Simple!
                } else {
                    throw error(peek(), "Esperado tipo de retorno válido após ':'.");
                }
            }

            consume(TokenType.SEMICOLON, "Esperado ';' após a assinatura do método na interface.");

            // ⭐ 4. A Nova Instanciação (Ajusta os parâmetros consoante o construtor real da tua classe)
            // Se a tua classe final tiver a lista de parâmetros descomentada, envia os 'parameters' também!
            methods.add(new Stmt.FunctionSig(modifier, methodName, parameters, returnTypeNode));
        }

        consume(TokenType.RBRACE, "Esperado '}' após o corpo da interface.");
        return new Stmt.InterfaceDecl(name, methods);

    }

    private Stmt implementDeclaration(boolean isAbstractImplement) {
        // 1. O Alvo Base (Ex: Mamifero ou Animal)
        Token targetName = consume(TokenType.IDENTIFIER, "Esperado nome do modelo de dados base.");

        // 2. A Variante / Alias (Opcional - Ex: as Mam1)
        Token aliasName = null;
        if (match(TokenType.AS)) {
            aliasName = consume(TokenType.IDENTIFIER, "Esperado nome da variante após 'as'.");
        }

        // 3. Os Contratos (Opcional - Ex: for CRUD, EXEC)
        // Como podemos ter 'abstract implement Animal {}', o 'for' nem sempre existe!
        java.util.List<Token> interfaces = new java.util.ArrayList<>();
        if (match(TokenType.FOR)) {
            do {
                interfaces.add(consume(TokenType.IDENTIFIER, "Esperado nome da interface."));
            } while (match(TokenType.COMMA));
        }

        // 4. O Corpo com o Código
        consume(TokenType.LBRACE, "Esperado '{' antes do corpo da implementação.");

        // ⭐ LER O BLOCO DEFAULT ⭐
        java.util.Map<String, Expr> defaultState = new java.util.HashMap<>();
        if (match(TokenType.DEFAULT)) {
            consume(TokenType.LBRACE, "Esperado '{' após 'default'.");
            if (!check(TokenType.RBRACE)) {
                do {
                    Token key = consume(TokenType.IDENTIFIER, "Esperado nome da propriedade no bloco default.");
                    consume(TokenType.COLON, "Esperado ':' após o nome da propriedade.");
                    Expr value = expression();
                    defaultState.put(key.lexeme, value);
                } while (match(TokenType.COMMA));
            }
            consume(TokenType.RBRACE, "Esperado '}' após o bloco 'default'.");
        }


        java.util.List<Stmt.Function> methods = new java.util.ArrayList<>();

        while (!check(TokenType.RBRACE) && !isAtEnd()) {

            // ⭐ 1. Modificadores de Acesso (pub / priv)
            Token modifier = null;
            if (match(TokenType.PUB, TokenType.PRIV)) {
                modifier = previous();
            }

            boolean isStatic = match(TokenType.STATIC);
            // ⭐ 2. Modificador de Abstração (abstract)
            boolean isAbstract = false;
            if (match(TokenType.ABSTRACT)) {
                isAbstract = true;
            }

            // ⭐ 3. A Palavra-chave OBRIGATÓRIA
            consume(TokenType.FUN, "Esperada a palavra-chave 'fun' para declarar um método.");

            // 4. Nome do Método
            Token methodName = consume(TokenType.IDENTIFIER, "Esperado nome do método.");

            // 5. Parâmetros ( )
            consume(TokenType.LPAREN, "Esperado '(' após o nome do método.");
            java.util.List<Stmt.Param> parameters = new java.util.ArrayList<>();
            if (!check(TokenType.RPAREN)) {
                do {
                    if (parameters.size() >= 255) {
                        error(peek(), "Não podes ter mais de 255 parâmetros.");
                    }

                    Token paramName = consume(TokenType.IDENTIFIER, "Esperado nome do parâmetro.");
                    consume(TokenType.COLON, "Esperado ':' após o nome do parâmetro para definir o tipo.");

                    // ⭐ A TRANSFORMAÇÃO: Adeus trator cego, olá leitor quântico!
                    TypeNode paramTypeNode = parseTypeAnnotation();

                    parameters.add(new Stmt.Param(paramName, paramTypeNode));

                } while (match(TokenType.COMMA));
            }
            consume(TokenType.RPAREN, "Esperado ')' após parâmetros.");

            // ⭐ 6. Tipo de Retorno (Ex: : int)
            TypeNode returnType = null; // Mudámos de Token para TypeNode!
            if (match(TokenType.COLON)) {
                if (match(TokenType.IDENTIFIER, TokenType.T_INT, TokenType.T_FLOAT, TokenType.T_STRING, TokenType.T_ARRAY, TokenType.T_OBJECT, TokenType.T_ENUM)) {

                    // Envolvemos o token lido dentro de um TypeNode para satisfazer a AST!
                    returnType = new TypeNode.Simple(previous());

                } else {
                    throw error(peek(), "Esperado tipo de retorno válido após ':'.");
                }
            }

            // ⭐ 6.5 A NOVA CLÁUSULA THROWS (O Contrato de Segurança) ⭐
            java.util.List<Token> thrownExceptions = new java.util.ArrayList<>();
            if (match(TokenType.THROWS)) {
                do {
                    Token errorName = consume(TokenType.IDENTIFIER, "Esperado nome da exceção após 'throws'.");
                    thrownExceptions.add(errorName);
                } while (match(TokenType.COMMA)); // Permite 'throws IOError, NetError'
            }

            // ⭐ 7. A BIFURCAÇÃO: Abstrato vs Concreto ⭐
            java.util.List<Stmt> body = null;
            if (isAbstract) {
                consume(TokenType.SEMICOLON, "Métodos abstratos não podem ter corpo '{}'. Esperado ';' no final da assinatura.");
            } else {
                consume(TokenType.LBRACE, "Esperado '{' antes do corpo do método.");
                body = block();
            }

            // ⭐ 8. Instanciação Perfeita com o Novo Construtor!
            // Nota: Passamos a lista 'thrownExceptions' para a AST.
            methods.add(new Stmt.Function(modifier, isStatic, isAbstract, methodName, parameters, returnType, thrownExceptions, body));
        }

        consume(TokenType.RBRACE, "Esperado '}' após o corpo do implement.");

        return new Stmt.ImplementDecl(isAbstractImplement, targetName, aliasName, interfaces, defaultState, methods);
    }


    private Stmt functionDeclaration() {
        // 1. Modificadores de Acesso (Opcionais - Se a tua AST já suportar)
        Token modifier = null;
        if (match(TokenType.PUB, TokenType.PRIV)) {
            modifier = previous();
        }

        // 2. Modificador Abstract (Opcional)
        boolean isAbstract = match(TokenType.ABSTRACT);

        // ⭐ 3. A NOVA REGRA DE SINTAXE: O TOKEN 'fun' É OBRIGATÓRIO ⭐
        consume(TokenType.FUN, "Esperada a palavra-chave 'fun' para declarar um método ou função.");

        // 4. Nome da Função
        Token name = consume(TokenType.IDENTIFIER, "Esperado nome da função.");
        consume(TokenType.LPAREN, "Esperado '(' após o nome da função.");

        // 5. Parâmetros (com a tua tipagem forte!)
        List<Stmt.Param> parameters = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                Token paramName = consume(TokenType.IDENTIFIER, "Esperado nome do parâmetro.");
                consume(TokenType.COLON, "Esperado ':' após o nome do parâmetro para definir o tipo.");

                // ⭐ A CORONOAÇÃO DO PARSER: Leitura fractal e padronizada de tipos!
                TypeNode paramTypeNode = parseTypeAnnotation();

                parameters.add(new Stmt.Param(paramName, paramTypeNode));
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RPAREN, "Esperado ')' após os parâmetros.");

        // 6. Tipo de Retorno (ex: : int)
        TypeNode returnType = null;
        if (match(TokenType.COLON)) {
            returnType = parseTypeAnnotation();
        }

        // ⭐ NOVO: Ler a cláusula 'throws' ⭐
        List<Token> thrownExceptions = new ArrayList<>();
        if (match(TokenType.THROWS)) {
            do {
                Token errorName = consume(TokenType.IDENTIFIER, "Esperado nome da exceção após 'throws'.");
                thrownExceptions.add(errorName);
            } while (match(TokenType.COMMA)); // Suporta múltiplas: throws IOError, NetError
        }

        // ⭐ 7. A BIFURCAÇÃO DA ABSTRAÇÃO (O Grande Salto!) ⭐
        if (isAbstract) {
            // Se for um método abstrato, NÃO PODE ter corpo. Exige ponto-e-vírgula!
            consume(TokenType.SEMICOLON, "Métodos abstratos não podem ter corpo '{}'. Esperado ';' no final da assinatura.");

            // ⭐ CORREÇÃO: Passamos o 'modifier' e o 'isAbstract' para o construtor!
            return new Stmt.Function(modifier,false, isAbstract, name, parameters, returnType, thrownExceptions,null);
        } else {
            // Se for um método concreto, EXIGE as chaves e o corpo de código!
            consume(TokenType.LBRACE, "Esperado '{' antes do corpo da função concreta.");
            List<Stmt> body = block();

            // ⭐ CORREÇÃO: Passamos o 'modifier' e o 'isAbstract' para o construtor!
            return new Stmt.Function(modifier,false, isAbstract, name, parameters, returnType,thrownExceptions, body);
        }
    }

    private Stmt varDeclaration() {
        Token keyword = previous(); // Pode ser LET, VAR ou CONST

        if (keyword.type == TokenType.VAR && scopeDepth > 0) {
            throw error(keyword, "Erro de Escopo: A palavra-chave 'var' só pode ser usada ao nível do arquivo global.");
        }

        Token name = consume(TokenType.IDENTIFIER, "Esperado nome da variável.");

        // ⭐ A EVOLUÇÃO: Agora usamos a Árvore de Tipos (TypeNode) em vez de String!
        TypeNode typeAnnotation = null;
        if (match(TokenType.COLON)) {
            typeAnnotation = parseTypeAnnotation(); // Devolve um TypeNode.Simple ou TypeNode.Generic
        }

        Expr initializer = null;
        if (match(TokenType.ASSIGN)) {
            initializer = expression();
        }

        if (keyword.type == TokenType.CONST && initializer == null) {
            throw error(name, "Uma constante ('const') precisa ser inicializada com um valor.");
        }

        consume(TokenType.SEMICOLON, "Esperado ';' após a declaração da variável.");

        // O teu VarDecl agora recebe o TypeNode estruturado com sucesso!
        return new Stmt.VarDecl(keyword, name, typeAnnotation, initializer);
    }

    private TypeNode parseTypeAnnotation() {
        // ⭐ 1. A INTERCEÇÃO DO OPCIONAL ('?') ⭐
        // Se começar por '?', consome-o e chama a si próprio para ler o tipo que vem à frente!
        if (match(TokenType.QUESTION)) {
            TypeNode inner = parseTypeAnnotation();
            return new TypeNode.Optional(inner);
        }

        Token baseName;

        // 1. Lê a base do tipo (T_INT, T_STRING, IDENTIFIER, etc.)
        if (match(TokenType.IDENTIFIER, TokenType.T_INT, TokenType.T_FLOAT, TokenType.T_STRING, TokenType.T_ARRAY, TokenType.T_OBJECT, TokenType.T_ENUM)) {
            baseName = previous();
        } else {
            throw error(peek(), "Esperado nome do tipo (ex: int, String, Map).");
        }

        // 2. Verifica se existem Tipos Genéricos '< ... >'
        if (match(TokenType.LESS)) {
            java.util.List<TypeNode> generics = new java.util.ArrayList<>();
            do {
                // Recursão: Lê o tipo interior e guarda na lista (ex: String e Integer)
                generics.add(parseTypeAnnotation());
            } while (match(TokenType.COMMA));

            consume(TokenType.GREATER, "Esperado '>' após os tipos genéricos.");

            // Retorna o nó complexo!
            return new TypeNode.Generic(baseName, generics);
        }

        // Se não tiver '<', retorna um nó simples
        return new TypeNode.Simple(baseName);
    }

    // ==========================================
    // STATEMENTS (Comandos de Ação)
    // Comandos que não criam variáveis globais, mas executam lógicas (If, For, Print, Atribuições).
    // ==========================================



    private Stmt statement() {
        // Redirecionamento inteligente: Dependendo da palavra-chave inicial, escolhe a regra certa.
        if (match(TokenType.FOR)) return forDispatcher();
        // Mudei o nome para organizares melhor os teus For Loops
        if (match(TokenType.BREAK)) return breakStatement();
        if (match(TokenType.CONTINUE)) return continueStatement();
        if (match(TokenType.RETURN)) return returnStatement();
        if (match(TokenType.TRY)) return tryStatement();
        if (match(TokenType.THROW)) return throwStatement();

        // ESQUELETOS FUTUROS:
        // if (match(TokenType.WHILE)) return whileStatement();

        // Se abrir chavetas soltas, cria um escopo (bloco) isolado.
        if (match(TokenType.LBRACE)) return new Stmt.Block(block());

        // Se não for nada disso, assume que é uma expressão a tentar calcular algo (ex: a = 10; ou println("ola");)
        return expressionStatement();
    }

    // ⭐ O NOVO IF (Bloco) - Agora devolve uma Expressão! ⭐
    private Expr.If ifExpressionBlock() {
        // Lê a condição (usando a tua sintaxe)
        Expr condition = expression();
        consume(TokenType.LBRACE, "Esperado '{' após a condição do if.");
        Stmt thenBranch = new Stmt.Block(block());

        Stmt elseBranch = null;

        // Suporte a IF ELSE encadeado (A tua recursão genial adaptada!)
        if (match(TokenType.ELSE)) {
            if (match(TokenType.IF)) {
                // Chamamos a recursão, mas embrulhamos o 'Expr.If' devolvido
                // dentro de um 'ExpressionStmt' para caber no 'elseBranch' (que espera um Stmt).
                Expr.If elseIfExpr = ifExpressionBlock();
                elseBranch = new Stmt.ExpressionStmt(elseIfExpr);
            } else {
                consume(TokenType.LBRACE, "Esperado '{' após 'else'.");
                elseBranch = new Stmt.Block(block());
            }
        }

        return new Expr.If(condition, thenBranch, elseBranch);
    }

    // ⭐ O LEITOR DO SWITCH SEM BREAK ⭐
    private Expr switchExpression() {
        consume(TokenType.LPAREN, "Esperado '(' após 'switch'.");
        Expr target = expression();
        consume(TokenType.RPAREN, "Esperado ')' após o alvo do switch.");

        consume(TokenType.LBRACE, "Esperado '{' antes dos casos do switch.");

        java.util.List<Expr.SwitchCase> cases = new java.util.ArrayList<>();
        Stmt defaultBranch = null;

        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            if (match(TokenType.CASE)) {
                java.util.List<Expr> values = new java.util.ArrayList<>();
                // 1. Lê todos os valores do 'case' separados por vírgula
                do {
                    values.add(expression());
                } while (match(TokenType.COMMA));

                consume(TokenType.COLON, "Esperado ':' após os valores do caso.");

                // 2. Lê o corpo! Como reaproveitamos o statement(), ele aceita um comando solto ou um bloco {}
                Stmt body = statement();
                cases.add(new Expr.SwitchCase(values, body));

            } else if (match(TokenType.DEFAULT)) {
                consume(TokenType.COLON, "Esperado ':' após 'default'.");
                defaultBranch = statement();
            } else {
                throw error(peek(), "Esperado 'case' ou 'default' dentro do switch.");
            }
        }

        consume(TokenType.RBRACE, "Esperado '}' após o corpo do switch.");
        return new Expr.Switch(target, cases, defaultBranch);
    }

    // ⭐ O LEITOR DO MATCH COMPLEXO ⭐
    private Expr matchExpression() {
        consume(TokenType.LPAREN, "Esperado '(' após 'match'.");
        Expr target = expression();
        consume(TokenType.RPAREN, "Esperado ')' após o alvo do match.");

        consume(TokenType.LBRACE, "Esperado '{' antes dos braços do match.");

        java.util.List<Expr.MatchArm> arms = new java.util.ArrayList<>();
        Stmt defaultBranch = null;

        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            TypeNode typeTest = null;
            Expr valueTest = null;
            Expr guard = null;

            // ⭐ A TUA REGRA: Suporte simultâneo a 'default:' e 'none:'
            if (match(TokenType.DEFAULT, TokenType.NONE)) { // Garante que NONE está no teu Lexer!
                consume(TokenType.COLON, "Esperado ':' após default/none.");
                defaultBranch = statement();
                continue;
            }

            // 1. É um teste de TIPO? (Ex: type String)
            if (match(TokenType.TYPE)) {
                typeTest = parseTypeAnnotation();
                if (match(TokenType.IF)) { // type String if (len > 5)
                    guard = expression();
                }
            }
            // 2. É uma GUARDA PURA? (Ex: if (x > 100))
            else if (match(TokenType.IF)) {
                guard = expression();
            }
            // 3. É um VALOR EXATO? (Ex: 200, "OK", variavel)
            else {
                valueTest = expression();
                if (match(TokenType.IF)) { // 200 if (modo == "seguro")
                    guard = expression();
                }
            }

            consume(TokenType.COLON, "Esperado ':' após a definição do padrão.");
            Stmt body = statement(); // Lê a linha ou o bloco {}

            arms.add(new Expr.MatchArm(typeTest, valueTest, guard, body));
        }

        consume(TokenType.RBRACE, "Esperado '}' após o corpo do match.");
        return new Expr.Match(target, arms, defaultBranch);
    }

    private Stmt throwStatement() {
        Token keyword = previous();
        Expr value = expression(); // O que vamos lançar? Pode ser uma string, número ou objeto!
        consume(TokenType.SEMICOLON, "Esperado ';' após o valor do throw.");
        return new Stmt.Throw(keyword, value);
    }

    private Stmt tryStatement() {
        consume(TokenType.LBRACE, "Esperado '{' após 'try'.");
        Stmt tryBlock = new Stmt.Block(block());

        // ⭐ NOVO: Lê vários blocos catch em loop!
        java.util.List<Stmt.CatchClause> catchClauses = new java.util.ArrayList<>();
        while (match(TokenType.CATCH)) {
            consume(TokenType.LPAREN, "Esperado '(' após 'catch'.");

            Token catchName = consume(TokenType.IDENTIFIER, "Esperado nome da variável para o erro.");
            consume(TokenType.COLON, "Esperado ':' após a variável para definir o tipo de erro a capturar.");

            TypeNode catchType = parseTypeAnnotation(); // Usa o teu sistema de tipagem nativo!

            consume(TokenType.RPAREN, "Esperado ')' após o tipo do erro.");
            consume(TokenType.LBRACE, "Esperado '{' antes do bloco catch.");

            Stmt.Block catchBlock = new Stmt.Block(block());
            catchClauses.add(new Stmt.CatchClause(catchName, catchType, catchBlock));
        }

        Stmt finallyBlock = null;
        if (match(TokenType.FINALLY)) {
            consume(TokenType.LBRACE, "Esperado '{' antes do bloco finally.");
            finallyBlock = new Stmt.Block(block());
        }

        if (catchClauses.isEmpty() && finallyBlock == null) {
            throw error(previous(), "O bloco 'try' exige pelo menos um 'catch' ou 'finally'.");
        }

        return new Stmt.Try(tryBlock, catchClauses, finallyBlock);
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

        Token typeAnnotation;
        if (match(TokenType.COLON)){
            System.out.println(previous());

            if (match(TokenType.T_INT, TokenType.T_FLOAT)) {
                typeAnnotation = previous();
            } else {
                throw error(peek(), "Esperado numerico tipo válido após ':' int ou float.");
            }
        }else {
            throw error(keyword, "Esperado ':' após o nome da variável.");
        }
        Expr initializer;
        if (match(TokenType.ASSIGN)) {
            initializer = expression();
        } else {
            throw error(keyword, "Esperado '=' após o tipo da variável.");
        }
        consume(TokenType.SEMICOLON, "Esperado ';' após a declaração da variável.");

        Expr condition = expression();
        consume(TokenType.SEMICOLON, "Esperado ';' após a condição do for-c-style.");

        // ⭐ Lemos a Expressão pura!
        Expr increment = expression();

        consume(TokenType.RPAREN, "Esperado ')' após o incremento do loop for-c-style.");
        consume(TokenType.LBRACE, "Esperado '{' após a expressão do for-c-style.");

        Stmt init = new Stmt.VarDecl(keyword, name, new TypeNode.Simple(typeAnnotation), initializer);
        Stmt body = new Stmt.Block(block());

        // Passamos o 'increment' diretamente como Expr para a AST!
        return new Stmt.ForCStyle(init, condition, increment, body);
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


    private Expr objectLiteral() {
        List<Expr> keys = new ArrayList<>();
        List<Expr> values = new ArrayList<>();

        if (!check(TokenType.RBRACE)) {
            do {
                // A chave pode ser um Identificador ou uma String Literal
                Expr key = expression();
                consume(TokenType.COLON, "Esperado ':' após a chave do objeto.");
                Expr value = expression();

                keys.add(key);
                values.add(value);
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RBRACE, "Esperado '}' após o corpo do objeto.");
        return new Expr.ObjectLiteral(keys, values);
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

        // Se a expressão for um If (que já termina limpo em '}'), não exigimos o ';'
        // ⭐ A REGRA DO RETORNO IMPLÍCITO ⭐
        if (!(expr instanceof Expr.If)) {
            if (check(TokenType.SEMICOLON)) {
                advance(); // Consome o ';' limpo
            } else if (!check(TokenType.RBRACE)) {
                // Só atira erro se faltar o ';' E não for a última respiração antes de fechar a chaveta '}'!
                throw error(peek(), "Esperado ';' após a expressão.");
            }
        }

        return new Stmt.ExpressionStmt(expr);
    }

    // ==========================================
    // EXPRESSÕES (Cálculos de Valores)
    // A ESCADA DA PRECEDÊNCIA MATEMÁTICA E LÓGICA
    // Começamos na prioridade mais baixa (atribuição '=') e descemos
    // até à prioridade máxima (parêntesis '()' e literais como '10').
    // ==========================================

    private Expr expression() {
        return inlineIf(); // Inicia a escada
    }

    // ⭐ O LEITOR PYTHONIC (x if cond else y) ⭐
    private Expr inlineIf() {
        Expr expr = assignment(); // Puxa a tua base de precedência normal

        if (match(TokenType.IF)) {
            Expr condition = expression();
            consume(TokenType.ELSE, "Esperado 'else' na expressão 'if' inline (Ex: valor if cond else default).");
            Expr elseExpr = expression();

            // Truque de Mestre: Embrulhamos as expressões simples em Stmt.ExpressionStmt para caberem na AST!
            Stmt thenBranch = new Stmt.ExpressionStmt(expr);
            Stmt elseBranch = new Stmt.ExpressionStmt(elseExpr);

            return new Expr.If(condition, thenBranch, elseBranch);
        }

        return expr;
    }

    private Expr assignment() {

        // ⭐ Detetar Arrow Function de 1 parâmetro (Ex: e => e.toUpperCase()) [INTACTO!]
        if (check(TokenType.IDENTIFIER) && current + 1 < tokens.size() && tokens.get(current + 1).type == TokenType.FAT_ARROW) {
            Token param = consume(TokenType.IDENTIFIER, "Esperado nome do parâmetro da Arrow Function.");
            consume(TokenType.FAT_ARROW, "Esperado '=>' após o parâmetro.");

            Expr body = expression();
            return new Expr.ArrowFunction(param, body);
        }

        // ⭐ A PONTE DE ENGENHARIA: Em vez de equality(), chamamos o topo da hierarquia lógica!
        Expr expr = nullCoalesce();

        // 1. Atribuição Simples (=)
        if (match(TokenType.ASSIGN)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }
            else if (expr instanceof Expr.Get get) {
                return new Expr.Set(get.object, get.name, value);
            }
            else if (expr instanceof Expr.IndexAccess access) {
                return new Expr.IndexAssign(access.object, access.bracket, access.index, value);
            }

            throw error(equals, "Alvo de atribuição inválido.");
        }

        // 2. Atribuição Composta (+=, -=, *=, /=, %=, #=)
        if (match(TokenType.PLUS_ASSIGN, TokenType.MINUS_ASSIGN, TokenType.STAR_ASSIGN, TokenType.SLASH_ASSIGN, TokenType.MODULO_ASSIGN, TokenType.HASH_ASSIGN)) {
            Token operator = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Variable || expr instanceof Expr.Get || expr instanceof Expr.IndexAccess) {
                return new Expr.CompoundAssign(expr, operator, value);
            }
            throw error(operator, "Alvo de atribuição composta inválido.");
        }

        // 3. Incremento e Decremento (++, --)
        if (match(TokenType.PLUS_PLUS, TokenType.MINUS_MINUS)) {
            Token operator = previous();
            if (expr instanceof Expr.Variable || expr instanceof Expr.Get || expr instanceof Expr.IndexAccess) {
                return new Expr.Update(expr, operator, false);
            }
            throw error(operator, "Alvo inválido para incremento/decremento.");
        }

        return expr;
    }

    // ⭐ O NOVO DEGRAU DA COALESCÊNCIA ⭐
    private Expr nullCoalesce() {
        Expr expr = logicalOr(); // Desce para o OR normal
        while (match(TokenType.QUESTION_QUESTION)) { // Token '??'
            Token operator = previous();
            Expr right = logicalOr();
            expr = new Expr.NullCoalesce(expr, operator, right);
        }
        return expr;
    }

    // =========================================================================
    // A ESCADA DE PRECEDÊNCIA (Cola isto imediatamente abaixo do assignment)
    // =========================================================================

    // 1. OR Lógico (||) -> Curto-Circuito
    private Expr logicalOr() {
        Expr expr = logicalAnd();
        while (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = logicalAnd();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    // 2. AND Lógico (&&) -> Curto-Circuito
    private Expr logicalAnd() {
        Expr expr = bitwiseOr();
        while (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = bitwiseOr();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    // 3. Bitwise OR (|)
    private Expr bitwiseOr() {
        Expr expr = bitwiseXor();
        while (match(TokenType.BIT_OR)) {
            Token operator = previous(); Expr right = bitwiseXor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // 4. Bitwise XOR (^)
    private Expr bitwiseXor() {
        Expr expr = bitwiseAnd();
        while (match(TokenType.BIT_XOR)) {
            Token operator = previous(); Expr right = bitwiseAnd();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // 5. Bitwise AND (&)
    private Expr bitwiseAnd() {
        Expr expr = equality();
        while (match(TokenType.BIT_AND)) {
            Token operator = previous(); Expr right = equality();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // 6. Igualdade (==, !=, ===, !==)
    private Expr equality() {
        Expr expr = comparison();
        while (match(TokenType.EQUAL, TokenType.NOT_EQUAL, TokenType.STRICT_EQUAL, TokenType.STRICT_NOT_EQUAL)) {
            Token operator = previous(); Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // 7. Comparação e Tipagem (<, >, type, instance)
    private Expr comparison() {
        Expr expr = shift();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL, TokenType.TYPE, TokenType.INSTANCE)) {
            Token operator = previous();
            if (operator.type == TokenType.TYPE || operator.type == TokenType.INSTANCE) {
                TypeNode type = parseTypeAnnotation();
                expr = new Expr.TypeCheck(expr, operator, type);
            } else {
                Expr right = shift();
                expr = new Expr.Binary(expr, operator, right);
            }
        }
        return expr;
    }

    // 8. Deslocamento de Bits (<<, >>)
    private Expr shift() {
        Expr expr = term(); // Aqui desce para o teu term() normal (+ e -)
        while (match(TokenType.SHIFT_LEFT, TokenType.SHIFT_RIGHT)) {
            Token operator = previous(); Expr right = term();
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

    // 1. O Factor agora chama a Potência (power) em vez do cast!
    private Expr factor() {
        Expr expr = power(); // ⭐ Mudou aqui!
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.MODULO)) {
            Token operator = previous();
            Expr right = power(); // ⭐ E aqui!
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // ⭐ 2. O NOVO DEGRAU DA POTÊNCIA (Chama o cast) ⭐
    private Expr power() {
        Expr expr = cast();

        // Nota: Se no teu TokenType a potência se chamar STAR_STAR, troca POWER por STAR_STAR
        while (match(TokenType.POWER)) {
            Token operator = previous();
            Expr right = cast();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // 2. Cria a nova regra do CAST (que chama o unary por baixo)
    private Expr cast() {
        Expr expr = unary();
        while (match(TokenType.AS)) {
            Token operator = previous();
            // ⭐ A MAGIA SINTÁTICA: Se o próximo token for '!', é forçado!
            boolean isForced = match(TokenType.BANG);
            TypeNode type = parseTypeAnnotation();
            expr = new Expr.Cast(expr, operator, type, isForced);
        }
        return expr;
    }

    private Expr unary() {

        // ⭐ NOVO: Ler o typeof(expr)
        if (match(TokenType.TYPEOF)) {
            Token keyword = previous();
            consume(TokenType.LPAREN, "Esperado '(' após 'typeof'.");
            Expr expr = expression();
            consume(TokenType.RPAREN, "Esperado ')' após a expressão.");
            return new Expr.Typeof(keyword, expr);
        }

        // Resolve operadores prefixados matemáticos (ex: -10)
        if (match(TokenType.MINUS, TokenType.BANG)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return postfix(); // ⭐ Antes chamava callExpression(), agora passa pelo postfixo!
    }

    // ⭐ O DEGRAU DO UNWRAP FORÇADO ( obj! )
    private Expr postfix() {
        Expr expr = callExpression();
        while (match(TokenType.BANG)) { // Se vir um '!' logo a seguir a um identificador/expressão:
            Token bang = previous();
            expr = new Expr.Unwrap(expr, bang);
        }
        return expr;
    }

    /**
     * O Motor de Invocações Moderno.
     * Analisa coisas como função() ou funçãoRetornaFunção()()
     */
    private Expr callExpression() {
        Expr expr = primary();

        while (true) {
            if (match(TokenType.LPAREN)) {
                expr = finishCall(expr); // Chamada de Função
            }
            else if (match(TokenType.LBRACKET)) {
                expr = finishIndexAccess(expr); // Acesso a Array [ ]
            }
            // ⭐ NOVO: O Operador Ponto ( . ) ⭐
            else if (match(TokenType.DOT)) {
                Token name = consume(TokenType.IDENTIFIER, "Esperado nome do método após '.'.");
                expr = new Expr.Get(expr, name);
            }
            // ⭐ NOVO: O Encadeamento Opcional (?.)
            else if (match(TokenType.QUESTION_DOT)) {
                Token name = consume(TokenType.IDENTIFIER, "Esperado nome da propriedade após '?.'");
                expr = new Expr.OptionalChaining(expr, name);
            } else {
                break;
            }
        }
        return expr;
    }

    // NOVO MÉTODO AUXILIAR
    private Expr finishIndexAccess(Expr object) {
        Expr index = expression();
        Token bracket = consume(TokenType.RBRACKET, "Esperado ']' após o índice.");
        return new Expr.IndexAccess(object, bracket, index);
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                arguments.add(expression());
            } while (match(TokenType.COMMA));
        }

        Token paren = consume(TokenType.RPAREN, "Esperado ')' após os argumentos da função.");
        // Se o alvo a ser invocado era um 'obj?.metodo', convertemo-lo instantaneamente num OptionalCall!
        if (callee instanceof Expr.OptionalChaining) {
            Expr.OptionalChaining opt = (Expr.OptionalChaining) callee;
            return new Expr.OptionalCall(opt.object, opt.name, paren, arguments);
        }

        return new Expr.Call(callee, paren, arguments);
    }

    /**
     * AS FOLHAS DA ÁRVORE SINTÁTICA.
     * Este é o fundo da "Recursive Descent". Aqui nós deixamos de tentar procurar operações
     * e consumimos apenas os valores base puros (Números, Strings, Arrays, Identificadores).
     */
    private Expr primary() {

        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.TRUE)) return new Expr.Literal(true);
        if (match(TokenType.NULL)) return new Expr.Literal(null); // ou TokenType.NIL
        // No teu método primary():
        if (match(TokenType.IF)) {
            // Se a tua linguagem usa parêntesis obrigatórios no if (ex: if (cond)),
            // lê-os aqui antes de chamar o bloco, ou deixa o ifExpressionBlock ler!
            // (Vou assumir que o ifExpressionBlock resolve tudo segundo a tua lógica)
            return ifExpressionBlock();
        }
        if (match(TokenType.SWITCH)) {
            return switchExpression();
        }
        if (match(TokenType.MATCH)) { // Garante que MATCH está no TokenType
            return matchExpression();
        }
        if (match(TokenType.INT_LITERAL, TokenType.FLOAT_LITERAL, TokenType.STRING_LITERAL)) {
            return new Expr.Literal(previous().literal);
        }


        if (match(TokenType.THIS)) {
            return new Expr.Variable(previous());
        }

        // ⭐ NOVO: Chamada ao método do Pai (ex: super.init)
        if (match(TokenType.SUPER)) {
            Token keyword = previous();
            consume(TokenType.DOT, "Esperado '.' após a palavra 'super'.");
            Token method = consume(TokenType.IDENTIFIER, "Esperado nome do método da superclasse.");
            return new Expr.Super(keyword, method);
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

        // ⭐ NOVO: Instanciação de classes
        // Instanciação de classes (ex: new Map() ou new Map<String, Integer>())
        if (match(TokenType.NEW)) {
            Token keyword = previous();
            Token className = consume(TokenType.IDENTIFIER, "Esperado nome da classe após 'new'.");

            // ⭐ NOVO: Lê os tipos genéricos, se existirem! ⭐
            StringBuilder typeArgs = new StringBuilder();
            if (match(TokenType.LESS)) { // Se encontrou o '<'
                typeArgs.append("<");
                do {
                    // Reutilizamos a tua função que lê os tipos!
                    typeArgs.append(parseTypeAnnotation());
                } while (match(TokenType.COMMA));

                consume(TokenType.GREATER, "Esperado '>' após os argumentos genéricos.");
                typeArgs.append(">");
            }

            consume(TokenType.LPAREN, "Esperado '(' após o nome da classe.");
            List<Expr> arguments = new ArrayList<>();
            if (!check(TokenType.RPAREN)) {
                do {
                    arguments.add(expression());
                } while (match(TokenType.COMMA));
            }
            consume(TokenType.RPAREN, "Esperado ')' após os argumentos.");

            // Passamos o typeArgs.toString() para a árvore
            return new Expr.New(keyword, className, typeArgs.toString(), arguments);
        }
        throw error(peek(), "Expressão inesperada.");
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
                // Adiciona as novas palavras-chave para ele saber onde parar de saltar!
                case FUN: case VAR: case LET: case CONST:
                case FOR: case IF: case INTERFACE: case DECLARE: case IMPLEMENT:
                    return;
            }
            advance();
        }
    }



    private static class ParseException extends RuntimeException {}
}