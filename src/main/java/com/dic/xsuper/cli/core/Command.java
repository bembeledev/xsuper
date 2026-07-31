package com.dic.xsuper.cli.core;

import java.nio.file.Path;

public interface Command {
    String getName();
    String getDescription();
    // Retorna o diretório atualizado (necessário para comandos como 'cd')
    Path execute(String[] args, Path currentDirectory) throws Exception;
}