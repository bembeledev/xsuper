package com.dic.xsuper.engine.ast;

import com.dic.xsuper.engine.core.Token;
import com.dic.xsuper.engine.core.TokenType;

import java.util.List;

public abstract class TypeNode {
    public final Token name; // O Token principal (ex: int, String, Map)

    protected TypeNode(Token name) {
        this.name = name;
    }

    // 1. Tipo Simples (ex: let idade: int)
    public static class Simple extends TypeNode {
        public Simple(Token name) {
            super(name);
        }

        @Override
        public String toString() {
            return name.lexeme;
        }
    }

    // 2. Tipo Genérico (ex: let mapa: Map<String, Integer>)
    public static class Generic extends TypeNode {
        public final List<TypeNode> typeArguments; // A lista de tipos dentro de < >

        public Generic(Token name, List<TypeNode> typeArguments) {
            super(name);
            this.typeArguments = typeArguments;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(name.lexeme).append("<");
            for (int i = 0; i < typeArguments.size(); i++) {
                sb.append(typeArguments.get(i).toString());
                if (i < typeArguments.size() - 1) sb.append(", ");
            }
            sb.append(">");
            return sb.toString();
        }
    }

    // ⭐ NOVO: Representa um tipo embrulhado em '?' (Ex: ?string)
    public static class Optional extends TypeNode {
        public final TypeNode innerType;

        public Optional(TypeNode innerType) {
            // ⭐ A SACADA DE MESTRE: Alimentamos o Pai com o Token do recheio!
            // Se o innerType for 'string', o Optional herda o nome 'string' na base.
            super(innerType.name);

            this.innerType = innerType;
        }

        @Override
        public String toString() {
            return "?" + innerType.toString();
        }
    }

    // =========================================================================
    // ⭐ NOVO: O TIPO FUNÇÃO MATURO (Ex: (int, string) -> bool)
    // =========================================================================
    public static class FunctionType extends TypeNode {
        public final java.util.List<TypeNode> paramTypes;
        public final TypeNode returnType; // Pode ser nulo (void)

        public FunctionType(java.util.List<TypeNode> paramTypes, TypeNode returnType) {
            // Chamamos o super com um Token sintético neutro (ex: "Callable")
            // apenas para satisfazer a hierarquia herdada.
            super(new Token(TokenType.IDENTIFIER, "Callable", null, 0, 0));
            this.paramTypes = paramTypes;
            this.returnType = returnType;
        }

        // Método utilitário vital para imprimir o tipo em mensagens de erro!
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("(");
            for (int i = 0; i < paramTypes.size(); i++) {
                sb.append(paramTypes.get(i) == null ? "any" : paramTypes.get(i).name.lexeme);
                if (i < paramTypes.size() - 1) sb.append(", ");
            }
            sb.append(") -> ");
            sb.append(returnType == null ? "void" : returnType.name.lexeme);
            return sb.toString();
        }
    }
}