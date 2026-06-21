println(">>> TESTE DE TRATAMENTO DE ERROS <<<", "#00FFFF");


declare NumberError extends Error {
    pub valorTentado: int;
}
implement NumberError { default { valorTentado: 0 } }

try {
    let err = new NumberError();
    err.message = "Falha ao calcular salário";
    err.valorTentado = 500;

    throw err; // Atiramos a instância!

} catch(e: NumberError) {
    println("Capturado erro numérico: " + e.message);
    println("Valor que falhou: " + e.valorTentado);
} catch(e: Error) {
    println("Capturado erro genérico!");
} catch(e: string) {
    println("Capturado erro de texto puro!");
}

println(">>> O XPL CONTINUA A RODAR PERFEITAMENTE! <<<", "#00FFFF");