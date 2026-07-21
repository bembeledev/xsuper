package com.dic.xsuper.lang.lint;

import com.dic.xsuper.lang.*;
import java.util.*;

public class XplLinter {

    private final LintConfig config;
    private final Set<String> globalSymbols;
    private final TypeEnvironment typeEnv;

    public XplLinter() {
        this(new LintConfig(), Set.of(), new TypeEnvironment());
    }

    public XplLinter(LintConfig config, Set<String> globalSymbols, TypeEnvironment typeEnv) {
        this.config = config;
        this.globalSymbols = globalSymbols != null ? globalSymbols : Set.of();
        this.typeEnv = typeEnv;
    }

    public List<LintIssue> lintXpl(List<Stmt> statements) {
        List<LintIssue> issues = new ArrayList<>();
        LintVisitor visitor = new LintVisitor(issues, config, globalSymbols, typeEnv);
        for (Stmt stmt : statements) {
            stmt.accept(visitor);
        }
        visitor.finish(issues);
        return issues;
    }

    // ─── Visitor ──────────────────────────────────────────────────────────

    private static class LintVisitor implements Stmt.Visitor<Void>, Expr.Visitor<Void> {
        private final List<LintIssue> issues;
        private final LintConfig config;
        private final Set<String> globalSymbols;
        private final TypeEnvironment typeEnv;

        // Contexto de análise
        private final Stack<TypeScope> scopes = new Stack<>();
        private final Set<String> functionsDeclared = new HashSet<>();
        private final Set<String> functionsCalled = new HashSet<>();
        private final Set<String> functionsDefined = new HashSet<>();
        private boolean inFunction = false;
        private boolean inLoop = false;
        private String currentClassName = null;
        private FunctionInfo currentFunction = null;

        public LintVisitor(List<LintIssue> issues, LintConfig config, Set<String> globalSymbols, TypeEnvironment typeEnv) {
            this.issues = issues;
            this.config = config;
            this.globalSymbols = globalSymbols;
            this.typeEnv = typeEnv;
            scopes.push(new TypeScope());
        }

        // ─── Auxiliares ────────────────────────────────────────────────────

        private void addIssue(LintIssue.Severity severity, String msg, Token token, String suggestion) {
            issues.add(new LintIssue(severity, msg, token, suggestion));
        }

        private TypeNode getTypeOfExpr(Expr expr) {
            if (expr instanceof Expr.Literal lit) {
                Object val = lit.value;
                if (val instanceof Long || val instanceof Integer) return new TypeNode.Simple(new Token(TokenType.T_INT, "int", null, 0, 0));
                if (val instanceof Double || val instanceof Float) return new TypeNode.Simple(new Token(TokenType.T_FLOAT, "float", null, 0, 0));
                if (val instanceof String || val instanceof Character) return new TypeNode.Simple(new Token(TokenType.T_STRING, "string", null, 0, 0));
                if (val instanceof Boolean) return new TypeNode.Simple(new Token(TokenType.T_BOOL, "bool", null, 0, 0));
                return null;
            }
            if (expr instanceof Expr.Variable var) {
                String name = var.name.lexeme;
                for (int i = scopes.size() - 1; i >= 0; i--) {
                    TypeNode t = scopes.get(i).getType(name);
                    if (t != null) return t;
                }
                if (globalSymbols.contains(name)) return null;
                if (currentClassName != null) {
                    Stmt.FieldDecl field = typeEnv.getField(currentClassName, name);
                    if (field != null) return field.type;
                }
                return null;
            }
            if (expr instanceof Expr.Binary bin) {
                TypeNode left = getTypeOfExpr(bin.left);
                TypeNode right = getTypeOfExpr(bin.right);
                if (left != null && right != null) {
                    if (isNumeric(left) && isNumeric(right)) {
                        if (isFloat(left) || isFloat(right)) return new TypeNode.Simple(new Token(TokenType.T_FLOAT, "float", null, 0, 0));
                        return new TypeNode.Simple(new Token(TokenType.T_INT, "int", null, 0, 0));
                    }
                    if (isString(left) || isString(right)) return new TypeNode.Simple(new Token(TokenType.T_STRING, "string", null, 0, 0));
                }
                return null;
            }
            if (expr instanceof Expr.Call call) {
                // Tentar obter tipo de retorno da função chamada
                if (call.callee instanceof Expr.Variable var) {
                    String funcName = var.name.lexeme;
                    Stmt.Function func = typeEnv.getFunction(funcName);
                    if (func != null) return func.returnType;
                }
                return null;
            }
            return null;
        }

