package com.dic.xsuper.engine.natives.models;

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
import com.dic.xsuper.engine.poo.relection.MetaReflectionEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReflectNativeModel {

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Reflect", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        XplClass reflectClass = new XplClass(model, interpreter.globals);

        // API Global: var ref = Reflect.on(alvo)
        model.staticFields.put("on", buildCallable(1, (intp, args) -> {
            Object target = intp.evaluate(args.getFirst().expression);
            return createReflectionInstance(target, intp, reflectClass);
        }));

        interpreter.registry_model.put("Reflect", model);
        interpreter.environment.defineConst("Reflect", reflectClass);
    }

    // =========================================================================
    // ⭐ A FÁBRICA DO "OBJETO REFLETIDO" TOTAL ⭐
    // O Objeto devolvido possui TODOS os superpoderes do MetaReflectionEngine.
    // =========================================================================
    public static XplInstance createReflectionInstance(Object target, Interpreter intp, XplClass reflectClass) {
        if (reflectClass == null) {
            reflectClass = (XplClass) intp.globals.get("Reflect");
        }

        XplInstance inst = new XplInstance(reflectClass);
        inst.fields.put("raw", target); // Guarda o objeto original (vazio de reatividade)

        // Extrai o ADN através da Engine principal
        XPLModel targetModel = MetaReflectionEngine.extractModel(target);

        // =========================================================================
        // 1. IDENTIDADE E MODIFICADORES
        // =========================================================================
        inst.fields.put("getName", buildCallable(0, (i, a) -> targetModel != null ? targetModel.name : i.getXplTypeName(target)));
        inst.fields.put("isSealed", buildCallable(0, (i, a) -> targetModel != null && targetModel.isSealed));
        inst.fields.put("isAbstract", buildCallable(0, (i, a) -> targetModel != null && targetModel.isAbstract));
        inst.fields.put("isGenericBlueprint", buildCallable(0, (i, a) -> targetModel != null && targetModel.isGenericBlueprint));
        inst.fields.put("hasBaseImplementation", buildCallable(0, (i, a) -> targetModel != null && targetModel.hasBaseImplementation));

        // =========================================================================
        // 2. INTROSPEÇÃO ESTRUTURAL (METADADOS PROFUNDOS)
        // =========================================================================
        inst.fields.put("getMethods", buildCallable(0, (i, a) -> {
            if (targetModel == null) return new ArrayList<>();
            return MetaReflectionEngine.buildMethodsMetadata(targetModel);
        }));

        inst.fields.put("getFields", buildCallable(0, (i, a) -> {
            if (targetModel == null) return new ArrayList<>();
            return MetaReflectionEngine.buildFieldsMetadata(targetModel);
        }));

        inst.fields.put("getInterfaces", buildCallable(0, (i, a) -> {
            if (targetModel == null) return new ArrayList<>();
            return new ArrayList<>(targetModel.implementedInterfaces);
        }));

        inst.fields.put("getDecorators", buildCallable(0, (i, a) -> {
            List<Map<String, Object>> decs = new ArrayList<>();
            if (targetModel != null && targetModel.decoratorNodes != null) {
                for (Stmt.DecoratorNode dec : targetModel.decoratorNodes) {
                    Map<String, Object> d = new HashMap<>();
                    d.put("name", dec.name.lexeme);
                    d.put("argsCount", dec.arguments.size());
                    decs.add(d);
                }
            }
            return decs;
        }));

        inst.fields.put("toObject", buildCallable(0, (i, a) -> {
            if (targetModel == null) return new HashMap<>();
            return MetaReflectionEngine.buildDeclareToObject(targetModel, i.typeAliases);
        }));

        // =========================================================================
        // 3. BUSCA FINA E RÁPIDA
        // =========================================================================
        inst.fields.put("hasMethod", buildCallable(1, (i, a) -> {
            if (targetModel == null) return false;
            String mName = i.stringify(i.evaluate(a.getFirst().expression));
            return targetModel.findMethod(mName) != null;
        }));

        inst.fields.put("hasField", buildCallable(1, (i, a) -> {
            if (targetModel == null) return false;
            String fName = i.stringify(i.evaluate(a.getFirst().expression));
            return targetModel.fields.containsKey(fName);
        }));

        inst.fields.put("getOwnerOfMethod", buildCallable(1, (i, a) -> {
            if (targetModel == null) return null;
            String mName = i.stringify(i.evaluate(a.getFirst().expression));
            XPLModel owner = targetModel.getOwnerOfMethod(mName);
            return owner != null ? owner.name : null;
        }));

        // =========================================================================
        // 4. GENEALOGIA E HERANÇA
        // =========================================================================
        inst.fields.put("getSuperclass", buildCallable(0, (i, a) -> {
            if (targetModel == null || targetModel.superclass == null) return null;
            return targetModel.superclass.name;
        }));

        inst.fields.put("implementsInterface", buildCallable(1, (i, a) -> {
            if (targetModel == null) return false;
            String iName = i.stringify(i.evaluate(a.getFirst().expression));
            return targetModel.implementsInterface(iName);
        }));

        inst.fields.put("isSubclassOf", buildCallable(1, (i, a) -> {
            if (targetModel == null) return false;
            String pName = i.stringify(i.evaluate(a.getFirst().expression));
            return targetModel.isSubclassOf(pName);
        }));

        inst.fields.put("isInstance", buildCallable(1, (i, a) -> {
            if (targetModel == null) return false;
            Object otherObj = i.evaluate(a.getFirst().expression);
            if (otherObj instanceof XplInstance xInst && xInst.klass != null) {
                return xInst.klass.model.isSubclassOf(targetModel.name);
            }
            return false;
        }));

        // =========================================================================
        // 5. MANIPULAÇÃO DO ESTADO NA RAM (Bypass de Segurança/Listeners)
        // =========================================================================
        inst.fields.put("getFieldValue", buildCallable(1, (i, a) -> {
            String propName = i.stringify(i.evaluate(a.getFirst().expression));
            if (target instanceof XplInstance xInst) return xInst.fields.get(propName);
            if (target instanceof XplClass xCls) return xCls.model.staticFields.get(propName);
            return null;
        }));

        inst.fields.put("setFieldValue", buildCallable(2, (i, a) -> {
            String propName = i.stringify(i.evaluate(a.get(0).expression));
            Object val = i.evaluate(a.get(1).expression);
            if (target instanceof XplInstance xInst) xInst.fields.put(propName, val);
            else if (target instanceof XplClass xCls) xCls.model.staticFields.put(propName, val);
            return true;
        }));

        // =========================================================================
        // 6. METAPROGRAMAÇÃO ATIVA (Criação, Injeção e Execução Dinâmica)
        // =========================================================================
        inst.fields.put("newInstance", buildCallable(-1, (i, a) -> {
            if (targetModel == null || !targetModel.canBeInstantiated())
                throw new ControlFlow.RuntimeError(null, "Reflect.newInstance: O alvo '" + (targetModel!=null ? targetModel.name : "null") + "' não é instanciável.");

            XplClass klass = new XplClass(targetModel, i.environment);
            List<Expr.CallArg> argsToPass = new ArrayList<>(a);
            return klass.call(i, argsToPass); // Passa argumentos capturados para o construtor 'init'
        }));

        inst.fields.put("cloneInstance", buildCallable(0, (i, a) -> {
            if (!(target instanceof XplInstance instance))
                throw new ControlFlow.RuntimeError(null, "Reflect.cloneInstance: Requer uma instância viva.");
            XplInstance cloned = new XplInstance(instance.klass);
            cloned.fields.putAll(instance.fields);
            cloned.fields.remove("__active_listeners__"); // Seguro e isolado contra loops
            return cloned;
        }));

        inst.fields.put("injectField", buildCallable(2, (i, a) -> {
            if (targetModel == null) throw new ControlFlow.RuntimeError(null, "Alvo não suporta injeção.");
            String fName = i.stringify(i.evaluate(a.get(0).expression));
            String tName = i.stringify(i.evaluate(a.get(1).expression));

            Token pubToken = new Token(TokenType.PUBLIC, "pub", null, 0, 0);
            TypeNode typeNode = new TypeNode.Simple(new Token(TokenType.IDENTIFIER, tName, null, 0, 0));
            targetModel.addField(new Stmt.FieldDecl(pubToken, false, false, false, new Token(TokenType.IDENTIFIER, fName, null, 0, 0), typeNode));
            return true;
        }));

        inst.fields.put("injectMethod", buildCallable(1, (i, a) -> {
            if (targetModel == null) throw new ControlFlow.RuntimeError(null, "Alvo não suporta injeção.");
            Object astNode = i.evaluate(a.getFirst().expression);

            // A ponte mágica para objetos Builder customizados com toAST()
            if (astNode instanceof XplInstance builderInst && builderInst.klass != null) {
                if (builderInst.klass.model.findMethod("toAST") != null) {
                    Token astToken = new Token(TokenType.IDENTIFIER, "toAST", null, 0, 0);
                    Object methodObj = builderInst.get(astToken);
                    if (methodObj instanceof XplCallable callAST) {
                        astNode = callAST.call(i, new ArrayList<>());
                    }
                }
            }

            if (!(astNode instanceof Stmt.Function) && !(astNode instanceof XplFunction)) {
                throw new ControlFlow.RuntimeError(null, "Reflect.injectMethod espera uma função AST.");
            }

            Stmt.Function novaFuncao = (astNode instanceof Stmt.Function f) ? f : ((XplFunction) astNode).declaration;

            if (target instanceof XplInstance instance) {
                if (instance.klass == null) throw new ControlFlow.RuntimeError(null, "Não é possível injetar método num MetaBuilder.");
                instance.fields.put(novaFuncao.name.lexeme, new XplFunction(novaFuncao, i.environment, instance.klass.model));
            } else {
                targetModel.addMethod(novaFuncao);
            }
            return true;
        }));

        inst.fields.put("CallMethod", buildCallable(-1, (i, a) -> {
            if (a.isEmpty()) throw new ControlFlow.RuntimeError(null, "CallMethod requer o nome do método.");
            String methodName = i.stringify(i.evaluate(a.getFirst().expression));
            List<Expr.CallArg> argsToPass = new ArrayList<>(a);
            argsToPass.removeFirst(); // Remove o nome do método dos parâmetros

            Token fakeToken = new Token(TokenType.IDENTIFIER, methodName, null, 0, 0);
            Object method = null;

            if (target instanceof XplInstance instance) {
                method = instance.get(fakeToken);
            } else if (target instanceof XplClass klass) {
                Stmt.Function staticFunc = klass.model.findMethod(methodName);
                if (staticFunc != null && staticFunc.isStatic) method = new XplFunction(staticFunc, klass.closure, klass.model);
            } else if (targetModel != null) {
                Stmt.Function staticFunc = targetModel.findMethod(methodName);
                if (staticFunc != null && staticFunc.isStatic) method = new XplFunction(staticFunc, i.globals, targetModel);
            }

            if (method instanceof XplCallable callable) {
                return callable.call(i, argsToPass);
            }
            throw new ControlFlow.RuntimeError(null, "O método '" + methodName + "' não é invocável no alvo.");
        }));

        // =========================================================================
        // 7. GENÉRICOS E ALIASES
        // =========================================================================
        inst.fields.put("getTypeParameters", buildCallable(0, (i, a) -> {
            if (targetModel == null) return new ArrayList<>();
            return targetModel.typeParameters.stream().map(t -> t.lexeme).collect(Collectors.toList());
        }));

        inst.fields.put("getResolvedGenerics", buildCallable(0, (i, a) -> {
            if (targetModel == null) return new HashMap<>();
            return new HashMap<>(targetModel.resolvedGenericMap);
        }));

        inst.fields.put("getAliases", buildCallable(0, (i, a) -> new HashMap<>(i.typeAliases)));

        inst.fields.put("getVariantAliases", buildCallable(0, (i, a) -> {
            if (targetModel == null) return new ArrayList<>();
            return new ArrayList<>(targetModel.variantAliases);
        }));

        // =========================================================================
        // 8. AUDITORIA DE LISTENERS (O Fim do Ciclo MOP)
        // =========================================================================
        inst.fields.put("getActiveListeners", buildCallable(0, (i, a) -> {
            if (target instanceof XplInstance instance && instance.fields.containsKey("__active_listeners__")) {
                return instance.fields.get("__active_listeners__");
            }
            return new ArrayList<>();
        }));

        inst.fields.put("hasListener", buildCallable(1, (i, a) -> {
            String lName = i.stringify(i.evaluate(a.getFirst().expression));

            if (target instanceof XplInstance instance && instance.fields.containsKey("__active_listeners__")) {
                @SuppressWarnings("unchecked")
                List<XplInstance> listeners = (List<XplInstance>) instance.fields.get("__active_listeners__");
                for (XplInstance l : listeners) {
                    if (l.klass.model.name.equals(lName)) return true;
                }
            }

            if (targetModel != null && targetModel.decoratorNodes != null) {
                for (Stmt.DecoratorNode dec : targetModel.decoratorNodes) {
                    if (dec.name.lexeme.equals(lName)) return true;
                }
            }
            return false;
        }));

        return inst;
    }

    private interface NativeAction { Object execute(Interpreter i, List<Expr.CallArg> a); }
    private static XplCallable buildCallable(int arity, NativeAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) { return action.execute(intp, args); }
        };
    }
}