package com.dic.xsuper.cli.services;

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

public class DataFormatService {
    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static void convertCsvToJson(Path source, Path target) throws Exception {
        List<Map<String, String>> records = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(source);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim())) {
            for (CSVRecord csvRecord : csvParser) {
                Map<String, String> map = new LinkedHashMap<>();
                for (String header : csvParser.getHeaderNames()) {
                    map.put(header, csvRecord.get(header));
                }
                records.add(map);
            }
        }
        mapper.writeValue(target.toFile(), records);
    }
}
