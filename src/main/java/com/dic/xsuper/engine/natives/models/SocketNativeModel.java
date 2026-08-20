package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.exceptions.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.execution.XplFunction;
import com.dic.xsuper.engine.natives.XplNativeObject;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SocketNativeModel {

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Socket", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        // ─── Socket.tcpConnect(host, port) ──────────────────────────────
        model.staticFields.put("tcpConnect", buildAction(2, (intp, args) -> {
            String host = getString(intp, args, 0);
            int port = (int) getLong(intp, args, 1, 80);

            try {
                Socket socket = new Socket(host, port);
                return new TcpClientWrapper(socket);
            } catch (IOException e) {
                throw new ControlFlow.RuntimeError(null, "Falha ao conectar via TCP: " + e.getMessage());
            }
        }));

        // ─── Socket.tcpServer(port, (client) => { ... }) ────────────────
        model.staticFields.put("tcpServer", buildAction(2, (intp, args) -> {
            int port = (int) getLong(intp, args, 0, 8080);
            Object handlerObj = intp.evaluate(args.get(1).expression);

            if (!(handlerObj instanceof XplFunction handler)) {
                throw new ControlFlow.RuntimeError(null, "Socket.tcpServer: o handler deve ser uma função (client) => { ... }");
            }

            try {
                ServerSocket serverSocket = new ServerSocket(port);
                TcpServerWrapper serverWrapper = new TcpServerWrapper(serverSocket, handler, intp);
                serverWrapper.start();
                return serverWrapper;
            } catch (IOException e) {
                throw new ControlFlow.RuntimeError(null, "Falha ao iniciar Servidor TCP: " + e.getMessage());
            }
        }));

        // ─── Socket.udpSocket(port) ─────────────────────────────────────
        model.staticFields.put("udpSocket", buildAction(1, (intp, args) -> {
            int port = (int) getLong(intp, args, 0, 0); // 0 = porta aleatória

            try {
                DatagramSocket socket = new DatagramSocket(port);
                return new UdpSocketWrapper(socket);
            } catch (SocketException e) {
                throw new ControlFlow.RuntimeError(null, "Falha ao criar Socket UDP: " + e.getMessage());
            }
        }));

        // ─── REGISTAR ───────────────────────────────────────────────────
        interpreter.registry_model.put("Socket", model);
        interpreter.environment.defineConst("Socket", new XplClass(model, interpreter.globals));
    }

    // =========================================================================
    // 🌐 CLIENTE TCP (Wrappa a ligação crua e suporta Buffers Binários!)
    // =========================================================================
    private static class TcpClientWrapper implements XplNativeObject {
        private final Socket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private final BufferedReader reader;
        private final BufferedWriter writer;
        private boolean closed = false;

        public TcpClientWrapper(Socket socket) throws IOException {
            this.socket = socket;
            this.inputStream = socket.getInputStream();
            this.outputStream = socket.getOutputStream();
            this.reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            this.writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        }

        @Override public void invokeMethod() {}

        @Override
        public Object invokeMethod(String methodName, List<Object> args, Interpreter interpreter) {
            if (closed) throw new ControlFlow.RuntimeError(null, "Socket TCP já fechado.");

            try {
                return switch (methodName) {
                    case "write" -> {
                        String data = (String) args.getFirst();
                        writer.write(data);
                        writer.flush();
                        yield true;
                    }
                    case "writeLine" -> {
                        String data = (String) args.getFirst();
                        writer.write(data);
                        writer.newLine();
                        writer.flush();
                        yield true;
                    }
                    // ⭐ NOVO: Escreve dados binários puros a partir de um Buffer XPL
                    case "writeBytes" -> {
                        Object bufferObj = args.getFirst();
                        if (bufferObj instanceof com.dic.xsuper.engine.poo.XplInstance bufferInstance) {
                            byte[] rawBytes = (byte[]) bufferInstance.fields.get("_bytes");
                            if (rawBytes != null) {
                                outputStream.write(rawBytes);
                                outputStream.flush();
                                yield (long) rawBytes.length;
                            }
                        }
                        throw new ControlFlow.RuntimeError(null, "O método writeBytes requer uma instância de Buffer.");
                    }
                    case "read" -> reader.readLine();
                    case "readAll" -> {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null && !line.isEmpty()) sb.append(line).append("\n");
                        yield sb.toString();
                    }
                    // ⭐ NOVO: Lê N bytes do socket para um Buffer XPL
                    case "readBytes" -> {
                        int length = ((Number) args.getFirst()).intValue();
                        byte[] rawBytes = new byte[length];

                        // Garante a drenagem correta dos buffers caso tenham usado strings antes
                        writer.flush();

                        int bytesLidos = inputStream.read(rawBytes);

                        if (bytesLidos <= 0) yield null; // Fim da stream

                        // Ajusta o array caso tenha lido menos do que o pedido
                        if (bytesLidos < length) {
                            byte[] trimmed = new byte[bytesLidos];
                            System.arraycopy(rawBytes, 0, trimmed, 0, bytesLidos);
                            rawBytes = trimmed;
                        }

                        // Instancia um Buffer XPL usando a Classe definida no teu ambiente
                        Object bufferClassObj = interpreter.globals.get("Buffer");
                        if (bufferClassObj instanceof com.dic.xsuper.engine.poo.XplClass bufferClass) {
                            com.dic.xsuper.engine.poo.XplInstance bufferInstance = new com.dic.xsuper.engine.poo.XplInstance(bufferClass);
                            bufferInstance.fields.put("_bytes", rawBytes);
                            bufferInstance.fields.put("size", (long) rawBytes.length);
                            yield bufferInstance;
                        }
                        throw new ControlFlow.RuntimeError(null, "Módulo Buffer não encontrado no núcleo!");
                    }
                    case "close" -> {
                        close();
                        yield null;
                    }
                    default -> throw new ControlFlow.RuntimeError(null, "Método TCP não suportado: " + methodName);
                };
            } catch (IOException e) {
                throw new ControlFlow.RuntimeError(null, "Erro de I/O no Socket TCP: " + e.getMessage());
            }
        }

        @Override
        public Object getProperty(String propertyName) {
            return switch (propertyName) {
                case "closed" -> closed || socket.isClosed();
                case "remoteIp" -> socket.getInetAddress().getHostAddress();
                case "remotePort" -> (long) socket.getPort();
                case "localPort" -> (long) socket.getLocalPort();
                case "write", "writeLine", "writeBytes", "read", "readAll", "readBytes", "close" -> new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                        return invokeMethod(propertyName, Interpreter.unpackNativeArgs(intp, args), intp);
                    }
                };
                default -> null;
            };
        }

        private void close() throws IOException {
            if (!closed) {
                socket.close();
                closed = true;
            }
        }
    }

    // =========================================================================
    // 🏢 SERVIDOR TCP (Aceita conexões em Background)
    // =========================================================================
    private static class TcpServerWrapper implements XplNativeObject {
        private final ServerSocket serverSocket;
        private final XplFunction handler;
        private final Interpreter parentInterpreter;
        private boolean closed = false;

        public TcpServerWrapper(ServerSocket serverSocket, XplFunction handler, Interpreter interpreter) {
            this.serverSocket = serverSocket;
            this.handler = handler;
            this.parentInterpreter = interpreter;
        }

        public void start() {
            Thread serverThread = new Thread(() -> {
                while (!serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        TcpClientWrapper clientWrapper = new TcpClientWrapper(clientSocket);

                        // Garante que a execução do XPL ocorre num interpretador isolado
                        Interpreter clientInterpreter = parentInterpreter.fork();

                        List<Expr.CallArg> args = List.of(
                                new Expr.CallArg(null, new Expr.Literal(clientWrapper))
                        );

                        // Invoca a função do lado do XPL!
                        handler.call(clientInterpreter, args);

                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) {
                            System.err.println("[Socket TCP Server] Erro a aceitar cliente: " + e.getMessage());
                        }
                    } catch (Exception xplError) {
                        System.err.println("[Socket TCP Server] Erro de XPL na thread do cliente: " + xplError.getMessage());
                    }
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();
        }

        @Override public void invokeMethod() {}

        @Override
        public Object invokeMethod(String methodName, List<Object> args, Interpreter interpreter) {
            if (methodName.equals("close")) {
                try {
                    serverSocket.close();
                    closed = true;
                    return true;
                } catch (IOException e) {
                    throw new ControlFlow.RuntimeError(null, "Erro ao fechar Servidor TCP: " + e.getMessage());
                }
            }
            throw new ControlFlow.RuntimeError(null, "Método não suportado.");
        }

        @Override
        public Object getProperty(String propertyName) {
            if (propertyName.equals("port")) return (long) serverSocket.getLocalPort();
            if (propertyName.equals("closed")) return closed;
            if (propertyName.equals("close")) return new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    return invokeMethod("close", Collections.emptyList(), intp);
                }
            };
            return null;
        }
    }

    // =========================================================================
    // 🛸 SOCKET UDP (Datagramas Rápidos)
    // =========================================================================
    private static class UdpSocketWrapper implements XplNativeObject {
        private final DatagramSocket socket;
        private boolean closed = false;

        public UdpSocketWrapper(DatagramSocket socket) {
            this.socket = socket;
        }

        @Override public void invokeMethod() {}

        @Override
        public Object invokeMethod(String methodName, List<Object> args, Interpreter interpreter) {
            if (closed) throw new ControlFlow.RuntimeError(null, "Socket UDP já fechado.");

            try {
                return switch (methodName) {
                    case "send" -> {
                        String host = (String) args.get(0);
                        int port = ((Number) args.get(1)).intValue();
                        String data = (String) args.get(2);

                        byte[] buffer = data.getBytes(StandardCharsets.UTF_8);
                        InetAddress address = InetAddress.getByName(host);
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
                        socket.send(packet);
                        yield true;
                    }
                    case "receive" -> {
                        int bufferSize = args.isEmpty() ? 1024 : ((Number) args.get(0)).intValue();
                        byte[] buffer = new byte[bufferSize];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);

                        // Constrói a resposta como Dicionário
                        Map<String, Object> result = new LinkedHashMap<>();
                        result.put("ip", packet.getAddress().getHostAddress());
                        result.put("port", (long) packet.getPort());
                        result.put("data", new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8));
                        yield result;
                    }
                    case "close" -> {
                        socket.close();
                        closed = true;
                        yield null;
                    }
                    default -> throw new ControlFlow.RuntimeError(null, "Método UDP não suportado: " + methodName);
                };
            } catch (IOException e) {
                throw new ControlFlow.RuntimeError(null, "Erro de I/O no Socket UDP: " + e.getMessage());
            }
        }

        @Override
        public Object getProperty(String propertyName) {
            return switch (propertyName) {
                case "localPort" -> (long) socket.getLocalPort();
                case "closed" -> closed;
                case "send", "receive", "close" -> new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                        return invokeMethod(propertyName, Interpreter.unpackNativeArgs(intp, args), intp);
                    }
                };
                default -> null;
            };
        }
    }

    // ─── HELPERS GENÉRICOS ──────────────────────────────────────────────

    @FunctionalInterface
    private interface NativeAction {
        Object execute(Interpreter interpreter, List<Expr.CallArg> args);
    }

    private static XplCallable buildAction(int arity, NativeAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return action.execute(intp, args);
            }
        };
    }

    private static String getString(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        return interpreter.stringify(val);
    }

    private static long getLong(Interpreter interpreter, List<Expr.CallArg> args, int index, long fallback) {
        if (index >= args.size()) return fallback;
        Object val = interpreter.evaluate(args.get(index).expression);
        if (val instanceof Number) return ((Number) val).longValue();
        return fallback;
    }
}