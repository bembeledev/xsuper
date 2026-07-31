package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.exceptions.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;

import java.net.*;
import java.util.*;
import java.io.IOException;

public class NetworkNativeModel {

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Network", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        // ─── Network.ping(host, [timeout]) ──────────────────────────────
        model.staticFields.put("ping", buildAction(-1, (intp, args) -> {
            if (args.isEmpty()) {
                throw new ControlFlow.RuntimeError(null, "ping precisa de pelo menos o host.");
            }
            String host = getString(intp, args, 0);
            int timeout = args.size() > 1 ? (int) getLong(intp, args, 1, 3000) : 3000;
            try {
                return InetAddress.getByName(host).isReachable(timeout);
            } catch (Exception e) {
                return false;
            }
        }));

        // ─── Network.dnsLookup(host) ─────────────────────────────────────
        model.staticFields.put("dnsLookup", buildAction(1, (intp, args) -> {
            String host = getString(intp, args, 0);
            try {
                return InetAddress.getByName(host).getHostAddress();
            } catch (Exception e) {
                return "Host Desconhecido";
            }
        }));

        // ─── Network.reverseLookup(ip) ───────────────────────────────────
        model.staticFields.put("reverseLookup", buildAction(1, (intp, args) -> {
            String ip = getString(intp, args, 0);
            try {
                InetAddress addr = InetAddress.getByName(ip);
                return addr.getCanonicalHostName();
            } catch (Exception e) {
                return "Nome não encontrado";
            }
        }));

        // ─── Network.localIp() ────────────────────────────────────────────
        model.staticFields.put("localIp", buildAction(0, (intp, args) -> {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                return "127.0.0.1";
            }
        }));

        // ─── Network.localHostname() ──────────────────────────────────────
        model.staticFields.put("localHostname", buildAction(0, (intp, args) -> {
            try {
                return InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                return "localhost";
            }
        }));

        // ─── Network.allIps() ─────────────────────────────────────────────
        model.staticFields.put("allIps", buildAction(0, (intp, args) -> {
            List<String> ips = new ArrayList<>();
            try {
                Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
                while (nets.hasMoreElements()) {
                    NetworkInterface ni = nets.nextElement();
                    Enumeration<InetAddress> addrs = ni.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        InetAddress addr = addrs.nextElement();
                        ips.add(addr.getHostAddress());
                    }
                }
            } catch (SocketException e) {
                throw new ControlFlow.RuntimeError(null, "Erro ao obter IPs: " + e.getMessage());
            }
            return ips;
        }));

        // ─── Network.portOpen(host, port, [timeout]) ─────────────────────
        model.staticFields.put("portOpen", buildAction(-1, (intp, args) -> {
            if (args.size() < 2) {
                throw new ControlFlow.RuntimeError(null, "portOpen precisa de host e porta.");
            }
            String host = getString(intp, args, 0);
            int port = (int) getLong(intp, args, 1, 0);
            int timeout = args.size() > 2 ? (int) getLong(intp, args, 2, 2000) : 2000;

            if (port < 1 || port > 65535) {
                throw new ControlFlow.RuntimeError(null, "Porta inválida: " + port);
            }

            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), timeout);
                return true;
            } catch (IOException e) {
                return false;
            }
        }));

        // ─── Network.interfaces() ─────────────────────────────────────────
        model.staticFields.put("interfaces", buildAction(0, (intp, args) -> {
            List<Map<String, Object>> result = new ArrayList<>();
            try {
                Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
                while (nets.hasMoreElements()) {
                    NetworkInterface ni = nets.nextElement();
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("name", ni.getName());
                    info.put("displayName", ni.getDisplayName());
                    info.put("mtu", (long) ni.getMTU());
                    info.put("loopback", ni.isLoopback());
                    info.put("up", ni.isUp());
                    info.put("virtual", ni.isVirtual());
                    info.put("mac", getMacAddress(ni));

                    List<String> ips = new ArrayList<>();
                    Enumeration<InetAddress> addrs = ni.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        InetAddress addr = addrs.nextElement();
                        ips.add(addr.getHostAddress());
                    }
                    info.put("ips", ips);
                    result.add(info);
                }
            } catch (SocketException e) {
                throw new ControlFlow.RuntimeError(null, "Erro ao obter interfaces: " + e.getMessage());
            }
            return result;
        }));

        // ─── REGISTAR ──────────────────────────────────────────────────────

        interpreter.registry_model.put("Network", model);
        interpreter.environment.defineConst("Network", new XplClass(model, interpreter.globals));
    }

    // ─── Auxiliar ──────────────────────────────────────────────────────────

    private static String getMacAddress(NetworkInterface ni) {
        try {
            byte[] mac = ni.getHardwareAddress();
            if (mac == null) return null;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X", mac[i]));
                if (i < mac.length - 1) sb.append(":");
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
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