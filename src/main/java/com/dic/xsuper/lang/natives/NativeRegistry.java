package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.Interpreter;

public class NativeRegistry {
    public static void InjectRegistry(Interpreter interpreter) {
        // ─── MÓDULOS BÁSICOS ────────────────────────────────────────────────

        // NativeConsole: Leitura e escrita no terminal (print, println, read_string, read_int, prompt, etc.)
        NativeConsole.register(interpreter);

        // NativeFileSystem: Operações com ficheiros e diretórios (ler, escrever, copiar, mover, listar, eliminar, etc.)
        NativeFileSystem.register(interpreter);

        // NativeMath: Funções matemáticas (sin, cos, sqrt, pow, random, min, max, etc.)
        NativeMath.register(interpreter);

        // NativeRegex: Expressões regulares (match, test, replace, split, captura de grupos)
        NativeRegex.register(interpreter);

        // NativeHttp: Cliente HTTP simples (GET, POST, PUT, DELETE com corpo e cabeçalhos)
        NativeHttp.register(interpreter);

        // NativeUrl: Codificação/decodificação de URLs e parsing de host
        NativeUrl.register(interpreter);

        // NativeNetwork: Utilitários de rede (ping, dns_lookup)
        NativeNetwork.register(interpreter);

        // NativeTask: Concorrência e tarefas assíncronas (task_run, task_await, task_sleep, task_cancel, etc.)
        NativeTask.register(interpreter);

        // ─── CONCORRÊNCIA AVANÇADA E DATAFLOW ──────────────────────────────

        // NativeMutex: Mutexes para sincronização entre threads (lock/unlock)
        NativeMutex.register(interpreter);

        // NativeChannel: Canais de comunicação entre threads (send/receive) – padrão CSP
        NativeChannel.register(interpreter);

        // ─── SEGURANÇA E CRIPTOGRAFIA ──────────────────────────────────────

        // NativeSecurity: Hashes (MD5, SHA, BLAKE2), HMAC, AES, ChaCha20, RSA, ECDSA, KDF (PBKDF2, Argon2), Base58, RNG
        NativeSecurity.register(interpreter);

        // NativeCryptoExtended: Funcionalidades criptográficas adicionais (ECDSA com curvas personalizadas, híbrido AES+RSA, certificados X.509 auto-assinados, JWT, SSH keys)
        NativeCryptoExtended.register(interpreter);

        // NativeCryptoAdvanced: CSR (Certificate Signing Requests), assinatura de CSR por CA, PKCS#12, SSH Ed25519
        NativeCryptoAdvanced.register(interpreter);

        // ─── I/O E STREAMS ──────────────────────────────────────────────────

        // NativeStream: Operações de baixo nível com ficheiros (RandomAccessFile, seek, tell, read/write chunks)
        NativeStream.register(interpreter);

        // ─── INTEROPERABILIDADE ─────────────────────────────────────────────

        // NativeInterop: Execução de scripts (Python, Node, etc.), spawn de processos com controlo total (cwd, env, timeout, charset, stdin/stdout/stderr), base para FFI (JNA)
        NativeInterop.register(interpreter);

        // ─── HARDWARE E SISTEMA OPERACIONAL ────────────────────────────────

        // NativeHardware: Informações detalhadas sobre CPU (modelo, cores, temperatura, uso), memória, discos, rede, GPU, bateria, motherboard/BIOS
        NativeHardware.register(interpreter);

        // NativeOS: Interação com o Sistema Operacional (nome, versão, arquitetura, hostname, utilizador, variáveis de ambiente, caminhos do sistema, execução de comandos, PID, sleep, exit)
        NativeOS.register(interpreter);

        // ─── BANCO DE DADOS ──────────────────────────────────────────────────

        // NativeDatabase: Conexão a bases de dados via JDBC (PostgreSQL, MySQL, SQLite, H2, Oracle, etc.) com suporte a queries, transações, prepared statements
        NativeDatabase.register(interpreter);

        // ─── NOVOS MÓDULOS (ADICIONADOS RECENTEMENTE) ──────────────────────

        // NativeJson: Codificação/decodificação JSON (json_encode, json_decode)
        NativeJson.register(interpreter);

        // NativeCompress: Compressão multi-formato (ZIP com senha, TAR, TAR.GZ, TAR.BZ2, TAR.XZ, GZIP, BZIP2, XZ, 7‑Zip)
        NativeCompress.register(interpreter);

        // NativeHttpServer: Servidor HTTP embutido (cria servidor web com handlers em XPL)
        NativeHttpServer.register(interpreter);

        // NativeScheduler: Agendador de tarefas (intervalos fixos e expressões cron)
        NativeScheduler.register(interpreter);

        // NativeLogging: Sistema de logs com níveis (DEBUG, INFO, WARN, ERROR), saída para ficheiro e histórico em memória
        NativeLogging.register(interpreter);

        // NativeAssert: Asserções para testes unitários (assert_eq, assert_neq, assert_true, assert_false, assert_throws)
        NativeAssert.register(interpreter);

        // NativeWebSocket: Cliente WebSocket (conexão, envio, recepção, fecho)
        NativeWebSocket.register(interpreter);

        // NativeSmtp: Envio de e‑mail via SMTP (com autenticação e TLS/SSL)
        NativeSmtp.register(interpreter);

        // NativeSerial: Comunicação via porta série (RS‑232 / USB‑serial) – listagem, abertura, leitura/escrita, configuração de baudrate, etc.
        NativeSerial.register(interpreter);
    }
}
