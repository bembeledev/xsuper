println("=== INFORMAÇÕES DE HARDWARE ===");

// CPU
let cpu = hw_cpu_info();
println("CPU:");
println("  Modelo: " + cpu.model);
println("  Vendor: " + cpu.vendor);
println("  Arquitetura: " + cpu.architecture);
println("  Núcleos físicos: " + cpu.physicalCores);
println("  Núcleos lógicos: " + cpu.logicalCores);
println("  Frequência máxima: " + cpu.maxFrequency + " MHz");
println("  Frequência atual: " + cpu.currentFrequency + " MHz");
println("  Cache L1: " + cpu.cacheL1);
println("  Cache L2: " + cpu.cacheL2);
println("  Cache L3: " + cpu.cacheL3);
println("  BogoMIPS: " + cpu.bogoMips);
println("  Uso CPU: " + cpu.cpuUsage + "%");
println("  Temperatura: " + hw_cpu_temp() + " °C");

// Memória
let mem = hw_memory_info();
println("\nMemória:");
println("  Total: " + mem.totalMB + " MB");
println("  Usada: " + mem.usedMB + " MB");
println("  Livre: " + mem.freeMB + " MB");
println("  Máxima: " + mem.maxMB + " MB");
println("  Uso: " + mem.usagePercent + "%");

// Discos
let disks = hw_disk_info();
println("\nDiscos:");
for disk in disks {
    println("  " + disk.path);
    println("    Total: " + disk.totalMB + " MB");
    println("    Usado: " + disk.usedMB + " MB");
    println("    Livre: " + disk.freeMB + " MB");
    println("    Uso: " + disk.usagePercent + "%");
    println("    Tipo: " + disk.typ);
    println("    Modelo: " + disk.model);
}

// Rede
let nets = hw_network_info();
println("\nInterfaces de rede:");
for iface in nets {
    println("  " + iface.name + " (" + iface.displayName + ")");
    println("    MAC: " + iface.mac);
    println("    MTU: " + iface.mtu);
    println("    IPs: " + iface.ips);
}

// Sistema
let sys = hw_system_info();
println("\nSistema:");
println("  Fabricante: " + sys.manufacturer);
println("  Modelo: " + sys.model);
println("  Serial: " + sys.serial);
println("  BIOS: " + sys.biosVendor + " " + sys.biosVersion + " (" + sys.biosDate + ")");

// GPU
let gpus = hw_gpu_info();
println("\nGPU(s):");
for gpu in gpus {
    println("  " + gpu.name);
    println("    Memória: " + gpu.memoryMB + " MB");
    println("    Driver: " + gpu.driverVersion);
}

// Bateria
let bat = hw_battery_info();
if (bat != null && !bat.isEmpty) {
    println("\nBateria:");
    println("  Carga: " + bat.chargePercent + "%");
    println("  Estado: " + bat.status);
}