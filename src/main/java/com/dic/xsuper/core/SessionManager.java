package com.dic.xsuper.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SessionManager {
    // Guarda os comandos executados
    public static final List<String> HISTORY = new ArrayList<>();

    // Guarda os teus atalhos personalizados (Ex: "ll" -> "ls")
    public static final Map<String, String> ALIASES = new LinkedHashMap<>();

    // Guarda as variáveis de ambiente carregadas de ficheiros .env
    public static final Map<String, String> ENV_VARS = new LinkedHashMap<>();
}