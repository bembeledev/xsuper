package com.dic.xsuper.lsp;

import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Future;

public class XplLspLauncher {

    public static void main(String[] args) {
        // Quando o VSCode chamar este Main, o servidor acorda e escuta o System.in
        startServer(System.in, System.out);
    }

    public static void startServer(InputStream in, OutputStream out) {
        try {
            XplLanguageServer server = new XplLanguageServer();

            org.eclipse.lsp4j.jsonrpc.Launcher<LanguageClient> launcher =
                    LSPLauncher.createServerLauncher(server, in, out);

            LanguageClient client = launcher.getRemoteProxy();
            server.connect(client);

            Future<Void> startListening = launcher.startListening();
            startListening.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}