package com.dic.xsuper.commands.xsuper;

import com.dic.xsuper.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import java.net.InetAddress;
import java.nio.file.Path;

public class PingCmd implements Command {
    @Override
    public String getName() { return "ping"; }

    @Override
    public String getDescription() { return "Testa a conectividade com um host ou IP. Uso: ping <dominio_ou_ip>"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 2) {
            System.out.println(ConsoleTheme.WARNING + "⚠️ Uso: ping <dominio_ou_ip> (ex: ping google.com)" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        String host = args[1];
        int timeout = 3000; // 3 segundos de limite

        System.out.println(ConsoleTheme.TEXT + "A disparar pacotes para " + host + "..." + ConsoleTheme.RESET);

        try {
            InetAddress inet = InetAddress.getByName(host);
            System.out.println(ConsoleTheme.TEXT + "Resolvido para IP: " + inet.getHostAddress() + ConsoleTheme.RESET);

            for (int i = 1; i <= 4; i++) {
                long startTime = System.nanoTime();
                boolean reachable = inet.isReachable(timeout);
                long endTime = System.nanoTime();

                long pingTime = (endTime - startTime) / 1_000_000;

                if (reachable) {
                    System.out.printf(ConsoleTheme.SUCCESS + "Resposta de %s: tempo=%dms%n" + ConsoleTheme.RESET, inet.getHostAddress(), pingTime);
                } else {
                    System.out.println(ConsoleTheme.ERROR + "Esgotado o tempo limite do pedido." + ConsoleTheme.RESET);
                }
                Thread.sleep(800); // Pausa entre pacotes
            }
        } catch (java.net.UnknownHostException e) {
            System.out.println(ConsoleTheme.ERROR + "❌ Host desconhecido. Verifica a tua ligação à Internet ou o nome do domínio." + ConsoleTheme.RESET);
        } catch (Exception e) {
            System.out.println(ConsoleTheme.ERROR + "❌ Erro de rede: " + e.getMessage() + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }
}