        private boolean isNumeric(TypeNode t) {
            if (t == null) return false;
            String name = t.name.lexeme;
            return name.equals("int") || name.equals("float") || name.equals("long") || name.equals("double");
        }

        private boolean isFloat(TypeNode t) {
            if (t == null) return false;
            return t.name.lexeme.equals("float") || t.name.lexeme.equals("double");
        }

        private boolean isString(TypeNode t) {
            if (t == null) return false;
            return t.name.lexeme.equals("string") || t.name.lexeme.equals("char");
        }

        private boolean isVoid(TypeNode t) {
            return t == null;
        }

        private boolean isAssignable(TypeNode target, TypeNode value) {
            if (target == null || value == null) return true;
            String tName = target.name.lexeme;
            String vName = value.name.lexeme;
            if (tName.equals("int") && (vName.equals("int") || vName.equals("long") || vName.equals("short") || vName.equals("byte"))) return true;
            if (tName.equals("float") && (vName.equals("float") || vName.equals("double") || isNumeric(value))) return true;
            if (tName.equals("string") && vName.equals("string")) return true;
            if (tName.equals("bool") && vName.equals("bool")) return true;
            // Classes
            if (typeEnv.declaredTypes.containsKey(tName) && typeEnv.declaredTypes.containsKey(vName)) {
                return typeEnv.isSubclass(vName, tName) || tName.equals(vName);
            }
            return false;
        }

        // ─── Visitantes de Statements ─────────────────────────────────────

        @Override
        public Void visitExpressionStmt(Stmt.ExpressionStmt stmt) {
            stmt.expression.accept(this);
            return null;
        }

        @Override
        public Void visitVarDeclStmt(Stmt.VarDecl stmt) {
            String name = stmt.name.lexeme;
            TypeNode declaredType = stmt.typeAnnotation;
            TypeNode inferredType = null;
            if (stmt.initializer != null) {
                inferredType = getTypeOfExpr(stmt.initializer);
                stmt.initializer.accept(this);
            }
            if (config.checkTypeCompatibility && declaredType != null && inferredType != null) {
                if (!isAssignable(declaredType, inferredType)) {
                    addIssue(LintIssue.Severity.ERROR,
                            "Tipo incompatível: variável '" + name + "' declarada como " + declaredType.name.lexeme +
                                    " mas inicializada com " + inferredType.name.lexeme,
                            stmt.name, "Ajuste o tipo ou o valor.");
                }
            }
            TypeNode effectiveType = declaredType != null ? declaredType : inferredType;
            if (config.checkDuplicateDeclarations && scopes.peek().containsKey(name)) {
                addIssue(LintIssue.Severity.ERROR, "Variável '" + name + "' já declarada neste escopo.", stmt.name,
                        "Remova a duplicata ou renomeie.");
            }
            scopes.peek().put(name, effectiveType);
            // Naming para constantes
            if (config.checkConstantNaming && stmt.keyword.type == TokenType.CONST) {
                if (!name.matches("^[A-Z][A-Z0-9_]*$")) {
                    addIssue(LintIssue.Severity.SUGGESTION,
                            "Constante '" + name + "' deve estar em UPPER_SNAKE_CASE (ex: MAX_SIZE).",
                            stmt.name, "Renomeie para MAIÚSCULAS_COM_UNDERSCORES.");
                }
            }
            return null;
        }

        @Override
        public Void visitBlockStmt(Stmt.Block stmt) {
            scopes.push(new TypeScope());
            for (Stmt s : stmt.statements) s.accept(this);
            scopes.pop();
            return null;
        }

        @Override
        public Void visitForInStmt(Stmt.ForIn stmt) {
            boolean prevInLoop = inLoop;
            inLoop = true;
            scopes.push(new TypeScope());
            scopes.peek().put(stmt.loopVariable.lexeme, null);
            stmt.iterable.accept(this);
            stmt.body.accept(this);
            scopes.pop();
            inLoop = prevInLoop;
            return null;
        }

