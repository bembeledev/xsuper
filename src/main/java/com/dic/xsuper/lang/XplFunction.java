package com.dic.xsuper.lang;

import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.poo.XplInstance;

import java.util.List;

public class XplFunction implements XplCallable {
    public  final Stmt.Function declaration;
    private final Environment closure; // Guarda o escopo onde a função foi criada
    private final XPLModel ownerModel;

    public XplFunction(Stmt.Function declaration, Environment closure, XPLModel ownerModel) {
        this.declaration = declaration;
        this.closure = closure;
        this.ownerModel = ownerModel;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, java.util.List<Expr.CallArg> passedArgs) {
        Environment environment = new Environment(this.closure);

        java.util.List<Stmt.Param> declaredParams = this.declaration.params;

        // ⭐ A FILA DOS SOBREVIVENTES: Contém a ordem exata da assinatura original
        java.util.LinkedList<Stmt.Param> remainingParams = new java.util.LinkedList<>(declaredParams);
        java.util.Map<String, Object> resolvedValues = new java.util.HashMap<>();

        // =====================================================================
        // FASE 1: EXTRAIR OS ARGUMENTOS NOMEADOS PRIMEIRO
        // =====================================================================
        for (Expr.CallArg arg : passedArgs) {
            if (arg.name != null) {
                String pName = arg.name.lexeme;

                Stmt.Param targetParam = findParamByName(declaredParams, pName);
                if (targetParam == null) {
                    throw new ControlFlow.RuntimeError(arg.name,
                            "Parâmetro Inexistente: A função '" + this.declaration.name.lexeme +
                                    "' não possui nenhum parâmetro chamado '" + pName + "'.");
                }

                if (resolvedValues.containsKey(pName)) {
                    throw new ControlFlow.RuntimeError(arg.name,
                            "Parâmetro Duplicado: O argumento '" + pName + "' foi passado mais do que uma vez.");
                }

                Object evalVal = interpreter.evaluate(arg.expression);

                // Validação de Tipagem contra o nó da AST
                if (!interpreter.checkTypeMatch(evalVal, targetParam.typeNode)) {
                    throw new ControlFlow.RuntimeError(arg.name,
                            "Erro de Tipo: O parâmetro '" + pName + "' esperava " +
                                    targetParam.typeNode.toString() + ", mas recebeu " + evalVal.getClass().getSimpleName());
                }

                resolvedValues.put(pName, evalVal);
                remainingParams.remove(targetParam); // Elimina da fila de sobreviventes!
            }
        }

        // =====================================================================
        // FASE 2: DISTRIBUIR OS POSICIONAIS PELA ORDEM DO QUE SOBROU
        // =====================================================================
        for (Expr.CallArg arg : passedArgs) {
            if (arg.name == null) { // É posicional (anónimo)

                // ⭐ A CORRECÇÃO: Em vez de atirar erro, se não há ranhuras, simplesmente ignoramos!
                if (remainingParams.isEmpty()) {
                    // Ignora este argumento extra (common pattern em Higher-Order Functions)
                    continue;
                }

                Stmt.Param targetSlot = remainingParams.removeFirst();
                Object evalVal = interpreter.evaluate(arg.expression);

                if (!interpreter.checkTypeMatch(evalVal, targetSlot.typeNode)) {
                    throw new ControlFlow.RuntimeError(this.declaration.name,
                            "Erro de Tipo no argumento posicional para '" + targetSlot.name.lexeme +
                                    "': esperava " + targetSlot.typeNode.toString() + ", mas recebeu " + evalVal.getClass().getSimpleName());
                }

                resolvedValues.put(targetSlot.name.lexeme, evalVal);
            }
        }

        // =====================================================================
        // FASE 3: AUDITORIA AOS SOBREVIVENTES FINAIS (Opcionais e Defaults)
        // =====================================================================
        for (Stmt.Param orfao : remainingParams) {
            String pName = orfao.name.lexeme;

            // Regra A: Tinha valor padrão? (telefone = "825...")
            if (orfao.defaultValue != null) {
                resolvedValues.put(pName, interpreter.evaluate(orfao.defaultValue));
            }
            // Regra B: Era um opcional quântico? (?string)
            else if (orfao.typeNode instanceof TypeNode.Optional) {
                resolvedValues.put(pName, null);
            }
            // Tragédia: Era obrigatório e ninguém o preencheu!
            else {
                throw new ControlFlow.RuntimeError(this.declaration.name,
                        "Argumento Obrigatório Ausente: O parâmetro '" + pName + "' não possui valor padrão e não foi fornecido.");
            }
        }

        // Despeja o mapa perfeitamente alinhado na memória local da função
        for (Stmt.Param p : declaredParams) {
            environment.defineLet(p.name.lexeme, resolvedValues.get(p.name.lexeme));
        }

        try {
            interpreter.executeBlock(this.declaration.body, environment);
        } catch (ControlFlow.ReturnException returnPacket) {
            Object valorDeRetorno = returnPacket.value;

            // ⭐ LEI 1: O Retorno Intruso (Função void que tenta devolver matéria)
            if (this.declaration.returnType == null) {
                if (valorDeRetorno != null) {
                    throw new ControlFlow.RuntimeError(this.declaration.name,
                            "Quebra de Contrato: A função '" + this.declaration.name.lexeme +
                                    "' foi declarada sem tipo de retorno (void), mas tentou retornar um valor do tipo " +
                                    valorDeRetorno.getClass().getSimpleName() + ".");
                }
                return null; // Retornou um 'return;' vazio legítimo num void.
            }

            // ⭐ LEI 3: Auditoria de Tipagem Rigorosa
            if (!interpreter.checkTypeMatch(valorDeRetorno, this.declaration.returnType)) {
                throw new ControlFlow.RuntimeError(this.declaration.name,
                        "Tipo de Retorno Inválido: A função '" + this.declaration.name.lexeme +
                                "' devolveu " + (valorDeRetorno == null ? "null" : valorDeRetorno.getClass().getSimpleName()) +
                                ", mas a assinatura exigia " + this.declaration.returnType.toString() + ".");
            }
            return valorDeRetorno;
        }

        // ⭐ LEI 2: A Promessa Falhada (Atingiu o '}' final sem disparar nenhum return!)
        if (this.declaration.returnType != null && !(this.declaration.returnType instanceof TypeNode.Optional)) {
            throw new ControlFlow.RuntimeError(this.declaration.name,
                    "Falta de Retorno: A função '" + this.declaration.name.lexeme +
                            "' exige um retorno obrigatório do tipo " + this.declaration.returnType.toString() +
                            ", mas a execução atingiu o fim do bloco sem retornar nenhum valor.");
        }

        return null;
    }

    private Stmt.Param findParamByName(java.util.List<Stmt.Param> list, String name) {
        for (Stmt.Param p : list) if (p.name.lexeme.equals(name)) return p;
        return null;
    }

    @Override
    public String toString() {
        return "<fun " + declaration.name.lexeme + ">";
    }

    // ⭐ Cria uma nova versão da função com o 'this' injetado no escopo!
    public XplFunction bind(XplInstance instance) {
        Environment environment = new Environment(closure);
        environment.defineConst("this", instance);
        // ⭐ O SEGREDO DO SUPER: Guardamos em que nível da árvore genealógica estamos!
        if (ownerModel != null) {
            environment.defineConst("__current_model", ownerModel);
        }
        return new XplFunction(declaration, environment, ownerModel);
    }
}