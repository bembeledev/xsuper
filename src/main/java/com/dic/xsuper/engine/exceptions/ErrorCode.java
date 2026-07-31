package com.dic.xsuper.engine.exceptions;

import com.dic.xsuper.engine.core.Token;

public enum ErrorCode {

    // =========================================================================
    // 📝 LEXER (XPL0001 - XPL0099)
    // =========================================================================
    XPL0001(ErrorCategory.LEXER, "Caractere inesperado ou não reconhecido: '%s'."),
    XPL0002(ErrorCategory.LEXER, "String literal não foi terminada antes do fim da linha."),
    XPL0003(ErrorCategory.LEXER, "String multi-linha não foi fechada."),
    XPL0004(ErrorCategory.LEXER, "Ficheiro terminou com um bloco de comentário '/*' não fechado."),
    XPL0005(ErrorCategory.LEXER, "Token inesperado '..'. Use '.' para propriedades ou '...' para spread."),

    // =========================================================================
    // 🌳 PARSER (XPL0100 - XPL0999)
    // =========================================================================
    XPL0100(ErrorCategory.PARSER, "Esperado '%s' após a expressão."),
    XPL0101(ErrorCategory.PARSER, "Declaração inválida: O token '%s' não é esperado neste escopo."),
    XPL0102(ErrorCategory.PARSER, "Símbolos de exportação consecutivos detetados. Separe os elementos por vírgula ','."),
    XPL0103(ErrorCategory.PARSER, "Caractere inválido '%s' na listagem de exportação."),
    XPL0104(ErrorCategory.PARSER, "Erro de Escopo: 'var' só pode ser usado ao nível do arquivo global."),
    XPL0105(ErrorCategory.PARSER, "Esperado ';' após a declaração da variável."),
    XPL0106(ErrorCategory.PARSER, "Esperado '}' após o corpo do declare."),
    XPL0107(ErrorCategory.PARSER, "Esperado '}' após o corpo da interface."),
    XPL0108(ErrorCategory.PARSER, "Esperado '}' após o corpo do implement."),
    XPL0109(ErrorCategory.PARSER, "Esperado '}' após o corpo do switch."),
    XPL0110(ErrorCategory.PARSER, "Esperado '}' após o corpo do match."),
    XPL0111(ErrorCategory.PARSER, "Esperado '(' após o comando 'while'."),
    XPL0112(ErrorCategory.PARSER, "Esperado ')' após a condição do 'while'."),
    XPL0113(ErrorCategory.PARSER, "Esperado '(' após o nome da função."),
    XPL0114(ErrorCategory.PARSER, "Esperado ')' após os parâmetros."),
    XPL0115(ErrorCategory.PARSER, "Esperado '{' antes do corpo da função concreta."),
    XPL0116(ErrorCategory.PARSER, "Esperado ':' após o nome da variável."),
    XPL0117(ErrorCategory.PARSER, "Esperado '=' após o tipo da variável."),
    XPL0118(ErrorCategory.PARSER, "Esperado 'in' após a variável do loop."),
    XPL0119(ErrorCategory.PARSER, "Esperado ':' após os valores do caso."),
    XPL0120(ErrorCategory.PARSER, "Esperado '->' para definir o retorno do tipo de função."),
    XPL0121(ErrorCategory.PARSER, "Esperado nome do tipo (ex: int, string, bool, object, array)."),
    XPL0122(ErrorCategory.PARSER, "Esperado identificador."),
    XPL0123(ErrorCategory.PARSER, "Erro de declaração: Apenas 'let' é suportado para o loop for-c-style."),
    XPL0124(ErrorCategory.PARSER, "Esperado ';' após a declaração da variável."),
    XPL0125(ErrorCategory.PARSER, "Esperado ';' após a condição do for-c-style."),
    XPL0126(ErrorCategory.PARSER, "Esperado ';' após o incremento do loop for-c-style."),
    XPL0127(ErrorCategory.PARSER, "O parâmetro Rest '...' deve ser o último parâmetro da função."),
    XPL0128(ErrorCategory.PARSER, "O parâmetro Rest '...' não pode ter um valor por defeito."),
    XPL0129(ErrorCategory.PARSER, "Métodos abstratos não podem ter corpo '{}'. Esperado ';'."),
    XPL0130(ErrorCategory.PARSER, "Apenas podes usar um modificador de acesso (pub, priv, prot)."),
    XPL0131(ErrorCategory.PARSER, "Uma constante ('const') precisa ser inicializada com um valor."),
    XPL0132(ErrorCategory.PARSER, "Esperado 'case' ou 'default' dentro do switch."),
    XPL0133(ErrorCategory.PARSER, "Só pode haver um 'default' no switch."),
    XPL0134(ErrorCategory.PARSER, "Esperado um braço do match."),
    XPL0135(ErrorCategory.PARSER, "O bloco 'try' exige pelo menos um 'catch' ou 'finally'."),
    XPL0136(ErrorCategory.PARSER, "Alvo de atribuição inválido."),
    XPL0137(ErrorCategory.PARSER, "Alvo de atribuição composta inválido."),
    XPL0138(ErrorCategory.PARSER, "Alvo inválido para incremento/decremento."),
    XPL0139(ErrorCategory.PARSER, "Expressão inesperada."),
    XPL0140(ErrorCategory.PARSER, "Não podes ter mais de 255 parâmetros."),
    XPL0141(ErrorCategory.PARSER, "Esperado ':' após o nome da propriedade."),
    XPL0142(ErrorCategory.PARSER, "Esperado '::'? (meta-acesso)"),
    XPL0143(ErrorCategory.PARSER, "Definições de @decorator não podem ser anotadas."),
    XPL0144(ErrorCategory.PARSER, "Definições de &listener não podem ser anotadas."),
    XPL0145(ErrorCategory.PARSER, "Anotações (@) e Listeners (&) devem estar anexados a uma declaração válida."),
    XPL0146(ErrorCategory.PARSER, "Esperado um literal string ou identificador após 'prefix'."),

