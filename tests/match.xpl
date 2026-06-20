var dado = "Código: 404";
var perigo = true;

let analise = match (dado) {
    // 1. Testa se é um Inteiro e se é maior que 500
    type int if (dado > 500): "Erro Numérico Grave";

    // 2. Testa se é um texto exato
    "OK": "Sistema Estável";

    // 3. Testa se é do tipo String E a variável externa 'perigo' está ativa!
    type String if (perigo): "Alerta de Texto Crítico: " + dado;

    // 4. Guarda lógica sem valor à esquerda
    if (dado == null): "Ausência total de sinal";

    // 5. A tua palavra limpa!
    none: "Sem diagnóstico possível";
};

println("Resultado da perícia: " + analise);