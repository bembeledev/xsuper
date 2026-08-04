// O Motor vai consultar o `sdm.lock`, encontrar a biblioteca e usar a VFS!
import tests.listen.*;

println("--- INICIANDO TESTES DE SEGURANÇA ESTRITA ---");

try {
    println("\n1. Teste de Memória Curta (Byte):");
    let idade: Byte = 45;       // Passa na Alfândega!
    println("Idade guardada: " + idade);
    
    idade = 150;                // ❌ ESTOURO! Vai disparar o Listener e cair no catch!
} catch (e:Error) {
    println("✅ Bloqueado com sucesso: " + e.message);
}

try {
    println("\n2. Teste de Segurança de Acesso (PIN):");
    let codigo: Pin = "1234";   // Passa!
    println("PIN guardado: " + codigo);
    
    codigo = "12345";           // ❌ ESTOURO! (Tem 5 dígitos, a regra exige 4 ou 6)
} catch (e:Error) {
    println("✅ Bloqueado com sucesso: " + e.message);
}

try {
    println("\n3. Teste de Comunicação (Email):");
    let contato: Email = "dev@xpl.mz"; // Passa!
    println("Email guardado: " + contato);
    
    contato = "xpl.mz";         // ❌ ESTOURO! (Falta o '@')
} catch (e:Error) {
    println("✅ Bloqueado com sucesso: " + e.message);
}

println("\n🚀 Teste Completo! O Ecossistema SDM e os Listeners funcionam em harmonia perfeita.");