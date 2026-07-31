package com.dic.xsuper.cli.commands.xsuper;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.Console;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Scanner;

public class CryptCmd implements Command {
    @Override
    public String getName() { return "crypt"; }

    @Override
    public String getDescription() { return "Encripta/Desencripta ficheiros com AES. Uso: crypt <lock|unlock> <ficheiro>"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 3 || (!args[1].equals("lock") && !args[1].equals("unlock"))) {
            System.out.println(ConsoleTheme.WARNING + "⚠️ Uso: crypt <lock|unlock> <ficheiro>" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        boolean isEncrypt = args[1].equals("lock");
        Path target = currentDirectory.resolve(args[2]).normalize();

        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            System.out.println(ConsoleTheme.ERROR + "❌ Ficheiro inválido ou não encontrado." + ConsoleTheme.RESET);
            return currentDirectory;
        }

        String password = readPassword();
        if (password == null || password.isEmpty()) {
            System.out.println(ConsoleTheme.ERROR + "❌ Password não pode estar vazia." + ConsoleTheme.RESET);
            return currentDirectory;
        }

        try {
            // Gerar chave AES-256 a partir da password usando SHA-256
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(password.getBytes("UTF-8"));
            keyBytes = Arrays.copyOf(keyBytes, 16); // AES-128 (16 bytes) para máxima compatibilidade no Java standard
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(isEncrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, secretKey);

            byte[] inputBytes = Files.readAllBytes(target);
            byte[] outputBytes = cipher.doFinal(inputBytes);

            // Adiciona ou remove a extensão .aes
            Path outputTarget = isEncrypt
                    ? target.resolveSibling(target.getFileName() + ".aes")
                    : target.resolveSibling(target.getFileName().toString().replace(".aes", ""));

            Files.write(outputTarget, outputBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Opcional: Elimina o ficheiro original inseguro
            Files.delete(target);

            System.out.println(ConsoleTheme.SUCCESS + "✅ Ficheiro " + (isEncrypt ? "encriptado" : "desencriptado") + " com sucesso: " + outputTarget.getFileName() + ConsoleTheme.RESET);

        } catch (javax.crypto.BadPaddingException e) {
            System.out.println(ConsoleTheme.ERROR + "❌ Password incorreta ou ficheiro corrompido!" + ConsoleTheme.RESET);
        } catch (Exception e) {
            System.out.println(ConsoleTheme.ERROR + "❌ Erro de criptografia: " + e.getMessage() + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }

    private String readPassword() {
        Console console = System.console();
        if (console != null) {
            char[] chars = console.readPassword(ConsoleTheme.WARNING + "🔑 Introduz a password: " + ConsoleTheme.RESET);
            return new String(chars);
        } else {
            // Fallback para IDEs que não suportam a classe Console nativa
            System.out.print(ConsoleTheme.WARNING + "🔑 Introduz a password (visível no IDE): " + ConsoleTheme.RESET);
            return new Scanner(System.in).nextLine();
        }
    }
}