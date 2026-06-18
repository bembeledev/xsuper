package com.dic.xsuper.lang;

import java.util.List;
import java.util.Optional;

public abstract class Stmt {

    public interface Visitor<R> {
        R visitExpressionStmt(ExpressionStmt stmt);
        R visitVarDeclStmt(VarDecl stmt);
        R visitBlockStmt(Block stmt);
        R visitIfStmt(If stmt);
        R visitForCStyleStmt(ForCStyle stmt);
        R visitFunctionStmt(Function stmt);
        R visitBreakStmt(Break stmt);
        R visitContinueStmt(Continue stmt);
        R visitReturnStmt(Return aReturn);
        R visitForInRangeStmt(ForInRange forInRange);
        R visitForInStmt(ForIn forIn);
        R visitEnumStmt(Enum stmt);
    }

    public abstract <R> R accept(Visitor<R> visitor);

    // --- Tipos de Declarações ---

    public static class ExpressionStmt extends Stmt {
        public final Expr expression;

        public ExpressionStmt(Expr expression) {
            this.expression = expression;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitExpressionStmt(this); }

        @Override
        public String toString() {
            return "ExpressionStmt{" +
                    "expression=" + expression +
                    '}';
        }
    }

    public static class VarDecl extends Stmt {
        public final Token keyword; // let, var ou const
        public final Token name;
        public final Token typeAnnotation; // Novo: Guarda o token do tipo (int, float, etc.) ou null se for implícito
        public final Expr initializer;

        public VarDecl(Token keyword, Token name, Token typeAnnotation, Expr initializer) {
            this.keyword = keyword;
            this.name = name;
            this.typeAnnotation = typeAnnotation;
            this.initializer = initializer;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitVarDeclStmt(this); }

        @Override
        public String toString() {
            return "VarDecl{" +
                    "keyword=" + keyword +
                    ", name=" + name +
                    ", typeAnnotation=" + typeAnnotation +
                    ", initializer=" + initializer +
                    '}';
        }
    }
    public static class Block extends Stmt {
        public final List<Stmt> statements;

        public Block(List<Stmt> statements) {
            this.statements = statements;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitBlockStmt(this); }

        @Override
        public String toString() {
            return "Block{" +
                    "statements=" + statements +
                    '}';
        }
    }

    public static class If extends Stmt {
        public final Expr condition;
        public final Stmt thenBranch;
        public final Stmt elseBranch;

        public If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitIfStmt(this); }

        @Override
        public String toString() {
            return "If{" +
                    "condition=" + condition +
                    ", thenBranch=" + thenBranch +
                    ", elseBranch=" + elseBranch +
                    '}';
        }
    }

    public static class ForIn extends Stmt {
        public final Token loopVariable; // O 'a' no teu "for a in [1,2,3]"
        public final Expr iterable;      // A array [1,2,3]
        public final Stmt body;          // O bloco {}

        public ForIn(Token loopVariable, Expr iterable, Stmt body) {
            this.loopVariable = loopVariable;
            this.iterable = iterable;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitForInStmt(this); }

        @Override
        public String toString() {
            return "ForIn{" +
                    "loopVariable=" + loopVariable +
                    ", iterable=" + iterable +
                    ", body=" + body +
                    '}';
        }
    }

    public static class ForCStyle extends Stmt {
        public final Stmt init; // O 'a' no teu "for (a=0; a<=12; a=a + 1 ){}"
        public final Expr condition;      // i<1; iz=12
        public final Stmt increment;      // i++, i--,
        public final Stmt body;          // O bloco {}

        public ForCStyle(Stmt init, Expr condition, Stmt increment, Stmt body) {
            this.init = init;
            this.condition = condition;
            this.increment = increment;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitForCStyleStmt(this); }

        @Override
        public String toString() {
            return "ForCStyle{" +
                    "init=" + init +
                    ", condition=" + condition +
                    ", increment=" + increment +
                    ", body=" + body +
                    '}';
        }
    }

    public static class ForInRange extends Stmt {
        public final Token loopVariable; // O 'a' no teu "for a in [1,2,3]"
        public final Expr start;      // i<1; iz=12
        public final Expr end;      // i<1; iz=12
        public final Optional<Expr> jump;      // i<1; iz=12
        public final Stmt body;          // O bloco {}

        public ForInRange(Token loopVariable, Expr start, Expr end, Optional<Expr> jump, Stmt body) {
            this.loopVariable = loopVariable;
            this.start = start;
            this.end = end;
            this.jump = jump;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitForInRangeStmt(this); }

        @Override
        public String toString() {
            String jump = this.jump.map(Object::toString).orElse(null);
            return "ForInRange{" +
                    "loopVariable=" + loopVariable +
                    ", start=" + start +
                    ", end=" + end +
                    ", jump=" + jump +
                    ", body=" + body +
                    '}';
        }
    }

    public static class Function extends Stmt {
        public final Token name;
        public final List<Token> params;
        public final Token returnType; // Ex: T_INT (pode ser null se não retornar nada)
        public final List<Stmt> body;

        public Function(Token name, List<Token> params, Token returnType, List<Stmt> body) {
            this.name = name;
            this.params = params;
            this.returnType = returnType;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitFunctionStmt(this); }

        @Override
        public String toString() {
            return "Function{" +
                    "name=" + name +
                    ", params=" + params +
                    ", returnType=" + returnType +
                    ", body=" + body +
                    '}';
        }
    }

    public static class Break extends Stmt {
        public final Token keyword;

        public Break(Token keyword) {
            this.keyword = keyword;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitBreakStmt(this); }

        @Override
        public String toString() {
            return "Break{" +
                    "keyword=" + keyword +
                    '}';
        }
    }

    public static class Continue extends Stmt {
        public final Token keyword;

        public Continue(Token keyword) {
            this.keyword = keyword;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitContinueStmt(this); }

        @Override
        public String toString() {
            return "Continue{" +
                    "keyword=" + keyword +
                    '}';
        }
    }

    public static class Return extends Stmt {
        public final Token keyword;
        public final Expr value; // O que vai ser retornado (pode ser null)

        public Return(Token keyword, Expr value) {
            this.keyword = keyword;
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitReturnStmt(this); }

        @Override
        public String toString() {
            return "Return{" +
                    "keyword=" + keyword +
                    ", value=" + value +
                    '}';
        }
    }

    // No fundo do ficheiro Stmt.java:
    public static class Enum extends Stmt {
        public final Token name;
        public final List<Token> constants;

        public Enum(Token name, List<Token> constants) {
            this.name = name;
            this.constants = constants;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitEnumStmt(this); }
    }
}