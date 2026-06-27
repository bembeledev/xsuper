package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.ControlFlow;
import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NativeHttp {
    // Cliente global reutilizável para máxima performance
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static void register(Interpreter interpreter) {

        // http_get(url) -> Devolve apenas a string do corpo (para uso rápido)
        interpreter.globals.defineConst("http_get", build(1, args -> {
            try {
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(args.get(0).toString())).GET().build();
                return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
            } catch (Exception e) { throw new ControlFlow.RuntimeError(null, "Erro HTTP GET: " + e.getMessage()); }
        }));

        // http_request(url, metodo, body) -> Devolve um Objeto XPL { status: 200, body: "..." }
        interpreter.globals.defineConst("http_request", new XplCallable() {
            @Override public int arity() { return 3; } // url, metodo, body (pode ser null)
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String url = intp.evaluate(args.get(0).expression).toString();
                String method = intp.evaluate(args.get(1).expression).toString();
                Object rawBody = intp.evaluate(args.get(2).expression);

                try {
                    HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url));

                    if (rawBody != null && !rawBody.toString().isEmpty()) {
                        builder.method(method, HttpRequest.BodyPublishers.ofString(rawBody.toString()));
                    } else {
                        builder.method(method, HttpRequest.BodyPublishers.noBody());
                    }

                    HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

                    // Retorna um Objeto/Dicionário para o XPL!
                    Map<String, Object> responseObj = new HashMap<>();
                    responseObj.put("status", (long) response.statusCode());
                    responseObj.put("body", response.body());
                    return responseObj;

                } catch (Exception e) { throw new ControlFlow.RuntimeError(null, "Erro HTTP Request: " + e.getMessage()); }
            }
        });
    }

    private interface NativeAction { Object execute(List<Object> args); }

    private static XplCallable build(int arity, NativeAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                List<Object> vals = new java.util.ArrayList<>();
                for (var arg : args) vals.add(intp.evaluate(arg.expression));
                return action.execute(vals);
            }
        };
    }
}