package com.dic.xsuper.lang.natives.models;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.poo.XplClass;

import org.bouncycastle.crypto.engines.ChaCha7539Engine;
import org.bouncycastle.crypto.engines.ChaChaEngine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.crypto.digests.Blake2sDigest;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SecurityNativeModel {

    // ─── Constantes ──────────────────────────────────────────────────────────
    private static final int PBKDF2_ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;

    // ─── Alfabeto Base58 ──────────────────────────────────────────────────────
    private static final String BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final BigInteger BASE58_BASE = BigInteger.valueOf(58);

    // ─── Registo do Provedor Bouncy Castle ──────────────────────────────────
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Security", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        // ─── 1. HASHES ──────────────────────────────────────────────────────

        // SHA-2 e SHA-3
        defineHash(model, "md5", "MD5");
        defineHash(model, "sha1", "SHA-1");
        defineHash(model, "sha224", "SHA-224");
        defineHash(model, "sha256", "SHA-256");
        defineHash(model, "sha384", "SHA-384");
        defineHash(model, "sha512", "SHA-512");
        defineHash(model, "sha3_224", "SHA3-224");
        defineHash(model, "sha3_256", "SHA3-256");
        defineHash(model, "sha3_384", "SHA3-384");
        defineHash(model, "sha3_512", "SHA3-512");

        // BLAKE2 (Bouncy Castle)
        model.staticFields.put("blake2b", buildAction(1, (intp, args) -> {
            String input = getString(intp, args, 0);
            byte[] data = input.getBytes(StandardCharsets.UTF_8);
            Blake2bDigest digest = new Blake2bDigest(512);
            digest.update(data, 0, data.length);
            byte[] out = new byte[digest.getDigestSize()];
            digest.doFinal(out, 0);
            return toHex(out);
        }));

        model.staticFields.put("blake2s", buildAction(1, (intp, args) -> {
            String input = getString(intp, args, 0);
            byte[] data = input.getBytes(StandardCharsets.UTF_8);
            Blake2sDigest digest = new Blake2sDigest(256);
            digest.update(data, 0, data.length);
            byte[] out = new byte[digest.getDigestSize()];
            digest.doFinal(out, 0);
            return toHex(out);
        }));

        // ─── 2. HMAC ────────────────────────────────────────────────────────

        defineHmac(model, "hmacMd5", "HmacMD5");
        defineHmac(model, "hmacSha256", "HmacSHA256");
        defineHmac(model, "hmacSha512", "HmacSHA512");

        // ─── 3. AES (CBC com IV) ───────────────────────────────────────────

        model.staticFields.put("aesEncrypt", buildAction(3, (intp, args) -> {
            byte[] key = getBytes(intp, args, 0);
            byte[] data = getBytes(intp, args, 1);
            byte[] iv = getBytes(intp, args, 2);
            validateAesParams(key, iv);
            return Base64.getEncoder().encodeToString(aesCipher(true, key, data, iv));
        }));

        model.staticFields.put("aesDecrypt", buildAction(3, (intp, args) -> {
            byte[] key = getBytes(intp, args, 0);
            byte[] encrypted = Base64.getDecoder().decode(getString(intp, args, 1));
            byte[] iv = getBytes(intp, args, 2);
            validateAesParams(key, iv);
            return new String(aesCipher(false, key, encrypted, iv), StandardCharsets.UTF_8);
        }));

        // ─── 4. ChaCha20 ──────────────────────────────────────────────────

        model.staticFields.put("chacha20Encrypt", buildAction(3, (intp, args) -> {
            byte[] key = getBytes(intp, args, 0);
            byte[] data = getBytes(intp, args, 1);
            byte[] nonce = getBytes(intp, args, 2);
            if (key.length != 32) {
                throw new ControlFlow.RuntimeError(null, "chacha20Encrypt: chave deve ter 32 bytes.");
            }
            org.bouncycastle.crypto.StreamCipher engine;
            if (nonce.length == 12) {
                engine = new ChaCha7539Engine();
            } else if (nonce.length == 8) {
                engine = new ChaChaEngine();
            } else {
                throw new ControlFlow.RuntimeError(null, "chacha20Encrypt: nonce deve ter 8 ou 12 bytes.");
            }
            ParametersWithIV params = new ParametersWithIV(new KeyParameter(key), nonce);
            engine.init(true, params);
            byte[] out = new byte[data.length];
            engine.processBytes(data, 0, data.length, out, 0);
            return Base64.getEncoder().encodeToString(out);
        }));

        model.staticFields.put("chacha20Decrypt", buildAction(3, (intp, args) -> {
            byte[] key = getBytes(intp, args, 0);
            byte[] encrypted = Base64.getDecoder().decode(getString(intp, args, 1));
            byte[] nonce = getBytes(intp, args, 2);
            if (key.length != 32) {
                throw new ControlFlow.RuntimeError(null, "chacha20Decrypt: chave deve ter 32 bytes.");
            }
            org.bouncycastle.crypto.StreamCipher engine;
            if (nonce.length == 12) {
                engine = new ChaCha7539Engine();
            } else if (nonce.length == 8) {
                engine = new ChaChaEngine();
            } else {
                throw new ControlFlow.RuntimeError(null, "chacha20Decrypt: nonce deve ter 8 ou 12 bytes.");
            }
            ParametersWithIV params = new ParametersWithIV(new KeyParameter(key), nonce);
            engine.init(false, params);
            byte[] out = new byte[encrypted.length];
            engine.processBytes(encrypted, 0, encrypted.length, out, 0);
            return new String(out, StandardCharsets.UTF_8);
        }));

        // ─── 5. RSA ────────────────────────────────────────────────────────

        model.staticFields.put("rsaGenerate", buildAction(1, (intp, args) -> {
            int keySize = (int) getLong(intp, args, 0, 2048);
            if (keySize < 512) keySize = 512;
            try {
                KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
                gen.initialize(keySize);
                KeyPair pair = gen.generateKeyPair();
                return Map.of(
                        "public", Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()),
                        "private", Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded())
                );
            } catch (NoSuchAlgorithmException e) {
                throw new ControlFlow.RuntimeError(null, "rsaGenerate: " + e.getMessage());
            }
        }));

        model.staticFields.put("rsaEncrypt", buildAction(2, (intp, args) -> {
            String publicKeyPem = getString(intp, args, 0);
            String data = getString(intp, args, 1);
            try {
                byte[] keyBytes = Base64.getDecoder().decode(publicKeyPem);
                X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PublicKey pub = kf.generatePublic(spec);
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.ENCRYPT_MODE, pub);
                byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(encrypted);
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "rsaEncrypt: " + e.getMessage());
            }
        }));

        model.staticFields.put("rsaDecrypt", buildAction(2, (intp, args) -> {
            String privateKeyPem = getString(intp, args, 0);
            String encrypted = getString(intp, args, 1);
            try {
                byte[] keyBytes = Base64.getDecoder().decode(privateKeyPem);
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PrivateKey priv = kf.generatePrivate(spec);
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.DECRYPT_MODE, priv);
                byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encrypted));
                return new String(decrypted, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "rsaDecrypt: " + e.getMessage());
            }
        }));

        model.staticFields.put("rsaSign", buildAction(2, (intp, args) -> {
            String privateKeyPem = getString(intp, args, 0);
            String data = getString(intp, args, 1);
            try {
                byte[] keyBytes = Base64.getDecoder().decode(privateKeyPem);
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PrivateKey priv = kf.generatePrivate(spec);
                Signature sig = Signature.getInstance("SHA256withRSA");
                sig.initSign(priv);
                sig.update(data.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(sig.sign());
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "rsaSign: " + e.getMessage());
            }
        }));

        model.staticFields.put("rsaVerify", buildAction(3, (intp, args) -> {
            String publicKeyPem = getString(intp, args, 0);
            String data = getString(intp, args, 1);
            String signature = getString(intp, args, 2);
            try {
                byte[] keyBytes = Base64.getDecoder().decode(publicKeyPem);
                X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PublicKey pub = kf.generatePublic(spec);
                Signature sig = Signature.getInstance("SHA256withRSA");
                sig.initVerify(pub);
                sig.update(data.getBytes(StandardCharsets.UTF_8));
                return sig.verify(Base64.getDecoder().decode(signature));
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "rsaVerify: " + e.getMessage());
            }
        }));

        // ─── 6. ECDSA ─────────────────────────────────────────────────────

        model.staticFields.put("ecdsaGenerate", buildAction(0, (intp, args) -> {
            try {
                KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
                gen.initialize(new ECGenParameterSpec("secp256r1"));
                KeyPair pair = gen.generateKeyPair();
                return Map.of(
                        "public", Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()),
                        "private", Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded())
                );
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "ecdsaGenerate: " + e.getMessage());
            }
        }));

        model.staticFields.put("ecdsaSign", buildAction(2, (intp, args) -> {
            String privateKeyPem = getString(intp, args, 0);
            String data = getString(intp, args, 1);
            try {
                byte[] keyBytes = Base64.getDecoder().decode(privateKeyPem);
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
                KeyFactory kf = KeyFactory.getInstance("EC");
                PrivateKey priv = kf.generatePrivate(spec);
                Signature sig = Signature.getInstance("SHA256withECDSA");
                sig.initSign(priv);
                sig.update(data.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(sig.sign());
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "ecdsaSign: " + e.getMessage());
            }
        }));

        model.staticFields.put("ecdsaVerify", buildAction(3, (intp, args) -> {
            String publicKeyPem = getString(intp, args, 0);
            String data = getString(intp, args, 1);
            String signature = getString(intp, args, 2);
            try {
                byte[] keyBytes = Base64.getDecoder().decode(publicKeyPem);
                X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                KeyFactory kf = KeyFactory.getInstance("EC");
                PublicKey pub = kf.generatePublic(spec);
                Signature sig = Signature.getInstance("SHA256withECDSA");
                sig.initVerify(pub);
                sig.update(data.getBytes(StandardCharsets.UTF_8));
                return sig.verify(Base64.getDecoder().decode(signature));
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "ecdsaVerify: " + e.getMessage());
            }
        }));

        // ─── 7. KDF ────────────────────────────────────────────────────────

        model.staticFields.put("pbkdf2Sha256", buildAction(3, (intp, args) -> {
            String password = getString(intp, args, 0);
            String salt = getString(intp, args, 1);
            int iterations = (int) getLong(intp, args, 2, PBKDF2_ITERATIONS);
            try {
                PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), iterations, KEY_LENGTH);
                SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                byte[] hash = skf.generateSecret(spec).getEncoded();
                return Base64.getEncoder().encodeToString(hash);
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "pbkdf2Sha256: " + e.getMessage());
            }
        }));

        model.staticFields.put("argon2idHash", buildAction(2, (intp, args) -> {
            String password = getString(intp, args, 0);
            String salt = getString(intp, args, 1);
            if (salt.length() < 8) {
                throw new ControlFlow.RuntimeError(null, "argon2idHash: salt deve ter pelo menos 8 bytes.");
            }
            try {
                int iterations = 3;
                int memory = 65536;
                int parallelism = 1;
                Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                        .withSalt(salt.getBytes(StandardCharsets.UTF_8))
                        .withIterations(iterations)
                        .withMemoryAsKB(memory)
                        .withParallelism(parallelism)
                        .build();
                Argon2BytesGenerator generator = new Argon2BytesGenerator();
                generator.init(params);
                byte[] hash = new byte[32];
                generator.generateBytes(password.toCharArray(), hash);
                return Base64.getEncoder().encodeToString(hash);
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "argon2idHash: " + e.getMessage());
            }
        }));

        model.staticFields.put("argon2idVerify", buildAction(3, (intp, args) -> {
            String password = getString(intp, args, 0);
            String salt = getString(intp, args, 1);
            String storedHash = getString(intp, args, 2);
            String computed = (String) SecurityNativeModel.<Object>invokeStaticMethod(
                    "argon2idHash", intp, List.of(
                            new Expr.CallArg(null, new Expr.Literal(password)),
                            new Expr.CallArg(null, new Expr.Literal(salt))
                    ), model
            );
            return computed.equals(storedHash);
        }));

        // ─── 8. Codificações ──────────────────────────────────────────────

        model.staticFields.put("base64Encode", buildAction(1, (intp, args) -> {
            String input = getString(intp, args, 0);
            return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
        }));

        model.staticFields.put("base64Decode", buildAction(1, (intp, args) -> {
            String input = getString(intp, args, 0);
            return new String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8);
        }));

        model.staticFields.put("base58Encode", buildAction(1, (intp, args) -> {
            String input = getString(intp, args, 0);
            return base58Encode(input.getBytes(StandardCharsets.UTF_8));
        }));

        model.staticFields.put("base58Decode", buildAction(1, (intp, args) -> {
            String input = getString(intp, args, 0);
            return new String(base58Decode(input), StandardCharsets.UTF_8);
        }));

        model.staticFields.put("hexEncode", buildAction(1, (intp, args) -> {
            String input = getString(intp, args, 0);
            return toHex(input.getBytes(StandardCharsets.UTF_8));
        }));

        model.staticFields.put("hexDecode", buildAction(1, (intp, args) -> {
            String hex = getString(intp, args, 0);
            if (hex.length() % 2 != 0) {
                throw new ControlFlow.RuntimeError(null, "hexDecode: string hexadecimal deve ter comprimento par.");
            }
            byte[] bytes = new byte[hex.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }));

        // ─── 9. RNG ────────────────────────────────────────────────────────

        model.staticFields.put("randomBytes", buildAction(1, (intp, args) -> {
            int size = (int) getLong(intp, args, 0, 16);
            if (size <= 0) size = 16;
            byte[] bytes = new byte[size];
            new SecureRandom().nextBytes(bytes);
            return Base64.getEncoder().encodeToString(bytes);
        }));

        model.staticFields.put("randomInt", buildAction(2, (intp, args) -> {
            long min = getLong(intp, args, 0, 0);
            long max = getLong(intp, args, 1, Long.MAX_VALUE);
            if (min > max) {
                throw new ControlFlow.RuntimeError(null, "randomInt: min deve ser <= max.");
            }
            if (max - min > Long.MAX_VALUE - 1) {
                throw new ControlFlow.RuntimeError(null, "randomInt: intervalo muito grande.");
            }
            SecureRandom rng = new SecureRandom();
            long range = max - min + 1;
            long mask = (1L << 63) - 1;
            long rand = rng.nextLong() & mask;
            return min + (rand % range);
        }));

        model.staticFields.put("randomUuid", buildAction(0, (intp, args) -> {
            return UUID.randomUUID().toString();
        }));

        // ─── 10. Sandbox (placeholder) ────────────────────────────────────

        model.staticFields.put("sandboxRun", buildAction(1, (intp, args) -> intp.evaluate(args.getFirst().expression)));

        // ─── 11. Comparação segura ────────────────────────────────────────

        model.staticFields.put("secureCompare", buildAction(2, (intp, args) -> {
            String a = getString(intp, args, 0);
            String b = getString(intp, args, 1);
            return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
        }));

        // ─── REGISTAR ──────────────────────────────────────────────────────

        interpreter.registry_model.put("Security", model);
        interpreter.environment.defineConst("Security", new XplClass(model, interpreter.globals));
    }

    // ─── Métodos auxiliares (copiados do NativeSecurity) ──────────────

    private static void defineHash(XPLModel model, String name, String algorithm) {
        model.staticFields.put(name, buildAction(1, (intp, args) -> {
            String input = getString(intp, args, 0);
            try {
                MessageDigest md = MessageDigest.getInstance(algorithm);
                byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
                return toHex(digest);
            } catch (NoSuchAlgorithmException e) {
                throw new ControlFlow.RuntimeError(null, "Algoritmo de hash não suportado: " + algorithm);
            }
        }));
    }

    private static void defineHmac(XPLModel model, String name, String algorithm) {
        model.staticFields.put(name, buildAction(2, (intp, args) -> {
            String key = getString(intp, args, 0);
            String data = getString(intp, args, 1);
            try {
                Mac mac = Mac.getInstance(algorithm);
                SecretKeySpec spec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm);
                mac.init(spec);
                byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
                return toHex(hmac);
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                throw new ControlFlow.RuntimeError(null, "HMAC " + algorithm + " falhou: " + e.getMessage());
            }
        }));
    }

    private static byte[] aesCipher(boolean encrypt, byte[] key, byte[] data, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec spec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, spec, ivSpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new ControlFlow.RuntimeError(null, "AES falhou: " + e.getMessage());
        }
    }

    private static void validateAesParams(byte[] key, byte[] iv) {
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new ControlFlow.RuntimeError(null, "AES: chave deve ter 16, 24 ou 32 bytes (AES-128/192/256).");
        }
        if (iv.length != 16) {
            throw new ControlFlow.RuntimeError(null, "AES: IV deve ter 16 bytes.");
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // ─── Base58 ──────────────────────────────────────────────────────────────

    private static String base58Encode(byte[] input) {
        if (input.length == 0) return "";
        BigInteger value = new BigInteger(1, input);
        StringBuilder sb = new StringBuilder();
        while (value.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divRem = value.divideAndRemainder(BASE58_BASE);
            sb.append(BASE58_ALPHABET.charAt(divRem[1].intValue()));
            value = divRem[0];
        }
        for (byte b : input) {
            if (b == 0) sb.append(BASE58_ALPHABET.charAt(0));
            else break;
        }
        return sb.reverse().toString();
    }

    private static byte[] base58Decode(String input) {
        if (input.isEmpty()) return new byte[0];
        BigInteger value = BigInteger.ZERO;
        for (char c : input.toCharArray()) {
            int idx = BASE58_ALPHABET.indexOf(c);
            if (idx < 0) throw new IllegalArgumentException("Caractere inválido em Base58: " + c);
            value = value.multiply(BASE58_BASE).add(BigInteger.valueOf(idx));
        }
        byte[] bytes = value.toByteArray();
        int leadingZeros = 0;
        for (char c : input.toCharArray()) {
            if (c == BASE58_ALPHABET.charAt(0)) leadingZeros++;
            else break;
        }
        byte[] result = new byte[leadingZeros + bytes.length];
        System.arraycopy(bytes, 0, result, leadingZeros, bytes.length);
        return result;
    }

    // ─── Extratores de argumentos ──────────────────────────────────────────

    private static String getString(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        return interpreter.stringify(val);
    }

    private static byte[] getBytes(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        if (val instanceof String str) {
            try {
                return Base64.getDecoder().decode(str);
            } catch (IllegalArgumentException e) {
                return str.getBytes(StandardCharsets.UTF_8);
            }
        }
        if (val instanceof byte[]) return (byte[]) val;
        if (val instanceof Number) return val.toString().getBytes(StandardCharsets.UTF_8);
        return interpreter.stringify(val).getBytes(StandardCharsets.UTF_8);
    }

    private static long getLong(Interpreter interpreter, List<Expr.CallArg> args, int index, long fallback) {
        if (index >= args.size()) return fallback;
        Object val = interpreter.evaluate(args.get(index).expression);
        if (val instanceof Number) return ((Number) val).longValue();
        return fallback;
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

    // Auxiliar para invocar métodos estáticos (usado em argon2idVerify)
    @SuppressWarnings("unchecked")
    private static <T> T invokeStaticMethod(String name, Interpreter intp, List<Expr.CallArg> args, XPLModel model) {
        Object method = model.staticFields.get(name);
        if (method instanceof XplCallable) {
            return (T) ((XplCallable) method).call(intp, args);
        }
        throw new ControlFlow.RuntimeError(null, "Método estático não encontrado: " + name);
    }
}