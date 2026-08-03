module files.controller;

import files.http.Http.Http;
import files.controller.Controller.Controller;
import files.backend.db; // Importamos a base de dados!

declare UploadController extends Controller {}

implement UploadController {
    pub fun init(http: Http) {
        super.init(http);

        let path: ?string = http.req.request.path;
        let method: ?string = http.req.request.method;

        // Configura a resposta para ser sempre JSON nesta API
        http.res.response.headers = {"Content-Type": "application/json; charset=UTF-8"};

        if (method == "POST" && path == "/api/upload/init") {
            this.handleInit(http);
        } else if (method == "POST" && path == "/api/upload/chunk") {
            this.handleChunk(http);
        } else {
            http.res.response.status = 404;
            http.res.response.body = Json.encode({"erro": "Endpoint de API não encontrado"});
        }
    }

    // ---------------------------------------------------------
    // 1. INICIAR UPLOAD E VERIFICAR LIMITES (20MB / 50MB / 50 Ficheiros)
    // ---------------------------------------------------------
    priv fun handleInit(http: Http) {
        let payload = Json.decode(http.req.request.body);
        let fileSize = payload.size;
        let folderId = payload.pasta_id;

        // Regra 1: O arquivo excede 20MB?
        if (fileSize > 20971520) {
            http.res.response.status = 400;
            http.res.response.body = Json.encode({ "erro": "O arquivo excede o limite de 20MB." });
            return null;
        }

        // Consultar ou criar pasta
        let pasta = db.queryOne("SELECT * FROM pastas WHERE id = ?", [folderId]);
        if (pasta == null) {
            db.execute("INSERT INTO pastas (id, nome) VALUES (?, ?)", [folderId, "Nova Pasta"]);
            pasta = { "tamanho_total": 0, "qtd_arquivos": 0 };
        }

        // Regra 2: Limite de 50 arquivos?
        if (pasta.qtd_arquivos >= 50) {
            http.res.response.status = 400;
            http.res.response.body = Json.encode({ "erro": "Limite máximo de 50 arquivos atingido." });
            return null;
        }

        // Regra 3: Limite de 50MB no total da pasta?
        if (pasta.tamanho_total + fileSize > 52428800) {
            http.res.response.status = 400;
            http.res.response.body = Json.encode({ "erro": "O upload excede o limite total de 50MB da pasta." });
            return null;
        }

        let fileId = Security.randomUuid();
        let fileUrl = "http://localhost:3000/download/" + fileId;

        db.execute(
            "INSERT INTO arquivos (id, pasta_id, nome, url, size, tipo, status, bytes_recebidos) VALUES (?, ?, ?, ?, ?, ?, 'PENDENTE', 0)",
            [fileId, folderId, payload.nome, fileUrl, fileSize, payload.tipo]
        );

        db.execute(
            "UPDATE pastas SET tamanho_total = tamanho_total + ?, qtd_arquivos = qtd_arquivos + 1 WHERE id = ?",
            [fileSize, folderId]
        );

        http.res.response.status = 200;
        http.res.response.body = Json.encode({
            "sucesso": true,
            "file_id": fileId,
            "mensagem": "Upload aprovado. Podes começar a enviar os chunks!"
        });
    }

    // ---------------------------------------------------------
    // 2. RECEBER OS CHUNKS E GRAVAR NO DISCO
    // ---------------------------------------------------------
    priv fun handleChunk(http: Http) {
        let headers = http.req.request.headers;

        // Os headers HTTP vêm muitas vezes em minúsculas (depende do browser)
        let fileId = headers.get("File-Id") ?? headers.get("file-id");

        if (fileId == null) {
            http.res.response.status = 400;
            http.res.response.body = Json.encode({"erro": "File-Id ausente nos cabeçalhos."});
            return null;
        }

        let arquivo = db.queryOne("SELECT * FROM arquivos WHERE id = ?", [fileId]);

        if (arquivo == null || arquivo.status == "CONCLUIDO") {
            http.res.response.status = 400;
            http.res.response.body = Json.encode({"erro": "Ficheiro inválido ou já concluído."});
            return null;
        }

        // Criar pasta de storage e gravar bytes brutos
        File.mkdirAll(OS.userDir() + "/storage");
        let caminhoFisico = OS.userDir() + "/storage/" + fileId + ".dat";

        // O rawBody foi injetado pelo nosso ajuste no Java!
        let buffer = http.req.request.rawBody;

        File.appendBytes(caminhoFisico, buffer);

        let novosBytes = arquivo.bytes_recebidos + buffer.size;
        let novoStatus = "UPLOADING";

        if (novosBytes >= arquivo.size) {
            novoStatus = "CONCLUIDO";
            println("🎉 Upload concluído com sucesso: " + arquivo.nome);
        }

        db.execute(
            "UPDATE arquivos SET bytes_recebidos = ?, status = ? WHERE id = ?",
            [novosBytes, novoStatus, fileId]
        );

        http.res.response.status = 200;
        http.res.response.body = Json.encode({
            "sucesso": true,
            "bytes_recebidos": novosBytes,
            "status": novoStatus
        });
    }
}

export UploadController;