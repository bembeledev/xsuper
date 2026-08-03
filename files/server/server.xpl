module files.server;
import files.backend.db;
import files.notfound.NotFond;
import files.controller.Controller.Controller;

// Inicializar o Servidor na porta 3000
var server = HttpServer.serve(3000, (req, res) => {

    new Controller(req);
    // Roteamento
    //if (req.method == "POST" && req.path == "/api/upload/init") {

    // Rota não encontrada
    res.status = 404;
    res.headers = {"Content-Type": "text/html; charset=UTF-8"};
    res.body = NotFond.view;
});


println("🚀 Backend API a rodar na porta " + server.port);