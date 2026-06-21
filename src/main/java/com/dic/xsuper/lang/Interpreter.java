package com.dic.xsuper.lang;

import com.dic.xsuper.core.CommandRegistry;
import com.dic.xsuper.lang.helpers.ArrayMethods;
import com.dic.xsuper.lang.helpers.ObjectMethods;
import com.dic.xsuper.lang.helpers.StringMethods;
import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.poo.XplClass;
import com.dic.xsuper.lang.poo.XplInstance;
import com.dic.xsuper.lang.poo.XplInterface;
import com.dic.xsuper.utils.ConsoleTheme;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    // O escopo global que criámos na Fase 1
    // 1. Cria a caixa global
    public final Environment globals = new Environment();
    public final CommandRegistry registry;
    public Path currentDirectory;
    // 2. O ambiente atual aponta para a caixa global logo no início!
    private Environment environment = globals;
    // ⭐ O NOSSO REGISTRY GLOBAL ⭐
    // Guarda tanto os modelos-base (Declare) quanto as variantes (Implement as)
    private final Map<String, XPLModel> registry_model = new HashMap<>();
    private final Map<String, XplInterface> registry_Interfaces = new HashMap<>();

    // O Armazém Global de prismas semânticos (NomeDoAlias -> TipoReal):
    private final java.util.Map<String, TypeNode> typeAliases = new java.util.HashMap<>();


    public Interpreter(CommandRegistry registry, Path currentDirectory) {

        this.registry = registry;
        this.currentDirectory = currentDirectory;

        // Função Nativa: println
        globals.defineConst("println", new XplCallable() {
            @Override
            public int arity() {
                return -1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                if (arguments.isEmpty() || arguments.size() > 2)
                    throw new RuntimeException("println espera 1 ou 2 argumentos.");
                String text = stringify(arguments.get(0));
                if (arguments.size() == 2) {
                    System.out.println(hexToAnsi(stringify(arguments.get(1))) + text + ConsoleTheme.RESET);
                } else {
                    System.out.println(ConsoleTheme.TEXT + text + ConsoleTheme.RESET);
                }
                return null;
            }
        });

        // Função Nativa: print
        globals.defineConst("print", new XplCallable() {
            @Override
            public int arity() {
                return -1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                if (arguments.isEmpty() || arguments.size() > 2)
                    throw new RuntimeException("print espera 1 ou 2 argumentos.");
                String text = stringify(arguments.get(0));
                if (arguments.size() == 2) {
                    System.out.print(hexToAnsi(stringify(arguments.get(1))) + text + ConsoleTheme.RESET);
                } else {
                    System.out.print(ConsoleTheme.TEXT + text + ConsoleTheme.RESET);
                }
                System.out.flush();
                return null;
            }
        });

        // Função Nativa: shell
        globals.defineConst("shell", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                String commandStr = stringify(arguments.getFirst());
                PrintStream originalOut = System.out;
                ByteArrayOutputStream memoryStream = new ByteArrayOutputStream();
                try (PrintStream captureOut = new PrintStream(memoryStream, true, StandardCharsets.UTF_8)) {
                    System.setOut(captureOut);
                    interpreter.currentDirectory = interpreter.registry.executeCommand(commandStr, interpreter.currentDirectory);
                } catch (Exception e) {
                    return "Erro no shell: " + e.getMessage();
                } finally {
                    System.setOut(originalOut);
                }
                return memoryStream.toString(StandardCharsets.UTF_8).trim();
            }
        });

        // Define o construtor nativo do 'Map' no escopo global!
        globals.defineConst("Map", new XplCallable() {
            @Override public int arity() { return 0; } // Construtor vazio new Map()
            @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                // Devolve um HashMap novo e vazio!
                return new java.util.LinkedHashMap<String, Object>();
            }
            @Override public String toString() { return "<native class Map>"; }
        });

        errorInject();
    }

    private void errorInject() {
        // =========================================================================
        // ⭐ O GÉNESIS DA CLASSE 'Error' NATÍVA (Com Modificador de Visibilidade) ⭐
        // =========================================================================
        XPLModel baseErrorModel = new XPLModel("Error", null);
        baseErrorModel.hasBaseImplementation = true;

        // ⭐ A ARMA DESARMADA: Fabricamos um Token de visibilidade 'pub' legítimo!
        // (Nota: Se no teu TokenType o modificador público se chamar PUBLIC em vez de PUB, altera abaixo)
        Token pubToken = new Token(TokenType.PUB, "pub", null, 0, 0);

        Token msgToken = new Token(TokenType.IDENTIFIER, "message", null, 0, 0);

        // Injetamos o 'pubToken' no 1º argumento em vez de 'null'!
        baseErrorModel.addField(new Stmt.FieldDecl(
                pubToken,
                false, false, false,
                msgToken,
                new TypeNode.Simple(new Token(TokenType.T_STRING, "string", null, 0, 0))
        ));

        this.registry_model.put("Error", baseErrorModel);
        this.environment.defineConst("Error", new XplClass(baseErrorModel, this.environment));
    }

    public void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (ControlFlow.RuntimeError error) {
            System.err.println(ConsoleTheme.ERROR + "Erro de Execução (Linha " + error.token.line + "): " + error.getMessage() + ConsoleTheme.RESET);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }


    // ⭐ A POLÍCIA DE ENCAPSULAMENTO ⭐
    private void checkAccess(Token name, Stmt.FieldDecl field, XPLModel targetModel, boolean isWriting) {

        // 1. Regra do FINAL: Ninguém escreve num Final! (O valor só nasce pelo bloco 'default')
        if (isWriting && field.isFinal) {
            throw new ControlFlow.RuntimeError(name, "Erro de Segurança: A propriedade '" + name.lexeme + "' é FINAL e não pode ser alterada.");
        }

        // Descobre em que classe estamos a rodar AGORA (quem é o invasor?)
        XPLModel currentModel = null;
        try {
            currentModel = (XPLModel) environment.get("__current_model");
        } catch (Exception e) {} // Se der erro, estamos no espaço global (script)

        boolean isInsideClass = (currentModel != null && currentModel.name.equals(targetModel.name));
        boolean isSubclass = (currentModel != null && currentModel.isSubclassOf(targetModel.name));

        // 2. Regra do READONLY / DYN: Leitura pública, Escrita privada!
        if (isWriting && field.isReadonly && !isInsideClass) {
            throw new ControlFlow.RuntimeError(name, "Erro de Acesso: A propriedade '" + name.lexeme + "' é READONLY. Só pode ser alterada dentro da própria classe.");
        }

        // 3. Regra do PRIV (Privado): Só a própria classe lê e escreve!
        if (field.modifier.type == TokenType.PRIV && !isInsideClass) {
            throw new ControlFlow.RuntimeError(name, "Erro de Acesso: A propriedade '" + name.lexeme + "' é PRIVADA. Só a classe '" + targetModel.name + "' pode aceder.");
        }

        // 4. Regra do PROT (Protegido): Só a classe e os filhos (herança) acedem!
        if (field.modifier.type == TokenType.PROT && !isSubclass) {
            throw new ControlFlow.RuntimeError(name, "Erro de Acesso: A propriedade '" + name.lexeme + "' é PROTEGIDA. Só acessível por herança.");
        }
    }



    // ==========================================
    // EXECUÇÃO DE DECLARAÇÕES (STATEMENTS)
    // ==========================================

    @Override
    public Void visitExpressionStmt(Stmt.ExpressionStmt stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitVarDeclStmt(Stmt.VarDecl stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        // Verificação Básica de Tipos (se a anotação :long, :string, etc. foi usada)
        if (stmt.typeAnnotation != null && value != null) {
            checkTypeCompatability(stmt.typeAnnotation.name, value);
        }

        // Delega para o teu Environment aplicar as regras restritas!
        String name = stmt.name.lexeme;
        switch (stmt.keyword.type) {
            case VAR:
                environment.defineVar(name, value);
                break;
            case LET:
                environment.defineLet(name, value);
                break;
            case CONST:
                environment.defineConst(name, value);
                break;
        }
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(this.environment));
        return null;
    }

    public void executeBlock(List<Stmt> statements, Environment blockEnv) {
        Environment previous = this.environment;
        try {
            this.environment = blockEnv; // Entra no novo escopo
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous; // Restaura o escopo pai ao sair do bloco
        }
    }


    @Override
    public Void visitForInStmt(Stmt.ForIn stmt) {
        Object iterable = evaluate(stmt.iterable);

        if (!(iterable instanceof Iterable)) {
            throw new ControlFlow.RuntimeError(stmt.loopVariable, "O alvo do 'for-in' precisa ser um Array ou Lista iterável.");
        }

        // O 'for-in' cria um mini-escopo só para a variável de interação
        for (Object element : (Iterable<?>) iterable) {
            Environment loopEnv = new Environment(this.environment);
            loopEnv.defineLet(stmt.loopVariable.lexeme, element);

            Environment previous = this.environment;
            try {
                this.environment = loopEnv;
                execute(stmt.body);
            } catch (ControlFlow.BreakException e) {
                this.environment = previous;
                break; // Sai do loop!
            } catch (ControlFlow.ContinueException e) {
                this.environment = previous;
                // Continua para o próximo ciclo
            } finally {
                this.environment = previous;
            }
        }
        return null;
    }

    @Override
    public Void visitEnumStmt(Stmt.Enum stmt) {
        // 1. Imprime no terminal para garantirmos que o Parser leu o Enum e o enviou para aqui
        System.out.println("[XPL Engine] -> Compilando Enum: " + stmt.name.lexeme);

        // 2. Cria o Map do Enum mantendo a ordem (LinkedHashMap)
        java.util.Map<String, Object> enumMap = new java.util.LinkedHashMap<>();

        for (Token constant : stmt.constants) {
            enumMap.put(constant.lexeme, constant.lexeme);
        }

        // ⭐ 3. A INTEGRAÇÃO PERFEITA COM O TEU ENVIRONMENT ⭐
        // Guardamos como CONSTANTE para que a tua linguagem o proteja de reatribuições!
        environment.defineConst(stmt.name.lexeme, enumMap);

        return null;
    }

    @Override
    public Void visitDeclareDeclStmt(Stmt.DeclareDecl stmt) {
        String modelName = stmt.name.lexeme;


        System.out.println("[XPL Engine] -> Compilando Modelo de Dados (Declare): " + modelName);

        // 1. Resolve a herança (Extends)
        XPLModel superclass = null;
        if (stmt.superclass != null) {
            superclass = registry_model.get(stmt.superclass.lexeme);
            if (superclass == null) {
                throw new ControlFlow.RuntimeError(stmt.superclass,
                        "Erro: O modelo pai '" + stmt.superclass.lexeme + "' não foi encontrado ou declarado antes de " + modelName + ".");
            }
        }

        // 2. Cria o Molde (Blueprint) Base
        XPLModel model = new XPLModel(modelName, superclass);
        model.canBeInstantiated = false; // Declare puro NÃO nasce.

        // Se este modelo tem um pai, ele herda IMEDIATAMENTE todos os campos do pai!
        if (superclass != null) {
            model.fields.putAll(superclass.fields);
        }

        // 3. Injeta as propriedades (Campos de Dados)
        for (Stmt.FieldDecl field : stmt.fields) {
            model.addField(field);
        }

        // 4. Arquiva o Molde no teu Registry de POO!
        // Sem isto, o 'implement' nunca conseguiria fundir os métodos.
        registry_model.put(modelName, model);

        return null;
    }

    @Override
    public Void visitInterfaceDeclStmt(Stmt.InterfaceDecl stmt) {
        String interfaceName = stmt.name.lexeme;
        System.out.println("[XPL Engine] -> Registando Interface: " + interfaceName);


        // 1. Converte a Declaração da AST num Contrato em Memória
        XplInterface contract = new XplInterface(stmt);

        // 2. Guarda no Arquivo de Contratos
        registry_Interfaces.put(interfaceName, contract);

        return null;
    }


    @Override
    public Void visitImplementDeclStmt(Stmt.ImplementDecl stmt) {





        String baseName = stmt.targetName.lexeme;

        // 1. Vai buscar o modelo base (Declare) ao teu NOVO map!
        XPLModel baseModel = registry_model.get(baseName);
        if (baseModel == null) {
            throw new ControlFlow.RuntimeError(stmt.targetName, "Erro Fatal: O modelo base '" + baseName + "' não foi declarado.");
        }

        XPLModel activeModel; // O modelo que vamos validar, registar e instanciar

        // ⭐ 2. A BIFURCAÇÃO (BASE vs VARIANTE) ⭐
        if (stmt.aliasName == null) {
            // ---> É UMA IMPLEMENTAÇÃO DE BASE! <---
            // Modificamos o próprio baseModel diretamente para NÃO perder as flags!
            baseModel.hasBaseImplementation = true;

            // Injeta os métodos diretamente no ADN do modelo base
            for (Stmt.Function method : stmt.methods) {
                baseModel.addMethod(method);
            }
            if (stmt.isAbstract) {
                baseModel.isAbstract = true; // Carimba o modelo na RAM como Abstrato!
            }
            activeModel = baseModel;
            System.out.println("[XPL Engine] -> Injetando Comportamento (Base): " + activeModel.name);

        }
        else {
            // ---> É UMA VARIANTE! (Ex: implement Mamifero as Mam1) <---
            String variantName = stmt.aliasName.lexeme;

            // Criamos uma ramificação limpa
            activeModel = new XPLModel(variantName, baseModel.superclass);

            // Uma variante É uma implementação base de si mesma!
            activeModel.hasBaseImplementation = true;

            // ⭐ A PEÇA QUE FALTAVA: A Variante também pode ser abstrata! ⭐
            if (stmt.isAbstract) {
                activeModel.isAbstract = true;
            }

            // Copia a memória (fields) do modelo base para a variante
            activeModel.fields.putAll(baseModel.fields);

            // Injeta os métodos específicos da variante
            for (Stmt.Function method : stmt.methods) {
                activeModel.addMethod(method);
            }

            // Regista a nova variante no Registry Interno, sem apagar a Base!
            registry_model.put(variantName, activeModel);

            // Log da injeção
            System.out.println("[XPL Engine] -> Injetando Comportamento (Variante): " + variantName + " (Base: " + baseName + ")");
        }


        // ⭐ AVALIA O BLOCO DEFAULT E DIVIDE AS ÁGUAS ⭐
        for (Map.Entry<String, Expr> entry : stmt.defaultState.entrySet()) {
            String fieldName = entry.getKey();
            Object value = evaluate(entry.getValue()); // Calcula o valor 1 única vez!

            Stmt.FieldDecl field = activeModel.fields.get(fieldName);
            if (field == null) {
                throw new ControlFlow.RuntimeError(stmt.targetName, "O campo '" + fieldName + "' não existe no 'declare " + activeModel.name + "'.");
            }

            if (field.isStatic) {
                activeModel.staticFields.put(fieldName, value); // Vai para a memória estática global
            } else {
                activeModel.defaultInstanceFields.put(fieldName, value); // Fica de reserva para o próximo 'new'
            }
        }


        // ⭐ 3. A GUILHOTINA: VALIDAÇÃO DE CONTRATOS (TYPE CHECKING) ⭐
        for (Token interfaceToken : stmt.interfaces) {
            String interfaceName = interfaceToken.lexeme;
            XplInterface contract = registry_Interfaces.get(interfaceName);

            if (contract == null) {
                throw new ControlFlow.RuntimeError(interfaceToken, "Erro de Linkage: A interface '" + interfaceName + "' não foi encontrada no Registry.");
            }

            // O motor cruza a lista do contrato com os métodos do activeModel (Usando Busca Genética!)
            for (String requiredMethod : contract.requiredMethods.keySet()) {
                // Evolução: Em vez de usar apenas containsKey, usa o findMethod para suportar contratos cumpridos por herança!
                if (activeModel.findMethod(requiredMethod) == null) {
                    throw new ControlFlow.RuntimeError(stmt.targetName,
                            "Quebra de Contrato Fatal: O modelo '" + activeModel.name + "' não implementou o método obrigatório '" + requiredMethod + "()' exigido pela interface '" + interfaceName + "'.");
                }
            }
        }

        // 4. Instancia a classe para a memória RAM (O Global Environment)
        XplClass executableClass = new XplClass(activeModel, this.globals);
        globals.defineConst(activeModel.name, executableClass);

        return null;
    }

    @Override
    public Void visitThrowStmt(Stmt.Throw stmt) {
        Object value = evaluate(stmt.value);
        // Dispara a exceção invisível no motor Java!
        throw new ControlFlow.ThrowException(value);
    }

    @Override
    public Void visitTypeAliasDecl(Stmt.TypeAliasDecl stmt) {
        String aliasName = stmt.name.lexeme;

        // Regra 1: Não pode ter o mesmo nome de um tipo ou alias já existente.
        if (typeAliases.containsKey(aliasName) || registry_model.containsKey(aliasName)) {
            throw new ControlFlow.RuntimeError(stmt.name,
                    "Erro de Semântica: O identificador '" + aliasName + "' já designa um tipo existente.");
        }

        // Regra 2: Proibição estrita de referências circulares (ex: type A = B; type B = A;).
        if (detectCircularAlias(aliasName, stmt.targetType)) {
            throw new ControlFlow.RuntimeError(stmt.name,
                    "Referência Circular Proibida: O alias '" + aliasName + "' aponta para si mesmo num ciclo infinito.");
        }

        typeAliases.put(aliasName, stmt.targetType);
        return null;
    }

    // Detetor proativo de buracos negros (Ciclos infinitos):
    private boolean detectCircularAlias(String originName, TypeNode target) {
        if (target instanceof TypeNode.Simple) {
            String targetName = ((TypeNode.Simple) target).name.lexeme;
            if (targetName.equals(originName)) return true;

            if (typeAliases.containsKey(targetName)) {
                return detectCircularAlias(originName, typeAliases.get(targetName));
            }
        } else if (target instanceof TypeNode.Optional) {
            return detectCircularAlias(originName, ((TypeNode.Optional) target).innerType);
        }
        return false;
    }

    // ⭐ 1. VALIDADOR DE TIPOS DO CATCH ⭐
    private boolean isTypeMatch(Object value, TypeNode typeAnnotation) {
        TokenType expectedType = typeAnnotation.name.type;

        if (expectedType == TokenType.T_STRING && value instanceof String) return true;
        if (expectedType == TokenType.T_INT && value instanceof Long) return true;
        if (expectedType == TokenType.T_FLOAT && (value instanceof Double || value instanceof Long)) return true;
        if (expectedType == TokenType.T_ARRAY && value instanceof List) return true;
        if (expectedType == TokenType.T_OBJECT && value instanceof Map) return true;

        // É um Objeto Orientado a Dados (POO)?
        if (expectedType == TokenType.IDENTIFIER && value instanceof XplInstance instance) {
            // Usa o rastreador genético para aceitar filhos num catch de pais!
            return instance.klass.model.isSubclassOf(typeAnnotation.name.lexeme);
        }

        return false;
    }

    // ⭐ 2. O ROTEADOR PRINCIPAL ⭐
    @Override
    public Void visitTryStmt(Stmt.Try stmt) {
        try {
            execute(stmt.tryBlock);

        } catch (ControlFlow.ThrowException e) {
            handleCatch(stmt, e.value, e); // Rota A: Throw explícito do utilizador

        } catch (ControlFlow.RuntimeError e) {
            // Rota B: O Hipervisor delega a fabricação do objeto para a linha de montagem
            Object errorValue = resolveRuntimeErrorObject(e);
            handleCatch(stmt, errorValue, e);

        } finally {
            if (stmt.finallyBlock != null) execute(stmt.finallyBlock);
        }
        return null;
    }

    // ⭐ FÁBRICA DE TRANSMUTAÇÃO (Auxiliar privada do Roteador 2) ⭐
    private Object resolveRuntimeErrorObject(ControlFlow.RuntimeError e) {
        if (!registry_model.containsKey("Error")) return e.getMessage();

        XPLModel errModel = registry_model.get("Error");
        if (!errModel.hasBaseImplementation) return e.getMessage();

        XplClass errClass = new XplClass(errModel, globals);
        XplInstance errInst = new XplInstance(errClass);

        // Heurística flexível de injeção de texto (messa / mensa)
        for (String field : errModel.fields.keySet()) {
            String fLower = field.toLowerCase();
            if (fLower.contains("mensa") || fLower.contains("messa")) {
                Token fieldToken = new Token(TokenType.IDENTIFIER, field, null, -1, -1);
                errInst.set(fieldToken, e.getMessage());
                break;
            }
        }
        return errInst; // Transmutado em POO nativa com sucesso!
    }


    // ⭐ 3. O ROTEADOR SEQUENCIAL ⭐
    private void handleCatch(Stmt.Try stmt, Object errorValue, RuntimeException originalException) {

        for (Stmt.CatchClause clause : stmt.catchClauses) {

            if (isTypeMatch(errorValue, clause.type)) {

                Environment catchEnv = new Environment(this.environment);
                catchEnv.defineLet(clause.name.lexeme, errorValue);

                Environment previous = this.environment;
                try {
                    this.environment = catchEnv;
                    execute(clause.body);
                    return; // ⭐ O erro foi capturado e tratado. Corta a função instantaneamente!

                } finally {
                    this.environment = previous;
                }
            }
        }

        // Se o loop rodou até ao fim sem disparar o 'return', nenhum catch serviu. Explode!
        throw originalException;
    }
    @Override
    public Void visitForCStyleStmt(Stmt.ForCStyle stmt) {
        // 1. Criamos uma "Jaula" (Escopo) só para o loop.
        // Assim, o 'let i = 1' não vaza para fora do for!
        Environment previous = this.environment;

        try {
            this.environment = new Environment(previous);

            // 2. Inicialização (ex: let i:int = 1;)
            if (stmt.init != null) {
                execute(stmt.init);
            }

            // 3. A Roda do Loop
            while (true) {
                // Avalia a condição (ex: i <= 12)
                if (stmt.condition != null) {
                    if (!isTruthy(evaluate(stmt.condition))) {
                        break; // A condição deu falso? Sai do loop!
                    }
                }

                // Executa o corpo do loop (ex: println(i);)
                try {
                    execute(stmt.body);
                } catch (ControlFlow.BreakException e) {
                    break; // Sai do loop imediatamente
                } catch (ControlFlow.ContinueException e) {
                    // O continue salta o resto do corpo, mas VAI para o incremento!
                }

                // 4. Incremento (ex: i++)
                if (stmt.increment != null) {
                    evaluate(stmt.increment);
                }
            }
        } finally {
            // ⭐ CRÍTICO: Restaura a memória original para apagar a variável 'i'
            this.environment = previous;
        }

        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new ControlFlow.BreakException();
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        throw new ControlFlow.ContinueException();
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        // Transforma a declaração da AST num Objeto Executável XplFunction
        XplFunction function = new XplFunction(stmt, this.environment,null);

        // Guarda a função na memória (no escopo atual)
        environment.defineLet(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) {
            value = evaluate(stmt.value);
        }
        // Dispara o valor de volta para o chamador através da nossa exceção leve
        throw new ControlFlow.ReturnException(value);
    }


    @Override
    public Void visitForInRangeStmt(Stmt.ForInRange stmt) {
        // 1. Avalia as expressões matemáticas para descobrir os valores
        double startVal = toDouble(evaluate(stmt.start));
        double endVal = toDouble(evaluate(stmt.end));

        // Se o utilizador não passou o jump, o padrão é 1.
        double jumpVal = 1.0;

        if (stmt.jump.isPresent()) {
            // 1. Se o utilizador forneceu um jump explicitamente, usamos sempre o dele!
            jumpVal = toDouble(evaluate(stmt.jump.get()));

        } else if (startVal > endVal) {
            // ⭐ 2. A MAGIA CORRIGIDA:
            // Se o utilizador NÃO forneceu o jump, e o início for maior que o fim, invertemos automaticamente para -1!
            jumpVal = -1.0;
        }

        // Validação de segurança crítica
        if (jumpVal == 0) {
            throw new ControlFlow.RuntimeError(stmt.loopVariable, "Erro de Loop: O incremento (jump) não pode ser zero.");
        }

        // Descobre a direção do loop
        boolean isAscending = jumpVal > 0;
        double current = startVal;

        // O motor do Range Loop
        while ((isAscending && current <= endVal) || (!isAscending && current >= endVal)) {

            // Cria a "caixa" (escopo) só para esta volta do loop
            Environment loopEnv = new Environment(this.environment);

            // Um toque de classe: se o número for inteiro (ex: 5.0), guarda como Long (5) para ficar limpo.
            Object valueToStore;
            if (current == Math.floor(current)) {
                valueToStore = (long) current;
            } else {
                valueToStore = current;
            }

            // Injeta a variável (ex: 'a') na memória
            loopEnv.defineLet(stmt.loopVariable.lexeme, valueToStore);

            Environment previous = this.environment;
            try {
                this.environment = loopEnv;
                execute(stmt.body);

            } catch (ControlFlow.BreakException e) {
                this.environment = previous;
                break; // Sai do loop imediatamente!

            } catch (ControlFlow.ContinueException e) {
                this.environment = previous;
                // O continue interrompe a execução do bloco, mas deixamos o loop saltar para o 'finally' e continuar!

            } finally {
                this.environment = previous;
            }

            // Faz o salto para a próxima iteração
            current += jumpVal;
        }

        return null;
    }

    // ==========================================
    // AVALIAÇÃO DE EXPRESSÕES (EXPRESSIONS)
    // ==========================================

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitArrayExpr(Expr.ArrayLiteral expr) {
        List<Object> array = new ArrayList<>();
        for (Expr element : expr.elements) {
            array.add(evaluate(element));
        }
        return array;
    }

    @Override
    public Object visitCompoundAssignExpr(Expr.CompoundAssign expr) {
        // 1. Lê o valor atual onde quer que ele esteja (Variável, Array ou Objeto/Classe)
        Object currentValue = switch (expr.target) {
            case Expr.Variable variable -> environment.get(variable.name.lexeme);
            case Expr.Get get -> visitGetExpr(get);
            case Expr.IndexAccess indexAccess -> visitIndexAccessExpr(indexAccess);
            case null, default ->
                    throw new ControlFlow.RuntimeError(expr.operator, "Alvo de atribuição composta inválido.");
        };

        // 2. Calcula o valor da direita (ex: o '5' no += 5)
        Object rightValue = evaluate(expr.value);

        // 3. Faz as contas baseadas no operador
        Object newValue = null;
        switch (expr.operator.type) {
            case PLUS_ASSIGN:
                if (currentValue instanceof String || rightValue instanceof String) newValue = stringify(currentValue) + stringify(rightValue);
                else if (currentValue instanceof Double || rightValue instanceof Double) newValue = toDouble(currentValue) + toDouble(rightValue);
                else if (currentValue instanceof Long && rightValue instanceof Long) newValue = (long) currentValue + (long) rightValue;
                break;
            case MINUS_ASSIGN:
                if (currentValue instanceof Double || rightValue instanceof Double) newValue = toDouble(currentValue) - toDouble(rightValue);
                else newValue = (long) currentValue - (long) rightValue;
                break;
            case STAR_ASSIGN:
                if (currentValue instanceof Double || rightValue instanceof Double) newValue = toDouble(currentValue) * toDouble(rightValue);
                else newValue = (long) currentValue * (long) rightValue;
                break;
            case SLASH_ASSIGN:
                if (currentValue instanceof Double || rightValue instanceof Double) newValue = toDouble(currentValue) / toDouble(rightValue);
                else newValue = (long) currentValue / (long) rightValue;
                break;
            case MODULO_ASSIGN:
                if (currentValue instanceof Double || rightValue instanceof Double) newValue = toDouble(currentValue) % toDouble(rightValue);
                else newValue = (long) currentValue % (long) rightValue;
                break;
            case HASH_ASSIGN: // Divisão inteira
                newValue = (long) toDouble(currentValue) / (long) toDouble(rightValue);
                break;
        }

        if (newValue == null) throw new ControlFlow.RuntimeError(expr.operator, "Operação inválida para estes tipos de dados.");

        // 4. Guarda o novo valor no lugar correto (Memória local, Objeto ou Classe Estática!)
        if (expr.target instanceof Expr.Variable) {
            environment.assign(((Expr.Variable) expr.target).name.lexeme, newValue);
        } else if (expr.target instanceof Expr.Get getExpr) {
            Object obj = evaluate(getExpr.object);
            if (obj instanceof XplClass) { // ⭐ Injeta na memória ESTÁTICA
                ((XplClass) obj).model.staticFields.put(getExpr.name.lexeme, newValue);
            } else if (obj instanceof XplInstance) {
                ((XplInstance) obj).set(getExpr.name, newValue);
            }
        } else {
            Expr.IndexAccess idx = (Expr.IndexAccess) expr.target;// Usa o teu próprio IndexAssign para atualizar o array!
            visitIndexAssignExpr(new Expr.IndexAssign(idx.object, idx.bracket, idx.index, new Expr.Literal(newValue)));
        }

        return newValue;
    }

    @Override
    public Object visitUpdateExpr(Expr.Update expr) {
        // 1. Lê o valor atual onde quer que ele esteja
        Object currentValue = switch (expr.target) {
            case Expr.Variable variable -> environment.get(variable.name.lexeme);
            case Expr.Get get -> visitGetExpr(get);
            case Expr.IndexAccess indexAccess -> visitIndexAccessExpr(indexAccess);
            case null, default -> throw new ControlFlow.RuntimeError(expr.operator, "Alvo inválido.");
        };

        // 2. Incrementa o valor
        Object newValue = null;
        if (currentValue instanceof Double) {
            double val = (double) currentValue;
            newValue = (expr.operator.type == TokenType.PLUS_PLUS) ? val + 1.0 : val - 1.0;
        } else if (currentValue instanceof Long) {
            long val = (long) currentValue;
            newValue = (expr.operator.type == TokenType.PLUS_PLUS) ? val + 1L : val - 1L;
        } else {
            throw new ControlFlow.RuntimeError(expr.operator, "Só podes incrementar números.");
        }

        // 3. Guarda o valor de volta (Variável, Instância ou Classe Estática!)
        if (expr.target instanceof Expr.Variable) {
            environment.assign(((Expr.Variable) expr.target).name.lexeme, newValue);
        } else if (expr.target instanceof Expr.Get getExpr) {
            Object obj = evaluate(getExpr.object);
            if (obj instanceof XplClass) { // ⭐ Injeta na memória ESTÁTICA
                ((XplClass) obj).model.staticFields.put(getExpr.name.lexeme, newValue);
            } else if (obj instanceof XplInstance) {
                ((XplInstance) obj).set(getExpr.name, newValue);
            }
        } else {
            Expr.IndexAccess idx = (Expr.IndexAccess) expr.target;
            visitIndexAssignExpr(new Expr.IndexAssign(idx.object, idx.bracket, idx.index, new Expr.Literal(newValue)));
        }

        return expr.isPrefix ? newValue : currentValue;
    }

    @Override
    public Object visitIndexAccessExpr(Expr.IndexAccess expr) {
        // Avalia quem é o array (lista) e quem é o índice (ex: 0)
        Object object = evaluate(expr.object);
        Object index = evaluate(expr.index);

        if (object instanceof List<?> list) {
            if (index instanceof Long) {
                int idx = (int) (long) index; // Convertemos Long para Int porque as listas do Java pedem Int
                if (idx >= 0 && idx < list.size()) {
                    return list.get(idx);
                }
                throw new ControlFlow.RuntimeError(expr.bracket, "Índice fora dos limites do Array (Index out of bounds).");
            }
            throw new ControlFlow.RuntimeError(expr.bracket, "O índice do Array tem de ser um número inteiro.");
        }
        if (object instanceof Map<?, ?> map) {
            return map.get(index.toString());
        }


        throw new ControlFlow.RuntimeError(expr.bracket, "Apenas Arrays e Strings suportam acesso por índice.");
    }

    @Override
    public Object visitIndexAssignExpr(Expr.IndexAssign expr) {
        Object object = evaluate(expr.object);
        Object index = evaluate(expr.index);
        Object value = evaluate(expr.value);

        if (object instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) object;

            if (index instanceof Long) {
                int idx = (int) (long) index;
                if (idx >= 0 && idx < list.size()) {
                    list.set(idx, value);
                    return value;
                }
                throw new ControlFlow.RuntimeError(expr.bracket, "Índice fora dos limites do Array (Index out of bounds).");
            }
            throw new ControlFlow.RuntimeError(expr.bracket, "O índice do Array tem de ser um número inteiro.");
        }

        if (object instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) object;
            map.put(index.toString(), value);
            return value;
        }

        throw new ControlFlow.RuntimeError(expr.bracket, "Apenas Arrays suportam atribuição por índice.");
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        // 1. Descobre quem é o objeto à esquerda do ponto (ex: a variável ou a string literal)
        Object object = evaluate(expr.object);

        // 2. É uma Lista (Array)? Delega para ArrayMethods
        if (object instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) object;

                // 1. Tenta ver se é uma propriedade direta (ex: arr.length, arr.first)
                return switch (expr.name.lexeme) {
                    case "length", "isEmpty", "first", "last" -> ArrayMethods.getProperty(list, expr.name.lexeme);
                    default ->

                        // 2. Se não for propriedade, devolve o método para ser executado
                            ArrayMethods.getMethod(list, expr.name.lexeme);
                };

            } catch (RuntimeException e) {
                throw new ControlFlow.RuntimeError(expr.name, e.getMessage());
            }
        }

        // ⭐ 3. É uma String? Delega para StringMethods
        // É uma String?
        if (object instanceof String) {
            try {
                String str = (String) object;

                // ⭐ 1. INTERCETA AS PROPRIEDADES PRIMEIRO (Sem parêntesis) ⭐
                return switch (expr.name.lexeme) {
                    case "length", "size", "isEmpty", "empty" -> StringMethods.getProperty(str, expr.name.lexeme);
                    default ->

                        // 2. Se não for propriedade, devolve a função para o visitCallExpr executar
                            StringMethods.getMethod(str, expr.name.lexeme);
                };

            } catch (RuntimeException e) {
                throw new ControlFlow.RuntimeError(expr.name, e.getMessage());
            }
        }

        // É um Dicionário/Objeto?
        if (object instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) object;
            String propName = expr.name.lexeme;

            // 1. Propriedades especiais diretas
            if (propName.equals("length") || propName.equals("size") || propName.equals("isEmpty") || propName.equals("empty")) {
                return ObjectMethods.getProperty(map, propName);
            }

            // 2. Tenta encontrar um método nativo (ex: keys, flatten, pick)
            try {
                return ObjectMethods.getMethod(map, propName);
            } catch (RuntimeException eMethod) {
                // 3. Se não for um método nativo, é porque o utilizador quer ler uma chave (ex: obj.nome)
                try {
                    return ObjectMethods.getProperty(map, propName);
                } catch (RuntimeException eProperty) {
                    // Se também não for uma chave, aí sim, damos erro!
                    throw new ControlFlow.RuntimeError(expr.name, "A propriedade ou método '" + propName + "' não existe no objeto.");
                }
            }
        }

