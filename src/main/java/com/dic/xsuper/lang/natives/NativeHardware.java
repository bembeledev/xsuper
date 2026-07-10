package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.ControlFlow;
import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;

import java.io.*;
import java.lang.management.*;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Módulo Nativo para obter informações detalhadas sobre o Hardware.
 * CPU, Memória, Discos, Rede, Sistema (motherboard, BIOS, etc.).
 */
public class NativeHardware {

    private static final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private static final Runtime runtime = Runtime.getRuntime();

    public static void register(Interpreter interpreter) {

        // =========================================================
        // 1. INFORMAÇÕES DA CPU
        // =========================================================
        interpreter.globals.defineConst("hw_cpu_info", buildFunc(0, (intp, args) -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("model", getCpuModel());
            info.put("vendor", getCpuVendor());
            info.put("architecture", osBean.getArch());
            info.put("cores", (long) osBean.getAvailableProcessors());
            info.put("physicalCores", getPhysicalCores());
            info.put("logicalCores", (long) osBean.getAvailableProcessors());
            info.put("maxFrequency", getCpuMaxFrequency());
            info.put("currentFrequency", getCpuCurrentFrequency());
            info.put("cacheL1", getCpuCache("L1"));
            info.put("cacheL2", getCpuCache("L2"));
            info.put("cacheL3", getCpuCache("L3"));
            info.put("bogoMips", getBogoMips());
            info.put("cpuUsage", getCpuUsage());
            return info;
        }));

        interpreter.globals.defineConst("hw_cpu_usage", buildFunc(0, (intp, args) -> {
            return getCpuUsage();
        }));

        interpreter.globals.defineConst("hw_cpu_temp", buildFunc(0, (intp, args) -> {
            return getCpuTemperature();
        }));

        // =========================================================
        // 2. INFORMAÇÕES DA MEMÓRIA
        // =========================================================
        interpreter.globals.defineConst("hw_memory_info", buildFunc(0, (intp, args) -> {
            Map<String, Object> info = new LinkedHashMap<>();
            long total = runtime.totalMemory();
            long free = runtime.freeMemory();
            long used = total - free;
            long max = runtime.maxMemory();

            info.put("totalMB", total / (1024 * 1024));
            info.put("usedMB", used / (1024 * 1024));
            info.put("freeMB", free / (1024 * 1024));
            info.put("maxMB", max / (1024 * 1024));
            info.put("usagePercent", (double) used / total * 100);
            return info;
        }));

        interpreter.globals.defineConst("hw_memory_usage", buildFunc(0, (intp, args) -> {
            long total = runtime.totalMemory();
            long used = total - runtime.freeMemory();
            return (double) used / total * 100;
        }));

        // =========================================================
        // 3. INFORMAÇÕES DOS DISCOS
        // =========================================================
        interpreter.globals.defineConst("hw_disk_info", buildFunc(0, (intp, args) -> {
            List<Map<String, Object>> disks = new ArrayList<>();
            File[] roots = File.listRoots();
            for (File root : roots) {
                Map<String, Object> disk = new LinkedHashMap<>();
                String path = root.getAbsolutePath();
                disk.put("path", path);
                disk.put("totalMB", root.getTotalSpace() / (1024 * 1024));
                disk.put("freeMB", root.getFreeSpace() / (1024 * 1024));
                disk.put("usedMB", (root.getTotalSpace() - root.getFreeSpace()) / (1024 * 1024));
                disk.put("usagePercent", root.getTotalSpace() > 0 ?
                        (double) (root.getTotalSpace() - root.getFreeSpace()) / root.getTotalSpace() * 100 : 0);
                disk.put("typ", getDiskType(path));
                disk.put("model", getDiskModel(path));
                disks.add(disk);
            }
            return disks;
        }));

        interpreter.globals.defineConst("hw_disk_usage", buildFunc(1, (intp, args) -> {
            String path = getString(intp, args, 0);
            File file = new File(path);
            if (!file.exists()) {
                throw new ControlFlow.RuntimeError(null, "Caminho não existe: " + path);
            }
            long total = file.getTotalSpace();
            if (total == 0) {
                throw new ControlFlow.RuntimeError(null, "Não foi possível obter espaço para: " + path);
            }
            long free = file.getFreeSpace();
            long used = total - free;
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("totalMB", total / (1024 * 1024));
            info.put("usedMB", used / (1024 * 1024));
            info.put("freeMB", free / (1024 * 1024));
            info.put("usagePercent", (double) used / total * 100);
            return info;
        }));

