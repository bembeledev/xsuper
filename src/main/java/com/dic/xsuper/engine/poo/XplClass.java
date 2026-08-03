package com.dic.xsuper.engine.poo;

import com.dic.xsuper.engine.core.Environment;
import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.exceptions.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.execution.XplFunction;
import com.dic.xsuper.engine.ast.Stmt;
import java.util.List;
import java.util.ArrayList;

public class XplClass implements XplCallable {
    public final XPLModel model;
    public final Environment closure;

    public XplClass(XPLModel model, Environment closure) {
        this.model = model;
        this.closure = closure;
    }

    @Override
    public int arity() {
        Stmt.Function initializer = model.findMethod("init");
        return initializer == null ? 0 : initializer.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
        XplInstance instance = new XplInstance(this);

        // =================================================================
        // ⭐ A MAGIA DA INTROSPECÇÃO: Injetar Listeners de Classe!
        // Se a classe tem decoradores (ex: @Validate declare Pessoa), ativamos aqui!
        // =================================================================
        if (model.decoratorNodes != null && !model.decoratorNodes.isEmpty()) {
            List<XplInstance> objectListeners = new ArrayList<>();

            for (Stmt.DecoratorNode adorno : model.decoratorNodes) {
                String listenerName = adorno.name.lexeme;
                XPLModel listenerModel = interpreter.registry_model.get(listenerName);

                if (listenerModel != null) {
                    XplClass listenerClass;
                    try { listenerClass = (XplClass) interpreter.environment.get(listenerName); }
                    catch (Exception e) { listenerClass = new XplClass(listenerModel, interpreter.globals); }

                    XplInstance listenerInstance = new XplInstance(listenerClass);
                    // ⭐ O TARGET NASCE COMO A API REFLECT DESDE O INÍCIO!
                    listenerInstance.fields.put("target", com.dic.xsuper.engine.natives.models.ReflectNativeModel.createReflectionInstance(instance, interpreter, null));
                    listenerInstance.fields.put("event", "INIT");
                    listenerInstance.fields.put("property", "constructor");
                    listenerInstance.fields.put("value", null);
                    // Construtor do Decorador
                    Stmt.Function construtor = listenerModel.findMethod("init");
                    if (construtor != null) {
                        new XplFunction(construtor, listenerClass.closure, listenerModel).bind(listenerInstance).call(interpreter, adorno.arguments);
                    }

                    // Hook de Instanciação (@Listen.Init) -> 0 ARGUMENTOS
                    if (listenerModel.metaInitHook != null) {
                        Stmt.Function hookFunc = listenerModel.findMethod(listenerModel.metaInitHook);
                        if (hookFunc != null) {
                            new XplFunction(hookFunc, listenerClass.closure, listenerModel).bind(listenerInstance).call(interpreter, new ArrayList<>());
                        }
                    }
                    objectListeners.add(listenerInstance);
                }
            }
            // Acopla os espiões à memória nativa da instância!
            instance.fields.put("__active_listeners__", objectListeners);
        }

        // =================================================================
        // 2. Chama o construtor original da Classe
        // =================================================================
        Stmt.Function initializer = model.findMethod("init");
        if (initializer != null) {
            XPLModel owner = model.getOwnerOfMethod("init");
            XplFunction constructor = new XplFunction(initializer, closure, owner);
            constructor.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }

    @Override public String toString() { return "<class " + model.name + ">"; }
}