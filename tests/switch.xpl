var status = 404;

// 1. Usado como Expressão (com Injeção Direta e Múltiplos Casos)
let mensagem = switch (status) {
    case 200, 201: "Sucesso Absoluto!";
    case 400:      "Erro do Cliente";
    case 404:      "Recurso não encontrado!";
    case 500: {
        println("Alerta interno enviado ao admin!");
        "Falha no Servidor"; // O retorno implícito dentro de um bloco!
    }
    default: "Código desconhecido";
};

println("Resposta do Roteador: " + mensagem);

// 2. Usado como Comando (Sem Variável, Efeito Colateral Puro)
switch (status) {
    case 404: println("Acionado Log de 404...");
    default:  println("Nada a registar.");
};