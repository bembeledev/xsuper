package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.ast.Stmt;
import com.dic.xsuper.engine.ast.TypeNode;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.core.Token;
import com.dic.xsuper.engine.core.TokenType;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;

public class ErrorNativeModel {

    public static void Registry(Interpreter interpreter){
        // =========================================================================
        // ⭐ O GÉNESIS DA CLASSE 'Error' NATIVA ⭐
        // =========================================================================
        XPLModel baseErrorModel = new XPLModel("Error", null);
        baseErrorModel.hasBaseImplementation = true;

        Token pubToken = new Token(TokenType.PUBLIC, "pub", null, 0, 0);
        Token msgToken = new Token(TokenType.IDENTIFIER, "message", null, 0, 0);

        baseErrorModel.addField(new Stmt.FieldDecl(
                pubToken,
                false, false, false,
                msgToken,
                new TypeNode.Simple(new Token(TokenType.T_STRING, "string", null, 0, 0))
        ));

        // =========================================================================
        // ⭐ INJEÇÃO NATIVA DO MÉTODO toString() ⭐
        // Retorna o valor guardado na propriedade 'message' da instância.
        // =========================================================================
        Token toStringName = new Token(TokenType.IDENTIFIER, "toString", null, 0, 0);
        TypeNode returnType = new TypeNode.Simple(new Token(TokenType.T_STRING, "string", null, 0, 0));

        Token returnToken = new Token(TokenType.RETURN, "return", null, 0, 0);
        Token thisToken = new Token(TokenType.IDENTIFIER, "this", null, 0, 0);
        Token messageFieldToken = new Token(TokenType.IDENTIFIER, "message", null, 0, 0);

        Expr messageExpr = new Expr.Get(new Expr.Variable(thisToken), messageFieldToken);
        java.util.List<Stmt> body = new java.util.ArrayList<>();
        body.add(new Stmt.Return(returnToken, messageExpr));

        Stmt.Function toStringFunc = new Stmt.Function(
                pubToken, false, false, toStringName,
                new java.util.ArrayList<>(), returnType,
                new java.util.ArrayList<>(), body,
                new java.util.ArrayList<>(), new java.util.ArrayList<>()
        );
        baseErrorModel.addMethod(toStringFunc);

        interpreter.registry_model.put("Error", baseErrorModel);
        interpreter.environment.defineConst("Error", new XplClass(baseErrorModel, interpreter.environment));
    }
}