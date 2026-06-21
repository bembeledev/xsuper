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
        R visitSuperExpr(Super expr);
        R visitCastExpr(Cast expr);
        R visitTypeCheckExpr(TypeCheck typeCheck);
        R visitTypeofExpr(Typeof typeof);
        R visitIfExpr(If expr);
        R visitLogicalExpr(Logical logical);
        R visitSwitchExpr(Switch expr);
        R visitMatchExpr(Match expr);
        R visitUnwrapExpr(Unwrap unwrap);
        R visitNullCoalesceExpr(NullCoalesce nullCoalesce);
        R visitOptionalChainingExpr(OptionalChaining optionalChaining);
        R visitOptionalCallExpr(OptionalCall optionalCall);
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

    // ⭐ ARGUMENTO BIVALENTE DE CHAMADA (Pode ser posicional ou nomeado)
    public static class CallArg {
        public final Token name; // null se for posicional (ex: "82570"), preenchido se for nomeado (ex: nome: "Nelson")
        public final Expr expression;

        public CallArg(Token name, Expr expression) {
            this.name = name;
            this.expression = expression;
        }

        @Override
        public String toString() {
            return "CallArg{" +
                    "name=" + name +
                    ", expression=" + expression +
                    '}';
        }
    }

    public static class Call extends Expr {
        public final Expr callee; // O nome da função
        public final Token paren; // O parêntesis de fecho (para indicar a linha em caso de erro)
        public final java.util.List<CallArg> arguments;

        public Call(Expr callee, Token paren, List<CallArg> arguments) {
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

    // Nó para a += 5, arr[0] += 2, Animal.INSTANCE += 1
    public static class CompoundAssign extends Expr {
        public final Expr target; // ⭐ MUDOU: Agora é uma Expressão (Alvo)!
        public final Token operator;
        public final Expr value;

        public CompoundAssign(Expr target, Token operator, Expr value) {
            this.target = target;
            this.operator = operator;
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitCompoundAssignExpr(this); }

        @Override
        public String toString() {
            return "CompoundAssign{" +
                    "target=" + target +
                    ", operator=" + operator +
                    ", value=" + value +
                    '}';
        }
    }

    // Nó para a++, arr[0]++, Animal.INSTANCE++
    public static class Update extends Expr {
        public final Expr target; // ⭐ MUDOU: Agora é uma Expressão (Alvo)!
        public final Token operator;
        public final boolean isPrefix;

        public Update(Expr target, Token operator, boolean isPrefix) {
            this.target = target;
            this.operator = operator;
            this.isPrefix = isPrefix;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitUpdateExpr(this); }

        @Override
        public String toString() {
            return "Update{" +
                    "target=" + target +
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
        public final String typeArguments;
        public final List<CallArg> arguments; // ⭐ PROMOVIDO de List<Expr> para List<CallArg>!

        public New(Token keyword, Token className, String typeArguments, List<CallArg> arguments) {
            this.keyword = keyword;
            this.className = className;
            this.arguments = arguments;
            this.typeArguments = typeArguments;
        }
        @Override public <R> R accept(Visitor<R> visitor) { return visitor.visitNewExpr(this); }

        @Override
        public String toString() {
            return "New{" +
                    "keyword=" + keyword +
                    ", className=" + className +
                    ", typeArguments='" + typeArguments + '\'' +
                    ", arguments=" + arguments +
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

    public static class Super extends Expr {
        public final Token keyword; // O 'super'
        public final Token method;  // O método (ex: 'init')

        public Super(Token keyword, Token method) {
            this.keyword = keyword;
            this.method = method;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitSuperExpr(this); }

        @Override
        public String toString() {
            return "Super{method=" + method.lexeme + "}";
        }
    }

    // ⭐ NOVO: O nó para o typeof(expr)
    public static class Typeof extends Expr {
        public final Token keyword;
        public final Expr expression;

        public Typeof(Token keyword, Expr expression) {
            this.keyword = keyword;
            this.expression = expression;
        }
        @Override public <R> R accept(Visitor<R> visitor) { return visitor.visitTypeofExpr(this); }

        @Override
        public String toString() {
            return "Typeof{" +
                    "keyword=" + keyword +
                    ", expression=" + expression +
                    '}';
        }
    }

    // ⭐ NOVO: O nó para 'expr type Tipo' e 'expr instance Tipo'
    public static class TypeCheck extends Expr {
        public final Expr left;
        public final Token operator; // Guarda o token 'type' ou 'instance'
        public final TypeNode rightType;

        public TypeCheck(Expr left, Token operator, TypeNode rightType) {
            this.left = left;
            this.operator = operator;
            this.rightType = rightType;
        }
        @Override public <R> R accept(Visitor<R> visitor) { return visitor.visitTypeCheckExpr(this); }

        @Override
        public String toString() {
            return "TypeCheck{" +
                    "left=" + left +
                    ", operator=" + operator +
                    ", rightType=" + rightType +
                    '}';
        }
    }

    // ⭐ NOVO NÓ DE CONVERSÃO (CAST) ⭐
    public static class Cast extends Expr {
        public final Expr value;
        public final Token operator; // Guardamos o token 'as' para dar erros precisos
        public final TypeNode type;  // O tipo de destino (int, float, string, etc.)
        public final boolean isForced;

        public Cast(Expr value, Token operator, TypeNode type, boolean isForced) {
            this.value = value;
            this.operator = operator;
            this.type = type;
            this.isForced = isForced;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitCastExpr(this); }

        @Override
        public String toString() {
            return "Cast{" +
                    "value=" + value +
                    ", operator=" + operator +
                    ", type=" + type +
                    ", isForced=" + isForced +
                    '}';
        }
    }

    // ⭐ O IF PROMOVIDO A EXPRESSÃO ⭐
    public static class If extends Expr {
        public final Expr condition;
        public final Stmt thenBranch; // Usamos Stmt para suportar tanto Blocos {} como simples Expressões!
        public final Stmt elseBranch; // Pode ser null

        public If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitIfExpr(this); }

        @Override
        public String toString() {
            return "If{" +
                    "condition=" + condition +
                    ", thenBranch=" + thenBranch +
                    ", elseBranch=" + elseBranch +
                    '}';
        }
    }

    public static class Logical extends Expr {
        // ⭐ AS VARIÁVEIS ONDE GUARDAMOS OS DADOS ⭐
        public final Expr left;      // O lado esquerdo (ex: a > 10)
        public final Token operator;  // O token '&&' ou '||'
        public final Expr right;     // O lado direito (ex: b < 5)

        public Logical(Expr expr, Token operator, Expr right) {
            this.left = expr;        // Guardamos o teu 'expr' no campo 'left'
            this.operator = operator;
            this.right = right;
        }

        // ⭐ A PORTA DE ENTRADA DO INTERPRETADOR (Visitor) ⭐
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLogicalExpr(this);
        }

        @Override
        public String toString() {
            return "Logical{" +
                    "left=" + left +
                    ", operator=" + operator +
                    ", right=" + right +
                    '}';
        }
    }

    // ⭐ O AUXILIAR: Representa uma linha 'case 1, 2: bloco;' ⭐
    public static class SwitchCase {
        public final java.util.List<Expr> values; // Valores a testar (ex: 1, 2)
        public final Stmt body;                   // O código a executar

        public SwitchCase(java.util.List<Expr> values, Stmt body) {
            this.values = values;
            this.body = body;
        }

        @Override
        public String toString() {
            return "SwitchCase{" +
                    "values=" + values +
                    ", body=" + body +
                    '}';
        }
    }

    // ⭐ O NÓ PRINCIPAL: O Switch Orientado a Expressão ⭐
    public static class Switch extends Expr {
        public final Expr target;
        public final java.util.List<SwitchCase> cases;
        public final Stmt defaultBranch; // Pode ser null se não houver 'default:'

        public Switch(Expr target, java.util.List<SwitchCase> cases, Stmt defaultBranch) {
            this.target = target;
            this.cases = cases;
            this.defaultBranch = defaultBranch;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitSwitchExpr(this); }

        @Override
        public String toString() {
            return "Switch{" +
                    "target=" + target +
                    ", cases=" + cases +
                    ", defaultBranch=" + defaultBranch +
                    '}';
        }
    }

    // ⭐ O BRAÇO DO MATCH ⭐
    public static class MatchArm {
        public final TypeNode typeTest; // Ex: 'type String' (Pode ser null)
        public final Expr valueTest;    // Ex: '200' ou '"OK"' (Pode ser null)
        public final Expr guard;        // Ex: 'if (x > 10)' (Pode ser null)
        public final Stmt body;         // O código a executar

        public MatchArm(TypeNode typeTest, Expr valueTest, Expr guard, Stmt body) {
            this.typeTest = typeTest;
            this.valueTest = valueTest;
            this.guard = guard;
            this.body = body;
        }

        @Override
        public String toString() {
            return "MatchArm{" +
                    "typeTest=" + typeTest +
                    ", valueTest=" + valueTest +
                    ", guard=" + guard +
                    ", body=" + body +
                    '}';
        }
    }

    // ⭐ O COLOSSO: A Expressão Match ⭐
    public static class Match extends Expr {
        public final Expr target;
        public final java.util.List<MatchArm> arms;
        public final Stmt defaultBranch; // O 'default:' ou 'none:'

        public Match(Expr target, java.util.List<MatchArm> arms, Stmt defaultBranch) {
            this.target = target;
            this.arms = arms;
            this.defaultBranch = defaultBranch;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitMatchExpr(this); }

        @Override
        public String toString() {
            return "Match{" +
                    "target=" + target +
                    ", arms=" + arms +
                    ", defaultBranch=" + defaultBranch +
                    '}';
        }
    }


    // ⭐ 1. COALESCÊNCIA NULA ( a ?? b )
    public static class NullCoalesce extends Expr {
        public final Expr left;
        public final Token operator;
        public final Expr right;

        public NullCoalesce(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }
        @Override public <R> R accept(Visitor<R> visitor) { return visitor.visitNullCoalesceExpr(this); }

        @Override
        public String toString() {
            return "NullCoalesce{" +
                    "left=" + left +
                    ", operator=" + operator +
                    ", right=" + right +
                    '}';
        }
    }

    // ⭐ 2. ENCADEAMENTO OPCIONAL DE PROPRIEDADE ( obj?.nome )
    public static class OptionalChaining extends Expr {
        public final Expr object;
        public final Token name;

        public OptionalChaining(Expr object, Token name) {
            this.object = object;
            this.name = name;
        }
        @Override public <R> R accept(Visitor<R> visitor) { return visitor.visitOptionalChainingExpr(this); }

        @Override
        public String toString() {
            return "OptionalChaining{" +
                    "object=" + object +
                    ", name=" + name +
                    '}';
        }
    }

    // ⭐ 3. CHAMADA OPCIONAL DE MÉTODO ( obj?.limpar() )
    public static class OptionalCall extends Expr {
        public final Expr object;
        public final Token methodName;
        public final Token paren;
        public final List<CallArg> arguments; // ⭐ PROMOVIDO de List<Expr> para List<CallArg>!

        public OptionalCall(Expr object, Token methodName, Token paren, List<CallArg> arguments) {
            this.object = object;
            this.methodName = methodName;
            this.paren = paren;
            this.arguments = arguments;
        }
        @Override public <R> R accept(Visitor<R> visitor) { return visitor.visitOptionalCallExpr(this); }

        @Override
        public String toString() {
            return "OptionalCall{" +
                    "object=" + object +
                    ", methodName=" + methodName +
                    ", paren=" + paren +
                    ", arguments=" + arguments +
                    '}';
        }
    }

    // ⭐ 4. UNWRAP FORÇADO ( obj! )
    public static class Unwrap extends Expr {
        public final Expr expr;
        public final Token operator;

        public Unwrap(Expr expr, Token operator) {
            this.expr = expr;
            this.operator = operator;
        }
        @Override public <R> R accept(Visitor<R> visitor) { return visitor.visitUnwrapExpr(this); }

        @Override
        public String toString() {
            return "Unwrap{" +
                    "expr=" + expr +
                    ", operator=" + operator +
                    '}';
        }
    }


}