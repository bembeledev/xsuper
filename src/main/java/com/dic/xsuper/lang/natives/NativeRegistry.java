package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.Interpreter;

public class NativeRegistry {
    public static void InjectRegistry(Interpreter interpreter){
        //  funções nativas nativo
        NativeConsole.register(interpreter);
        NativeFileSystem.register(interpreter);
        NativeMath.register(interpreter);
        NativeRegex.register(interpreter);
        NativeHttp.register(interpreter);
        NativeUrl.register(interpreter);
        NativeNetwork.register(interpreter);
        NativeTask.register(interpreter);
        // ⭐ CONCORRÊNCIA E DATAFLOW AVANÇADO ⭐
        NativeMutex.register(interpreter);    // Injeção de Locks
        NativeChannel.register(interpreter);  // Injeção de Canais
        NativeSecurity.register(interpreter);  // Injeção de Security
        NativeCryptoExtended.register(interpreter);  // Injeção de Security
        NativeCryptoAdvanced.register(interpreter);  // Injeção de Security
        NativeStream.register(interpreter);  // Injeção de Stream
        NativeInterop.register(interpreter);  // Injeção de Interpolação
        NativeHardware.register(interpreter);  // Injeção de Comunicação do o Hardware
        NativeOS.register(interpreter);  // Injeção de Comunicação com o Sistema Operacional
        NativeDatabase.register(interpreter);  // Injeção de Banco de Dados
    }
}
