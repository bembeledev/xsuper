package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.ControlFlow;
import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Base64;

// Importante adicionar

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security; // Importante adicionar
import java.util.Date;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class NativeCryptoExtended {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void register(Interpreter interpreter) {

        // =========================================================
        // 1. ECDSA com curvas personalizadas
        // =========================================================
        interpreter.globals.defineConst("ecdsa_generate_ext", buildFunc(1, (intp, args) -> {
            String curve = getString(intp, args, 0);
            if (!curve.startsWith("secp") && !curve.startsWith("prime")) {
                throw new ControlFlow.RuntimeError(null, "Curva não suportada: " + curve + ". Use secp256r1, secp384r1, secp521r1.");
            }
            try {
                KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
                gen.initialize(new ECGenParameterSpec(curve));
                KeyPair pair = gen.generateKeyPair();
                return Map.of(
                        "public", Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()),
                        "private", Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()),
                        "curve", curve
                );
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "ecdsa_generate_ext: " + e.getMessage());
            }
        }));

        interpreter.globals.defineConst("ecdsa_sign_ext", buildFunc(3, (intp, args) -> {
            String privateKeyPem = getString(intp, args, 0);
            String data = getString(intp, args, 1);
            String curve = getString(intp, args, 2);
            try {
                PrivateKey priv = getPrivateKeyFromPem(privateKeyPem);
                Signature sig = Signature.getInstance(getEcdsaSignatureAlgorithm(curve));
                sig.initSign(priv);
                sig.update(data.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(sig.sign());
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "ecdsa_sign_ext: " + e.getMessage());
            }
        }));

        interpreter.globals.defineConst("ecdsa_verify_ext", buildFunc(4, (intp, args) -> {
            String publicKeyPem = getString(intp, args, 0);
            String data = getString(intp, args, 1);
            String signature = getString(intp, args, 2);
            String curve = getString(intp, args, 3);
            try {
                PublicKey pub = getPublicKeyFromPem(publicKeyPem);
                Signature sig = Signature.getInstance(getEcdsaSignatureAlgorithm(curve));
                sig.initVerify(pub);
                sig.update(data.getBytes(StandardCharsets.UTF_8));
                return sig.verify(Base64.getDecoder().decode(signature));
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "ecdsa_verify_ext: " + e.getMessage());
            }
        }));

        // =========================================================
        // 2. Encriptação híbrida (AES + RSA)
        // =========================================================
        interpreter.globals.defineConst("hybrid_encrypt", buildFunc(2, (intp, args) -> {
            String publicKeyPem = getString(intp, args, 0);
            String plaintext = getString(intp, args, 1);
            try {
                PublicKey rsaPublic = getPublicKeyFromPem(publicKeyPem);

                // 1. Gerar chave AES
                KeyGenerator kg = KeyGenerator.getInstance("AES");
                kg.init(256);
                SecretKey aesKey = kg.generateKey();
                byte[] aesKeyBytes = aesKey.getEncoded();

                // 2. Encriptar dados com AES
                Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                byte[] iv = new byte[16];
                new SecureRandom().nextBytes(iv);
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);
                byte[] encryptedData = aesCipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

                // 3. Encriptar chave AES com RSA
                Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                rsaCipher.init(Cipher.ENCRYPT_MODE, rsaPublic);
                byte[] encryptedAesKey = rsaCipher.doFinal(aesKeyBytes);

                return Map.of(
                        "iv", Base64.getEncoder().encodeToString(iv),
                        "encryptedKey", Base64.getEncoder().encodeToString(encryptedAesKey),
                        "encryptedData", Base64.getEncoder().encodeToString(encryptedData)
                );
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "hybrid_encrypt: " + e.getMessage());
            }
        }));

        interpreter.globals.defineConst("hybrid_decrypt", buildFunc(2, (intp, args) -> {
            String privateKeyPem = getString(intp, args, 0);
            Map<String, String> packageMap = getStringMap(intp, args, 1);
            try {
                PrivateKey rsaPrivate = getPrivateKeyFromPem(privateKeyPem);

                // 1. Desencriptar chave AES
                byte[] encryptedAesKey = Base64.getDecoder().decode(packageMap.get("encryptedKey"));
                Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                rsaCipher.init(Cipher.DECRYPT_MODE, rsaPrivate);
                byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAesKey);
                SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

                // 2. Desencriptar dados
                byte[] iv = Base64.getDecoder().decode(packageMap.get("iv"));
                byte[] encryptedData = Base64.getDecoder().decode(packageMap.get("encryptedData"));
                Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                aesCipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);
                byte[] plaintext = aesCipher.doFinal(encryptedData);
                return new String(plaintext, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "hybrid_decrypt: " + e.getMessage());
            }
        }));


