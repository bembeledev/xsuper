package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Stmt;
import com.dic.xsuper.engine.ast.TypeNode;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.core.Token;
import com.dic.xsuper.engine.core.TokenType;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;

public class XplElementNativeModel {
    public static void Registry(Interpreter interpreter) {
        // =========================================================================
        // ⭐ O GÉNESIS DA CLASSE 'XplElement' NATIVA (Pai dos Componentes) ⭐
        // =========================================================================
        XPLModel baseElementModel = new XPLModel("XplElement", null);
        baseElementModel.hasBaseImplementation = true; // É nativo, não precisa de código XPL!

        // Criamos os tokens de visibilidade para construir a AST nativa
        Token pubToken = new Token(TokenType.PUBLIC, "pub", null, 0, 0);

        // (Opcional) Podemos injetar propriedades nativas base que todos os elementos terão.
        // Exemplo: 'pub id: string'
        Token idToken = new Token(TokenType.IDENTIFIER, "id", null, 0, 0);
        baseElementModel.addField(new Stmt.FieldDecl(
                pubToken,
                false, false, false,
                idToken,
                new TypeNode.Simple(new Token(TokenType.T_STRING, "string", null, 0, 0))
        ));

        // 1. Registar o Modelo na AST para o Resolver / Type-Checker aprovar o 'extends'
        interpreter.registry_model.put("XplElement", baseElementModel);

        // 2. Registar a Classe na memória Runtime para permitir instanciar e herdar
        interpreter.environment.defineConst("XplElement", new XplClass(baseElementModel, interpreter.environment));


        // =========================================================================
        // ⭐ A INJEÇÃO DO OBJETO GLOBAL '__ui_engine' ⭐
        // =========================================================================
        // Precisamos que o __ui_engine exista no compilador para não dar "Variável indefinida"

        // Se tens uma classe wrapper nativa em Java para o teu UI Engine (que interceta o loadView),
        // tu injetas a instância dela aqui. Exemplo genérico:

        // Object nativeUiEngineInstance = ... (a tua instância do SuperUiEngine ou wrapper XplInstance)
        // interpreter.environment.defineConst("__ui_engine", nativeUiEngineInstance);

        // Nota: O compilador só precisa que a variável exista no environment global!
    }
}
