package com.dic.xsuper.lang;

import com.dic.xsuper.core.CommandRegistry;
import com.dic.xsuper.utils.ConsoleTheme;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    // O escopo global que criámos na Fase 1
    // 1. Cria a caixa global
    public final Environment globals = new Environment();
    public final CommandRegistry registry;
    public Path currentDirectory;
    // 2. O ambiente atual aponta para a caixa global logo no início!
    private Environment environment = globals;

    public Interpreter(CommandRegistry registry, Path currentDirectory) {
        this.registry = registry;
        this.currentDirectory = currentDirectory;

        // Função Nativa: println
        globals.defineConst("println", new XplCallable() {
            @Override
            public int arity() {
                return -1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                if (arguments.size() < 1 || arguments.size() > 2)
                    throw new RuntimeException("println espera 1 ou 2 argumentos.");
                String text = stringify(arguments.get(0));
                if (arguments.size() == 2) {
                    System.out.println(hexToAnsi(stringify(arguments.get(1))) + text + ConsoleTheme.RESET);
                } else {
                    System.out.println(ConsoleTheme.TEXT + text + ConsoleTheme.RESET);
                }
                return null;
            }
        });

        // Função Nativa: print
        globals.defineConst("print", new XplCallable() {
            @Override
            public int arity() {
                return -1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                if (arguments.isEmpty() || arguments.size() > 2)
                    throw new RuntimeException("print espera 1 ou 2 argumentos.");
                String text = stringify(arguments.get(0));
                if (arguments.size() == 2) {
                    System.out.print(hexToAnsi(stringify(arguments.get(1))) + text + ConsoleTheme.RESET);
                } else {
                    System.out.print(ConsoleTheme.TEXT + text + ConsoleTheme.RESET);
                }
                System.out.flush();
                return null;
            }
        });

        // Função Nativa: shell
        globals.defineConst("shell", new XplCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                String commandStr = stringify(arguments.get(0));
                PrintStream originalOut = System.out;
                ByteArrayOutputStream memoryStream = new ByteArrayOutputStream();
                try (PrintStream captureOut = new PrintStream(memoryStream, true, StandardCharsets.UTF_8)) {
                    System.setOut(captureOut);
                    interpreter.currentDirectory = interpreter.registry.executeCommand(commandStr, interpreter.currentDirectory);
                } catch (Exception e) {
                    return "Erro no shell: " + e.getMessage();
                } finally {
                    System.setOut(originalOut);
                }
                return memoryStream.toString(StandardCharsets.UTF_8).trim();
            }
        });
    }

    public void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (ControlFlow.RuntimeError error) {
            System.err.println(ConsoleTheme.ERROR + "Erro de Execução (Linha " + error.token.line + "): " + error.getMessage() + ConsoleTheme.RESET);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    // ==========================================
    // EXECUÇÃO DE DECLARAÇÕES (STATEMENTS)
    // ==========================================

    @Override
    public Void visitExpressionStmt(Stmt.ExpressionStmt stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitVarDeclStmt(Stmt.VarDecl stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        // Verificação Básica de Tipos (se a anotação :long, :string, etc. foi usada)
        if (stmt.typeAnnotation != null && value != null) {
            checkTypeCompatability(stmt.typeAnnotation, value);
        }

        // Delega para o teu Environment aplicar as regras restritas!
        String name = stmt.name.lexeme;
        switch (stmt.keyword.type) {
            case VAR:
                environment.defineVar(name, value);
                break;
            case LET:
                environment.defineLet(name, value);
                break;
            case CONST:
                environment.defineConst(name, value);
                break;
        }
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(this.environment));
        return null;
    }

    public void executeBlock(List<Stmt> statements, Environment blockEnv) {
        Environment previous = this.environment;
        try {
            this.environment = blockEnv; // Entra no novo escopo
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous; // Restaura o escopo pai ao sair do bloco
        }
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }


    @Override
    public Void visitForInStmt(Stmt.ForIn stmt) {
        Object iterable = evaluate(stmt.iterable);

        if (!(iterable instanceof Iterable)) {
            throw new ControlFlow.RuntimeError(stmt.loopVariable, "O alvo do 'for-in' precisa ser um Array ou Lista iterável.");
        }

        // O 'for-in' cria um mini-escopo só para a variável de interação
        for (Object element : (Iterable<?>) iterable) {
            Environment loopEnv = new Environment(this.environment);
            loopEnv.defineLet(stmt.loopVariable.lexeme, element);

            Environment previous = this.environment;
            try {
                this.environment = loopEnv;
                execute(stmt.body);
            } catch (ControlFlow.BreakException e) {
                this.environment = previous;
                break; // Sai do loop!
            } catch (ControlFlow.ContinueException e) {
                this.environment = previous;
                // Continua para o próximo ciclo
            } finally {
                this.environment = previous;
            }
        }
        return null;
    }

    @Override
    public Void visitForCStyleStmt(Stmt.ForCStyle stmt) {
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new ControlFlow.BreakException();
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        throw new ControlFlow.ContinueException();
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        // Transforma a declaração da AST num Objeto Executável XplFunction
        XplFunction function = new XplFunction(stmt, this.environment);

        // Guarda a função na memória (no escopo atual)
        environment.defineLet(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) {
            value = evaluate(stmt.value);
        }
        // Dispara o valor de volta para o chamador através da nossa exceção leve
        throw new ControlFlow.ReturnException(value);
    }


    @Override
    public Void visitForInRangeStmt(Stmt.ForInRange stmt) {
        // 1. Avalia as expressões matemáticas para descobrir os valores
        double startVal = toDouble(evaluate(stmt.start));
        double endVal = toDouble(evaluate(stmt.end));

        // Se o utilizador não passou o jump, o padrão é 1.
        double jumpVal = 1.0;
        if (stmt.jump.isPresent()) {
            jumpVal = toDouble(evaluate(stmt.jump.get()));
        }

        // Validação de segurança crítica
        if (jumpVal == 0) {
            throw new ControlFlow.RuntimeError(stmt.loopVariable, "Erro de Loop: O incremento (jump) não pode ser zero.");
        }

        // Magia de usabilidade: Se o start for maior que o end e não houver jump, inverte automaticamente para -1
        if (startVal > endVal && stmt.jump.isPresent()) {
            jumpVal = -1.0;
        }

        // Descobre a direção do loop
        boolean isAscending = jumpVal > 0;
        double current = startVal;

        // O motor do Range Loop
        while ((isAscending && current <= endVal) || (!isAscending && current >= endVal)) {

            // Cria a "caixa" (escopo) só para esta volta do loop
            Environment loopEnv = new Environment(this.environment);

            // Um toque de classe: se o número for inteiro (ex: 5.0), guarda como Long (5) para ficar limpo.
            Object valueToStore = (current == Math.floor(current)) ? (long) current : current;

            // Injeta a variável (ex: 'a') na memória
            loopEnv.defineLet(stmt.loopVariable.lexeme, valueToStore);

            Environment previous = this.environment;
            try {
                this.environment = loopEnv;
                execute(stmt.body);

            } catch (ControlFlow.BreakException e) {
                this.environment = previous;
                break; // Sai do loop imediatamente!

            } catch (ControlFlow.ContinueException e) {
                this.environment = previous;
                // O continue interrompe a execução do bloco, mas deixamos o loop saltar para o 'finally' e continuar!

            } finally {
                this.environment = previous;
            }

            // Faz o salto para a próxima iteração
            current += jumpVal;
        }

        return null;
    }

    // ==========================================
    // AVALIAÇÃO DE EXPRESSÕES (EXPRESSIONS)
    // ==========================================

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitArrayExpr(Expr.ArrayLiteral expr) {
        List<Object> array = new ArrayList<>();
        for (Expr element : expr.elements) {
            array.add(evaluate(element));
        }
        return array;
    }

    @Override
    public Object visitCompoundAssignExpr(Expr.CompoundAssign expr) {
        // 1. Vai buscar o valor atual da variável à memória
        Object currentValue = environment.get(expr.name.lexeme);
        // 2. Calcula o valor da direita
        Object rightValue = evaluate(expr.value);

        // 3. Faz as contas
        Object newValue = null;
        switch (expr.operator.type) {
            case PLUS_ASSIGN:
                if (currentValue instanceof Double || rightValue instanceof Double) newValue = toDouble(currentValue) + toDouble(rightValue);
                else if (currentValue instanceof Long && rightValue instanceof Long) newValue = (long) currentValue + (long) rightValue;
                else if (currentValue instanceof String || rightValue instanceof String) newValue = currentValue + String.valueOf(rightValue);
                break;
            case MINUS_ASSIGN:
                if (currentValue instanceof Double || rightValue instanceof Double) newValue = toDouble(currentValue) - toDouble(rightValue);
                else newValue = (long) currentValue - (long) rightValue;
                break;
            case STAR_ASSIGN:
                if (currentValue instanceof Double || rightValue instanceof Double) newValue = toDouble(currentValue) * toDouble(rightValue);
                else newValue = (long) currentValue * (long) rightValue;
                break;
            case SLASH_ASSIGN:
                if (currentValue instanceof Double || rightValue instanceof Double) newValue = toDouble(currentValue) / toDouble(rightValue);
                else newValue = (long) currentValue / (long) rightValue;
                break;
            case MODULO_ASSIGN:
                if (currentValue instanceof Double || rightValue instanceof Double) newValue = toDouble(currentValue) % toDouble(rightValue);
                else newValue = (long) currentValue % (long) rightValue;
                break;
            case HASH_ASSIGN: // Divisão inteira
                newValue = (long) toDouble(currentValue) / (long) toDouble(rightValue);
                break;
        }

        if (newValue == null) throw new ControlFlow.RuntimeError(expr.operator, "Operação inválida para estes tipos de dados.");

        // 4. Guarda o novo valor na memória!
        environment.assign(expr.name.lexeme, newValue);
        return newValue;
    }

    @Override
    public Object visitUpdateExpr(Expr.Update expr) {
        // 1. Vai buscar o valor atual
        Object currentValue = environment.get(expr.name.lexeme);

        // 2. Prepara o novo valor
        Object newValue = null;
        if (currentValue instanceof Double) {
            double val = (double) currentValue;
            newValue = (expr.operator.type == TokenType.PLUS_PLUS) ? val + 1.0 : val - 1.0;
        } else if (currentValue instanceof Long) {
            long val = (long) currentValue;
            newValue = (expr.operator.type == TokenType.PLUS_PLUS) ? val + 1L : val - 1L;
        } else {
            throw new ControlFlow.RuntimeError(expr.operator, "Só podes incrementar ou decrementar números.");
        }

        // 3. Atualiza na memória
        environment.assign(expr.name.lexeme, newValue);

        // Se for a++ (postfix), devolve o valor antigo ANTES de atualizar!
        // Se for ++a (prefix), devolve o novo valor!
        return expr.isPrefix ? newValue : currentValue;
    }

    @Override
    public Object visitIndexAccessExpr(Expr.IndexAccess expr) {
        // Avalia quem é o array (lista) e quem é o índice (ex: 0)
        Object object = evaluate(expr.object);
        Object index = evaluate(expr.index);

        if (object instanceof List) {
            List<?> list = (List<?>) object;
            if (index instanceof Long) {
                int idx = (int) (long) index; // Convertemos Long para Int porque as listas do Java pedem Int
                if (idx >= 0 && idx < list.size()) {
                    return list.get(idx);
                }
                throw new ControlFlow.RuntimeError(expr.bracket, "Índice fora dos limites do Array (Index out of bounds).");
            }
            throw new ControlFlow.RuntimeError(expr.bracket, "O índice do Array tem de ser um número inteiro.");
        }
        if (object instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) object;
            return map.get(index.toString());
        }


        throw new ControlFlow.RuntimeError(expr.bracket, "Apenas Arrays e Strings suportam acesso por índice.");
    }

    @Override
    public Object visitIndexAssignExpr(Expr.IndexAssign expr) {
        Object object = evaluate(expr.object);
        Object index = evaluate(expr.index);
        Object value = evaluate(expr.value);

        if (object instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) object;

            if (index instanceof Long) {
                int idx = (int) (long) index;
                if (idx >= 0 && idx < list.size()) {
                    list.set(idx, value);
                    return value;
                }
                throw new ControlFlow.RuntimeError(expr.bracket, "Índice fora dos limites do Array (Index out of bounds).");
            }
            throw new ControlFlow.RuntimeError(expr.bracket, "O índice do Array tem de ser um número inteiro.");
        }

        if (object instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) object;
            map.put(index.toString(), value);
            return value;
        }

        throw new ControlFlow.RuntimeError(expr.bracket, "Apenas Arrays suportam atribuição por índice.");
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        // 1. Descobre quem é o objeto à esquerda do ponto (ex: a variável ou a string literal)
        Object object = evaluate(expr.object);

        // 2. É uma Lista (Array)? Delega para ArrayMethods
        if (object instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) object;

                // 1. Tenta ver se é uma propriedade direta (ex: arr.length, arr.first)
                switch (expr.name.lexeme) {
                    case "length":
                    case "isEmpty":
                    case "first":
                    case "last":
                        return ArrayMethods.getProperty(list, expr.name.lexeme);
                }

                // 2. Se não for propriedade, devolve o método para ser executado
                return ArrayMethods.getMethod(list, expr.name.lexeme);

            } catch (RuntimeException e) {
                throw new ControlFlow.RuntimeError(expr.name, e.getMessage());
            }
        }

        // ⭐ 3. É uma String? Delega para StringMethods
        // É uma String?
        if (object instanceof String) {
            try {
                String str = (String) object;

                // ⭐ 1. INTERCETA AS PROPRIEDADES PRIMEIRO (Sem parêntesis) ⭐
                switch (expr.name.lexeme) {
                    case "length":
                    case "size":
                    case "isEmpty":
                    case "empty":
                        return StringMethods.getProperty(str, expr.name.lexeme);
                }

                // 2. Se não for propriedade, devolve a função para o visitCallExpr executar
                return StringMethods.getMethod(str, expr.name.lexeme);

            } catch (RuntimeException e) {
                throw new ControlFlow.RuntimeError(expr.name, e.getMessage());
            }
        }

        // É um Dicionário/Objeto?
        if (object instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) object;
            String propName = expr.name.lexeme;

            // 1. Propriedades especiais diretas
            if (propName.equals("length") || propName.equals("size") || propName.equals("isEmpty") || propName.equals("empty")) {
                return ObjectMethods.getProperty(map, propName);
            }

            // 2. Tenta encontrar um método nativo (ex: keys, flatten, pick)
            try {
                return ObjectMethods.getMethod(map, propName);
            } catch (RuntimeException eMethod) {
                // 3. Se não for um método nativo, é porque o utilizador quer ler uma chave (ex: obj.nome)
                try {
                    return ObjectMethods.getProperty(map, propName);
                } catch (RuntimeException eProperty) {
                    // Se também não for uma chave, aí sim, damos erro!
                    throw new ControlFlow.RuntimeError(expr.name, "A propriedade ou método '" + propName + "' não existe no objeto.");
                }
            }
        }

        throw new ControlFlow.RuntimeError(expr.name, "Apenas Arrays, Objetos e Strings possuem propriedades/métodos.");
    }
    @Override
    public Object visitArrowFunctionExpr(Expr.ArrowFunction expr) {
        // Guarda o ambiente atual para que a Arrow Function se lembre das variáveis de fora (Closure!)
        Environment closure = this.environment;

        // Criamos uma função anónima na hora
        return new XplCallable() {
            @Override
            public int arity() {
                return 1; // Recebe exatamente 1 parâmetro (ex: o 'e')
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                // 1. Cria um mini-escopo para a função
                Environment arrowEnv = new Environment(closure);

                // 2. Injeta o valor do parâmetro lá para dentro
                arrowEnv.defineLet(expr.parameter.lexeme, arguments.get(0));

                // 3. Executa o corpo da função e devolve o resultado!
                Environment previous = interpreter.environment; // Acede através da instância atual
                try {
                    // Forçamos o interpretador a usar o mini-escopo
                    // Usamos uma abordagem reflexiva ou alteramos temporariamente o escopo do interpretador
                    interpreter.executeBlock(new ArrayList<>(), arrowEnv); // Truque para mudar de escopo

                    // IMPORTANTE: Como é uma Expressão (Expr) e não um Bloco de Stmt,
                    // avaliamos a expressão diretamente com o escopo trocado temporariamente!
                    interpreter.environment = arrowEnv;
                    return interpreter.evaluate(expr.body);

                } finally {
                    interpreter.environment = previous; // Restaura sempre!
                }
            }

            @Override
            public String toString() { return "<arrow fn>"; }
        };
    }

    @Override
    public Object visitObjectLiteralExpr(Expr.ObjectLiteral expr) {
        // Usamos LinkedHashMap para manter a ordem de inserção das chaves
        Map<String, Object> map = new java.util.LinkedHashMap<>();

        for (int i = 0; i < expr.keys.size(); i++) {
            // Se a chave for um identificador (ex: nome), extraímos o lexeme, se for string extraímos o valor
            Object keyObj = evaluate(expr.keys.get(i));
            String key = (keyObj instanceof Token) ? ((Token) keyObj).lexeme : keyObj.toString();
            Object value = evaluate(expr.values.get(i));
            map.put(key, value);
        }
        return map;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name.lexeme);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        environment.assign(expr.name.lexeme, value);
        return value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        if (expr.operator.type == TokenType.MINUS) {
            checkNumberOperand(expr.operator, right);
            if (right instanceof Double) return -(double) right;
            if (right instanceof Long) return -(long) right;
        }
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case PLUS:
                if (left instanceof Double || right instanceof Double) return toDouble(left) + toDouble(right);
                if (left instanceof Long && right instanceof Long) return (long) left + (long) right;
                if (left instanceof String || right instanceof String) return left + String.valueOf(right);
                throw new ControlFlow.RuntimeError(expr.operator, "Os operandos devem ser números ou strings.");
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                if (left instanceof Double || right instanceof Double) return toDouble(left) - toDouble(right);
                return (long) left - (long) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                if (left instanceof Double || right instanceof Double) return toDouble(left) * toDouble(right);
                return (long) left * (long) right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if (toDouble(right) == 0)
                    throw new ControlFlow.RuntimeError(expr.operator, "Divisão por zero não permitida.");
                if (left instanceof Double || right instanceof Double) return toDouble(left) / toDouble(right);
                return (long) left / (long) right;
            case MODULO:
                checkNumberOperands(expr.operator, left, right);
                if (toDouble(right) == 0)
                    throw new ControlFlow.RuntimeError(expr.operator, "Divisão por zero não permitida.");
                if (left instanceof Double || right instanceof Double) return (toDouble(left) % toDouble(right));
                return (long) left % (long) right;
            case HASH:
                checkNumberOperands(expr.operator, left, right);
                if (toDouble(right) == 0)
                    throw new ControlFlow.RuntimeError(expr.operator, "Divisão por zero não permitida.");
                if (left instanceof Double || right instanceof Double) return (long)(toDouble(left) / toDouble(right));
                return (long) left / (long) right;
            case POWER:
                checkNumberOperands(expr.operator, left, right);
                if (toDouble(right) == 0)
                    throw new ControlFlow.RuntimeError(expr.operator, "Divisão por zero não permitida.");
                if (left instanceof Double || right instanceof Double) return Math.pow(toDouble(left) , toDouble(right));
                return (long)Math.pow((long) left ,(long) right);
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return toDouble(left) > toDouble(right);
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return toDouble(left) >= toDouble(right);
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return toDouble(left) < toDouble(right);
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return toDouble(left) <= toDouble(right);
            case EQUAL:
                return isEqual(left, right);
            case NOT_EQUAL:
                return !isEqual(left, right);
        }
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        // 1. Avalia o nome da função (ex: procura 'println' ou 'shell' na memória)
        Object callee = evaluate(expr.callee);

        // 2. Avalia os argumentos que passaste dentro dos parênteses
        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        // 3. Verifica se o que tentaste chamar é realmente uma função
        if (!(callee instanceof XplCallable function)) {
            throw new ControlFlow.RuntimeError(expr.paren, "Isto não é uma função e não pode ser chamado.");
        }

        // 4. Valida a quantidade de parâmetros
        if (function.arity() != -1 && arguments.size() != function.arity()) {
            throw new ControlFlow.RuntimeError(expr.paren,
                    "Esperado " + function.arity() + " argumentos, mas obteve " + arguments.size() + ".");
        }

        // 5. Executa a função de verdade!
        try {
            return function.call(this, arguments);
        } catch (RuntimeException e) {
            throw new ControlFlow.RuntimeError(expr.paren, e.getMessage());
        }
    }

    // ==========================================
    // MÉTODOS AUXILIARES
    // ==========================================

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private double toDouble(Object obj) {
        if (obj instanceof Double) return (double) obj;
        if (obj instanceof Long) return (double) (long) obj;
        return 0.0;
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double || operand instanceof Long) return;
        throw new ControlFlow.RuntimeError(operator, "O operando deve ser um número.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if ((left instanceof Double || left instanceof Long) && (right instanceof Double || right instanceof Long))
            return;
        throw new ControlFlow.RuntimeError(operator, "Ambos os operandos devem ser números.");
    }

    private void checkTypeCompatability(Token typeAnnotation, Object value) {
        switch (typeAnnotation.type) {
            case T_INT:
                if (!(value instanceof Long))
                    throw new ControlFlow.RuntimeError(typeAnnotation, "O valor atribuído não é do tipo 'long'.");
                break;
            case T_FLOAT:
                if (!(value instanceof Double || value instanceof Long))
                    throw new ControlFlow.RuntimeError(typeAnnotation, "O valor atribuído não é do tipo 'float'.");
                break;
            case T_STRING:
                if (!(value instanceof String))
                    throw new ControlFlow.RuntimeError(typeAnnotation, "O valor atribuído não é do tipo 'string'.");
                break;
            case T_ARRAY:
                if (!(value instanceof List))
                    throw new ControlFlow.RuntimeError(typeAnnotation, "O valor atribuído não é do tipo 'array'.");
                break;
        }
    }

    // ==========================================
    // MÉTODOS AUXILIARES (CORES E STRINGS)
    // ==========================================

    // Converte literais do Java para representação XPL segura no terminal
    private String stringify(Object object) {
        if (object == null) return "null";
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2); // Exibe 10 em vez de 10.0
            }
            return text;
        }
        return object.toString();
    }

    // Converte "#RRGGBB" para Códigos ANSI True Color (24-bit)
    private String hexToAnsi(String hex) {
        if (hex != null && hex.startsWith("#") && hex.length() == 7) {
            try {
                long r = Math.toIntExact(Long.valueOf(hex.substring(1, 3), 16));
                long g = Math.toIntExact(Long.valueOf(hex.substring(3, 5), 16));
                long b = Math.toIntExact(Long.valueOf(hex.substring(5, 7), 16));
                return String.format("\033[38;2;%d;%d;%dm", r, g, b);
            } catch (NumberFormatException e) {
                return ConsoleTheme.TEXT;
            }
        }
        return ConsoleTheme.TEXT;
    }
}