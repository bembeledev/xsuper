package com.dic.xsuper.lang.natives.models;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.poo.XplClass;
import com.dic.xsuper.lang.poo.XplInstance;
import com.dic.xsuper.lang.poo.XplInterface;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

public class ObserverNativeModel {

    // ⭐ Anti-Loop Quântico: Impede que o Observer reaja às suas próprias ações
    private static final ThreadLocal<AtomicBoolean> isDispatching = ThreadLocal.withInitial(() -> new AtomicBoolean(false));

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Observer", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false; // Estático

        // =========================================================
        // ⭐ 1. CONSTANTES DE EVENTOS (Observer.Event.CREATE)
        // =========================================================
        Map<String, String> events = new LinkedHashMap<>();
        events.put("CREATE", "CREATE");
        events.put("UPDATE", "UPDATE");
        events.put("READ", "READ");
        events.put("DELETE", "DELETE");
        model.staticFields.put("Event", Collections.unmodifiableMap(events));

        // =========================================================
        // ⭐ 2. CONSTANTES DE TIPOS ESTRUTURAIS (Observer.Type.VARIABLE)
        // =========================================================
        Map<String, String> types = new LinkedHashMap<>();
        types.put("VARIABLE", "variable"); // Engloba ints, strings, arrays, objects (let/var/const)
        types.put("INTERFACE", "interface");
        types.put("DECLARE", "declare");   // O Molde (Blueprint)
        types.put("CLASS", "class");       // A Classe Executável
        types.put("INSTANCE", "instance"); // Objeto instanciado (new)
        types.put("FUNCTION", "function");
        model.staticFields.put("Type", Collections.unmodifiableMap(types));

        // =========================================================
        // ⭐ 3. MOLDE DA INSTÂNCIA (Observable)
        // =========================================================
        XPLModel observableModel = new XPLModel("Observable", null);
        observableModel.hasBaseImplementation = true;
        observableModel.canBeInstantiated = false;

        Token pubToken = new Token(TokenType.PUBLIC, "pub", null, 0, 0);
        observableModel.addField(new Stmt.FieldDecl(pubToken, false, false, true, new Token(TokenType.IDENTIFIER, "targetName", null, 0, 0), new TypeNode.Simple(new Token(TokenType.T_STRING, "string", null, 0, 0))));
        observableModel.addField(new Stmt.FieldDecl(pubToken, false, false, true, new Token(TokenType.IDENTIFIER, "targetType", null, 0, 0), new TypeNode.Simple(new Token(TokenType.T_STRING, "string", null, 0, 0))));

        XplClass observableClass = new XplClass(observableModel, interpreter.globals);

        // =========================================================
        // ⭐ 4. O CRIADOR: Observer.observable("nome", Observer.Type.VARIABLE)
        // =========================================================
        model.staticFields.put("observable", buildAction(2, (intp, args) -> {
            String targetName = intp.stringify(intp.evaluate(args.get(0).expression));
            String targetType = intp.stringify(intp.evaluate(args.get(1).expression));

            // Cria a Instância "Observable"
            XplInstance obsInstance = new XplInstance(observableClass);
            obsInstance.fields.put("targetName", targetName);
            obsInstance.fields.put("targetType", targetType);

            // Caixa forte para guardar os listeners associados a este Observable
            List<Environment.XplEnvironmentListener> myListeners = new ArrayList<>();
            obsInstance.fields.put("_listeners", myListeners);

            // ---------------------------------------------------------
            // Método: observable.watch((evento, velho, novo) => {})
            // ---------------------------------------------------------
            obsInstance.fields.put("watch", buildAction(1, (i, a) -> {
                Object callbackObj = i.evaluate(a.get(0).expression);
                if (!(callbackObj instanceof XplFunction callback)) {
                    throw new ControlFlow.RuntimeError(null, "O método watch exige uma função de callback.");
                }

                Interpreter callbackInterpreter = i.fork();

                Environment.XplEnvironmentListener listener = new Environment.XplEnvironmentListener() {
                    private void dispararXpl(String evento, String nome, Object velho, Object novo, String escopo) {
                        // 1. FILTRO: É a nossa variável alvo?
                        if (!targetName.equals(nome)) return;

                        // 2. FILTRO: É a estrutura (Variable, Function, Declare) que pedimos?
                        String actualType = getStructuralType(novo != null ? novo : velho);
                        if (!targetType.equals(actualType)) return;

                        // 3. BLINDAGEM DE LOOP
                        AtomicBoolean flag = isDispatching.get();
                        if (flag.get()) return;
                        flag.set(true);

                        try {
                            // ⭐ A Nova Assinatura Elegante: Apenas 3 argumentos úteis
                            List<Expr.CallArg> cbArgs = new ArrayList<>();
                            cbArgs.add(new Expr.CallArg(null, new Expr.Literal(evento)));
                            cbArgs.add(new Expr.CallArg(null, new Expr.Literal(velho)));
                            cbArgs.add(new Expr.CallArg(null, new Expr.Literal(novo)));

                            callback.call(callbackInterpreter, cbArgs);
                        }
                        catch (ControlFlow.RuntimeError e) {
                            if (!e.getMessage().contains("Tipo de Retorno Inválido")) {
                                System.err.println("[Observable] Erro de Lógica (" + nome + "): " + e.getMessage());
                            }
                        }
                        catch (Exception e) {
                            System.err.println("[Observable] Crash Interno (" + nome + "): " + e.getMessage());
                        } finally {
                            flag.set(false);
                        }
                    }

                    @Override public void onVariableDeclared(String name, Object value, String scopeType) {
                        dispararXpl("CREATE", name, null, value, scopeType);
                    }
                    @Override public void onVariableMutated(String name, Object oldValue, Object newValue) {
                        dispararXpl("UPDATE", name, oldValue, newValue, "mut");
                    }
                    @Override public void onVariableRead(String name, Object value) {
                        dispararXpl("READ", name, value, value, "read");
                    }
                    @Override public void onVariableRemove(String name, Object value) {
                        dispararXpl("DELETE", name, value, null, "del");
                    }
                };

                // ⭐ A CURA DOS ZOMBIES: Acopla o espião APENAS a esta execução do Interpretador
                i.globals.addListener(listener);
                myListeners.add(listener);
                return true;
            }));

            // ---------------------------------------------------------
            // Método: observable.stop()
            // ---------------------------------------------------------
            obsInstance.fields.put("stop", buildAction(0, (i, a) -> {
                for (Environment.XplEnvironmentListener l : myListeners) {
                    i.globals.removeListener(l);
                }
                myListeners.clear();
                return true;
            }));

            return obsInstance;
        }));

        interpreter.registry_model.put("Observer", model);
        interpreter.environment.defineConst("Observer", new XplClass(model, interpreter.globals));
    }

    // --- A NOVA TAXONOMIA ESTRUTURAL ---
    private static String getStructuralType(Object obj) {
        if (obj instanceof XplInterface) return "interface";
        if (obj instanceof XPLModel) return "declare";
        if (obj instanceof XplClass) return "class";
        if (obj instanceof XplInstance) return "instance";
        if (obj instanceof XplCallable) return "function";
        return "variable"; // Cobre Tipos Primitivos, Arrays e Maps
    }

    @FunctionalInterface private interface NativeAction { Object execute(Interpreter i, List<Expr.CallArg> a); }
    private static XplCallable buildAction(int arity, NativeAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return action.execute(intp, args);
            }
        };
    }
}