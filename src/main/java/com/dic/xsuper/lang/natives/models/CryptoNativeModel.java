package com.dic.xsuper.lang.natives.models;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.poo.XplClass;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Base64;

public class CryptoNativeModel {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Crypto", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        // ─── 1. CSR ──────────────────────────────────────────────────────────
        model.staticFields.put("csrGenerate", buildAction(3, (intp, args) -> {
            String subject = getString(intp, args, 0);
            String privateKeyPem = getString(intp, args, 1);
            String publicKeyPem = getString(intp, args, 2);
            try {
                PrivateKey priv = getPrivateKeyFromPem(privateKeyPem);
                PublicKey pub = getPublicKeyFromPem(publicKeyPem);
                String sigAlgo = priv.getAlgorithm().equals("RSA") ? "SHA256withRSA" : "SHA256withECDSA";
                X500Name x500Name = new X500Name(subject);
                PKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(x500Name, pub);
                ContentSigner signer = new JcaContentSignerBuilder(sigAlgo).setProvider("BC").build(priv);
                PKCS10CertificationRequest csr = csrBuilder.build(signer);
                StringWriter sw = new StringWriter();
                sw.write("-----BEGIN CERTIFICATE REQUEST-----\n");
                sw.write(Base64.getEncoder().encodeToString(csr.getEncoded()));
                sw.write("\n-----END CERTIFICATE REQUEST-----\n");
                return sw.toString();
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "csrGenerate: " + e.getMessage());
            }
        }));

        model.staticFields.put("csrSign", buildAction(3, (intp, args) -> {
            String csrPem = getString(intp, args, 0);
            String caPrivateKeyPem = getString(intp, args, 1);
            String caCertificatePem = getString(intp, args, 2);
            try {
                byte[] csrBytes = pemToBytes(csrPem);
                PKCS10CertificationRequest csr = new PKCS10CertificationRequest(csrBytes);
                PrivateKey caPrivate = getPrivateKeyFromPem(caPrivateKeyPem);
                X509Certificate caCert = loadCertificateFromPem(caCertificatePem);
                X500Name subject = csr.getSubject();
                SubjectPublicKeyInfo pubInfo = csr.getSubjectPublicKeyInfo();
                PublicKey pubKey = new JcaPEMKeyConverter().setProvider("BC").getPublicKey(pubInfo);
                BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
                Instant now = Instant.now();
                Date startDate = Date.from(now);
                Date endDate = Date.from(now.plus(365, ChronoUnit.DAYS));
                X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                        caCert, serial, startDate, endDate, subject, pubKey
                );
                String sigAlgo = caPrivate.getAlgorithm().equalsIgnoreCase("RSA") ? "SHA256withRSA" : "SHA256withECDSA";
                ContentSigner signer = new JcaContentSignerBuilder(sigAlgo).setProvider("BC").build(caPrivate);
                X509CertificateHolder holder = certBuilder.build(signer);
                X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
                String base64Cert = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(cert.getEncoded());
                StringWriter sw = new StringWriter();
                sw.write("-----BEGIN CERTIFICATE-----\n");
                sw.write(base64Cert);
                sw.write("\n-----END CERTIFICATE-----\n");
                return sw.toString();
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "csrSign: " + e.getMessage());
            }
        }));

        // ─── 2. PKCS#12 ──────────────────────────────────────────────────────
        model.staticFields.put("pkcs12Create", buildAction(3, (intp, args) -> {
            String certificatePem = getString(intp, args, 0);
            String privateKeyPem = getString(intp, args, 1);
            String password = getString(intp, args, 2);
            try {
                X509Certificate cert = loadCertificateFromPem(certificatePem);
                PrivateKey priv = getPrivateKeyFromPem(privateKeyPem);
                KeyStore ks = KeyStore.getInstance("PKCS12");
                ks.load(null, null);
                ks.setKeyEntry("alias", priv, password.toCharArray(), new java.security.cert.Certificate[]{cert});
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ks.store(baos, password.toCharArray());
                return Base64.getEncoder().encodeToString(baos.toByteArray());
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "pkcs12Create: " + e.getMessage());
            }
        }));

        model.staticFields.put("pkcs12Extract", buildAction(2, (intp, args) -> {
            String pkcs12Base64 = getString(intp, args, 0);
            String password = getString(intp, args, 1);
            try {
                byte[] data = Base64.getDecoder().decode(pkcs12Base64);
                KeyStore ks = KeyStore.getInstance("PKCS12");
                ks.load(new ByteArrayInputStream(data), password.toCharArray());
                Enumeration<String> aliases = ks.aliases();
                if (!aliases.hasMoreElements()) throw new Exception("Keystore vazio");
                String alias = aliases.nextElement();
                PrivateKey priv = (PrivateKey) ks.getKey(alias, password.toCharArray());
                X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
                StringWriter swCert = new StringWriter();
                swCert.write("-----BEGIN CERTIFICATE-----\n");
                swCert.write(Base64.getEncoder().encodeToString(cert.getEncoded()));
                swCert.write("\n-----END CERTIFICATE-----\n");
                StringWriter swPriv = new StringWriter();
                swPriv.write("-----BEGIN PRIVATE KEY-----\n");
                swPriv.write(Base64.getEncoder().encodeToString(priv.getEncoded()));
                swPriv.write("\n-----END PRIVATE KEY-----\n");
                return Map.of("certificate", swCert.toString(), "privateKey", swPriv.toString());
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "pkcs12Extract: " + e.getMessage());
            }
        }));

        // ─── 3. SSH Ed25519 ──────────────────────────────────────────────────
        model.staticFields.put("sshKeygenEd25519", buildAction(0, (intp, args) -> {
            try {
                KeyPairGenerator gen = KeyPairGenerator.getInstance("Ed25519");
                KeyPair pair = gen.generateKeyPair();
                byte[] rawKey = extractEd25519PublicKey(pair.getPublic());
                String publicKeyOpenSsh = "ssh-ed25519 " + Base64.getEncoder().encodeToString(buildSshEd25519PublicKey(rawKey));
                StringWriter swPriv = new StringWriter();
                swPriv.write("-----BEGIN PRIVATE KEY-----\n");
                swPriv.write(Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
                swPriv.write("\n-----END PRIVATE KEY-----\n");
                return Map.of("public", publicKeyOpenSsh, "private", swPriv.toString());
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "sshKeygenEd25519: " + e.getMessage());
            }
        }));

        // ─── 4. ECDSA com curvas personalizadas ──────────────────────────────
        model.staticFields.put("ecdsaGenerateExt", buildAction(1, (intp, args) -> {
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
                throw new ControlFlow.RuntimeError(null, "ecdsaGenerateExt: " + e.getMessage());
            }
        }));

        model.staticFields.put("ecdsaSignExt", buildAction(3, (intp, args) -> {
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
                throw new ControlFlow.RuntimeError(null, "ecdsaSignExt: " + e.getMessage());
            }
        }));

        model.staticFields.put("ecdsaVerifyExt", buildAction(4, (intp, args) -> {
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
                throw new ControlFlow.RuntimeError(null, "ecdsaVerifyExt: " + e.getMessage());
            }
        }));

        // ─── 5. Encriptação híbrida (AES + RSA) ──────────────────────────────
        model.staticFields.put("hybridEncrypt", buildAction(2, (intp, args) -> {
            String publicKeyPem = getString(intp, args, 0);
            String plaintext = getString(intp, args, 1);
            try {
                PublicKey rsaPublic = getPublicKeyFromPem(publicKeyPem);
                KeyGenerator kg = KeyGenerator.getInstance("AES");
                kg.init(256);
                SecretKey aesKey = kg.generateKey();
                byte[] aesKeyBytes = aesKey.getEncoded();
                Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                byte[] iv = new byte[16];
                new SecureRandom().nextBytes(iv);
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);
                byte[] encryptedData = aesCipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
                Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                rsaCipher.init(Cipher.ENCRYPT_MODE, rsaPublic);
                byte[] encryptedAesKey = rsaCipher.doFinal(aesKeyBytes);
                return Map.of(
                        "iv", Base64.getEncoder().encodeToString(iv),
                        "encryptedKey", Base64.getEncoder().encodeToString(encryptedAesKey),
                        "encryptedData", Base64.getEncoder().encodeToString(encryptedData)
                );
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "hybridEncrypt: " + e.getMessage());
            }
        }));

        model.staticFields.put("hybridDecrypt", buildAction(2, (intp, args) -> {
            String privateKeyPem = getString(intp, args, 0);
            Map<String, String> packageMap = getStringMap(intp, args, 1);
            try {
                PrivateKey rsaPrivate = getPrivateKeyFromPem(privateKeyPem);
                byte[] encryptedAesKey = Base64.getDecoder().decode(packageMap.get("encryptedKey"));
                Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                rsaCipher.init(Cipher.DECRYPT_MODE, rsaPrivate);
                byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAesKey);
                SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");
                byte[] iv = Base64.getDecoder().decode(packageMap.get("iv"));
                byte[] encryptedData = Base64.getDecoder().decode(packageMap.get("encryptedData"));
                Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                aesCipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);
                byte[] plaintext = aesCipher.doFinal(encryptedData);
                return new String(plaintext, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "hybridDecrypt: " + e.getMessage());
            }
        }));

        // ─── 6. X.509 self-signed ────────────────────────────────────────────
        model.staticFields.put("x509GenerateSelfSigned", buildAction(3, (intp, args) -> {
            String subject = getString(intp, args, 0);
            String publicKeyPem = getString(intp, args, 1);
            String privateKeyPem = getString(intp, args, 2);
            try {
                PublicKey pub = getPublicKeyFromPem(publicKeyPem);
                PrivateKey priv = getPrivateKeyFromPem(privateKeyPem);
                String sigAlgo;
                if (priv.getAlgorithm().equalsIgnoreCase("RSA")) sigAlgo = "SHA256withRSA";
                else if (priv.getAlgorithm().equalsIgnoreCase("EC")) sigAlgo = "SHA256withECDSA";
                else throw new Exception("Algoritmo de chave não suportado: " + priv.getAlgorithm());
                X500Name x500Name = new X500Name(subject);
                BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
                Instant now = Instant.now();
                Date startDate = Date.from(now);
                Date endDate = Date.from(now.plus(365, ChronoUnit.DAYS));
                X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                        x500Name, serial, startDate, endDate, x500Name, pub
                );
                ContentSigner signer = new JcaContentSignerBuilder(sigAlgo).setProvider("BC").build(priv);
                X509CertificateHolder holder = certBuilder.build(signer);
                X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
                String base64Cert = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(cert.getEncoded());
                StringWriter sw = new StringWriter();
                sw.write("-----BEGIN CERTIFICATE-----\n");
                sw.write(base64Cert);
                sw.write("\n-----END CERTIFICATE-----\n");
                return sw.toString();
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "x509GenerateSelfSigned: " + e.getMessage());
            }
        }));

        model.staticFields.put("x509Validate", buildAction(1, (intp, args) -> {
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

        // ─── 7. JWT ──────────────────────────────────────────────────────────
        model.staticFields.put("jwtSign", buildAction(3, (intp, args) -> {
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
                throw new ControlFlow.RuntimeError(null, "jwtSign: " + e.getMessage());
            }
        }));

        model.staticFields.put("jwtVerify", buildAction(3, (intp, args) -> {
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
                if (!algorithm.equals(header.get("alg"))) throw new Exception("Algoritmo não corresponde ao cabeçalho");
                String signingInput = headerB64 + "." + payloadB64;
                byte[] signature = base64UrlDecode(signatureB64);
                if (!verifyJwt(algorithm, signingInput, signature, secret)) throw new Exception("Assinatura inválida");
                String payloadJson = new String(base64UrlDecode(payloadB64), StandardCharsets.UTF_8);
                Map<String, Object> payload = parseSimpleJsonObject(payloadJson);
                if (payload.containsKey("exp")) {
                    long exp = ((Number) payload.get("exp")).longValue();
                    if (exp < System.currentTimeMillis() / 1000) throw new Exception("Token expirado");
                }
                return payload;
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "jwtVerify: " + e.getMessage());
            }
        }));

        // ─── 8. SSH keys (OpenSSH format) ──────────────────────────────────
        model.staticFields.put("sshPublicKey", buildAction(1, (intp, args) -> {
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
                throw new ControlFlow.RuntimeError(null, "sshPublicKey: " + e.getMessage());
            }
        }));

        model.staticFields.put("sshParsePublicKey", buildAction(1, (intp, args) -> {
            String sshKey = getString(intp, args, 0);
            try {
                return parseOpenSshPublicKey(sshKey);
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "sshParsePublicKey: " + e.getMessage());
            }
        }));

        // ─── REGISTAR ──────────────────────────────────────────────────────

        interpreter.registry_model.put("Crypto", model);
        interpreter.environment.defineConst("Crypto", new XplClass(model, interpreter.globals));
    }

    // ─── UTILITÁRIOS PRIVADOS (partilhados) ──────────────────────────────────

    // ECDSA helper
    private static String getEcdsaSignatureAlgorithm(String curve) {
        switch (curve) {
            case "secp256r1": return "SHA256withECDSA";
            case "secp384r1": return "SHA384withECDSA";
            case "secp521r1": return "SHA512withECDSA";
            default: throw new ControlFlow.RuntimeError(null, "Curva não suportada: " + curve);
        }
    }

    // PEM helpers
    private static byte[] pemToBytes(String pem) {
        String cleaned = pem.replaceAll("-----(BEGIN|END) .*?-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(cleaned);
    }

    private static PrivateKey getPrivateKeyFromPem(String pem) throws Exception {
        byte[] bytes = pemToBytes(pem);
        String[] algos = {"RSA", "EC", "Ed25519"};
        Exception last = null;
        for (String algo : algos) {
            try {
                KeyFactory kf = KeyFactory.getInstance(algo);
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
                return kf.generatePrivate(spec);
            } catch (Exception e) { last = e; }
        }
        throw new Exception("Não foi possível interpretar a chave privada: " + last.getMessage());
    }

    private static PublicKey getPublicKeyFromPem(String pem) throws Exception {
        byte[] bytes = pemToBytes(pem);
        String[] algos = {"RSA", "EC", "Ed25519"};
        Exception last = null;
        for (String algo : algos) {
            try {
                KeyFactory kf = KeyFactory.getInstance(algo);
                X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
                return kf.generatePublic(spec);
            } catch (Exception e) { last = e; }
        }
        throw new Exception("Não foi possível interpretar a chave pública: " + last.getMessage());
    }

    private static X509Certificate loadCertificateFromPem(String pem) throws Exception {
        byte[] bytes = pemToBytes(pem);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(bytes));
    }

    // SSH Ed25519 helpers
    private static byte[] extractEd25519PublicKey(PublicKey pub) {
        byte[] encoded = pub.getEncoded();
        if (encoded.length >= 32) {
            byte[] raw = new byte[32];
            System.arraycopy(encoded, encoded.length - 32, raw, 0, 32);
            return raw;
        }
        throw new ControlFlow.RuntimeError(null, "Não foi possível extrair chave pública Ed25519");
    }

    private static byte[] buildSshEd25519PublicKey(byte[] rawKey) {
        String type = "ssh-ed25519";
        byte[] typeBytes = type.getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[4 + typeBytes.length + 4 + rawKey.length];
        int pos = 0;
        pos = writeBytes(data, pos, typeBytes);
        pos = writeBytes(data, pos, rawKey);
        return data;
    }

    private static int writeBytes(byte[] dest, int pos, byte[] src) {
        dest[pos++] = (byte) ((src.length >> 24) & 0xFF);
        dest[pos++] = (byte) ((src.length >> 16) & 0xFF);
        dest[pos++] = (byte) ((src.length >> 8) & 0xFF);
        dest[pos++] = (byte) (src.length & 0xFF);
        System.arraycopy(src, 0, dest, pos, src.length);
        return pos + src.length;
    }

    // JWT helpers
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
            if (val == null) sb.append("null");
            else if (val instanceof String) sb.append("\"").append(escapeJson((String) val)).append("\"");
            else if (val instanceof Number || val instanceof Boolean) sb.append(val);
            else sb.append("\"").append(escapeJson(val.toString())).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

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
        if (!json.startsWith("{") || !json.endsWith("}")) return new HashMap<>();
        Map<String, Object> result = new HashMap<>();
        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty()) return result;
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        boolean inKey = true;
        boolean inString = false;
        char prev = 0;
        for (char c : json.toCharArray()) {
            if (c == '"' && prev != '\\') {
                inString = !inString;
                if (inKey) key.append(c);
                else value.append(c);
            } else if (c == ':' && !inString) {
                inKey = false;
            } else if (c == ',' && !inString) {
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
        if (key.length() > 0) addPair(result, key.toString().trim(), value.toString().trim());
        return result;
    }

    private static void addPair(Map<String, Object> map, String key, String rawValue) {
        if (key.startsWith("\"") && key.endsWith("\"")) key = key.substring(1, key.length() - 1);
        rawValue = rawValue.trim();
        Object val;
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

    // SSH helpers (RSA e EC)
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

    private static Map<String, Object> parseOpenSshPublicKey(String sshKey) {
        String[] parts = sshKey.trim().split(" ");
        if (parts.length < 2) throw new ControlFlow.RuntimeError(null, "Chave SSH inválida");
        Map<String, Object> result = new HashMap<>();
        result.put("type", parts[0]);
        result.put("key", parts[1]);
        return result;
    }

    // ─── HELPERS GENÉRICOS ──────────────────────────────────────────────────

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

    private static String getString(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        return interpreter.stringify(val);
    }

    // ─── Builders ──────────────────────────────────────────────────────────

    @FunctionalInterface
    private interface NativeAction {
        Object execute(Interpreter interpreter, List<Expr.CallArg> args);
    }

    private static XplCallable buildAction(int arity, NativeAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return action.execute(intp, args);
            }
        };
    }
}