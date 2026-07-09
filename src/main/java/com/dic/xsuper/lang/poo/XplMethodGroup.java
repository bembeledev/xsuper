package com.dic.xsuper.lang.poo;

import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;
import com.dic.xsuper.lang.XplFunction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XplMethodGroup implements XplCallable {
    private final String name;

    // ⭐ A VTable Viva (Assinatura -> Função Executável)
    private final Map<String, XplFunction> signatures = new HashMap<>();

    public XplMethodGroup(String name) {
        this.name = name;
    }

    public void addSignature(String signature, XplFunction function) {
        this.signatures.put(signature, function);
    }

    // ⭐ BINDING LÉXICO: Vincula o 'this' (A Instância) a todas as sobrecargas
    public XplMethodGroup bind(XplInstance instance) {
        XplMethodGroup boundGroup = new XplMethodGroup(this.name);
        for (Map.Entry<String, XplFunction> entry : this.signatures.entrySet()) {
            boundGroup.addSignature(entry.getKey(), entry.getValue().bind(instance));
        }
        return boundGroup;
    }

    @Override
    public int arity() {
        return -1; // Desliga a verificação de aridade estrita do Interpretador base
    }

    // ⭐ A TUA ASSINATURA EXATA COM CallArg ⭐
    @Override
    public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
        // 1. Quantos CallArgs recebemos?
        int providedArity = arguments.size();

        // 2. Qual é a Assinatura (Descritor) requerida?
        String targetSignature = this.name + "@" + providedArity;

        // 3. Procura na VTable (Lookup Hash O(1))
        XplFunction targetMethod = signatures.get(targetSignature);

        if (targetMethod != null) {
            // Delega a execução intocada para a função real
            return targetMethod.call(interpreter, arguments);
        }

        // Se não encontrar, lança um erro com a precisão do Java!
        throw new RuntimeException("Erro de Método: A assinatura '" + targetSignature +
                "' não foi encontrada. Sobrecargas disponíveis: " + signatures.keySet());
    }
}