        // =========================================================
        // 4. INFORMAÇÕES DA REDE
        // =========================================================
        interpreter.globals.defineConst("hw_network_info", buildFunc(0, (intp, args) -> {
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
                throw new ControlFlow.RuntimeError(null, "Erro ao obter interfaces de rede: " + e.getMessage());
            }
            return interfaces;
        }));

        // =========================================================
        // 5. INFORMAÇÕES DO SISTEMA (Motherboard, BIOS, etc.)
        // =========================================================
        interpreter.globals.defineConst("hw_system_info", buildFunc(0, (intp, args) -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("manufacturer", getSystemManufacturer());
            info.put("model", getSystemModel());
            info.put("serial", getSystemSerial());
            info.put("biosVendor", getBiosVendor());
            info.put("biosVersion", getBiosVersion());
            info.put("biosDate", getBiosDate());
            return info;
        }));

        // =========================================================
        // 6. INFORMAÇÕES DA GPU (simplificada)
        // =========================================================
        interpreter.globals.defineConst("hw_gpu_info", buildFunc(0, (intp, args) -> {
            return getGpuInfo();
        }));

        // =========================================================
        // 7. INFORMAÇÕES DA BATERIA (para laptops)
        // =========================================================
        interpreter.globals.defineConst("hw_battery_info", buildFunc(0, (intp, args) -> {
            return getBatteryInfo();
        }));
    }

    // ─── MÉTODOS DE OBTENÇÃO DE DADOS ──────────────────────────────────

    private static String getCpuModel() {
        try {
            // Fallback para /proc/cpuinfo (Linux) ou wmic (Windows)
            if (isWindows()) {
                return execCommand("wmic cpu get name /format:csv").split(",")[1].trim();
            } else if (isLinux()) {
                return execCommand("grep 'model name' /proc/cpuinfo | head -1").replace("model name\t: ", "").trim();
            } else if (isMac()) {
                return execCommand("sysctl -n machdep.cpu.brand_string").trim();
            }
        } catch (Exception ignored) {
        }
        return System.getProperty("os.arch");
    }

    private static String getCpuVendor() {
        try {
            if (isLinux()) {
                return execCommand("grep 'vendor_id' /proc/cpuinfo | head -1").replace("vendor_id\t: ", "").trim();
            } else if (isMac()) {
                return execCommand("sysctl -n machdep.cpu.vendor").trim();
            } else if (isWindows()) {
                return execCommand("wmic cpu get manufacturer /format:csv").split(",")[1].trim();
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    private static long getPhysicalCores() {
        if (isWindows()) {
            try {
                String output = execCommand("wmic cpu get NumberOfCores /format:csv");
                return Long.parseLong(output.split(",")[1].trim());
            } catch (Exception ignored) {
            }
        } else if (isLinux()) {
            try {
                String output = execCommand("grep -c '^processor' /proc/cpuinfo");
                // Na verdade, isso dá lógicos. Para físicos, usamos o número de sockets * cores por socket
                // Simplificação: tentar ler 'cpu cores' por socket
                String coresPerSocket = execCommand("grep 'cpu cores' /proc/cpuinfo | head -1").replace("cpu cores\t: ", "").trim();
                if (!coresPerSocket.isEmpty()) {
                    long cores = Long.parseLong(coresPerSocket);
                    String sockets = execCommand("grep 'physical id' /proc/cpuinfo | sort -u | wc -l").trim();
                    if (!sockets.isEmpty()) {
                        return Long.parseLong(sockets) * cores;
                    }
                }
            } catch (Exception ignored) {
            }
        } else if (isMac()) {
            try {
                return Long.parseLong(execCommand("sysctl -n hw.physicalcpu").trim());
            } catch (Exception ignored) {
            }
        }
        return osBean.getAvailableProcessors(); // fallback
    }

    private static long getCpuMaxFrequency() {
        if (isLinux()) {
            try {
                String freq = execCommand("cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq").trim();
                return Long.parseLong(freq) / 1000; // kHz -> MHz
            } catch (Exception ignored) {
            }
        } else if (isMac()) {
            try {
                long freq = Long.parseLong(execCommand("sysctl -n hw.cpufrequency").trim());
                return freq / 1_000_000; // Hz -> MHz
            } catch (Exception ignored) {
            }
        } else if (isWindows()) {
            try {
                String out = execCommand("wmic cpu get MaxClockSpeed /format:csv").split(",")[1].trim();
                return Long.parseLong(out);
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    private static long getCpuCurrentFrequency() {
        if (isLinux()) {
            try {
                String freq = execCommand("cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_cur_freq").trim();
                return Long.parseLong(freq) / 1000;
            } catch (Exception ignored) {
            }
        } else if (isMac()) {
            return getCpuMaxFrequency(); // macOS não expõe facilmente a frequência atual
        } else if (isWindows()) {
            // Tenta usar a frequência atual via wmic
            try {
                String out = execCommand("wmic cpu get CurrentClockSpeed /format:csv").split(",")[1].trim();
                return Long.parseLong(out);
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    private static String getCpuCache(String level) {
        if (isLinux()) {
            try {
                String path = "/sys/devices/system/cpu/cpu0/cache/index" +
                        (level.equals("L1") ? "0" : level.equals("L2") ? "2" : "3") + "/size";
                return execCommand("cat " + path).trim();
            } catch (Exception ignored) {
            }
        } else if (isMac()) {
            // sysctl hw.l1icachesize, etc.
            String key = level.equals("L1") ? "hw.l1icachesize" :
                    level.equals("L2") ? "hw.l2cachesize" : "hw.l3cachesize";
            try {
                long size = Long.parseLong(execCommand("sysctl -n " + key).trim());
                return (size / 1024) + " KB";
            } catch (Exception ignored) {
            }
        } else if (isWindows()) {
            // wmic cpu get L2CacheSize, L3CacheSize
            try {
                String key = level.equals("L1") ? "L1CacheSize" :
                        level.equals("L2") ? "L2CacheSize" : "L3CacheSize";
                String out = execCommand("wmic cpu get " + key + " /format:csv").split(",")[1].trim();
                return out + " KB";
            } catch (Exception ignored) {
            }
        }
        return "unknown";
    }

    private static double getBogoMips() {
        if (isLinux()) {
            try {
                String bogo = execCommand("grep 'bogomips' /proc/cpuinfo | head -1").replace("bogomips\t: ", "").trim();
                return Double.parseDouble(bogo);
            } catch (Exception ignored) {
            }
        }
        return 0.0;
    }

    private static double getCpuUsage() {
        // Usa OperatingSystemMXBean se for com.sun.management.OperatingSystemMXBean
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOs =
                    (com.sun.management.OperatingSystemMXBean) osBean;
            return sunOs.getCpuLoad() * 100;
        }
        // Fallback: usar thread CPU time (mais complicado)
        return 0.0;
    }

    private static double getCpuTemperature() {
        if (isLinux()) {
            // Tentar /sys/class/thermal/thermal_zone0/temp
            try {
                String temp = execCommand("cat /sys/class/thermal/thermal_zone0/temp").trim();
                return Double.parseDouble(temp) / 1000.0;
            } catch (Exception ignored) {
            }
        } else if (isMac()) {
            try {
                String temp = execCommand("sudo powermetrics --samplers smc -n 1 | grep -i 'CPU die temperature'").trim();
                // Extrair número
                return Double.parseDouble(temp.replaceAll("[^0-9.]", ""));
            } catch (Exception ignored) {
            }
        } else if (isWindows()) {
            try {
                // Usar wmic /namespace:\\root\wmi PATH MSAcpi_ThermalZoneTemperature
                String out = execCommand("wmic /namespace:\\\\root\\wmi PATH MSAcpi_ThermalZoneTemperature get CurrentTemperature /format:csv").split(",")[1].trim();
                return (Double.parseDouble(out) - 2732) / 10.0; // Kelvin -> Celsius
            } catch (Exception ignored) {
            }
        }
        return 0.0;
    }

    private static String getDiskType(String path) {
        // Tenta determinar se é SSD ou HDD (Windows)
        if (isWindows()) {
            try {
                // wmic diskdrive where "DeviceID='\\\\.\\PHYSICALDRIVE0'" get MediaType
                String physicalDrive = getPhysicalDrive(path);
                if (physicalDrive != null) {
                    String out = execCommand("wmic diskdrive where \"DeviceID='" + physicalDrive + "'\" get MediaType /format:csv").split(",")[1].trim();
                    if (out.contains("SSD") || out.contains("Solid State")) return "SSD";
                    else if (out.contains("HDD") || out.contains("Hard Disk")) return "HDD";
                }
            } catch (Exception ignored) {
            }
        } else if (isLinux()) {
            try {
                // Verificar /sys/block/sdX/queue/rotational
                String[] parts = path.split("/");
                String dev = parts[parts.length - 1];
                String rot = execCommand("cat /sys/block/" + dev + "/queue/rotational 2>/dev/null").trim();
                if ("0".equals(rot)) return "SSD";
                else if ("1".equals(rot)) return "HDD";
            } catch (Exception ignored) {
            }
        }
        return "unknown";
    }

    private static String getDiskModel(String path) {
        if (isWindows()) {
            try {
                String physicalDrive = getPhysicalDrive(path);
                if (physicalDrive != null) {
                    return execCommand("wmic diskdrive where \"DeviceID='" + physicalDrive + "'\" get Model /format:csv").split(",")[1].trim();
                }
            } catch (Exception ignored) {
            }
        } else if (isLinux()) {
            try {
                String[] parts = path.split("/");
                String dev = parts[parts.length - 1];
                return execCommand("cat /sys/block/" + dev + "/device/model 2>/dev/null").trim();
            } catch (Exception ignored) {
            }
        }
        return "unknown";
    }

    private static String getPhysicalDrive(String path) {
        if (isWindows()) {
            try {
                // Usa volume para obter o dispositivo físico
                String volume = path.replace("\\", "\\\\");
                String out = execCommand("wmic volume where \"DriveLetter='" + volume + "'\" get DeviceID /format:csv");
                // Parse...
            } catch (Exception ignored) {
            }
        }
        return null;
    }

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
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String getSystemManufacturer() {
        if (isWindows()) {
            try {
                return execCommand("wmic csproduct get Vendor /format:csv").split(",")[1].trim();
            } catch (Exception ignored) {
            }
        } else if (isLinux()) {
            try {
                return execCommand("sudo dmidecode -s system-manufacturer").trim();
            } catch (Exception ignored) {
            }
        } else if (isMac()) {
            try {
                return execCommand("system_profiler SPHardwareDataType | grep 'Manufacturer' | cut -d ':' -f2").trim();
            } catch (Exception ignored) {
            }
        }
        return "unknown";
    }

    private static String getSystemModel() {
        if (isWindows()) {
            try {
                return execCommand("wmic csproduct get Name /format:csv").split(",")[1].trim();
            } catch (Exception ignored) {
            }
        } else if (isLinux()) {
            try {
                return execCommand("sudo dmidecode -s system-product-name").trim();
            } catch (Exception ignored) {
            }
        } else if (isMac()) {
            try {
                return execCommand("system_profiler SPHardwareDataType | grep 'Model Identifier' | cut -d ':' -f2").trim();
            } catch (Exception ignored) {
            }
        }
        return "unknown";
    }

    private static String getSystemSerial() {
        if (isWindows()) {
            try {
                return execCommand("wmic bios get SerialNumber /format:csv").split(",")[1].trim();
            } catch (Exception ignored) {
            }
        } else if (isLinux()) {
            try {
                return execCommand("sudo dmidecode -s system-serial-number").trim();
            } catch (Exception ignored) {
            }
        } else if (isMac()) {
            try {
                return execCommand("system_profiler SPHardwareDataType | grep 'Serial Number' | cut -d ':' -f2").trim();
            } catch (Exception ignored) {
            }
        }
        return "unknown";
    }

    private static String getBiosVendor() {
        if (isWindows()) {
            try {
                return execCommand("wmic bios get Manufacturer /format:csv").split(",")[1].trim();
            } catch (Exception ignored) {
            }
        } else if (isLinux()) {
            try {
                return execCommand("sudo dmidecode -s bios-vendor").trim();
            } catch (Exception ignored) {
            }
        } else if (isMac()) {
            try {
                return execCommand("system_profiler SPHardwareDataType | grep 'Boot ROM Version' | cut -d ':' -f2").trim();
            } catch (Exception ignored) {
            }
        }
        return "unknown";
    }

    private static String getBiosVersion() {
        if (isWindows()) {
            try {
                return execCommand("wmic bios get SMBIOSBIOSVersion /format:csv").split(",")[1].trim();
            } catch (Exception ignored) {
            }
        } else if (isLinux()) {
            try {
                return execCommand("sudo dmidecode -s bios-version").trim();
            } catch (Exception ignored) {
            }
        } else if (isMac()) {
            try {
                return execCommand("system_profiler SPHardwareDataType | grep 'Boot ROM Version' | cut -d ':' -f2").trim();
            } catch (Exception ignored) {
            }
        }
        return "unknown";
    }

    private static String getBiosDate() {
        if (isWindows()) {
            try {
                return execCommand("wmic bios get ReleaseDate /format:csv").split(",")[1].trim();
            } catch (Exception ignored) {
            }
        } else if (isLinux()) {
            try {
                return execCommand("sudo dmidecode -s bios-release-date").trim();
            } catch (Exception ignored) {
            }
        } else if (isMac()) {
            try {
                return execCommand("system_profiler SPHardwareDataType | grep 'System Version' | cut -d ':' -f2").trim();
            } catch (Exception ignored) {
            }
        }
        return "unknown";
    }

    private static List<Map<String, Object>> getGpuInfo() {
        List<Map<String, Object>> gpus = new ArrayList<>();
        if (isWindows()) {
            try {
                String out = execCommand("wmic path win32_VideoController get Name,AdapterRAM,DriverVersion /format:csv");
                String[] lines = out.split("\n");
                for (int i = 1; i < lines.length; i++) {
                    String[] parts = lines[i].split(",");
                    if (parts.length >= 3) {
                        Map<String, Object> gpu = new LinkedHashMap<>();
                        gpu.put("name", parts[1].trim());
                        gpu.put("memoryMB", parts[2].isEmpty() ? 0 : Long.parseLong(parts[2].trim()) / (1024 * 1024));
                        gpu.put("driverVersion", parts[3].trim());
                        gpus.add(gpu);
                    }
                }
            } catch (Exception ignored) {
            }
        } else if (isLinux()) {
            try {
                // Tentar lspci
                String out = execCommand("lspci -v | grep -A 10 -i 'VGA'");
                Map<String, Object> gpu = new LinkedHashMap<>();
                gpu.put("name", out.split("\n")[0].trim());
                // Extrair memória
                // ... simplificado
                gpus.add(gpu);
            } catch (Exception ignored) {
            }
        } else if (isMac()) {
            try {
                String out = execCommand("system_profiler SPDisplaysDataType | grep 'Chipset Model'");
                Map<String, Object> gpu = new LinkedHashMap<>();
                gpu.put("name", out.replace("Chipset Model: ", "").trim());
                gpus.add(gpu);
            } catch (Exception ignored) {
            }
        }
        return gpus;
    }

    private static Map<String, Object> getBatteryInfo() {
        Map<String, Object> battery = new LinkedHashMap<>();
        if (isWindows()) {
            try {
                String out = execCommand("wmic path Win32_Battery get EstimatedChargeRemaining,EstimatedRunTime /format:csv");
                String[] parts = out.split(",");
                if (parts.length >= 3) {
                    battery.put("chargePercent", parts[1].isEmpty() ? 0 : Long.parseLong(parts[1].trim()));
                    battery.put("runTimeMinutes", parts[2].isEmpty() ? 0 : Long.parseLong(parts[2].trim()));
                }
            } catch (Exception ignored) {
            }
        } else if (isLinux()) {
            try {
                // /sys/class/power_supply/BAT0/
                String capacity = execCommand("cat /sys/class/power_supply/BAT0/capacity 2>/dev/null").trim();
                if (!capacity.isEmpty()) {
                    battery.put("chargePercent", Long.parseLong(capacity));
                }
                String status = execCommand("cat /sys/class/power_supply/BAT0/status 2>/dev/null").trim();
                battery.put("status", status);
            } catch (Exception ignored) {
            }
        } else if (isMac()) {
            try {
                String out = execCommand("pmset -g batt | grep -E '([0-9]+)%'");
                String percent = out.replaceAll(".*?([0-9]+)%.*", "$1");
                battery.put("chargePercent", Long.parseLong(percent));
                String status = out.contains("charging") ? "Charging" : "Discharging";
                battery.put("status", status);
            } catch (Exception ignored) {
            }
        }
        return battery;
    }

    // ─── AUXILIARES ──────────────────────────────────────────────────────

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }

    private static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }

    private static String execCommand(String command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder();
        if (isWindows()) {
            pb.command("cmd.exe", "/c", command);
        } else {
            pb.command("sh", "-c", command);
        }
        Process p = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            try {
                p.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return output.toString().trim();
        }
    }

    // ─── HELPERS GENÉRICOS ──────────────────────────────────────────────

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