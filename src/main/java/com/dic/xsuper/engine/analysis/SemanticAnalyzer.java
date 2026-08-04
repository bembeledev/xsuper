package com.dic.xsuper.engine.analysis;

import com.dic.xsuper.engine.ast.*;
import com.dic.xsuper.engine.core.*;
import com.dic.xsuper.engine.modules.XplModuleManager;

import java.util.*;

/**
 * Analisador Semântico Robusto para a linguagem XPL.
 * Valida tipos, escopos, mutabilidade, contratos de POO, fluxo de controlo, etc.
 * Reporta erros semânticos sem interromper a análise (recuperação).
 */
public class SemanticAnalyzer implements Expr.Visitor<String>, Stmt.Visitor<Void> {

    private SemanticScope currentScope;
    private final List<SemanticError> errors = new ArrayList<>();
    // ⭐ MEMÓRIA DO LINTER: Previne Loops Infinitos de Importação (Dependências Circulares)
    private java.util.Set<String> visitedModules = new java.util.HashSet<>();
    // Estado para verificação de fluxo
    private String currentFunctionReturnType = "any";
    private boolean inLoop = false;
    private boolean inTryCatch = false;
    private int functionDepth = 0; // ⭐ Regista se estamos dentro de uma função

    private Interpreter interpreter;
    // =========================================================================
    // ⭐ RASTREADOR DE EXCEÇÕES (Checked Exceptions)
    // =========================================================================
    private final Stack<List<String>> catchStack = new Stack<>();
    private List<String> currentFunctionThrows = new ArrayList<>();
    private final Map<String, List<String>> functionThrowsRegistry = new HashMap<>();

    // Para verificação de override e métodos abstratos
    private final Stack<SemanticScope.XPLModelInfo> currentClassStack = new Stack<>();

    // 1. Atualiza o construtor para receber o Interpretador
    public SemanticAnalyzer(com.dic.xsuper.engine.core.Interpreter interpreter) {
        this.interpreter = interpreter;
        this.currentScope = new SemanticScope(null);
        // ⭐ Injeta a biblioteca lendo dinamicamente a RAM do Interpretador!
        injectStandardLibrary(interpreter);
    }

    private void injectStandardLibrary(com.dic.xsuper.engine.core.Interpreter interpreter) {
        // 1. Tipos primitivos e fundações da linguagem
        String[] primitives = {"int", "float", "string", "bool", "array", "object", "any", "null", "void", "function", "error", "declare", "interface", "enum", "type", "decorator", "class"};
        for (String p : primitives) {
            currentScope.defineType(new Token(TokenType.IDENTIFIER, p, null, 0, 0), "primitive");
        }

        // =========================================================================
        // ⭐ 2. PRIMEIRO: REGISTA OS TIPOS NATIVOS (Blueprints) ⭐
        // Lemos exclusivamente do registry_model, que é onde os Declares nascem!
        // =========================================================================
        for (String modelName : interpreter.registry_model.keySet()) {
            Token t = new Token(TokenType.IDENTIFIER, modelName, null, 0, 0);
            if (!currentScope.isTypeDefined(modelName)) {
                currentScope.defineType(t, "declare");
            }
        }

        // =========================================================================
        // ⭐ 3. SEGUNDO: REGISTA AS VARIÁVEIS NA MEMÓRIA GLOBAL ⭐
        // Lemos do globals (onde vivem as funções nativas e as instâncias estáticas de classes)
        // =========================================================================
        for (java.util.Map.Entry<String, Object> entry : interpreter.globals.values.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            Token t = new Token(TokenType.IDENTIFIER, name, null, 0, 0);

            if (value instanceof com.dic.xsuper.engine.poo.XplClass) {
                // É o objeto que representa a classe na memória (ex: Math.PI, Console.readInt())
                currentScope.define(t, "class", false, true);
            }
            else if (value instanceof com.dic.xsuper.engine.execution.XplCallable) {
                // É uma Função Nativa invocável (ex: println, shell, task_run)
                currentScope.define(t, "function", false, true);
            }
            else {
                // Outras constantes (ex: os Dicionários TYPES, VISIBILITY)
                currentScope.define(t, "any", false, true);
            }
        }
    }

    // =========================================================================
    // ⭐ A POLÍCIA DE METADADOS (ESTÁTICA) ⭐
    // =========================================================================
    private void validateMetaBags(java.util.List<Stmt.DecoratorNode> decorators, java.util.List<Stmt.DecoratorNode> listeners) {
        // 1. Vasculha a mochila dos Decoradores (@)
        if (decorators != null) {
            for (Stmt.DecoratorNode dec : decorators) {
                // Ignora gatilhos de sistema como @(Listen.Set)
                if (dec.name.lexeme.contains(".")) continue;

                // Se o programador usou '@', mas o tipo na memória é 'listener', cai a guilhotina!
                if (currentScope.isListener(dec.name.lexeme)) {
                    errors.add(new SemanticError(dec.name,
                            "Erro de Sintaxe: '" + dec.name.lexeme + "' é um Listener Reativo. Usa o símbolo '&' em vez de '@'."));
                }
            }
        }

        // 2. Vasculha a mochila dos Listeners (&)
        if (listeners != null) {
            for (Stmt.DecoratorNode lis : listeners) {
                // Se o programador usou '&', mas o tipo na memória é 'decorator', cai a guilhotina!
                if (currentScope.isDecorator(lis.name.lexeme)) {
                    errors.add(new SemanticError(lis.name,
                            "Erro de Sintaxe: '" + lis.name.lexeme + "' é um Decorador Passivo. Usa o símbolo '@' em vez de '&'."));
                }
            }
        }
    }

    public List<SemanticError> analyze(List<Stmt> statements) {
        for (Stmt stmt : statements) {
            if (stmt != null) {
                try {
                    execute(stmt);
                } catch (SemanticError e) {
                    errors.add(e);
                }
            }
        }
        return errors;
    }

    private void execute(Stmt stmt) {
        if (stmt == null) return;
        try {
            stmt.accept(this);
        } catch (SemanticError e) {
            errors.add(e);
        }
    }

    private String  evaluate(Expr expr) {
        if (expr == null) return "any";
        try {
            return expr.accept(this);
        } catch (SemanticError e) {
            errors.add(e);
            return "any";
        }
    }

    private void beginScope() {
        currentScope = new SemanticScope(currentScope);
    }

    private void endScope() {
        if (currentScope.enclosing != null) {
            currentScope = currentScope.enclosing;
        }
    }

    // =========================================================================
    // AUXILIARES DE TIPO
    // =========================================================================

    private void checkExceptionCaught(String errType, Token errorToken) {
        // 1. Verifica se estamos protegidos por um bloco try-catch ativo
        for (int i = catchStack.size() - 1; i >= 0; i--) {
            for (String catchType : catchStack.get(i)) {
                // Apanha se for 'any', 'Error', o próprio erro exato, ou um Pai (Superclasse)
                if (catchType.equals("any") || catchType.equals("Error") ||
                        errType.equals(catchType) || currentScope.isSubclass(errType, catchType)) {
                    return; // 🛡️ Salvo pelo catch!
                }
            }
        }

        // 2. Verifica se a função atual declarou que empurra o problema (throws)
        if (currentFunctionThrows != null) {
            for (String declaredType : currentFunctionThrows) {
                if (declaredType.equals("Error") || errType.equals(declaredType) || currentScope.isSubclass(errType, declaredType)) {
                    return; // 🛡️ Salvo pela delegação de responsabilidade!
                }
            }
        }

        // =====================================================================
        // ⭐ 3. VIA VERDE PARA SCRIPTS GLOBAIS
        // =====================================================================
        if (functionDepth == 0) {
            return; // É perfeitamente legal o script raiz atirar erros para forçar uma paragem geral!
        }

        // 3. A Guilhotina Bateu!
        errors.add(new SemanticError(errorToken,
                "Exceção não tratada: '" + errType + "'. A função deve capturá-la com um bloco 'try-catch' ou declarar a propagação na sua assinatura usando 'throws " + errType + "'."));
    }

    private String stringifyTypeNode(TypeNode node) {
        return switch (node) {
            case null -> "any";
            case TypeNode.Simple simple -> node.name.lexeme;
            case TypeNode.Optional optional -> "?" + stringifyTypeNode(optional.innerType);
            case TypeNode.Generic generic ->
                // simplificado: só o nome base
                    node.name.lexeme;
            case TypeNode.FunctionType functionType -> "function";
            default -> "any";
        };
    }

    private TypeNode resolveAlias(TypeNode node) {
        if (node instanceof TypeNode.Simple) {
            String name = node.name.lexeme;
            if (currentScope.isTypeAlias(name)) {
                TypeNode target = currentScope.resolveTypeAlias(name);
                if (target != null) {
                    return resolveAlias(target);
                }
            }
        } else if (node instanceof TypeNode.Optional) {
            TypeNode inner = resolveAlias(((TypeNode.Optional) node).innerType);
            return new TypeNode.Optional(inner);
        }
        return node;
    }

    private boolean isTypeCompatible(String expected, String actual) {
        if (expected.equals("function")) return actual.equals("function");
        if (expected.equals("void")) return actual.equals("void");
        if (expected.equals("any") || actual.equals("any")) return true;
        if (expected.equals(actual)) return true;
        if (expected.startsWith("?")) {
            if (actual.equals("null")) return true;
            if (expected.substring(1).equals(actual)) return true;
        }
        // Coerção numérica
        if (expected.equals("float") && actual.equals("int")) return true;
        if (expected.equals("int") && actual.equals("float")) return false; // perda de precisão, rejeitar
        // Tipos definidos por utilizador: verifica herança
        if (currentScope.isClass(actual) && currentScope.isClass(expected)) {
            return currentScope.isSubclass(actual, expected);
        }
        return false;
    }