        @Override
        public Void visitForCStyleStmt(Stmt.ForCStyle stmt) {
            boolean prevInLoop = inLoop;
            inLoop = true;
            if (stmt.init != null) stmt.init.accept(this);
            if (stmt.condition != null) stmt.condition.accept(this);
            if (stmt.increment != null) stmt.increment.accept(this);
            stmt.body.accept(this);
            inLoop = prevInLoop;
            return null;
        }

        @Override
        public Void visitDoWhileStmt(Stmt.DoWhile stmt) {
            boolean prevInLoop = inLoop;
            inLoop = true;
            stmt.body.accept(this);
            stmt.condition.accept(this);
            inLoop = prevInLoop;
            return null;
        }

        @Override
        public Void visitWhileStmt(Stmt.While stmt) {
            boolean prevInLoop = inLoop;
            inLoop = true;
            stmt.condition.accept(this);
            stmt.body.accept(this);
            inLoop = prevInLoop;
            return null;
        }

        @Override
        public Void visitBreakStmt(Stmt.Break stmt) {
            if (config.checkBreakContinueOutsideLoop && !inLoop && !inFunction) {
                addIssue(LintIssue.Severity.ERROR, "'break' fora de um loop.", stmt.keyword,
                        "Coloque o break dentro de um loop.");
            }
            return null;
        }

        @Override
        public Void visitContinueStmt(Stmt.Continue stmt) {
            if (config.checkBreakContinueOutsideLoop && !inLoop && !inFunction) {
                addIssue(LintIssue.Severity.ERROR, "'continue' fora de um loop.", stmt.keyword,
                        "Coloque o continue dentro de um loop.");
            }
            return null;
        }

        @Override
        public Void visitReturnStmt(Stmt.Return stmt) {
            if (config.checkReturnOutsideFunction && currentFunction == null) {
                addIssue(LintIssue.Severity.ERROR, "'return' fora de função.", stmt.keyword, null);
                return null;
            }
            if (currentFunction != null) {
                TypeNode expected = currentFunction.declaration.returnType;
                if (stmt.value != null) {
                    TypeNode actual = getTypeOfExpr(stmt.value);
                    if (config.checkReturnType && expected != null && !isVoid(expected)) {
                        if (!isAssignable(expected, actual)) {
                            addIssue(LintIssue.Severity.ERROR,
                                    "Tipo de retorno incompatível: esperado " + expected.name.lexeme +
                                            ", mas retornou " + (actual != null ? actual.name.lexeme : "desconhecido"),
                                    stmt.keyword, "Ajuste o tipo de retorno.");
                        }
                    } else if (expected == null) {
                        addIssue(LintIssue.Severity.ERROR, "Função void não pode retornar valor.",
                                stmt.keyword, "Remova o valor ou declare o tipo de retorno.");
                    }
                    stmt.value.accept(this);
                } else {
                    if (expected != null && !isVoid(expected)) {
                        addIssue(LintIssue.Severity.ERROR,
                                "Função com tipo de retorno '" + expected.name.lexeme + "' não pode ter 'return' sem valor.",
                                stmt.keyword, "Adicione um valor de retorno ou remova o tipo.");
                    }
                }
            }
            return null;
        }

        @Override
        public Void visitForInRangeStmt(Stmt.ForInRange stmt) {
            boolean prevInLoop = inLoop;
            inLoop = true;
            scopes.push(new TypeScope());
            scopes.peek().put(stmt.loopVariable.lexeme, null);
            stmt.start.accept(this);
            stmt.end.accept(this);
            stmt.jump.ifPresent(j -> j.accept(this));
            stmt.body.accept(this);
            scopes.pop();
            inLoop = prevInLoop;
            return null;
        }

