import files.controller.Controller.{Controller};
import files.http.Http.Http;
import files.http.RequestHttp.RequestHttp;
import files.http.ResponseHttp.ResponseHttp;

// Inicializar o Servidor na porta 3000
var server = HttpServer.serve(3000, (req, res) => {
    res.headers = {"Content-Type": "text/html; charset=UTF-8"};
    let http = new Http(new RequestHttp(req), new ResponseHttp(res));

    new Controller(http);
});

println("🚀 Backend API a rodar na porta " + server.port);