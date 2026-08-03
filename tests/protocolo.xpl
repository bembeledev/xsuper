var server = Socket.tcpServer(9090, (client) => {
    println("🌐 Novo cliente conectado: " + client.remoteIp);

    let requestLine = client.read();
    println("Requisição: " + requestLine);

    // ⭐ A CURA: Drenar os cabeçalhos até encontrar a linha vazia!
    // Isto impede que a conexão aborte abruptamente.
    while (true) {
        let header = client.read();
        if (header == null || header == "") {
            break;
        }
    }

    // Agora sim, respondemos e fechamos educadamente!
    let html = "<html><body style='background:#0f172a; color:white; padding:40px;'><h1>🚀 Servidor XPL Limpo!</h1></body></html>";

    client.writeLine("HTTP/1.1 200 OK");
    client.writeLine("Content-Type: text/html; charset=UTF-8");
    client.writeLine("Connection: close");
    client.writeLine("");
    client.writeLine(html);
    client.close();
});
println("Servidor a ouvir na porta 9090...");