        @Override
        public Void visitFunctionStmt(Stmt.Function stmt) {
            String funcName = stmt.name.lexeme;
            functionsDeclared.add(funcName);
            if (stmt.body != null) functionsDefined.add(funcName);

            if (stmt.body != null) {
                boolean prevInFunction = inFunction;
                inFunction = true;
                FunctionInfo prevFunc = currentFunction;
                currentFunction = new FunctionInfo(stmt);
                scopes.push(new TypeScope());
                for (Stmt.Param param : stmt.params) {
                    scopes.peek().put(param.name.lexeme, param.typeNode);
                }
                for (Stmt s : stmt.body) {
                    s.accept(this);
                }
                // Verificar parâmetros não utilizados
                if (config.checkUnusedParameters) {
                    for (Stmt.Param param : stmt.params) {
                        VarInfo info = scopes.peek().getVarInfo(param.name.lexeme);
                        if (info != null && !info.used) {
                            addIssue(LintIssue.Severity.WARNING,
                                    "Parâmetro '" + param.name.lexeme + "' não utilizado na função.",
                                    param.name, "Remova-o ou utilize-o no corpo.");
                        }
                    }
                }
                // Verificar retorno obrigatório
                TypeNode returnType = stmt.returnType;
                if (config.checkReturnType && returnType != null && !isVoid(returnType)) {
                    boolean foundReturn = false;
                    for (Stmt s : stmt.body) {
                        if (s instanceof Stmt.Return ret && ret.value != null) {
                            foundReturn = true;
                            break;
                        }
                    }
                    if (!foundReturn) {
                        addIssue(LintIssue.Severity.ERROR,
                                "Função '" + funcName + "' com tipo de retorno '" + returnType.name.lexeme +
                                        "' não possui 'return' com valor.",
                                stmt.name, "Adicione um 'return' com valor compatível.");
                    }
                } else if (returnType == null) {
                    for (Stmt s : stmt.body) {
                        if (s instanceof Stmt.Return ret && ret.value != null) {
                            addIssue(LintIssue.Severity.ERROR,
                                    "Função void não pode ter 'return' com valor.",
                                    ret.keyword, "Remova o valor ou declare o tipo de retorno.");
                        }
                    }
                }
                scopes.pop();
                inFunction = prevInFunction;
                currentFunction = prevFunc;
            }
            return null;
        }

        @Override
        public Void visitTryStmt(Stmt.Try stmt) {
            stmt.tryBlock.accept(this);
            for (Stmt.CatchClause cc : stmt.catchClauses) {
                scopes.push(new TypeScope());
                scopes.peek().put(cc.name.lexeme, cc.type);
                cc.body.accept(this);
                scopes.pop();
            }
            if (stmt.finallyBlock != null) stmt.finallyBlock.accept(this);
            return null;
        }

        @Override
        public Void visitThrowStmt(Stmt.Throw stmt) {
            stmt.value.accept(this);
            return null;
        }

        // Declarações que não exigem processamento adicional
        @Override public Void visitTypeAliasDecl(Stmt.TypeAliasDecl stmt) { return null; }
        @Override public Void visitDecoratorDeclStmt(Stmt.DecoratorDecl stmt) { return null; }
        @Override public Void visitModuleDeclStmt(Stmt.ModuleDecl stmt) { return null; }
        @Override public Void visitImportDeclStmt(Stmt.ImportDecl stmt) { return null; }
        @Override public Void visitExportDeclStmt(Stmt.ExportDecl stmt) {
            if (stmt.declaration != null) stmt.declaration.accept(this);
            return null;
        }
        @Override public Void visitGlobalDeclStmt(Stmt.GlobalDecl stmt) {
            if (stmt.initializer != null) stmt.initializer.accept(this);
            return null;
        }
        @Override public Void visitEnumStmt(Stmt.Enum stmt) { return null; }
        @Override public Void visitDeclareDeclStmt(Stmt.DeclareDecl stmt) { return null; }
        @Override public Void visitInterfaceDeclStmt(Stmt.InterfaceDecl stmt) { return null; }
        @Override public Void visitImplementDeclStmt(Stmt.ImplementDecl stmt) {
            for (Stmt.Function m : stmt.methods) m.accept(this);
            return null;
        }

        // ─── Visitantes de Expressões ─────────────────────────────────────

