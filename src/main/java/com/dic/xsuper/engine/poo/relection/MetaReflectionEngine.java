package com.dic.xsuper.engine.poo.relection;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.ast.Stmt;
import com.dic.xsuper.engine.ast.TypeNode;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.core.Token;
import com.dic.xsuper.engine.core.TokenType;
import com.dic.xsuper.engine.execution.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.execution.XplFunction;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;
import com.dic.xsuper.engine.poo.XplInstance;

import java.util.*;
import java.util.stream.Collectors;

/**
 * O Motor Central de Metaprogramação e Reflexão (Nível Enterprise).
 * Extrai a Árvore Sintática Abstrata (AST) para dicionários profundos.
 */
public class MetaReflectionEngine {

    // ========================================================
    // 1. EXTRATORES DE ADN
    // ========================================================
    public static XPLModel extractModel(Object target) {
        if (target instanceof XplInstance) return ((XplInstance) target).klass.model;
        if (target instanceof XplClass) return ((XplClass) target).model;
        if (target instanceof XPLModel) return (XPLModel) target;
        return null;
    }

    private static String extractTypeName(TypeNode typeNode) {
        if (typeNode == null) return "any";
        if (typeNode instanceof TypeNode.Simple) {
            return typeNode.name.lexeme;
        }
        return "complex_type";
    }

    // ========================================================
    // 2. CONSTRUTORES DE METADADOS PROFUNDOS
    // ========================================================

