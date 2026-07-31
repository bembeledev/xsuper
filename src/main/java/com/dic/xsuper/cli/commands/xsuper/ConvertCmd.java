package com.dic.xsuper.cli.commands.xsuper;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConvertCmd implements Command {
    // Jackson Mapper configurado para fazer "Pretty Print" (indentação bonita) do JSON
    private static final ObjectMapper jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public String getName() { return "convert"; }

    @Override
    public String getDescription() { return "Converte formatos de dados. Uso: convert csv2json <input.csv> <output.json>"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 4) {
            System.out.println(ConsoleTheme.WARNING + "⚠️ Uso: convert <tipo> <origem> <destino>\n   Ex: convert csv2json dados.csv dados.json" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        String mode = args[1].toLowerCase();
        Path source = currentDirectory.resolve(args[2]).normalize();
        Path target = currentDirectory.resolve(args[3]).normalize();

        if (!Files.exists(source)) {
            System.out.println(ConsoleTheme.ERROR + "❌ Ficheiro de origem não encontrado." + ConsoleTheme.RESET);
            return currentDirectory;
        }

        if (mode.equals("csv2json")) {
            System.out.println(ConsoleTheme.TEXT + "A iniciar motor de conversão (CSV -> JSON)..." + ConsoleTheme.RESET);
            try {
                convertCsvToJson(source, target);
                System.out.println(ConsoleTheme.SUCCESS + "✅ Conversão concluída: " + target.getFileName() + ConsoleTheme.RESET);
            } catch (Exception e) {
                System.out.println(ConsoleTheme.ERROR + "❌ Falha na conversão estrutural: " + e.getMessage() + ConsoleTheme.RESET);
            }
        } else {
            System.out.println(ConsoleTheme.WARNING + "❌ Modo de conversão não suportado: " + mode + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }

    private void convertCsvToJson(Path sourceCsv, Path targetJson) throws Exception {
        List<Map<String, String>> dataList = new ArrayList<>();

        // Lê o CSV usando o cabeçalho como chaves do JSON
        try (Reader reader = Files.newBufferedReader(sourceCsv);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            for (CSVRecord csvRecord : csvParser) {
                Map<String, String> row = new LinkedHashMap<>();
                for (String header : csvParser.getHeaderNames()) {
                    row.put(header, csvRecord.get(header));
                }
                dataList.add(row);
            }
        }
        // Escreve o ficheiro JSON no disco
        jsonMapper.writeValue(targetJson.toFile(), dataList);
    }
}