        @Override
        public Void visitVariableExpr(Expr.Variable expr) {
            String name = expr.name.lexeme;
            for (int i = scopes.size() - 1; i >= 0; i--) {
                VarInfo info = scopes.get(i).getVarInfo(name);
                if (info != null) {
                    info.used = true;
                    break;
                }
            }
            boolean found = false;
            for (int i = scopes.size() - 1; i >= 0; i--) {
                if (scopes.get(i).containsKey(name)) {
                    found = true;
                    break;
                }
            }
            if (!found && !functionsDeclared.contains(name) && !globalSymbols.contains(name)) {
                addIssue(LintIssue.Severity.ERROR, "Variável não declarada: '" + name + "'.", expr.name,
                        "Declare a variável antes de usá-la.");
            }
            return null;
        }

        @Override
        public Void visitAssignExpr(Expr.Assign expr) {
            expr.value.accept(this);
            String name = expr.name.lexeme;
            boolean found = false;
            for (int i = scopes.size() - 1; i >= 0; i--) {
                if (scopes.get(i).containsKey(name)) {
                    found = true;
                    VarInfo info = scopes.get(i).getVarInfo(name);
                    if (info != null && info.isConstant) {
                        addIssue(LintIssue.Severity.ERROR, "Atribuição a constante '" + name + "'.", expr.name,
                                "Constantes não podem ser reatribuídas.");
                    }
                    TypeNode expected = scopes.get(i).getType(name);
                    TypeNode actual = getTypeOfExpr(expr.value);
                    if (config.checkTypeCompatibility && expected != null && actual != null && !isAssignable(expected, actual)) {
                        addIssue(LintIssue.Severity.ERROR,
                                "Tipo incompatível: variável '" + name + "' é do tipo " + expected.name.lexeme +
                                        " mas está a receber " + actual.name.lexeme,
                                expr.name, "Ajuste o valor.");
                    }
                    break;
                }
            }
            if (!found && !globalSymbols.contains(name)) {
                addIssue(LintIssue.Severity.ERROR, "Variável não declarada: '" + name + "'.", expr.name,
                        "Declare a variável antes de atribuir.");
            }
            return null;
        }

        @Override
        public Void visitCallExpr(Expr.Call expr) {
            if (expr.callee instanceof Expr.Variable) {
                String funcName = ((Expr.Variable) expr.callee).name.lexeme;
                functionsCalled.add(funcName);
                Stmt.Function funcDecl = typeEnv.getFunction(funcName);
                if (funcDecl != null) {
                    List<Stmt.Param> params = funcDecl.params;
                    List<Expr.CallArg> args = expr.arguments;
                    if (config.checkFunctionArity && params.size() != args.size()) {
                        addIssue(LintIssue.Severity.ERROR,
                                "Número de argumentos incorreto para função '" + funcName +
                                        "': espera " + params.size() + ", recebeu " + args.size(),
                                expr.paren, "Ajuste o número de argumentos.");
                    } else if (config.checkTypeCompatibility) {
                        for (int i = 0; i < Math.min(params.size(), args.size()); i++) {
                            TypeNode paramType = params.get(i).typeNode;
                            TypeNode argType = getTypeOfExpr(args.get(i).expression);
                            if (paramType != null && argType != null && !isAssignable(paramType, argType)) {
                                addIssue(LintIssue.Severity.ERROR,
                                        "Tipo de argumento incompatível: parâmetro " + (i+1) +
                                                " espera " + paramType.name.lexeme + " mas recebeu " + argType.name.lexeme,
                                        expr.paren, "Ajuste o tipo do argumento.");
                            }
                        }
                    }
                } else if (!globalSymbols.contains(funcName)) {
                    addIssue(LintIssue.Severity.ERROR, "Função não declarada: '" + funcName + "'.", expr.paren,
                            "Declare a função antes de chamá-la.");
                }
            }
            expr.callee.accept(this);
            for (Expr.CallArg arg : expr.arguments) {
                arg.expression.accept(this);
            }
            return null;
        }

