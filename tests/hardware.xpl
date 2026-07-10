println("=== INFORMAÇÕES DE HARDWARE ===");

// Memória
println("Memória total: " + hw_memory_total() + " MB");
println("Memória livre: " + hw_memory_free() + " MB");
println("Memória máxima: " + hw_memory_max() + " MB");

// CPU
println("Núcleos: " + hw_cpu_cores());
println("Carga média (1 min): " + hw_os_load());

// Discos
let disks = hw_disks();
println("Discos:");
for d in disks {
    println("  " + d.name + " (" + d.typ + ") - " + d.path);
    println("    Total: " + d.total + " MB");
    println("    Livre: " + d.free + " MB");
    println("    Usado: " + d.used + " MB");
}

// Informação detalhada de um disco
let c = hw_disk_info("C:");
println("Info do disco C: " + c);

// Java
println("Java: " + hw_java_version() + " (" + hw_java_vendor() + ")");
println("Uptime: " + hw_uptime() + " ms");

// Bateria (pode ser null se não suportado)
let batt = hw_battery();
if (batt != null) {
    println("Carga da bateria: " + batt + "%");
} else {
    println("Bateria não disponível");
}