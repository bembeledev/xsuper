package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.core.Interpreter;

public class ModelNativeRegistry {

    public static void InjectRegistry(Interpreter interpreter){
        //resto do XplElement
        XplElementNativeModel.Registry(interpreter);
        //registo do Error
        ErrorNativeModel.Registry(interpreter);

        // ==========================================
        // NOVOS MODELOS DE DADOS NATIVOS
        // ==========================================
        DateTimeNativeModel.Registry(interpreter);
        RegexNativeModel.Registry(interpreter);
        BufferNativeModel.Registry(interpreter);

        MathNativeModel.Registry(interpreter);

        JsonNativeModel.Registry(interpreter);
        FileNativeModel.Registry(interpreter);

        HttpNativeModel.Registry(interpreter);
        OSNativeModel.Registry(interpreter);

        ConsoleNativeModel.Registry(interpreter);
        TaskNativeModel.Registry(interpreter);

        LogNativeModel.Registry(interpreter);
        AssertNativeModel.Registry(interpreter);

        // ⭐ O nosso Servidor Web Nativo!
        HttpServerNativeModel.Registry(interpreter);
        HardwareNativeModel.Registry(interpreter);
        MutexNativeModel.Registry(interpreter);

        ChannelNativeModel.Registry(interpreter);

        SecurityNativeModel.Registry(interpreter);

        DatabaseNativeModel.Registry(interpreter);

        InteropNativeModel.Registry(interpreter);

        WebSocketNativeModel.Registry(interpreter);

       SmtpNativeModel.Registry(interpreter);

        CryptoNativeModel.Registry(interpreter);

        SchedulerNativeModel.Registry(interpreter);

        StreamNativeModel.Registry(interpreter);

        CompressNativeModel.Registry(interpreter);

        NetworkNativeModel.Registry(interpreter);

        MongoNativeModel.Registry(interpreter);

        SerialNativeModel.Registry(interpreter);

        UrlNativeModel.Registry(interpreter);

        ObserverNativeModel.Registry(interpreter);

        EventLoopNativeModel.Registry(interpreter);

        SocketNativeModel.Registry(interpreter);

        ReflectNativeModel.Registry(interpreter);
    }
}
