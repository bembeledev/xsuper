module app.main;

var autor = "Samito Macassa";
var linguagem = "Super XPL";
var versao = 2.0;

// 1. Teste de String Multi-linha (Com quebras de linha reais!)
var cabecalho = """
=====================================
    BEM-VINDO AO NOVO MOTOR XPL
=====================================
""";

println(cabecalho);

// 2. Teste de Interpolação Avançada com expressões complexas
var mensagem = """
        Olá, sou o ${autor}!
        A minha linguagem ${linguagem}
            acaba de atingir a versão ${versao}.
        A soma de 50 + 50
            dentro de uma string é ${50 + 50}!
        "Bom Duo"
""";
println(mensagem);

// 3. String normal com interpolação
println("Um teste numa linha só: A área de um círculo raio 2 é ${3.14 * (2 * 2)}.");