        @Override
        public Void visitGetExpr(Expr.Get expr) {
            expr.object.accept(this);
            if (config.checkPrivateAccess && currentClassName != null) {
                TypeNode objType = getTypeOfExpr(expr.object);
                if (objType != null && typeEnv.classFields.containsKey(objType.name.lexeme)) {
                    String fieldName = expr.name.lexeme;
                    Stmt.FieldDecl field = typeEnv.getField(objType.name.lexeme, fieldName);
                    if (field != null && field.modifier != null && field.modifier.type == TokenType.PRIVATE) {
                        if (!objType.name.lexeme.equals(currentClassName)) {
                            addIssue(LintIssue.Severity.ERROR,
                                    "Acesso a campo privado '" + fieldName + "' da classe " + objType.name.lexeme +
                                            " fora da classe.",
                                    expr.name, "Torne o campo público ou aceda dentro da classe.");
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public Void visitSetExpr(Expr.Set expr) {
            expr.object.accept(this);
            expr.value.accept(this);
            if (config.checkPrivateAccess && currentClassName != null) {
                TypeNode objType = getTypeOfExpr(expr.object);
                if (objType != null && typeEnv.classFields.containsKey(objType.name.lexeme)) {
                    String fieldName = expr.name.lexeme;
                    Stmt.FieldDecl field = typeEnv.getField(objType.name.lexeme, fieldName);
                    if (field != null && field.modifier != null && field.modifier.type == TokenType.PRIVATE) {
                        if (!objType.name.lexeme.equals(currentClassName)) {
                            addIssue(LintIssue.Severity.ERROR,
                                    "Modificação de campo privado '" + fieldName + "' da classe " + objType.name.lexeme +
                                            " fora da classe.",
                                    expr.name, "Torne o campo público ou modifique dentro da classe.");
                        }
                    }
                }
            }
            return null;
        }

        // Outros visitantes de expressão (delegam para filhos)
        @Override public Void visitBinaryExpr(Expr.Binary expr) {
            expr.left.accept(this);
            expr.right.accept(this);
            return null;
        }
        @Override public Void visitUnaryExpr(Expr.Unary expr) {
            expr.right.accept(this);
            return null;
        }
        @Override public Void visitLiteralExpr(Expr.Literal expr) { return null; }
        @Override public Void visitArrayExpr(Expr.ArrayLiteral expr) {
            for (Expr e : expr.elements) e.accept(this);
            return null;
        }
        @Override public Void visitCompoundAssignExpr(Expr.CompoundAssign expr) {
            expr.target.accept(this);
            expr.value.accept(this);
            return null;
        }
        @Override public Void visitUpdateExpr(Expr.Update expr) {
            expr.target.accept(this);
            return null;
        }
        @Override public Void visitIndexAccessExpr(Expr.IndexAccess expr) {
            expr.object.accept(this);
            expr.index.accept(this);
            return null;
        }
        @Override public Void visitIndexAssignExpr(Expr.IndexAssign expr) {
            expr.object.accept(this);
            expr.index.accept(this);
            expr.value.accept(this);
            return null;
        }
        @Override public Void visitArrowFunctionExpr(Expr.ArrowFunction expr) {
            scopes.push(new TypeScope());
            /*if (expr.parameter != null) {
                scopes.peek().put(expr.parameter.lexeme, null);
            }*/
            expr.body.accept(this);
            scopes.pop();
            return null;
        }
        @Override public Void visitObjectLiteralExpr(Expr.ObjectLiteral expr) {
            for (Expr key : expr.keys) key.accept(this);
            for (Expr val : expr.values) val.accept(this);
            return null;
        }
        @Override public Void visitNewExpr(Expr.New expr) {
            for (Expr.CallArg arg : expr.arguments) arg.expression.accept(this);
            return null;
        }
        @Override public Void visitSuperExpr(Expr.Super expr) { return null; }
        @Override public Void visitCastExpr(Expr.Cast expr) {
            expr.value.accept(this);
            return null;
        }
        @Override public Void visitTypeCheckExpr(Expr.TypeCheck expr) {
            expr.left.accept(this);
            return null;
        }
        @Override public Void visitTypeofExpr(Expr.Typeof expr) {
            expr.expression.accept(this);
            return null;
        }
        @Override public Void visitIfExpr(Expr.If expr) {
            expr.condition.accept(this);
            expr.thenBranch.accept(this);
            if (expr.elseBranch != null) expr.elseBranch.accept(this);
            return null;
        }
        @Override public Void visitLogicalExpr(Expr.Logical expr) {
            expr.left.accept(this);
            expr.right.accept(this);
            return null;
        }
        @Override public Void visitSwitchExpr(Expr.Switch expr) {
            expr.target.accept(this);
            for (Expr.SwitchCase sc : expr.cases) {
                for (Expr val : sc.values) val.accept(this);
                sc.body.accept(this);
            }
            if (expr.defaultBranch != null) expr.defaultBranch.accept(this);
            return null;
        }
        @Override public Void visitMatchExpr(Expr.Match expr) {
            expr.target.accept(this);
            for (Expr.MatchArm arm : expr.arms) {
                if (arm.valueTest != null) arm.valueTest.accept(this);
                if (arm.guard != null) arm.guard.accept(this);
                arm.body.accept(this);
            }
            if (expr.defaultBranch != null) expr.defaultBranch.accept(this);
            return null;
        }
        @Override public Void visitUnwrapExpr(Expr.Unwrap expr) {
            expr.expr.accept(this);
            return null;
        }
        @Override public Void visitNullCoalesceExpr(Expr.NullCoalesce expr) {
            expr.left.accept(this);
            expr.right.accept(this);
            return null;
        }
        @Override public Void visitOptionalChainingExpr(Expr.OptionalChaining expr) {
            expr.object.accept(this);
            return null;
        }
        @Override public Void visitOptionalCallExpr(Expr.OptionalCall expr) {
            expr.object.accept(this);
            for (Expr.CallArg arg : expr.arguments) arg.expression.accept(this);
            return null;
        }
        @Override public Void visitMetaAccessExpr(Expr.MetaAccess expr) {
            expr.object.accept(this);
            return null;
        }
        @Override public Void visitTernaryExpr(Expr.Ternary expr) {
            expr.condition.accept(this);
            expr.trueBranch.accept(this);
            expr.falseBranch.accept(this);
            return null;
        }
        @Override public Void visitBlockExpr(Expr.Block expr) {
            scopes.push(new TypeScope());
            for (Stmt stmt : expr.statements) stmt.accept(this);
            scopes.pop();
            return null;
        }

        // ─── Finalização ───────────────────────────────────────────────────

        public void finish(List<LintIssue> issues) {
            if (config.checkUnusedVariables) {
                for (TypeScope scope : scopes) {
                    for (Map.Entry<String, VarInfo> entry : scope.vars.entrySet()) {
                        if (!entry.getValue().used && !entry.getValue().isParameter) {
                            addIssue(LintIssue.Severity.WARNING,
                                    "Variável '" + entry.getKey() + "' declarada mas não utilizada.",
                                    entry.getValue().token, "Remova a variável ou utilize-a.");
                        }
                    }
                }
            }
            if (config.checkUnusedFunctions) {
                for (String func : functionsDefined) {
                    if (!functionsCalled.contains(func)) {
                        addIssue(LintIssue.Severity.WARNING,
                                "Função '" + func + "' definida mas não chamada.",
                                null, "Remova ou chame a função.");
                    }
                }
            }
        }

        // ─── Classes internas ──────────────────────────────────────────────

        private static class FunctionInfo {
            final Stmt.Function declaration;
            FunctionInfo(Stmt.Function decl) { this.declaration = decl; }
        }

        private static class TypeScope {
            final Map<String, TypeNode> types = new HashMap<>();
            final Map<String, VarInfo> vars = new HashMap<>();

            public void put(String name, TypeNode type) {
                types.put(name, type);
                if (!vars.containsKey(name)) {
                    vars.put(name, new VarInfo(null, false, false));
                }
            }

            public void putVarInfo(String name, VarInfo info) {
                vars.put(name, info);
            }

            public TypeNode getType(String name) {
                return types.get(name);
            }

            public VarInfo getVarInfo(String name) {
                return vars.get(name);
            }

            public boolean containsKey(String name) {
                return vars.containsKey(name);
            }
        }

        private static class VarInfo {
            Token token;
            boolean isConstant;
            boolean isParameter;
            boolean used = false;

            VarInfo(Token token, boolean isConstant, boolean isParameter) {
                this.token = token;
                this.isConstant = isConstant;
                this.isParameter = isParameter;
            }
        }
    }
}