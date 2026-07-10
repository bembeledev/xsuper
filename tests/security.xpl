println("==========================================");
println("     🔐 DEMONSTRAÇÃO COMPLETA: SECURITY   ");
println("==========================================");

// =========================================================
// 1. HASHES (todos os algoritmos)
// =========================================================
println("\n1. HASHES");
println("  MD5:      " + md5("XPL é incrível"));
println("  SHA-1:    " + sha1("XPL é incrível"));
println("  SHA-224:  " + sha224("XPL é incrível"));
println("  SHA-256:  " + sha256("XPL é incrível"));
println("  SHA-384:  " + sha384("XPL é incrível"));
println("  SHA-512:  " + sha512("XPL é incrível"));
println("  SHA3-224: " + sha3_224("XPL é incrível"));
println("  SHA3-256: " + sha3_256("XPL é incrível"));
println("  SHA3-384: " + sha3_384("XPL é incrível"));
println("  SHA3-512: " + sha3_512("XPL é incrível"));
println("  BLAKE2b:  " + blake2b("XPL é incrível"));
println("  BLAKE2s:  " + blake2s("XPL é incrível"));

// =========================================================
// 2. HMAC
// =========================================================
println("\n2. HMAC");
let chave_hmac = "minha_chave_secreta";
println("  HMAC-MD5:    " + hmac_md5(chave_hmac, "XPL"));
println("  HMAC-SHA256: " + hmac_sha256(chave_hmac, "XPL"));
println("  HMAC-SHA512: " + hmac_sha512(chave_hmac, "XPL"));

// =========================================================
// 3. AES (CBC) - COM CHAVE E IV CORRETOS
// =========================================================
println("\n3. AES-256-CBC (com chave de 32 bytes)");

// ⭐ GERAR CHAVE E IV DE 32 E 16 BYTES RESPETIVAMENTE
// random_bytes devolve Base64. Descodificamos para obter os bytes reais.
let chave_aes_b64 = random_bytes(32);        // 32 bytes em Base64
let iv_aes_b64   = random_bytes(16);         // 16 bytes em Base64

// Para usar, passamos a string Base64 diretamente – a função getBytes() descodifica automaticamente!
let texto_original = "Mensagem confidencial para o XPL";
println("  Texto original: " + texto_original);

let cifra_b64 = aes_encrypt(chave_aes_b64, texto_original, iv_aes_b64);
println("  Cifra (Base64): " + cifra_b64);

let decifrado = aes_decrypt(chave_aes_b64, cifra_b64, iv_aes_b64);
println("  Decifrado:     " + decifrado);

// =========================================================
// 4. ChaCha20 (com nonce de 12 bytes)
// =========================================================
println("\n4. ChaCha20");
let chave_chacha_b64 = random_bytes(32);   // 32 bytes
let nonce_chacha_b64 = random_bytes(12);   // 12 bytes (recomendado)

let msg_chacha = "ChaCha20 é muito rápido!";
println("  Mensagem: " + msg_chacha);

let cifra_chacha_b64 = chacha20_encrypt(chave_chacha_b64, msg_chacha, nonce_chacha_b64);
println("  Cifra:    " + cifra_chacha_b64);

let dec_chacha = chacha20_decrypt(chave_chacha_b64, cifra_chacha_b64, nonce_chacha_b64);
println("  Decifrado: " + dec_chacha);

// =========================================================
// 5. RSA (gerar par, encriptar/desencriptar, assinar/verificar)
// =========================================================
println("\n5. RSA (2048 bits)");
let par_rsa = rsa_generate(2048);
println("  Chave pública (Base64): " + par_rsa.public);
println("  Chave privada (Base64): " + par_rsa.private);

let dados_rsa = "Assinatura digital com XPL";
println("  Dados: " + dados_rsa);

let cifra_rsa_b64 = rsa_encrypt(par_rsa.public, dados_rsa);
println("  Cifra RSA: " + cifra_rsa_b64);

let dec_rsa = rsa_decrypt(par_rsa.private, cifra_rsa_b64);
println("  Decifrado: " + dec_rsa);

// Assinatura
let assinatura_b64 = rsa_sign(par_rsa.private, dados_rsa);
println("  Assinatura: " + assinatura_b64);

let verificacao = rsa_verify(par_rsa.public, dados_rsa, assinatura_b64);
println("  Assinatura válida? " + verificacao);

// =========================================================
// 6. ECDSA (secp256r1)
// =========================================================
println("\n6. ECDSA (secp256r1)");
let par_ecdsa = ecdsa_generate();
println("  Chave pública: " + par_ecdsa.public);
println("  Chave privada: " + par_ecdsa.private);

let dados_ecdsa = "Mensagem para ECDSA";
let assinatura_ecdsa = ecdsa_sign(par_ecdsa.private, dados_ecdsa);
println("  Assinatura: " + assinatura_ecdsa);

let verif_ecdsa = ecdsa_verify(par_ecdsa.public, dados_ecdsa, assinatura_ecdsa);
println("  Assinatura válida? " + verif_ecdsa);

// =========================================================
// 7. KDF (PBKDF2 e Argon2id)
// =========================================================
println("\n7. KDF");
let senha = "minha_senha_super_secreta";
let salt = "sal_aleatorio_123";

let pbkdf2_hash = pbkdf2_sha256(senha, salt, 10000);
println("  PBKDF2-SHA256: " + pbkdf2_hash);

let argon2_hash = argon2id_hash(senha, salt);
println("  Argon2id hash: " + argon2_hash);

let argon2_ok = argon2id_verify(senha, salt, argon2_hash);
println("  Argon2id verificação: " + argon2_ok);

// =========================================================
// 8. Codificações (Base64, Base58, Hex)
// =========================================================
println("\n8. CODIFICAÇÕES");
let texto_cod = "XPL é top!";

println("  Base64 encode: " + base64_encode(texto_cod));
println("  Base64 decode: " + base64_decode(base64_encode(texto_cod)));

println("  Base58 encode: " + base58_encode(texto_cod));
println("  Base58 decode: " + base58_decode(base58_encode(texto_cod)));

println("  Hex encode:    " + hex_encode(texto_cod));
println("  Hex decode:    " + hex_decode(hex_encode(texto_cod)));

// =========================================================
// 9. RNG (bytes, inteiros, UUID)
// =========================================================
println("\n9. RNG");
println("  Bytes aleatórios (16): " + random_bytes(16));
println("  Inteiro aleatório (1-100): " + random_int(1, 100));
println("  UUID v4: " + random_uuid());

// =========================================================
// 10. Comparação segura
// =========================================================
println("\n10. COMPARAÇÃO SEGURA");
let a = "abc123";
let b = "abc123";
let c = "xyz789";

println("  secure_compare(\"" + a + "\", \"" + b + "\") = " + secure_compare(a, b));
println("  secure_compare(\"" + a + "\", \"" + c + "\") = " + secure_compare(a, c));

println("\n==========================================");
println("  ✅ TODOS OS TESTES CONCLUÍDOS COM SUCESSO ");
println("==========================================");