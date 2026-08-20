module files.server;

import files.backend.db;
import files.views.view.View;
import files.http.Http.Http;
import files.http.RequestHttp.RequestHttp;
import files.http.ResponseHttp.ResponseHttp;
import files.controller.HomeController.HomeController;
import files.controller.PessoaController.PessoaController;
import files.controller.UploadController.UploadController;
import files.controller.UploadViewController.UploadViewController;
// Inicializar o Servidor na porta 3000
var server = HttpServer.serve(3000, (req, res) => {

    // Configurações padrão
    res.headers = {"Content-Type": "text/html; charset=UTF-8"};
    let http = new Http(new RequestHttp(req), new ResponseHttp(res));

    let path = req.path;

    // ROTEAMENTO PRINCIPAL
    switch (path) {
        case "/home","/": {
            new HomeController(http);
        }
        case "/upload": { // ⭐ ROTA DA PÁGINA DE UPLOAD
            new UploadViewController(http);
        }
        case "/pessoa": {
            new PessoaController(http);
        }
        case "/api/upload/init", "/api/upload/chunk": {
                    // ⭐ A API responde via UploadController
            new UploadController(http);
        }
        default: {
            // Rota não encontrada
            res.status = 404;
            res.body = View.load("notfound");
        }
    }
});

println("🚀 Backend API a rodar na porta " + server.port);