    // =========================================================================
    // ⚙️ RUNTIME BÁSICO E MATEMÁTICA (XPL1000 - XPL1199)
    // =========================================================================
    XPL1000(ErrorCategory.RUNTIME, "Divisão por zero não é permitida."),
    XPL1001(ErrorCategory.RUNTIME, "A variável '%s' não foi declarada neste escopo."),
    XPL1002(ErrorCategory.RUNTIME, "Violação de Tipagem Estrita: A variável '%s' foi trancada como '%s', mas recebeu '%s'."),
    XPL1003(ErrorCategory.RUNTIME, "UnwrapError: Tentativa de abrir um valor nulo pelo operador '!'."),
    XPL1004(ErrorCategory.RUNTIME, "Operação inválida: Não é possível aplicar o operador '%s' entre os tipos fornecidos."),
    XPL1005(ErrorCategory.RUNTIME, "Os operandos devem ser números ou strings."),
    XPL1006(ErrorCategory.RUNTIME, "O operando deve ser um número."),
    XPL1007(ErrorCategory.RUNTIME, "Ambos os operandos devem ser números."),
    XPL1008(ErrorCategory.RUNTIME, "Operadores de bits (&, |, <<, >>) requerem valores inteiros."),
    XPL1009(ErrorCategory.RUNTIME, "Só podes incrementar números."),
    XPL1010(ErrorCategory.RUNTIME, "Alvo de atribuição composta inválido."),
    XPL1011(ErrorCategory.RUNTIME, "O índice do Array tem de ser um número inteiro."),
    XPL1012(ErrorCategory.RUNTIME, "Índice fora dos limites do Array (Index out of bounds)."),
    XPL1013(ErrorCategory.RUNTIME, "Apenas Arrays e Strings suportam acesso por índice."),
    XPL1014(ErrorCategory.RUNTIME, "Apenas Arrays suportam atribuição por índice."),
    XPL1015(ErrorCategory.RUNTIME, "O alvo do 'for-in' precisa ser um Array ou Lista iterável."),
    XPL1016(ErrorCategory.RUNTIME, "Erro de Loop: O incremento (jump) não pode ser zero."),
    XPL1017(ErrorCategory.RUNTIME, "O operador spread '...' em Arrays exige uma Lista. Tipo recebido: %s."),
    XPL1018(ErrorCategory.RUNTIME, "O operador spread em Objetos requer outro Objeto/Dicionário."),
    XPL1019(ErrorCategory.RUNTIME, "O operador spread '...' em argumentos requer um Array."),
    XPL1020(ErrorCategory.RUNTIME, "A condição do comando 'while' deve resultar num tipo bool."),
    XPL1021(ErrorCategory.RUNTIME, "Thread XPL foi morta e abortada com sucesso."),
    XPL1022(ErrorCategory.RUNTIME, "Cast Forçado Falhou (ClassCastException): Não é possível converter '%s' para o tipo %s."),
    XPL1023(ErrorCategory.RUNTIME, "Isto não é uma função ou classe instanciável e não pode ser chamado."),
    XPL1024(ErrorCategory.RUNTIME, "Falha Nativa no Motor Java: %s"),
    XPL1025(ErrorCategory.RUNTIME, "Falha de Metaprogramação: Esperado um bloco encapsulado (ex: () => { ... })."),
    XPL1026(ErrorCategory.RUNTIME, "Falha de Metaprogramação: Cápsula vazia."),
    XPL1027(ErrorCategory.RUNTIME, "Falha de Metaprogramação: Esperada uma cápsula com instrução."),

