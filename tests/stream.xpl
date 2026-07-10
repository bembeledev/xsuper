println("===== TESTE DE STREAMS =====");

// 1. Escrever dados
var st = file_open_stream("demo.bin", "w");
st.write("Olá XPL!\n");
st.write("Linha 2");
st.close();

// 2. Ler dados em chunks
var st2 = file_open_stream("demo.bin", "r");
while (true) {
    let chunk = st2.read(16);
    if (chunk == null){break;}
    println("Chunk (Base64): " + chunk);
    println("Chunk (texto): " + base64_decode(chunk));
}
st2.close();

// 3. Leitura linha a linha (útil para logs/CSV)
var st3 = file_open_stream("demo.bin", "r");
while (true) {
    let linha = st3.readLine();
    if (linha == null) {break;};
    println("Linha: " + linha);
}
st3.close();

// 4. Propriedades
var st4 = file_open_stream("demo.bin", "r");
println("Tamanho: " + st4.size);
println("Caminho: " + st4.path);
st4.close();