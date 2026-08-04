package com.dic.xsuper.engine.poo.relection;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.ast.Stmt;
import com.dic.xsuper.engine.ast.TypeNode;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.core.Token;
import com.dic.xsuper.engine.core.TokenType;
import com.dic.xsuper.engine.exceptions.ControlFlow;
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

    public static List<Map<String, Object>> buildFieldsMetadata(XPLModel model) {
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

    public static List<Map<String, Object>> buildMethodsMetadata(XPLModel model) {
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
                exceptions.addAll(method.thrownExceptions.stream().map(t -> t.lexeme).toList());
            }
            meta.put("thrownExceptions", exceptions);

            List<String> decorators = new ArrayList<>();
            if (method.decorators != null) {
                decorators.addAll(method.decorators.stream().map(Object::toString).toList());
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
    public static Map<String, Object> buildDeclareToObject(XPLModel model, Map<String, TypeNode> typeAliases) {
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
                // ⭐ O DESEMPACOTADOR QUÂNTICO (Sem Proxies) ⭐
                // A nossa arquitectura nova aboliu Proxies, logo lemos os metadados
                // passivos directamente da memória oculta da instância!
                // =========================================================
                Object target = initialTarget;
                List<String> variableDecorators = new ArrayList<>();

                if (target instanceof XplInstance inst && inst.fields.containsKey("__applied_decorators__")) {
                    @SuppressWarnings("unchecked")
                    List<String> decs = (List<String>) inst.fields.get("__applied_decorators__");
                    variableDecorators.addAll(decs);
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
                                String args = dec.arguments.isEmpty() ? "" : "(" + dec.arguments.toString() + ")";
                                decs.add("@" + dec.name.lexeme + args);
                            }
                        }
                        yield decs;
                    }
                    // =========================================================
                    // ⭐ 1. INTROSPEÇÃO FINA E RÁPIDA
                    // =========================================================
                    case "hasMethod" -> {
                        if (arguments.isEmpty()) throw new ControlFlow.RuntimeError(expr.name, "hasMethod requer o nome do método.");
                        String mName = interpreter.stringify(interpreter.evaluate(arguments.get(0).expression));
                        yield model.findMethod(mName) != null;
                    }
                    case "hasField" -> {
                        if (arguments.isEmpty()) throw new ControlFlow.RuntimeError(expr.name, "hasField requer o nome da propriedade.");
                        String fName = interpreter.stringify(interpreter.evaluate(arguments.get(0).expression));
                        yield model.fields.containsKey(fName);
                    }

                    // =========================================================
                    // ⭐ 2. HERANÇA, ÁRVORE E CONTRATOS
                    // =========================================================
                    case "getSuperclass" -> model.superclass != null ? model.superclass.name : null;
                    case "isAbstract" -> model.isAbstract;
                    case "implementsInterface" -> {
                        if (arguments.isEmpty()) throw new ControlFlow.RuntimeError(expr.name, "implementsInterface requer o nome da interface.");
                        String iName = interpreter.stringify(interpreter.evaluate(arguments.get(0).expression));
                        yield model.implementsInterface(iName);
                    }
                    case "isSubclassOf" -> {
                        if (arguments.isEmpty()) throw new ControlFlow.RuntimeError(expr.name, "isSubclassOf requer o nome da classe base.");
                        String pName = interpreter.stringify(interpreter.evaluate(arguments.get(0).expression));
                        yield model.isSubclassOf(pName);
                    }

                    // =========================================================
                    // ⭐ 3. METAPROGRAMAÇÃO ATIVA (O PODER ABSOLUTO)
                    // =========================================================
                    case "newInstance" -> {
                        // Instancia a classe dinamicamente através da Reflexão!
                        XplClass klass = new XplClass(model, interpreter.environment);
                        yield klass.call(interpreter, arguments); // Passa todos os argumentos recebidos para o 'init'!
                    }
                    case "cloneInstance" -> {
                        if (!(target instanceof XplInstance instance)) throw new ControlFlow.RuntimeError(expr.name, "cloneInstance requer uma instância viva (não funciona apenas na classe).");
                        XplInstance cloned = new XplInstance(instance.klass);
                        cloned.fields.putAll(instance.fields); // Faz uma cópia da RAM
                        yield cloned;
                    }
                    case "injectField" -> {
                        if (arguments.size() < 2) throw new ControlFlow.RuntimeError(expr.name, "injectField requer (nome, tipo).");
                        String fName = interpreter.stringify(interpreter.evaluate(arguments.get(0).expression));
                        String tName = interpreter.stringify(interpreter.evaluate(arguments.get(1).expression));

                        Token pubToken = new Token(TokenType.PUBLIC, "pub", null, 0, 0);
                        TypeNode typeNode = new TypeNode.Simple(new Token(TokenType.IDENTIFIER, tName, null, 0, 0));

                        // Injeta a propriedade diretamente no molde da classe!
                        model.addField(new Stmt.FieldDecl(pubToken, false, false, false, new Token(TokenType.IDENTIFIER, fName, null, 0, 0), typeNode));
                        yield true;
                    }
                    // =========================================================
                    // ⭐ REFLEXÃO MADURA: Acesso e Mutação Silenciosa (Bypass Listeners)
                    // Podes ler ou alterar variáveis internas em tempo real!
                    // =========================================================
                    case "getFieldValue" -> {
                        if (arguments.isEmpty()) throw new ControlFlow.RuntimeError(expr.name, "getFieldValue requer o nome da propriedade.");
                        String propName = interpreter.stringify(interpreter.evaluate(arguments.get(0).expression));

                        if (target instanceof XplInstance instance) {
                            yield instance.fields.get(propName); // Lê a memória bruta!
                        } else if (target instanceof XplClass klass) {
                            yield klass.model.staticFields.get(propName);
                        }
                        yield null;
                    }

                    case "setFieldValue" -> {
                        if (arguments.size() < 2) throw new ControlFlow.RuntimeError(expr.name, "setFieldValue requer o nome da propriedade e o novo valor.");
                        String propName = interpreter.stringify(interpreter.evaluate(arguments.get(0).expression));
                        Object newVal = interpreter.evaluate(arguments.get(1).expression);

                        if (target instanceof XplInstance instance) {
                            instance.fields.put(propName, newVal); // Escreve na memória bruta (Bypass aos Listeners!)
                        } else if (target instanceof XplClass klass) {
                            klass.model.staticFields.put(propName, newVal);
                        }
                        yield true;
                    }

                    // =========================================================
                    // ⭐ 4. CAPTURA DE LISTENERS / DECORADORES ATIVOS
                    // =========================================================

                    // Retorna a lista de instâncias VIVAS dos listeners acoplados ao objeto!
                    case "getActiveListeners" -> {
                        if (target instanceof XplInstance instance) {
                            if (instance.fields.containsKey("__active_listeners__")) {
                                // Devolve a lista real das instâncias dos listeners!
                                yield instance.fields.get("__active_listeners__");
                            }
                        }
                        yield new ArrayList<>();
                    }

                    // Verifica rapidamente se um objeto ou classe tem um determinado listener
                    case "hasListener" -> {
                        if (arguments.isEmpty()) throw new ControlFlow.RuntimeError(expr.name, "hasListener requer o nome do listener.");
                        String lName = interpreter.stringify(interpreter.evaluate(arguments.get(0).expression));

                        // 1. Verifica os listeners VIVOS na memória (se for uma instância)
                        if (target instanceof XplInstance instance) {
                            if (instance.fields.containsKey("__active_listeners__")) {
                                @SuppressWarnings("unchecked")
                                List<XplInstance> listeners = (List<XplInstance>) instance.fields.get("__active_listeners__");
                                for (XplInstance l : listeners) {
                                    if (l.klass.model.name.equals(lName)) yield true;
                                }
                            }
                        }

                        // 2. Verifica a Árvore Genética (AST) se for apenas uma classe
                        if (model.decoratorNodes != null) {
                            for (Stmt.DecoratorNode dec : model.decoratorNodes) {
                                if (dec.name.lexeme.equals(lName)) yield true;
                            }
                        }
                        yield false;
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