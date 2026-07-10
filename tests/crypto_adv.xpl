println("==========================================");
println("   🔐 CRIPTOGRAFIA AVANÇADA - PARTE 2    ");
println("==========================================");

// =========================================================
// 1. Gerar par RSA para testes
// =========================================================
let rsaPar = rsa_generate(2048);
println("1. Par RSA gerado.");

// =========================================================
// 2. CSR (Certificate Signing Request)
// =========================================================
println("\n2. Gerar CSR");
let csr = csr_generate("CN=Meu Servidor, O=Exemplo", rsaPar.private, rsaPar.public);
println("  CSR PEM:\n" + csr);

// =========================================================
// 3. Assinar CSR com a própria CA (auto-assinado)
// =========================================================
println("\n3. Assinar CSR com CA auto-assinada");
// Primeiro gerar um certificado auto-assinado para usar como CA
let caCert = x509_generate_selfsigned("CN=Minha CA, O=Exemplo", rsaPar.public, rsaPar.private);
let certAssinado = csr_sign(csr, rsaPar.private, caCert);
println("  Certificado assinado:\n" + certAssinado);

// =========================================================
// 4. PKCS#12
// =========================================================
println("\n4. Criar PKCS#12");
let pkcs12 = pkcs12_create(certAssinado, rsaPar.private, "minha_senha");
println("  PKCS12 (Base64): " + pkcs12);

println("\n5. Extrair PKCS#12");
let extraido = pkcs12_extract(pkcs12, "minha_senha");
println("  Certificado extraído:\n" + extraido.certificate);
println("  Chave privada extraída:\n" + extraido.privateKey);

// =========================================================
// 6. SSH Ed25519
// =========================================================
println("\n6. Gerar par de chaves Ed25519");
let edPar = ssh_keygen_ed25519();
println("  Chave pública (OpenSSH): " + edPar.public);
println("  Chave privada (PEM):\n" + edPar.private);

// Nota: para extrair a chave pública a partir da privada, usamos ssh_public_key_ed25519 (se disponível)
// Mas devido a limitações do Java, pode não funcionar. O melhor é guardar o par completo.

println("\n==========================================");
println("  ✅ TESTES CONCLUÍDOS COM SUCESSO");
println("==========================================");