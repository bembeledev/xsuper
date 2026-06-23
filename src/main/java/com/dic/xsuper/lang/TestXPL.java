package com.dic.xsuper.lang;

import com.dic.xsuper.utils.ConsoleTheme;

import java.util.List;

public class TestXPL {
    public static void main(String[] args) {
        String sourceCode = """
                
                """;

            Lexer lexer = new Lexer(sourceCode, "filePath");
            List<Token> tokens = lexer.tokenize();

            // Imprime todos os tokens lidos para garantirmos que o Scanner funciona perfeitamente!
            for (Token token : tokens) {
                System.out.println(ConsoleTheme.TEXT + token.toString() + ConsoleTheme.RESET);
            }

    }
}
