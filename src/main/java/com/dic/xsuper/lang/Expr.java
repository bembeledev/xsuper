package com.dic.xsuper.lang;

import java.util.List;

public abstract class Expr {

    // O "Pattern Matching" do Java (Visitor Pattern)
    public interface Visitor<R> {
        R visitBinaryExpr(Binary expr);
        R visitUnaryExpr(Unary expr);
        R visitLiteralExpr(Literal expr);
        R visitVariableExpr(Variable expr);
        R visitAssignExpr(Assign expr);
        R visitCallExpr(Call expr);
        R visitArrayExpr(ArrayLiteral expr);
        R visitCompoundAssignExpr(CompoundAssign expr);
        R visitUpdateExpr(Update expr);
        R visitIndexAccessExpr(IndexAccess expr);
        R visitIndexAssignExpr(IndexAssign expr);
        R visitGetExpr(Get expr);
        R visitArrowFunctionExpr(ArrowFunction expr);
        R visitObjectLiteralExpr(ObjectLiteral expr);
        R visitNewExpr(New expr);
        Object visitSetExpr(Set expr);
    }


    public abstract <R> R accept(Visitor<R> visitor);

    // --- Tipos de Expressões ---

    public static class Binary extends Expr {
        public final Expr left;
        public final Token operator;
        public final Expr right;

        public Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitBinaryExpr(this); }

