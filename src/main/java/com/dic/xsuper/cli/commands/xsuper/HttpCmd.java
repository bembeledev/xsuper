package com.dic.xsuper.cli.commands.xsuper;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

public class HttpCmd implements Command {

    @Override
    public String getName() {
        return "http";
    }

    @Override
    public String getDescription() {
        return "Faz requisições HTTP/REST diretamente do terminal. Uso: http <METODO> <URL> [BODY]";
    }

    @Override
    public String getDetailedInfo() {
        return """
                 Faz requisições de rede usando o HttpClient nativo do Java.
                 Suporta requisições diretas no terminal ou execução de pequenos ficheiros .http.

                 Exemplos diretos:
                   > http GET https://jsonplaceholder.typicode.com/todos/1
                   > http POST https://api.exemplo.com/users '{"nome": "Fernando"}'

                 Exemplo com ficheiro:
                   > http run api.http
                   (O ficheiro deve ter 'METODO URL' na 1ª linha e o JSON BODY nas seguintes)
               """;
    }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 3) {
            System.out.println(ConsoleTheme.WARNING + "  Uso: http <METODO> <URL> [JSON_BODY] ou http run <arquivo.http>" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        String method = args[1].toUpperCase();
        String url = "";
        String body = "";
        java.util.Map<String, String> headers = new java.util.HashMap<>();

        // ⭐ MODO LEITURA DE FICHEIRO (.http)
        // ⭐ MODO LEITURA DE FICHEIRO INTELIGENTE
        if (method.equals("RUN")) {
            String fileName = args[2];
            Path filePath = currentDirectory.resolve(fileName);
            if (!java.nio.file.Files.exists(filePath)) {
                System.out.println(ConsoleTheme.ERROR + " ❌ Ficheiro não encontrado: " + fileName + ConsoleTheme.RESET);
                return currentDirectory;
            }

            String[] lines = java.nio.file.Files.readString(filePath).split("\\r?\\n");
            String[] firstLine = lines[0].trim().split("\\s+");
            method = firstLine[0].toUpperCase();
            url = firstLine[1];

            boolean readingHeaders = true;
            StringBuilder bodyBuilder = new StringBuilder();

            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                if (readingHeaders) {
                    if (line.trim().isEmpty()) {
                        readingHeaders = false; // Linha em branco = Início do Body!
                    } else {
                        String[] hParts = line.split(":", 2);
                        if (hParts.length == 2) headers.put(hParts[0].trim(), hParts[1].trim());
                    }
                } else {
                    bodyBuilder.append(line).append("\n");
                }
            }
            body = bodyBuilder.toString().trim();
        }
        // ⭐ MODO CLI DIRECTO
        else {
            url = args[2];
            if (args.length > 3) {
                StringBuilder sb = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    sb.append(args[i]).append(" ");
                }
                body = sb.toString().trim();

                // Limpa aspas envolventes do terminal
                if ((body.startsWith("'") && body.endsWith("'")) || (body.startsWith("\"") && body.endsWith("\""))) {
                    body = body.substring(1, body.length() - 1);
                }
            }
        }

        System.out.println(ConsoleTheme.WARNING + " 🌐 A enviar requisição " + method + " para: " + url + "..." + ConsoleTheme.RESET);

        // Cliente HTTP robusto com Timeout
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // ⭐ 1. REMOVE O.header() DAQUI:
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url));

        // ⭐ 2. INJECTA OS CABEÇALHOS DINÂMICOS AQUI:
        for (java.util.Map.Entry<String, String> entry : headers.entrySet()) {
            requestBuilder.header(entry.getKey(), entry.getValue());
        }

        // Proteção: Se tem body mas o ficheiro/utilizador não definiu Content-Type, assumimos JSON
        if (!headers.containsKey("Content-Type") && !headers.containsKey("content-type") && !body.isEmpty()) {
            requestBuilder.header("Content-Type", "application/json");
        }

        // 3. Prepara o Body (se estiver vazio, envia um 'noBody')
        HttpRequest.BodyPublisher publisher = body.isEmpty() ?
                HttpRequest.BodyPublishers.noBody() :
                HttpRequest.BodyPublishers.ofString(body);

        // 4. Proteção e injeção do método
        if ((method.equals("GET") || method.equals("DELETE") || method.equals("HEAD")) && body.isEmpty()) {
            requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            requestBuilder.method(method, publisher);
        }

        try {
            HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            String statusColor = (status >= 200 && status < 300) ? ConsoleTheme.SUCCESS : ConsoleTheme.ERROR;

            System.out.println(statusColor + " 📄 Status: " + status + ConsoleTheme.RESET);
            System.out.println(ConsoleTheme.TEXT + response.body() + ConsoleTheme.RESET);

        } catch (Exception e) {
            System.out.println(ConsoleTheme.ERROR + " ❌ Falha na ligação: " + e.getMessage() + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }
}