    private boolean isTypeCompatible(TypeNode expectedNode, TypeNode actualNode) {
        String exp = stringifyTypeNode(resolveAlias(expectedNode));
        String act = stringifyTypeNode(resolveAlias(actualNode));
        return isTypeCompatible(exp, act);
    }

    private boolean isTypeCompatible(TypeNode expectedNode, String actualType) {
        String exp = stringifyTypeNode(resolveAlias(expectedNode));
        return isTypeCompatible(exp, actualType);
    }

    private String inferTypeFromLiteral(Expr.Literal lit) {
        if (lit.value == null) return "null";
        if (lit.value instanceof String) return "string";
        if (lit.value instanceof Long || lit.value instanceof Integer) return "int";
        if (lit.value instanceof Double || lit.value instanceof Float) return "float";
        if (lit.value instanceof Boolean) return "bool";
        if (lit.value instanceof List) return "array";
        if (lit.value instanceof Map) return "object";
        return "any";
    }

    // =========================================================================
    // STATEMENTS
    // =========================================================================

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        for (Stmt s : stmt.statements) {
            execute(s);
        }
        endScope();
        return null;
    }

    @Override
    public Void visitVarDeclStmt(Stmt.VarDecl stmt) {
        validateMetaBags(stmt.decorators, stmt.listeners);
        String inferredType = "any";
        if (stmt.initializer != null) {
            inferredType = evaluate(stmt.initializer);
        }
        String declaredType = (stmt.typeAnnotation != null) ? stringifyTypeNode(resolveAlias(stmt.typeAnnotation)) : inferredType;

        // Verifica se o tipo declarado existe
        if (stmt.typeAnnotation != null) {
            String typeName = stringifyTypeNode(resolveAlias(stmt.typeAnnotation));
            if (!typeName.equals("any") && !currentScope.isTypeDefined(typeName)) {
                errors.add(new SemanticError(stmt.typeAnnotation.name, "Tipo desconhecido: '" + typeName + "'"));
            }
        }

        // Verifica compatibilidade tipo declarado vs inicializador
        if (stmt.typeAnnotation != null && !inferredType.equals("any")) {
            if (!isTypeCompatible(stmt.typeAnnotation, inferredType)) {
                errors.add(new SemanticError(stmt.name,
                        "A variável '" + stmt.name.lexeme + "' exige o tipo '" +
                                stringifyTypeNode(resolveAlias(stmt.typeAnnotation)) + "', mas recebeu '" + inferredType + "'."));
            }
        }

        boolean isConst = stmt.keyword.type == TokenType.CONST;
        boolean isMutable = !isConst;
        if (isConst && stmt.initializer == null) {
            errors.add(new SemanticError(stmt.name, "Constante '" + stmt.name.lexeme + "' precisa de um inicializador."));
        }

        // Regra: var só no escopo global
        if (stmt.keyword.type == TokenType.VAR && currentScope.depth() > 0) {
            errors.add(new SemanticError(stmt.keyword, "'var' só pode ser usado no escopo global (nível 0)."));
        }

        currentScope.define(stmt.name, declaredType, isMutable, stmt.initializer != null);

        // Verifica listeners: valida se os listeners existem
        if (stmt.listeners != null) {
            for (Stmt.DecoratorNode listener : stmt.listeners) {
                String listenerName = listener.name.lexeme;
                if (!currentScope.isTypeDefined(listenerName) && !currentScope.isDecorator(listenerName)) {
                    errors.add(new SemanticError(listener.name, "Listener '" + listenerName + "' não encontrado."));
                }
            }
        }

        return null;
    }

    @Override
    public Void visitGlobalDeclStmt(Stmt.GlobalDecl stmt) {
        String inferredType = "any";
        if (stmt.initializer != null) {
            inferredType = evaluate(stmt.initializer);
        }
        String declaredType = (stmt.typeAnnotation != null) ? stringifyTypeNode(resolveAlias(stmt.typeAnnotation)) : inferredType;

        if (stmt.typeAnnotation != null) {
            String typeName = stringifyTypeNode(resolveAlias(stmt.typeAnnotation));
            if (!typeName.equals("any") && !currentScope.isTypeDefined(typeName)) {
                errors.add(new SemanticError(stmt.typeAnnotation.name, "Tipo desconhecido: '" + typeName + "'"));
            }
            if (!isTypeCompatible(stmt.typeAnnotation, inferredType)) {
                errors.add(new SemanticError(stmt.name, "Tipo incompatível na global."));
            }
        }

        // Globals são constantes por definição
        currentScope.define(stmt.name, declaredType, false, true);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        validateMetaBags(stmt.decorators, stmt.listeners);
        // ⭐ 1. MEMORIZA O QUE ESTA FUNÇÃO ATIRA
        List<String> prevThrows = currentFunctionThrows;
        currentFunctionThrows = new ArrayList<>();
        if (stmt.thrownExceptions != null) {
            for (Token t : stmt.thrownExceptions) {
                currentFunctionThrows.add(t.lexeme);
            }
        }
        if (stmt.name != null) {
            functionThrowsRegistry.put(stmt.name.lexeme, currentFunctionThrows);
        }

        String returnType = "void";
        if (stmt.returnType != null) {
            returnType = stringifyTypeNode(resolveAlias(stmt.returnType));
            if (!returnType.equals("any") && !returnType.equals("void") && !currentScope.isTypeDefined(returnType)) {
                errors.add(new SemanticError(stmt.returnType.name, "Tipo de retorno desconhecido: '" + returnType + "'"));
            }
        }

        // Valida parâmetros
        List<String> paramTypes = new ArrayList<>();
        for (Stmt.Param param : stmt.params) {
            String pType = stringifyTypeNode(resolveAlias(param.typeNode));
            if (!pType.equals("any") && !currentScope.isTypeDefined(pType)) {
                errors.add(new SemanticError(param.name, "Tipo do parâmetro '" + param.name.lexeme + "' desconhecido: '" + pType + "'"));
            }

            // ⭐ A MAGIA: Encripta o Nome + Tipo + Obrigatoriedade na mesma String!
            // Exemplo gerado: "idade:int:false" ou "codigoOperacao:string:true"
            boolean isOptional = pType.startsWith("?") || param.defaultValue != null;
            paramTypes.add(param.name.lexeme + ":" + pType + ":" + isOptional);
        }

        // Define a função no escopo
        if (stmt.name != null) {
            currentScope.defineFunction(stmt.name, returnType, paramTypes);
        }

        // Entra no escopo da função
        String prevReturnType = currentFunctionReturnType;
        currentFunctionReturnType = returnType;

        beginScope();
        functionDepth++;

        for (Stmt.Param param : stmt.params) {
            String pType = stringifyTypeNode(resolveAlias(param.typeNode));
            currentScope.define(param.name, pType, true, true);
        }

        // Verifica corpo (se não for abstrato)
        if (stmt.body != null && !stmt.isAbstract) {
            for (Stmt bodyStmt : stmt.body) {
                execute(bodyStmt);
            }
            // Se a função não tem return explícito e não é void, avisar?
            // (Opcional: verificar se todos os caminhos têm return)
        }

        functionDepth--;
        endScope();
        currentFunctionReturnType = prevReturnType;
        currentFunctionThrows = prevThrows;
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        String actualType = "void";
        if (stmt.value != null) {
            actualType = evaluate(stmt.value);
        }

        // ⭐ 2. GUILHOTINA: Se a função é 'void' e o dev colocou uma expressão no return (mesmo que seja null ou qualquer coisa)
        if (currentFunctionReturnType.equals("void") && stmt.value != null) {
            errors.add(new SemanticError(stmt.keyword,
                    "Erro de Contrato: A função não declarou tipo de retorno (é 'void'), por isso é proibido retornar valores. Declare o tipo de retorno na função (ex: ': ?V' ou ': string')."));
            return null;
        }

        if (!isTypeCompatible(currentFunctionReturnType, actualType)) {
            // ⭐ MENSAGEM CLARA PARA FUNÇÕES VOID
            if (currentFunctionReturnType.equals("void")) {
                errors.add(new SemanticError(stmt.keyword,
                        "Erro de Contrato: A função não declarou um tipo de retorno (é 'void'), mas está a tentar retornar um valor do tipo '" + actualType + "'."));
            } else {
                errors.add(new SemanticError(stmt.keyword,
                        "A função exige retorno do tipo '" + currentFunctionReturnType + "', mas tentou retornar '" + actualType + "'."));
            }
        }
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        if (!inLoop) {
            errors.add(new SemanticError(stmt.keyword, "'break' fora de um loop."));
        }
        return null;
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        if (!inLoop) {
            errors.add(new SemanticError(stmt.keyword, "'continue' fora de um loop."));
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        evaluate(stmt.condition);
        boolean prevLoop = inLoop;
        inLoop = true;
        execute(stmt.body);
        inLoop = prevLoop;
        return null;
    }

    @Override
    public Void visitDoWhileStmt(Stmt.DoWhile stmt) {
        execute(stmt.body);
        evaluate(stmt.condition);
        // O do-while já tem o corpo executado antes, mas a verificação de break/continue é igual
        boolean prevLoop = inLoop;
        inLoop = true;
        execute(stmt.body);
        inLoop = prevLoop;
        return null;
    }

    @Override
    public Void visitForCStyleStmt(Stmt.ForCStyle stmt) {
        beginScope();
        if (stmt.init != null) execute(stmt.init);
        if (stmt.condition != null) evaluate(stmt.condition);
        if (stmt.increment != null) evaluate(stmt.increment);
        boolean prevLoop = inLoop;
        inLoop = true;
        execute(stmt.body);
        inLoop = prevLoop;
        endScope();
        return null;
    }

    @Override
    public Void visitForInStmt(Stmt.ForIn stmt) {
        evaluate(stmt.iterable);
        beginScope();
        currentScope.define(stmt.loopVariable, "any", true, true);
        boolean prevLoop = inLoop;
        inLoop = true;
        execute(stmt.body);
        inLoop = prevLoop;
        endScope();
        return null;
    }

    @Override
    public Void visitForInRangeStmt(Stmt.ForInRange stmt) {
        evaluate(stmt.start);
        evaluate(stmt.end);
        stmt.jump.ifPresent(this::evaluate);
        beginScope();
        currentScope.define(stmt.loopVariable, "int", true, true);
        boolean prevLoop = inLoop;
        inLoop = true;
        execute(stmt.body);
        inLoop = prevLoop;
        endScope();
        return null;
    }

    @Override
    public Void visitTryStmt(Stmt.Try stmt) {
        boolean prevTry = inTryCatch;
        inTryCatch = true;

        // =========================================================================
        // ⭐ 1. LEVANTA OS ESCUDOS ANTES DE ENTRAR NO TRY!
        // =========================================================================
        List<String> catchTypes = new ArrayList<>();
        for (Stmt.CatchClause clause : stmt.catchClauses) {
            String cType = (clause.type != null) ? stringifyTypeNode(resolveAlias(clause.type)) : "any";
            catchTypes.add(cType);
        }
        catchStack.push(catchTypes);

        // =========================================================================
        // ⭐ 2. ENTRA NO CAMPO MINADO (Executa o tryBlock)
        // =========================================================================
        execute(stmt.tryBlock);

        // =========================================================================
        // ⭐ 3. BAIXA OS ESCUDOS! (O Try acabou)
        // =========================================================================
        catchStack.pop();

        // =========================================================================
        // ⭐ 4. A POLÍCIA DO CATCH (Valida as cláusulas e executa-as)
        // =========================================================================
        for (Stmt.CatchClause clause : stmt.catchClauses) {
            beginScope();
            String catchType = (clause.type != null) ? stringifyTypeNode(resolveAlias(clause.type)) : "any";

            if (!catchType.equals("any") && !currentScope.isTypeDefined(catchType)) {
                errors.add(new SemanticError(clause.type.name, "Tipo de exceção desconhecido: '" + catchType + "'"));
            }
            else if (!catchType.equals("any")) {
                boolean isValidError = catchType.equals("Error");

                if (!isValidError && currentScope.isClass(catchType)) {
                    isValidError = currentScope.isSubclass(catchType, "Error");
                }

                if (!isValidError) {
                    errors.add(new SemanticError(clause.type.name,
                            "Erro de Tipagem: Um bloco 'catch' só pode capturar instâncias de 'Error' ou classes que herdem dele. Tentou-se capturar: '" + catchType + "'."));
                }
            }

            currentScope.define(clause.name, catchType, true, true);
            execute(clause.body);
            endScope();
        }

        if (stmt.finallyBlock != null) execute(stmt.finallyBlock);
        inTryCatch = prevTry;
        return null;
    }

    @Override
    public Void visitThrowStmt(Stmt.Throw stmt) {
        String thrownType = evaluate(stmt.value);

        // ⭐ A GUILHOTINA ESTÁTICA DO THROW ⭐
        // Permitimos o 'any' passar para evitar falsos positivos se a variável vier de metaprogramação
        if (!thrownType.equals("any")) {
            boolean isValidError = thrownType.equals("Error");

            // Se não for o Error base, verifica se é uma classe que herda de Error
            if (!isValidError && currentScope.isClass(thrownType)) {
                isValidError = currentScope.isSubclass(thrownType, "Error");
            }

            if (!isValidError) {
                errors.add(new SemanticError(stmt.keyword,
                        "A instrução 'throw' exige uma instância do tipo 'Error' (ou herdeiros). Foi recebido um valor do tipo '" + thrownType + "'."));
            } else {
                // ⭐ SE ATIRARMOS UM ERRO VÁLIDO, TEMOS DE TER CATCH OU THROWS!
                checkExceptionCaught(thrownType, stmt.keyword);
            }
        }

        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.ExpressionStmt stmt) {
        evaluate(stmt.expression);
        return null;
    }

    // =========================================================================
    // DECLARAÇÕES DE POO
    // =========================================================================

    @Override
    public Void visitDeclareDeclStmt(Stmt.DeclareDecl stmt) {
        validateMetaBags(stmt.decorators, stmt.listeners);
        String name = stmt.name.lexeme;

        // Verifica se já existe
        if (currentScope.isTypeDefined(name)) {
            errors.add(new SemanticError(stmt.name, "Tipo '" + name + "' já declarado."));
        }

        // ⭐ 1. Regista o nome do Modelo globalmente PRIMEIRO
        currentScope.defineType(stmt.name, "declare");

        // ⭐ 2. A BOLHA DOS GENÉRICOS! Abre o escopo temporário para T, U, V...
        beginScope();

        List<String> typeParamNames = new ArrayList<>();
        if (stmt.typeParameters != null) {
            for (Token t : stmt.typeParameters) {
                currentScope.defineType(t, "generic");
                typeParamNames.add(t.lexeme);
            }
        }

        // Resolve superclass
        String superName = null;
        if (stmt.superclass != null) {
            superName = stmt.superclass.lexeme;
            if (!currentScope.isTypeDefined(superName)) {
                errors.add(new SemanticError(stmt.superclass, "Superclasse '" + superName + "' não encontrada."));
            }
        }

        // Valida campos (Agora já encontram o "T" e o "V" vivos na memória!)
        for (Stmt.FieldDecl field : stmt.fields) {
            String fieldType = stringifyTypeNode(resolveAlias(field.type));
            if (!fieldType.equals("any") && !currentScope.isTypeDefined(fieldType)) {
                errors.add(new SemanticError(field.type.name, "Tipo do campo '" + field.name.lexeme + "' desconhecido: '" + fieldType + "'"));
            }
        }

        // ⭐ 3. FECHA A BOLHA DOS GENÉRICOS (Destrói o T, U e V para não vazarem)
        endScope();

        // Guarda informação da classe para validações futuras e passa a contagem!
        // Passa os genéricos para a memória!
        int typeParamCount = (stmt.typeParameters != null) ? stmt.typeParameters.size() : 0;
        currentScope.defineClassInfo(name, superName, stmt.fields, new ArrayList<>(), stmt.isSealed, typeParamCount, typeParamNames);
        return null;
    }

    @Override
    public Void visitInterfaceDeclStmt(Stmt.InterfaceDecl stmt) {
        String name = stmt.name.lexeme;
        if (currentScope.isTypeDefined(name)) {
            errors.add(new SemanticError(stmt.name, "Interface '" + name + "' já declarada."));
        }

        // Valida assinaturas de métodos
        for (Stmt.FunctionSig sig : stmt.methods) {
            for (Stmt.Param p : sig.parameters) {
                String pType = stringifyTypeNode(resolveAlias(p.typeNode));
                if (!pType.equals("any") && !currentScope.isTypeDefined(pType)) {
                    errors.add(new SemanticError(p.name, "Tipo de parâmetro desconhecido na interface: '" + pType + "'"));
                }
            }
            if (sig.returnType != null) {
                String retType = stringifyTypeNode(resolveAlias(sig.returnType));
                if (!retType.equals("any") && !retType.equals("void") && !currentScope.isTypeDefined(retType)) {
                    errors.add(new SemanticError(sig.returnType.name, "Tipo de retorno desconhecido na interface: '" + retType + "'"));
                }
            }
        }

        currentScope.defineType(stmt.name, "interface");
        currentScope.defineInterfaceInfo(stmt);
        return null;
    }

    @Override
    public Void visitImplementDeclStmt(Stmt.ImplementDecl stmt) {
        String targetName = stmt.targetName.lexeme;

        // =================================================================
        // ⭐ O REGISTO DE VARIANTES (AS) NO LINTER ⭐
        // Se isto for um 'implement Base as Variante', temos de avisar o
        // Linter que a Variante é uma classe legítima que acaba de nascer!
        // =================================================================
        if (stmt.aliasName != null) { // ⚠️ NOTA: Ajusta 'aliasName' para o nome exato do teu campo na AST (pode ser 'alias', 'variantName', etc.)
            String variantName = stmt.aliasName.lexeme;

            if (!currentScope.isTypeDefined(variantName)) {
                // 1. Regista a variante como um tipo válido
                currentScope.defineType(stmt.aliasName, "class");

                // 2. Herança Estática: Clona o esqueleto do Pai para a Variante
                // (para que o Linter saiba que o Mam1 tem os campos do Mamifero!)
                SemanticScope.XPLModelInfo parentInfo = currentScope.getClassInfo(targetName);
                if (parentInfo != null) {
                    currentScope.defineClassInfo(variantName, targetName, parentInfo.fields, new ArrayList<>(), false, 0, new ArrayList<>());
                }
            }
        }

        if (!currentScope.isTypeDefined(targetName)) {
            errors.add(new SemanticError(stmt.targetName, "O alvo base '" + targetName + "' não foi declarado."));
        } else {
            // 1. Decoradores não podem ter implement de todo!
            if (currentScope.isDecorator(targetName)) {
                errors.add(new SemanticError(stmt.targetName,
                        "Erro de Arquitetura: Decoradores passivos (@) não podem ser implementados com métodos."));
            }
            // 2. Listeners exigem estritamente 'abstract implement'
            else if (currentScope.isListener(targetName) && !stmt.isAbstract) {
                errors.add(new SemanticError(stmt.targetName,
                        "Erro de Arquitetura: O Listener '" + targetName + "' tem de ser implementado obrigatoriamente como 'abstract implement'."));
            }
        }

        // ⭐ 1. INÍCIO: Entra na Classe e guarda os métodos!
        SemanticScope.XPLModelInfo info = currentScope.getClassInfo(targetName);
        if (info != null) {
            currentClassStack.push(info);
            info.methods.addAll(stmt.methods);
            // ⭐ A NOVA MEMÓRIA: Regista se a implementação foi marcada como abstrata!
            info.isAbstract = stmt.isAbstract;
        }

        // ⭐ 2. ADICIONA ISTO AQUI: BOLHA DOS GENÉRICOS DA CLASSE!
        beginScope();
        if (stmt.typeParameters != null) {
            for (Token t : stmt.typeParameters) {
                currentScope.defineType(t, "generic");
            }
        }

        // Verifica se é sealed (se a classe for sealed e a implementação for fora do mesmo módulo, deve ser erro)
        // Por simplicidade, não vamos validar módulos agora.

        // Verifica se a classe base é abstrata? Se for, a implementação pode ser concreta ou abstrata.
        // Não temos essa info aqui, mas podemos assumir.

        // Valida interfaces implementadas (for)
        for (Token interfaceToken : stmt.interfaces) {
            String ifaceName = interfaceToken.lexeme;
            if (!currentScope.isTypeDefined(ifaceName)) {
                errors.add(new SemanticError(interfaceToken, "Interface '" + ifaceName + "' não declarada."));
            }
        }

        // Valida métodos
        for (Stmt.Function method : stmt.methods) {

            // ⭐ NOVO: SE FOR O CONSTRUTOR, GUARDA A ASSINATURA NA MEMÓRIA!
            if (method.name.lexeme.equals("init")) {
                if (info != null) {
                    for (Stmt.Param p : method.params) {
                        info.initParamTypes.add(stringifyTypeNode(resolveAlias(p.typeNode)));
                    }
                }
            }

            // =====================================================================
            // ⭐ A AUDITORIA DO @Override NO LINTER (Análise Estática) ⭐
            // =====================================================================
            boolean hasOverride = method.decorators != null && method.decorators.stream()
                    .anyMatch(d -> d.name.lexeme.equals("Override"));

            // 1. Verifica se sobrepõe um método da superclasse
            boolean overridesSuper = false;
            if (info != null && info.superclass != null) {
                SemanticScope.XPLModelInfo superInfo = currentScope.getClassInfo(info.superclass);
                if (superInfo != null) {
                    overridesSuper = superInfo.methods.stream()
                            .anyMatch(m -> m.name.lexeme.equals(method.name.lexeme));
                }
            }

            // 2. Verifica se cumpre um contrato das interfaces implementadas
            boolean fulfillsInterface = false;
            if (stmt.interfaces != null) {
                for (Token interfaceToken : stmt.interfaces) {
                    Stmt.InterfaceDecl contract = currentScope.getInterfaceInfo(interfaceToken.lexeme);
                    if (contract != null) {
                        // Procura a assinatura do método dentro da Interface
                        if (contract.methods.stream().anyMatch(sig -> sig.name.lexeme.equals(method.name.lexeme))) {
                            fulfillsInterface = true;
                            break;
                        }
                    }
                }
            }

            boolean isConstructor = method.name.lexeme.equals("init");

            if (!isConstructor) {
                // REGRA 1: Prometeu sobrepor, mas a base/interface não o tem?
                if (hasOverride && !overridesSuper && !fulfillsInterface) {
                    errors.add(new SemanticError(method.name,
                            "Erro de Sobreposição: O método '" + method.name.lexeme + "()' está marcado com @Override, mas não sobrepõe nenhum método da superclasse nem cumpre nenhum contrato."));
                }

                // REGRA 2: Cumpriu o contrato mas esqueceu-se do @Override?
                if (!hasOverride && (overridesSuper || fulfillsInterface)) {
                    errors.add(new SemanticError(method.name,
                            "Decorador Ausente: O método '" + method.name.lexeme + "()' está a cumprir um contrato (Interface) ou a sobrepor um método pai. É obrigatório marcá-lo com @Override."));
                }
            }

            // =====================================================================

            // Valida parâmetros
            for (Stmt.Param p : method.params) {
                String pType = stringifyTypeNode(resolveAlias(p.typeNode));
                if (!pType.equals("any") && !currentScope.isTypeDefined(pType)) {
                    errors.add(new SemanticError(p.name, "Tipo de parâmetro desconhecido no método '" + method.name.lexeme + "': '" + pType + "'"));
                }
            }

            // Valida return type
            if (method.returnType != null) {
                String retType = stringifyTypeNode(resolveAlias(method.returnType));
                if (!retType.equals("any") && !retType.equals("void") && !currentScope.isTypeDefined(retType)) {
                    errors.add(new SemanticError(method.returnType.name, "Tipo de retorno desconhecido: '" + retType + "'"));
                }
            }

            // Se o método é abstract, não tem corpo (já verificado no parser)
            if (method.isAbstract && method.body != null) {
                errors.add(new SemanticError(method.name, "Método abstrato não pode ter corpo."));
            }
            if (!method.isAbstract && method.body == null) {
                errors.add(new SemanticError(method.name, "Método concreto precisa de corpo."));
            }

            // Valida corpo (se concreto)
            if (!method.isAbstract && method.body != null) {
                String prevReturn = currentFunctionReturnType;
                currentFunctionReturnType = (method.returnType != null) ? stringifyTypeNode(resolveAlias(method.returnType)) : "void";

                // =====================================================================
                // ⭐ NOVO: MEMORIZA AS EXCEÇÕES EXIGIDAS POR ESTE MÉTODO
                // =====================================================================
                List<String> prevThrows = currentFunctionThrows;
                currentFunctionThrows = new ArrayList<>();
                if (method.thrownExceptions != null) {
                    for (Token t : method.thrownExceptions) {
                        currentFunctionThrows.add(t.lexeme);
                    }
                }

                beginScope();
                for (Stmt.Param p : method.params) {
                    String pType = stringifyTypeNode(resolveAlias(p.typeNode));
                    currentScope.define(p.name, pType, true, true);
                }
                // Adiciona 'this' ao escopo (para métodos de instância)
                if (!method.isStatic) {
                    Token thisToken = new Token(TokenType.THIS, "this", null, method.name.line, method.name.column);
                    currentScope.define(thisToken, targetName, false, true);
                }
                for (Stmt bodyStmt : method.body) {
                    execute(bodyStmt);
                }
                endScope();
                currentFunctionReturnType = prevReturn;
            }
        }

        if (info != null) {
            currentClassStack.pop();
        }


        // =========================================================================
        // ⭐ NOVA MURALHA: VALIDAÇÃO DO BLOCO DEFAULT ⭐
        // =========================================================================
        if (stmt.defaultState != null) {
            if (info != null) {
                for (Map.Entry<String, Expr> entry : stmt.defaultState.entrySet()) {
                    String fieldName = entry.getKey();
                    String valueType = evaluate(entry.getValue());

                    boolean found = false;
                    for (Stmt.FieldDecl f : info.fields) {
                        if (f.name.lexeme.equals(fieldName)) {
                            found = true;
                            String expectedType = stringifyTypeNode(resolveAlias(f.type));

                            // A Guilhotina Semântica do Default
                            if (!isTypeCompatible(expectedType, valueType)) {
                                errors.add(new SemanticError(stmt.targetName,
                                        "Erro de Tipo no bloco 'default': O campo '" + fieldName + "' exige o tipo '" + expectedType + "', mas tentou-se atribuir '" + valueType + "'."));
                            }
                            break;
                        }
                    }
                    if (!found) {
                        errors.add(new SemanticError(stmt.targetName, "O campo '" + fieldName + "' não existe no 'declare " + targetName + "'."));
                    }
                }
            }
        }



        endScope();
        return null;
    }

    @Override
    public Void visitEnumStmt(Stmt.Enum stmt) {
        String name = stmt.name.lexeme;
        if (currentScope.isTypeDefined(name)) {
            errors.add(new SemanticError(stmt.name, "Enum '" + name + "' já declarado."));
        }
        // Valida variantes
        for (Stmt.EnumVariant variant : stmt.variants) {
            // Os parâmetros são apenas nomes, sem tipo (assumimos any)
        }
        currentScope.defineType(stmt.name, "enum");

        // ⭐ 2. A IDENTIDADE DE VARIÁVEL: Permite acesso a propriedades (ex: Estado.PENDENTE)
        // Registamos como uma constante (false para isMutable) inicializada (true para isInitialized).
        currentScope.define(stmt.name, "enum", false, true);

        return null;
    }

    @Override
    public Void visitTypeAliasDecl(Stmt.TypeAliasDecl stmt) {
        validateMetaBags(stmt.decorators, stmt.listeners);
        String name = stmt.name.lexeme;
        if (currentScope.isTypeDefined(name)) {
            errors.add(new SemanticError(stmt.name, "Alias '" + name + "' já declarado."));
        }

        // Resolve o alvo
        TypeNode target = resolveAlias(stmt.targetType);
        String targetType = stringifyTypeNode(target);
        if (!targetType.equals("any") && !currentScope.isTypeDefined(targetType)) {
            errors.add(new SemanticError(stmt.targetType.name, "Tipo alvo do alias desconhecido: '" + targetType + "'"));
        }

        // Guarda alias
        currentScope.defineTypeAlias(stmt.name, stmt.targetType);

        // Verifica listeners
        if (stmt.listeners != null) {
            for (Stmt.DecoratorNode listener : stmt.listeners) {
                String listenerName = listener.name.lexeme;
                if (!currentScope.isTypeDefined(listenerName) && !currentScope.isDecorator(listenerName)) {
                    errors.add(new SemanticError(listener.name, "Listener '" + listenerName + "' não encontrado."));
                }
            }
        }

        return null;
    }

    // =========================================================================
    // ⭐ 1. A POLÍCIA DOS DECORADORES (Apenas regista o Nome e Tipo)
    // =========================================================================
    @Override
    public Void visitDecoratorDeclStmt(Stmt.DecoratorDecl stmt) {
        String name = stmt.name.lexeme;

        // 1. Evita nomes duplicados
        if (currentScope.isTypeDefined(name)) {
            errors.add(new SemanticError(stmt.name, "Conflito de Nomes: O Decorador '" + name + "' já se encontra declarado neste escopo."));
        }

        // 2. Regista o tipo na memória do Linter como "decorator" para a validação do símbolo '@' funcionar!
        currentScope.defineType(stmt.name, "decorator");

        // (Opcional: Se quiseres validar os tipos dos campos internos, podes iterar stmt.fields aqui)
        return null;
    }

    // =========================================================================
    // ⭐ 2. A POLÍCIA DOS LISTENERS (Registo de Estado)
    // =========================================================================
    @Override
    public Void visitListenerDeclStmt(Stmt.ListenerDecl stmt) {
        String name = stmt.name.lexeme;

        // 1. Evita nomes duplicados
        if (currentScope.isTypeDefined(name)) {
            errors.add(new SemanticError(stmt.name, "Conflito de Nomes: O Listener '" + name + "' já se encontra declarado."));
        }

        // 2. Regista o tipo na memória do Linter como "listener" para a validação do símbolo '&' funcionar!
        currentScope.defineType(stmt.name, "listener");

        return null;
    }

    @Override
    public Void visitImplementListenerStmt(Stmt.ImplementListener stmt) {
        String targetName = stmt.targetName.lexeme;

        // 1. O Listener base existe na memória?
        if (!currentScope.isTypeDefined(targetName)) {
            errors.add(new SemanticError(stmt.targetName, "O Listener alvo '" + targetName + "' não foi declarado."));
        } else if (!currentScope.isListener(targetName)) {
            errors.add(new SemanticError(stmt.targetName, "Erro de Arquitetura: '" + targetName + "' não é um Listener válido."));
        }

        // 2. Valida e analisa cada método reativo declarado no listener
        for (Stmt.Function method : stmt.methods) {
            beginScope();

            // Injeta 'this' para que o programador possa aceder a this.property, this.value, etc.
            Token thisToken = new Token(TokenType.THIS, "this", null, method.name.line, method.name.column);
            currentScope.define(thisToken, targetName, false, true);

            // Valida os parâmetros do método
            for (Stmt.Param p : method.params) {
                String pType = stringifyTypeNode(resolveAlias(p.typeNode));
                if (!pType.equals("any") && !currentScope.isTypeDefined(pType)) {
                    errors.add(new SemanticError(p.name, "Tipo de parâmetro desconhecido: '" + pType + "'"));
                }
                currentScope.define(p.name, pType, true, true);
            }

            // Executa a análise estática do corpo do método
            if (method.body != null) {
                for (Stmt bodyStmt : method.body) {
                    execute(bodyStmt);
                }
            }
            endScope();
        }

        return null;
    }

    // =========================================================================
    // MÓDULOS E IMPORTS/EXPORTS
    // =========================================================================

    @Override
    public Void visitModuleDeclStmt(Stmt.ModuleDecl stmt) {
        // Apenas verifica se o caminho é válido (não fazemos validação profunda)
        return null;
    }

    @Override
    public Void visitImportDeclStmt(Stmt.ImportDecl stmt) {
        String moduleName = stmt.modulePath;

        // 1. O teu manager usa barras e extensão (ex: "tests/listen.xpl")
        String relativePath = moduleName.replace(".", "/") + ".xpl";

        // =================================================================
        // ⭐ A PONTE COM O TEU MANAGER (SDM) VIA INTERPRETADOR ⭐
        // =================================================================
        // Capturamos a instância do moduleManager que já vive no Interpretador!
        java.io.File file = this.interpreter.moduleManager.resolvePhysicalFile(relativePath);

        if (file == null || !file.exists()) {
            errors.add(new SemanticError(stmt.prefix, "Erro SDM (Análise Estática): O módulo '" + moduleName + "' não foi encontrado nas pastas locais nem no cofre SDM."));
            return null;
        }

        String absolutePath = file.getAbsolutePath();

        // =================================================================
        // ⭐ ESCUDO DE DEPENDÊNCIAS CIRCULARES ESTÁTICAS ⭐
        // =================================================================
        if (visitedModules.contains(absolutePath)) {
            return null;
        }
        visitedModules.add(absolutePath);

        try {
            // =================================================================
            // ⭐ A CONSTRUÇÃO DO GRAFO DE DEPENDÊNCIAS (RIGOR JAVA) ⭐
            // =================================================================
            String sourceCode;

            // Se for um pacote selado (.xplx), o Linter ignora para não explodir a memória em Compile-Time
            if (file.getName().endsWith(".xplx")) {
                return null;
            } else {
                sourceCode = java.nio.file.Files.readString(file.toPath());
            }

            // Lexer & Parser (Tua lógica nativa)
            Lexer lexer = new Lexer(sourceCode, absolutePath);
            java.util.List<Token> tokens = lexer.tokenize();

            Parser parser = new Parser(tokens);
            java.util.List<Stmt> importedStatements = parser.parse();

            // Injeta o código do ficheiro importado no ESCOPO ATUAL!
            for (Stmt s : importedStatements) {
                if (!(s instanceof Stmt.ModuleDecl)) {
                    execute(s);
                }
            }

        } catch (Exception e) {
            errors.add(new SemanticError(stmt.prefix, "Falha estática ao processar a importação de '" + moduleName + "': " + e.getMessage()));
        }

        return null;
    }

    @Override
    public Void visitExportDeclStmt(Stmt.ExportDecl stmt) {
        if (stmt.declaration != null) {
            // A declaração já será validada
            execute(stmt.declaration);
        }
        // Não validamos os símbolos inline, pois podem ser resolvidos depois.
        return null;
    }

    // =========================================================================
    // OUTROS STATEMENTS
    // =========================================================================

    @Override
    public Void visitDebuggerStmt(Stmt.Debugger stmt) {
        return null; // Sem validação
    }

    // =========================================================================
    // EXPRESSÕES
    // =========================================================================

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        return inferTypeFromLiteral(expr);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        String name = expr.name.lexeme;
        // =========================================================================
        // ⭐ O ESCUDO DE TITÂNIO DO LINTER (Fallback para Modelos Globais) ⭐
        // Se o identificador não for uma variável normal, mas for uma Classe,
        // Interface, Alias ou Decorador, permitimos a passagem porque o utilizador
        // está a tentar aceder-lhe de forma estática (Ex: Classe.estatico ou Classe::meta)
        // =========================================================================
        if (currentScope.isTypeDefined(name)) {
            // Se for uma classe, informamos o Linter de que o tipo de retorno é a própria classe.
            return currentScope.isClass(name) ? name : "any";
        }

        try {
            SemanticScope.SymbolInfo info = currentScope.resolve(expr.name);
            if (info.paramTypes != null) {
                return "function";
            }
            return info.type;
        } catch (SemanticError e) {

            if (expr.name.lexeme.equals("this")) {
                return "any";
            }

            errors.add(e);
            return "any";
        }
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        String rightType = evaluate(expr.value);
        try {
            SemanticScope.SymbolInfo info = currentScope.resolve(expr.name);
            if (!info.isMutable) {
                errors.add(new SemanticError(expr.name, "Não é possível reatribuir a constante '" + expr.name.lexeme + "'."));
            }
            if (!isTypeCompatible(info.type, rightType)) {
                errors.add(new SemanticError(expr.name,
                        "A variável '" + expr.name.lexeme + "' é do tipo '" + info.type + "', não pode receber '" + rightType + "'."));
            }
            return rightType;
        } catch (SemanticError e) {
            errors.add(e);
            return "any";
        }
    }

    @Override
    public String visitCompoundAssignExpr(Expr.CompoundAssign expr) {
        // Avalia o alvo
        String targetType = evaluate(expr.target);
        String rightType = evaluate(expr.value);

        if (targetType.equals("any") || rightType.equals("any")) return "any";
        // Verifica se o alvo é mutável (se for variável)
        if (expr.target instanceof Expr.Variable) {
            Token name = ((Expr.Variable) expr.target).name;
            try {
                SemanticScope.SymbolInfo info = currentScope.resolve(name);
                if (!info.isMutable) {
                    errors.add(new SemanticError(name, "Não é possível modificar constante '" + name.lexeme + "'."));
                }
            } catch (SemanticError e) {
                errors.add(e);
            }
        }
        // Compatibilidade de tipos para operação composta
        if (!targetType.equals("any") && !rightType.equals("any")) {
            // Simplificamos: a maioria das operações composta requerem tipos numéricos ou string para +
            if (expr.operator.type == TokenType.PLUS_ASSIGN) {
                if (!(targetType.equals("string") || targetType.equals("int") || targetType.equals("float") ||
                        rightType.equals("string") || rightType.equals("int") || rightType.equals("float"))) {
                    errors.add(new SemanticError(expr.operator, "Operação '+=' requer números ou strings."));
                }
            } else {
                if (!(targetType.equals("int") || targetType.equals("float") || rightType.equals("int") || rightType.equals("float"))) {
                    errors.add(new SemanticError(expr.operator, "Operação composta requer tipos numéricos."));
                }
            }
        }
        return targetType;
    }

    @Override
    public String visitUpdateExpr(Expr.Update expr) {
        String targetType = evaluate(expr.target);
        if (!targetType.equals("int") && !targetType.equals("float")) {
            errors.add(new SemanticError(expr.operator, "Operador de incremento/decremento requer tipo numérico."));
        }
        if (expr.target instanceof Expr.Variable) {
            Token name = ((Expr.Variable) expr.target).name;
            try {
                SemanticScope.SymbolInfo info = currentScope.resolve(name);
                if (!info.isMutable) {
                    errors.add(new SemanticError(name, "Não é possível modificar constante '" + name.lexeme + "'."));
                }
            } catch (SemanticError e) {
                errors.add(e);
            }
        }
        return targetType;
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        String leftType = evaluate(expr.left);
        String rightType = evaluate(expr.right);
        if (leftType.equals("any") || rightType.equals("any")) return "any";
        Token op = expr.operator;
        switch (op.type) {
            case PLUS:
                if (leftType.equals("string") || rightType.equals("string")) {
                    return "string";
                }
                if (!leftType.equals("int") && !leftType.equals("float") && !rightType.equals("int") && !rightType.equals("float")) {
                    errors.add(new SemanticError(op, "Operador '+' requer números ou strings."));
                    return "any";
                }
                return (leftType.equals("float") || rightType.equals("float")) ? "float" : "int";

            case MINUS:
            case STAR:
            case SLASH:
            case MODULO:
            case POWER:
                if (!leftType.matches("int|float") || !rightType.matches("int|float")) {
                    errors.add(new SemanticError(op, "Operador '" + op.lexeme + "' requer números."));
                }
                return (leftType.equals("float") || rightType.equals("float")) ? "float" : "int";

            case GREATER:
            case GREATER_EQUAL:
            case LESS:
            case LESS_EQUAL:
                if (!leftType.matches("int|float") || !rightType.matches("int|float")) {
                    errors.add(new SemanticError(op, "Operador de comparação requer números."));
                }
                return "bool";

            case EQUAL:
            case NOT_EQUAL:
            case STRICT_EQUAL:
            case STRICT_NOT_EQUAL:
                return "bool";

            case BIT_AND:
            case BIT_OR:
            case BIT_XOR:
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
                if (!leftType.matches("int|float") || !rightType.matches("int|float")) {
                    errors.add(new SemanticError(op, "Operadores bitwise requerem inteiros."));
                }
                return "int";

            default:
                return "any";
        }
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        String rightType = evaluate(expr.right);
        if (expr.operator.type == TokenType.MINUS) {
            if (!rightType.matches("int|float")) {
                errors.add(new SemanticError(expr.operator, "Operador unário '-' requer número."));
            }
            return rightType;
        } else if (expr.operator.type == TokenType.BANG) {
            return "bool";
        }
        return "any";
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        String calleeType = evaluate(expr.callee);

        // =====================================================================
        // ⭐ A POLÍCIA DE INVOCADORES (Chamar funções perigosas) ⭐
        // =====================================================================
        if (expr.callee instanceof Expr.Variable varExpr) {
            if (functionThrowsRegistry.containsKey(varExpr.name.lexeme)) {
                for (String errType : functionThrowsRegistry.get(varExpr.name.lexeme)) {
                    checkExceptionCaught(errType, expr.paren);
                }
            }
        } else if (expr.callee instanceof Expr.Get getExpr) {
            String objectType = evaluate(getExpr.object);
            if (currentScope.isClass(objectType)) {
                String currentType = objectType;
                boolean foundException = false;

                // ⭐ ELEVADOR DAS EXCEÇÕES (THROWS)
                while (currentType != null && !foundException) {
                    SemanticScope.XPLModelInfo info = currentScope.getClassInfo(currentType);
                    if (info == null) break;

                    for (Stmt.Function m : info.methods) {
                        if (m.name.lexeme.equals(getExpr.name.lexeme)) {
                            if (m.thrownExceptions != null) {
                                for (Token t : m.thrownExceptions) {
                                    checkExceptionCaught(t.lexeme, expr.paren);
                                }
                            }
                            foundException = true; // Método encontrado, para o elevador!
                            break;
                        }
                    }
                    currentType = info.superclass;
                }
            }
        }

        // ⭐ 1. AVALIA TODOS OS ARGUMENTOS PRIMEIRO
        // (Isto garante que não engolimos os erros de variáveis aninhadas)
        List<String> argTypes = new ArrayList<>();
        for (Expr.CallArg arg : expr.arguments) {
            argTypes.add(evaluate(arg.expression));
        }

        // =====================================================================
        // ⭐ 2. A ALFÂNDEGA DE ARGUMENTOS (NOMEADOS & SOBREVIVENTES) ⭐
        // =====================================================================
        if (expr.callee instanceof Expr.Variable varExpr) {
            Token name = varExpr.name;
            try {
                SemanticScope.SymbolInfo info = currentScope.resolve(name);
                if (info.paramTypes != null) {
                    int expected = info.paramTypes.size();
                    String[] mappedTypes = new String[expected];
                    Token[] mappedTokens = new Token[expected];

                    // Extrair os metadados da assinatura encriptada
                    String[] pNames = new String[expected];
                    String[] pTypes = new String[expected];
                    boolean[] pOptionals = new boolean[expected];

                    for(int i = 0; i < expected; i++) {
                        String enc = info.paramTypes.get(i);
                        if (enc.contains(":")) {
                            String[] pts = enc.split(":");
                            pNames[i] = pts[0]; pTypes[i] = pts[1]; pOptionals[i] = Boolean.parseBoolean(pts[2]);
                        } else {
                            pNames[i] = ""; pTypes[i] = enc; pOptionals[i] = false; // Fallback Nativos
                        }
                    }

                    // A) Distribuir Argumentos Nomeados (Tira pela Chave)
                    for (int i = 0; i < expr.arguments.size(); i++) {
                        Expr.CallArg arg = expr.arguments.get(i);
                        if (arg.name != null) {
                            boolean found = false;
                            for (int p = 0; p < expected; p++) {
                                if (pNames[p].equals(arg.name.lexeme)) {
                                    mappedTypes[p] = argTypes.get(i);
                                    mappedTokens[p] = arg.name;
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) errors.add(new SemanticError(arg.name, "A função '" + name.lexeme + "' não possui o parâmetro '" + arg.name.lexeme + "'."));
                        }
                    }

                    // B) Distribuir Argumentos Posicionais (Os Sobreviventes na primeira ranhura vazia!)
                    for (int i = 0; i < expr.arguments.size(); i++) {
                        Expr.CallArg arg = expr.arguments.get(i);
                        if (arg.name == null) {
                            boolean placed = false;
                            for (int p = 0; p < expected; p++) {
                                if (mappedTypes[p] == null) {
                                    mappedTypes[p] = argTypes.get(i);
                                    Token fallbackToken = expr.paren;
                                    if (arg.expression instanceof Expr.Variable v) fallbackToken = v.name;
                                    mappedTokens[p] = fallbackToken;
                                    placed = true;
                                    break;
                                }
                            }
                            if (!placed) errors.add(new SemanticError(expr.paren, "Demasiados argumentos posicionais fornecidos para a função '" + name.lexeme + "'."));
                        }
                    }

                    // C) A Guilhotina de Validação Final (Verifica Tipos e Falhas Obrigatórias)
                    for (int i = 0; i < expected; i++) {
                        if (mappedTypes[i] == null) {
                            if (!pOptionals[i]) {
                                errors.add(new SemanticError(expr.paren, "O argumento obrigatório '" + pNames[i] + "' não foi fornecido."));
                            }
                        } else {
                            if (!isTypeCompatible(pTypes[i], mappedTypes[i])) {
                                Token errTk = mappedTokens[i] != null ? mappedTokens[i] : expr.paren;
                                errors.add(new SemanticError(errTk, "O argumento '" + pNames[i] + "' espera '" + pTypes[i] + "', mas recebeu '" + mappedTypes[i] + "'."));
                            }
                        }
                    }

                    return info.type;
                }
            } catch (SemanticError e) {
                errors.add(e);
            }
        }

        if (calleeType.equals("class")) {
            if (expr.callee instanceof Expr.Variable varCls) {
                return varCls.name.lexeme;
            }
        }
        return "any";
    }

    @Override
    public String visitNewExpr(Expr.New expr) {
        String className = expr.className.lexeme;
        SemanticScope.XPLModelInfo info = currentScope.getClassInfo(className);

        if (info == null && !currentScope.isTypeDefined(className)) {
            errors.add(new SemanticError(expr.className, "Classe ou tipo '" + className + "' não declarado."));
        }

        // =========================================================================
        // ⭐ NOVA MURALHA 1: POLÍCIA DE ACESSO AO CONSTRUTOR (PRIV/PROT) ⭐
        // =========================================================================
        if (info != null) {

            // =========================================================================
            // ⭐ A GUILHOTINA: BLOQUEIA INSTANCIAÇÃO DE CLASSES ABSTRATAS
            // =========================================================================
            if (info.isAbstract) {
                errors.add(new SemanticError(expr.className,
                        "Erro de Instanciação: O modelo '" + className + "' possui uma implementação abstrata e não pode ser instanciado diretamente."));
            }

            boolean isInside = !currentClassStack.isEmpty() && currentClassStack.peek().name.equals(className);
            boolean isSubclass = !currentClassStack.isEmpty() && currentScope.isSubclass(currentClassStack.peek().name, className);

            for (Stmt.Function m : info.methods) {
                if (m.name.lexeme.equals("init")) {
                    if (m.accessModifier != null) {
                        if (m.accessModifier.type == TokenType.PRIVATE && !isInside) {
                            errors.add(new SemanticError(expr.className, "Erro de Acesso: O construtor de '" + className + "' é PRIVADO. Não pode ser instanciado diretamente."));
                        } else if (m.accessModifier.type == TokenType.PROTECTED && !isSubclass) {
                            errors.add(new SemanticError(expr.className, "Erro de Acesso: O construtor de '" + className + "' é PROTEGIDO."));
                        }
                    }
                    break; // O construtor foi encontrado e avaliado
                }
            }
        }

        Map<String, String> genericMap = new HashMap<>();

        // 1. Valida a quantidade de Genéricos (<int, string, float>)
        if (info != null) {
            int expectedGenerics = info.typeParamCount;
            int actualGenerics = (expr.typeArguments != null) ? expr.typeArguments.size() : 0;

            if (expectedGenerics > 0 && actualGenerics != expectedGenerics) {
                errors.add(new SemanticError(expr.className,
                        "Aridade Genérica Incorreta: A classe '" + className + "' exige " + expectedGenerics +
                                " parâmetro(s) de tipo, mas forneceste " + actualGenerics + "."));
            } else if (expectedGenerics > 0) {
                // Monta o mapa de tradução quântica (Ex: T = int, V = float)
                for (int i = 0; i < expectedGenerics; i++) {
                    genericMap.put(info.typeParameters.get(i), stringifyTypeNode(resolveAlias(expr.typeArguments.get(i))));
                }
            }
        }

        // 2. Avalia todos os argumentos passados
        List<String> actualArgTypes = new ArrayList<>();
        for (Expr.CallArg arg : expr.arguments) {
            actualArgTypes.add(evaluate(arg.expression));
        }

        // ⭐ 3. A NOVA MURALHA: VALIDAÇÃO DO CONSTRUTOR COM TRANSMUTAÇÃO ⭐
        if (info != null && !info.initParamTypes.isEmpty()) {
            int expectedArgs = info.initParamTypes.size();
            int actualArgs = actualArgTypes.size();

            if (expectedArgs != actualArgs) {
                errors.add(new SemanticError(expr.className, "O construtor de '" + className + "' exige " + expectedArgs + " argumento(s), mas forneceste " + actualArgs + "."));
            } else {
                for (int i = 0; i < expectedArgs; i++) {
                    String expectedType = info.initParamTypes.get(i);

                    // Transmuta T, U, V para os tipos reais da instanciação!
                    if (expectedType.startsWith("?")) {
                        String baseType = expectedType.substring(1);
                        if (genericMap.containsKey(baseType)) {
                            expectedType = "?" + genericMap.get(baseType);
                        }
                    } else if (genericMap.containsKey(expectedType)) {
                        expectedType = genericMap.get(expectedType);
                    }

                    String actualType = actualArgTypes.get(i);

                    // A Guilhotina Bateu!
                    if (!isTypeCompatible(expectedType, actualType)) {
                        // Encontra o token apropriado para a linha vermelha no VSCode
                        Token errorToken = expr.className;
                        if (expr.arguments.get(i).expression instanceof Expr.Variable) {
                            errorToken = ((Expr.Variable) expr.arguments.get(i).expression).name;
                        }

                        errors.add(new SemanticError(errorToken,
                                "Erro de Tipo no construtor de '" + className + "': o argumento " + (i + 1) +
                                        " esperava o tipo '" + expectedType + "', mas recebeu '" + actualType + "'."));
                    }
                }
            }
        }

        if (expr.anonymousMethods != null) {
            for (Stmt.Function method : expr.anonymousMethods) {
                execute(method);
            }
        }

        // =========================================================================
        // ⭐ A POLÍCIA DOS CONTRATOS ANÓNIMOS (@Override) E ISOLAMENTO ⭐
        // =========================================================================
        if (expr.anonymousMethods != null) {
            // ⭐ A BOLHA ANÓNIMA
            beginScope();

            // ⭐ Recolhemos os métodos base quer venham de uma Classe ou de uma Interface!
            List<Stmt.Function> baseMethods = new ArrayList<>();
            if (info != null) {
                baseMethods.addAll(info.methods);
            } else if (currentScope.isInterfaceDefined(className)) {
                Stmt.InterfaceDecl ifaceDecl = currentScope.getInterfaceInfo(className);
                if (ifaceDecl != null && ifaceDecl.methods != null) {
                    for (Stmt.FunctionSig sig : ifaceDecl.methods) {
                        baseMethods.add(new Stmt.Function(
                                null, false, true, sig.name, sig.parameters, sig.returnType, null, null, null, null
                        ));
                    }
                }
            }

            // =========================================================================
            // ⭐ 3. A GUILHOTINA DOS MÉTODOS OBRIGATÓRIOS (Missing Implementation) ⭐
            // =========================================================================
            for (Stmt.Function baseMethod : baseMethods) {
                // Se o método base for abstrato (Interfaces são 100% abstratas), é obrigatório!
                if (baseMethod.isAbstract) {
                    boolean isImplemented = false;
                    for (Stmt.Function anonMethod : expr.anonymousMethods) {
                        if (anonMethod.name.lexeme.equals(baseMethod.name.lexeme)) {
                            isImplemented = true;
                            break;
                        }
                    }

                    if (!isImplemented) {
                        errors.add(new SemanticError(expr.className,
                                "Erro de Contrato: A classe anónima não implementou o método obrigatório '" + baseMethod.name.lexeme + "()' exigido por '" + className + "'."));
                    }
                }
            }

            // =========================================================================
            // 4. Validação de Falsos Overrides (o que já tinhas)
            // =========================================================================
            for (Stmt.Function method : expr.anonymousMethods) {
                boolean hasOverride = method.decorators != null && method.decorators.stream()
                        .anyMatch(d -> d.name.lexeme.equals("Override"));

                if (!hasOverride) {
                    errors.add(new SemanticError(method.name,
                            "Decorador Ausente: O método '" + method.name.lexeme + "()' na classe anónima exige @Override."));
                }
                else {
                    boolean existsInBase = false;
                    for (Stmt.Function baseMethod : baseMethods) {
                        if (baseMethod.name.lexeme.equals(method.name.lexeme)) {
                            existsInBase = true;
                            break;
                        }
                    }

                    if (!existsInBase) {
                        errors.add(new SemanticError(method.name,
                                "Erro de Sobreposição: O método '" + method.name.lexeme + "()' na classe anónima tem @Override mas não pertence à base."));
                    }
                }

                execute(method);
            }

            endScope();
        }

        return className;
    }

    @Override
    public String visitArrowFunctionExpr(Expr.ArrowFunction expr) {
        beginScope();
        for (Stmt.Param p : expr.parameters) {
            String pType = (p.typeNode != null) ? stringifyTypeNode(resolveAlias(p.typeNode)) : "any";
            currentScope.define(p.name, pType, true, true);
        }
        // Avalia o corpo (que pode ser uma expressão ou bloco)
        evaluate(expr.body);
        endScope();
        return "function";
    }

    @Override
    public String visitObjectLiteralExpr(Expr.ObjectLiteral expr) {
        for (Expr v : expr.values) {
            evaluate(v);
        }
        return "object";
    }

    @Override
    public String visitArrayExpr(Expr.ArrayLiteral expr) {
        for (Expr e : expr.elements) {
            evaluate(e);
        }
        return "array";
    }

    @Override
    public String visitIfExpr(Expr.If expr) {
        evaluate(expr.condition);
        // Os ramos são Stmt, executamos
        execute(expr.thenBranch);
        if (expr.elseBranch != null) execute(expr.elseBranch);
        return "any";
    }

    @Override
    public String visitSwitchExpr(Expr.Switch expr) {
        evaluate(expr.target);
        for (Expr.SwitchCase sc : expr.cases) {
            for (Expr v : sc.values) evaluate(v);
            execute(sc.body);
        }
        if (expr.defaultBranch != null) execute(expr.defaultBranch);
        return "any";
    }

    @Override
    public String visitMatchExpr(Expr.Match expr) {
        evaluate(expr.target);
        for (Expr.MatchArm arm : expr.arms) {
            if (arm.typeTest != null) {
                String typeName = stringifyTypeNode(resolveAlias(arm.typeTest));
                if (!typeName.equals("any") && !currentScope.isTypeDefined(typeName)) {
                    errors.add(new SemanticError(arm.typeTest.name, "Tipo desconhecido no match: '" + typeName + "'"));
                }
            }
            if (arm.valueTest != null) evaluate(arm.valueTest);
            if (arm.guard != null) evaluate(arm.guard);
            execute(arm.body);
        }
        if (expr.defaultBranch != null) execute(expr.defaultBranch);
        return "any";
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        evaluate(expr.left);
        evaluate(expr.right);
        return "bool";
    }

    @Override
    public String visitTernaryExpr(Expr.Ternary expr) {
        evaluate(expr.condition);
        evaluate(expr.trueBranch);
        evaluate(expr.falseBranch);
        return "any";
    }

    @Override
    public String visitGetExpr(Expr.Get expr) {
        String objectType = evaluate(expr.object);

        if (currentScope.isClass(objectType)) {
            String currentType = objectType;

            // =================================================================
            // ⭐ O ELEVADOR DA HERANÇA: Sobe até ao ObjectBase!
            // =================================================================
            while (currentType != null) {
                SemanticScope.XPLModelInfo info = currentScope.getClassInfo(currentType);
                if (info == null) break;

                // O Invasor (Onde estamos a programar agora?)
                String currentContext = currentClassStack.isEmpty() ? null : currentClassStack.peek().name;

                // Validação de Acesso Relativa
                boolean isInside = currentContext != null && currentContext.equals(currentType);
                boolean isSubclass = currentContext != null && currentScope.isSubclass(currentContext, currentType);

                // 1. Verifica Propriedades (Fields)
                for (Stmt.FieldDecl f : info.fields) {
                    if (f.name.lexeme.equals(expr.name.lexeme)) {
                        if (f.modifier != null) {
                            if (f.modifier.type == TokenType.PRIVATE && !isInside) {
                                errors.add(new SemanticError(expr.name, "A propriedade '" + expr.name.lexeme + "' é PRIVADA na classe '" + currentType + "'."));
                            } else if (f.modifier.type == TokenType.PROTECTED && !isInside && !isSubclass) {
                                errors.add(new SemanticError(expr.name, "A propriedade '" + expr.name.lexeme + "' é PROTEGIDA na classe '" + currentType + "'."));
                            }
                        }
                        return stringifyTypeNode(resolveAlias(f.type));
                    }
                }

                // 2. Verifica Métodos (Functions)
                for (Stmt.Function m : info.methods) {
                    if (m.name.lexeme.equals(expr.name.lexeme)) {
                        if (m.accessModifier != null) {
                            if (m.accessModifier.type == TokenType.PRIVATE && !isInside) {
                                errors.add(new SemanticError(expr.name, "O método '" + expr.name.lexeme + "()' é PRIVADO na classe '" + currentType + "'."));
                            } else if (m.accessModifier.type == TokenType.PROTECTED && !isInside && !isSubclass) {
                                errors.add(new SemanticError(expr.name, "O método '" + expr.name.lexeme + "()' é PROTEGIDO na classe '" + currentType + "'."));
                            }
                        }
                        return "function"; // Encontrou e validou acesso!
                    }
                }

                // Sobe para a classe pai (ex: De 'Mamifero' para 'Animal')
                currentType = info.superclass;
            }
        }
        return "any";
    }

    @Override
    public String visitSetExpr(Expr.Set expr) {
        String objectType = evaluate(expr.object);
        String valueType = evaluate(expr.value);

        if (currentScope.isClass(objectType)) {
            String currentType = objectType;
            boolean found = false;

            // ⭐ ELEVADOR DA HERANÇA NA ATRIBUIÇÃO (SET)
            while (currentType != null && !found) {
                SemanticScope.XPLModelInfo info = currentScope.getClassInfo(currentType);
                if (info == null) break;

                String currentContext = currentClassStack.isEmpty() ? null : currentClassStack.peek().name;
                boolean isInside = currentContext != null && currentContext.equals(currentType);
                boolean isSubclass = currentContext != null && currentScope.isSubclass(currentContext, currentType);

                for (Stmt.FieldDecl f : info.fields) {
                    if (f.name.lexeme.equals(expr.name.lexeme)) {
                        found = true;

                        // 1. A Polícia de Acesso
                        if (f.modifier != null) {
                            if (f.modifier.type == TokenType.PRIVATE && !isInside) {
                                errors.add(new SemanticError(expr.name, "A propriedade '" + expr.name.lexeme + "' é PRIVADA na classe '" + currentType + "'."));
                            } else if (f.modifier.type == TokenType.PROTECTED && !isInside && !isSubclass) {
                                errors.add(new SemanticError(expr.name, "A propriedade '" + expr.name.lexeme + "' é PROTEGIDA na classe '" + currentType + "'."));
                            }
                        }

                        // 2. A Polícia de Mutabilidade (Constantes)
                        if (f.isReadonly || f.isFinal) {
                            errors.add(new SemanticError(expr.name, "Propriedade '" + expr.name.lexeme + "' é readonly/final e não pode ser modificada."));
                        } else {
                            // 3. A Polícia de Tipos
                            String expectedFieldType = stringifyTypeNode(resolveAlias(f.type));
                            if (!isTypeCompatible(expectedFieldType, valueType)) {
                                errors.add(new SemanticError(expr.name, "Erro de Tipo: A propriedade '" + expr.name.lexeme + "' exige o tipo '" + expectedFieldType + "', mas tentou-se atribuir '" + valueType + "'."));
                            }
                        }
                        break;
                    }
                }

                currentType = info.superclass; // Sobe para o pai
            }
        }
        return valueType;
    }

    @Override
    public String visitIndexAccessExpr(Expr.IndexAccess expr) {
        String objType = evaluate(expr.object);
        String indexType = evaluate(expr.index);

        // 1. Valida o Alvo (Quem está a ser acedido?)
        if (!objType.equals("any") && !objType.equals("array") && !objType.equals("string") && !objType.equals("object")) {
            errors.add(new SemanticError(expr.bracket, "Apenas arrays, strings e objetos suportam acesso por índice. (Tipo recebido: " + objType + ")"));
        }

        // 2. Valida o Índice com base no tipo do alvo!
        switch (objType) {
            case "object" -> {
                if (!indexType.equals("any") && !indexType.equals("string")) {
                    errors.add(new SemanticError(expr.bracket, "O índice para aceder a um objeto/dicionário tem de ser uma string."));
                }
            }
            case "array", "string" -> {
                if (!indexType.equals("any") && !indexType.equals("int")) {
                    errors.add(new SemanticError(expr.bracket, "O índice para arrays ou strings tem de ser numérico."));
                }
            }
            case "any" -> {
                // Se o alvo for dinâmico ("any"), somos tolerantes: aceitamos números OU strings
                if (!indexType.equals("any") && !indexType.equals("int") && !indexType.equals("string")) {
                    errors.add(new SemanticError(expr.bracket, "Índice inválido. Usa um número (para arrays) ou uma string (para objetos)."));
                }
            }
        }

        // O resultado de extrair algo de uma lista ou dicionário é dinâmico (não sabemos o que está lá dentro estatisticamente)
        return "any";
    }

    @Override
    public String visitIndexAssignExpr(Expr.IndexAssign expr) {
        String objType = evaluate(expr.object);
        String indexType = evaluate(expr.index);
        String valueType = evaluate(expr.value);

        if (!objType.equals("any") && !objType.equals("array") && !objType.equals("object")) {
            errors.add(new SemanticError(expr.bracket, "Apenas arrays e objetos suportam atribuição por índice."));
        }

        switch (objType) {
            case "object" -> {
                if (!indexType.equals("any") && !indexType.equals("string")) {
                    errors.add(new SemanticError(expr.bracket, "O índice para atribuição num objeto tem de ser uma string."));
                }
            }
            case "array" -> {
                if (!indexType.equals("any") && !indexType.equals("int")) {
                    errors.add(new SemanticError(expr.bracket, "O índice para atribuição num array tem de ser numérico."));
                }
            }
            case "any" -> {
                if (!indexType.equals("any") && !indexType.equals("int") && !indexType.equals("string")) {
                    errors.add(new SemanticError(expr.bracket, "Índice inválido para atribuição."));
                }
            }
        }

        return valueType;
    }

    @Override
    public String visitNullCoalesceExpr(Expr.NullCoalesce expr) {
        String leftType = evaluate(expr.left);
        String rightType = evaluate(expr.right);

        // =====================================================================
        // ⭐ O DESEMPACOTADOR ESTÁTICO (O Segredo do '??') ⭐
        // Se a esquerda for "?int", o tipo base garantido é "int"
        // =====================================================================
        String baseLeft = leftType.startsWith("?") ? leftType.substring(1) : leftType;

        // A Polícia do Fallback: Garante que não fazes (?int ?? "texto")
        if (!baseLeft.equals("any") && !rightType.equals("any") && !baseLeft.equals("null") && !rightType.equals("null")) {
            // Chama o teu método de verificação de tipos!
            if (!isTypeCompatible(baseLeft, rightType)) {
                errors.add(new SemanticError(expr.operator,
                        "Conflito no Operador '??': A variável (esquerda) é do tipo base '" + baseLeft + "', mas o valor de fallback (direita) fornecido é '" + rightType + "'."));
            }
        }

        // A Magia: O resultado de um '??' é sempre o tipo base sólido!
        if (baseLeft.equals("null") || baseLeft.equals("any")) {
            return rightType;
        }

        return baseLeft; // Devolve "int" puro, permitindo que a matemática continue!
    }

    @Override
    public String visitTypeCheckExpr(Expr.TypeCheck expr) {
        String leftType = evaluate(expr.left);
        // Verifica se o tipo à direita existe
        String rightTypeName = stringifyTypeNode(resolveAlias(expr.rightType));
        if (!rightTypeName.equals("any") && !currentScope.isTypeDefined(rightTypeName)) {
            errors.add(new SemanticError(expr.rightType.name, "Tipo desconhecido na verificação: '" + rightTypeName + "'"));
        }
        return "bool";
    }

    @Override
    public String visitTypeofExpr(Expr.Typeof expr) {
        evaluate(expr.expression);
        return "string";
    }

    @Override
    public String visitCastExpr(Expr.Cast expr) {
        String valueType = evaluate(expr.value);
        String targetType = stringifyTypeNode(resolveAlias(expr.type));

        if (!targetType.equals("any") && !currentScope.isTypeDefined(targetType)) {
            errors.add(new SemanticError(expr.type.name, "Tipo de cast desconhecido: '" + targetType + "'"));
        }

        // =================================================================
        // ⭐ A ESCOTILHA DE EMERGÊNCIA DO CAST FORÇADO (as!) ⭐
        // =================================================================
        // Se o programador forçou o cast (as!), devolvemos "any" para cegar o Linter.
        // Isto obriga o erro a rebentar apenas no Run-Time (dentro do try/catch).
        // (Nota: Ajusta 'expr.operator.lexeme' caso o teu token de operador tenha outro nome na AST)
        // ⭐ Se tens uma flag booleana na AST:
        if (expr.isForced) {
            return "any"; // Cega o Linter!
        }

        // Verifica se o cast é possível? Não fazemos, pois pode ser upcast ou downcast.
        // Se for um "as" normal, devolve o tipo estrito!
        return targetType;
    }

    @Override
    public String visitUnwrapExpr(Expr.Unwrap expr) {
        String type = evaluate(expr.expr);
        if (!type.startsWith("?")) {
            errors.add(new SemanticError(expr.operator, "Operador '!' só pode ser aplicado a tipos opcionais (?)."));
        }
        // Remove o '?' do tipo
        return type.length() > 1 ? type.substring(1) : "any";
    }

    @Override
    public String visitOptionalChainingExpr(Expr.OptionalChaining expr) {
        evaluate(expr.object);
        return "any";
    }

    @Override
    public String visitOptionalCallExpr(Expr.OptionalCall expr) {
        evaluate(expr.object);
        for (Expr.CallArg arg : expr.arguments) evaluate(arg.expression);
        return "any";
    }

    @Override
    public String visitMetaAccessExpr(Expr.MetaAccess expr) {
        evaluate(expr.object);
        // Metadados sempre retornam any (ou object)
        return "any";
    }

    @Override
    public String visitSuperExpr(Expr.Super expr) {
        // Verifica se estamos dentro de uma classe
        if (currentClassStack.isEmpty()) {
            errors.add(new SemanticError(expr.keyword, "'super' só pode ser usado dentro de uma classe."));
        }
        return "any";
    }

    @Override
    public String visitSpreadExpr(Expr.Spread expr) {
        // Não é avaliado diretamente, mas o operando é
        evaluate(expr.expression);
        return "any";
    }

    @Override
    public String visitBlockExpr(Expr.Block expr) {
        beginScope();
        for (Stmt s : expr.statements) {
            execute(s);
        }
        endScope();
        return "any";
    }
}