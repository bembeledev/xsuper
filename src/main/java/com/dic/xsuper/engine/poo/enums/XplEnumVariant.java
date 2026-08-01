package com.dic.xsuper.engine.poo.enums;

import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.ast.Expr;
import java.util.*;

// Uma Variante de Enum age como uma Função (para construir) E como um Objeto (para aceder)
public class XplEnumVariant implements XplCallable {
    public final String enumName;
    public final String variantName;
    public final List<String> paramNames;

    // Onde os dados ficam guardados após a variante ser "invocada"
    public final Map<String, Object> data = new HashMap<>();

    public XplEnumVariant(String enumName, String variantName, List<String> paramNames) {
        this.enumName = enumName;
        this.variantName = variantName;
        this.paramNames = paramNames;
    }

    @Override
    public int arity() {
        return paramNames.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Expr.CallArg> args) {
        // Quando fazem Resposta.ERRO(404, "Erro"), geramos uma NOVA instância da variante com os dados!
        XplEnumVariant instance = new XplEnumVariant(enumName, variantName, paramNames);

        for (int i = 0; i < args.size(); i++) {
            // Avalia o argumento passado e guarda-o com o nome do parâmetro da variante
            Object val = interpreter.evaluate(args.get(i).expression);
            instance.data.put(paramNames.get(i), val);
        }
        return instance;
    }

    // Permite fazer `meuEstado.codigo`
    public Object getProperty(String name) {
        if (data.containsKey(name)) return data.get(name);
        throw new RuntimeException("A variante '" + variantName + "' não tem a propriedade '" + name + "'.");
    }

    @Override
    public String toString() {
        if (data.isEmpty()) return enumName + "." + variantName;
        return enumName + "." + variantName + "(" + data.values() + ")";
    }
}