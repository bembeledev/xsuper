package com.dic.xsuper.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;

import java.util.concurrent.CompletableFuture;

public class XplLanguageServer implements LanguageServer, LanguageClientAware {

    private final XplTextDocumentService textDocumentService;
    private final WorkspaceService workspaceService;
    private LanguageClient client;

    public XplLanguageServer() {
        this.textDocumentService = new XplTextDocumentService();

        this.workspaceService = new WorkspaceService() {
            @Override public void didChangeConfiguration(DidChangeConfigurationParams params) {}
            @Override public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {}
        };
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        ServerCapabilities capabilities = new ServerCapabilities();

        // Diz ao VSCode que queremos receber o texto completo a cada tecla digitada
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);

        return CompletableFuture.completedFuture(new InitializeResult(capabilities));
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        System.exit(0);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
        this.textDocumentService.setClient(client);
    }
}