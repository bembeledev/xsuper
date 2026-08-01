package com.dic.xsuper.engine.poo.enums;

import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.ast.Expr;
import java.util.*;

public class XplEnum {
    public final String name;
    public final Map<String, XplEnumVariant> variants = new LinkedHashMap<>();

    public XplEnum(String name) {
        this.name = name;
    }

    public void addVariant(XplEnumVariant variant) {
        variants.put(variant.variantName, variant);
    }

    // Permite fazer Resposta.SUCESSO ou Estado.keys()
    public Object get(String variantName) {

        // Propriedade pura (Sem parênteses: Estado.size)
        switch (variantName) {
            case "size" -> {
                return (double) variants.size();
            }

            // ⭐ FUNÇÃO NATIVA: Estado.keys()
            case "keys" -> {
                return new XplCallable() {
                    @Override
                    public int arity() {
                        return 0;
                    }

                    @Override
                    public Object call(Interpreter interpreter, List<Expr.CallArg> args) {
                        return new ArrayList<>(variants.keySet());
                    }

                    @Override
                    public String toString() {
                        return "<native fn keys>";
                    }
                };
            }


            // ⭐ FUNÇÃO NATIVA: Estado.has("...")
            case "has" -> {
                return new XplCallable() {
                    @Override
                    public int arity() {
                        return 1;
                    }

                    @Override
                    public Object call(Interpreter interpreter, List<Expr.CallArg> args) {
                        Object arg = interpreter.evaluate(args.getFirst().expression);
                        return variants.containsKey(String.valueOf(arg));
                    }

                    @Override
                    public String toString() {
                        return "<native fn has>";
                    }
                };
            }
        }

        // Se for uma Variante (Ex: Estado.PENDENTE ou Resposta.ERRO)
        if (variants.containsKey(variantName)) {
            return variants.get(variantName);
        }

        throw new RuntimeException("Enum '" + name + "' não possui a propriedade ou variante '" + variantName + "'.");
    }

    @Override
    public String toString() {
        return "<enum " + name + ">";
    }
}