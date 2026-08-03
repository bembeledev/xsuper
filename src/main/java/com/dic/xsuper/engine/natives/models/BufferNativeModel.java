package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.ast.Stmt;
import com.dic.xsuper.engine.ast.TypeNode;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.core.Token;
import com.dic.xsuper.engine.core.TokenType;
import com.dic.xsuper.engine.exceptions.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;
import com.dic.xsuper.engine.poo.XplInstance;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class BufferNativeModel {
    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Buffer", null);
        model.hasBaseImplementation = true;
        Token pubToken = new Token(TokenType.PUBLIC, "pub", null, 0, 0);

        model.addField(new Stmt.FieldDecl(pubToken, false, true, false, new Token(TokenType.IDENTIFIER, "size", null, 0, 0), new TypeNode.Simple(new Token(TokenType.T_INT, "int", null, 0, 0))));

        XplClass bufferClass = new XplClass(model, interpreter.globals);

        // =========================================================
        // MÉTODO ESTÁTICO: Buffer.alloc(tamanho)
        // =========================================================
        model.staticFields.put("alloc", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                int size = ((Number) intp.evaluate(args.getFirst().expression)).intValue();
                byte[] data = new byte[size];

                XplInstance inst = new XplInstance(bufferClass);
                inst.fields.put("size", (long) size);
                inst.fields.put("_bytes", data); // Variável oculta do XPL para guardar o array Java original

                // Método .write(index, byte)
                inst.fields.put("write", new XplCallable() {
                    @Override public int arity() { return 2; }
                    @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                        int index = ((Number) i.evaluate(a.get(0).expression)).intValue();
                        int value = ((Number) i.evaluate(a.get(1).expression)).intValue();
                        if (index >= 0 && index < size) data[index] = (byte) value;
                        return null;
                    }
                });

                // Método .read(index)
                inst.fields.put("read", new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                        int index = ((Number) i.evaluate(a.get(0).expression)).intValue();
                        if (index >= 0 && index < size) return (long) (data[index] & 0xFF);
                        throw new ControlFlow.RuntimeError(null, "Buffer OutOfBounds: Índice fora dos limites.");
                    }
                });

                // Método .writeString(index, "texto")
                inst.fields.put("writeString", new XplCallable() {
                    @Override public int arity() { return 2; }
                    @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                        int index = ((Number) i.evaluate(a.get(0).expression)).intValue();
                        String str = i.stringify(i.evaluate(a.get(1).expression));
                        byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
                        int len = Math.min(strBytes.length, size - index);
                        System.arraycopy(strBytes, 0, data, index, len);
                        return (long) len; // devolve quantos bytes escreveu
                    }
                });

                // Método .toString()
                inst.fields.put("toString", new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                        // Converte o buffer de bytes para string ignorando bytes vazios (0) no final
                        return new String(data, StandardCharsets.UTF_8).trim();
                    }
                });

                return inst;
            }
        });

        interpreter.registry_model.put("Buffer", model);
        interpreter.environment.defineConst("Buffer", bufferClass);
    }
}