    private static List<Map<String, Object>> buildFieldsMetadata(XPLModel model) {
        List<Map<String, Object>> fieldsArray = new ArrayList<>();
        for (Stmt.FieldDecl field : model.fields.values()) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("name", field.name.lexeme);
            meta.put("visibility", field.modifier != null ? field.modifier.lexeme : "priv");
            meta.put("type", extractTypeName(field.type));
            meta.put("isReadOnly", field.isReadonly);
            meta.put("isStatic", field.isStatic);
            meta.put("isFinal", field.isFinal);
            fieldsArray.add(meta);
        }
        return fieldsArray;
    }

    private static List<Map<String, Object>> buildMethodsMetadata(XPLModel model) {
        List<Map<String, Object>> methodsArray = new ArrayList<>();
        for (Stmt.Function method : model.methods.values()) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("name", method.name.lexeme);
            meta.put("visibility", method.accessModifier != null ? method.accessModifier.lexeme : "priv");
            meta.put("returnType", extractTypeName(method.returnType));
            meta.put("isStatic", method.isStatic);
            meta.put("isAbstract", method.isAbstract);
            meta.put("isConstructor", method.name.lexeme.equals("init"));

            List<String> exceptions = new ArrayList<>();
            if (method.thrownExceptions != null) {
                exceptions.addAll(method.thrownExceptions.stream().map(t -> t.lexeme).collect(Collectors.toList()));
            }
            meta.put("thrownExceptions", exceptions);

            List<String> decorators = new ArrayList<>();
            if (method.decorators != null) {
                decorators.addAll(method.decorators.stream().map(Object::toString).collect(Collectors.toList()));
            }
            meta.put("decorators", decorators);

            List<Map<String, Object>> params = new ArrayList<>();
            if (method.params != null) {
                for (Stmt.Param p : method.params) {
                    Map<String, Object> paramMeta = new HashMap<>();
                    paramMeta.put("name", p.name.lexeme);
                    paramMeta.put("type", extractTypeName(p.typeNode));
                    paramMeta.put("hasDefaultValue", p.defaultValue != null);
                    params.add(paramMeta);
                }
            }
            meta.put("params", params);
            methodsArray.add(meta);
        }
        return methodsArray;
    }

    // ⭐ MÉTODO MADURO: recebe o mapa de aliases e constrói o objeto completo
    private static Map<String, Object> buildDeclareToObject(XPLModel model, Map<String, TypeNode> typeAliases) {
        Map<String, Object> metaObject = new HashMap<>();

        // 1. Informações básicas
        metaObject.put("name", model.name);
        metaObject.put("isSealed", model.isSealed);
        metaObject.put("isAbstract", model.isAbstract);
        metaObject.put("superclass", model.superclass != null ? model.superclass.name : null);
        metaObject.put("baseModel", model.baseModel != null ? model.baseModel.name : null);
        metaObject.put("hasBaseImplementation", model.hasBaseImplementation);
        metaObject.put("canBeInstantiated", model.canBeInstantiated());

        // 2. Estruturas (fields, methods)
        metaObject.put("fields", buildFieldsMetadata(model));
        metaObject.put("methods", buildMethodsMetadata(model));

        // 3. Decoradores (com argumentos)
        List<String> decoratorStrings = new ArrayList<>();
        for (Stmt.DecoratorNode dec : model.decoratorNodes) {
            String args = dec.arguments.isEmpty() ? "" : dec.arguments.toString();
            decoratorStrings.add(dec.name.lexeme + args);
        }
        metaObject.put("decorators", decoratorStrings);
        // Também manter a lista simples (appliedDecorators) se existir
        metaObject.put("appliedDecorators", new ArrayList<>(model.appliedDecorators));

        // 4. Interfaces implementadas
        metaObject.put("interfaces", new ArrayList<>(model.implementedInterfaces));

        // 5. Genéricos (parâmetros e resolução)
        List<String> typeParams = model.typeParameters.stream()
                .map(t-> t.lexeme)
                .collect(Collectors.toList());
        metaObject.put("typeParameters", typeParams);
        metaObject.put("resolvedGenerics", new HashMap<>(model.resolvedGenericMap));

        // 6. Campos estáticos e valores default
        metaObject.put("staticFields", new HashMap<>(model.staticFields));
        metaObject.put("defaultInstanceFields", new HashMap<>(model.defaultInstanceFields));

        // 7. Aliases que apontam para este modelo
        List<String> aliases = new ArrayList<>();
        for (Map.Entry<String, TypeNode> entry : typeAliases.entrySet()) {
            String aliasName = entry.getKey();
            TypeNode target = entry.getValue();
            if (target instanceof TypeNode.Simple) {
                if (((TypeNode.Simple) target).name.lexeme.equals(model.name)) {
                    aliases.add(aliasName);
                }
            } else if (target instanceof TypeNode.Generic) {
                if (target.name.lexeme.equals(model.name)) {
                    aliases.add(aliasName);
                }
            } else if (target instanceof TypeNode.Optional) {
                // Pode-se aprofundar, mas simplificamos
                TypeNode inner = ((TypeNode.Optional) target).innerType;
                if (inner instanceof TypeNode.Simple && ((TypeNode.Simple) inner).name.lexeme.equals(model.name)) {
                    aliases.add(aliasName);
                }
            }
        }
        metaObject.put("aliases", aliases);

        // 8. Variantes (as) que derivam deste modelo
        if (!model.variantAliases.isEmpty()) {
            metaObject.put("variantAliases", new ArrayList<>(model.variantAliases));
        } else {
            // ⭐ A CORREÇÃO DE OURO: Se não houver, garante que a chave existe devolvendo uma lista vazia!
            metaObject.put("variantAliases", new ArrayList<>());
        }

        // 9. Metadados adicionais (se necessário)
        metaObject.put("isDecorator", model.isDecorator);
        metaObject.put("metaInitHook", model.metaInitHook);
        metaObject.put("metaGetHook", model.metaGetHook);
        metaObject.put("metaSetHook", model.metaSetHook);
        metaObject.put("metaEndHook", model.metaEndHook);
        metaObject.put("isGenericBlueprint", model.isGenericBlueprint);

        return metaObject;
    }

    // ========================================================
    // 3. O INTERCETOR NATIVO (O PROXY QUÂNTICO)
    // ========================================================
    // ========================================================
    // 3. O INTERCETOR NATIVO (O PROXY QUÂNTICO)
    // ========================================================
    public static XplCallable createMetaCallable(Object initialTarget, Expr.MetaAccess expr) {
        String metaMethodName = expr.name.lexeme;

        return new XplCallable() {
            @Override
            public int arity() { return -1; }

            @Override
            public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {

                // =========================================================
                // ⭐ O DESEMPACOTADOR QUÂNTICO (Bypass do Decorador) ⭐
                // O alvo pode ser um proxy @ContextDecorator. Precisamos descer
                // até ao núcleo real e guardar os decoradores encontrados pelo caminho!
                // =========================================================
                Object target = initialTarget;
                List<String> variableDecorators = new ArrayList<>();

                while (target instanceof XplInstance proxy && Boolean.TRUE.equals(proxy.fields.get("_isDecoratorProxy"))) {
                    Object decInst = proxy.fields.get("_decoratorInstance");
                    if (decInst instanceof XplInstance dec) {
                        variableDecorators.add("@" + dec.klass.model.name); // Guarda o nome (Ex: @Audit)
                    }
                    target = proxy.fields.get("_val"); // Desce para a próxima camada!
                }

                // Agora o 'target' é o Repositorio REAL!
                XPLModel model = extractModel(target);
                if (model == null) {
                    throw new ControlFlow.RuntimeError(expr.operator, "Erro Meta: O objeto não possui metadados XPL.");
                }

                return switch (metaMethodName) {
                    case "getDeclareName" -> model.name;
                    case "isSealed" -> model.isSealed;

                    // Metadados estruturais
                    case "getMethods" -> buildMethodsMetadata(model);
                    case "getFields" -> buildFieldsMetadata(model);
                    case "getDeclareToObject" -> buildDeclareToObject(model, interpreter.typeAliases);

                    // Metadados avançados
                    case "getInterfaces" -> new ArrayList<>(model.implementedInterfaces);
                    case "getDecorators" -> {
                        // Junta os decoradores aplicados à Variável com os da Classe!
                        List<String> decs = new ArrayList<>(variableDecorators);
                        if (model.decoratorNodes != null) {
                            for (Stmt.DecoratorNode dec : model.decoratorNodes) {
                                String args = dec.arguments.isEmpty() ? "" : dec.arguments.toString();
                                decs.add(dec.name.lexeme + args);
                            }
                        }
                        yield decs;
                    }
                    case "getTypeParameters" -> model.typeParameters.stream()
                            .map(t-> t.lexeme)
                            .collect(Collectors.toList());
                    case "getResolvedGenerics" -> new HashMap<>(model.resolvedGenericMap);
                    case "getAliases" -> java.util.Collections.unmodifiableMap(interpreter.typeAliases);
                    case "getVariantAliases" -> new ArrayList<>(model.variantAliases);

                    // Intercessão dinâmica (Usamos o 'target' DESEMPACOTADO!)
                    case "CallMethod" -> executeCallMethod(target, interpreter, arguments, expr.name);

                    case "injectMethod" -> {
                        if (arguments.isEmpty()) {
                            throw new ControlFlow.RuntimeError(expr.name, "injectMethod requer um argumento: a função a injetar.");
                        }
                        Object astNode = interpreter.evaluate(arguments.getFirst().expression);
                        if (!(astNode instanceof Stmt.Function novaFuncao)) {
                            throw new ControlFlow.RuntimeError(expr.name, "injectMethod espera um argumento do tipo Function (retornado por Func()).");
                        }
                        if (target instanceof XplInstance instance) {
                            if (instance.klass == null) {
                                throw new ControlFlow.RuntimeError(expr.name, "Não é possível injetar método numa instância JIT (MetaBuilder).");
                            }
                            instance.fields.put(novaFuncao.name.lexeme, new XplFunction(novaFuncao, interpreter.environment, instance.klass.model));
                        } else if (target instanceof XplClass klass) {
                            klass.model.addMethod(novaFuncao);
                        } else {
                            throw new ControlFlow.RuntimeError(expr.name, "injectMethod só pode ser aplicado a instâncias ou classes XPL.");
                        }
                        yield true;
                    }
                    default -> throw new ControlFlow.RuntimeError(expr.name,
                            "Erro Meta: A propriedade '::" + metaMethodName + "' não existe no Motor XPL.");
                };
            }
        };
    }

    // ========================================================
    // 4. INVOCAÇÃO DINÂMICA
    // ========================================================
    private static Object executeCallMethod(Object target, Interpreter interpreter, List<Expr.CallArg> arguments, Token errorToken) {
        if (!(target instanceof XplInstance instance)) {
            throw new ControlFlow.RuntimeError(errorToken, "O ::CallMethod apenas pode ser usado em Instâncias vivas.");
        }
        if (arguments.isEmpty()) {
            throw new ControlFlow.RuntimeError(errorToken, "O ::CallMethod precisa do nome do método (String) como primeiro argumento.");
        }

        String methodName = (String) interpreter.evaluate(arguments.getFirst().expression);

        List<Expr.CallArg> argsToPass = new ArrayList<>(arguments);
        argsToPass.removeFirst();

        Token fakeToken = new Token(TokenType.IDENTIFIER, methodName, null, 0, 0);
        Object method = instance.get(fakeToken);

        if (method instanceof XplCallable callable) {
            return callable.call(interpreter, argsToPass);
        }
        throw new ControlFlow.RuntimeError(errorToken, "Meta falhou: '" + methodName + "' não é uma função invocável.");
    }
}