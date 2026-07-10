println("=== SISTEMA ===");
println("SO: " + os_name() + " " + os_version());
println("Arquitetura: " + os_arch());
println("Hostname: " + os_hostname());
println("Username: " + os_username());
println("PID: " + os_pid());

println("\n=== HARDWARE ===");
println("CPUs: " + os_cpus());
println("Memória total: " + os_total_memory() + " MB");
println("Memória livre: " + os_free_memory() + " MB");

println("\n=== DISCOS ===");
let disks = os_disk_info();
for disk in disks {
    println("  " + disk.path + " (" + disk.usagePercent + "%)");
    println("    Total: " + disk.totalMB + " MB");
    println("    Usado: " + disk.usedMB + " MB");
    println("    Livre: " + disk.freeMB + " MB");
}

println("\n=== REDE ===");
let nets = os_network_interfaces();
for iface in nets {
    println("  " + iface.name + " (" + iface.displayName + ")");
    println("    MAC: " + iface.mac);
    println("    MTU: " + iface.mtu);
    println("    IPs: " + iface.ips);
}

println("\n=== AMBIENTE ===");
println("PATH: " + os_getenv("PATH"));
println("USER: " + os_getenv("USER"));

println("\n=== PROCESSO ===");
let cmd = {
    command: "echo",
    args: ["Olá do XPL!"],
    timeout: 1000
};
let res = os_exec(cmd);
println("Exit code: " + res.exitCode);
println("Stdout: " + res.stdout);