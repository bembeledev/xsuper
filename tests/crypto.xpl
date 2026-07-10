println("==========================================");
println("   🔐 CRIPTOGRAFIA AVANÇADA - DEMONSTRAÇÃO  ");
println("==========================================");

// =========================================================
// 1. ECDSA com curvas personalizadas
// =========================================================
println("\n1. ECDSA - secp384r1");
let par1 = ecdsa_generate_ext("secp384r1");
println("  Chave pública: " + par1.public);
println("  Chave privada: " + par1.private);

let msg = "Mensagem para assinar com secp384r1";
let assinatura = ecdsa_sign_ext(par1.private, msg, "secp384r1");
println("  Assinatura: " + assinatura);

let verif = ecdsa_verify_ext(par1.public, msg, assinatura, "secp384r1");
println("  Verificação: " + verif);

// =========================================================
// 2. Encriptação híbrida (AES + RSA)
// =========================================================
println("\n2. Encriptação Híbrida (AES-256 + RSA-2048)");

// Gerar par RSA
let rsaPar = rsa_generate(2048);
let dados = "Mensagem muito longa que excede o limite do RSA puro... Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.";

let pacote = hybrid_encrypt(rsaPar.public, dados);
println("  IV (Base64): " + pacote.iv);
println("  Chave AES encriptada: " + pacote.encryptedKey);
println("  Dados encriptados: " + pacote.encryptedData);

let decifrado = hybrid_decrypt(rsaPar.private, pacote);
println("  Decifrado: " + decifrado);

// =========================================================
// 3. Certificados X.509
// =========================================================
println("\n3. Certificado Auto-Assinado X.509");

let certPem = x509_generate_selfsigned("CN=Meu Servidor, O=Exemplo", rsaPar.public, rsaPar.private);
println("  Certificado PEM:\n" + certPem);

let validacao = x509_validate(certPem);
println("  Validação:");
println("    Válido: " + validacao.valid);
println("    Sujeito: " + validacao.subject);
println("    Emissor: " + validacao.issuer);
println("    Válido de: " + validacao.notBefore);
println("    Válido até: " + validacao.notAfter);

// =========================================================
// 4. JWT (JSON Web Tokens)
// =========================================================
println("\n4. JWT - HS256");

let payload = {
    "sub": "1234567890",
    "name": "João Silva",
    "iat": 1516239022,
    "exp": 1916239022
};

let jwt = jwt_sign(payload, "minha_chave_secreta", "HS256");
println("  Token JWT: " + jwt);

let verificado = jwt_verify(jwt, "minha_chave_secreta", "HS256");
println("  Payload verificado:");
println("    sub: " + verificado.sub);
println("    name: " + verificado.name);
println("    exp: " + verificado.exp);

// =========================================================
// 5. SSH Keys
// =========================================================
println("\n5. SSH Public Key (OpenSSH format)");

let sshPub = ssh_public_key(rsaPar.public);
println("  SSH RSA Public Key:\n" + sshPub);

let parsed = ssh_parse_public_key(sshPub);
println("  Parsed: " + parsed.typ + " -> " + parsed.key);

println("\n==========================================");
println("  ✅ TESTES CONCLUÍDOS COM SUCESSO");
println("==========================================");