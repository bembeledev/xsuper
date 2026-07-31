package com.dic.xsuper.engine.core;
import com.dic.xsuper.cli.core.CommandRegistry;
import com.dic.xsuper.dom.node.XplNativeObject;
import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.ast.Stmt;
import com.dic.xsuper.engine.ast.TypeNode;
import com.dic.xsuper.engine.execution.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.execution.XplFunction;
import com.dic.xsuper.engine.helpers.ArrayMethods;
import com.dic.xsuper.engine.helpers.ObjectMethods;
import com.dic.xsuper.engine.helpers.StringMethods;
import com.dic.xsuper.engine.poo.*;
import com.dic.xsuper.engine.poo.relection.*;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Path;
import java.util.*;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    // O escopo global que criámos na Fase 1
    // 1. Cria a caixa global
    public Environment globals = new Environment();
    public final CommandRegistry registry;
    public Path currentDirectory;
    // 2. O ambiente atual aponta para a caixa global logo no início!
    public Environment environment = globals;
    // ⭐ O NOSSO REGISTRY GLOBAL ⭐
    // Guarda tanto os modelos-base (Declare) quanto as variantes (Implement as)
    public Map<String, XPLModel> registry_model = new HashMap<>();
    private final Map<String, XplInterface> registry_Interfaces = new HashMap<>();


    // Guarda o Metadado completo dos Tipos para podermos executar Listeners/Validadores!
    public final java.util.Map<String, Stmt.TypeAliasDecl> typeAliasMetadata = new java.util.HashMap<>();

    // ⭐ NOVO: Guarda as instâncias VIVAS (Singletons) dos listeners dos tipos! ⭐
    public final java.util.Map<String, java.util.List<XplInstance>> typeListenersRegistry = new java.util.HashMap<>();
    // =========================================================================
    // ⭐ CÂMARA CRIOGÉNICA DE GENÉRICOS (Monomorfização) ⭐
    // Guarda o nó cru da AST exatamente como o utilizador o digitou!
    // =========================================================================

    // O Armazém Global de prismas semânticos (NomeDoAlias -> TipoReal):
    public final java.util.Map<String, TypeNode> typeAliases = new java.util.HashMap<>();

    // ⭐ CÂMARA CRIOGÉNICA DE MOLDES GENÉRICOS (Monomorfização) ⭐
    private final Map<String, XPLModel> registry_generic_models = new HashMap<>();


    // Ponteiro quântico para saber que módulo estamos a compilar neste momento

    // O Manager supremo de Módulos (Gere Cache, I/O e Exports)
    public final com.dic.xsuper.engine.modules.XplModuleManager moduleManager;

    public Interpreter(CommandRegistry registry, Path currentDirectory) {

        this.registry = registry;
        this.currentDirectory = currentDirectory;


        // ⭐ INJETA O MANAGER
        this.moduleManager = new com.dic.xsuper.engine.modules.XplModuleManager(currentDirectory);

        decoratorInject();

    }

    private void decoratorInject() {
        XPLModel rootDecModel = new XPLModel("DecoratorRoot", null);
        rootDecModel.isDecorator = true;
        this.registry_model.put("DecoratorRoot", rootDecModel);
    }

    public static java.util.List<Object> unpackNativeArgs(Interpreter interpreter, java.util.List<Expr.CallArg> rawArgs) {
        java.util.List<Object> evaluated = new java.util.ArrayList<>();
        for (Expr.CallArg arg : rawArgs) {
            // As funções nativas do Prelude não usam nomes, engolem tudo posicionalmente:
            evaluated.add(interpreter.evaluate(arg.expression));
        }
        return evaluated;
    }

    // ⭐ A CURA DA CONCORRÊNCIA: Fork do Interpretador ⭐
    // Cria um clone perfeito do motor para ser usado em Threads em Background,
    // partilhando os registos globais e a memória, mas isolando a Pilha de Execução.
    public Interpreter fork() {
        Interpreter threadEngine = new Interpreter(this.registry, this.currentDirectory);

        // 1. Partilhamos as Variáveis Globais e Nativas
        threadEngine.globals = this.globals;

        // 2. Partilhamos os Modelos de Classes e Decoradores
        threadEngine.registry_model = this.registry_model;

        // 3. A memória atual fica ligada ao global, mas o XplFunction vai
        // injetar a Closure (variáveis locais) correta quando a função arrancar!
        threadEngine.environment = this.globals;

        return threadEngine;
    }

    // =========================================================================
    // ⭐ MÁQUINAS DE EXTRAÇÃO DE AST (Para Metaprogramação)
    // =========================================================================

    public static Stmt.Block extractToBlock(Object val) {
        if (val instanceof XplFunction xplFunc) {
            return new Stmt.Block(xplFunc.declaration.body);
        }
        throw new ControlFlow.RuntimeError(null, "Falha de Metaprogramação: Esperado um bloco encapsulado (ex: () => { ... }).");
    }

    public static Expr extractExpression(Object val) {
        if (val instanceof XplFunction xplFunc) {
            if (!xplFunc.declaration.body.isEmpty()) {
                Stmt first = xplFunc.declaration.body.getFirst(); // ou get(0)
                if (first instanceof Stmt.Return ret) return ret.value;
                if (first instanceof Stmt.ExpressionStmt exprStmt) return exprStmt.expression;
            }
            throw new ControlFlow.RuntimeError(null, "Falha de Metaprogramação: Cápsula vazia.");
        }

        // ⭐ A MAGIA: Se o utilizador passar um valor direto (ex: Switch("comando") ou If(true)),
        // nós transformamos esse valor numa AST Literal para o motor conseguir ler em tempo real sem crashar!
        return new Expr.Literal(val);
    }

    public static Stmt extractFirstStatement(Object val) {
        if (val instanceof XplFunction xplFunc && !xplFunc.declaration.body.isEmpty()) {
            return xplFunc.declaration.body.getFirst();
        }
        throw new ControlFlow.RuntimeError(null, "Falha de Metaprogramação: Esperada uma cápsula com instrução (ex: () => { let i = 0; }).");
    }

    // =========================================================================
    // ⭐ DESCASCADOR QUÂNTICO DE TEARDOWN (@Listen.End) ⭐
    // =========================================================================
    private void triggerEndHooksRecursively(Object obj) {
        if (obj instanceof XplInstance instance) {
            // Se o objeto real tem Vigilantes nativos colados a ele:
            if (instance.fields.containsKey("__active_listeners__")) {
                @SuppressWarnings("unchecked")
                List<XplInstance> listeners = (List<XplInstance>) instance.fields.get("__active_listeners__");
                for (XplInstance dec : listeners) {
                    if (dec.klass.model.metaEndHook != null) {
                        Stmt.Function hookFunc = dec.klass.model.findMethod(dec.klass.model.metaEndHook);
                        if (hookFunc != null) {
                            try {
                                new XplFunction(hookFunc, dec.klass.closure, dec.klass.model).bind(dec).call(this, java.util.Collections.emptyList());
                            } catch (Exception ignored) {} // O Teardown de memória é estritamente silencioso
                        }
                    }
                }
            }
        }
    }

    public void interpret(List<Stmt> statements) {
        try {
            // A CADEIA DE GLOBAIS AUTOMÁTICA PARA O SCRIPT PRINCIPAL
            Environment parentEnv = this.globals;
            if (this.moduleManager.resolvePhysicalFile("globals.xpl") != null) {
                parentEnv = this.moduleManager.loadModule("globals", new Token(TokenType.IDENTIFIER, "globals", null, 0, 0), this, this.globals).localEnvironment;
            }
            this.environment = parentEnv;

            for (Stmt statement : statements) {
                if (statement == null) continue;
                execute(statement);
            }
        }
        catch (RuntimeException error) {
            // O ESCUDO DO JIT: Protege o relator de erros contra Tokens Fantasmas!
            if (error instanceof ControlFlow.RuntimeError rtError) {
                String path = (rtError.token != null && rtError.token.filePath != null) ? rtError.token.filePath : "Nativo/JIT";
                int line = (rtError.token != null) ? rtError.token.line : 0;
                int col = (rtError.token != null) ? rtError.token.column : 0;
                System.err.println(ConsoleTheme.ERROR + path + ":" + line + ":" + col + ":\n\t Erro de Execução: " + rtError.getMessage() + ConsoleTheme.RESET);
            }
            else if (error instanceof ControlFlow.ThrowException throwError) {
                System.err.println(ConsoleTheme.ERROR + "Erro Fatal Não Capturado (Uncaught Exception): " + stringify(throwError.value) + ConsoleTheme.RESET);
            } else {
                error.printStackTrace(); // Para erros profundos do Java
            }
        }
        finally {
            // Gatilho global de fim de script
            for (Object obj : globals.values.values()) {
                triggerEndHooksRecursively(obj);
            }
        }
    }

    public void execute(Stmt stmt) {
        // ⭐ VERIFICADOR DE SINAL DE MORTE (CANCELAMENTO) ⭐
        if (Thread.currentThread().isInterrupted()) {
            throw new ControlFlow.RuntimeError(null, "Thread XPL foi morta e abortada com sucesso.");
        }
        stmt.accept(this);
    }

    // =========================================================================
    // ⭐ MAPA DE CORRESPONDÊNCIA DE TIPOS (INFERÊNCIA DE DADOS) ⭐
    // =========================================================================
    public String getXplTypeName(Object value) {
        if (value == null) return "null";

        // Primitivos Nativos
        if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte) return "int";
        if (value instanceof Double || value instanceof Float) return "float";
        if (value instanceof String || value instanceof Character) return "string";
        return switch (value) {
            case Boolean b -> "bool";

            // Estruturas
            case List list -> "array";
            case Map map -> "object";

            // POO XPL
            case XplInstance inst -> (inst.klass != null) ? inst.klass.model.name : "MetaInstance";
            case XplClass xplClass -> "class";
            case XplCallable xplCallable -> "function";
            default -> "any";
        };

    }

    public Object evaluate(Expr expr) {
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
        } catch (Exception ignored) {

        } // Se der erro, estamos no espaço global (script)

        boolean isInsideClass = (currentModel != null && currentModel.name.equals(targetModel.name));
        boolean isSubclass = (currentModel != null && currentModel.isSubclassOf(targetModel.name));

        // 2. Regra do READONLY / DYN: Leitura pública, Escrita privada!
        if (isWriting && field.isReadonly && !isInsideClass) {
            throw new ControlFlow.RuntimeError(name, "Erro de Acesso: A propriedade '" + name.lexeme + "' é READONLY. Só pode ser alterada dentro da própria classe.");
        }

        // 3. Regra do PRIV (Privado): Só a própria classe lê e escreve!
        if (field.modifier.type == TokenType.PRIVATE && !isInsideClass) {
            throw new ControlFlow.RuntimeError(name, "Erro de Acesso: A propriedade '" + name.lexeme + "' é PRIVADA. Só a classe '" + targetModel.name + "' pode aceder.");
        }

        // 4. Regra do PROT (Protegido): Só a classe e os filhos (herança) acedem!
        if (field.modifier.type == TokenType.PROTECTED && !isSubclass) {
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
    public Void visitModuleDeclStmt(Stmt.ModuleDecl stmt) {
        if (this.moduleManager.currentCompilingModule != null) {
            String importPath = this.moduleManager.currentCompilingModule.path; // Ex: "geometria.Ponto"
            String declaredModule = stmt.modulePath;         // Ex: "geometria"

            // A FLEXIBILIDADE DOS NAMESPACES (Estilo Java)
            if (!importPath.equals(declaredModule) && !importPath.startsWith(declaredModule + ".")) {
                throw new ControlFlow.RuntimeError(stmt.keyword,
                        "Inconsistência de Namespace: O ficheiro físico declara pertencer ao módulo '" + declaredModule +
                                "', mas foi importado sob o caminho '" + importPath + "'. A hierarquia não coincide.");
            }
        }
        return null;
    }


    @Override
    public Void visitVarDeclStmt(Stmt.VarDecl stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        if (stmt.typeAnnotation != null && value != null) {
            checkTypeCompatability(stmt.typeAnnotation.name, value);
        }

        // =====================================================================
        // ⭐ NOVA ERA: INJEÇÃO NATIVA DE LISTENERS (Sem Proxies!) ⭐
        // =====================================================================
        if (stmt.listeners != null && !stmt.listeners.isEmpty()) {
            List<XplInstance> objectListeners = new ArrayList<>();

            for (Stmt.DecoratorNode adorno : stmt.listeners) {
                String listenerName = adorno.name.lexeme;
                XPLModel listenerModel = registry_model.get(listenerName);

                if (listenerModel == null) {
                    throw new ControlFlow.RuntimeError(adorno.name, "O identificador '" + listenerName + "' não designa um Listener válido.");
                }

                XplClass listenerClass;
                try { listenerClass = (XplClass) environment.get(listenerName); }
                catch (Exception e) { listenerClass = new XplClass(listenerModel, this.globals); }

                XplInstance listenerInstance = new XplInstance(listenerClass);
                // =====================================================================
                // ⭐ INJEÇÃO DE CONTEXTO: O Listener passa a conhecer o seu dono! ⭐
                // =====================================================================
                listenerInstance.fields.put("target", value);
                listenerInstance.fields.put("targetName", stmt.name.lexeme); // Guarda também o nome da variável!

                // ⭐ 1. O CONSTRUTOR (A Alfândega de Argumentos: Ex: value: 10)
                Stmt.Function construtor = listenerModel.findMethod("init");
                if (construtor != null) {
                    new XplFunction(construtor, listenerClass.closure, listenerModel).bind(listenerInstance).call(this, adorno.arguments);
                } else if (adorno.arguments != null && !adorno.arguments.isEmpty()) {
                    throw new ControlFlow.RuntimeError(adorno.name, "O listener '" + listenerName + "' recebeu argumentos, mas não possui um construtor 'init' declarado.");
                }

                // ⭐ 2. O GATILHO DE CICLO DE VIDA: @(Listen.Init)
                if (listenerModel.metaInitHook != null) {
                    Stmt.Function hookFunc = listenerModel.findMethod(listenerModel.metaInitHook);
                    if (hookFunc != null) {
                        new XplFunction(hookFunc, listenerClass.closure, listenerModel).bind(listenerInstance).call(this, java.util.Collections.emptyList());
                    }
                }

                objectListeners.add(listenerInstance);
            }

            // ⭐ INJEÇÃO ESTRUTURAL NO OBJETO POO ⭐
            // Em vez de um Proxy sujo, colamos os vigilantes na memória interna nativa do próprio objeto!
            if (value instanceof XplInstance inst) {
                inst.fields.put("__active_listeners__", objectListeners);
            }
        }

        String name = stmt.name.lexeme;
        switch (stmt.keyword.type) {
            case VAR:   environment.defineVar(name, value); break;
            case LET:   environment.defineLet(name, value); break;
            case CONST: environment.defineConst(name, value); break;
        }

        // =================================================================
        // ⭐ A MAGIA DA INFERÊNCIA E BLOQUEIO DE TIPO (Type Locking) ⭐
        // =================================================================
        String lockedType = "any";
        if (stmt.typeAnnotation != null) {
            // ROTA A: O utilizador exigiu o tipo explicitamente (let x: string)
            lockedType = stmt.typeAnnotation.name.lexeme;
        } else if (value != null) {
            // ROTA B: O motor infere o tipo olhando para o ADN do primeiro valor!
            lockedType = getXplTypeName(value);
        }

        // Tranca a variável no cofre de tipos!
        environment.lockType(name, lockedType);

        // ⭐ NOVO: Ativa a blindagem do tipo imediatamente ao nascer!
        if (value != null) {
            fireTypeListeners(lockedType, value, stmt.name);
        }

        return null;
    }

    // ⭐ POLÍCIA DE FRONTEIRA: Impede que uma variável mude de espécie!
    private void validateAssignmentType(Token nameToken, Object newValue) {
        String expectedType = environment.getLockedType(nameToken.lexeme);

        // Se a variável for flexível ("any") ou o novo valor for nulo, deixamos passar!
        if (expectedType.equals("any") || newValue == null) return;

        String actualType = getXplTypeName(newValue);

        // Fabricamos um TypeNode fantasma para passar pela tua Alfândega Quântica (checkTypeMatch)
        Token fakeToken = new Token(getPrimitiveTokenType(expectedType), expectedType, null, nameToken.line, nameToken.column);
        TypeNode fakeTypeNode = new TypeNode.Simple(fakeToken);

        // Aproveitamos a tua função que já sabe lidar com herança de POO e primitivos!
        if (!checkTypeMatch(newValue, fakeTypeNode)) {
            throw new ControlFlow.RuntimeError(nameToken,
                    "Violação de Tipagem Estrita: A variável '" + nameToken.lexeme + "' foi trancada como '" + expectedType + "'. Não podes atribuir um valor do tipo '" + actualType + "'.");
        }

        // ⭐ NOVO: A Tipagem Estrita passou? Ótimo! Dispara a validação comportamental!
        fireTypeListeners(expectedType, newValue, nameToken);
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(this.environment));
        return null;
    }

    public void executeBlock(List<Stmt> statements, Environment blockEnv) {
        Environment previous = this.environment;
        try {
            this.environment = blockEnv;
            for (Stmt statement : statements) {
                if (statement==null) continue;
                execute(statement);
            }
        } finally {
            // ⭐ O CANTO DO CISNE: Varre as variáveis que estão a morrer neste bloco
            // e desmonta as Matryoshkas disparando os ganchos @Context.End!
            for (Object obj : blockEnv.values.values()) {
                triggerEndHooksRecursively(obj);
            }
            this.environment = previous;
        }
    }


    // =========================================================================
    // ⭐ O MONOMORFIZADOR (Impressora 3D de Reificação C++ / Rust) ⭐
    // =========================================================================
    private XPLModel resolveMonomorphizedModel(Expr.New expr) {
        String baseName = expr.className.lexeme;

        // =====================================================================
        // ⭐ 1 e 2. EXTRAÇÃO DIRETA DA AST (Adeus manipulação manual de strings!)
        // =====================================================================
        List<TypeNode> typeArgsNodes = expr.typeArguments; // Agora é uma lista nativa!
        String[] concreteTypes = new String[typeArgsNodes.size()];

        // =====================================================================
        // ⭐ TRADUTOR DE GENÉRICOS ANINHADOS (JIT Translation) ⭐
        // =====================================================================
        XPLModel currentContext = null;
        try {
            currentContext = (XPLModel) environment.get("__current_model");
        } catch (Exception ignored) {}

        for (int i = 0; i < typeArgsNodes.size(); i++) {
            // Converte o TypeNode real numa String baseada no Lexema
            String cType = stringifyTypeNode(typeArgsNodes.get(i));

            // Bate na cábula do contexto atual e traduz instantaneamente:
            if (currentContext != null && currentContext.resolvedGenericMap.containsKey(cType)) {
                concreteTypes[i] = currentContext.resolvedGenericMap.get(cType);
            } else {
                concreteTypes[i] = cType;
            }
        }

        // 3. Vai buscar o Blueprint congelado à Câmara Criogénica
        XPLModel blueprint = registry_generic_models.get(baseName);
        if (blueprint == null) {
            throw new ControlFlow.RuntimeError(expr.className,
                    "Erro de Linkage: O molde genérico '" + baseName + "<...>' não foi declarado.");
        }

        // 4. Valida a Aridade Genérica (O número de tipos passados bate certo com os <T>?)
        if (concreteTypes.length != blueprint.typeParameters.size()) {
            throw new ControlFlow.RuntimeError(expr.className,
                    "Aridade Genérica Incorreta: O molde '" + baseName + "' requer " +
                            blueprint.typeParameters.size() + " parâmetro(s) de tipo, mas forneceste " + concreteTypes.length + ".");
        }

        // 5. Fabrica a Chave Genética Única da RAM (Ex: "Caixa<int>")
        String synthesizedName = baseName + "<" + String.join(", ", concreteTypes) + ">";

        // ⭐ OTIMIZAÇÃO DE CACHE: Se já fabricámos esta exata variação antes, devolve a que já está viva na RAM!
        if (registry_model.containsKey(synthesizedName)) {
            return registry_model.get(synthesizedName);
        }

        System.out.println("[XPL Monomorfizador] -> Sintetizando nova classe física na RAM: " + synthesizedName);

        // ⭐ O PARTO DA CLASSE CLONE ⭐
        XPLModel clonedModel = new XPLModel(synthesizedName, blueprint.superclass);
        clonedModel.hasBaseImplementation = blueprint.hasBaseImplementation;
        clonedModel.canBeInstantiated = true; // O clone nasce destrancado!

        // Monta o dicionário de tradução quântica { "T": "int", "U": "string", "V": "object" }
        Map<String, String> translationMap = new HashMap<>();
        for (int i = 0; i < blueprint.typeParameters.size(); i++) {
            translationMap.put(blueprint.typeParameters.get(i).lexeme, concreteTypes[i]);
        }

        // ⭐ GUARDA O ADN NESTA INSTÂNCIA PARA OS FILHOS PODEREM LER! ⭐
        clonedModel.resolvedGenericMap.putAll(translationMap);

        // --- A) TRANSMUTAR OS CAMPOS DA RAM (Fields) ---
        for (Stmt.FieldDecl oldField : blueprint.fields.values()) {
            TypeNode mutatedType = transmuteType(oldField.type, translationMap); // Nota: Usei typeAnnotation como definimos antes

            Stmt.FieldDecl newField = new Stmt.FieldDecl(
                    oldField.modifier, oldField.isStatic, oldField.isFinal, oldField.isReadonly, oldField.name, mutatedType
            );
            clonedModel.addField(newField);
        }

        // --- B) ⭐ TRANSMUTAR AS ASSINATURAS DOS MÉTODOS (Methods) ⭐ ---
        for (Stmt.Function oldMethod : blueprint.methods.values()) {
            // Invocamos o bisturi antes de cravar o método no clone!
            Stmt.Function mutatedMethod = transmuteMethodSignature(oldMethod, translationMap);
            clonedModel.addMethod(mutatedMethod);
        }

        clonedModel.defaultInstanceFields.putAll(blueprint.defaultInstanceFields);

        // 6. Regista a nova classe no ecossistema normal de execução
        registry_model.put(synthesizedName, clonedModel);

        // 7. Regista o construtor da classe no escopo global para o 'instanceof / typeof' funcionar
        XplClass runtimeClass = new XplClass(clonedModel, this.globals);
        this.environment.defineConst(synthesizedName, runtimeClass);

        return clonedModel;
    }

    // ⭐ MÁQUINA DE REVERSÃO: TypeNode -> String ⭐
    private String stringifyTypeNode(TypeNode node) {
        if (node instanceof TypeNode.Simple simple) {
            return simple.name.lexeme;
        }
        else if (node instanceof TypeNode.Generic gen) {
            StringBuilder sb = new StringBuilder(gen.name.lexeme).append("<");
            for (int i = 0; i < gen.typeArguments.size(); i++) {
                sb.append(stringifyTypeNode(gen.typeArguments.get(i)));
                if (i < gen.typeArguments.size() - 1) sb.append(", ");
            }
            sb.append(">");
            return sb.toString();
        }
        else if (node instanceof TypeNode.Optional opt) {
            return "?" + stringifyTypeNode(opt.innerType); // Se for ?MotorAPI
        }
        return "any"; // Fallback de segurança
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
        XplEnum xplEnum = new XplEnum(stmt.name.lexeme);

        for (Stmt.EnumVariant vNode : stmt.variants) {
            List<String> pNames = new ArrayList<>();
            for (Token pToken : vNode.params) {
                pNames.add(pToken.lexeme);
            }
            xplEnum.addVariant(new XplEnumVariant(stmt.name.lexeme, vNode.name.lexeme, pNames));
        }

        // Regista o Enum no ambiente global!
        environment.defineLet(stmt.name.lexeme, xplEnum);
        return null;
    }

    @Override
    public Void visitDeclareDeclStmt(Stmt.DeclareDecl stmt) {
        String modelName = stmt.name.lexeme;

        // =====================================================================
        // ⭐ ROTA A: É UM MOLDE GENÉRICO? (Ex: declare Caixa<T>)
        // =====================================================================
        if (stmt.typeParameters != null && !stmt.typeParameters.isEmpty()) {
            System.out.println("[XPL Genéricos] -> Criando Blueprint Estrutural: " + modelName + "<" + stmt.typeParameters.size() + " parâmetro(s)>");

            XPLModel blueprint = new XPLModel(modelName, null);
            blueprint.isSealed = stmt.isSealed;
            blueprint.isGenericBlueprint = true;
            blueprint.typeParameters = stmt.typeParameters;
            blueprint.canBeInstantiated = false;

            for (Stmt.FieldDecl field : stmt.fields) {
                blueprint.addField(field);
            }



            registry_generic_models.put(modelName, blueprint);
            this.environment.defineConst(modelName, blueprint);
            return null; // <-- Corta aqui! Não entra no registry_model normal.
        }

        // =====================================================================
        // ⭐ ROTA B: É UMA CLASSE CONCRETA NORMAL? (Ex: declare Pessoa)
        // =====================================================================
        System.out.println("[XPL Engine] -> Compilando Modelo de Dados (Declare): " + modelName);

        // 1. Resolve a herança (Extends)
        XPLModel superclass = null;
        if (stmt.superclass != null) {
            superclass = registry_model.get(stmt.superclass.lexeme);
            if (superclass == null) {
                throw new ControlFlow.RuntimeError(stmt.superclass,
                        "Erro: O modelo pai '" + stmt.superclass.lexeme + "' não foi encontrado.");
            }
        }

        XPLModel model = new XPLModel(modelName, superclass);
        model.isSealed = stmt.isSealed;
        model.canBeInstantiated = false;

        if (superclass != null) {
            model.fields.putAll(superclass.fields);
        }

        for (Stmt.FieldDecl field : stmt.fields) {
            model.addField(field);
        }


        registry_model.put(modelName, model);
        this.environment.defineConst(modelName, model);
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

        // =====================================================================
        // ⭐ A MURALHA HÍBRIDA FINAL (SEALED CLASSES BLINDADO) ⭐
        // =====================================================================
        // 1. O símbolo foi importado?
        boolean isImported = this.environment.isImported(baseName);

        // =====================================================================
        // ⭐ A MURALHA DE ENCAPSULAMENTO (SEALED CLASSES / OPAQUE TYPES) ⭐
        // =====================================================================
        Object localSymbol = null;
        XPLModel baseModel;
        boolean isLocal = true;
        try {
            // Tenta ler o declare da memória ESTRITAMENTE LOCAL do ficheiro!
            localSymbol = this.environment.get(baseName);
        } catch (RuntimeException e) {
            isLocal = false; // Não foi criado nem importado neste ficheiro!
        }

        if (!isLocal) {
            // 2. Não está local? Tenta buscar ao Cofre Global (Extensão Comunitária!)
            localSymbol = safeGetSymbol(baseName);
            if (localSymbol == null) {
                throw new ControlFlow.RuntimeError(stmt.targetName, "Erro Fatal: O modelo '" + baseName + "' não foi declarado em lado nenhum no ecossistema.");
            }
        }

        if (localSymbol instanceof XPLModel) {
            baseModel = (XPLModel) localSymbol;
        } else if (localSymbol instanceof XplClass) {
            // Se já tem implementação, o símbolo exportado foi uma classe executável.
            // Extraímos a "alma" (XPLModel) lá de dentro!
            baseModel = ((XplClass) localSymbol).model;
        } else {
            throw new ControlFlow.RuntimeError(stmt.targetName, "O identificador '" + baseName + "' não corresponde a um modelo de dados válido para implementação.");
        }

        // =====================================================================
        // ⭐ A GUILHOTINA DO 'SEALED' ⭐
        // Se o programador for de FORA do ficheiro (isLocal = false) e tentar
        // mexer num declare que o autor marcou como SEALED, o compilador esmaga a execução!
        // =====================================================================
        if ((isImported || !isLocal) && baseModel.isSealed) {
            throw new ControlFlow.RuntimeError(stmt.targetName,
                    "Erro de Segurança (Sealed Class): O 'declare " + baseName + "' está SELADO. " +
                            "Modelos selados só podem ser implementados no próprio ficheiro onde foram criados. Acesso negado para extensões externas.");
        }

        // =====================================================================
        // ⭐ ROTA A: É A IMPLEMENTAÇÃO DE UM MOLDE GENÉRICO? (Ex: implement Caixa<T>)
        // =====================================================================
        if (baseModel.isGenericBlueprint) {
            baseModel.hasBaseImplementation = true;


            // =====================================================================
            // ⭐ A ALFÂNDEGA DE MÉTODOS E AUDITORIA DO @Override ⭐
            // =====================================================================
            for (Stmt.Function method : stmt.methods) {
                boolean hasOverrideDecorator = false;

                // 1. Vasculha os decoradores à procura do @Override e dos Hooks
                if (method.decorators != null) {
                    for (Stmt.DecoratorNode dec : method.decorators) {
                        String decName = dec.name.lexeme;

                        if (decName.equals("Override")) {
                            hasOverrideDecorator = true; // Detetou o decorador!
                        }
                        // Os teus hooks originais de Metaprogramação
                        else if (decName.contains("Init")) { baseModel.metaInitHook = method.name.lexeme; }
                        else if (decName.contains("Get")) { baseModel.metaGetHook = method.name.lexeme; }
                        else if (decName.contains("Set")) { baseModel.metaSetHook = method.name.lexeme; }
                        else if (decName.contains("End") || decName.contains("Morrer")) { baseModel.metaEndHook = method.name.lexeme; }
                    }
                }

                // ⭐ 2. VERIFICAÇÃO DE ADN (SUPERCLASSE)
                // O modelo pai já tem este método?
                boolean overridesSuper = (baseModel.superclass != null && baseModel.superclass.findMethod(method.name.lexeme) != null);

                // ⭐ 3. VERIFICAÇÃO DE CONTRATOS (INTERFACES)
                // Este método pertence a alguma interface assinada neste bloco 'implement'?
                boolean fulfillsInterface = false;
                if (stmt.interfaces != null) {
                    for (Token interfaceToken : stmt.interfaces) {
                        XplInterface contract = registry_Interfaces.get(interfaceToken.lexeme);
                        if (contract != null && contract.requiredMethods.containsKey(method.name.lexeme)) {
                            fulfillsInterface = true;
                            break; // Encontrou na interface, não precisa procurar mais!
                        }
                    }
                }

                // =====================================================================
                // ⭐ A SENTENÇA DO @Override (Fail-Fast)
                // =====================================================================
                // REGRA 1: Prometeu sobrepor, mas escreveu mal o nome ou não existe?
                if (hasOverrideDecorator && !overridesSuper && !fulfillsInterface) {
                    throw new ControlFlow.RuntimeError(method.name,
                            "Erro de Sobreposição: O método '" + method.name.lexeme + "()' está marcado com @Override, mas não está a sobrepor nenhum método da superclasse nem a cumprir nenhum contrato de interface.");
                }

                // REGRA 2: Sobrepôs em silêncio sem colocar a placa de @Override?
                if (!hasOverrideDecorator && (overridesSuper || fulfillsInterface)) {
                    throw new ControlFlow.RuntimeError(method.name,
                            "Decorador Ausente: O método '" + method.name.lexeme + "()' está a cumprir um contrato (Interface) ou a sobrepor um método pai. É obrigatório marcá-lo com o decorador @Override.");
                }

                // 4. Se sobreviveu à auditoria, adiciona finalmente o método à classe!
                baseModel.addMethod(method);
            }
            System.out.println("[XPL Genéricos] -> Acoplando Comportamento ao Blueprint: " + baseName + "<...>");
            return null; // <-- Corta aqui! O blueprint fica completo na câmara criogénica.
        }

        // =====================================================================
        // ⭐ ROTA B: IMPLEMENTAÇÃO DE CLASSE CONCRETA NORMAL
        // =====================================================================
        XPLModel activeModel; // O modelo que vamos validar, registar e instanciar

        // ⭐ 2. A BIFURCAÇÃO (BASE vs VARIANTE) ⭐
        if (stmt.aliasName == null) {
            // ---> É UMA IMPLEMENTAÇÃO DE BASE! <---
            baseModel.hasBaseImplementation = true;

            if (stmt.isAbstract) {
                baseModel.isAbstract = true; // Carimba o modelo na RAM como Abstrato!
            }
            activeModel = baseModel;
            System.out.println("[XPL Engine] -> Injetando Comportamento (Base): " + activeModel.name);

        } else {
            // ---> É UMA VARIANTE! (Ex: implement Mamifero as Mam1) <---
            String variantName = stmt.aliasName.lexeme;

            // Criamos uma ramificação limpa
            activeModel = new XPLModel(variantName, baseModel.superclass);
            baseModel.variantAliases.add(activeModel.name);
            activeModel.hasBaseImplementation = true;

            if (stmt.isAbstract) {
                activeModel.isAbstract = true;
            }

            // Copia a memória (fields) do modelo base para a variante
            activeModel.fields.putAll(baseModel.fields);

            // Regista a nova variante no Registry Interno, sem apagar a Base!
            registry_model.put(variantName, activeModel);
            System.out.println("[XPL Engine] -> Injetando Comportamento (Variante): " + variantName + " (Base: " + baseName + ")");
        }

        // =====================================================================
        // ⭐ A ALFÂNDEGA DE MÉTODOS E AUDITORIA DO @Override (UNIFICADA) ⭐
        // =====================================================================
        for (Stmt.Function method : stmt.methods) {
            boolean hasOverrideDecorator = false;

            // 1. Vasculha os decoradores à procura do @Override e dos Hooks
            if (method.decorators != null) {
                for (Stmt.DecoratorNode dec : method.decorators) {
                    String decName = dec.name.lexeme;

                    if (decName.equals("Override")) {
                        hasOverrideDecorator = true; // Detetou o decorador!
                    }
                    // Os teus hooks originais de Metaprogramação
                    else if (decName.contains("Init")) { activeModel.metaInitHook = method.name.lexeme; }
                    else if (decName.contains("Get")) { activeModel.metaGetHook = method.name.lexeme; }
                    else if (decName.contains("Set")) { activeModel.metaSetHook = method.name.lexeme; }
                    else if (decName.contains("End") || decName.contains("Morrer")) { activeModel.metaEndHook = method.name.lexeme; }
                }
            }

            // ⭐ 2. VERIFICAÇÃO DE ADN (SUPERCLASSE)
            boolean overridesSuper = (activeModel.superclass != null && activeModel.superclass.findMethod(method.name.lexeme) != null);

            // ⭐ 3. VERIFICAÇÃO DE CONTRATOS (INTERFACES)
            boolean fulfillsInterface = false;
            if (stmt.interfaces != null) {
                for (Token interfaceToken : stmt.interfaces) {
                    XplInterface contract = registry_Interfaces.get(interfaceToken.lexeme);
                    if (contract != null && contract.requiredMethods.containsKey(method.name.lexeme)) {
                        fulfillsInterface = true;
                        break;
                    }
                }
            }

            // =====================================================================
            // ⭐ A SENTENÇA DO @Override (Fail-Fast)
            // =====================================================================
            if (hasOverrideDecorator && !overridesSuper && !fulfillsInterface) {
                throw new ControlFlow.RuntimeError(method.name,
                        "Erro de Sobreposição: O método '" + method.name.lexeme + "()' está marcado com @Override, mas não está a sobrepor nenhum método da superclasse nem a cumprir nenhum contrato de interface.");
            }

            if (!hasOverrideDecorator && (overridesSuper || fulfillsInterface)) {
                throw new ControlFlow.RuntimeError(method.name,
                        "Decorador Ausente: O método '" + method.name.lexeme + "()' está a cumprir um contrato (Interface) ou a sobrepor um método pai. É obrigatório marcá-lo com o decorador @Override.");
            }

            // 4. Se sobreviveu à auditoria, adiciona finalmente o método ao modelo ativo!
            activeModel.addMethod(method);
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

        // ⭐ 3. A GUILHOTINA: VALIDAÇÃO DE CONTRATOS E REGISTO (TYPE CHECKING) ⭐
        for (Token interfaceToken : stmt.interfaces) {
            String interfaceName = interfaceToken.lexeme;
            XplInterface contract = registry_Interfaces.get(interfaceName);

            if (contract == null) {
                throw new ControlFlow.RuntimeError(interfaceToken, "Erro de Linkage: A interface '" + interfaceName + "' não foi encontrada no Registry.");
            }

            // O motor cruza a lista do contrato com os métodos do activeModel (Usando Busca Genética!)
            for (String requiredMethod : contract.requiredMethods.keySet()) {
                if (activeModel.findMethod(requiredMethod) == null) {
                    throw new ControlFlow.RuntimeError(stmt.targetName,
                            "Quebra de Contrato Fatal: O modelo '" + activeModel.name + "' não implementou o método obrigatório '" + requiredMethod + "()' exigido pela interface '" + interfaceName + "'.");
                }
            }

            // ⭐ A PEÇA QUE FALTAVA (O COLECIONADOR DE CONTRATOS) ⭐
            // Depois de validar que a classe cumpriu todas as regras,
            // guarda o nome da interface na "mochila" do modelo ativo para o operador 'use' a encontrar!
            activeModel.implementedInterfaces.add(interfaceName);
        }

        // ⭐ 4. Instancia a classe e regista-a no escopo do Ficheiro Atual ⭐
        if (!activeModel.isDecorator) {
            XplClass executableClass = new XplClass(activeModel, this.environment);

            if (stmt.aliasName == null) {
                // ---> A FUSÃO QUÂNTICA <---
                // O 'declare' já tinha reservado o nome como constante na RAM.
                // Em vez de criarmos uma constante nova (o que dá erro), fazemos o UPGRADE
                // do modelo nu (XPLModel) para a classe armada (XplClass) forçando no mapa!
                this.environment.values.put(activeModel.name, executableClass);
            } else {
                // ---> É UMA VARIANTE (Ex: as Circe) <---
                // Como a variante tem um nome novo que nunca foi declarado,
                // usamos a via oficial para a registar como uma nova constante intocável!
                this.environment.defineConst(activeModel.name, executableClass);
            }
        }
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

        if (typeAliases.containsKey(aliasName) || registry_model.containsKey(aliasName)) {
            throw new ControlFlow.RuntimeError(stmt.name, "Erro de Semântica: O identificador '" + aliasName + "' já designa um tipo existente.");
        }
        if (detectCircularAlias(aliasName, stmt.targetType)) {
            throw new ControlFlow.RuntimeError(stmt.name, "Referência Circular Proibida: O alias '" + aliasName + "' aponta para si mesmo num ciclo infinito.");
        }

        typeAliases.put(aliasName, stmt.targetType);
        typeAliasMetadata.put(aliasName, stmt);

        // =====================================================================
        // ⭐ NOVA ERA: INSTANCIAÇÃO SINGLETON DOS LISTENERS DE TIPO ⭐
        // O Listener nasce UMA ÚNICA VEZ e protege todas as variáveis deste tipo!
        // =====================================================================
        if (stmt.listeners != null && !stmt.listeners.isEmpty()) {
            java.util.List<XplInstance> activeTypeListeners = new java.util.ArrayList<>();

            for (Stmt.DecoratorNode listenerNode : stmt.listeners) {
                String listenerName = listenerNode.name.lexeme;
                XPLModel listenerModel = registry_model.get(listenerName);

                if (listenerModel == null) throw new ControlFlow.RuntimeError(listenerNode.name, "O listener '" + listenerName + "' não foi encontrado.");

                XplClass listenerClass;
                try { listenerClass = (XplClass) environment.get(listenerName); }
                catch (Exception e) { listenerClass = new XplClass(listenerModel, this.globals); }

                XplInstance listenerInstance = new XplInstance(listenerClass);

                // ⭐ INJEÇÃO DE CONTEXTO: O Alvo é o próprio Nome do Tipo Blueprint (ex: 'Email')!
                listenerInstance.fields.put("targetName", aliasName);

                // ⭐ 1. O CONSTRUTOR (Alfândega de Argumentos)
                Stmt.Function construtor = listenerModel.findMethod("init");
                if (construtor != null) {
                    new XplFunction(construtor, listenerClass.closure, listenerModel).bind(listenerInstance).call(this, listenerNode.arguments);
                } else if (listenerNode.arguments != null && !listenerNode.arguments.isEmpty()) {
                    throw new ControlFlow.RuntimeError(listenerNode.name, "O listener '" + listenerName + "' recebeu argumentos, mas não possui construtor 'init'.");
                }

                // ⭐ 2. O GATILHO DE CICLO DE VIDA ÚNICO: @(Listen.Init)
                if (listenerModel.metaInitHook != null) {
                    Stmt.Function hookFunc = listenerModel.findMethod(listenerModel.metaInitHook);
                    if (hookFunc != null) {
                        new XplFunction(hookFunc, listenerClass.closure, listenerModel).bind(listenerInstance).call(this, java.util.Collections.emptyList());
                    }
                }

                activeTypeListeners.add(listenerInstance);
            }
            // Guarda os vigilantes VIVOS na Esquadra Global de Tipos!
            typeListenersRegistry.put(aliasName, activeTypeListeners);
        }

        return null;
    }
    // =========================================================================
    // ⭐ VISITAÇÃO DA DECLARAÇÃO DO DECORADOR / LISTENER (A alocação de RAM) ⭐
    // =========================================================================
    @Override
    public Void visitDecoratorDeclStmt(Stmt.DecoratorDecl stmt) {
        String decName = stmt.name.lexeme;

        XPLModel superPai = this.registry_model.get("DecoratorRoot");
        if (superPai == null) {
            superPai = new XPLModel("DecoratorRoot", null);
            superPai.isDecorator = true;
            this.registry_model.put("DecoratorRoot", superPai);
        }

        XPLModel modelo = this.registry_model.get(decName);
        if (modelo == null) {
            modelo = new XPLModel(decName, superPai);
            modelo.isDecorator = true;
            this.registry_model.put(decName, modelo);
        }

        // ⭐ LÊ OS DADOS (FIELDS) ⭐
        if (stmt.fields != null) {
            for (Stmt.FieldDecl campo : stmt.fields) {
                modelo.fields.put(campo.name.lexeme, campo);
            }
        }

        // ⭐ LÊ OS MÉTODOS E MAPEIA OS HOOKS LIVRES (A MAGIA!) ⭐
        if (stmt.methods != null) {
            for (Stmt.Function method : stmt.methods) {
                // Procura os metadados @(Listen.Set), @(Listen.Init)...
                if (method.decorators != null) {
                    for (Stmt.DecoratorNode dec : method.decorators) {
                        String hookName = dec.name.lexeme;
                        switch (hookName) {
                            case "Listen.Init" ->
                                    modelo.metaInitHook = method.name.lexeme; // Grava o nome que o Dev escolheu!
                            case "Listen.Set" -> modelo.metaSetHook = method.name.lexeme;
                            case "Listen.Get" -> modelo.metaGetHook = method.name.lexeme;
                            case "Listen.End" -> modelo.metaEndHook = method.name.lexeme;
                        }
                    }
                }
                modelo.addMethod(method); // Regista a função no modelo
            }
        }

        com.dic.xsuper.engine.poo.XplClass classeDecoradora = new com.dic.xsuper.engine.poo.XplClass(modelo, this.environment);
        this.environment.defineConst(decName, classeDecoradora);

        return null;
    }

    // =========================================================================
    // ⭐ MOTOR DE VALIDAÇÃO DE TIPOS REFINADOS (Stateful Cache-Based) ⭐
    // =========================================================================
    private void fireTypeListeners(String typeName, Object value, Token originToken) {

        // 1. Vai buscar os vigilantes vivos (Singletons) atados a este Tipo
        java.util.List<XplInstance> listeners = typeListenersRegistry.get(typeName);

        if (listeners != null) {
            for (XplInstance listenerInstance : listeners) {
                XPLModel listenerModel = listenerInstance.klass.model;

                // ⭐ INJEÇÃO DE CONTEXTO DINÂMICO (Atualiza a memória com o valor que está a entrar)
                listenerInstance.fields.put("target", value);

                // ⭐ 2. Dispara APENAS o Hook de Atribuição: @(Listen.Set)
                if (listenerModel.metaSetHook != null) {
                    Stmt.Function onAssignFunc = listenerModel.findMethod(listenerModel.metaSetHook);
                    if (onAssignFunc != null) {
                        XplFunction onAssignCallable = new XplFunction(onAssignFunc, listenerInstance.klass.closure, listenerModel);

                        java.util.List<Expr.CallArg> hookArgs = new java.util.ArrayList<>();
                        hookArgs.add(new Expr.CallArg(null, new Expr.Literal(value))); // O novo valor validado

                        try {
                            onAssignCallable.bind(listenerInstance).call(this, hookArgs);
                        } catch (ControlFlow.RuntimeError e) {
                            throw new ControlFlow.RuntimeError(originToken, "Violação de Tipo ('" + typeName + "'): " + e.getMessage());
                        }
                    }
                }
            }
        }

        // Recursividade: Se o alias apontar para outro alias (ex: type B = A)
        Stmt.TypeAliasDecl aliasNode = typeAliasMetadata.get(typeName);
        if (aliasNode != null && aliasNode.targetType instanceof TypeNode.Simple simpleType) {
            fireTypeListeners(simpleType.name.lexeme, value, originToken);
        }
    }

    @Override
    public Void visitExportDeclStmt(Stmt.ExportDecl stmt) {
        Token exportTokenBase = new Token(TokenType.IDENTIFIER, "export", null, 0, 0);

        if (this.moduleManager.currentCompilingModule == null) {
            throw new ControlFlow.RuntimeError(exportTokenBase, "Comando 'export' usado fora de um módulo!");
        }

        if (stmt.isExportAll) {
            this.moduleManager.currentCompilingModule.exportAll = true;
            return null;
        }

        if (stmt.declaration != null) {
            execute(stmt.declaration);
            String symbolName = null;
            Token symbolToken = exportTokenBase;
            if (stmt.declaration instanceof Stmt.DeclareDecl d) {
                symbolName = d.name.lexeme;
                symbolToken = d.name;
            } else if (stmt.declaration instanceof Stmt.Function f) {
                symbolName = f.name.lexeme;
                symbolToken = f.name;
            } else if (stmt.declaration instanceof Stmt.VarDecl v) {
                symbolName = v.name.lexeme;
                symbolToken = v.name;
            }

            if (symbolName != null) {
                Object valor = safeGetSymbol(symbolName);
                if (valor != null) {
                    this.moduleManager.currentCompilingModule.exports.put(symbolName, valor);
                } else {
                    throw new ControlFlow.RuntimeError(symbolToken, "Falha Crítica no Export: O símbolo '" + symbolName + "' não foi encontrado na RAM.");
                }
            }
        } else if (stmt.inlineSymbols != null) {
            for (Token sym : stmt.inlineSymbols) {
                Object valor = safeGetSymbol(sym.lexeme);
                if (valor != null) {
                    this.moduleManager.currentCompilingModule.exports.put(sym.lexeme, valor);
                } else {
                    throw new ControlFlow.RuntimeError(sym, "Falha Crítica no Export: O símbolo '" + sym.lexeme + "' não foi encontrado na RAM.");
                }
            }
        }
        return null;
    }

    @Override
    public Void visitGlobalDeclStmt(Stmt.GlobalDecl stmt) {
        if (stmt.name.lexeme.startsWith("$_")) {
            throw new ControlFlow.RuntimeError(stmt.name, "Erro de Sintaxe: O prefixo '$_' é estritamente reservado para variáveis globais nativas do ecossistema Super.");
        }

        Object value = evaluate(stmt.initializer);

        if (stmt.typeAnnotation != null && value != null) {
            if (!checkTypeMatch(value, stmt.typeAnnotation)) {
                throw new ControlFlow.RuntimeError(stmt.name, "Erro de Tipagem na variável global.");
            }
        }

        this.environment.defineConst(stmt.name.lexeme, value);

        if (this.moduleManager.currentCompilingModule == null) {
            this.globals.defineConst(stmt.name.lexeme, value);
        }
        return null;
    }

    @Override
    public Void visitDoWhileStmt(Stmt.DoWhile doWhile) {
        do {
            try {
                execute(doWhile.body);
            } catch (ControlFlow.BreakException e) {
                break; // Se o código fizer um 'break', sai do loop
            } catch (ControlFlow.ContinueException e) {
                // Se o código fizer um 'continue', salta a execução do bloco
                // e vai direto para a verificação da condição do while!
            }
        } while (isTruthy(evaluate(doWhile.condition))); // A magia do do-while no Java!

        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        // ⭐ O CICLO DE VIDA NATIVO DO WHILE LOOP ⭐
        // Avalia a condição e executa enquanto ela retornar verdadeiro (true)
        while (executeCondition(stmt.condition)) {
            try {
                execute(stmt.body);
            } catch (ControlFlow.BreakException b) {
                // 🛑 comando 'break': Interrompe o loop imediatamente!
                break;
            } catch (ControlFlow.ContinueException c) {
                // 🔄 comando 'continue': Salta o resto do bloco e vai para a próxima iteração!
                // Não faz nada, o ciclo while nativo do Java vai reavaliar a condição automaticamente.
            }
        }
        return null;
    }

    // Auxiliar seguro para garantir que a condição avaliada é um Boolean nativo do XPL
    private boolean executeCondition(Expr condition) {
        Object value = evaluate(condition);
        if (value instanceof Boolean b) {
            return b;
        }
        throw new ControlFlow.RuntimeError(null, "A condição do comando 'while' deve resultar num tipo bool.");
    }

    // ⭐ AUXILIAR: Procura a exportação em todos os cofres do motor
    private Object safeGetSymbol(String symbolName) {
        // 1. Tenta no Environment (Variáveis, Funções, Classes Implementadas)
        try { return this.environment.get(symbolName); } catch (RuntimeException ignored) {}

        // 2. Tenta no Cofre de Modelos (Declare)
        if (this.registry_model.containsKey(symbolName)) return this.registry_model.get(symbolName);

        // 3. Tenta no Cofre de Genéricos (declare Caixa<T>)
        if (this.registry_generic_models.containsKey(symbolName)) return this.registry_generic_models.get(symbolName);

        // 4. Tenta no Cofre de Interfaces
        if (this.registry_Interfaces.containsKey(symbolName)) return this.registry_Interfaces.get(symbolName);

        return null;
    }

    @Override
    public Void visitImportDeclStmt(Stmt.ImportDecl stmt) {
        Token importKeyword = new Token(TokenType.IMPORT, "import", null, 0, 0);

        // ⭐ DELEGA PARA O MANAGER E INJETA O INTERPRETADOR (this)
        Environment globalsChain = resolveGlobalsChain(stmt.modulePath);
        com.dic.xsuper.engine.modules.XplModuleManager.XplModule module =
                this.moduleManager.loadModule(stmt.modulePath, importKeyword, this, globalsChain);

        // LÓGICA DO PREFIXO
        String sufixoPrefixo = "";
        if (stmt.prefix != null) {
            sufixoPrefixo = stmt.prefix.lexeme.replace("\"", "") + "_";
        }

        if (stmt.isWildcard) {
            for (java.util.Map.Entry<String, Object> entry : module.exports.entrySet()) {
                String nomeFinal = sufixoPrefixo + entry.getKey();
                injectImportedSymbol(nomeFinal, entry.getValue());
            }
        } else {
            for (Stmt.ImportSymbol sym : stmt.symbols) {
                String targetName = sym.originalName.lexeme;
                if (!module.exports.containsKey(targetName)) {
                    throw new ControlFlow.RuntimeError(sym.originalName,
                            "O módulo '" + stmt.modulePath + "' não exporta o símbolo '" + targetName + "'.");
                }
                Object importedValue = module.exports.get(targetName);
                String baseLocalName = (sym.aliasName != null) ? sym.aliasName.lexeme : targetName;
                String nomeFinal = sufixoPrefixo + baseLocalName;
                injectImportedSymbol(nomeFinal, importedValue);
            }
        }
        return null;
    }

    private void injectImportedSymbol(String localName, Object importedValue) {
        switch (importedValue) {
            case XplClass xplClass -> {
                this.registry_model.put(localName, xplClass.model);
                this.environment.defineImported(localName, xplClass); // ⭐ MUDOU AQUI
            }
            case XPLModel model -> {
                if (model.isGenericBlueprint) {
                    this.registry_generic_models.put(localName, model);
                    this.environment.defineImported(localName, model); // ⭐ MUDOU AQUI
                } else {
                    this.registry_model.put(localName, model);
                    if (model.hasBaseImplementation && !model.isDecorator) {
                        this.environment.defineImported(localName, new XplClass(model, this.globals)); // ⭐ MUDOU AQUI
                    } else {
                        this.environment.defineImported(localName, model); // ⭐ MUDOU AQUI
                    }
                }
            }
            case XplInterface iface -> this.registry_Interfaces.put(localName, iface);
            case null, default -> this.environment.defineImported(localName, importedValue); // ⭐ MUDOU AQUI
        }
    }


    // ⭐ NOVO: Resolve a hierarquia cascata de escopos globais do projeto/módulo
    private Environment resolveGlobalsChain(String modulePath) {
        Environment currentParent = this.globals;
        boolean isGlobalsFile = modulePath.equals("globals") || modulePath.endsWith(".globals");

        if (!isGlobalsFile) {
            // 1. Busca primeiro o globals do pacote específico (escopo mais próximo)
            if (modulePath.contains(".")) {
                int lastDot = modulePath.lastIndexOf('.');
                String packagePath = modulePath.substring(0, lastDot);
                String pkgGlobals1 = packagePath + ".globals";
                if (this.moduleManager.resolvePhysicalFile(pkgGlobals1.replace(".", "/") + ".xpl") != null) {
                    return this.moduleManager.loadModule(pkgGlobals1, new Token(TokenType.IDENTIFIER, "globals", null, 0, 0), this, this.globals).localEnvironment;
                }
            }
            // 2. Fallback para o globals raiz do projeto geral
            if (this.moduleManager.resolvePhysicalFile("globals.xpl") != null) {
                return this.moduleManager.loadModule("globals", new Token(TokenType.IDENTIFIER, "globals", null, 0, 0), this, this.globals).localEnvironment;
            }
        } else {
            // Se for um globals de pacote, ele herda do globals raiz da aplicação se existir
            if (modulePath.contains(".") && !modulePath.equals("globals")) {
                if (this.moduleManager.resolvePhysicalFile("globals.xpl") != null) {
                    return this.moduleManager.loadModule("globals", new Token(TokenType.IDENTIFIER, "globals", null, 0, 0), this, this.globals).localEnvironment;
                }
            }
        }
        return currentParent;
    }

    private java.io.File resolvePhysicalFile(String relativePath) {
        relativePath = relativePath.replace("\\", "/");
        String[] searchPaths = {".", "src", "lib"};
        for (String base : searchPaths) {
            java.io.File f = new java.io.File(base, relativePath);
            if (f.exists() && f.isFile()) return f;
        }
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
        if (expectedType == TokenType.T_BOOL && value instanceof Boolean) return true;
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

    // =========================================================================
    // ⭐ MOTOR DE TRANSMUTAÇÃO DE AST (O Bisturi Quântico de C++/Rust) ⭐
    // =========================================================================

    // Helper quântico: Converte uma string crua no TokenType oficial da tua linguagem!
    private TokenType getPrimitiveTokenType(String typeName) {
        return switch (typeName.toLowerCase()) {
            case "int", "long", "short", "byte" -> TokenType.T_INT;
            case "float", "double" -> TokenType.T_FLOAT;
            case "string", "char" -> TokenType.T_STRING;
            case "bool", "boolean" -> TokenType.T_BOOL;
            case "array", "list" -> TokenType.T_ARRAY;
            case "object", "map", "dict" -> TokenType.T_OBJECT;
            default -> TokenType.IDENTIFIER; // Se for uma classe POO (Ex: Pessoa)
        };
    }

    private TypeNode transmuteType(TypeNode node, Map<String, String> dict) {
        if (node == null) return null;

        if (node instanceof TypeNode.Simple simple) {
            String lex = simple.name.lexeme;
            if (dict.containsKey(lex)) {
                String concreteName = dict.get(lex);

                // ⭐ A CURA: Em vez de fixar IDENTIFIER, detetamos o tipo biológico real!
                TokenType realTokenType = getPrimitiveTokenType(concreteName);

                Token concreteToken = new Token(realTokenType, concreteName, null, simple.name.line, simple.name.column);
                return new TypeNode.Simple(concreteToken);
            }
        }
        else if (node instanceof TypeNode.Generic gen) {
            List<TypeNode> newArgs = new ArrayList<>();
            for (TypeNode arg : gen.typeArguments) newArgs.add(transmuteType(arg, dict));
            return new TypeNode.Generic(gen.name, newArgs);
        }
        else if (node instanceof TypeNode.Optional opt) {
            return new TypeNode.Optional(transmuteType(opt.innerType, dict));
        }

        return node;
    }

    // 2. Transmuta a assinatura inteira de uma Função!
    private Stmt.Function transmuteMethodSignature(Stmt.Function oldFunc, Map<String, String> dict) {
        // A) Transmuta a lista de parâmetros: (a: U, b: T) vira (a: string, b: int)
        List<Stmt.Param> newParams = new ArrayList<>();
        for (Stmt.Param oldParam : oldFunc.params) {
            TypeNode mutatedType = transmuteType(oldParam.typeNode, dict);
            newParams.add(new Stmt.Param(oldParam.name, mutatedType, oldParam.defaultValue,false));
        }

        // B) Transmuta o tipo de retorno: : V vira : object
        TypeNode newReturn = transmuteType(oldFunc.returnType, dict);

        // C) Devolve um nó Stmt.Function novinho em folha, puramente concreto!
        return new Stmt.Function(
                oldFunc.accessModifier,
                oldFunc.isStatic,
                oldFunc.isAbstract,
                oldFunc.name,
                newParams, // <-- Injetados os parâmetros transmutados!
                newReturn, // <-- Injetado o retorno transmutado!
                oldFunc.thrownExceptions,
                oldFunc.body, // O corpo desce igual
                oldFunc.decorators, // (Mantemos a mochila de decoradores que criámos ontem!)
                oldFunc.listeners
        );
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
        List<Object> elements = new ArrayList<>();

        for (Expr elementExpr : expr.elements) {
            // ⭐ INTERCETOR DE SPREAD!
            if (elementExpr instanceof Expr.Spread spread) {
                Object iterable = evaluate(spread.expression);

                if (iterable instanceof List<?> list) {
                    elements.addAll(list); // Despeja o array inteiro aqui dentro!
                } else {
                    throw new ControlFlow.RuntimeError(spread.operator,
                            "O operador spread '...' em Arrays exige uma Lista. Tipo recebido: " +
                                    (iterable != null ? iterable.getClass().getSimpleName() : "null"));
                }
            } else {
                elements.add(evaluate(elementExpr));
            }
        }
        return elements;
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
        Object newValue;
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
        // ---------------------------------------------------------------------
        // ⭐ INTERCEÇÃO: RESOLUÇÃO DE CAMINHOS ABSOLUTOS DE MÓDULOS (Java Style)
        // ---------------------------------------------------------------------
        Object object;
        try {
            object = evaluate(expr.object);
        } catch (RuntimeException e) {
            // Se falhou a avaliar o objeto da esquerda, pode ser uma cadeia de pacotes (ex: com.dic.ui)
            String absoluteModulePath = rebuildAbsoluteModulePath(expr.object);

            throw e; // Se não era um módulo válido na cache, mantém o erro original!
        }

        // Compatibilidade com o nosso Enum
        if (object instanceof XplEnum e) {
            if (expr.name.lexeme.equals("has")) {
                // Retornas uma função nativa Callable que faz return e.has(args[0])
            }
            return e.get(expr.name.lexeme);
        }

        // Compatibilidade com as propriedades das variantes: estado.codigo
        if (object instanceof XplEnumVariant variant) {
            return variant.getProperty(expr.name.lexeme);
        }

        // ---------------------------------------------------------------------

        // =========================================================================
        // ⭐ A PONTE DINÂMICA NATIVA (A MÁGICA DOS MÉTODOS) ⭐
        // =========================================================================
        if (object instanceof XplNativeObject nativeObj) {

            // 1. Tenta ler uma propriedade direta (ex: document.body)
            Object propValue = nativeObj.getProperty(expr.name.lexeme);
            if (propValue != null) {
                return propValue;
            }

        }

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
        if (object instanceof XplInstance instance) {

            // =====================================================================
            // ⭐ A NOVA INTERCEÇÃO REATIVA (GET) ⭐
            // =====================================================================
            if (instance.fields.containsKey("__active_listeners__")) {
                @SuppressWarnings("unchecked")
                List<XplInstance> listeners = (List<XplInstance>) instance.fields.get("__active_listeners__");
                for (XplInstance dec : listeners) {
                    if (dec.klass.model.metaGetHook != null) {
                        Stmt.Function hookFunc = dec.klass.model.findMethod(dec.klass.model.metaGetHook);
                        if (hookFunc != null) {
                            new XplFunction(hookFunc, dec.klass.closure, dec.klass.model).bind(dec).call(this, java.util.Collections.emptyList());
                        }
                    }
                }
            }

            // 🚀0. INJEÇÃO NATIVA: O Método toObject() 🚀
            if (expr.name.lexeme.equals("toObject")) {
                // Devolve uma função nativa anónima para ser executada ()
                return new XplCallable() { // Usa a tua interface de funções nativas!
                    @Override
                    public int arity() { return 0; }

                    @Override
                    public Object call(Interpreter interpreter, java.util.List<Expr.CallArg> args) {
                        // Copia todas as propriedades vivas e CONGELA-AS num mapa imutável!
                        java.util.Map<String, Object> snapshot = new java.util.HashMap<>(instance.fields);
                        return java.util.Collections.unmodifiableMap(snapshot);
                    }
                };
            }

            // 1. É uma variável/propriedade ou MetaBuilder JIT?
            if (instance.fields.containsKey(expr.name.lexeme)) {

                // ⭐ A VACINA 1: Só verifica regras de acesso se NÃO for um MetaBuilder (klass != null)
                if (instance.klass != null) {
                    Stmt.FieldDecl field = instance.klass.model.fields.get(expr.name.lexeme);
                    if (field != null) {
                        checkAccess(expr.name, field, instance.klass.model, false);
                    }
                }

                // ⭐ A CURA DO 'THIS' FANTASMA ⭐
                // Se a propriedade da memória for uma Função (injetada dinamicamente),
                // ligamos (bind) o 'this' à instância atual antes de a entregarmos!
                Object val = instance.fields.get(expr.name.lexeme);
                if (val instanceof XplFunction func) {
                    return func.bind(instance);
                }

                return val;
            }

            // 2. É um Comportamento/Método normal?
            if (instance.klass != null) {
                Stmt.Function method = instance.klass.model.findMethod(expr.name.lexeme);
                if (method != null) {
                    return instance.get(expr.name);
                }
                throw new ControlFlow.RuntimeError(expr.name, "A propriedade ou método '" + expr.name.lexeme + "' não existe na instância de " + instance.klass.model.name + ".");
            }

            // ⭐ Se o código chegar aqui, significa que o motor tentou aceder a uma propriedade
            // (ex: .ElseIf, .execute) num MetaBuilder e falhou! Isto vai imprimir exatamente o que falhou!
            throw new ControlFlow.RuntimeError(expr.name, "A propriedade '" + expr.name.lexeme + "' não existe nesta MetaInstance nativa.");
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


        System.out.println(expr);
        throw new ControlFlow.RuntimeError(expr.name, "Apenas Arrays, Objetos, Strings, Instâncias e Classes possuem propriedades/métodos.");

    }

    // Transmuta uma árvore de Expr.Get aninhada numa String limpa "com.dic.ui"
    private String rebuildAbsoluteModulePath(Expr expr) {
        if (expr instanceof Expr.Variable v) {
            return v.name.lexeme;
        } else if (expr instanceof Expr.Get g) {
            String parentPath = rebuildAbsoluteModulePath(g.object);
            if (parentPath == null) return null;
            return parentPath + "." + g.name.lexeme;
        }
        return null;
    }

    @Override
    public Object visitArrowFunctionExpr(Expr.ArrowFunction expr) {
        Environment closure = this.environment;

        // 1. Criar a lista de parâmetros
        java.util.List<Stmt.Param> params = new java.util.ArrayList<>();
        TypeNode anyType = new TypeNode.Simple(new Token(TokenType.IDENTIFIER, "any", null, 0, 0));

        if (expr.parameters != null) {
            for (Stmt.Param p : expr.parameters) {
                TypeNode resolvedType = (p.typeNode != null) ? p.typeNode : anyType;
                params.add(new Stmt.Param(p.name, resolvedType, p.defaultValue,false));
            }
        }

        // 2. Construir o corpo
        java.util.List<Stmt> bodyStmts = new java.util.ArrayList<>();

        // Assumimos SEMPRE 'any' por padrão para não colidir com o Type Checker rigoroso
        TypeNode inferredReturnType = (expr.returnType != null) ? expr.returnType : anyType;

        if (expr.body instanceof Expr.Block blockExpr) {
            // Copia todo o código escrito pelo utilizador
            bodyStmts.addAll(blockExpr.statements);

            // ⭐ A JOGADA DE MESTRE: Injetamos um 'return null;' silencioso no final! ⭐
            // Assim o Interpretador nunca reclama de "Falta de Retorno" e o 'null' encaixa no 'any'.
            bodyStmts.add(new Stmt.Return(
                    new Token(TokenType.RETURN, "return", null, 0, 0),
                    new Expr.Literal(null)
            ));
        } else {
            // Expressões diretas (ex: a => a * 2) já ganham o seu return nativo
            bodyStmts.add(new Stmt.Return(
                    new Token(TokenType.RETURN, "return", null, 0, 0),
                    expr.body
            ));
        }

        // 3. Criar um nome sintético único
        Token syntheticName = new Token(
                TokenType.IDENTIFIER,
                "_arrow_" + System.identityHashCode(expr),
                null, 0, 0
        );

        // 4. Construir a declaração da função final
        Stmt.Function funcDecl = new Stmt.Function(
                null,
                false,
                false,
                syntheticName,
                params,
                inferredReturnType,
                java.util.Collections.emptyList(),
                bodyStmts,
                java.util.Collections.emptyList(),
                java.util.Collections.emptyList()
        );

        return new XplFunction(funcDecl, closure, null);
    }
    @Override
    public Object visitObjectLiteralExpr(Expr.ObjectLiteral expr) {
        Map<String, Object> map = new LinkedHashMap<>();

        for (int i = 0; i < expr.keys.size(); i++) {
            Expr keyExpr = expr.keys.get(i);
            Expr valueExpr = expr.values.get(i);

            // ⭐ INTERCETOR DE SPREAD! (Lembra-te: no Parser, se for spread, passámos 'key' como null)
            if (keyExpr == null && valueExpr instanceof Expr.Spread spread) {
                Object spreadObj = evaluate(spread.expression);

                if (spreadObj instanceof Map<?, ?> spreadMap) {
                    for (Map.Entry<?, ?> entry : spreadMap.entrySet()) {
                        map.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                } else {
                    throw new ControlFlow.RuntimeError(spread.operator,
                            "O operador spread em Objetos requer outro Objeto/Dicionário.");
                }
            } else {
                // Comportamento Normal de chave-valor
                Object key = evaluate(keyExpr);
                Object value = evaluate(valueExpr);
                map.put(String.valueOf(key), value);
            }
        }
        return map;
    }

    @Override
    public Object visitNewExpr(Expr.New expr) {
        String modelName = expr.className.lexeme;
        XPLModel model = registry_model.get(modelName);

        // =====================================================================
        // ⭐ DESVIO QUÂNTICO: É uma invocação Genérica? (Ex: new Caixa<int>())
        // =====================================================================
        if (expr.typeArguments != null && !expr.typeArguments.isEmpty()) {
            model = resolveMonomorphizedModel(expr);
        } else {
            model = registry_model.get(modelName);
        }

        // 1. Verifica se o modelo sequer existe
        if (model == null) {
            // Trava de Proteção: Se o utilizador tentou instanciar 'new Caixa()' sem os < >, avisa-o!
            if (registry_generic_models.containsKey(modelName)) {
                throw new ControlFlow.RuntimeError(expr.className,
                        "Erro de Tipagem: '" + modelName + "' é um Template Genérico. Deves instanciá-lo declarando os seus tipos concretos (Ex: new " + modelName + "<int>()).");
            }
            throw new ControlFlow.RuntimeError(expr.className,
                    "Erro: O modelo '" + modelName + "' não foi declarado.");
        }

        // 2. Verificamos as travas estruturais de base
        if (!model.hasBaseImplementation) {
            throw new ControlFlow.RuntimeError(expr.className,
                    "ERRO FATAL: O modelo '" + modelName + "' não possui uma implementação base.");
        }

        if (model.isAbstract) {
            throw new ControlFlow.RuntimeError(expr.className,
                    "ERRO FATAL: Operação Ilegal. O modelo '" + modelName + "' possui uma implementação abstrata e não pode ser instanciado diretamente.");
        }

        // =====================================================================
        // ⭐ A SENTINELA: A GUILHOTINA DE CONSTRUTORES FANTASMAS ⭐
        // =====================================================================
        // O modelo tem um método chamado 'init'?
        // Se não tiver, o utilizador NÃO PODE passar argumentos!
        Stmt.Function initMethod = model.findMethod("init");

        if (initMethod == null && !expr.arguments.isEmpty()) {
            throw new ControlFlow.RuntimeError(expr.className,
                    "Quebra de Contrato: O modelo '" + modelName + "' não possui um método construtor 'init(...)', mas forneceste " + expr.arguments.size() + " argumento(s) na instanciação.");
        }
        // =====================================================================

        XplClass klass = new XplClass(model, environment);

        // Agora, klass.call() só será chamado se:
        // a) O método init existir; OU
        // b) O método init NÃO existir E o programador não passou argumentos.
        return klass.call(this, expr.arguments);
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

            // ⭐ A NOVA INTERCEÇÃO REATIVA (SET) ⭐
            // =====================================================================
            if (instance.fields.containsKey("__active_listeners__")) {
                @SuppressWarnings("unchecked")
                List<XplInstance> listeners = (List<XplInstance>) instance.fields.get("__active_listeners__");
                for (XplInstance dec : listeners) {
                    if (dec.klass.model.metaSetHook != null) {
                        Stmt.Function hookFunc = dec.klass.model.findMethod(dec.klass.model.metaSetHook);
                        if (hookFunc != null) {
                            XplFunction hookCallable = new XplFunction(hookFunc, dec.klass.closure, dec.klass.model);
                            java.util.List<Expr.CallArg> hookArgs = new java.util.ArrayList<>();
                            hookArgs.add(new Expr.CallArg(null, new Expr.Literal(value))); // Envia o novo valor!
                            hookCallable.bind(dec).call(this, hookArgs);
                        }
                    }
                }
            }
            // ⭐ A VACINA DO JIT: Escrita direta para MetaBuilders (sem classe) ⭐
            if (instance.klass == null) {
                instance.fields.put(expr.name.lexeme, value);
                return value;
            }

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
            Environment classClosure = ((XplInstance) currentInstance).klass.closure;
            XplFunction function = new XplFunction(method, classClosure, owner);

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
                        case Long ignored -> {
                            return value;
                        }
                        case String string -> {
                            return Long.parseLong(string);
                        }
                        default -> {}
                    }
                    break;
                case T_FLOAT:
                    switch (value) {
                        case Long l -> {
                            return l.doubleValue();
                        }
                        case Double ignored -> {
                            return value;
                        }
                        case String string -> {
                            return Double.parseDouble(string);
                        }
                        default -> {}
                    }
                    break;
                case T_STRING:
                    return stringify(value);
                case IDENTIFIER:
                    if (value instanceof XplInstance instance) {
                        // ⭐ VERIFICAÇÃO ADICIONADA
                        if (instance.klass != null && instance.klass.model.isSubclassOf(expr.type.name.lexeme)) {
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

        // Proteção global: Se a variável estiver vazia, não é instância, nem tipo, nem usa interface!
        if (left == null) return false;

        // =================================================================
        // 1. INSTANCE: Exatidão Pura (Tem de ser exatamente a mesma classe)
        // =================================================================
        if (expr.operator.type == TokenType.INSTANCE) {
            if (left instanceof XplInstance inst) {
                if (inst.klass == null) return false;
                return inst.klass.model.name.equals(targetType);
            }
            return false;
        }
        // =================================================================
        // 2. USE: Contratos (Implementa esta Interface?)
        // =================================================================
        else if (expr.operator.type == TokenType.USE) {
            if (left instanceof XplInstance inst) {
                if (inst.klass == null) return false;
                // Chama o método implementsInterface que adicionámos ao XPLModel!
                return inst.klass.model.implementsInterface(targetType);
            }
            return false;
        }
        // =================================================================
        // 3. TYPE: ADN Flexível (Primitivos e Herança de Classes)
        // =================================================================
        else if (expr.operator.type == TokenType.TYPE) {
            if (targetType.equals("int") && left instanceof Long) return true;
            if (targetType.equals("float") && (left instanceof Double || left instanceof Long)) return true;

            return switch (left) {
                case String string when targetType.equals("string") -> true;
                case Boolean b when targetType.equals("bool") -> true;
                case List list when targetType.equals("array") -> true;
                case XplInstance xplInstance -> {
                    if (xplInstance.klass == null) yield false;

                    // ⭐ Verifica se é a própria classe OU uma classe filha
                    if (xplInstance.klass.model.name.equals(targetType)) yield true;
                    yield xplInstance.klass.model.isSubclassOf(targetType);
                }
                default -> false;
            };
        }

        // Fallback de segurança
        return false;
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
            case XplInstance xplInstance -> {
                // ⭐ VERIFICAÇÃO ADICIONADA
                if (xplInstance.klass == null) yield "MetaInstance";
                yield xplInstance.klass.model.name;
            }
            case XplClass ignored -> "class";
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

        // 3. Se o curto-circuito não foi activado (ex: 'falso || X' ou 'verdadeiro && X'),
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
    // 3. ENCADEAMENTO DE PROPRIEDADE ( obj?.prop ) - BILINGUE ⭐
    // =========================================================================
    @Override
    public Object visitOptionalChainingExpr(Expr.OptionalChaining expr) {
        Object leftObject = evaluate(expr.object);

        // 1. Se o elo anterior é estritamente nulo, a corrente dissipa-se em null
        switch (leftObject) {
            case null -> {
                return null;
            }

            // ROTA A: É uma instância de um 'declare' do utilizador?
            case XplInstance xplInstance -> {
                return xplInstance.get(expr.name);
            }

            // ⭐ ROTA B: É um Mapa / Dicionário Literal? (A salvação da Linha 36!)
            case Map<?, ?> mapa -> {
                String chave = expr.name.lexeme;

                // Em JS/TypeScript, fazer 'mapa?.chaveInexistente' devolve null em vez de dar erro.
                return mapa.getOrDefault(chave, null);
            }
            default -> {
            }
        }

        throw new ControlFlow.RuntimeError(expr.name,
                "Operação '?.' inválida: O alvo (do tipo " + leftObject.getClass().getSimpleName() + ") não possui propriedades acessíveis.");
    }

    // =========================================================================
    // 4. CHAMADA OPCIONAL DE MÉTODO ( obj?.metodo() ) - LAZY BINDING ⭐
    // =========================================================================
    @Override
    public Object visitOptionalCallExpr(Expr.OptionalCall expr) {
        Object leftObject = evaluate(expr.object);

        if (leftObject == null) return null;

        Object metodoInvocavel = null;

        if (leftObject instanceof XplInstance instance) {
            metodoInvocavel = instance.get(expr.methodName);
        }
        else if (leftObject instanceof java.util.Map<?, ?> mapa) {
            metodoInvocavel = mapa.get(expr.methodName.lexeme);
        }

        if (metodoInvocavel instanceof XplCallable callable) {
            // ⭐ PASSE DIRETO PURO: Entregamos a fila de CallArgs crua ao callee!
            return callable.call(this, expr.arguments);
        }

        throw new ControlFlow.RuntimeError(expr.methodName,
                "O método opcional '?." + expr.methodName.lexeme + "()' não existe ou não é invocável no objeto alvo.");
    }

    @Override
    public Object visitMetaAccessExpr(Expr.MetaAccess expr) {
        // 1. Avalia o objeto base para descobrir quem ele é na RAM
        Object target = evaluate(expr.object);

        // 2. Delega o trabalho pesado para o Motor de Reflexão Isolado!
        return MetaReflectionEngine.createMetaCallable(target, expr);
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        // Avalia a condição primeiro
        Object condition = evaluate(expr.condition);

        // Usa a tua função auxiliar isTruthy() para decidir o caminho
        if (isTruthy(condition)) {
            return evaluate(expr.trueBranch);
        } else {
            return evaluate(expr.falseBranch);
        }
    }

    @Override
    public Object visitBlockExpr(Expr.Block expr) {
        return evaluateBranchAsExpression(new Stmt.Block(expr.statements));
    }

    @Override
    public Object visitSpreadExpr(Expr.Spread expr) {
        throw new ControlFlow.RuntimeError(expr.operator,
                "O operador spread '...' só pode ser usado dentro de Arrays, Objetos ou Chamadas de Funções.");
    }

    // =========================================================================
    // O DETECTOR DE METADADOS (Verifica se um Objecto Java pertence a um TypeNode)
    // =========================================================================
    public boolean checkTypeMatch(Object obj, TypeNode typeNode) {

        // ⭐ 0. O PRISMA DO VOLUME 20: Dissolve qualquer Alias no seu tipo concreto! ⭐
        typeNode = resolveConcreteType(typeNode);

        // O vácuo quântico: se a ranhura não tem tipo (ex: fun teste(x)), passa tudo!
        if (typeNode == null) return true;

        // ⭐ 1. A REGRA DE OURO DO VOLUME 21 ⭐
        if (typeNode instanceof TypeNode.Optional opt) {
            if (obj == null) return true;
            return checkTypeMatch(obj, opt.innerType); // Desempacota e re-testa
        }

        // =========================================================================
        // ⭐ AUDITORIA DE PRIMEIRA CLASSE: FUNÇÕES COMO ARGUMENTO ⭐
        // =========================================================================
        if (typeNode instanceof TypeNode.FunctionType fnType) {

            // 1. O que tentaram enfiar nesta ranhura é sequer invocável?
            if (!(obj instanceof XplCallable callable)) {
                return false;
            }

            // 2. A Aridade (quantidade de parâmetros) bate certo?
            // Se a ranhura pede (int, string) -> bool, a função fornecida TEM de exigir 2 parâmetros!
            if (callable.arity() != fnType.paramTypes.size()) {
                return false;
            }

            // 3. Validação de Assinatura Profunda (Se for uma função nativa do teu XPL)
            if (obj instanceof XplFunction xplFunc) {
                Stmt.Function declaracao = xplFunc.declaration;

                // Verifica o tipo de retorno
                if (fnType.returnType != null && declaracao.returnType != null) {
                    // (Opcional: Podes chamar uma função auxiliar estática para comparar TypeNodes)
                    if (!fnType.returnType.name.lexeme.equals(declaracao.returnType.name.lexeme)) {
                        return false;
                    }
                }
            }

            // Se é invocável e tem o número certo de argumentos, a Alfândega aprova!
            return true;
        }

        // Para todos os tipos normais (não-opcionais), o null é estritamente PROIBIDO!
        if (obj == null) return typeNode.name.lexeme.equals("any");

        // ⭐ CORRECÇÃO VITAL: Extrai o Token tanto de Simple (int) quanto de Generic (Caixa<T>)!
        Token typeToken = typeNode.name;

        switch (typeToken.type) {
            case T_INT:
                // Promove e engole os 4 tamanhos de inteiros do silício!
                return obj instanceof Long || obj instanceof Integer ||
                        obj instanceof Short || obj instanceof Byte;

            case T_FLOAT:
                // Engole decimais E TAMBÉM aceita promover um inteiro a float (ex: float x = 5)
                return obj instanceof Double || obj instanceof Float ||
                        obj instanceof Long || obj instanceof Integer ||
                        obj instanceof Short || obj instanceof Byte;

            case T_STRING:
                return obj instanceof String || obj instanceof Character;

            case T_BOOL:
                return obj instanceof Boolean;

            case T_ARRAY:
                return obj instanceof java.util.List;

            case T_OBJECT:
                return obj instanceof java.util.Map;

            case IDENTIFIER:
                String customTypeName = typeToken.lexeme;

                // =============================================================
                // ⭐ A REDE DE SEGURANÇA DOS TIPOS JAVA (Sem Token Próprio) ⭐
                // Se o programador digitou 'var x: byte', o Lexer leu "byte"
                // como um Identifier comum. Interceptamos os nomes nativos aqui!
                // =============================================================
                switch (customTypeName.toLowerCase()) {
                    case "long":
                    case "short":
                    case "byte":
                    case "integer":
                        return obj instanceof Long || obj instanceof Integer || obj instanceof Short || obj instanceof Byte;
                    case "double":
                    case "number":
                        return obj instanceof Number;
                    case "char":
                    case "character":
                        return obj instanceof Character || obj instanceof String;
                    case "any":
                    case "object":
                        return true;
                }

                // =============================================================
                // ⭐ A CURA DO ERRO "Esperava XplEvent, recebeu XplEvent" ⭐
                // =============================================================
                if (obj instanceof com.dic.xsuper.engine.poo.XplInstance instance) {
                    // 1. Se a classe XPL já foi injetada com sucesso, verifica pelo Nome ou pelo Modelo
                    if (instance.klass != null) {
                        if (instance.klass.model.name.equals(customTypeName)) return true;
                        return instance.klass.model.isSubclassOf(customTypeName);
                    }
                    // 2. Se for NULL (como no caso dos eventos que nascem "órfãos" da UI),
                    // NÃO FAZEMOS 'return false'. Deixamos a execução passar para o Fallback abaixo!
                }

                // Fallback para objectos Java nativos injectados no motor
                return obj.getClass().getSimpleName().equalsIgnoreCase(customTypeName);

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
        try {
            return environment.get(expr.name.lexeme);
        } catch (RuntimeException e) {
            // ⭐ O ESCUDO DE TITÂNIO: Fallback para Modelos Globais
            // Se a variável não estiver na memória, mas for uma Classe/Molde registado, entregamos a classe!
            String varName = expr.name.lexeme;

            if (registry_model.containsKey(varName)) {
                XPLModel model = registry_model.get(varName);
                if (model.hasBaseImplementation && !model.isDecorator) {
                    return new XplClass(model, this.globals);
                }
                return model;
            }
            if (registry_generic_models.containsKey(varName)) return registry_generic_models.get(varName);
            if (registry_Interfaces.containsKey(varName)) return registry_Interfaces.get(varName);

            // Se não for classe nenhuma, explode o erro real!
            throw e;
        }
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        // ⭐ O POLÍCIA INTERCEPTA A ATRIBUIÇÃO AQUI! ⭐
        validateAssignmentType(expr.name, value);

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
        // 1. Avalia o nome da função ou método (ex: 'println', 'dados', 'eu.falar')
        Object callee = evaluate(expr.callee);

        // 2. Verifica se o alvo implementa o contrato de invocação
        if (!(callee instanceof XplCallable function)) {
            throw new ControlFlow.RuntimeError(expr.paren, "Isto não é uma função ou classe instanciável e não pode ser chamado.");
        }

        // ⭐ INTERCETOR DE SPREAD NOS ARGUMENTOS! ⭐
        // Criamos uma nova fila onde os arrays desempacotados se vão misturar com os argumentos normais.
        java.util.List<Expr.CallArg> processedArgs = new java.util.ArrayList<>();

        for (Expr.CallArg arg : expr.arguments) {
            if (arg.expression instanceof Expr.Spread spread) {
                // Se for um spread, temos de avaliar AGORA para saber o que desempacotar!
                Object iterable = evaluate(spread.expression);
                if (iterable instanceof java.util.List<?> list) {
                    // Despejamos cada item da lista como se fosse um argumento individual injetado
                    for (Object item : list) {
                        processedArgs.add(new Expr.CallArg(null, new Expr.Literal(item)));
                    }
                } else {
                    throw new ControlFlow.RuntimeError(spread.operator, "O operador spread '...' em argumentos requer um Array.");
                }
            } else {
                // Mantemos o PASSE DIRECTO PURO (TRUE LAZY BINDING) para todos os outros argumentos!
                processedArgs.add(arg);
            }
        }

        try {
            // Entregamos a fila já processada (expandida) directamente à função ou classe!
            return function.call(this, processedArgs);
        } catch (ControlFlow.RuntimeError erroNativo) {
            // Preserva a coordenada exacta (linha/coluna) do erro disparado pelo Binder!
            throw erroNativo;
        } catch (RuntimeException erroJava) {
            // ⭐ A VACINA DO DEBUGGER: Se a mensagem for nula (ex: NullPointerException),
            // imprime o rasto no terminal para sabermos exactamente onde a bomba rebentou!
            if (erroJava.getMessage() == null) {
                erroJava.printStackTrace();
            }
            throw new ControlFlow.RuntimeError(expr.paren, "Falha Nativa no Motor Java: " + erroJava);
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
            case T_BOOL:
                if (!(value instanceof Boolean))
                    throw new ControlFlow.RuntimeError(typeAnnotation, "O valor atribuído não é do tipo 'bool'.");
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
    // ⭐ A MAGIA DO TO_STRING NATIVO (AGORA RECURSIVO!) ⭐
    public String stringify(Object object) {
        switch (object) {
            case null -> {
                return "null";
            }

            // 1. Se for uma Instância XPL, tenta invocar o toString() automaticamente!
            case XplInstance instance -> {
                // ⭐ VACINA: É um MetaBuilder (Fantasma)?
                if (instance.klass == null) return "<MetaInstance JIT>";

                Stmt.Function toStringMethod = instance.klass.model.findMethod("toString");

                if (toStringMethod != null && toStringMethod.params.isEmpty()) {
                    try {
                        XPLModel owner = instance.klass.model.getOwnerOfMethod("toString");
                        XplFunction func = new XplFunction(toStringMethod, instance.klass.closure, owner);
                        Object result = func.bind(instance).call(this, new ArrayList<>());
                        return String.valueOf(result);
                    } catch (Exception e) {
                        return "<Erro ao executar toString() na Instância de " + instance.klass.model.name + ">";
                    }
                }
                return "<Instância de " + instance.klass.model.name + ">";
            }

            // ⭐ 2. INJEÇÃO RECURSIVA EM LISTAS (Arrays) ⭐
            case List<?> list -> {
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
            case Map<?, ?> map -> {
                StringBuilder sb = new StringBuilder();
                sb.append("{");
                int i = 0;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    sb.append(entry.getKey().toString()).append(": ").append(stringify(entry.getValue()));
                    if (i < map.size() - 1) sb.append(", ");
                    i++;
                }
                sb.append("}");
                return sb.toString();
            }

            // 4. Comportamento numérico base
            case Double v -> {
                String text = object.toString();
                if (text.endsWith(".0")) {
                    text = text.substring(0, text.length() - 2);
                }
                return text;
            }
            default -> {}
        }

        return object.toString();
    }

    // Converte "#RRGGBB" para Códigos ANSI True Color (24-bit)
    public String hexToAnsi(String hex) {
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