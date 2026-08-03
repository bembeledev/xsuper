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
        Token thisToken = new Token(TokenType.IDENTIFIER, "this", null, 0, 0);

        // Adiciona a Propriedade 'message'
        baseErrorModel.addField(new Stmt.FieldDecl(
                pubToken,
                false, false, false,
                msgToken,
                new TypeNode.Simple(new Token(TokenType.T_STRING, "string", null, 0, 0))
        ));

        // =========================================================================
        // ⭐ INJEÇÃO NATIVA DO CONSTRUTOR: init(msg: string = "Erro fatal") ⭐
        // =========================================================================
        Token initName = new Token(TokenType.IDENTIFIER, "init", null, 0, 0);
        Token paramName = new Token(TokenType.IDENTIFIER, "msg", null, 0, 0);

        // 1. Criar o parâmetro 'msg' com um valor por defeito elegante
        Expr.Literal defaultMessage = new Expr.Literal("Ocorreu um erro interno e inesperado no sistema.");
        Stmt.Param msgParam = new Stmt.Param(
                paramName,
                new TypeNode.Simple(new Token(TokenType.T_STRING, "string", null, 0, 0)),
                defaultMessage,
                false
        );
        java.util.List<Stmt.Param> initParams = new java.util.ArrayList<>();
        initParams.add(msgParam);

        // 2. Construir o corpo da função: this.message = msg;
        Expr.Variable thisLeft = new Expr.Variable(thisToken);
        Expr.Variable paramRight = new Expr.Variable(paramName);
        Expr.Set setMsgExpr = new Expr.Set(thisLeft, msgToken, paramRight); // Faz a atribuição

        java.util.List<Stmt> initBody = new java.util.ArrayList<>();
        initBody.add(new Stmt.ExpressionStmt(setMsgExpr));

        // 3. Empacotar tudo no Stmt.Function do init()
        Stmt.Function initFunc = new Stmt.Function(
                pubToken, false, false, initName,
                initParams, null, // O init não tem tipo de retorno (void/null)
                new java.util.ArrayList<>(), initBody,
                new java.util.ArrayList<>(), new java.util.ArrayList<>()
        );
        baseErrorModel.addMethod(initFunc);

        // =========================================================================
        // ⭐ INJEÇÃO NATIVA DO MÉTODO toString() ⭐
        // =========================================================================
        Token toStringName = new Token(TokenType.IDENTIFIER, "toString", null, 0, 0);
        TypeNode returnType = new TypeNode.Simple(new Token(TokenType.T_STRING, "string", null, 0, 0));
        Token returnToken = new Token(TokenType.RETURN, "return", null, 0, 0);

        Expr messageExpr = new Expr.Get(new Expr.Variable(thisToken), msgToken);
        java.util.List<Stmt> body = new java.util.ArrayList<>();
        body.add(new Stmt.Return(returnToken, messageExpr));

        Stmt.Function toStringFunc = new Stmt.Function(
                pubToken, false, false, toStringName,
                new java.util.ArrayList<>(), returnType,
                new java.util.ArrayList<>(), body,
                new java.util.ArrayList<>(), new java.util.ArrayList<>()
        );
        baseErrorModel.addMethod(toStringFunc);

        // Regista o molde e a classe global
        interpreter.registry_model.put("Error", baseErrorModel);
        interpreter.environment.defineConst("Error", new XplClass(baseErrorModel, interpreter.environment));
    }
}