// ⭐ É uma instância da nossa POO? ⭐
        if (object instanceof XplInstance) {
            XplInstance instance = (XplInstance) object;


            // 🚀0. INJEÇÃO NATIVA: O Método toObject() 🚀
            if (expr.name.lexeme.equals("toObject")) {
                // Devolve uma função nativa anónima para ser executada ()
                return new XplCallable() { // Usa a tua interface de funções nativas!
                    @Override
                    public int arity() { return 0; }

                    @Override
                    public Object call(Interpreter interpreter, java.util.List<Object> args) {
                        // Copia todas as propriedades vivas e CONGELA-AS num mapa imutável!
                        java.util.Map<String, Object> snapshot = new java.util.HashMap<>(instance.fields);
                        return java.util.Collections.unmodifiableMap(snapshot);
                    }
                };
            }

            // 1. É uma variável/propriedade?
            if (instance.fields.containsKey(expr.name.lexeme)) {
                Stmt.FieldDecl field = instance.klass.model.fields.get(expr.name.lexeme);

                // 🛑 CHAMA A POLÍCIA ANTES DE LER! 🛑
                if (field != null) {
                    checkAccess(expr.name, field, instance.klass.model, false);
                }

                return instance.fields.get(expr.name.lexeme);
            }

            // 2. É um Comportamento/Método? (Delega para o método nativo que injeta o 'this')
            Stmt.Function method = instance.klass.model.findMethod(expr.name.lexeme);
            if (method != null) {
                return instance.get(expr.name);
            }

            throw new ControlFlow.RuntimeError(expr.name, "A propriedade ou método '" + expr.name.lexeme + "' não existe na instância.");
        }

        // ⭐ É uma Classe/Fábrica (Acesso Estático)? ⭐
        if (object instanceof XplClass) {
            XPLModel model = ((XplClass) object).model;

            // 1. É um Dado Estático?
            if (model.staticFields.containsKey(expr.name.lexeme)) {
                Stmt.FieldDecl field = model.fields.get(expr.name.lexeme);

                // 🛑 A Polícia protege as leituras estáticas!
                if (field != null) {
                    checkAccess(expr.name, field, model, false);
                }

                return model.staticFields.get(expr.name.lexeme);
            }

            // 2. É um Comportamento Estático?
            Stmt.Function method = model.findMethod(expr.name.lexeme);
            if (method != null && method.isStatic) {
                XPLModel owner = model.getOwnerOfMethod(expr.name.lexeme);
                // Não tem bind(this) porque o método estático não tem dono instanciado!
                return new XplFunction(method, ((XplClass) object).closure, owner);
            }

            throw new ControlFlow.RuntimeError(expr.name, "A propriedade ou método estático '" + expr.name.lexeme + "' não existe no modelo " + model.name + ".");
        }

        throw new ControlFlow.RuntimeError(expr.name, "Apenas Arrays, Objetos, Strings, Instâncias e Classes possuem propriedades/métodos.");
    }
    @Override
    public Object visitArrowFunctionExpr(Expr.ArrowFunction expr) {
        // Guarda o ambiente atual para que a Arrow Function se lembre das variáveis de fora (Closure!)
        Environment closure = this.environment;

        // Criamos uma função anónima na hora
        return new XplCallable() {
            @Override
            public int arity() {
                return 1; // Recebe exatamente 1 parâmetro (ex: o 'e')
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                // 1. Cria um mini-escopo para a função
                Environment arrowEnv = new Environment(closure);

                // 2. Injeta o valor do parâmetro lá para dentro
                arrowEnv.defineLet(expr.parameter.lexeme, arguments.getFirst());

                // 3. Executa o corpo da função e devolve o resultado!
                Environment previous = interpreter.environment; // Acede através da instância atual
                try {
                    // Forçamos o interpretador a usar o mini-escopo
                    // Usamos uma abordagem reflexiva ou alteramos temporariamente o escopo do interpretador
                    interpreter.executeBlock(new ArrayList<>(), arrowEnv); // Truque para mudar de escopo

                    // IMPORTANTE: Como é uma Expressão (Expr) e não um Bloco de Stmt,
                    // avaliamos a expressão diretamente com o escopo trocado temporariamente!
                    interpreter.environment = arrowEnv;
                    return interpreter.evaluate(expr.body);

                } finally {
                    interpreter.environment = previous; // Restaura sempre!
                }
            }

            @Override
            public String toString() { return "<arrow fn>"; }
        };
    }

    @Override
    public Object visitObjectLiteralExpr(Expr.ObjectLiteral expr) {
        // Usamos LinkedHashMap para manter a ordem de inserção das chaves
        Map<String, Object> map = new java.util.LinkedHashMap<>();

        for (int i = 0; i < expr.keys.size(); i++) {
            // Se a chave for um identificador (ex: nome), extraímos o lexeme, se for string extraímos o valor
            Object keyObj = evaluate(expr.keys.get(i));
            String key = (keyObj instanceof Token) ? ((Token) keyObj).lexeme : keyObj.toString();
            Object value = evaluate(expr.values.get(i));
            map.put(key, value);
        }
        return map;
    }

    @Override
    public Object visitNewExpr(Expr.New expr) {
        String modelName = expr.className.lexeme;
        XPLModel model = registry_model.get(modelName);

        // 1. Verifica se o modelo sequer existe
        if (model == null) {
            throw new ControlFlow.RuntimeError(expr.className,
                    "Erro: O modelo '" + modelName + "' não foi declarado.");
        }

        // 2. Só agora verificamos as regras de negócio (as "Guilhotinas")
        if (!model.hasBaseImplementation) {
            throw new ControlFlow.RuntimeError(expr.className,
                    "ERRO FATAL: O modelo '" + modelName + "' não possui uma implementação base.");
        }

        if (model.isAbstract) {
            throw new ControlFlow.RuntimeError(expr.className,
                    "ERRO FATAL: Operação Ilegal. O modelo '" + modelName + "' possui uma implementação abstrata e não pode ser instanciado diretamente.");
        }

        // ⭐ 3. A CORREÇÃO: Avaliar os argumentos como fazemos no call()! ⭐
        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument)); // Transforma Expr no valor real (String, Long, etc)
        }

        // Se passou pelas validações, instancia!
        XplClass klass = new XplClass(model, environment);

        // ⭐ 4. SUPER-BÓNUS: A Guilhotina do Construtor! ⭐
        // Aproveitamos e protegemos para que não deixem faltar argumentos no 'new'
        if (klass.arity() != -1 && arguments.size() != klass.arity()) {
            throw new ControlFlow.RuntimeError(expr.className,
                    "O construtor do modelo '" + modelName + "' espera " + klass.arity() + " argumentos, mas obteve " + arguments.size() + ".");
        }

        // Passamos a lista de argumentos perfeitamente processada!
        return klass.call(this, arguments);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        // 1. Avalia quem é o dono e o valor
        Object object = evaluate(expr.object);
        Object value = evaluate(expr.value);

        // ⭐ É Atribuição numa variável estática? (Ex: Animal.INSTANCIAS = 5) ⭐
        if (object instanceof XplClass) {
            XPLModel model = ((XplClass) object).model;

            if (model.fields.containsKey(expr.name.lexeme) && model.fields.get(expr.name.lexeme).isStatic) {
                Stmt.FieldDecl field = model.fields.get(expr.name.lexeme);

                // 🛑 A Polícia protege também as variáveis estáticas!
                checkAccess(expr.name, field, model, true);

                model.staticFields.put(expr.name.lexeme, value);
                return value;
            }
            throw new ControlFlow.RuntimeError(expr.name, "A propriedade estática '" + expr.name.lexeme + "' não existe ou não pode ser alterada no modelo " + model.name + ".");
        }

        // ⭐ É Atribuição num Mapa/Dicionário? (E proteção de Imutabilidade) ⭐
        if (object instanceof java.util.Map) {
            try {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) object;
                Object dictValue = evaluate(expr.value);
                map.put(expr.name.lexeme, dictValue);
                return dictValue;
            } catch (UnsupportedOperationException e) {
                // Apanha o mapa congelado do toObject()!
                throw new ControlFlow.RuntimeError(expr.name, "Erro de Segurança: Este objeto é estritamente imutável (Read-Only) pois foi exportado via toObject(). Nenhuma propriedade pode ser modificada ou removida.");
            }
        }

        // 2. É Atribuição numa Instância POO? (Ex: leao.nome = "Simba")
        if (object instanceof XplInstance instance) {
            Stmt.FieldDecl field = instance.klass.model.fields.get(expr.name.lexeme);

            if (field != null) {
                // 🛑 CHAMA A POLÍCIA ANTES DE ESCREVER! 🛑
                checkAccess(expr.name, field, instance.klass.model, true);

                instance.set(expr.name, value);
                return value;
            }
            throw new ControlFlow.RuntimeError(expr.name, "A propriedade '" + expr.name.lexeme + "' não existe no modelo " + instance.klass.model.name + ".");
        }

        // 3. Se não for Classe nem Instância, é um erro estrutural!
        throw new ControlFlow.RuntimeError(expr.name, "Apenas instâncias e classes XPL possuem propriedades modificáveis.");
    }

    @Override
    public Object visitSuperExpr(Expr.Super expr) {
        try {
            // 1. Apanha a instância atual e em que classe estamos AGORA
            Object currentInstance = environment.get("this");
            XPLModel currentModel = (XPLModel) environment.get("__current_model");

            // 2. Descobre quem é o Pai!
            XPLModel superclass = currentModel.superclass;
            if (superclass == null) {
                throw new ControlFlow.RuntimeError(expr.keyword, "A classe '" + currentModel.name + "' não possui uma superclasse.");
            }

            // 3. Puxa o método lá de cima
            Stmt.Function method = superclass.findMethod(expr.method.lexeme);
            if (method == null) {
                throw new ControlFlow.RuntimeError(expr.method, "Método '" + expr.method.lexeme + "' não encontrado na superclasse.");
            }

            // 4. Prepara a invocação!
            XPLModel owner = superclass.getOwnerOfMethod(expr.method.lexeme);
            XplFunction function = new XplFunction(method, this.globals, owner);

            return function.bind((XplInstance) currentInstance);

        } catch (RuntimeException e) {
            throw new ControlFlow.RuntimeError(expr.keyword, "A palavra-chave 'super' só pode ser usada dentro de um método herdado.");
        }
    }

    @Override
    public Object visitCastExpr(Expr.Cast expr) {
        Object value = evaluate(expr.value);

        // Se for null, devolvemos null (não se pode fazer cast a nulls, a menos que seja um upcast que já era null)
        if (value == null) return null;

        TokenType targetType = expr.type.name.type;

        try {
            switch (targetType) {
                case T_INT:
                    switch (value) {
                        case Double aDouble -> {
                            return aDouble.longValue();
                        }
                        case Long l -> {
                            return value;
                        }
                        case String string -> {
                            return Long.parseLong(string);
                        }
                        default -> {
                        }
                    }
                    break;
                case T_FLOAT:
                    switch (value) {
                        case Long l -> {
                            return l.doubleValue();
                        }
                        case Double v -> {
                            return value;
                        }
                        case String string -> {
                            return Double.parseDouble(string);
                        }
                        default -> {
                        }
                    }
                    break;
                case T_STRING:
                    return stringify(value);
                case IDENTIFIER:
                    if (value instanceof XplInstance instance) {
                        if (instance.klass.model.isSubclassOf(expr.type.name.lexeme)) {
                            return value; // Upcast seguro
                        }
                    }
                    break;
            }
        } catch (NumberFormatException e) {
            // O cast falhou matematicamente (ex: "texto" as! int)
        }

        // ⭐ A DECISÃO: Seguro vs Forçado ⭐
        if (expr.isForced) {
            throw new ControlFlow.RuntimeError(expr.operator, "Cast Forçado Falhou (ClassCastException): Não é possível converter '" + stringify(value) + "' para o tipo " + expr.type.name.lexeme + ".");
        }

        return null; // Cast Seguro devolve null!
    }

    @Override
    public Object visitTypeCheckExpr(Expr.TypeCheck expr) {
        Object left = evaluate(expr.left);
        String targetType = expr.rightType.name.lexeme;

        if (expr.operator.type == TokenType.INSTANCE) {
            // ⭐ INSTANCE: Exige correspondência exata de classe! Sem heranças.
            if (left instanceof XplInstance) {
                return ((XplInstance) left).klass.model.name.equals(targetType);
            }
            return false; // Primitivos não são instâncias exactas de classes
        } else {
            // ⭐ TYPE: Validação flexível (aceita primitivos e subclasses)
            if (left == null) return false;
            if (targetType.equals("int") && left instanceof Long) return true;
            if (targetType.equals("float") && (left instanceof Double || left instanceof Long)) return true;
            return switch (left) {
                case String string when targetType.equals("string") -> true;
                case Boolean b when targetType.equals("bool") -> true;
                case List list when targetType.equals("array") -> true;
                case XplInstance xplInstance -> xplInstance.klass.model.isSubclassOf(targetType); // O ADN bate certo?

                default -> false;
            };
        }
    }

    @Override
    public Object visitTypeofExpr(Expr.Typeof expr) {
        Object value = evaluate(expr.expression);
        return switch (value) {
            case null -> "null";
            case Long l -> "int";
            case Double v -> "float";
            case String string -> "string";
            case Boolean b -> "bool";
            case List list -> "array";
            case Map map -> "object";
            case XplInstance xplInstance -> xplInstance.klass.model.name;
            case XplClass xplClass -> "class";
            default -> "unknown";
        };
    }

    @Override
    public Object visitIfExpr(Expr.If expr) {
        Object condValue = evaluate(expr.condition);

        Stmt branchToRun = isTruthy(condValue) ? expr.thenBranch : expr.elseBranch;

        if (branchToRun == null) return null;

        return evaluateBranchAsExpression(branchToRun);
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        // 1. Avalia APENAS o lado esquerdo primeiro!
        Object left = evaluate(expr.left);

        // 2. A Magia do Curto-Circuito
        if (expr.operator.type == TokenType.OR) {
            // Se for '||' e a esquerda já for VERDADEIRA, a condição inteira já é verdadeira.
            // Ignoramos completamente a direita e devolvemos o valor esquerdo!
            if (isTruthy(left)) return left;

        } else {
            // Se não é OR, é AND ('&&').
            // Se for '&&' e a esquerda for FALSA, a condição inteira já falhou.
            // Ignoramos a direita (evitando crashes) e devolvemos o valor esquerdo!
            if (!isTruthy(left)) return left;
        }

        // 3. Se o curto-circuito não foi ativado (ex: 'falso || X' ou 'verdadeiro && X'),
        // a resposta final depende exclusivamente do lado direito.
        return evaluate(expr.right);
    }

    @Override
    public Object visitSwitchExpr(Expr.Switch expr) {
        // 1. Avalia o Alvo principal (ex: a variável que está dentro do switch(x))
        Object targetValue = evaluate(expr.target);

        // 2. Percorre todos os casos à procura de um match perfeito
        for (Expr.SwitchCase switchCase : expr.cases) {
            for (Expr caseValueExpr : switchCase.values) {
                Object caseValue = evaluate(caseValueExpr);

                // Compara o alvo com o valor do caso usando o método seguro da linguagem
                if (isEqualStrict(targetValue, caseValue)) {
                    // ⭐ DEVOLVE IMEDIATO! (Simula o Break automático + Retorno)
                    return evaluateBranchAsExpression(switchCase.body);
                }
            }
        }

        // 3. Se nenhum caso bateu certo, tenta a rota de fuga (default)
        if (expr.defaultBranch != null) {
            return evaluateBranchAsExpression(expr.defaultBranch);
        }

        // 4. Se não tem default e falhou tudo, não devolve nada.
        return null;
    }

    @Override
    public Object visitMatchExpr(Expr.Match expr) {
        Object targetVal = evaluate(expr.target);

        for (Expr.MatchArm arm : expr.arms) {

            // 1. Teste de Tipo falhou? Salta para o próximo braço!
            if (arm.typeTest != null) {
                if (!checkTypeMatch(targetVal, arm.typeTest)) continue;
            }

            // 2. Teste de Valor falhou? Salta!
            if (arm.valueTest != null) {
                Object valValue = evaluate(arm.valueTest);
                if (!isEqualStrict(targetVal, valValue)) continue;
            }

            // 3. A Guarda Matemática 'if (cond)' deu falso? Salta!
            if (arm.guard != null) {
                Object guardResult = evaluate(arm.guard);
                if (!isTruthy(guardResult)) continue;
            }

            // ⭐ PASSOU NA ALFÂNDEGA! Este é o braço vencedor.
            return evaluateBranchAsExpression(arm.body);
        }

        // Se nenhum braço serviu, tenta a tua saída limpa (none: ou default:)
        if (expr.defaultBranch != null) {
            return evaluateBranchAsExpression(expr.defaultBranch);
        }

        return null;
    }

    // =========================================================================
    // 1. COALESCÊNCIA NULA ( a ?? b )
    // =========================================================================
    @Override
    public Object visitNullCoalesceExpr(Expr.NullCoalesce expr) {
        Object left = evaluate(expr.left);

        // Se a esquerda for estritamente nula, avalia e devolve a direita!
        if (left == null) {
            return evaluate(expr.right);
        }
        return left;
    }

    // =========================================================================
    // 2. UNWRAP FORÇADO ( obj! )
    // =========================================================================
    @Override
    public Object visitUnwrapExpr(Expr.Unwrap expr) {
        Object valor = evaluate(expr.expr);

        if (valor == null) {
            // ⭐ A TUA MENSAGEM EXATA DO VOLUME 21:
            throw new ControlFlow.RuntimeError(expr.operator,
                    "UnwrapError: Tentativa de abrir um valor nulo!");
        }
        return valor;
    }

    // =========================================================================
    // 3. ENCADEAMENTO DE PROPRIEDADE ( obj?.prop )
    // =========================================================================
    // =========================================================================
    // 3. ENCADEAMENTO DE PROPRIEDADE ( obj?.prop ) - BILINGUE ⭐
    // =========================================================================
    @Override
    public Object visitOptionalChainingExpr(Expr.OptionalChaining expr) {
        Object leftObject = evaluate(expr.object);

        // 1. Se o elo anterior é estritamente nulo, a corrente dissipa-se em null
        if (leftObject == null) return null;

        // ROTA A: É uma instância de um 'declare' do utilizador?
        if (leftObject instanceof XplInstance) {
            return ((XplInstance) leftObject).get(expr.name);
        }

        // ⭐ ROTA B: É um Mapa / Dicionário Literal? (A salvação da Linha 36!)
        if (leftObject instanceof Map<?, ?> mapa) {
            String chave = expr.name.lexeme;

            // Em JS/TypeScript, fazer 'mapa?.chaveInexistente' devolve null em vez de dar erro.
            return mapa.getOrDefault(chave, null);
        }

        throw new ControlFlow.RuntimeError(expr.name,
                "Operação '?.' inválida: O alvo (do tipo " + leftObject.getClass().getSimpleName() + ") não possui propriedades acessíveis.");
    }

    // =========================================================================
    // 4. CHAMADA OPCIONAL DE MÉTODO ( obj?.metodo() ) - BILINGUE ⭐
    // =========================================================================
    @Override
    public Object visitOptionalCallExpr(Expr.OptionalCall expr) {
        Object leftObject = evaluate(expr.object);

        if (leftObject == null) return null;

        Object metodoInvocavel = null;

        // Rota A: Método de uma XplInstance
        if (leftObject instanceof XplInstance) {
            metodoInvocavel = ((XplInstance) leftObject).get(expr.methodName);
        }
        // Rota B: Uma função/lambda guardada dentro de uma chave de um Mapa Literal!
        else if (leftObject instanceof java.util.Map) {
            metodoInvocavel = ((java.util.Map<?, ?>) leftObject).get(expr.methodName.lexeme);
        }

        if (metodoInvocavel instanceof XplCallable callable) {

            java.util.List<Object> evalArgs = new java.util.ArrayList<>();
            for (Expr arg : expr.arguments) {
                evalArgs.add(evaluate(arg));
            }
            return callable.call(this, evalArgs);
        }

        throw new ControlFlow.RuntimeError(expr.methodName,
                "O método opcional '?." + expr.methodName.lexeme + "()' não existe ou não é invocável no objeto alvo.");
    }


    // =========================================================================
    // O DETETOR DE METADADOS (Verifica se um Objeto Java pertence a um TypeNode)
    // =========================================================================
    private boolean checkTypeMatch(Object obj, TypeNode typeNode) {

        // ⭐ 0. O PRISMA DO VOLUME 20: Dissolve qualquer Alias no seu tipo concreto! ⭐
        typeNode = resolveConcreteType(typeNode);

        // ⭐ 1. A REGRA DE OURO DO VOLUME 21 ⭐
        if (typeNode instanceof TypeNode.Optional) {
            // Se o objeto é nulo, e o tipo aceita nulo (?T), PASSOU NA ALFÂNDEGA!
            if (obj == null) return true;

            // Se não é nulo, desempacota o '?' e testa o valor real contra o tipo interno:
            return checkTypeMatch(obj, ((TypeNode.Optional) typeNode).innerType);
        }

        // Para todos os outros tipos normais (não-opcionais), o null é estritamente PROIBIDO!
        if (obj == null) return false;

        Token typeToken;
        if (typeNode instanceof TypeNode.Simple) {
            typeToken = ((TypeNode.Simple) typeNode).name;
        } else {
            return false;
        }

        switch (typeToken.type) {
            case T_INT:     return obj instanceof Long || obj instanceof Integer;
            case T_FLOAT:   return obj instanceof Double || obj instanceof Float;
            case T_STRING:  return obj instanceof String;
            case T_BOOL:    return obj instanceof Boolean;
            case T_ARRAY:   return obj instanceof java.util.List;
            case T_OBJECT:  return obj instanceof java.util.Map;

            case IDENTIFIER:
                String customTypeName = typeToken.lexeme;
                if (obj instanceof XplInstance) {
                    XPLModel modelo = ((XplInstance) obj).klass.model;
                    return modelo.isSubclassOf(customTypeName);
                }
                return obj.getClass().getSimpleName().equals(customTypeName);

            default:
                return false;
        }
    }


    // =========================================================================
    // O PRISMA DE DISSOLUÇÃO (Auxiliar do Volume 20)
    // =========================================================================
    private TypeNode resolveConcreteType(TypeNode node) {
        if (node instanceof TypeNode.Simple) {
            String typeName = ((TypeNode.Simple) node).name.lexeme;

            // Se este identificador é um Alias conhecido, mergulha recursivamente!
            if (this.typeAliases.containsKey(typeName)) {
                return resolveConcreteType(this.typeAliases.get(typeName));
            }
        }
        else if (node instanceof TypeNode.Optional) {
            TypeNode resolvedInner = resolveConcreteType(((TypeNode.Optional) node).innerType);

            // ⭐ Otimização de Garbage Collector: Se o miolo não era um alias, devolve a casca intacta!
            if (resolvedInner == ((TypeNode.Optional) node).innerType) {
                return node;
            }
            return new TypeNode.Optional(resolvedInner);
        }
        return node; // É matéria nativa pura (int, string, Map), devolve como está.
    }

    // ⭐ O MOTOR DA "ÚLTIMA LINHA" (Retorno Implícito) ⭐
    private Object evaluateBranchAsExpression(Stmt branch) {
        // 1. É um Bloco { ... }? Executa tudo e captura a última respiração!
        if (branch instanceof Stmt.Block) {
            java.util.List<Stmt> statements = ((Stmt.Block) branch).statements;
            if (statements.isEmpty()) return null;

            Environment previous = this.environment;
            try {
                this.environment = new Environment(previous);
                Object lastValue = null;

                for (Stmt stmt : statements) {
                    if (stmt instanceof Stmt.ExpressionStmt) {
                        // Se for uma expressão solta (Ex: 'v ** 3;'), guardamos o seu valor!
                        lastValue = evaluate(((Stmt.ExpressionStmt) stmt).expression);
                    } else {
                        // Se for um 'var x = 1' ou um 'while', o valor gerado é null
                        execute(stmt);
                        lastValue = null;
                    }
                }
                return lastValue; // O valor da linha final!
            } finally {
                this.environment = previous;
            }
        }
        // 2. É uma expressão simples (O inline do Python ou um if de 1 linha sem {})?
        else if (branch instanceof Stmt.ExpressionStmt) {
            return evaluate(((Stmt.ExpressionStmt) branch).expression);
        }
        // 3. É um comando normal (Ex: return x; ou break;)?
        else {
            execute(branch);
            return null;
        }
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name.lexeme);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        environment.assign(expr.name.lexeme, value);
        return value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        if (expr.operator.type == TokenType.MINUS) {
            checkNumberOperand(expr.operator, right);
            if (right instanceof Double) return -(double) right;
            if (right instanceof Long) return -(long) right;
        } else if (expr.operator.type == TokenType.BANG) {
            return !isTruthy(right);
        }
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case PLUS:
                if (left instanceof String || right instanceof String) return stringify(left) + stringify(right);
                if (left instanceof Double || right instanceof Double) return toDouble(left) + toDouble(right);
                if (left instanceof Long && right instanceof Long) return (long) left + (long) right;
                throw new ControlFlow.RuntimeError(expr.operator, "Os operandos devem ser números ou strings.");
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                if (left instanceof Double || right instanceof Double) return toDouble(left) - toDouble(right);
                return (long) left - (long) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                if (left instanceof Double || right instanceof Double) return toDouble(left) * toDouble(right);
                return (long) left * (long) right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if (toDouble(right) == 0)
                    throw new ControlFlow.RuntimeError(expr.operator, "Divisão por zero não permitida.");
                if (left instanceof Double || right instanceof Double) return toDouble(left) / toDouble(right);
                return (long) left / (long) right;
            case MODULO:
                checkNumberOperands(expr.operator, left, right);
                if (toDouble(right) == 0)
                    throw new ControlFlow.RuntimeError(expr.operator, "Divisão por zero não permitida.");
                if (left instanceof Double || right instanceof Double) return (toDouble(left) % toDouble(right));
                return (long) left % (long) right;
            case HASH:
                checkNumberOperands(expr.operator, left, right);
                if (toDouble(right) == 0)
                    throw new ControlFlow.RuntimeError(expr.operator, "Divisão por zero não permitida.");
                if (left instanceof Double || right instanceof Double) return (long)(toDouble(left) / toDouble(right));
                return (long) left / (long) right;
            case POWER:
                checkNumberOperands(expr.operator, left, right);
                if (toDouble(right) == 0)
                    throw new ControlFlow.RuntimeError(expr.operator, "Divisão por zero não permitida.");
                if (left instanceof Double || right instanceof Double) return Math.pow(toDouble(left) , toDouble(right));
                return (long)Math.pow((long) left ,(long) right);
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return toDouble(left) > toDouble(right);
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return toDouble(left) >= toDouble(right);
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return toDouble(left) < toDouble(right);
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return toDouble(left) <= toDouble(right);
            case EQUAL:
                return isEqual(left, right);
            case NOT_EQUAL:
                return !isEqual(left, right);
            // ==========================================
            // 1. IGUALDADE ESTRITA (=== e !==)
            // ==========================================
            case STRICT_EQUAL:
                return isEqualStrict(left, right);
            case STRICT_NOT_EQUAL:
                return !isEqualStrict(left, right);

            // ==========================================
            // 2. OPERADORES DE BITS (Bitwise)
            // ==========================================
            case BIT_AND:
                return toBitLong(expr.operator, left) & toBitLong(expr.operator, right);
            case BIT_OR:
                return toBitLong(expr.operator, left) | toBitLong(expr.operator, right);
            case BIT_XOR:
                return toBitLong(expr.operator, left) ^ toBitLong(expr.operator, right);
            case SHIFT_LEFT:
                return toBitLong(expr.operator, left) << toBitLong(expr.operator, right);
            case SHIFT_RIGHT:
                return toBitLong(expr.operator, left) >> toBitLong(expr.operator, right);
        }
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        // 1. Avalia o nome da função (ex: procura 'println' ou 'shell' na memória)
        Object callee = evaluate(expr.callee);

        // 2. Avalia os argumentos que passaste dentro dos parênteses
        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        // 3. Verifica se o que tentaste chamar é realmente uma função
        if (!(callee instanceof XplCallable function)) {
            throw new ControlFlow.RuntimeError(expr.paren, "Isto não é uma função e não pode ser chamado.");
        }

        // 4. Valida a quantidade de parâmetros
        if (function.arity() != -1 && arguments.size() != function.arity()) {
            throw new ControlFlow.RuntimeError(expr.paren,
                    "Esperado " + function.arity() + " argumentos, mas obteve " + arguments.size() + ".");
        }

        // 5. Executa a função de verdade!
        try {
            return function.call(this, arguments);
        } catch (RuntimeException e) {
            throw new ControlFlow.RuntimeError(expr.paren, e.getMessage());
        }
    }

    // ==========================================
    // MÉTODOS AUXILIARES
    // ==========================================

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private double toDouble(Object obj) {
        if (obj instanceof Double) return (double) obj;
        if (obj instanceof Long) return (double) (long) obj;
        return 0.0;
    }


    // Assistente da Igualdade Estrita (Compara Memória/Tipo antes do valor)
    private boolean isEqualStrict(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        // Se as classes nativas do Java forem diferentes (ex: Long vs String), é Falso!
        if (!a.getClass().equals(b.getClass())) return false;
        return a.equals(b);
    }

    // Assistente de Conversão de Bits
    private long toBitLong(Token op, Object operand) {
        if (operand instanceof Double) return ((Double) operand).longValue();
        if (operand instanceof Long) return (Long) operand;
        throw new ControlFlow.RuntimeError(op, "Operadores de bits (&, |, <<, >>) requerem valores inteiros.");
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double || operand instanceof Long) return;
        throw new ControlFlow.RuntimeError(operator, "O operando deve ser um número.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if ((left instanceof Double || left instanceof Long) && (right instanceof Double || right instanceof Long))
            return;
        throw new ControlFlow.RuntimeError(operator, "Ambos os operandos devem ser números.");
    }

    private void checkTypeCompatability(Token typeAnnotation, Object value) {
        switch (typeAnnotation.type) {
            case T_INT:
                if (!(value instanceof Long))
                    throw new ControlFlow.RuntimeError(typeAnnotation, "O valor atribuído não é do tipo 'long'.");
                break;
            case T_FLOAT:
                if (!(value instanceof Double || value instanceof Long))
                    throw new ControlFlow.RuntimeError(typeAnnotation, "O valor atribuído não é do tipo 'float'.");
                break;
            case T_STRING:
                if (!(value instanceof String))
                    throw new ControlFlow.RuntimeError(typeAnnotation, "O valor atribuído não é do tipo 'string'.");
                break;
            case T_ARRAY:
                if (!(value instanceof List))
                    throw new ControlFlow.RuntimeError(typeAnnotation, "O valor atribuído não é do tipo 'array'.");
                break;
        }
    }

    // ==========================================
    // MÉTODOS AUXILIARES (CORES E STRINGS)
    // ==========================================

    // Converte literais do Java para representação XPL segura no terminal
    // ⭐ A MAGIA DO TOSTRING NATIVO (AGORA RECURSIVO!) ⭐
    private String stringify(Object object) {
        if (object == null) return "null";

        // 1. Se for uma Instância XPL, tenta invocar o toString() automaticamente!
        if (object instanceof XplInstance instance) {
            Stmt.Function toStringMethod = instance.klass.model.findMethod("toString");

            if (toStringMethod != null && toStringMethod.params.isEmpty()) {
                try {
                    XPLModel owner = instance.klass.model.getOwnerOfMethod("toString");
                    XplFunction func = new XplFunction(toStringMethod, instance.klass.closure, owner);
                    Object result = func.bind(instance).call(this, new java.util.ArrayList<>());
                    return String.valueOf(result);
                } catch (Exception e) {
                    return "<Erro ao executar toString() na Instância de " + instance.klass.model.name + ">";
                }
            }
            return "<Instância de " + instance.klass.model.name + ">";
        }

        // ⭐ 2. INJEÇÃO RECURSIVA EM LISTAS (Arrays) ⭐
        if (object instanceof List<?> list) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < list.size(); i++) {
                sb.append(stringify(list.get(i))); // RECURSIVIDADE: Chama a magia de novo!
                if (i < list.size() - 1) sb.append(", ");
            }
            sb.append("]");
            return sb.toString();
        }

        // ⭐ 3. INJEÇÃO RECURSIVA EM MAPAS (Dicionários/toObject) ⭐
        if (object instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            int i = 0;
            for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
                sb.append(entry.getKey().toString()).append(": ").append(stringify(entry.getValue()));
                if (i < map.size() - 1) sb.append(", ");
                i++;
            }
            sb.append("}");
            return sb.toString();
        }

        // 4. Comportamento numérico base
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    // Converte "#RRGGBB" para Códigos ANSI True Color (24-bit)
    private String hexToAnsi(String hex) {
        if (hex != null && hex.startsWith("#") && hex.length() == 7) {
            try {
                long r = Math.toIntExact(Long.valueOf(hex.substring(1, 3), 16));
                long g = Math.toIntExact(Long.valueOf(hex.substring(3, 5), 16));
                long b = Math.toIntExact(Long.valueOf(hex.substring(5, 7), 16));
                return String.format("\033[38;2;%d;%d;%dm", r, g, b);
            } catch (NumberFormatException e) {
                return ConsoleTheme.TEXT;
            }
        }
        return ConsoleTheme.TEXT;
    }
}