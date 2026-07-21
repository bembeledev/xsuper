package com.dic.xsuper.lang.natives.models;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.poo.XplClass;

public class ErrorNativeModel {

    public static void Registry(Interpreter interpreter){
        // =========================================================================
        // ⭐ O GÉNESIS DA CLASSE 'Error' NATÍVA (Com Modificador de Visibilidade) ⭐
        // =========================================================================
        XPLModel baseErrorModel = new XPLModel("Error", null);
        baseErrorModel.hasBaseImplementation = true;

        // ⭐ A ARMA DESARMADA: Fabricamos um Token de visibilidade 'pub' legítimo!
        // (Nota: Se no teu TokenType o modificador público se chamar PUBLIC em vez de PUB, altera abaixo)
        Token pubToken = new Token(TokenType.PUBLIC, "pub", null, 0, 0);

        Token msgToken = new Token(TokenType.IDENTIFIER, "message", null, 0, 0);

        // Injetamos o 'pubToken' no 1º argumento em vez de 'null'!
        baseErrorModel.addField(new Stmt.FieldDecl(
                pubToken,
                false, false, false,
                msgToken,
                new TypeNode.Simple(new Token(TokenType.T_STRING, "string", null, 0, 0))
        ));

        interpreter.registry_model.put("Error", baseErrorModel);
        interpreter.environment.defineConst("Error", new XplClass(baseErrorModel, interpreter.environment));

    }
}
