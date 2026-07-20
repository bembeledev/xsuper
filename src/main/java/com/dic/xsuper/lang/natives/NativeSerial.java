package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.ControlFlow;
import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;
import com.dic.xsuper.lang.poo.XplInstance;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Módulo nativo para comunicação via porta série (RS-232, USB-serial).
 * Baseado na biblioteca jSerialComm.
 * <p>
 * Uso no XPL:
 *   var ports = serial_list()
 *   var handle = serial_open("/dev/ttyUSB0", 9600, 8, 1, 0)
 *   serial_write(handle, "AT\r\n")
 *   var resposta = serial_read(handle, 100)
 *   serial_close(handle)
 */
public class NativeSerial {

    // Armazena handles abertos para evitar múltiplas aberturas da mesma porta
    private static final Map<String, SerialPort> openPorts = new ConcurrentHashMap<>();

    public static void register(Interpreter interpreter) {
        // ─── serial_list() ─────────────────────────────────────────────────
        interpreter.globals.defineConst("serial_list", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
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
            }
        });

        // ─── serial_open(path, baudRate, dataBits, stopBits, parity, readTimeout, writeTimeout) ──
        interpreter.globals.defineConst("serial_open", new XplCallable() {
            @Override public int arity() { return 7; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                String path = getString(intp, args, 0);
                int baudRate = (int) getLong(intp, args, 1, 9600);
                int dataBits = (int) getLong(intp, args, 2, 8);
                int stopBits = (int) getLong(intp, args, 3, 1);
                int parity = (int) getLong(intp, args, 4, 0);
                int readTimeout = (int) getLong(intp, args, 5, 1000);
                int writeTimeout = (int) getLong(intp, args, 6, 1000);

                // Verifica se a porta já está aberta
                if (openPorts.containsKey(path)) {
                    throw new ControlFlow.RuntimeError(null,
                            "Porta já está aberta: " + path);
                }

                SerialPort sp = SerialPort.getCommPort(path);
                if (sp == null) {
                    throw new ControlFlow.RuntimeError(null,
                            "Porta série não encontrada: " + path);
                }

                // Configuração
                sp.setBaudRate(baudRate);
                sp.setNumDataBits(dataBits);
                sp.setNumStopBits(stopBits);
                sp.setParity(parity);
                sp.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING |
                                SerialPort.TIMEOUT_WRITE_BLOCKING,
                        readTimeout, writeTimeout);

                // Abrir
                if (!sp.openPort()) {
                    throw new ControlFlow.RuntimeError(null,
                            "Falha ao abrir a porta série: " + path);
                }

                openPorts.put(path, sp);

                // Criar objeto XplInstance como handle
                XplInstance handle = new XplInstance(null);
                handle.fields.put("_port", sp);
                handle.fields.put("_path", path);

                // ─── Métodos do handle ────────────────────────────────
                handle.fields.put("read", new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                        int bytesToRead = (int) getLong(intp, args, 0, 1024);
                        if (bytesToRead <= 0) bytesToRead = 1024;
                        byte[] buffer = new byte[bytesToRead];
                        int numRead = sp.readBytes(buffer, bytesToRead);
                        if (numRead <= 0) return "";
                        // Converte para string UTF-8 (assumindo texto)
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
                        // Devolve em Base64 para binário
                        return Base64.getEncoder().encodeToString(
                                Arrays.copyOf(buffer, numRead)
                        );
                    }
                });

                handle.fields.put("write", new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                        String data = getString(intp, args, 0);
                        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
                        int written = sp.writeBytes(bytes, bytes.length);
                        if (written < 0) {
                            throw new ControlFlow.RuntimeError(null,
                                    "Erro ao escrever na porta série.");
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
                            throw new ControlFlow.RuntimeError(null,
                                    "Erro ao escrever bytes na porta série.");
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
                            throw new ControlFlow.RuntimeError(null,
                                    "Falha ao reconfigurar a porta série.");
                        }
                        return null;
                    }
                });

                // ─── Propriedades como campos (getters) ──────────────
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
            }
        });

        // ─── serial_close(handle) ────────────────────────────────────────
        interpreter.globals.defineConst("serial_close", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                Object handle = intp.evaluate(args.get(0).expression);
                if (!(handle instanceof XplInstance)) {
                    throw new ControlFlow.RuntimeError(null,
                            "serial_close: argumento deve ser um handle retornado por serial_open");
                }
                XplInstance inst = (XplInstance) handle;
                SerialPort sp = (SerialPort) inst.fields.get("_port");
                String path = (String) inst.fields.get("_path");
                if (sp == null) {
                    throw new ControlFlow.RuntimeError(null,
                            "Handle inválido ou já fechado.");
                }
                doClose(sp, path);
                return null;
            }
        });

        // ─── serial_read(handle, bytes) ──────────────────────────────────
        interpreter.globals.defineConst("serial_read", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                Object handle = intp.evaluate(args.get(0).expression);
                int bytesToRead = (int) getLong(intp, args, 1, 1024);
                if (!(handle instanceof XplInstance)) {
                    throw new ControlFlow.RuntimeError(null,
                            "serial_read: primeiro argumento deve ser um handle");
                }
                XplInstance inst = (XplInstance) handle;
                SerialPort sp = (SerialPort) inst.fields.get("_port");
                if (sp == null || !sp.isOpen()) {
                    throw new ControlFlow.RuntimeError(null,
                            "Porta série não está aberta.");
                }
                if (bytesToRead <= 0) bytesToRead = 1024;
                byte[] buffer = new byte[bytesToRead];
                int numRead = sp.readBytes(buffer, bytesToRead);
                if (numRead <= 0) return "";
                return new String(buffer, 0, numRead, StandardCharsets.UTF_8);
            }
        });

        // ─── serial_write(handle, data) ──────────────────────────────────
        interpreter.globals.defineConst("serial_write", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                Object handle = intp.evaluate(args.get(0).expression);
                String data = getString(intp, args, 1);
                if (!(handle instanceof XplInstance)) {
                    throw new ControlFlow.RuntimeError(null,
                            "serial_write: primeiro argumento deve ser um handle");
                }
                XplInstance inst = (XplInstance) handle;
                SerialPort sp = (SerialPort) inst.fields.get("_port");
                if (sp == null || !sp.isOpen()) {
                    throw new ControlFlow.RuntimeError(null,
                            "Porta série não está aberta.");
                }
                byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
                int written = sp.writeBytes(bytes, bytes.length);
                if (written < 0) {
                    throw new ControlFlow.RuntimeError(null,
                            "Erro ao escrever na porta série.");
                }
                return (long) written;
            }
        });
    }

    // ─── Helper para fechar porta e remover do mapa ──────────────────────
    private static void doClose(SerialPort sp, String path) {
        if (sp != null && sp.isOpen()) {
            sp.closePort();
        }
        openPorts.remove(path);
    }

    // ─── Extratores de argumentos (conforme padrão dos seus módulos) ──
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
}