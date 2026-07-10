package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.ControlFlow;
import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Base64;

public class NativeCryptoAdvanced {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void register(Interpreter interpreter) {

        // =========================================================
        // 1. CSR (Certificate Signing Requests) - com chave pública
        // =========================================================
        interpreter.globals.defineConst("csr_generate", buildFunc(3, (intp, args) -> {
            String subject = getString(intp, args, 0);
            String privateKeyPem = getString(intp, args, 1);
            String publicKeyPem = getString(intp, args, 2);
            try {
                PrivateKey priv = getPrivateKeyFromPem(privateKeyPem);
                PublicKey pub = getPublicKeyFromPem(publicKeyPem);

                // Deteta o algoritmo de assinatura
                String sigAlgo = priv.getAlgorithm().equals("RSA") ? "SHA256withRSA" : "SHA256withECDSA";

                X500Name x500Name = new X500Name(subject);
                PKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(x500Name, pub);

                ContentSigner signer = new JcaContentSignerBuilder(sigAlgo)
                        .setProvider("BC")
                        .build(priv);

                PKCS10CertificationRequest csr = csrBuilder.build(signer);

                // Exportar para PEM
                StringWriter sw = new StringWriter();
                sw.write("-----BEGIN CERTIFICATE REQUEST-----\n");
                sw.write(Base64.getEncoder().encodeToString(csr.getEncoded()));
                sw.write("\n-----END CERTIFICATE REQUEST-----\n");
                return sw.toString();
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "csr_generate: " + e.getMessage());
            }
        }));