    // =========================================================================
    // 🏛️ POO & CONTRATOS (XPL1200 - XPL1999)
    // =========================================================================
    XPL1200(ErrorCategory.POO, "Decorador Ausente: O método '%s()' cumpre um contrato ou sobrepõe um pai. Exige @Override."),
    XPL1201(ErrorCategory.POO, "Erro de Sobreposição: O método '%s()' tem @Override, mas não sobrepõe nenhum contrato ou classe pai."),
    XPL1202(ErrorCategory.POO, "Quebra de Contrato Fatal: O modelo '%s' não implementou o método obrigatório '%s()' exigido pela interface '%s'."),
    XPL1203(ErrorCategory.POO, "Operação Ilegal: O modelo '%s' possui uma implementação abstrata e não pode ser instanciado diretamente."),
    XPL1204(ErrorCategory.POO, "Erro de Acesso: A propriedade '%s' é PRIVADA. Só a classe '%s' pode aceder."),
    XPL1205(ErrorCategory.POO, "Erro de Segurança (Sealed): O modelo '%s' é SELADO. Só pode ser implementado no ficheiro onde nasceu."),
    XPL1206(ErrorCategory.POO, "Erro de Acesso: A propriedade '%s' é READONLY. Só pode ser alterada dentro da própria classe."),
    XPL1207(ErrorCategory.POO, "Erro de Acesso: A propriedade '%s' é PROTEGIDA. Só acessível por herança."),
    XPL1208(ErrorCategory.POO, "Erro de Segurança: A propriedade '%s' é FINAL e não pode ser alterada."),
    XPL1209(ErrorCategory.POO, "A propriedade estática '%s' não existe ou não pode ser alterada no modelo %s."),
    XPL1210(ErrorCategory.POO, "Erro de Segurança: Este objeto é estritamente imutável (Read-Only) pois foi exportado via toObject()."),
    XPL1211(ErrorCategory.POO, "Apenas instâncias e classes XPL possuem propriedades modificáveis."),
    XPL1212(ErrorCategory.POO, "O modelo '%s' não possui uma implementação base."),
    XPL1213(ErrorCategory.POO, "A classe '%s' não possui uma superclasse."),
    XPL1214(ErrorCategory.POO, "Método '%s' não encontrado na superclasse."),
    XPL1215(ErrorCategory.POO, "A palavra-chave 'super' só pode ser usada dentro de um método herdado."),
    XPL1216(ErrorCategory.POO, "Erro de Linkage: O molde genérico '%s<...>' não foi declarado."),
    XPL1217(ErrorCategory.POO, "Aridade Genérica Incorreta: O molde '%s' requer %d parâmetro(s), mas forneceste %d."),
    XPL1218(ErrorCategory.POO, "O identificador '%s' não designa um Listener válido."),
    XPL1219(ErrorCategory.POO, "O listener '%s' recebeu argumentos, mas não possui um construtor 'init'."),
    XPL1220(ErrorCategory.POO, "O listener '%s' não foi encontrado."),
    XPL1221(ErrorCategory.POO, "Violação de Tipo ('%s'): %s"),
    XPL1222(ErrorCategory.POO, "Erro: O modelo '%s' não foi declarado."),
    XPL1223(ErrorCategory.POO, "Erro de Tipagem: '%s' é um Template Genérico. Deves instanciá-lo com tipos concretos (new %s<int>())."),
    XPL1224(ErrorCategory.POO, "Quebra de Contrato: O modelo '%s' não possui um construtor 'init', mas forneceste argumentos."),
    XPL1225(ErrorCategory.POO, "Erro Fatal: O modelo '%s' não foi declarado em lado nenhum."),
    XPL1226(ErrorCategory.POO, "O identificador '%s' não corresponde a um modelo de dados válido."),
    XPL1227(ErrorCategory.POO, "Inconsistência de Namespace: O ficheiro declara '%s' mas foi importado como '%s'."),
    XPL1228(ErrorCategory.POO, "Comando 'export' usado fora de um módulo."),
    XPL1229(ErrorCategory.POO, "Falha Crítica no Export: O símbolo '%s' não foi encontrado."),
    XPL1230(ErrorCategory.POO, "O módulo '%s' não exporta o símbolo '%s'."),
    XPL1231(ErrorCategory.POO, "Erro de Sintaxe: O prefixo '$_' é reservado para variáveis globais nativas."),
    XPL1232(ErrorCategory.POO, "Erro de Tipagem na variável global."),