// =========================================================
// 3. Certificados X.509 (auto-assinados)
// =========================================================
        interpreter.globals.defineConst("x509_generate_selfsigned", buildFunc(3, (intp, args) -> {
            String subject = getString(intp, args, 0);
            String publicKeyPem = getString(intp, args, 1);
            String privateKeyPem = getString(intp, args, 2);
            try {
                PublicKey pub = getPublicKeyFromPem(publicKeyPem);
                PrivateKey priv = getPrivateKeyFromPem(privateKeyPem);

                // Deteta o algoritmo de assinatura (RSA ou EC)
                String sigAlgo;
                if (priv.getAlgorithm().equalsIgnoreCase("RSA")) {
                    sigAlgo = "SHA256withRSA";
                } else if (priv.getAlgorithm().equalsIgnoreCase("EC")) {
                    sigAlgo = "SHA256withECDSA";
                } else {
                    throw new Exception("Algoritmo de chave não suportado: " + priv.getAlgorithm());
                }

                X500Name x500Name = new X500Name(subject);
                BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
                Instant now = Instant.now();
                Date startDate = Date.from(now);
                Date endDate = Date.from(now.plus(365, ChronoUnit.DAYS));

                X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                        x500Name, serial, startDate, endDate, x500Name, pub
                );

                ContentSigner signer = new JcaContentSignerBuilder(sigAlgo)
                        .setProvider("BC")
                        .build(priv);

                X509CertificateHolder holder = certBuilder.build(signer);
                X509Certificate cert = new JcaX509CertificateConverter()
                        .setProvider("BC")
                        .getCertificate(holder);

                // Exportar para PEM com quebras de linha a cada 64 caracteres (Padrão RFC 7468)
                String base64Cert = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(cert.getEncoded());

                StringWriter sw = new StringWriter();
                sw.write("-----BEGIN CERTIFICATE-----\n");
                sw.write(base64Cert);
                sw.write("\n-----END CERTIFICATE-----\n");
                return sw.toString();

            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "x509_generate_selfsigned: " + e.getMessage());
            }
        }));


        interpreter.globals.defineConst("x509_validate", buildFunc(1, (intp, args) -> {
            String pem = getString(intp, args, 0);
            try {
                X509Certificate cert = loadCertificateFromPem(pem);
                cert.checkValidity();
                cert.verify(cert.getPublicKey());
                return Map.of(
                        "valid", true,
                        "subject", cert.getSubjectX500Principal().toString(),
                        "issuer", cert.getIssuerX500Principal().toString(),
                        "notBefore", cert.getNotBefore().toString(),
                        "notAfter", cert.getNotAfter().toString(),
                        "serial", cert.getSerialNumber().toString()
                );
            } catch (Exception e) {
                return Map.of("valid", false, "error", e.getMessage());
            }
        }));

        // =========================================================
        // 4. JWT (JSON Web Tokens) - SEM DEPENDÊNCIAS EXTERNAS
        // =========================================================
        interpreter.globals.defineConst("jwt_sign", buildFunc(3, (intp, args) -> {
            Map<String, Object> payload = getMap(intp, args, 0);
            String secret = getString(intp, args, 1);
            String algorithm = getString(intp, args, 2);
            try {
                Map<String, String> header = new LinkedHashMap<>();
                header.put("alg", algorithm);
                header.put("typ", "JWT");

                String headerJson = toJson(header);
                String payloadJson = toJson(payload);

                String headerB64 = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
                String payloadB64 = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

                String signingInput = headerB64 + "." + payloadB64;
                byte[] signature = signJwt(algorithm, signingInput, secret);

                return headerB64 + "." + payloadB64 + "." + base64UrlEncode(signature);
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "jwt_sign: " + e.getMessage());
            }
        }));

        interpreter.globals.defineConst("jwt_verify", buildFunc(3, (intp, args) -> {
            String token = getString(intp, args, 0);
            String secret = getString(intp, args, 1);
            String algorithm = getString(intp, args, 2);
            try {
                String[] parts = token.split("\\.");
                if (parts.length != 3) throw new Exception("Token inválido");

                String headerB64 = parts[0];
                String payloadB64 = parts[1];
                String signatureB64 = parts[2];

                String headerJson = new String(base64UrlDecode(headerB64), StandardCharsets.UTF_8);
                Map<String, String> header = parseSimpleJson(headerJson);
                if (!algorithm.equals(header.get("alg"))) {
                    throw new Exception("Algoritmo não corresponde ao cabeçalho");
                }

                String signingInput = headerB64 + "." + payloadB64;
                byte[] signature = base64UrlDecode(signatureB64);
                boolean valid = verifyJwt(algorithm, signingInput, signature, secret);

                if (!valid) throw new Exception("Assinatura inválida");

                String payloadJson = new String(base64UrlDecode(payloadB64), StandardCharsets.UTF_8);
                Map<String, Object> payload = parseSimpleJsonObject(payloadJson);

                if (payload.containsKey("exp")) {
                    long exp = ((Number) payload.get("exp")).longValue();
                    if (exp < System.currentTimeMillis() / 1000) {
                        throw new Exception("Token expirado");
                    }
                }
                return payload;
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "jwt_verify: " + e.getMessage());
            }
        }));

        // =========================================================
        // 5. SSH Keys (OpenSSH format)
        // =========================================================
        interpreter.globals.defineConst("ssh_public_key", buildFunc(1, (intp, args) -> {
            String publicKeyPem = getString(intp, args, 0);
            try {
                PublicKey pub = getPublicKeyFromPem(publicKeyPem);
                if (pub instanceof RSAPublicKey) {
                    return toOpenSshRsa((RSAPublicKey) pub);
                } else if (pub instanceof ECPublicKey) {
                    return toOpenSshEc((ECPublicKey) pub);
                } else {
                    throw new Exception("Tipo de chave não suportado para SSH");
                }
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "ssh_public_key: " + e.getMessage());
            }
        }));

        interpreter.globals.defineConst("ssh_parse_public_key", buildFunc(1, (intp, args) -> {
            String sshKey = getString(intp, args, 0);
            try {
                return parseOpenSshPublicKey(sshKey);
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "ssh_parse_public_key: " + e.getMessage());
            }
        }));
    }

    // ─── UTILITÁRIOS DE ECDSA ──────────────────────────────────────────────

    private static String getEcdsaSignatureAlgorithm(String curve) {
        switch (curve) {
            case "secp256r1": return "SHA256withECDSA";
            case "secp384r1": return "SHA384withECDSA";
            case "secp521r1": return "SHA512withECDSA";
            default: throw new ControlFlow.RuntimeError(null, "Curva não suportada: " + curve);
        }
    }

    // ─── PARSER PEM ROBUSTO (auto-detecta RSA/EC) ────────────────────────

    private static byte[] pemToBytes(String pem) {
        // Remove cabeçalhos, rodapés, quebras de linha e espaços
        String cleaned = pem.replaceAll("-----(BEGIN|END) .*?-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(cleaned);
    }

    private static PrivateKey getPrivateKeyFromPem(String pem) throws Exception {
        byte[] bytes = pemToBytes(pem);
        // Tenta RSA primeiro
        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch (Exception eRsa) {
            try {
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
                KeyFactory kf = KeyFactory.getInstance("EC");
                return kf.generatePrivate(spec);
            } catch (Exception eEc) {
                throw new Exception("Não foi possível interpretar a chave privada (RSA/EC).");
            }
        }
    }

    private static PublicKey getPublicKeyFromPem(String pem) throws Exception {
        byte[] bytes = pemToBytes(pem);
        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch (Exception eRsa) {
            try {
                X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
                KeyFactory kf = KeyFactory.getInstance("EC");
                return kf.generatePublic(spec);
            } catch (Exception eEc) {
                throw new Exception("Não foi possível interpretar a chave pública (RSA/EC).");
            }
        }
    }

    // ─── PARSER DE CERTIFICADOS ────────────────────────────────────────────

    private static X509Certificate loadCertificateFromPem(String pem) throws Exception {
        byte[] bytes = pemToBytes(pem);
        java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new java.io.ByteArrayInputStream(bytes));
    }

    // ─── JWT HELPERS ──────────────────────────────────────────────────────

    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static byte[] base64UrlDecode(String data) {
        return Base64.getUrlDecoder().decode(data);
    }

    private static String toJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (i++ > 0) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":");
            Object val = e.getValue();
            if (val == null) {
                sb.append("null");
            } else if (val instanceof String) {
                sb.append("\"").append(escapeJson((String) val)).append("\"");
            } else if (val instanceof Number || val instanceof Boolean) {
                sb.append(val);
            } else {
                sb.append("\"").append(escapeJson(val.toString())).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ─── PARSER JSON MANUAL (SEM DEPENDÊNCIAS) ────────────────────────────

    private static Map<String, String> parseSimpleJson(String json) {
        Map<String, Object> raw = parseSimpleJsonObject(json);
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            result.put(e.getKey(), e.getValue() == null ? "null" : e.getValue().toString());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseSimpleJsonObject(String json) {
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            return new HashMap<>();
        }
        Map<String, Object> result = new HashMap<>();
        // Remove chavetas exteriores
        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty()) return result;

        // Tokenização básica (não suporta strings com vírgulas ou objetos aninhados, mas é suficiente para JWT)
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        boolean inKey = true;
        boolean inString = false;
        char prev = 0;

        for (char c : json.toCharArray()) {
            if (c == '"' && prev != '\\') {
                inString = !inString;
                if (inKey) {
                    // Nome da chave
                    key.append(c);
                } else {
                    value.append(c);
                }
            } else if (c == ':' && !inString) {
                inKey = false;
            } else if (c == ',' && !inString) {
                // Par chave-valor completo
                addPair(result, key.toString().trim(), value.toString().trim());
                key.setLength(0);
                value.setLength(0);
                inKey = true;
            } else {
                if (inKey) key.append(c);
                else value.append(c);
            }
            prev = c;
        }
        if (key.length() > 0) {
            addPair(result, key.toString().trim(), value.toString().trim());
        }
        return result;
    }

    private static void addPair(Map<String, Object> map, String key, String rawValue) {
        // Remove aspas da chave
        if (key.startsWith("\"") && key.endsWith("\"")) {
            key = key.substring(1, key.length() - 1);
        }
        // Interpreta o valor
        rawValue = rawValue.trim();
        Object val = null;
        if (rawValue.startsWith("\"") && rawValue.endsWith("\"")) {
            val = rawValue.substring(1, rawValue.length() - 1);
        } else if ("null".equals(rawValue)) {
            val = null;
        } else if ("true".equals(rawValue)) {
            val = true;
        } else if ("false".equals(rawValue)) {
            val = false;
        } else if (rawValue.matches("-?\\d+")) {
            try { val = Long.parseLong(rawValue); } catch (NumberFormatException e) { val = rawValue; }
        } else if (rawValue.matches("-?\\d+\\.\\d+")) {
            try { val = Double.parseDouble(rawValue); } catch (NumberFormatException e) { val = rawValue; }
        } else {
            val = rawValue;
        }
        map.put(key, val);
    }

    // ─── JWT ASSINATURA E VERIFICAÇÃO ────────────────────────────────────

    private static byte[] signJwt(String algorithm, String input, String secret) throws Exception {
        byte[] data = input.getBytes(StandardCharsets.UTF_8);
        switch (algorithm) {
            case "HS256": {
                Mac mac = Mac.getInstance("HmacSHA256");
                SecretKeySpec spec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
                mac.init(spec);
                return mac.doFinal(data);
            }
            case "RS256": {
                PrivateKey priv = getPrivateKeyFromPem(secret);
                Signature sig = Signature.getInstance("SHA256withRSA");
                sig.initSign(priv);
                sig.update(data);
                return sig.sign();
            }
            case "ES256": {
                PrivateKey priv = getPrivateKeyFromPem(secret);
                Signature sig = Signature.getInstance("SHA256withECDSA");
                sig.initSign(priv);
                sig.update(data);
                return sig.sign();
            }
            default: throw new Exception("Algoritmo JWT não suportado: " + algorithm);
        }
    }

    private static boolean verifyJwt(String algorithm, String input, byte[] signature, String secret) throws Exception {
        byte[] data = input.getBytes(StandardCharsets.UTF_8);
        switch (algorithm) {
            case "HS256": {
                Mac mac = Mac.getInstance("HmacSHA256");
                SecretKeySpec spec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
                mac.init(spec);
                byte[] computed = mac.doFinal(data);
                return MessageDigest.isEqual(computed, signature);
            }
            case "RS256": {
                PublicKey pub = getPublicKeyFromPem(secret);
                Signature sig = Signature.getInstance("SHA256withRSA");
                sig.initVerify(pub);
                sig.update(data);
                return sig.verify(signature);
            }
            case "ES256": {
                PublicKey pub = getPublicKeyFromPem(secret);
                Signature sig = Signature.getInstance("SHA256withECDSA");
                sig.initVerify(pub);
                sig.update(data);
                return sig.verify(signature);
            }
            default: throw new Exception("Algoritmo JWT não suportado: " + algorithm);
        }
    }

    // ─── SSH HELPERS ─────────────────────────────────────────────────────

    private static String toOpenSshRsa(RSAPublicKey pub) {
        byte[] e = pub.getPublicExponent().toByteArray();
        byte[] n = pub.getModulus().toByteArray();
        String type = "ssh-rsa";
        byte[] data = new byte[4 + type.length() + 4 + e.length + 4 + n.length];
        int pos = 0;
        pos = writeBytes(data, pos, type.getBytes(StandardCharsets.UTF_8));
        pos = writeBytes(data, pos, e);
        pos = writeBytes(data, pos, n);
        return type + " " + Base64.getEncoder().encodeToString(data);
    }

    private static String toOpenSshEc(ECPublicKey pub) {
        int size = pub.getParams().getCurve().getField().getFieldSize();
        String curveName = size == 256 ? "nistp256" : size == 384 ? "nistp384" : "nistp521";
        String type = "ecdsa-sha2-" + curveName;
        byte[] curveNameBytes = curveName.getBytes(StandardCharsets.UTF_8);
        byte[] keyData = extractEcPublicKey(pub);
        byte[] data = new byte[4 + type.length() + 4 + curveNameBytes.length + 4 + keyData.length];
        int pos = 0;
        pos = writeBytes(data, pos, type.getBytes(StandardCharsets.UTF_8));
        pos = writeBytes(data, pos, curveNameBytes);
        pos = writeBytes(data, pos, keyData);
        return type + " " + Base64.getEncoder().encodeToString(data);
    }

    private static byte[] extractEcPublicKey(ECPublicKey ec) {
        java.security.spec.ECPoint point = ec.getW();
        byte[] x = point.getAffineX().toByteArray();
        byte[] y = point.getAffineY().toByteArray();
        byte[] result = new byte[1 + x.length + y.length];
        result[0] = 0x04;
        System.arraycopy(x, 0, result, 1, x.length);
        System.arraycopy(y, 0, result, 1 + x.length, y.length);
        return result;
    }

    private static int writeBytes(byte[] dest, int pos, byte[] src) {
        dest[pos++] = (byte) ((src.length >> 24) & 0xFF);
        dest[pos++] = (byte) ((src.length >> 16) & 0xFF);
        dest[pos++] = (byte) ((src.length >> 8) & 0xFF);
        dest[pos++] = (byte) (src.length & 0xFF);
        System.arraycopy(src, 0, dest, pos, src.length);
        return pos + src.length;
    }

    private static Map<String, Object> parseOpenSshPublicKey(String sshKey) {
        String[] parts = sshKey.trim().split(" ");
        if (parts.length < 2) throw new ControlFlow.RuntimeError(null, "Chave SSH inválida");
        Map<String, Object> result = new HashMap<>();
        result.put("typ", parts[0]);
        result.put("key", parts[1]);
        return result;
    }

    // ─── HELPERS GENÉRICOS ────────────────────────────────────────────────

    private static String getString(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        return interpreter.stringify(val);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMap(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        if (val instanceof Map) return (Map<String, Object>) val;
        throw new ControlFlow.RuntimeError(null, "Esperado um dicionário (Map).");
    }

    private static Map<String, String> getStringMap(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Map<String, Object> raw = getMap(interpreter, args, index);
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            result.put(e.getKey(), interpreter.stringify(e.getValue()));
        }
        return result;
    }

    @FunctionalInterface
    private interface NativeAction {
        Object execute(Interpreter interpreter, List<Expr.CallArg> args);
    }

    private static XplCallable buildFunc(int arity, NativeAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return action.execute(intp, args);
            }
        };
    }
}