        @Override
        public String toString() {
            return "Binary{" +
                    "left=" + left +
                    ", operator=" + operator +
                    ", right=" + right +
                    '}';
        }
    }

    public static class Unary extends Expr {
        public final Token operator;
        public final Expr right;

        public Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitUnaryExpr(this); }

        @Override
        public String toString() {
            return "Unary{" +
                    "operator=" + operator +
                    ", right=" + right +
                    '}';
        }
    }

    public static class Literal extends Expr {
        public final Object value;

        public Literal(Object value) {
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitLiteralExpr(this); }

        @Override
        public String toString() {
            return "Literal{" +
                    "value=" + value +
                    '}';
        }
    }

    public static class Variable extends Expr {
        public final Token name;

        public Variable(Token name) {
            this.name = name;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitVariableExpr(this); }

        @Override
        public String toString() {
            return "Variable{" +
                    "name=" + name +
                    '}';
        }
    }

    public static class Assign extends Expr {
        public final Token name;
        public final Expr value;

        public Assign(Token name, Expr value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitAssignExpr(this); }

        @Override
        public String toString() {
            return "Assign{" +
                    "name=" + name +
                    ", value=" + value +
                    '}';
        }
    }

    public static class Call extends Expr {
        public final Expr callee; // O nome da função
        public final Token paren; // O parêntesis de fecho (para indicar a linha em caso de erro)
        public final List<Expr> arguments;

        public Call(Expr callee, Token paren, List<Expr> arguments) {
            this.callee = callee;
            this.paren = paren;
            this.arguments = arguments;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitCallExpr(this); }

        @Override
        public String toString() {
            return "Call{" +
                    "callee=" + callee +
                    ", paren=" + paren +
                    ", arguments=" + arguments +
                    '}';
        }
    }

    public static class ArrayLiteral extends Expr {
        public final List<Expr> elements;

        public ArrayLiteral(List<Expr> elements) {
            this.elements = elements;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitArrayExpr(this); }

        @Override
        public String toString() {
            return "ArrayLiteral{" +
                    "elements=" + elements +
                    '}';
        }
    }
    // Nó para a += 5, a *= 2, etc.
    public static class CompoundAssign extends Expr {
        public final Token name;
        public final Token operator;
        public final Expr value;

        public CompoundAssign(Token name, Token operator, Expr value) {
            this.name = name;
            this.operator = operator;
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitCompoundAssignExpr(this); }

        @Override
        public String toString() {
            return "CompoundAssign{" +
                    "name=" + name +
                    ", operator=" + operator +
                    ", value=" + value +
                    '}';
        }
    }

    // Nó para a++, a--
    public static class Update extends Expr {
        public final Token name;
        public final Token operator;
        public final boolean isPrefix; // false para a++, true para ++a (se quiseres no futuro)

        public Update(Token name, Token operator, boolean isPrefix) {
            this.name = name;
            this.operator = operator;
            this.isPrefix = isPrefix;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitUpdateExpr(this); }

        @Override
        public String toString() {
            return "Update{" +
                    "name=" + name +
                    ", operator=" + operator +
                    ", isPrefix=" + isPrefix +
                    '}';
        }
    }

    // Nó para: lista[0]
    public static class IndexAccess extends Expr {
        public final Expr object;  // Ex: lista
        public final Token bracket; // O ']' para mostrar erros se falhar
        public final Expr index;    // Ex: 0

        public IndexAccess(Expr object, Token bracket, Expr index) {
            this.object = object;
            this.bracket = bracket;
            this.index = index;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitIndexAccessExpr(this); }

        @Override
        public String toString() {
            return "IndexAccess{" +
                    "object=" + object +
                    ", bracket=" + bracket +
                    ", index=" + index +
                    '}';
        }
    }

    // Nó para: lista[0] = 99
    public static class IndexAssign extends Expr {
        public final Expr object;
        public final Token bracket;
        public final Expr index;
        public final Expr value; // Ex: 99

        public IndexAssign(Expr object, Token bracket, Expr index, Expr value) {
            this.object = object;
            this.bracket = bracket;
            this.index = index;
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitIndexAssignExpr(this); }

        @Override
        public String toString() {
            return "IndexAssign{" +
                    "object=" + object +
                    ", bracket=" + bracket +
                    ", index=" + index +
                    ", value=" + value +
                    '}';
        }
    }
    // Nó para: objeto.propriedade (ex: arr.push)
    public static class Get extends Expr {
        public final Expr object; // O que está à esquerda do ponto (ex: arr)
        public final Token name;  // O nome do método à direita (ex: push)

        public Get(Expr object, Token name) {
            this.object = object;
            this.name = name;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitGetExpr(this); }

        @Override
        public String toString() {
            return "Get{" +
                    "object=" + object +
                    ", name=" + name +
                    '}';
        }
    }

    public static class ArrowFunction extends Expr {
        public final Token parameter; // Por agora suportamos 1 parâmetro: e => ...
        public final Expr body;       // O que ela retorna: e.toUpperCase()

        public ArrowFunction(Token parameter, Expr body) {
            this.parameter = parameter;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitArrowFunctionExpr(this); }

        @Override
        public String toString() {
            return "ArrowFunction{" +
                    "parameter=" + parameter +
                    ", body=" + body +
                    '}';
        }
    }

    public static class ObjectLiteral extends Expr {
        public final List<Expr> keys;
        public final List<Expr> values;

        public ObjectLiteral(List<Expr> keys, List<Expr> values) {
            this.keys = keys;
            this.values = values;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitObjectLiteralExpr(this); }

        @Override
        public String toString() {
            return "ObjectLiteral{" +
                    "keys=" + keys +
                    ", values=" + values +
                    '}';
        }
    }

    public static class New extends Expr {
        public final Token keyword;
        public final Token className;
        public final List<Expr> arguments;
        public final String typeArguments;

        public New(Token keyword, Token className, String typeArguments, List<Expr> arguments) {
            this.keyword = keyword;
            this.className = className;
            this.arguments = arguments;
            this.typeArguments = typeArguments;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitNewExpr(this); }

        @Override
        public String toString() {
            return "New{" +
                    "keyword=" + keyword +
                    ", className=" + className +
                    ", arguments=" + arguments +
                    ", typeArguments='" + typeArguments + '\'' +
                    '}';
        }
    }

    public static class Set extends Expr {
        public final Expr object;
        public final Token name;
        public final Expr value;

        public Set(Expr object, Token name, Expr value) {
            this.object = object;
            this.name = name;
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return (R) visitor.visitSetExpr(this);
        }

        @Override
        public String toString() {
            return "Set{" +
                    "object=" + object +
                    ", name=" + name +
                    ", value=" + value +
                    '}';
        }
    }
}