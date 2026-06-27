println("A verificar servidor...");
if (ping("api.github.com")) {
    println("Servidor Online! A obter dados...");

    // O 3º argumento (body) é null porque é um GET
    let resposta = http_request("https://api.github.com/users/fernando", HTTP.GET, null);

    if (resposta.status == 200) {
        println("Sucesso! Dados recebidos.");
        // Imprime o corpo da resposta
        println(resposta.body);
    } else {
        println("Erro na API. Status: " + resposta.status);
    }
} else {
    println("Rede indisponível.");
}