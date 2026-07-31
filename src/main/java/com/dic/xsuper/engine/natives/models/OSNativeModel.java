package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.exceptions.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;

import java.io.*;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class OSNativeModel {

    // ─── Provider singleton ──────────────────────────────────────────────
    private static final OSProvider provider = OSProvider.detect();

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("OS", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        // ─── 1. INFORMAÇÕES DO SISTEMA ─────────────────────────────────
        model.staticFields.put("name", buildAction(0, (i, a) -> provider.getOsName()));
        model.staticFields.put("version", buildAction(0, (i, a) -> provider.getOsVersion()));
        model.staticFields.put("arch", buildAction(0, (i, a) -> provider.getArch()));
        model.staticFields.put("hostname", buildAction(0, (i, a) -> provider.getHostname()));
        model.staticFields.put("username", buildAction(0, (i, a) -> provider.getUsername()));

        // ─── 2. HARDWARE ────────────────────────────────────────────────
        model.staticFields.put("cpus", buildAction(0, (i, a) -> provider.getCpuCount()));
        model.staticFields.put("totalMemory", buildAction(0, (i, a) -> provider.getTotalMemory()));
        model.staticFields.put("freeMemory", buildAction(0, (i, a) -> provider.getFreeMemory()));
        model.staticFields.put("diskInfo", buildAction(0, (i, a) -> provider.getDiskInfo()));
        model.staticFields.put("networkInterfaces", buildAction(0, (i, a) -> provider.getNetworkInterfaces()));

        // ─── 3. VARIÁVEIS DE AMBIENTE E PATHS ──────────────────────────
        model.staticFields.put("env", buildAction(1, (intp, args) -> {
            String varName = intp.stringify(intp.evaluate(args.getFirst().expression));
            return provider.getEnv(varName);
        }));

        // OS.envDefault(varName, fallback)
        model.staticFields.put("envDefault", buildAction(2, (intp, args) -> {
            String varName = intp.stringify(intp.evaluate(args.get(0).expression));
            String fallback = intp.stringify(intp.evaluate(args.get(1).expression));
            String value = provider.getEnv(varName);
            return value != null ? value : fallback;
        }));

        model.staticFields.put("envAll", buildAction(0, (i, a) -> provider.getEnvAll()));

        model.staticFields.put("userHome", buildAction(0, (i, a) -> provider.getUserHome()));
        model.staticFields.put("userDir", buildAction(0, (i, a) -> provider.getUserDir()));
        model.staticFields.put("tmpDir", buildAction(0, (i, a) -> provider.getTmpDir()));

        // ─── 4. PROCESSOS E UTILITÁRIOS ────────────────────────────────
        model.staticFields.put("pid", buildAction(0, (i, a) -> provider.getPid()));

        model.staticFields.put("sleep", buildAction(1, (intp, args) -> {
            long ms = ((Number) intp.evaluate(args.getFirst().expression)).longValue();
            if (ms < 0) throw new ControlFlow.RuntimeError(null, "OS.sleep: o tempo não pode ser negativo.");
            try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return null;
        }));

        model.staticFields.put("exit", buildAction(1, (intp, args) -> {
            int code = ((Number) intp.evaluate(args.getFirst().expression)).intValue();
            System.exit(code);
            return null;
        }));

        model.staticFields.put("exec", buildAction(1, (intp, args) -> {
            Object val = intp.evaluate(args.getFirst().expression);
            if (!(val instanceof Map)) {
                throw new ControlFlow.RuntimeError(null, "OS.exec: esperado um dicionário (Map) com opções.");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> opts = (Map<String, Object>) val;
            return provider.exec(opts);
        }));

        // ─── 5. CONSTANTES DE PLATAFORMA ──────────────────────────────
        model.staticFields.put("IS_WINDOWS", isWindows());
        model.staticFields.put("IS_LINUX", isLinux());
        model.staticFields.put("IS_MAC", isMac());
        model.staticFields.put("IS_ANDROID", isAndroid());

        model.staticFields.put("OS_FAMILY", buildAction(0, (i, a) -> provider.getOsFamily()));

        // ─── 6. PROPRIEDADES DO SISTEMA JAVA ──────────────────────────
        model.staticFields.put("javaVersion", buildAction(0, (i, a) -> System.getProperty("java.version")));
        model.staticFields.put("javaVendor", buildAction(0, (i, a) -> System.getProperty("java.vendor")));
        model.staticFields.put("javaHome", buildAction(0, (i, a) -> System.getProperty("java.home")));

        // ─── 7. VERIFICAÇÃO DE PRIVILÉGIOS ────────────────────────────
        model.staticFields.put("isAdmin", buildAction(0, (i, a) -> provider.isAdmin()));

        // ─── Registar a classe OS ──────────────────────────────────────
        interpreter.registry_model.put("OS", model);
        interpreter.environment.defineConst("OS", new XplClass(model, interpreter.globals));
    }

    // ─── Métodos auxiliares para deteção do SO ───────────────────────────
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }

    private static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }

    private static boolean isAndroid() {
        return System.getProperty("java.vendor", "").toLowerCase().contains("android");
    }

    // ─── Helpers internos ──────────────────────────────────────────────────
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

    // ─── CLASSE ABSTRATA: OSProvider ──────────────────────────────────────

    public abstract static class OSProvider {
        public static OSProvider detect() {
            if (isAndroid()) return new AndroidProvider();
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) return new WindowsProvider();
            if (os.contains("linux") || os.contains("nix") || os.contains("aix")) return new LinuxProvider();
            if (os.contains("mac") || os.contains("darwin")) return new MacProvider();
            if (os.contains("ios")) return new IOSProvider();
            return new UnknownProvider();
        }

        // Métodos obrigatórios
        public abstract String getOsName();
        public abstract String getOsVersion();
        public abstract String getArch();
        public abstract String getHostname();
        public abstract String getUsername();
        public abstract long getPid();
        public abstract long getCpuCount();
        public abstract long getTotalMemory(); // em MB
        public abstract long getFreeMemory();  // em MB
        public abstract List<Map<String, Object>> getDiskInfo();
        public abstract List<Map<String, Object>> getNetworkInterfaces();
        public abstract String getEnv(String name);
        public abstract Map<String, String> getEnvAll();
        public abstract String getUserHome();
        public abstract String getUserDir();
        public abstract String getTmpDir();
        public abstract String getPathSeparator();
        public abstract String getFileSeparator();
        public abstract Map<String, Object> exec(Map<String, Object> opts);
        public abstract String getOsFamily();
        public abstract boolean isAdmin();
    }

    // ─── PROVIDER: WINDOWS ────────────────────────────────────────────────

    public static class WindowsProvider extends OSProvider {
        @Override public String getOsName() { return System.getProperty("os.name"); }
        @Override public String getOsVersion() { return System.getProperty("os.version"); }
        @Override public String getArch() { return System.getProperty("os.arch"); }
        @Override public String getHostname() {
            try { return java.net.InetAddress.getLocalHost().getHostName(); } catch(Exception e) { return "localhost"; }
        }
        @Override public String getUsername() { return System.getProperty("user.name"); }
        @Override public long getPid() { return ProcessHandle.current().pid(); }
        @Override public long getCpuCount() { return Runtime.getRuntime().availableProcessors(); }
        @Override public long getTotalMemory() { return Runtime.getRuntime().totalMemory() / (1024 * 1024); }
        @Override public long getFreeMemory() { return Runtime.getRuntime().freeMemory() / (1024 * 1024); }

        @Override
        public List<Map<String, Object>> getDiskInfo() {
            List<Map<String, Object>> disks = new ArrayList<>();
            for (File root : File.listRoots()) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("path", root.getAbsolutePath());
                info.put("totalMB", root.getTotalSpace() / (1024 * 1024));
                info.put("freeMB", root.getFreeSpace() / (1024 * 1024));
                info.put("usedMB", (root.getTotalSpace() - root.getFreeSpace()) / (1024 * 1024));
                info.put("usagePercent", root.getTotalSpace() > 0 ?
                        (double) (root.getTotalSpace() - root.getFreeSpace()) / root.getTotalSpace() * 100 : 0);
                disks.add(info);
            }
            return disks;
        }

        @Override
        public List<Map<String, Object>> getNetworkInterfaces() {
            List<Map<String, Object>> interfaces = new ArrayList<>();
            try {
                Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
                while (nets.hasMoreElements()) {
                    NetworkInterface ni = nets.nextElement();
                    Map<String, Object> iface = new LinkedHashMap<>();
                    iface.put("name", ni.getName());
                    iface.put("displayName", ni.getDisplayName());
                    iface.put("mac", getMacAddress(ni));
                    iface.put("mtu", (long) ni.getMTU());
                    iface.put("loopback", ni.isLoopback());
                    iface.put("up", ni.isUp());
                    iface.put("virtual", ni.isVirtual());
                    List<String> ips = new ArrayList<>();
                    ni.getInetAddresses().asIterator().forEachRemaining(addr -> ips.add(addr.getHostAddress()));
                    iface.put("ips", ips);
                    interfaces.add(iface);
                }
            } catch (SocketException ignored) {}
            return interfaces;
        }

        @Override public String getEnv(String name) { return System.getenv(name); }
        @Override public Map<String, String> getEnvAll() { return new HashMap<>(System.getenv()); }
        @Override public String getUserHome() { return System.getProperty("user.home"); }
        @Override public String getUserDir() { return System.getProperty("user.dir"); }
        @Override public String getTmpDir() { return System.getProperty("java.io.tmpdir"); }
        @Override public String getPathSeparator() { return File.pathSeparator; }
        @Override public String getFileSeparator() { return File.separator; }
        @Override public String getOsFamily() { return "windows"; }
        @Override public boolean isAdmin() {
            try { return System.getenv("USERNAME").equals("Administrator") || System.getenv("USERNAME").equals("root"); }
            catch (Exception e) { return false; }
        }

        @Override
        public Map<String, Object> exec(Map<String, Object> opts) {
            return executeProcess(opts, "cmd.exe", "/c");
        }
    }

    // ─── PROVIDER: LINUX ──────────────────────────────────────────────────

    public static class LinuxProvider extends OSProvider {
        @Override public String getOsName() { return System.getProperty("os.name"); }
        @Override public String getOsVersion() { return System.getProperty("os.version"); }
        @Override public String getArch() { return System.getProperty("os.arch"); }
        @Override public String getHostname() {
            try { return java.net.InetAddress.getLocalHost().getHostName(); } catch(Exception e) { return "localhost"; }
        }
        @Override public String getUsername() { return System.getProperty("user.name"); }
        @Override public long getPid() { return ProcessHandle.current().pid(); }
        @Override public long getCpuCount() { return Runtime.getRuntime().availableProcessors(); }
        @Override public long getTotalMemory() { return Runtime.getRuntime().totalMemory() / (1024 * 1024); }
        @Override public long getFreeMemory() { return Runtime.getRuntime().freeMemory() / (1024 * 1024); }

        @Override
        public List<Map<String, Object>> getDiskInfo() {
            List<Map<String, Object>> disks = new ArrayList<>();
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"df", "-P"});
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    boolean header = true;
                    while ((line = reader.readLine()) != null) {
                        if (header) { header = false; continue; }
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 6) {
                            Map<String, Object> info = new LinkedHashMap<>();
                            info.put("filesystem", parts[0]);
                            info.put("totalMB", Long.parseLong(parts[1]) / 1024);
                            info.put("usedMB", Long.parseLong(parts[2]) / 1024);
                            info.put("freeMB", Long.parseLong(parts[3]) / 1024);
                            info.put("usagePercent", Double.parseDouble(parts[4].replace("%", "")));
                            info.put("path", parts[5]);
                            disks.add(info);
                        }
                    }
                }
                p.waitFor();
            } catch (Exception ignored) {}
            return disks;
        }

        @Override
        public List<Map<String, Object>> getNetworkInterfaces() {
            List<Map<String, Object>> interfaces = new ArrayList<>();
            try {
                Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
                while (nets.hasMoreElements()) {
                    NetworkInterface ni = nets.nextElement();
                    Map<String, Object> iface = new LinkedHashMap<>();
                    iface.put("name", ni.getName());
                    iface.put("displayName", ni.getDisplayName());
                    iface.put("mac", getMacAddress(ni));
                    iface.put("mtu", (long) ni.getMTU());
                    iface.put("loopback", ni.isLoopback());
                    iface.put("up", ni.isUp());
                    iface.put("virtual", ni.isVirtual());
                    List<String> ips = new ArrayList<>();
                    ni.getInetAddresses().asIterator().forEachRemaining(addr -> ips.add(addr.getHostAddress()));
                    iface.put("ips", ips);
                    interfaces.add(iface);
                }
            } catch (SocketException ignored) {}
            return interfaces;
        }

        @Override public String getEnv(String name) { return System.getenv(name); }
        @Override public Map<String, String> getEnvAll() { return new HashMap<>(System.getenv()); }
        @Override public String getUserHome() { return System.getProperty("user.home"); }
        @Override public String getUserDir() { return System.getProperty("user.dir"); }
        @Override public String getTmpDir() { return System.getProperty("java.io.tmpdir"); }
        @Override public String getPathSeparator() { return File.pathSeparator; }
        @Override public String getFileSeparator() { return File.separator; }
        @Override public String getOsFamily() { return "linux"; }
        @Override public boolean isAdmin() {
            try { return "root".equals(System.getenv("USER")) || "root".equals(System.getenv("LOGNAME")); }
            catch (Exception e) { return false; }
        }

        @Override
        public Map<String, Object> exec(Map<String, Object> opts) {
            return executeProcess(opts, "sh", "-c");
        }
    }

    // ─── PROVIDER: MAC (macOS / darwin) ──────────────────────────────────

    public static class MacProvider extends OSProvider {
        @Override public String getOsName() { return System.getProperty("os.name"); }
        @Override public String getOsVersion() { return System.getProperty("os.version"); }
        @Override public String getArch() { return System.getProperty("os.arch"); }
        @Override public String getHostname() {
            try { return java.net.InetAddress.getLocalHost().getHostName(); } catch(Exception e) { return "localhost"; }
        }
        @Override public String getUsername() { return System.getProperty("user.name"); }
        @Override public long getPid() { return ProcessHandle.current().pid(); }
        @Override public long getCpuCount() { return Runtime.getRuntime().availableProcessors(); }
        @Override public long getTotalMemory() { return Runtime.getRuntime().totalMemory() / (1024 * 1024); }
        @Override public long getFreeMemory() { return Runtime.getRuntime().freeMemory() / (1024 * 1024); }

        @Override
        public List<Map<String, Object>> getDiskInfo() {
            List<Map<String, Object>> disks = new ArrayList<>();
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"df", "-P"});
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    boolean header = true;
                    while ((line = reader.readLine()) != null) {
                        if (header) { header = false; continue; }
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 6) {
                            Map<String, Object> info = new LinkedHashMap<>();
                            info.put("filesystem", parts[0]);
                            info.put("totalMB", Long.parseLong(parts[1]) / 1024);
                            info.put("usedMB", Long.parseLong(parts[2]) / 1024);
                            info.put("freeMB", Long.parseLong(parts[3]) / 1024);
                            info.put("usagePercent", Double.parseDouble(parts[4].replace("%", "")));
                            info.put("path", parts[5]);
                            disks.add(info);
                        }
                    }
                }
                p.waitFor();
            } catch (Exception ignored) {}
            return disks;
        }

        @Override
        public List<Map<String, Object>> getNetworkInterfaces() {
            List<Map<String, Object>> interfaces = new ArrayList<>();
            try {
                Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
                while (nets.hasMoreElements()) {
                    NetworkInterface ni = nets.nextElement();
                    Map<String, Object> iface = new LinkedHashMap<>();
                    iface.put("name", ni.getName());
                    iface.put("displayName", ni.getDisplayName());
                    iface.put("mac", getMacAddress(ni));
                    iface.put("mtu", (long) ni.getMTU());
                    iface.put("loopback", ni.isLoopback());
                    iface.put("up", ni.isUp());
                    iface.put("virtual", ni.isVirtual());
                    List<String> ips = new ArrayList<>();
                    ni.getInetAddresses().asIterator().forEachRemaining(addr -> ips.add(addr.getHostAddress()));
                    iface.put("ips", ips);
                    interfaces.add(iface);
                }
            } catch (SocketException ignored) {}
            return interfaces;
        }

        @Override public String getEnv(String name) { return System.getenv(name); }
        @Override public Map<String, String> getEnvAll() { return new HashMap<>(System.getenv()); }
        @Override public String getUserHome() { return System.getProperty("user.home"); }
        @Override public String getUserDir() { return System.getProperty("user.dir"); }
        @Override public String getTmpDir() { return System.getProperty("java.io.tmpdir"); }
        @Override public String getPathSeparator() { return File.pathSeparator; }
        @Override public String getFileSeparator() { return File.separator; }
        @Override public String getOsFamily() { return "mac"; }
        @Override public boolean isAdmin() {
            try { return "root".equals(System.getenv("USER")) || "root".equals(System.getenv("LOGNAME")); }
            catch (Exception e) { return false; }
        }

        @Override
        public Map<String, Object> exec(Map<String, Object> opts) {
            return executeProcess(opts, "sh", "-c");
        }
    }

    // ─── PROVIDER: ANDROID ─────────────────────────────────────────────────

    public static class AndroidProvider extends OSProvider {
        @Override public String getOsName() { return "Android"; }
        @Override public String getOsVersion() { return System.getProperty("os.version"); }
        @Override public String getArch() { return System.getProperty("os.arch"); }
        @Override public String getHostname() {
            try { return java.net.InetAddress.getLocalHost().getHostName(); } catch(Exception e) { return "localhost"; }
        }
        @Override public String getUsername() { return System.getProperty("user.name"); }
        @Override public long getPid() { return ProcessHandle.current().pid(); }
        @Override public long getCpuCount() { return Runtime.getRuntime().availableProcessors(); }
        @Override public long getTotalMemory() { return Runtime.getRuntime().totalMemory() / (1024 * 1024); }
        @Override public long getFreeMemory() { return Runtime.getRuntime().freeMemory() / (1024 * 1024); }

        @Override
        public List<Map<String, Object>> getDiskInfo() {
            // Android: podemos tentar /proc/mounts, mas simplificamos
            return Collections.emptyList();
        }

        @Override
        public List<Map<String, Object>> getNetworkInterfaces() {
            List<Map<String, Object>> interfaces = new ArrayList<>();
            try {
                Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
                while (nets.hasMoreElements()) {
                    NetworkInterface ni = nets.nextElement();
                    Map<String, Object> iface = new LinkedHashMap<>();
                    iface.put("name", ni.getName());
                    iface.put("displayName", ni.getDisplayName());
                    iface.put("mac", getMacAddress(ni));
                    iface.put("mtu", (long) ni.getMTU());
                    iface.put("loopback", ni.isLoopback());
                    iface.put("up", ni.isUp());
                    iface.put("virtual", ni.isVirtual());
                    List<String> ips = new ArrayList<>();
                    ni.getInetAddresses().asIterator().forEachRemaining(addr -> ips.add(addr.getHostAddress()));
                    iface.put("ips", ips);
                    interfaces.add(iface);
                }
            } catch (SocketException ignored) {}
            return interfaces;
        }

        @Override public String getEnv(String name) { return System.getenv(name); }
        @Override public Map<String, String> getEnvAll() { return new HashMap<>(System.getenv()); }
        @Override public String getUserHome() { return System.getProperty("user.home"); }
        @Override public String getUserDir() { return System.getProperty("user.dir"); }
        @Override public String getTmpDir() { return System.getProperty("java.io.tmpdir"); }
        @Override public String getPathSeparator() { return File.pathSeparator; }
        @Override public String getFileSeparator() { return File.separator; }
        @Override public String getOsFamily() { return "android"; }
        @Override public boolean isAdmin() { return false; } // Android não tem conceito de root no app

        @Override
        public Map<String, Object> exec(Map<String, Object> opts) {
            return executeProcess(opts, "sh", "-c");
        }
    }

    // ─── PROVIDER: IOS (fallback) ─────────────────────────────────────────

    public static class IOSProvider extends OSProvider {
        @Override public String getOsName() { return "iOS"; }
        @Override public String getOsVersion() { return System.getProperty("os.version"); }
        @Override public String getArch() { return System.getProperty("os.arch"); }
        @Override public String getHostname() {
            try { return java.net.InetAddress.getLocalHost().getHostName(); } catch(Exception e) { return "localhost"; }
        }
        @Override public String getUsername() { return System.getProperty("user.name"); }
        @Override public long getPid() { return ProcessHandle.current().pid(); }
        @Override public long getCpuCount() { return Runtime.getRuntime().availableProcessors(); }
        @Override public long getTotalMemory() { return Runtime.getRuntime().totalMemory() / (1024 * 1024); }
        @Override public long getFreeMemory() { return Runtime.getRuntime().freeMemory() / (1024 * 1024); }

        @Override public List<Map<String, Object>> getDiskInfo() { return Collections.emptyList(); }

        @Override
        public List<Map<String, Object>> getNetworkInterfaces() {
            List<Map<String, Object>> interfaces = new ArrayList<>();
            try {
                Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
                while (nets.hasMoreElements()) {
                    NetworkInterface ni = nets.nextElement();
                    Map<String, Object> iface = new LinkedHashMap<>();
                    iface.put("name", ni.getName());
                    iface.put("displayName", ni.getDisplayName());
                    iface.put("mac", getMacAddress(ni));
                    iface.put("mtu", (long) ni.getMTU());
                    iface.put("loopback", ni.isLoopback());
                    iface.put("up", ni.isUp());
                    iface.put("virtual", ni.isVirtual());
                    List<String> ips = new ArrayList<>();
                    ni.getInetAddresses().asIterator().forEachRemaining(addr -> ips.add(addr.getHostAddress()));
                    iface.put("ips", ips);
                    interfaces.add(iface);
                }
            } catch (SocketException ignored) {}
            return interfaces;
        }

        @Override public String getEnv(String name) { return System.getenv(name); }
        @Override public Map<String, String> getEnvAll() { return new HashMap<>(System.getenv()); }
        @Override public String getUserHome() { return System.getProperty("user.home"); }
        @Override public String getUserDir() { return System.getProperty("user.dir"); }
        @Override public String getTmpDir() { return System.getProperty("java.io.tmpdir"); }
        @Override public String getPathSeparator() { return File.pathSeparator; }
        @Override public String getFileSeparator() { return File.separator; }
        @Override public String getOsFamily() { return "ios"; }
        @Override public boolean isAdmin() { return false; }

        @Override
        public Map<String, Object> exec(Map<String, Object> opts) {
            throw new ControlFlow.RuntimeError(null, "Execução de processos não suportada em iOS.");
        }
    }

    // ─── PROVIDER: UNKNOWN (fallback) ─────────────────────────────────────

    public static class UnknownProvider extends OSProvider {
        @Override public String getOsName() { return "Unknown"; }
        @Override public String getOsVersion() { return "Unknown"; }
        @Override public String getArch() { return "Unknown"; }
        @Override public String getHostname() {
            try { return java.net.InetAddress.getLocalHost().getHostName(); } catch(Exception e) { return "localhost"; }
        }
        @Override public String getUsername() { return System.getProperty("user.name"); }
        @Override public long getPid() { return ProcessHandle.current().pid(); }
        @Override public long getCpuCount() { return Runtime.getRuntime().availableProcessors(); }
        @Override public long getTotalMemory() { return Runtime.getRuntime().totalMemory() / (1024 * 1024); }
        @Override public long getFreeMemory() { return Runtime.getRuntime().freeMemory() / (1024 * 1024); }

        @Override
        public List<Map<String, Object>> getDiskInfo() {
            List<Map<String, Object>> disks = new ArrayList<>();
            for (File root : File.listRoots()) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("path", root.getAbsolutePath());
                info.put("totalMB", root.getTotalSpace() / (1024 * 1024));
                info.put("freeMB", root.getFreeSpace() / (1024 * 1024));
                info.put("usedMB", (root.getTotalSpace() - root.getFreeSpace()) / (1024 * 1024));
                info.put("usagePercent", root.getTotalSpace() > 0 ?
                        (double) (root.getTotalSpace() - root.getFreeSpace()) / root.getTotalSpace() * 100 : 0);
                disks.add(info);
            }
            return disks;
        }

        @Override
        public List<Map<String, Object>> getNetworkInterfaces() {
            List<Map<String, Object>> interfaces = new ArrayList<>();
            try {
                Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
                while (nets.hasMoreElements()) {
                    NetworkInterface ni = nets.nextElement();
                    Map<String, Object> iface = new LinkedHashMap<>();
                    iface.put("name", ni.getName());
                    iface.put("displayName", ni.getDisplayName());
                    iface.put("mac", getMacAddress(ni));
                    iface.put("mtu", (long) ni.getMTU());
                    iface.put("loopback", ni.isLoopback());
                    iface.put("up", ni.isUp());
                    iface.put("virtual", ni.isVirtual());
                    List<String> ips = new ArrayList<>();
                    ni.getInetAddresses().asIterator().forEachRemaining(addr -> ips.add(addr.getHostAddress()));
                    iface.put("ips", ips);
                    interfaces.add(iface);
                }
            } catch (SocketException ignored) {}
            return interfaces;
        }

        @Override public String getEnv(String name) { return System.getenv(name); }
        @Override public Map<String, String> getEnvAll() { return new HashMap<>(System.getenv()); }
        @Override public String getUserHome() { return System.getProperty("user.home"); }
        @Override public String getUserDir() { return System.getProperty("user.dir"); }
        @Override public String getTmpDir() { return System.getProperty("java.io.tmpdir"); }
        @Override public String getPathSeparator() { return File.pathSeparator; }
        @Override public String getFileSeparator() { return File.separator; }
        @Override public String getOsFamily() { return "unknown"; }
        @Override public boolean isAdmin() { return false; }

        @Override
        public Map<String, Object> exec(Map<String, Object> opts) {
            try {
                return executeProcess(opts, "sh", "-c");
            } catch (Exception e) {
                try {
                    return executeProcess(opts, "cmd.exe", "/c");
                } catch (Exception e2) {
                    throw new ControlFlow.RuntimeError(null, "Não foi possível executar o processo.");
                }
            }
        }
    }

    // ─── MÉTODO AUXILIAR: EXECUÇÃO DE PROCESSOS ──────────────────────────

    private static Map<String, Object> executeProcess(Map<String, Object> opts, String shell, String shellArg) {
        String command = (String) opts.get("command");
        if (command == null || command.isEmpty()) {
            throw new ControlFlow.RuntimeError(null, "OS.exec: 'command' é obrigatório.");
        }

        @SuppressWarnings("unchecked")
        List<String> argList = (List<String>) opts.get("args");
        if (argList == null) argList = new ArrayList<>();

        String cwd = (String) opts.get("cwd");
        @SuppressWarnings("unchecked")
        Map<String, String> env = (Map<String, String>) opts.get("env");
        long timeoutMs = opts.containsKey("timeout") ? ((Number) opts.get("timeout")).longValue() : 0;
        String stdin = (String) opts.get("stdin");

        List<String> cmd = new ArrayList<>();
        cmd.add(shell);
        cmd.add(shellArg);
        cmd.add(command);
        cmd.addAll(argList);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (cwd != null && !cwd.isEmpty()) pb.directory(new File(cwd));
        if (env != null && !env.isEmpty()) pb.environment().putAll(env);

        Process process = null;
        try {
            process = pb.start();

            if (stdin != null && !stdin.isEmpty()) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(stdin.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            }

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread outThread = readStream(process.getInputStream(), stdout);
            Thread errThread = readStream(process.getErrorStream(), stderr);

            int exitCode;
            if (timeoutMs > 0) {
                boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    throw new ControlFlow.RuntimeError(null, "Processo excedeu o tempo limite de " + timeoutMs + "ms");
                }
                exitCode = process.exitValue();
            } else {
                exitCode = process.waitFor();
            }

            outThread.join(100);
            errThread.join(100);

            Map<String, Object> result = new HashMap<>();
            result.put("exitCode", (long) exitCode);
            result.put("stdout", stdout.toString());
            result.put("stderr", stderr.toString());
            return result;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ControlFlow.RuntimeError(null, "Erro ao executar processo: " + e.getMessage());
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
        }
    }

    private static Thread readStream(final InputStream is, final StringBuilder sb) {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append('\n');
            } catch (IOException ignored) {}
        });
        t.setDaemon(true);
        t.start();
        return t;
    }

    // ─── UTILITÁRIOS DE REDE ───────────────────────────────────────────────

    private static String getMacAddress(NetworkInterface ni) {
        try {
            byte[] mac = ni.getHardwareAddress();
            if (mac == null) return null;
            StringBuilder sb = new StringBuilder();
            for (byte b : mac) sb.append(String.format("%02X", b)).append(':');
            if (!sb.isEmpty()) sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        } catch (Exception ignored) { return null; }
    }
}