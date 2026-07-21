package com.dic.xsuper.lang.natives.models;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.poo.XplClass;
import com.dic.xsuper.lang.poo.XplInstance;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class DateTimeNativeModel {

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("DateTime", null);
        model.hasBaseImplementation = true;
        Token pubToken = new Token(TokenType.PUBLIC, "pub", null, 0, 0);

        // Campos READONLY - São atualizados automaticamente pelo motor quando fazes contas
        model.addField(new Stmt.FieldDecl(pubToken, false, false, true, new Token(TokenType.IDENTIFIER, "year", null, 0, 0), new TypeNode.Simple(new Token(TokenType.T_INT, "int", null, 0, 0))));
        model.addField(new Stmt.FieldDecl(pubToken, false, false, true, new Token(TokenType.IDENTIFIER, "month", null, 0, 0), new TypeNode.Simple(new Token(TokenType.T_INT, "int", null, 0, 0))));
        model.addField(new Stmt.FieldDecl(pubToken, false, false, true, new Token(TokenType.IDENTIFIER, "day", null, 0, 0), new TypeNode.Simple(new Token(TokenType.T_INT, "int", null, 0, 0))));
        model.addField(new Stmt.FieldDecl(pubToken, false, false, true, new Token(TokenType.IDENTIFIER, "hour", null, 0, 0), new TypeNode.Simple(new Token(TokenType.T_INT, "int", null, 0, 0))));
        model.addField(new Stmt.FieldDecl(pubToken, false, false, true, new Token(TokenType.IDENTIFIER, "minute", null, 0, 0), new TypeNode.Simple(new Token(TokenType.T_INT, "int", null, 0, 0))));
        model.addField(new Stmt.FieldDecl(pubToken, false, false, true, new Token(TokenType.IDENTIFIER, "second", null, 0, 0), new TypeNode.Simple(new Token(TokenType.T_INT, "int", null, 0, 0))));
        model.addField(new Stmt.FieldDecl(pubToken, false, false, true, new Token(TokenType.IDENTIFIER, "timezone", null, 0, 0), new TypeNode.Simple(new Token(TokenType.T_STRING, "string", null, 0, 0))));
        model.addField(new Stmt.FieldDecl(pubToken, false, false, true, new Token(TokenType.IDENTIFIER, "timestamp", null, 0, 0), new TypeNode.Simple(new Token(TokenType.T_INT, "int", null, 0, 0))));

        XplClass dtClass = new XplClass(model, interpreter.globals);

        // =========================================================
        // 1. CONSTRUTORES ESTÁTICOS
        // =========================================================

        // DateTime.now("Europe/Lisbon") ou DateTime.now()
        model.staticFields.put("now", buildCallable(-1, (i, a) -> {
            ZoneId zone = a.isEmpty() ? ZoneId.systemDefault() : ZoneId.of(i.stringify(i.evaluate(a.get(0).expression)));
            return createInstance(dtClass, ZonedDateTime.now(zone), i);
        }));

        // DateTime.fromUnix(1710000000000)
        model.staticFields.put("fromUnix", buildCallable(1, (i, a) -> {
            long ms = ((Number) i.evaluate(a.get(0).expression)).longValue();
            ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault());
            return createInstance(dtClass, zdt, i);
        }));

        // DateTime.parse("2026-12-25 15:30", "yyyy-MM-dd HH:mm")
        model.staticFields.put("parse", buildCallable(2, (i, a) -> {
            String dateStr = i.stringify(i.evaluate(a.get(0).expression));
            String pattern = i.stringify(i.evaluate(a.get(1).expression));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            ZonedDateTime zdt = java.time.LocalDateTime.parse(dateStr, formatter).atZone(ZoneId.systemDefault());
            return createInstance(dtClass, zdt, i);
        }));

        // =========================================================
        // 2. PARSER DE DURAÇÕES MATEMÁTICAS (A tua ideia do 40s * 19m)
        // DateTime.parseDuration("40s", "s") -> Retorna 40
        // DateTime.parseDuration("19m", "s") -> Retorna 1140 (Em segundos!)
        // =========================================================
        model.staticFields.put("parseDuration", buildCallable(-1, (i, a) -> {
            if (a.isEmpty()) throw new ControlFlow.RuntimeError(null, "parseDuration precisa de pelo menos 1 argumento (ex: '40s').");
            String durationStr = i.stringify(i.evaluate(a.get(0).expression)).trim().toLowerCase();

            // Unidade de saída (padrão é "s" = segundos)
            String outputUnit = a.size() > 1 ? i.stringify(i.evaluate(a.get(1).expression)).toLowerCase() : "s";

            double seconds = parseToSeconds(durationStr);

            // Converte os segundos para a unidade desejada
            return switch (outputUnit) {
                case "ms", "millisecond" -> seconds * 1000.0;
                case "m", "minute", "minutes" -> seconds / 60.0;
                case "h", "hour", "hours" -> seconds / 3600.0;
                case "d", "day", "days" -> seconds / 86400.0;
                default -> seconds; // Padrão: "s" ou "second"
            };
        }));

        interpreter.registry_model.put("DateTime", model);
        interpreter.environment.defineConst("DateTime", dtClass);
    }

    // =========================================================================
    // MÁQUINAS INTERNAS DE INSTANCIAÇÃO
    // =========================================================================

    private static XplInstance createInstance(XplClass dtClass, ZonedDateTime zdt, Interpreter i) {
        XplInstance inst = new XplInstance(dtClass);
        inst.fields.put("_zdt", zdt); // O Coração (Variável Nativa Oculta)
        syncFields(inst, zdt);        // Sincroniza os campos públicos (year, month, etc.)

        // --- MÉTODOS DE INSTÂNCIA ---

        // .format("yyyy-MM-dd HH:mm:ss")
        inst.fields.put("format", buildCallable(-1, (intp, args) -> {
            String pattern = args.isEmpty() ? "yyyy-MM-dd HH:mm:ss" : intp.stringify(intp.evaluate(args.get(0).expression));
            ZonedDateTime currentZdt = (ZonedDateTime) inst.fields.get("_zdt");
            return currentZdt.format(DateTimeFormatter.ofPattern(pattern));
        }));

        // .add(10, "days") ou .add(1, "months")
        inst.fields.put("add", buildCallable(2, (intp, args) -> {
            long amount = ((Number) intp.evaluate(args.get(0).expression)).longValue();
            String unit = intp.stringify(intp.evaluate(args.get(1).expression)).toLowerCase();
            ZonedDateTime currentZdt = (ZonedDateTime) inst.fields.get("_zdt");

            ZonedDateTime newZdt = currentZdt.plus(amount, mapUnit(unit));
            inst.fields.put("_zdt", newZdt);
            syncFields(inst, newZdt);
            return inst; // Retorna a própria instância para encadeamento (chaining)
        }));

        // .subtract(5, "hours")
        inst.fields.put("subtract", buildCallable(2, (intp, args) -> {
            long amount = ((Number) intp.evaluate(args.get(0).expression)).longValue();
            String unit = intp.stringify(intp.evaluate(args.get(1).expression)).toLowerCase();
            ZonedDateTime currentZdt = (ZonedDateTime) inst.fields.get("_zdt");

            ZonedDateTime newZdt = currentZdt.minus(amount, mapUnit(unit));
            inst.fields.put("_zdt", newZdt);
            syncFields(inst, newZdt);
            return inst;
        }));

        // .diff(outraData, "days") -> Diferença entre duas datas
        inst.fields.put("diff", buildCallable(2, (intp, args) -> {
            Object otherObj = intp.evaluate(args.get(0).expression);
            String unitStr = intp.stringify(intp.evaluate(args.get(1).expression)).toLowerCase();

            if (!(otherObj instanceof XplInstance otherInst) || !otherInst.fields.containsKey("_zdt")) {
                throw new ControlFlow.RuntimeError(null, "O primeiro argumento de diff() deve ser outra instância DateTime.");
            }

            ZonedDateTime thisZdt = (ZonedDateTime) inst.fields.get("_zdt");
            ZonedDateTime otherZdt = (ZonedDateTime) otherInst.fields.get("_zdt");
            ChronoUnit unit = mapUnit(unitStr);

            return unit.between(thisZdt, otherZdt);
        }));

        // .toTimezone("America/New_York")
        inst.fields.put("toTimezone", buildCallable(1, (intp, args) -> {
            String zoneName = intp.stringify(intp.evaluate(args.get(0).expression));
            ZonedDateTime currentZdt = (ZonedDateTime) inst.fields.get("_zdt");

            ZonedDateTime newZdt = currentZdt.withZoneSameInstant(ZoneId.of(zoneName));
            inst.fields.put("_zdt", newZdt);
            syncFields(inst, newZdt);
            return inst;
        }));

        return inst;
    }

    // Sincroniza o objeto oculto do Java com os campos públicos do XPL
    private static void syncFields(XplInstance inst, ZonedDateTime zdt) {
        inst.fields.put("year", (long) zdt.getYear());
        inst.fields.put("month", (long) zdt.getMonthValue());
        inst.fields.put("day", (long) zdt.getDayOfMonth());
        inst.fields.put("hour", (long) zdt.getHour());
        inst.fields.put("minute", (long) zdt.getMinute());
        inst.fields.put("second", (long) zdt.getSecond());
        inst.fields.put("timezone", zdt.getZone().getId());
        inst.fields.put("timestamp", zdt.toInstant().toEpochMilli());
    }

    // Mapeador de Unidades de Tempo
    private static ChronoUnit mapUnit(String unit) {
        return switch (unit) {
            case "y", "year", "years" -> ChronoUnit.YEARS;
            case "M", "month", "months" -> ChronoUnit.MONTHS;
            case "w", "week", "weeks" -> ChronoUnit.WEEKS;
            case "d", "day", "days" -> ChronoUnit.DAYS;
            case "h", "hour", "hours" -> ChronoUnit.HOURS;
            case "m", "minute", "minutes" -> ChronoUnit.MINUTES;
            case "s", "second", "seconds" -> ChronoUnit.SECONDS;
            case "ms", "millisecond", "milliseconds" -> ChronoUnit.MILLIS;
            default -> throw new ControlFlow.RuntimeError(null, "Unidade de tempo desconhecida: " + unit);
        };
    }

    // O Motor Híbrido de Parsing para a tua matemática de "40s * 19m"
    private static double parseToSeconds(String duration) {
        try {
            double value = Double.parseDouble(duration.replaceAll("[^0-9.]", ""));
            if (duration.endsWith("ms")) return value / 1000.0;
            if (duration.endsWith("s")) return value;
            if (duration.endsWith("m")) return value * 60.0;
            if (duration.endsWith("h")) return value * 3600.0;
            if (duration.endsWith("d")) return value * 86400.0;
            return value; // Assume segundos por padrão se não tiver sufixo
        } catch (Exception e) {
            throw new ControlFlow.RuntimeError(null, "Falha ao analisar a duração: " + duration);
        }
    }

    // Builder Rápido de Funções
    private interface NativeAction { Object execute(Interpreter i, List<Expr.CallArg> a); }
    private static XplCallable buildCallable(int arity, NativeAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) { return action.execute(intp, args); }
        };
    }
}