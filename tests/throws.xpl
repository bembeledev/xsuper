declare IOError extends Error {
    pub message: string;
}
implement IOError { default { message: "Erro Desconhecido" } }


// Uma função altamente perigosa
fun lerFicheiro(): string throws IOError {
    return "Dados do disco...";
}

// 1. ISTO DEVE CRASHAR A COMPILAÇÃO (Nenhuma proteção):
let dados = lerFicheiro();

// 2. ISTO É PERMITIDO (A função delega a responsabilidade):
fun processoGlobal() throws IOError {
    let d = lerFicheiro();
}

// 3. ISTO É PERMITIDO (Proteção imediata):
fun processoSeguro() {
    try {
        let d = lerFicheiro();
    } catch(e: IOError) {
        println("Salvo pelo catch!");
    }
}