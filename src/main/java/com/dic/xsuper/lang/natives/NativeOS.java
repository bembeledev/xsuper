package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.ControlFlow;
import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Módulo Nativo para interagir com o Sistema Operacional.
 * <p>
 * Detecta automaticamente o SO e delega as operações ao provider adequado.
 * Suporta Windows, Linux, macOS, Android, iOS (fallback) e Unknown.
 * <p>
 * Fornece:
 * <ul>
 *   <li>Informações do sistema (nome, versão, arquitetura, hostname, username)</li>
 *   <li>Hardware básico (CPUs, memória, discos, interfaces de rede)</li>
 *   <li>Variáveis de ambiente</li>
 *   <li>Execução de processos com controlo (timeout, cwd, env, stdin)</li>
 *   <li>Utilitários (sleep, exit, PID)</li>
 * </ul>
 */
public class NativeOS {

    private static final OSProvider provider;

    static {
        provider = OSProvider.detect();
    }

    public static void register(Interpreter interpreter) {

        // ─── Informações do Sistema ────────────────────────────────────────
        interpreter.globals.defineConst("os_name", buildFunc(0, (intp, args) -> provider.getOsName()));
        interpreter.globals.defineConst("os_version", buildFunc(0, (intp, args) -> provider.getOsVersion()));
        interpreter.globals.defineConst("os_arch", buildFunc(0, (intp, args) -> provider.getArch()));
        interpreter.globals.defineConst("os_hostname", buildFunc(0, (intp, args) -> provider.getHostname()));
        interpreter.globals.defineConst("os_username", buildFunc(0, (intp, args) -> provider.getUsername()));

        // ─── Hardware Básico ───────────────────────────────────────────────
        interpreter.globals.defineConst("os_cpus", buildFunc(0, (intp, args) -> provider.getCpuCount()));
        interpreter.globals.defineConst("os_total_memory", buildFunc(0, (intp, args) -> provider.getTotalMemory()));
        interpreter.globals.defineConst("os_free_memory", buildFunc(0, (intp, args) -> provider.getFreeMemory()));
        interpreter.globals.defineConst("os_disk_info", buildFunc(0, (intp, args) -> provider.getDiskInfo()));
        interpreter.globals.defineConst("os_network_interfaces", buildFunc(0, (intp, args) -> provider.getNetworkInterfaces()));

        // ─── Variáveis de Ambiente ─────────────────────────────────────────
        interpreter.globals.defineConst("os_getenv", buildFunc(1, (intp, args) -> {
            String name = getString(intp, args, 0);
            return provider.getEnv(name);
        }));
        interpreter.globals.defineConst("os_getenv_all", buildFunc(0, (intp, args) -> provider.getEnvAll()));

        // ─── Caminhos do Sistema ──────────────────────────────────────────
        interpreter.globals.defineConst("os_user_home", buildFunc(0, (intp, args) -> provider.getUserHome()));
        interpreter.globals.defineConst("os_user_dir", buildFunc(0, (intp, args) -> provider.getUserDir()));
        interpreter.globals.defineConst("os_tmp_dir", buildFunc(0, (intp, args) -> provider.getTmpDir()));
        interpreter.globals.defineConst("os_path_separator", buildFunc(0, (intp, args) -> provider.getPathSeparator()));
        interpreter.globals.defineConst("os_file_separator", buildFunc(0, (intp, args) -> provider.getFileSeparator()));

        // ─── Execução de Processos ────────────────────────────────────────
        interpreter.globals.defineConst("os_exec", buildFunc(1, (intp, args) -> {
            Map<String, Object> opts = getMap(intp, args, 0);
            return provider.exec(opts);
        }));

        // ─── Utilitários ────────────────────────────────────────────────────
        interpreter.globals.defineConst("os_pid", buildFunc(0, (intp, args) -> provider.getPid()));
        interpreter.globals.defineConst("os_sleep", buildFunc(1, (intp, args) -> {
            long ms = ((Number) intp.evaluate(args.get(0).expression)).longValue();
            if (ms < 0) throw new ControlFlow.RuntimeError(null, "os_sleep: tempo não pode ser negativo.");
            try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return null;
        }));
        interpreter.globals.defineConst("os_exit", buildFunc(1, (intp, args) -> {
            int code = ((Number) intp.evaluate(args.get(0).expression)).intValue();
            System.exit(code);
            return null;
        }));
    }

    // ─── CLASSE ABSTRATA: OSProvider ──────────────────────────────────────

    public abstract static class OSProvider {
        // Deteção do SO
        public static OSProvider detect() {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) return new WindowsProvider();
            if (os.contains("linux") || os.contains("nix") || os.contains("aix")) {
                // Verifica se é Android
                if (System.getProperty("java.vendor", "").toLowerCase().contains("android") ||
                        System.getProperty("java.vm.name", "").toLowerCase().contains("android")) {
                    return new AndroidProvider();
                }
                return new LinuxProvider();
            }
            if (os.contains("mac") || os.contains("darwin")) return new MacProvider();
            if (os.contains("ios")) return new IOSProvider();
            return new UnknownProvider();
        }

