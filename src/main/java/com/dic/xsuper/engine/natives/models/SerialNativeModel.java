package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.execution.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;
import com.dic.xsuper.engine.poo.XplInstance;
import com.fazecast.jSerialComm.SerialPort;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SerialNativeModel {

    // Armazena handles abertos para evitar múltiplas aberturas da mesma porta
    private static final Map<String, SerialPort> openPorts = new ConcurrentHashMap<>();

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Serial", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        // ─── Serial.list() ──────────────────────────────────────────────────
        model.staticFields.put("list", buildAction(0, (intp, args) -> {
            List<Map<String, Object>> result = new ArrayList<>();
            SerialPort[] ports = SerialPort.getCommPorts();
            for (SerialPort sp : ports) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("systemPort", sp.getSystemPortName());
                info.put("descriptiveName", sp.getDescriptivePortName());
                info.put("path", sp.getSystemPortPath());
                info.put("description", sp.getPortDescription());
                info.put("location", sp.getPortLocation());
                info.put("vendorID", sp.getVendorID());
                info.put("productID", sp.getProductID());
                info.put("serialNumber", sp.getSerialNumber());
                info.put("manufacturer", sp.getManufacturer());
                result.add(info);
            }
            return result;
        }));

        // ─── Serial.open(path, baudRate, dataBits, stopBits, parity, readTimeout, writeTimeout) ──
        model.staticFields.put("open", buildAction(7, (intp, args) -> {
            String path = getString(intp, args, 0);
            int baudRate = (int) getLong(intp, args, 1, 9600);
            int dataBits = (int) getLong(intp, args, 2, 8);
            int stopBits = (int) getLong(intp, args, 3, 1);
            int parity = (int) getLong(intp, args, 4, 0);
            int readTimeout = (int) getLong(intp, args, 5, 1000);
            int writeTimeout = (int) getLong(intp, args, 6, 1000);

            if (openPorts.containsKey(path)) {
                throw new ControlFlow.RuntimeError(null, "Porta já está aberta: " + path);
            }

            SerialPort sp = SerialPort.getCommPort(path);
            if (sp == null) {
                throw new ControlFlow.RuntimeError(null, "Porta série não encontrada: " + path);
            }

            sp.setBaudRate(baudRate);
            sp.setNumDataBits(dataBits);
            sp.setNumStopBits(stopBits);
            sp.setParity(parity);
            sp.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_SEMI_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
                    readTimeout, writeTimeout
            );

            if (!sp.openPort()) {
                throw new ControlFlow.RuntimeError(null, "Falha ao abrir a porta série: " + path);
            }

            openPorts.put(path, sp);

            // Criar objeto XplInstance como handle
            XplInstance handle = new XplInstance(null);
            handle.fields.put("_port", sp);
            handle.fields.put("_path", path);

            // ─── Métodos do handle ──────────────────────────────────────
            handle.fields.put("read", new XplCallable() {
                @Override public int arity() { return 1; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    int bytesToRead = (int) getLong(intp, args, 0, 1024);
                    if (bytesToRead <= 0) bytesToRead = 1024;
                    byte[] buffer = new byte[bytesToRead];
                    int numRead = sp.readBytes(buffer, bytesToRead);
                    if (numRead <= 0) return "";
                    return new String(buffer, 0, numRead, StandardCharsets.UTF_8);
                }
            });

            handle.fields.put("readBytes", new XplCallable() {
                @Override public int arity() { return 1; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    int bytesToRead = (int) getLong(intp, args, 0, 1024);
                    if (bytesToRead <= 0) bytesToRead = 1024;
                    byte[] buffer = new byte[bytesToRead];
                    int numRead = sp.readBytes(buffer, bytesToRead);
                    if (numRead <= 0) return "";
                    return Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, numRead));
                }
            });

            handle.fields.put("write", new XplCallable() {
                @Override public int arity() { return 1; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    String data = getString(intp, args, 0);
                    byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
                    int written = sp.writeBytes(bytes, bytes.length);
                    if (written < 0) {
                        throw new ControlFlow.RuntimeError(null, "Erro ao escrever na porta série.");
                    }
                    return (long) written;
                }
            });

            handle.fields.put("writeBytes", new XplCallable() {
                @Override public int arity() { return 1; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    String base64 = getString(intp, args, 0);
                    byte[] bytes = Base64.getDecoder().decode(base64);
                    int written = sp.writeBytes(bytes, bytes.length);
                    if (written < 0) {
                        throw new ControlFlow.RuntimeError(null, "Erro ao escrever bytes na porta série.");
                    }
                    return (long) written;
                }
            });

            handle.fields.put("flush", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    sp.flushIOBuffers();
                    return null;
                }
            });

            handle.fields.put("close", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    doClose(sp, path);
                    return null;
                }
            });

            handle.fields.put("setParams", new XplCallable() {
                @Override public int arity() { return 4; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    int newBaud = (int) getLong(intp, args, 0, 9600);
                    int newData = (int) getLong(intp, args, 1, 8);
                    int newStop = (int) getLong(intp, args, 2, 1);
                    int newParity = (int) getLong(intp, args, 3, 0);
                    boolean ok = sp.setComPortParameters(newBaud, newData, newStop, newParity);
                    if (!ok) {
                        throw new ControlFlow.RuntimeError(null, "Falha ao reconfigurar a porta série.");
                    }
                    return null;
                }
            });

            // ─── Propriedades do handle ────────────────────────────────
            handle.fields.put("baudRate", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    return (long) sp.getBaudRate();
                }
            });
            handle.fields.put("dataBits", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    return (long) sp.getNumDataBits();
                }
            });
            handle.fields.put("stopBits", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    return (long) sp.getNumStopBits();
                }
            });
            handle.fields.put("parity", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    return (long) sp.getParity();
                }
            });
            handle.fields.put("isOpen", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    return sp.isOpen();
                }
            });
            handle.fields.put("bytesAvailable", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    return (long) sp.bytesAvailable();
                }
            });
            handle.fields.put("path", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    return sp.getSystemPortPath();
                }
            });

            return handle;
        }));

        // ─── Serial.close(handle) ──────────────────────────────────────
        model.staticFields.put("close", buildAction(1, (intp, args) -> {
            Object handle = intp.evaluate(args.getFirst().expression);
            if (!(handle instanceof XplInstance inst)) {
                throw new ControlFlow.RuntimeError(null, "Serial.close: argumento deve ser um handle retornado por Serial.open");
            }
            SerialPort sp = (SerialPort) inst.fields.get("_port");
            String path = (String) inst.fields.get("_path");
            if (sp == null) {
                throw new ControlFlow.RuntimeError(null, "Handle inválido ou já fechado.");
            }
            doClose(sp, path);
            return null;
        }));

        // ─── Serial.read(handle, bytes) ──────────────────────────────────
        model.staticFields.put("read", buildAction(2, (intp, args) -> {
            Object handle = intp.evaluate(args.getFirst().expression);
            int bytesToRead = (int) getLong(intp, args, 1, 1024);
            if (!(handle instanceof XplInstance inst)) {
                throw new ControlFlow.RuntimeError(null, "Serial.read: primeiro argumento deve ser um handle");
            }
            SerialPort sp = (SerialPort) inst.fields.get("_port");
            if (sp == null || !sp.isOpen()) {
                throw new ControlFlow.RuntimeError(null, "Porta série não está aberta.");
            }
            if (bytesToRead <= 0) bytesToRead = 1024;
            byte[] buffer = new byte[bytesToRead];
            int numRead = sp.readBytes(buffer, bytesToRead);
            if (numRead <= 0) return "";
            return new String(buffer, 0, numRead, StandardCharsets.UTF_8);
        }));

        // ─── Serial.write(handle, data) ──────────────────────────────────
        model.staticFields.put("write", buildAction(2, (intp, args) -> {
            Object handle = intp.evaluate(args.getFirst().expression);
            String data = getString(intp, args, 1);
            if (!(handle instanceof XplInstance inst)) {
                throw new ControlFlow.RuntimeError(null, "Serial.write: primeiro argumento deve ser um handle");
            }
            SerialPort sp = (SerialPort) inst.fields.get("_port");
            if (sp == null || !sp.isOpen()) {
                throw new ControlFlow.RuntimeError(null, "Porta série não está aberta.");
            }
            byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
            int written = sp.writeBytes(bytes, bytes.length);
            if (written < 0) {
                throw new ControlFlow.RuntimeError(null, "Erro ao escrever na porta série.");
            }
            return (long) written;
        }));

        // ─── Serial.readBytes(handle, bytes) ────────────────────────────
        model.staticFields.put("readBytes", buildAction(2, (intp, args) -> {
            Object handle = intp.evaluate(args.getFirst().expression);
            int bytesToRead = (int) getLong(intp, args, 1, 1024);
            if (!(handle instanceof XplInstance inst)) {
                throw new ControlFlow.RuntimeError(null, "Serial.readBytes: primeiro argumento deve ser um handle");
            }
            SerialPort sp = (SerialPort) inst.fields.get("_port");
            if (sp == null || !sp.isOpen()) {
                throw new ControlFlow.RuntimeError(null, "Porta série não está aberta.");
            }
            if (bytesToRead <= 0) bytesToRead = 1024;
            byte[] buffer = new byte[bytesToRead];
            int numRead = sp.readBytes(buffer, bytesToRead);
            if (numRead <= 0) return "";
            return Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, numRead));
        }));

        // ─── Serial.writeBytes(handle, base64) ──────────────────────────
        model.staticFields.put("writeBytes", buildAction(2, (intp, args) -> {
            Object handle = intp.evaluate(args.getFirst().expression);
            String base64 = getString(intp, args, 1);
            if (!(handle instanceof XplInstance inst)) {
                throw new ControlFlow.RuntimeError(null, "Serial.writeBytes: primeiro argumento deve ser um handle");
            }
            SerialPort sp = (SerialPort) inst.fields.get("_port");
            if (sp == null || !sp.isOpen()) {
                throw new ControlFlow.RuntimeError(null, "Porta série não está aberta.");
            }
            byte[] bytes = Base64.getDecoder().decode(base64);
            int written = sp.writeBytes(bytes, bytes.length);
            if (written < 0) {
                throw new ControlFlow.RuntimeError(null, "Erro ao escrever bytes na porta série.");
            }
            return (long) written;
        }));

        // ─── Serial.flush(handle) ────────────────────────────────────────
        model.staticFields.put("flush", buildAction(1, (intp, args) -> {
            Object handle = intp.evaluate(args.getFirst().expression);
            if (!(handle instanceof XplInstance inst)) {
                throw new ControlFlow.RuntimeError(null, "Serial.flush: argumento deve ser um handle");
            }
            SerialPort sp = (SerialPort) inst.fields.get("_port");
            if (sp == null || !sp.isOpen()) {
                throw new ControlFlow.RuntimeError(null, "Porta série não está aberta.");
            }
            sp.flushIOBuffers();
            return null;
        }));

        // ─── Serial.setParams(handle, baud, data, stop, parity) ─────────
        model.staticFields.put("setParams", buildAction(5, (intp, args) -> {
            Object handle = intp.evaluate(args.getFirst().expression);
            int newBaud = (int) getLong(intp, args, 1, 9600);
            int newData = (int) getLong(intp, args, 2, 8);
            int newStop = (int) getLong(intp, args, 3, 1);
            int newParity = (int) getLong(intp, args, 4, 0);
            if (!(handle instanceof XplInstance inst)) {
                throw new ControlFlow.RuntimeError(null, "Serial.setParams: primeiro argumento deve ser um handle");
            }
            SerialPort sp = (SerialPort) inst.fields.get("_port");
            if (sp == null || !sp.isOpen()) {
                throw new ControlFlow.RuntimeError(null, "Porta série não está aberta.");
            }
            boolean ok = sp.setComPortParameters(newBaud, newData, newStop, newParity);
            if (!ok) {
                throw new ControlFlow.RuntimeError(null, "Falha ao reconfigurar a porta série.");
            }
            return null;
        }));

        // ─── REGISTAR ──────────────────────────────────────────────────────

        interpreter.registry_model.put("Serial", model);
        interpreter.environment.defineConst("Serial", new XplClass(model, interpreter.globals));
    }

    // ─── Helper para fechar porta e remover do mapa ──────────────────────
    private static void doClose(SerialPort sp, String path) {
        if (sp != null && sp.isOpen()) {
            sp.closePort();
        }
        openPorts.remove(path);
    }

    // ─── Extratores de argumentos ──────────────────────────────────────────

    private static String getString(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        return interpreter.stringify(val);
    }

    private static long getLong(Interpreter interpreter, List<Expr.CallArg> args, int index, long fallback) {
        if (index >= args.size()) return fallback;
        Object val = interpreter.evaluate(args.get(index).expression);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return fallback;
    }

    // ─── Builders ──────────────────────────────────────────────────────────

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
}