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

    // ⭐ NOVO: Contador universal para IDs sintéticos únicos
    private int syntheticIdCounter = 0;

    private int errorCount = 0;

    // Mantemos o hasErrors() para não quebrar a lógica antiga, mas agora ele lê o contador!
    public boolean hasErrors() {
        return this.errorCount > 0;
    }

    // ⭐ NOVO: Permite extrair a quantidade exacta de bugs encontrados!
    public int getErrorCount() {
        return this.errorCount;
    }

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
                Stmt stmt = declaration();
                // ⭐ A VACINA DO NPE: Só adiciona à AST se o nó não for nulo!
                if (stmt != null) {
                    statements.add(stmt);
                }
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
        Token name = consumeIdentifierSoft( "Esperado nome do Enum.");
        consumeSoft(TokenType.LBRACE, "{", "Esperado '{' antes do corpo do Enum.");

        List<Token> constants = new ArrayList<>();

        if (!check(TokenType.RBRACE)) {
            do {
                constants.add(consumeIdentifierSoft( "Esperado nome da constante do Enum."));
            } while (match(TokenType.COMMA));
        }

        consumeSoft(TokenType.RBRACE, "}", "Esperado '}' após o corpo do Enum.");
        return new Stmt.Enum(name, constants);
    }


    private Stmt declaration() {
        // ⭐ 1. AS DUAS MOCHILAS DE METADADOS ⭐
        java.util.List<Stmt.DecoratorNode> decorators = new java.util.ArrayList<>();
        java.util.List<Stmt.DecoratorNode> listeners = new java.util.ArrayList<>();

        // Permite ler `@Anotacao` e `&Listener` em qualquer ordem!
        while (check(TokenType.AT) || check(TokenType.BIT_AND)) {
            if (match(TokenType.AT)) {
                decorators.add(parseDecoratorNode()); // O teu leitor original
            } else if (match(TokenType.BIT_AND)) {
                listeners.add(parseListenerNode());   // O novo leitor de Listeners
            }
        }

        try {
            if (match(TokenType.GLOBAL)) return globalDeclaration();
            if (match(TokenType.MODULE)) return moduleDeclaration();
            if (match(TokenType.IMPORT)) return importDeclaration();
            if (match(TokenType.EXPORT)) return exportDeclaration();

            // ⭐ 2. LER AS ESTRUTURAS DE BASE ⭐
            if (match(TokenType.DECORATOR)) {
                if (!decorators.isEmpty() || !listeners.isEmpty()) throw error(previous(), "Definições de @decorator não podem ser anotadas.");
                return decoratorDeclaration();
            }
            if (match(TokenType.LISTENER)) {
                if (!decorators.isEmpty() || !listeners.isEmpty()) throw error(previous(), "Definições de &listener não podem ser anotadas.");
                return listenerDeclaration();
            }

            if (match(TokenType.TYPE)) return typeAliasDeclaration();

            // ⭐ 3. ENTREGAR AS DUAS MOCHILAS ÀS ASTs! ⭐
            if (check(TokenType.FUN)) return functionDeclaration(decorators, listeners);
            if (match(TokenType.VAR, TokenType.LET, TokenType.CONST)) return varDeclaration(decorators, listeners);

            if (match(TokenType.INTERFACE)) return interfaceDeclaration(decorators, listeners);

            boolean isSealed = match(TokenType.SEALED);
            if (isSealed || match(TokenType.DECLARE)) {
                if (isSealed) consumeSoft(TokenType.DECLARE, "declare", "Esperado 'declare' após o modificador 'sealed'.");
                return declareDeclaration(isSealed, decorators, listeners);
            }

            if (match(TokenType.ABSTRACT)) {
                consumeSoft(TokenType.IMPLEMENT, "implement", "Esperado 'implement' após a palavra 'abstract'.");
                return implementDeclaration(true, decorators, listeners);
            }
            if (match(TokenType.IMPLEMENT)) {
                return implementDeclaration(false, decorators, listeners);
            }

            if (match(TokenType.T_ENUM)) return enumDeclaration();

            if (!decorators.isEmpty() || !listeners.isEmpty()) {
                throw error(previous(), "Anotações (@) e Listeners (&) devem estar anexados a uma declaração válida (var, fun, declare, interface...).");
            }

            return statement();
        } catch (ParseException e) {
            synchronize();
            return null;
        }
    }

    private Stmt globalDeclaration() {
        Token name = consumeIdentifierSoft( "Esperado nome da variável global.");

        TypeNode typeAnnotation = null;
        if (match(TokenType.COLON)) {
            typeAnnotation = parseTypeAnnotation(); // Lê o ':object' ou ':string'
        }

        consumeSoft(TokenType.ASSIGN, "=", "Esperado '=' após a declaração da variável global.");
        Expr initializer = expression();
        consumeSoft(TokenType.SEMICOLON, ";", "Esperado ';' no final da linha global.");

        return new Stmt.GlobalDecl(name, typeAnnotation, initializer);
    }

    // Lê: module banco.modelos;
    private Stmt moduleDeclaration() {
        Token keyword = previous();
        StringBuilder pathBuilder = new StringBuilder();

        do {
            pathBuilder.append(consumeIdentifierSoft( "Esperado nome do módulo.").lexeme);
            if (check(TokenType.DOT)) {
                advance(); // Consome o '.'
                pathBuilder.append(".");
            } else {
                break;
            }
        } while (true);

        consumeSoft(TokenType.SEMICOLON, ";", "Esperado ';' após a declaração do módulo.");
        return new Stmt.ModuleDecl(keyword, pathBuilder.toString());
    }

    // Lê as 5 variações do teu documento:
    // import x.y.Z; | import x.y.*; | import x.y.{A, B as C}; | import x.y.A as B;
    private Stmt importDeclaration() {
        StringBuilder pathBuilder = new StringBuilder();
        java.util.List<Stmt.ImportSymbol> symbols = new java.util.ArrayList<>();
        boolean isWildcard = false;

        while (true) {
            if (match(TokenType.STAR)) {
                isWildcard = true;
                break;
            }
            if (match(TokenType.LBRACE)) {
                do {
                    Token originalName = consumeIdentifierSoft( "Esperado nome do símbolo para importar.");
                    Token aliasName = null;
                    if (match(TokenType.AS)) {
                        aliasName = consumeIdentifierSoft( "Esperado alias após 'as'.");
                    }
                    symbols.add(new Stmt.ImportSymbol(originalName, aliasName));
                } while (match(TokenType.COMMA));
                consumeSoft(TokenType.RBRACE, "}", "Esperado '}' após lista de imports.");
                break;
            }

            Token part = consumeIdentifierSoft( "Esperado nome no caminho de importação.");

            if (match(TokenType.DOT)) {
                pathBuilder.append(part.lexeme).append(".");
            } else {
                Token aliasName = null;
                if (match(TokenType.AS)) {
                    aliasName = consumeIdentifierSoft( "Esperado alias após 'as'.");
                }
                symbols.add(new Stmt.ImportSymbol(part, aliasName));
                break;
            }
        }

        String path = pathBuilder.toString();
        if (path.endsWith(".")) path = path.substring(0, path.length() - 1);

        // ⭐ NOVO: Captura o modificador 'prefix' se ele existir antes do ';'
        Token prefixToken = null;
        if (match(TokenType.PREFIX)) {
            if (check(TokenType.STRING_LITERAL) || check(TokenType.IDENTIFIER)) {
                prefixToken = advance(); // Aceita tanto prefix "PDF" quanto prefix PDF
            } else {
                throw error(peek(), "Esperado uma string literal ou identificador após 'prefix'.");
            }
        }

        consumeSoft(TokenType.SEMICOLON, ";", "Esperado ';' no final do import.");
        return new Stmt.ImportDecl(path, symbols, isWildcard, prefixToken);
    }

    // ⭐ CONSTRUTOR ESTRUTURAL DO LISTENER (listener Auditoria { pub id: int; }) ⭐
    private Stmt listenerDeclaration() {
        Token name = consumeIdentifierSoft("Esperado nome do listener.");
        consumeSoft(TokenType.LBRACE, "{", "Esperado '{' antes do corpo do listener.");

        java.util.List<Stmt.FieldDecl> fields = new java.util.ArrayList<>();

        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            Token accessModifier = null;
            boolean isStatic = false, isFinal = false, isReadonly = false;

            while (match(TokenType.PUBLIC, TokenType.PRIVATE, TokenType.PROTECTED, TokenType.STATIC, TokenType.FINAL, TokenType.READONLY)) {
                Token t = previous();
                switch (t.type) {
                    case PUBLIC: case PRIVATE: case PROTECTED:
                        if (accessModifier != null) throw error(t, "Apenas podes usar um modificador de acesso.");
                        accessModifier = t;
                        break;
                    case STATIC: isStatic = true; break;
                    case FINAL: isFinal = true; break;
                    case READONLY: isReadonly = true; break;
                }
            }

            if (accessModifier == null) {
                accessModifier = new Token(TokenType.PUBLIC, "pub", null, peek().line, peek().column);
            }

            Token memberName = consumeIdentifierSoft("Esperado nome da propriedade do listener.");
            consumeSoft(TokenType.COLON, ":", "Esperado ':' após o nome da propriedade.");
            TypeNode type = parseTypeAnnotation();
            consumeSoft(TokenType.SEMICOLON, ";", "Esperado ';' no final da declaração da propriedade.");

            fields.add(new Stmt.FieldDecl(accessModifier, isStatic, isFinal, isReadonly, memberName, type));
        }

        consumeSoft(TokenType.RBRACE, "}", "Esperado '}' após o corpo do listener.");
        // Reutilizamos o DecoratorDecl na AST, pois a estrutura de dados é idêntica!
        return new Stmt.DecoratorDecl(name, fields);
    }

    // ⭐ COLHEITADOR DE LISTENERS (&Auditoria(id:12)) ⭐
    private Stmt.DecoratorNode parseListenerNode() {
        // O token '&' já foi consumido pelo match(TokenType.BIT_AND) no loop chamador!
        Token name = consumeIdentifierSoft("Esperado identificador do listener após '&'.");

        java.util.List<Expr.CallArg> arguments = new java.util.ArrayList<>();

        if (match(TokenType.LPAREN)) {
            if (!check(TokenType.RPAREN)) {
                do {
                    Token argName = null;
                    if (check(TokenType.IDENTIFIER) && peekNext().type == TokenType.COLON) {
                        argName = consumeIdentifierSoft("Esperado identificador do argumento do listener.");
                        consumeSoft(TokenType.COLON, ":", "Esperado ':' após o nome do argumento.");
                    }
                    arguments.add(new Expr.CallArg(argName, expression()));
                } while (match(TokenType.COMMA));
            }
            consumeSoft(TokenType.RPAREN, ")", "Esperado ')' após os argumentos do listener.");
        }

        return new Stmt.DecoratorNode(name, arguments);
    }

    // Lê: export declare... | export Cliente, Pessoa; | export all;
    // =========================================================================
    // ⭐ ACTUALIZAÇÃO: EXPORT DECLARATION BLINDADO E ESTRITO ⭐
    // =========================================================================
    private Stmt exportDeclaration() {
        // 1. Caso 1: export all;
        if (match(TokenType.ALL)) {
            consumeSoft(TokenType.SEMICOLON, ";", "Esperado ';' após 'export all'.");
            return new Stmt.ExportDecl(null, true);
        }

        // 2. Caso 2: Declaração embutida (export var, export fun, export global...)
        if (check(TokenType.DECLARE) || check(TokenType.FUN) || check(TokenType.VAR) ||
                check(TokenType.LET) || check(TokenType.CONST) || check(TokenType.GLOBAL)) {

            Stmt decl = declaration();
            return new Stmt.ExportDecl(decl);
        }

        // 3. Caso 3: Lista inline (export Cliente, Pessoa;)
        java.util.List<Token> exportNames = new java.util.ArrayList<>();

        // Consome o primeiro identificador
        exportNames.add(consumeIdentifierSoft("Esperado nome do símbolo para exportar."));

        // ⭐ A MURALHA CONTEXTUAL: O detetor de colisões de identificadores ⭐
        while (!check(TokenType.SEMICOLON) && !isAtEnd()) {

            if (check(TokenType.COMMA)) {
                advance(); // Consome a vírgula legítima ','
                exportNames.add(consumeIdentifierSoft("Esperado nome do próximo símbolo após a vírgula ','."));
            }
            // 💡 A MÁGICA AQUI: Se o próximo token for OUTRO identificador ou literal solto sem vírgula!
            else if (check(TokenType.IDENTIFIER) || check(TokenType.STRING_LITERAL) ||
                    check(TokenType.INT_LITERAL) || check(TokenType.FLOAT_LITERAL)) {

                // Dispara o erro correto e cristalino na coordenada exata do invasor!
                reportSoftError(peek(), "Símbolos de exportação consecutivos detetados. Os elementos devem ser estritamente separados por vírgula ','.");

                // Forçamos o avanço de uma casa para consumir o invasor e não prender o Parser em loop!
                exportNames.add(advance());
            }
            else {
                // Se encontrou qualquer outra anomalia estranha que não seja uma vírgula ou ponto e vírgula
                reportSoftError(peek(), "Caractere inválido '" + peek().lexeme + "' na listagem de exportação. Use apenas vírgulas(',') para separar os símbolos.");
                advance(); // Sincroniza o fluxo
            }
        }

        // Agora sim! O consumeSoft só vai falhar se o ';' realmente não estiver lá no fim!
        consumeSoft(TokenType.SEMICOLON, ";", "Esperado ';' no encerramento da linha de exportação.");

        return new Stmt.ExportDecl(exportNames, false);
    }

    // ⭐ O CONSTRUTOR SINTÁTICO DO ALIAS ⭐
    private Stmt typeAliasDeclaration() {
        Token name = consumeIdentifierSoft( "Esperado identificador para o nome do Alias.");
        consumeSoft(TokenType.ASSIGN, "=", "Esperado '=' após o nome do Alias.");

        // Reutilizamos a nossa coroa de ouro: a leitura fractal de tipos!
        TypeNode target = parseTypeAnnotation();

        consumeSoft(TokenType.SEMICOLON, ";", "Esperado ';' após a definição do sinónimo de tipo.");

        return new Stmt.TypeAliasDecl(name, target);
    }

    private Stmt declareDeclaration(boolean isSealed, java.util.List<Stmt.DecoratorNode> decorators, java.util.List<Stmt.DecoratorNode> listeners) {
        Token name = consumeIdentifierSoft( "Esperado nome do modelo de dados (declare).");

        // =====================================================================
        // ⭐ ENXERTO QUÂNTICO: LEITURA DE PARÂMETROS GENÉRICOS (Ex: <T, U>) ⭐
        // =====================================================================
        java.util.List<Token> typeParameters = new java.util.ArrayList<>();
        if (match(TokenType.LESS)) { // Se encontrar o caractere '<'
            do {
                typeParameters.add(consumeIdentifierSoft( "Esperado identificador do tipo genérico (ex: T)."));
            } while (match(TokenType.COMMA));
            consumeSoft(TokenType.GREATER, ">", "Esperado '>' para fechar os parâmetros genéricos.");
        }

        Token superclass = null;
        if (match(TokenType.EXTENDS)) {
            superclass = consumeIdentifierSoft( "Esperado nome do modelo pai após 'extends'.");
        }

        consumeSoft(TokenType.LBRACE, "{", "Esperado '{' antes do corpo do declare.");

        java.util.List<Stmt.FieldDecl> fields = new java.util.ArrayList<>();

        // Removida a lista de methods! O declare só guarda variáveis.

        // Percorre tudo até fechar a chaveta
        while (!check(TokenType.RBRACE) && !isAtEnd()) {

            // ⭐1. O Colecionador de Modificadores ⭐
            Token accessModifier = null;
            boolean isStatic = false, isFinal = false, isReadonly = false;

            while (match(TokenType.PUBLIC, TokenType.PRIVATE, TokenType.PROTECTED, TokenType.STATIC, TokenType.FINAL, TokenType.READONLY)) {
                Token t = previous();
                switch (t.type) {
                    case PUBLIC: case PRIVATE: case PROTECTED:
                        if (accessModifier != null) throw error(t, "Apenas podes usar um modificador de acesso (pub, priv, prot).");
                        accessModifier = t;
                        break;
                    case STATIC: isStatic = true; break;
                    case FINAL: isFinal = true; break;
                    case READONLY: isReadonly = true; break;
                }
            }

            // Se o programador não escreveu pub/priv/prot, o padrão por segurança é PRIVATE!
            if (accessModifier == null) {
                accessModifier = new Token(TokenType.PRIVATE, "priv", null, peek().line, peek().column);
            }

            Token memberName = consumeIdentifierSoft( "Esperado nome da propriedade.");
            consumeSoft(TokenType.COLON, ":", "Esperado ':' após a propriedade.");
            TypeNode type = parseTypeAnnotation();
            consumeSoft(TokenType.SEMICOLON, ";", "Esperado ';' no final da declaração.");

            fields.add(new Stmt.FieldDecl(accessModifier, isStatic, isFinal, isReadonly, memberName, type));
        }

        consumeSoft(TokenType.RBRACE, "}", "Esperado '}' após o corpo do declare.");

        // ⭐ NOTA: Atualiza a tua classe Stmt.DeclareDecl para deixar de pedir a lista de methods!
        return new Stmt.DeclareDecl(isSealed,name, superclass, fields,typeParameters, decorators, listeners);
    }

    private Stmt interfaceDeclaration(java.util.List<Stmt.DecoratorNode> decorators, java.util.List<Stmt.DecoratorNode> listeners) {
        Token name = consumeIdentifierSoft( "Esperado nome da interface.");
        consumeSoft(TokenType.LBRACE, "{", "Esperado '{' antes do corpo da interface.");

        // ⭐ 1. MUDANÇA: A lista passa a ser de FunctionSig
        java.util.List<Stmt.FunctionSig> methods = new java.util.ArrayList<>();

        while (!check(TokenType.RBRACE) && !isAtEnd()) {

            // ⭐ 1. Capturar o Modificador (Opcional na interface, mas suportado!)
            Token modifier = null;
            if (match(TokenType.PUBLIC, TokenType.PRIVATE, TokenType.PROTECTED)) { // Garante que PROT está no teu Lexer!
                modifier = previous();
            }

            consumeSoft(TokenType.FUN, "fun", "Esperada a palavra-chave 'fun' para definir um método na interface.");
            Token methodName = consumeIdentifierSoft( "Esperado nome do método.");

            // 2. Parâmetros (Mantém-se igual, mesmo que o tenhas simplificado no teu comentário)
            consumeSoft(TokenType.LPAREN, "(", "Esperado '(' após o nome do método.");
            java.util.List<Stmt.Param> parameters = new java.util.ArrayList<>();
            if (!check(TokenType.RPAREN)) {
                do {
                    Token paramName = consumeIdentifierSoft( "Esperado nome do parâmetro.");
                    consumeSoft(TokenType.COLON, ":", "Esperado ':' após o nome do parâmetro.");

                    TypeNode type = parseTypeAnnotation(); // O nosso rei quântico!

                    Expr defaultValue = null;
                    if (match(TokenType.ASSIGN)) { // Se o programador escreveu '= "825702255"'
                        defaultValue = expression();
                    }

                    parameters.add(new Stmt.Param(paramName, type, defaultValue));

                } while (match(TokenType.COMMA));
            }
            consumeSoft(TokenType.RPAREN, ")", "Esperado ')' após parâmetros.");

            // ⭐ 3. Embrulhar o Retorno na Alfândega Universal!
            TypeNode returnTypeNode = null;
            if (match(TokenType.COLON)) {
                returnTypeNode = parseTypeAnnotation();
            }

            consumeSoft(TokenType.SEMICOLON, ";", "Esperado ';' após a assinatura do método na interface.");

            // ⭐ 4. A Nova Instanciação (Ajusta os parâmetros consoante o construtor real da tua classe)
            // Se a tua classe final tiver a lista de parâmetros descomentada, envia os 'parameters' também!
            methods.add(new Stmt.FunctionSig(modifier, methodName, parameters, returnTypeNode));
        }

        consumeSoft(TokenType.RBRACE, "}", "Esperado '}' após o corpo da interface.");
        return new Stmt.InterfaceDecl(name, methods, decorators, listeners);

    }

    private Stmt implementDeclaration(boolean isAbstractImplement, java.util.List<Stmt.DecoratorNode> decorators, java.util.List<Stmt.DecoratorNode> listeners) {
        // 1. O Alvo Base (Ex: Mamifero ou Animal)
        Token targetName = consumeIdentifierSoft( "Esperado nome do modelo de dados base.");

        // =====================================================================
        // ⭐ ENXERTO QUÂNTICO: CAPTURA DO <T> NO IMPLEMENT ⭐
        // =====================================================================
        java.util.List<Token> typeParameters = new java.util.ArrayList<>();
        if (match(TokenType.LESS)) {
            do {
                typeParameters.add(consumeIdentifierSoft( "Esperado identificador do tipo genérico."));
            } while (match(TokenType.COMMA));
            consumeSoft(TokenType.GREATER, ">", "Esperado '>' para fechar os parâmetros genéricos.");
        }

        // 2. A Variante / Alias (Opcional - Ex: as Mam1)
        Token aliasName = null;
        if (match(TokenType.AS)) {
            aliasName = consumeIdentifierSoft( "Esperado nome da variante após 'as'.");
        }

        // 3. Os Contratos (Opcional - Ex: for CRUD, EXEC)
        // Como podemos ter 'abstract implement Animal {}', o 'for' nem sempre existe!
        java.util.List<Token> interfaces = new java.util.ArrayList<>();
        if (match(TokenType.FOR)) {
            do {
                interfaces.add(consumeIdentifierSoft( "Esperado nome da interface."));
            } while (match(TokenType.COMMA));
        }

        // 4. O Corpo com o Código
        consumeSoft(TokenType.LBRACE, "{", "Esperado '{' antes do corpo da implementação.");

        // ⭐ LER O BLOCO DEFAULT ⭐
        java.util.Map<String, Expr> defaultState = new java.util.HashMap<>();
        if (match(TokenType.DEFAULT)) {
            consumeSoft(TokenType.LBRACE, "{", "Esperado '{' após 'default'.");
            if (!check(TokenType.RBRACE)) {
                do {
                    Token key = consumeIdentifierSoft( "Esperado nome da propriedade no bloco default.");
                    consumeSoft(TokenType.COLON, ":", "Esperado ':' após o nome da propriedade.");
                    Expr value = expression();
                    defaultState.put(key.lexeme, value);
                } while (match(TokenType.COMMA));
            }
            consumeSoft(TokenType.RBRACE, "}", "Esperado '}' após o bloco 'default'.");
        }

        java.util.List<Stmt.Function> methods = new java.util.ArrayList<>();

        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            // ⭐ MUDANÇA AQUI: Os Métodos também têm mochilas duplas!
            java.util.List<Stmt.DecoratorNode> methodDecorators = new java.util.ArrayList<>();
            java.util.List<Stmt.DecoratorNode> methodListeners = new java.util.ArrayList<>();

            while (check(TokenType.AT) || check(TokenType.BIT_AND)) {
                if (match(TokenType.AT)) methodDecorators.add(parseDecoratorNode());
                else if (match(TokenType.BIT_AND)) methodListeners.add(parseListenerNode());
            }

            Token modifier = null;
            if (match(TokenType.PUBLIC, TokenType.PRIVATE)) modifier = previous();

            boolean isStatic = match(TokenType.STATIC);
            boolean isAbstract = match(TokenType.ABSTRACT);

            consumeSoft(TokenType.FUN, "fun", "Esperada a palavra-chave 'fun' para declarar um método.");
            Token methodName = consumeIdentifierSoft("Esperado nome do método.");

            consumeSoft(TokenType.LPAREN, "(", "Esperado '(' após o nome do método.");
            java.util.List<Stmt.Param> parameters = new java.util.ArrayList<>();
            if (!check(TokenType.RPAREN)) {
                do {
                    if (parameters.size() >= 255) throw error(peek(), "Não podes ter mais de 255 parâmetros.");
                    Token paramName = consumeIdentifierSoft("Esperado nome do parâmetro.");
                    consumeSoft(TokenType.COLON, ":", "Esperado ':' após o nome do parâmetro.");
                    TypeNode type = parseTypeAnnotation();
                    Expr defaultValue = null;
                    if (match(TokenType.ASSIGN)) defaultValue = expression();
                    parameters.add(new Stmt.Param(paramName, type, defaultValue));
                } while (match(TokenType.COMMA));
            }
            consumeSoft(TokenType.RPAREN, ")", "Esperado ')' após parâmetros.");

            TypeNode returnType = null;
            if (match(TokenType.COLON)) returnType = parseTypeAnnotation();

            java.util.List<Token> thrownExceptions = new java.util.ArrayList<>();
            if (match(TokenType.THROWS)) {
                do {
                    thrownExceptions.add(consumeIdentifierSoft("Esperado nome da exceção após 'throws'."));
                } while (match(TokenType.COMMA));
            }

            java.util.List<Stmt> body = null;
            if (isAbstract) {
                consumeSoft(TokenType.SEMICOLON, ";", "Métodos abstratos não podem ter corpo '{}'. Esperado ';' no final da assinatura.");
            } else {
                consumeSoft(TokenType.LBRACE, "{", "Esperado '{' antes do corpo do método.");
                body = block();
            }

            // ⭐ Enviamos as duas listas de metadados dos MÉTODOS
            methods.add(new Stmt.Function(modifier, isStatic, isAbstract, methodName, parameters, returnType, thrownExceptions, body, methodDecorators, methodListeners));
        }

        consumeSoft(TokenType.RBRACE, "}", "Esperado '}' após o corpo do implement.");

        return new Stmt.ImplementDecl(isAbstractImplement, targetName, aliasName, interfaces, defaultState, methods, typeParameters, decorators, listeners);
    }

    private Stmt functionDeclaration(java.util.List<Stmt.DecoratorNode> decorators, java.util.List<Stmt.DecoratorNode> listeners) {
        // 1. Modificadores de Acesso (Opcionais - Se a tua AST já suportar)
        Token modifier = null;
        if (match(TokenType.PUBLIC, TokenType.PRIVATE)) {
            modifier = previous();
        }

        // 2. Modificador Abstract (Opcional)
        boolean isAbstract = match(TokenType.ABSTRACT);

        // ⭐ 3. A NOVA REGRA DE SINTAXE: O TOKEN 'fun' É OBRIGATÓRIO ⭐
        consumeSoft(TokenType.FUN, "fun", "Esperada a palavra-chave 'fun' para declarar um método ou função.");

        // 4. Nome da Função
        Token name = consumeIdentifierSoft( "Esperado nome da função.");
        consumeSoft(TokenType.LPAREN, "(", "Esperado '(' após o nome da função.");

        // 5. Parâmetros (com a tua tipagem forte!)
        List<Stmt.Param> parameters = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                Token paramName = consumeIdentifierSoft( "Esperado nome do parâmetro.");
                consumeSoft(TokenType.COLON, ":", "Esperado ':' após o nome do parâmetro.");

                TypeNode type = parseTypeAnnotation(); // O nosso rei quântico!

                Expr defaultValue = null;
                if (match(TokenType.ASSIGN)) { // Se o programador escreveu '= "825702255"'
                    defaultValue = expression();
                }

                parameters.add(new Stmt.Param(paramName, type, defaultValue));
            } while (match(TokenType.COMMA));
        }
        consumeSoft(TokenType.RPAREN, ")", "Esperado ')' após os parâmetros.");

        // 6. Tipo de Retorno (ex: : int)
        TypeNode returnType = null;
        if (match(TokenType.COLON)) {
            returnType = parseTypeAnnotation();
        }

        // ⭐ NOVO: Ler a cláusula 'throws' ⭐
        List<Token> thrownExceptions = new ArrayList<>();
        if (match(TokenType.THROWS)) {
            do {
                Token errorName = consumeIdentifierSoft( "Esperado nome da exceção após 'throws'.");
                thrownExceptions.add(errorName);
            } while (match(TokenType.COMMA)); // Suporta múltiplas: throws IOError, NetError
        }

        // ⭐ 7. A BIFURCAÇÃO DA ABSTRAÇÃO (O Grande Salto!) ⭐
        if (isAbstract) {
            // Se for um método abstrato, NÃO PODE ter corpo. Exige ponto-e-vírgula!
            consumeSoft(TokenType.SEMICOLON, ";", "Métodos abstratos não podem ter corpo '{}'. Esperado ';' no final da assinatura.");

            // ⭐ CORREÇÃO: Passamos o 'modifier' e o 'isAbstract' para o construtor!
            return new Stmt.Function(modifier,false, isAbstract, name, parameters, returnType, thrownExceptions,null, decorators,listeners);
        } else {
            // Se for um método concreto, EXIGE as chaves e o corpo de código!
            consumeSoft(TokenType.LBRACE, "{", "Esperado '{' antes do corpo da função concreta.");
            List<Stmt> body = block();

            // ⭐ CORREÇÃO: Passamos o 'modifier' e o 'isAbstract' para o construtor!
            return new Stmt.Function(modifier,false, isAbstract, name, parameters, returnType,thrownExceptions, body, decorators,listeners);
        }
    }

    private Stmt varDeclaration(java.util.List<Stmt.DecoratorNode> decorators, java.util.List<Stmt.DecoratorNode> listeners) {
        Token keyword = previous(); // Pode ser LET, VAR ou CONST

        if (keyword.type == TokenType.VAR && scopeDepth > 0) {
            throw error(keyword, "Erro de Escopo: A palavra-chave 'var' só pode ser usada ao nível do arquivo global.");
        }

        Token name = consumeIdentifierSoft( "Esperado nome da variável.");

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

        consumeSoft(TokenType.SEMICOLON, ";", "Esperado ';' após a declaração da variável.");

        // O teu VarDecl agora recebe o TypeNode estruturado com sucesso!
        return new Stmt.VarDecl(keyword, name, typeAnnotation, initializer,decorators,listeners);
    }

    private TypeNode parseTypeAnnotation() {


        // 1. É UM TIPO DE FUNÇÃO? (Ex: (int, string) -> bool)
        if (match(TokenType.LPAREN)) {
            java.util.List<TypeNode> paramTypes = new java.util.ArrayList<>();

            // Lê a lista de tipos de parâmetros
            if (!check(TokenType.RPAREN)) {
                do {
                    paramTypes.add(parseTypeAnnotation());
                } while (match(TokenType.COMMA));
            }
            consumeSoft(TokenType.RPAREN, ")", "Esperado ')' após os tipos de parâmetros.");

            // Exige a seta (->)
            consumeSoft(TokenType.ARROW, "->", "Esperado '->' para definir o retorno do tipo de função.");

            // Lê o tipo de retorno
            TypeNode returnType = parseTypeAnnotation();

            return new TypeNode.FunctionType(paramTypes, returnType);
        }

        // ⭐ 1. A INTERCEÇÃO DO OPCIONAL ('?') ⭐
        // Se começar por '?', consome-o e chama a si próprio para ler o tipo que vem à frente!
        if (match(TokenType.QUESTION)) {
            TypeNode inner = parseTypeAnnotation();
            return new TypeNode.Optional(inner);
        }

        Token baseName;

        // 1. Lê a base do tipo (T_INT, T_STRING, IDENTIFIER, etc.)
        if (match(TokenType.IDENTIFIER, TokenType.T_INT,TokenType.T_BOOL, TokenType.T_FLOAT, TokenType.T_STRING, TokenType.T_ARRAY, TokenType.T_OBJECT, TokenType.T_ENUM)) {
            baseName = previous();
        } else {
            throw error(peek(), "Esperado nome do tipo (ex: int, string,bool, object, array, etc).");
        }

        // 2. Verifica se existem Tipos Genéricos '< ... >'
        if (match(TokenType.LESS)) {
            java.util.List<TypeNode> generics = new java.util.ArrayList<>();
            do {
                // Recursão: Lê o tipo interior e guarda na lista (ex: String e Integer)
                generics.add(parseTypeAnnotation());
            } while (match(TokenType.COMMA));

            consumeSoft(TokenType.GREATER, ">", "Esperado '>' após os tipos genéricos.");

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

        if (match(TokenType.DO)) return doWhileStatement();
        // ESQUELETOS FUTUROS:
        if (match(TokenType.WHILE)) return whileStatement();

        // Se abrir chavetas soltas, cria um escopo (bloco) isolado.
        if (match(TokenType.LBRACE)) return new Stmt.Block(block());

        // Se não for nada disso, assume que é uma expressão a tentar calcular algo (ex: a = 10; ou println("ola");)
        return expressionStatement();
    }

    private Stmt doWhileStatement() {
        // 1. Lê o bloco de código que vai ser executado pelo menos uma vez
        Stmt body = statement();

        // 2. Exige o 'while' no final
        consumeSoft(TokenType.WHILE,"while",  "Esperado 'while' no final do bloco 'do'.");
        consumeSoft(TokenType.LPAREN, "(","Esperado '(' após 'while'.");
        // 3. Lê a condição
        Expr condition = expression();

        consumeSoft(TokenType.RPAREN, ")","Esperado ')' após a condição do 'while'.");
        consumeSoft(TokenType.SEMICOLON, ";","Esperado ';' no final do laço 'do-while'.");
        return new Stmt.DoWhile(body, condition);
    }

    private Expr ternary() {
        // Desce para o próximo nível de precedência (geralmente logicalOr)
        Expr expr = nullCoalesce();

        // Se encontrar um '?', é um ternário!
        if (match(TokenType.QUESTION)) {
            Expr trueBranch = expression();

            // Exige os dois pontos DO TERNÁRIO (o TokenType.COLON isolado)
            consumeSoft(TokenType.COLON, ":","Esperado ':' após a expressão verdadeira no operador ternário.");

            // Recursão para suportar ternários encadeados (a ? b : c ? d : e)
            Expr falseBranch = ternary();

            expr = new Expr.Ternary(expr, trueBranch, falseBranch);
        }

        return expr;
    }

    // ⭐ O NOVO IF (Bloco) - Agora devolve uma Expressão! ⭐
    private Expr.If ifExpressionBlock() {
        // Lê a condição (usando a tua sintaxe)
        Expr condition = expression();
        consumeSoft(TokenType.LBRACE, "{", "Esperado '{' após a condição do if.");
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
                consumeSoft(TokenType.LBRACE, "{", "Esperado '{' após 'else'.");
                elseBranch = new Stmt.Block(block());
            }
        }

        return new Expr.If(condition, thenBranch, elseBranch);
    }

    // ⭐ O LEITOR DO SWITCH SEM BREAK ⭐
    private Expr switchExpression() {
        consumeSoft(TokenType.LPAREN, "(", "Esperado '(' após 'switch'.");
        Expr target = expression();
        consumeSoft(TokenType.RPAREN, ")", "Esperado ')' após o alvo do switch.");

        consumeSoft(TokenType.LBRACE, "{", "Esperado '{' antes dos casos do switch.");

        java.util.List<Expr.SwitchCase> cases = new java.util.ArrayList<>();
        Stmt defaultBranch = null;

        while (!check(TokenType.RBRACE) && !isAtEnd()) {

            if (match(TokenType.CASE)) {
                java.util.List<Expr> values = new java.util.ArrayList<>();
                // 1. Lê todos os valores do 'case' separados por vírgula
                do {
                    values.add(expression());
                } while (match(TokenType.COMMA));

                consumeSoft(TokenType.COLON, ":", "Esperado ':' após os valores do caso.");

                // 2. Lê o corpo! Como reaproveitamos o statement(), ele aceita um comando solto ou um bloco {}
                Stmt body = statement();
                cases.add(new Expr.SwitchCase(values, body));

            } else if (match(TokenType.DEFAULT)) {
                consumeSoft(TokenType.COLON, ":", "Esperado ':' após 'default'.");
                defaultBranch = statement();
            } else {
                throw error(peek(), "Esperado 'case' ou 'default' dentro do switch.");
            }
        }

        consumeSoft(TokenType.RBRACE, "}", "Esperado '}' após o corpo do switch.");
        return new Expr.Switch(target, cases, defaultBranch);
    }

    // ⭐ O LEITOR DO MATCH COMPLEXO ⭐
    private Expr matchExpression() {
        consumeSoft(TokenType.LPAREN, "(", "Esperado '(' após 'match'.");
        Expr target = expression();
        consumeSoft(TokenType.RPAREN, ")", "Esperado ')' após o alvo do match.");

        consumeSoft(TokenType.LBRACE, "{", "Esperado '{' antes dos braços do match.");

        java.util.List<Expr.MatchArm> arms = new java.util.ArrayList<>();
        Stmt defaultBranch = null;

        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            TypeNode typeTest = null;
            Expr valueTest = null;
            Expr guard = null;

            // ⭐ A TUA REGRA: Suporte simultâneo a 'default:' e 'none:'
            if (match(TokenType.DEFAULT, TokenType.NONE)) { // Garante que NONE está no teu Lexer!
                consumeSoft(TokenType.COLON, ":", "Esperado ':' após default/none.");
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

            consumeSoft(TokenType.COLON, ":", "Esperado ':' após a definição do padrão.");
            Stmt body = statement(); // Lê a linha ou o bloco {}

            arms.add(new Expr.MatchArm(typeTest, valueTest, guard, body));
        }

        consumeSoft(TokenType.RBRACE, "}", "Esperado '}' após o corpo do match.");
        return new Expr.Match(target, arms, defaultBranch);
    }

    private Stmt throwStatement() {
        Token keyword = previous();
        Expr value = expression(); // O que vamos lançar? Pode ser uma string, número ou objeto!
        consumeSoft(TokenType.SEMICOLON, ";", "Esperado ';' após o valor do throw.");
        return new Stmt.Throw(keyword, value);
    }

    private Stmt tryStatement() {
        consumeSoft(TokenType.LBRACE, "{", "Esperado '{' após 'try'.");
        Stmt tryBlock = new Stmt.Block(block());

        // ⭐ NOVO: Lê vários blocos catch em loop!
        java.util.List<Stmt.CatchClause> catchClauses = new java.util.ArrayList<>();
        while (match(TokenType.CATCH)) {
            consumeSoft(TokenType.LPAREN, "(", "Esperado '(' após 'catch'.");

            Token catchName = consumeIdentifierSoft( "Esperado nome da variável para o erro.");
            consumeSoft(TokenType.COLON, ":", "Esperado ':' após a variável para definir o tipo de erro a capturar.");

            TypeNode catchType = parseTypeAnnotation(); // Usa o teu sistema de tipagem nativo!

            consumeSoft(TokenType.RPAREN, ")", "Esperado ')' após o tipo do erro.");
            consumeSoft(TokenType.LBRACE, "{", "Esperado '{' antes do bloco catch.");

            Stmt.Block catchBlock = new Stmt.Block(block());
            catchClauses.add(new Stmt.CatchClause(catchName, catchType, catchBlock));
        }

        Stmt finallyBlock = null;
        if (match(TokenType.FINALLY)) {
            consumeSoft(TokenType.LBRACE, "{", "Esperado '{' antes do bloco finally.");
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
        consumeSoft(TokenType.SEMICOLON, ";", "Esperado ';' após o valor de retorno.");
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
        Token loopVar = consumeIdentifierSoft( "Esperado nome da variável após 'for'.");
        consumeSoft(TokenType.IN, "in", "Esperado 'in' após a variável do loop.");

        if (check(TokenType.LPAREN)){
            return forInRangeStatement(loopVar);
        }
        Expr iterable = expression();
        consumeSoft(TokenType.LBRACE, "{", "Esperado '{' após a expressão do for-in.");
        Stmt body = new Stmt.Block(block());
        return new Stmt.ForIn(loopVar, iterable, body);
    }

    private Stmt forCStyleStatement() {
        // TODO: Criar o nó AST Stmt.ForCStyle no ficheiro Stmt.java e processar aqui!
        // Ex: for (let i = 0; i < 10; i = i + 1) { ... }
        consumeSoft(TokenType.LPAREN, "(","Esperado '(' após a definição do loop for-c-style.");

        Token keyword = consumeSoft(TokenType.LET, "let","Erro de declaração: Apenas a palavra chave 'let' é suportada para o loop for-c-style.");

        Token name = consumeIdentifierSoft( "Esperado nome da variável.");

        Token typeAnnotation;
        if (match(TokenType.COLON)){

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
        consumeSoft(TokenType.SEMICOLON, ";", "Esperado ';' após a declaração da variável.");

        Expr condition = expression();
        consumeSoft(TokenType.SEMICOLON, ";", "Esperado ';' após a condição do for-c-style.");

        // ⭐ Lemos a Expressão pura!
        Expr increment = expression();

        consumeSoft(TokenType.RPAREN, ")", "Esperado ')' após o incremento do loop for-c-style.");
        consumeSoft(TokenType.LBRACE, "{", "Esperado '{' após a expressão do for-c-style.");

        Stmt init = new Stmt.VarDecl(keyword, name, new TypeNode.Simple(typeAnnotation), initializer,null,null);
        Stmt body = new Stmt.Block(block());

        // Passamos o 'increment' diretamente como Expr para a AST!
        return new Stmt.ForCStyle(init, condition, increment, body);
    }

    // ESQUELETO FUTURO: for a in (1, 10, 2)
    private Stmt forInRangeStatement(Token loopVar) {
        // TODO: Criar a lógica de range loop baseado na tua especificação.
        consumeSoft(TokenType.LPAREN, "(","Esperado '(' após a variável do loop.");

        Expr start = expression();
        consumeSoft(TokenType.COMMA, ",","Esperado ',' após a variável do loop.");
        Expr end = expression();

        Optional<Expr> jump = Optional.empty();
        if (match(TokenType.COMMA)){
            jump = Optional.ofNullable(expression());
        }

        consumeSoft(TokenType.RPAREN, ")", "Esperado ')' para fechar o range.");
        consumeSoft(TokenType.LBRACE, "{", "Esperado '{' antes do corpo do loop.");
        Stmt body = new Stmt.Block(block());
        return new Stmt.ForInRange(loopVar,start,end,jump,body);
    }

    // =========================================================================
// 🏛️ PARSER: ESTRUTURA SINTÁTICA DO WHILE LOOP
// =========================================================================
    private Stmt whileStatement() {
        // 1. Consome o parêntese esquerdo '(' obrigatoriamente
        consume(TokenType.LPAREN, "Esperado '(' após o comando 'while'.");

        // 2. Extrai a expressão lógica de condição (ex: i < 10)
        Expr condition = expression();

        // 3. Consome o parêntese direito ')' obrigatoriamente
        consume(TokenType.RPAREN, "Esperado ')' após a condição do 'while'.");

        // 4. Extrai o corpo do loop (pode ser uma única declaração ou um Bloco { ... })
        // Usamos statement() para dar suporte tanto a uma linha quanto a blocos estruturados
        Stmt body = statement();

        // 5. Retorna o nó da árvore pronto a ser executado pelo Interpreter
        return new Stmt.While(condition, body);
    }


    private Expr objectLiteral() {
        List<Expr> keys = new ArrayList<>();
        List<Expr> values = new ArrayList<>();

        if (!check(TokenType.RBRACE)) {
            do {
                Expr key;
                // 💡 A MAGIA: Se a chave for uma palavra sem aspas (Identificador),
                // transformamos diretamente numa String Literal!
                if (check(TokenType.IDENTIFIER)) {
                    key = new Expr.Literal(advance().lexeme);
                } else {
                    key = expression();
                }

                consumeSoft(TokenType.COLON, ":", "Esperado ':' após a chave do objeto.");
                Expr value = expression();

                keys.add(key);
                values.add(value);
            } while (match(TokenType.COMMA));
        }

        consumeSoft(TokenType.RBRACE, "}", "Esperado '}' após o corpo do objeto.");
        return new Expr.ObjectLiteral(keys, values);
    }

    private Stmt breakStatement() {
        Token keyword = previous();
        consumeSoft(TokenType.SEMICOLON, ";", "Esperado ';' após 'break'.");
        return new Stmt.Break(keyword);
    }

    private Stmt continueStatement() {
        Token keyword = previous();
        consumeSoft(TokenType.SEMICOLON, ";", "Esperado ';' após 'continue'.");
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

        consumeSoft(TokenType.RBRACE, "}", "Esperado '}' para fechar o bloco.");
        scopeDepth--; // Sai do mundo (escopo)
        return statements;
    }

    private Stmt expressionStatement() {
        Expr expr = expression();

        // ⭐ A REGRA DO RETORNO IMPLÍCITO EXPANDIDA ⭐
        // Se a expressão for um bloco que já termina em '}', não exigimos o ';'
        boolean isBlockExpr = (expr instanceof Expr.If) ||
                (expr instanceof Expr.Switch) ||
                (expr instanceof Expr.Match);

        if (!isBlockExpr) {
            if (check(TokenType.SEMICOLON)) {
                advance(); // Consome o ';' limpo
            } else if (!check(TokenType.RBRACE)) {
                // Só atira erro se faltar o ';' E não for a última respiração antes de fechar a chaveta '}'!
                throw error(peek(), "Esperado ';' após a expressão.");
            }
        }else {
            // PERDÃO SINTÁTICO: Consome o ';' silenciosamente se ele existir a seguir a um bloco!
            if (check(TokenType.SEMICOLON)) {
                advance();
            }
        }

        return new Stmt.ExpressionStmt(expr);
    }

    // =========================================================================
    // ⭐ VOLUME 15: RECUPERAÇÃO DE ERROS MADURA (SYNTHETIC TOKENS) ⭐
    // =========================================================================

    /** * 1. O Relator Tolerante: Regista o erro no terminal, mas NÃO atira a exceção fatal!
     */
    private void reportSoftError(Token token, String message) {
        this.errorCount++; // ⭐ Incrementa o contador de bugs!
        String path = (token.filePath != null) ? token.filePath : "Desconhecido";
        // O sufixo (Recuperado) mostra que o motor não entrou em pânico!
        System.err.println(path + ":" + token.line + ":" + token.column + ":\n\tErro Sintático (Recuperado): " + message);
    }

    /**
     * 2. O Injetor Sintético: Se o utilizador esquecer uma pontuação (}, ), ;),
     * este método cria o token fantasma na memória e permite à AST continuar a compilar!
     */
    private Token consumeSoft(TokenType type, String syntheticLexeme, String message) {
        if (check(type)) return advance(); // Fluxo perfeito

        // Avisa o programador do erro
        reportSoftError(peek(), message);

        // 💡 A EVOLUÇÃO: Se o token atual for um erro óbvio ou pontuação trocada,
        // avançamos uma casa para não prender o Parser num loop infinito de falsos erros!
        if (!isAtEnd() && (peek().type == TokenType.SEMICOLON || peek().type == TokenType.COMMA || peek().type == TokenType.RBRACE)) {
            advance();
        }

        // Injeta o token fantasma para a AST fechar o nó feliz
        return new Token(type, syntheticLexeme, null, peek().line, peek().column, peek().filePath);
    }


    /**
     * ⭐ NOVO: Consumidor Universal e Resiliente de Identificadores.
     * Se o nome faltar, ele injeta um identificador único na AST e prossegue.
     */
    private Token consumeIdentifierSoft(String errorMessage) {
        if (check(TokenType.IDENTIFIER)) {
            return advance(); // Se o identificador existe, segue o fluxo perfeito!
        }

        // 1. Reporta o erro suave no terminal (path:linha:coluna)
        reportSoftError(peek(), errorMessage);

        // 2. Fabrica um nome único para evitar colisões na tabela de símbolos
        syntheticIdCounter++;
        String uniqueSyntheticName = "_synthetic_id_" + syntheticIdCounter;

        // 3. Injeta o Token fantasma
        return new Token(
                TokenType.IDENTIFIER,
                uniqueSyntheticName,
                null,
                peek().line,
                peek().column,
                peek().filePath
        );
    }

    // =============================================================
    // EXPRESSÕES (Cálculos de Valores)
    // A ESCADA DA PRECEDÊNCIA MATEMÁTICA E LÓGICA
    // Começamos na prioridade mais baixa (atribuição '=') e descemos
    // até à prioridade máxima (parêntesis '()' e literais como '10').
    // =============================================================

    private Expr expression() {
        return inlineIf(); // Inicia a escada
    }

    // ⭐ O LEITOR PYTHONIC (x if cond else y) ⭐
    private Expr inlineIf() {
        Expr expr = assignment(); // Puxa a tua base de precedência normal

        // ⭐ A CURA DO BUG EM CASCATA ⭐
        // Expressões de Bloco (If, Switch, Match) já estão fechadas.
        // Não podem ser usadas como a base de um 'if' inline!
        boolean isBlockExpr = (expr instanceof Expr.If) ||
                (expr instanceof Expr.Switch) ||
                (expr instanceof Expr.Match);

        if (!isBlockExpr && match(TokenType.IF)) {
            Expr condition = expression();
            consumeSoft(TokenType.ELSE, "else", "Esperado 'else' na expressão 'if' inline (Ex: valor if cond else default).");
            Expr elseExpr = expression();

            // Truque de Mestre: Embrulhamos as expressões simples em Stmt.ExpressionStmt para caberem na AST!
            Stmt thenBranch = new Stmt.ExpressionStmt(expr);
            Stmt elseBranch = new Stmt.ExpressionStmt(elseExpr);

            return new Expr.If(condition, thenBranch, elseBranch);
        }

        return expr;
    }

    private Expr assignment() {

        // =====================================================================
        // ⭐ 1. O DETETOR UNIVERSAL DE ARROW FUNCTIONS (Múltiplos Parâmetros)
        // Suporta: () => {}, (a, b) => {}, (req: object, res: object): void => {}
        // =====================================================================
        if (check(TokenType.LPAREN)) {
            int lookahead = current;
            boolean isArrow = false;

            // Olha para a frente até encontrar a seta (=>) ou um limite (, ; {)
            while (lookahead < tokens.size() && tokens.get(lookahead).type != TokenType.EOF && tokens.get(lookahead).type != TokenType.SEMICOLON && tokens.get(lookahead).type != TokenType.LBRACE) {
                if (tokens.get(lookahead).type == TokenType.FAT_ARROW) {
                    isArrow = true;
                    break;
                }
                lookahead++;
            }

            if (isArrow) {
                consumeSoft(TokenType.LPAREN, "(", "Esperado '(' no início da Arrow Function.");

                java.util.List<Stmt.Param> arrowParams = new java.util.ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    do {
                        Token paramName = consumeIdentifierSoft("Esperado nome do parâmetro da Arrow Function.");
                        TypeNode type = null;
                        if (match(TokenType.COLON)) {
                            type = parseTypeAnnotation();
                        }
                        arrowParams.add(new Stmt.Param(paramName, type, null));
                    } while (match(TokenType.COMMA));
                }
                consumeSoft(TokenType.RPAREN, ")", "Esperado ')' após os parâmetros da Arrow Function.");

                TypeNode returnType = null;
                if (match(TokenType.COLON)) {
                    returnType = parseTypeAnnotation();
                }

                consumeSoft(TokenType.FAT_ARROW, "=>", "Esperado '=>' após a assinatura da função anónima.");
                Expr body = parseArrowBody();

                return new Expr.ArrowFunction(arrowParams, body, returnType);
            }
        }

        // =====================================================================
        // ⭐ 2. DETETAR ARROW FUNCTION CURTA (Sem parêntesis: e => e.nome)
        // =====================================================================
        if (check(TokenType.IDENTIFIER) && current + 1 < tokens.size() && tokens.get(current + 1).type == TokenType.FAT_ARROW) {
            Token paramName = advance(); // Consome o identificador
            consumeSoft(TokenType.FAT_ARROW, "=>", "Esperado '=>' após o parâmetro.");
            Expr body = parseArrowBody();

            java.util.List<Stmt.Param> singleParam = new java.util.ArrayList<>();
            singleParam.add(new Stmt.Param(paramName, null, null));

            return new Expr.ArrowFunction(singleParam, body, null);
        }

        // ⭐ A PONTE DE ENGENHARIA: Em vez de equality(), chamamos o topo da hierarquia lógica!
        Expr expr = ternary();

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

    private Expr parseArrowBody() {
        if (match(TokenType.LBRACE)) {
            // Corpo é um bloco { ... }
            List<Stmt> stmts = block(); // block() consome o '}' internamente
            return new Expr.Block(stmts);
        } else {
            return expression();
        }
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
            consumeSoft(TokenType.LPAREN, "(", "Esperado '(' após 'typeof'.");
            Expr expr = expression();
            consumeSoft(TokenType.RPAREN, ")", "Esperado ')' após a expressão.");
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
                Token name = consumeIdentifierSoft( "Esperado nome do método após '.'.");
                expr = new Expr.Get(expr, name);
            }
            // ⭐ NOVO: O Encadeamento Opcional (?.)
            else if (match(TokenType.QUESTION_DOT)) {
                Token name = consumeIdentifierSoft( "Esperado nome da propriedade ou método após '?.'");

                // ⭐ LOOKAHEAD QUÂNTICO: O token seguinte é um '(' ?
                if (match(TokenType.LPAREN)) {
                    // É uma CHAMADA OPCIONAL DE MÉTODO! ( obj?.metodo(...) )
                    expr = finishOptionalCall(expr, name);
                } else {
                    // É um ACESSO OPCIONAL DE PROPRIEDADE! ( obj?.propriedade )
                    expr = new Expr.OptionalChaining(expr, name);
                }
            }else if (match(TokenType.DOUBLE_COLON)) {
                // ⭐ CORREÇÃO AQUI: Usa um identificador neutro para recuperação de erro
                Token name = consumeSoft(TokenType.IDENTIFIER, "_meta_access_", "Esperado nome do método meta após '::'.");
                expr = new Expr.MetaAccess(expr, previous(), name);
            }else {
                break;
            }
        }
        return expr;
    }

    // O montador de pacotes para chamadas opcionais:
    private Expr finishOptionalCall(Expr calleeObject, Token methodName) {
        java.util.List<Expr.CallArg> arguments = new java.util.ArrayList<>();

        if (!check(TokenType.RPAREN)) {
            do {
                Token argName = null;
                if (check(TokenType.IDENTIFIER) && peekNext().type == TokenType.COLON) {
                    argName = consumeIdentifierSoft( "Esperado identificador do argumento.");
                    consumeSoft(TokenType.COLON, ":", "Esperado ':' após o nome do argumento.");
                }
                arguments.add(new Expr.CallArg(argName, expression()));
            } while (match(TokenType.COMMA));
        }

        Token paren = consumeSoft(TokenType.RPAREN, ")", "Esperado ')' após os argumentos da chamada opcional.");
        return new Expr.OptionalCall(calleeObject, methodName, paren, arguments);
    }

    // NOVO MÉTODO AUXILIAR
    private Expr finishIndexAccess(Expr object) {
        Expr index = expression();
        Token bracket = consumeSoft(TokenType.RBRACKET, "]", "Esperado ']' após o índice.");
        return new Expr.IndexAccess(object, bracket, index);
    }

    // ⭐ CONSTRUTOR ESTRUTURAL DO DECORADOR (decorator Logging { pub id: int; }) ⭐
    private Stmt decoratorDeclaration() {
        Token name = consumeIdentifierSoft( "Esperado nome do decorador.");
        consumeSoft(TokenType.LBRACE, "{", "Esperado '{' antes do corpo do decorador.");

        java.util.List<Stmt.FieldDecl> fields = new java.util.ArrayList<>();

        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            Token accessModifier = null;
            boolean isStatic = false, isFinal = false, isReadonly = false;

            while (match(TokenType.PUBLIC, TokenType.PRIVATE, TokenType.PROTECTED, TokenType.STATIC, TokenType.FINAL, TokenType.READONLY)) {
                Token t = previous();
                switch (t.type) {
                    case PUBLIC: case PRIVATE: case PROTECTED:
                        if (accessModifier != null) throw error(t, "Apenas podes usar um modificador de acesso.");
                        accessModifier = t;
                        break;
                    case STATIC: isStatic = true; break;
                    case FINAL: isFinal = true; break;
                    case READONLY: isReadonly = true; break;
                }
            }

            if (accessModifier == null) {
                accessModifier = new Token(TokenType.PUBLIC, "pub", null, peek().line, peek().column);
            }

            Token memberName = consumeIdentifierSoft( "Esperado nome da propriedade do decorador.");
            consumeSoft(TokenType.COLON, ":", "Esperado ':' após o nome da propriedade.");
            TypeNode type = parseTypeAnnotation();
            consumeSoft(TokenType.SEMICOLON, ";", "Esperado ';' no final da declaração da propriedade.");

            fields.add(new Stmt.FieldDecl(accessModifier, isStatic, isFinal, isReadonly, memberName, type));
        }

        consumeSoft(TokenType.RBRACE, "}", "Esperado '}' após o corpo do decorador.");
        return new Stmt.DecoratorDecl(name, fields);
    }

    // ⭐ COLHEITADOR DE METADADOS (Bivalente: @Logging(...) vs @(Context.Init)) ⭐
    private Stmt.DecoratorNode parseDecoratorNode() {
        // O token '@' já foi consumido pelo match(TokenType.AT) no loop chamador!

        // =====================================================================
        // ⭐ ROTA A: GATILHO DE SISTEMA (Ex: @(Context.Init))
        // =====================================================================
        if (match(TokenType.LPAREN)) {
            Token contextToken = consumeIdentifierSoft( "Esperado identificador 'Context' dentro de @(...)");
            consumeSoft(TokenType.DOT, ".", "Esperado '.' após 'Context'.");
            Token hookToken = consumeIdentifierSoft( "Esperado nome do gatilho (Init, Get, Set, End).");
            consumeSoft(TokenType.RPAREN, ")", "Esperado ')' para fechar a meta-anotação.");

            // Fundimos os dois tokens num só ("Context.Init") para a AST ler limpo:
            Token metaToken = new Token(
                    TokenType.IDENTIFIER,
                    contextToken.lexeme + "." + hookToken.lexeme,
                    null,
                    contextToken.line,
                    contextToken.column
            );

            return new Stmt.DecoratorNode(metaToken, java.util.Collections.emptyList());
        }

        // =====================================================================
        // ⭐ ROTA B: DECORADOR CLÁSSICO DE USUÁRIO (Ex: @Logging(id: 12))
        // =====================================================================
        Token name = consumeIdentifierSoft( "Esperado identificador do decorador após '@'.");

        java.util.List<Expr.CallArg> arguments = new java.util.ArrayList<>();

        if (match(TokenType.LPAREN)) {
            if (!check(TokenType.RPAREN)) {
                do {
                    Token argName = null;
                    if (check(TokenType.IDENTIFIER) && peekNext().type == TokenType.COLON) {
                        argName = consumeIdentifierSoft( "Esperado identificador do argumento do decorador.");
                        consumeSoft(TokenType.COLON, ":", "Esperado ':' após o nome do argumento.");
                    }
                    arguments.add(new Expr.CallArg(argName, expression()));
                } while (match(TokenType.COMMA));
            }
            consumeSoft(TokenType.RPAREN, ")", "Esperado ')' após os argumentos do decorador.");
        }

        return new Stmt.DecoratorNode(name, arguments);
    }

    private Expr finishCall(Expr callee) {
        java.util.List<Expr.CallArg> arguments = new java.util.ArrayList<>();

        if (!check(TokenType.RPAREN)) {
            do {
                Token argName = null;

                // ⭐ LOOKAHEAD: Se o token atual é uma palavra e o SEGUINTE é um dois-pontos, é nomeado!
                if (check(TokenType.IDENTIFIER) && peekNext().type == TokenType.COLON) {
                    argName = consumeIdentifierSoft( "Esperado identificador do argumento.");
                    consumeSoft(TokenType.COLON, ":", "Esperado ':' após o nome do argumento.");
                }

                Expr expr = expression();
                arguments.add(new Expr.CallArg(argName, expr));

            } while (match(TokenType.COMMA));
        }

        Token paren = consumeSoft(TokenType.RPAREN, ")", "Esperado ')' após os argumentos.");
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
            consumeSoft(TokenType.DOT, ".", "Esperado '.' após a palavra 'super'.");
            Token method = consumeIdentifierSoft( "Esperado nome do método da superclasse.");
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
            consumeSoft(TokenType.RBRACKET, "]", "Esperado ']' após os elementos do array.");
            return new Expr.ArrayLiteral(elements);
        }

        // ESQUELETO FUTURO: Construção de Objetos JavaScript-style: { "nome": "Fernando", "idade": 30 }
        if (match(TokenType.LBRACE)) {
            return objectLiteral();
        }

        if (match(TokenType.LPAREN)) {
            // O uso de parêntesis agrupa matemática (força precedência máxima).
            Expr expr = expression();
            consumeSoft(TokenType.RPAREN, ")", "Esperado ')' após a expressão.");
            return expr;
        }

        // ⭐ NOVO: Instanciação de classes (Com suporte a Argumentos Nomeados e Genéricos Reais!)
        if (match(TokenType.NEW)) {
            Token keyword = previous();
            Token className = consumeIdentifierSoft( "Esperado nome da classe após 'new'.");

            // ⭐ A CORREÇÃO: Em vez de StringBuilder, usamos uma Lista de TypeNodes!
            java.util.List<TypeNode> typeArguments = new java.util.ArrayList<>();
            if (match(TokenType.LESS)) {
                do {
                    // Guarda o objeto TypeNode intacto na memória da AST
                    typeArguments.add(parseTypeAnnotation());
                } while (match(TokenType.COMMA));

                consumeSoft(TokenType.GREATER, ">", "Esperado '>' após os argumentos genéricos.");
            }

            consumeSoft(TokenType.LPAREN, "(", "Esperado '(' após o nome da classe.");

            // =================================================================
            // ⭐ Captura de CallArgs (Nomeados/Posicionais)
            // =================================================================
            java.util.List<Expr.CallArg> arguments = new java.util.ArrayList<>();
            if (!check(TokenType.RPAREN)) {
                do {
                    Token argName = null;

                    // LOOKAHEAD: Se o token atual é um nome e o SEGUINTE é um ':', é nomeado!
                    if (check(TokenType.IDENTIFIER) && peekNext().type == TokenType.COLON) {
                        argName = consumeIdentifierSoft( "Esperado identificador do argumento.");
                        consumeSoft(TokenType.COLON, ":", "Esperado ':' após o nome do argumento.");
                    }

                    arguments.add(new Expr.CallArg(argName, expression()));

                } while (match(TokenType.COMMA));
            }
            consumeSoft(TokenType.RPAREN, ")", "Esperado ')' após os argumentos.");

            // ⭐ Passamos a Lista Estruturada em vez de uma String cega!
            return new Expr.New(keyword, className, typeArguments, arguments);
        }
        throw error(peek(), "Expressão inesperada.");
    }


    // ==========================================
    // "MÁQUINA DE COMER" (MÉTODOS UTILITÁRIOS DO PARSER)
    // Estes métodos "avançam" no array de Tokens passo a passo.
    // ==========================================

    /** Se o token actual for de um dos tipos solicitados, avança um passo e devolve true. */
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    /** Vê o token actual sem avançar (espia o que vem a seguir). */
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

    private Token peekNext() {
        return tokens.get(current + 1);
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
        this.errorCount++; // ⭐ Incrementa o contador de bugs!
        String path = (token.filePath != null) ? token.filePath : "Desconhecido";
        // Formato: C:\Caminho\arquivo.xpl:10:5
        System.err.println(path + ":" + token.line + ":" + token.column + ":\n\t Erro Sintático: " + message);
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
                case MATCH: case DECORATOR: case SWITCH: case LISTENER:
                    return;
            }
            advance();
        }
    }

    private static class ParseException extends RuntimeException {}
}