    // =========================================================================
    // 🖥️ UI SUPER E NATIVOS (XPL2000 - XPL2999)
    // =========================================================================
    XPL2000(ErrorCategory.UI, "SuperUI: O componente '%s' falhou ao renderizar."),
    XPL2001(ErrorCategory.UI, "Motor Gráfico: Tentativa de atualizar um nó da UI fora da Main Thread."),
    XPL2002(ErrorCategory.UI, "Operação '?.' inválida: O alvo (do tipo %s) não possui propriedades acessíveis."),
    XPL2003(ErrorCategory.UI, "O método opcional '?.%s()' não existe ou não é invocável no objeto alvo."),
    XPL2004(ErrorCategory.UI, "A propriedade ou método '%s' não existe no objeto."),
    XPL2005(ErrorCategory.UI, "A propriedade '%s' não existe nesta MetaInstance nativa."),

    XPL3000(ErrorCategory.FATAL, "%s");
    // -------------------------------------------------------------------------
    //  MOTOR INTERNO DO ENUM
    // -------------------------------------------------------------------------
    private final ErrorCategory category;
    private final String template;

    ErrorCode(ErrorCategory category, String template) {
        this.category = category;
        this.template = template;
    }

    /**
     * Formata a mensagem com os argumentos, adicionando o prefixo com código e categoria.
     * Exemplo: "[XPL1200] Erro de Estrutura (POO): Decorador Ausente: método x()..."
     */
    public String format(Object... args) {
        String msgBase = String.format(this.template, args);
        return String.format("[%s] %s: %s", this.name(), this.category.label, msgBase);
    }

    /**
     * Cria uma exceção RuntimeError com a mensagem formatada e um token opcional.
     */
    public ControlFlow.RuntimeError runtimeError(Token token, Object... args) {
        return new ControlFlow.RuntimeError(token, format(args));
    }

    public ControlFlow.RuntimeError runtimeError(Object... args) {
        return runtimeError(null, args);
    }
}