        // Métodos abstratos (obrigatórios para todos os providers)
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
    }

    // ─── PROVIDER: WINDOWS ────────────────────────────────────────────────

    public static class WindowsProvider extends OSProvider {
        @Override public String getOsName() { return System.getProperty("os.name"); }
        @Override public String getOsVersion() { return System.getProperty("os.version"); }
        @Override public String getArch() { return System.getProperty("os.arch"); }
        @Override public String getHostname() { try { return java.net.InetAddress.getLocalHost().getHostName(); } catch(Exception e) { return "localhost"; } }
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
                String path = root.getAbsolutePath();
                info.put("path", path);
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
            } catch (SocketException e) {
                // Ignora
            }
            return interfaces;
        }

        @Override public String getEnv(String name) { return System.getenv(name); }
        @Override public Map<String, String> getEnvAll() { return new HashMap<>(System.getenv()); }
        @Override public String getUserHome() { return System.getProperty("user.home"); }
        @Override public String getUserDir() { return System.getProperty("user.dir"); }
        @Override public String getTmpDir() { return System.getProperty("java.io.tmpdir"); }
        @Override public String getPathSeparator() { return File.pathSeparator; }
        @Override public String getFileSeparator() { return File.separator; }

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
        @Override public String getHostname() { try { return java.net.InetAddress.getLocalHost().getHostName(); } catch(Exception e) { return "localhost"; } }
        @Override public String getUsername() { return System.getProperty("user.name"); }
        @Override public long getPid() { return ProcessHandle.current().pid(); }
        @Override public long getCpuCount() { return Runtime.getRuntime().availableProcessors(); }
        @Override public long getTotalMemory() { return Runtime.getRuntime().totalMemory() / (1024 * 1024); }
        @Override public long getFreeMemory() { return Runtime.getRuntime().freeMemory() / (1024 * 1024); }

        @Override
        public List<Map<String, Object>> getDiskInfo() {
            // Linux: usa /proc/mounts e statfs para informação
            List<Map<String, Object>> disks = new ArrayList<>();
            try {
                // Usa df -P para obter informação de todos os sistemas de ficheiros montados
                Process p = Runtime.getRuntime().exec(new String[]{"df", "-P"});
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
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
            } catch (SocketException e) {
                // Ignora
            }
            return interfaces;
        }

        @Override public String getEnv(String name) { return System.getenv(name); }
        @Override public Map<String, String> getEnvAll() { return new HashMap<>(System.getenv()); }
        @Override public String getUserHome() { return System.getProperty("user.home"); }
        @Override public String getUserDir() { return System.getProperty("user.dir"); }
        @Override public String getTmpDir() { return System.getProperty("java.io.tmpdir"); }
        @Override public String getPathSeparator() { return File.pathSeparator; }
        @Override public String getFileSeparator() { return File.separator; }

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
        @Override public String getHostname() { try { return java.net.InetAddress.getLocalHost().getHostName(); } catch(Exception e) { return "localhost"; } }
        @Override public String getUsername() { return System.getProperty("user.name"); }
        @Override public long getPid() { return ProcessHandle.current().pid(); }
        @Override public long getCpuCount() { return Runtime.getRuntime().availableProcessors(); }
        @Override public long getTotalMemory() { return Runtime.getRuntime().totalMemory() / (1024 * 1024); }
        @Override public long getFreeMemory() { return Runtime.getRuntime().freeMemory() / (1024 * 1024); }

        @Override
        public List<Map<String, Object>> getDiskInfo() {
            // macOS: usa df -P
            List<Map<String, Object>> disks = new ArrayList<>();
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"df", "-P"});
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
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
            } catch (SocketException e) {
                // Ignora
            }
            return interfaces;
        }

        @Override public String getEnv(String name) { return System.getenv(name); }
        @Override public Map<String, String> getEnvAll() { return new HashMap<>(System.getenv()); }
        @Override public String getUserHome() { return System.getProperty("user.home"); }
        @Override public String getUserDir() { return System.getProperty("user.dir"); }
        @Override public String getTmpDir() { return System.getProperty("java.io.tmpdir"); }
        @Override public String getPathSeparator() { return File.pathSeparator; }
        @Override public String getFileSeparator() { return File.separator; }

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
        @Override public String getHostname() { try { return java.net.InetAddress.getLocalHost().getHostName(); } catch(Exception e) { return "localhost"; } }
        @Override public String getUsername() { return System.getProperty("user.name"); }
        @Override public long getPid() { return ProcessHandle.current().pid(); }
        @Override public long getCpuCount() { return Runtime.getRuntime().availableProcessors(); }
        @Override public long getTotalMemory() { return Runtime.getRuntime().totalMemory() / (1024 * 1024); }
        @Override public long getFreeMemory() { return Runtime.getRuntime().freeMemory() / (1024 * 1024); }

        @Override
        public List<Map<String, Object>> getDiskInfo() {
            // Android tem /proc/mounts, mas não é trivial
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
            } catch (SocketException e) {
                // Ignora
            }
            return interfaces;
        }

        @Override public String getEnv(String name) { return System.getenv(name); }
        @Override public Map<String, String> getEnvAll() { return new HashMap<>(System.getenv()); }
        @Override public String getUserHome() { return System.getProperty("user.home"); }
        @Override public String getUserDir() { return System.getProperty("user.dir"); }
        @Override public String getTmpDir() { return System.getProperty("java.io.tmpdir"); }
        @Override public String getPathSeparator() { return File.pathSeparator; }
        @Override public String getFileSeparator() { return File.separator; }

        @Override
        public Map<String, Object> exec(Map<String, Object> opts) {
            // Android tem um shell limitado
            return executeProcess(opts, "sh", "-c");
        }
    }

    // ─── PROVIDER: IOS (fallback) ─────────────────────────────────────────

    public static class IOSProvider extends OSProvider {
        @Override public String getOsName() { return "iOS"; }
        @Override public String getOsVersion() { return System.getProperty("os.version"); }
        @Override public String getArch() { return System.getProperty("os.arch"); }
        @Override public String getHostname() { try { return java.net.InetAddress.getLocalHost().getHostName(); } catch(Exception e) { return "localhost"; } }
        @Override public String getUsername() { return System.getProperty("user.name"); }
        @Override public long getPid() { return ProcessHandle.current().pid(); }
        @Override public long getCpuCount() { return Runtime.getRuntime().availableProcessors(); }
        @Override public long getTotalMemory() { return Runtime.getRuntime().totalMemory() / (1024 * 1024); }
        @Override public long getFreeMemory() { return Runtime.getRuntime().freeMemory() / (1024 * 1024); }

        @Override
        public List<Map<String, Object>> getDiskInfo() { return Collections.emptyList(); }

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
            } catch (SocketException e) {
                // Ignora
            }
            return interfaces;
        }

        @Override public String getEnv(String name) { return System.getenv(name); }
        @Override public Map<String, String> getEnvAll() { return new HashMap<>(System.getenv()); }
        @Override public String getUserHome() { return System.getProperty("user.home"); }
        @Override public String getUserDir() { return System.getProperty("user.dir"); }
        @Override public String getTmpDir() { return System.getProperty("java.io.tmpdir"); }
        @Override public String getPathSeparator() { return File.pathSeparator; }
        @Override public String getFileSeparator() { return File.separator; }

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
        @Override public String getHostname() { try { return java.net.InetAddress.getLocalHost().getHostName(); } catch(Exception e) { return "localhost"; } }
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
                String path = root.getAbsolutePath();
                info.put("path", path);
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
            } catch (SocketException e) {
                // Ignora
            }
            return interfaces;
        }

        @Override public String getEnv(String name) { return System.getenv(name); }
        @Override public Map<String, String> getEnvAll() { return new HashMap<>(System.getenv()); }
        @Override public String getUserHome() { return System.getProperty("user.home"); }
        @Override public String getUserDir() { return System.getProperty("user.dir"); }
        @Override public String getTmpDir() { return System.getProperty("java.io.tmpdir"); }
        @Override public String getPathSeparator() { return File.pathSeparator; }
        @Override public String getFileSeparator() { return File.separator; }

        @Override
        public Map<String, Object> exec(Map<String, Object> opts) {
            // Fallback: tenta sh, depois cmd
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
            throw new ControlFlow.RuntimeError(null, "os_exec: 'command' é obrigatório.");
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
        if (cwd != null && !cwd.isEmpty()) {
            pb.directory(new File(cwd));
        }
        if (env != null && !env.isEmpty()) {
            pb.environment().putAll(env);
        }

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
                    throw new ControlFlow.RuntimeError(null,
                            "Processo excedeu o tempo limite de " + timeoutMs + "ms");
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
        } catch (IOException e) {
            throw new ControlFlow.RuntimeError(null, "Erro ao executar processo: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ControlFlow.RuntimeError(null, "Processo interrompido.");
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private static Thread readStream(final InputStream is, final StringBuilder sb) {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            } catch (IOException ignored) {
                // Ignora erros de leitura
            }
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
            for (byte b : mac) {
                sb.append(String.format("%02X", b));
                sb.append(':');
            }
            if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        } catch (Exception ignored) { return null; }
    }

    // ─── HELPERS GENÉRICOS ──────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMap(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        if (val instanceof Map) return (Map<String, Object>) val;
        throw new ControlFlow.RuntimeError(null, "os_exec: esperado um dicionário (Map) com as opções.");
    }

    private static String getString(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        return interpreter.stringify(val);
    }

    @FunctionalInterface
    private interface NativeAction {
        Object execute(Interpreter interpreter, List<Expr.CallArg> args);
    }

    private static XplCallable buildFunc(int arity, NativeAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return action.execute(intp, args);
            }
        };
    }
}