package com.dic.xsuper.lang;

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
}