        // =========================================================
        // 2. Assinar CSR com CA
        // =========================================================
        interpreter.globals.defineConst("csr_sign", buildFunc(3, (intp, args) -> {
            String csrPem = getString(intp, args, 0);
            String caPrivateKeyPem = getString(intp, args, 1);
            String caCertificatePem = getString(intp, args, 2);
            try {
                // Carregar CSR
                byte[] csrBytes = pemToBytes(csrPem);
                PKCS10CertificationRequest csr = new PKCS10CertificationRequest(csrBytes);

                // Carregar CA privada e certificado
                PrivateKey caPrivate = getPrivateKeyFromPem(caPrivateKeyPem);
                X509Certificate caCert = loadCertificateFromPem(caCertificatePem);

                // Extrair o subject (X500Name) diretamente do CSR
                X500Name subject = csr.getSubject();

                // Converter SubjectPublicKeyInfo do CSR para PublicKey nativa do Java
                SubjectPublicKeyInfo pubInfo = csr.getSubjectPublicKeyInfo();
                PublicKey pubKey = new JcaPEMKeyConverter().setProvider("BC").getPublicKey(pubInfo);

                // Construir certificado usando BigInteger, Date, Date
                BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
                Instant now = Instant.now();
                Date startDate = Date.from(now);
                Date endDate = Date.from(now.plus(365, ChronoUnit.DAYS));

                // CORREÇÃO: Usando o construtor que aceita o Certificado da CA e a PublicKey do sujeito
                X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                        caCert, // O próprio certificado do emissor resolve o X500Principal internamente
                        serial,
                        startDate,
                        endDate,
                        subject, // X500Name vindo do CSR
                        pubKey   // PublicKey nativa gerada acima
                );

                String sigAlgo = caPrivate.getAlgorithm().equalsIgnoreCase("RSA") ? "SHA256withRSA" : "SHA256withECDSA";
                ContentSigner signer = new JcaContentSignerBuilder(sigAlgo)
                        .setProvider("BC")
                        .build(caPrivate);

                X509CertificateHolder holder = certBuilder.build(signer);
                X509Certificate cert = new JcaX509CertificateConverter()
                        .setProvider("BC")
                        .getCertificate(holder);

                // Exportar para PEM com quebras de linha padrão
                String base64Cert = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(cert.getEncoded());

                StringWriter sw = new StringWriter();
                sw.write("-----BEGIN CERTIFICATE-----\n");
                sw.write(base64Cert);
                sw.write("\n-----END CERTIFICATE-----\n");
                return sw.toString();
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "csr_sign: " + e.getMessage());
            }
        }));
        // =========================================================
        // 3. PKCS#12 / Keystore
        // =========================================================
        interpreter.globals.defineConst("pkcs12_create", buildFunc(3, (intp, args) -> {
            String certificatePem = getString(intp, args, 0);
            String privateKeyPem = getString(intp, args, 1);
            String password = getString(intp, args, 2);
            try {
                X509Certificate cert = loadCertificateFromPem(certificatePem);
                PrivateKey priv = getPrivateKeyFromPem(privateKeyPem);

                KeyStore ks = KeyStore.getInstance("PKCS12");
                ks.load(null, null);
                ks.setKeyEntry("alias", priv, password.toCharArray(),
                        new java.security.cert.Certificate[]{cert});

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ks.store(baos, password.toCharArray());
                return Base64.getEncoder().encodeToString(baos.toByteArray());
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "pkcs12_create: " + e.getMessage());
            }
        }));

        interpreter.globals.defineConst("pkcs12_extract", buildFunc(2, (intp, args) -> {
            String pkcs12Base64 = getString(intp, args, 0);
            String password = getString(intp, args, 1);
            try {
                byte[] data = Base64.getDecoder().decode(pkcs12Base64);
                KeyStore ks = KeyStore.getInstance("PKCS12");
                ks.load(new ByteArrayInputStream(data), password.toCharArray());

                Enumeration<String> aliases = ks.aliases();
                if (!aliases.hasMoreElements()) {
                    throw new Exception("Keystore vazio");
                }
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

                return Map.of(
                        "certificate", swCert.toString(),
                        "privateKey", swPriv.toString()
                );
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "pkcs12_extract: " + e.getMessage());
            }
        }));

        // =========================================================
        // 4. SSH Ed25519
        // =========================================================
        interpreter.globals.defineConst("ssh_keygen_ed25519", buildFunc(0, (intp, args) -> {
            try {
                KeyPairGenerator gen = KeyPairGenerator.getInstance("Ed25519");
                KeyPair pair = gen.generateKeyPair();

                byte[] rawKey = extractEd25519PublicKey(pair.getPublic());
                String publicKeyOpenSsh = "ssh-ed25519 " + Base64.getEncoder().encodeToString(
                        buildSshEd25519PublicKey(rawKey)
                );

                StringWriter swPriv = new StringWriter();
                swPriv.write("-----BEGIN PRIVATE KEY-----\n");
                swPriv.write(Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
                swPriv.write("\n-----END PRIVATE KEY-----\n");

                return Map.of(
                        "public", publicKeyOpenSsh,
                        "private", swPriv.toString()
                );
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "ssh_keygen_ed25519: " + e.getMessage());
            }
        }));
    }

    // ─── UTILITÁRIOS ──────────────────────────────────────────────────────

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

    private static byte[] pemToBytes(String pem) {
        String cleaned = pem.replaceAll("-----(BEGIN|END) .*?-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(cleaned);
    }

    private static PrivateKey getPrivateKeyFromPem(String pem) throws Exception {
        byte[] bytes = pemToBytes(pem);
        // Tenta RSA, EC, Ed25519
        String[] algos = {"RSA", "EC", "Ed25519"};
        Exception lastException = null;
        for (String algo : algos) {
            try {
                KeyFactory kf = KeyFactory.getInstance(algo);
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
                return kf.generatePrivate(spec);
            } catch (Exception e) {
                lastException = e;
            }
        }
        throw new Exception("Não foi possível interpretar a chave privada: " + lastException.getMessage());
    }

    private static PublicKey getPublicKeyFromPem(String pem) throws Exception {
        byte[] bytes = pemToBytes(pem);
        String[] algos = {"RSA", "EC", "Ed25519"};
        Exception lastException = null;
        for (String algo : algos) {
            try {
                KeyFactory kf = KeyFactory.getInstance(algo);
                X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
                return kf.generatePublic(spec);
            } catch (Exception e) {
                lastException = e;
            }
        }
        throw new Exception("Não foi possível interpretar a chave pública: " + lastException.getMessage());
    }

    private static X509Certificate loadCertificateFromPem(String pem) throws Exception {
        byte[] bytes = pemToBytes(pem);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(bytes));
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

    @SuppressWarnings("unchecked")
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