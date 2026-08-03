package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.exceptions.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.execution.XplFunction;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;
import com.dic.xsuper.dom.node.XplNativeObject;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.Base64;

public class DatabaseNativeModel {

    // Mapa de drivers por prefixo da URL
    private static final Map<String, String> DRIVER_MAP = new HashMap<>();

    static {
        DRIVER_MAP.put("jdbc:postgresql:", "org.postgresql.Driver");
        DRIVER_MAP.put("jdbc:mysql:", "com.mysql.cj.jdbc.Driver");
        DRIVER_MAP.put("jdbc:mariadb:", "org.mariadb.jdbc.Driver");
        DRIVER_MAP.put("jdbc:sqlite:", "org.sqlite.JDBC");
        DRIVER_MAP.put("jdbc:h2:", "org.h2.Driver");
        DRIVER_MAP.put("jdbc:oracle:", "oracle.jdbc.OracleDriver");
        DRIVER_MAP.put("jdbc:derby:", "org.apache.derby.jdbc.EmbeddedDriver");
        DRIVER_MAP.put("jdbc:jtds:", "net.sourceforge.jtds.jdbc.Driver");
        DRIVER_MAP.put("jdbc:microsoft:", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Database", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        // ─── Database.connect(url, [user, password]) ──────────────────────
        model.staticFields.put("connect", buildAction(-1, (intp, args) -> {
            List<Object> evaluated = Interpreter.unpackNativeArgs(intp, args);
            if (evaluated.isEmpty()) {
                throw new ControlFlow.RuntimeError(null, "Database.connect: precisa pelo menos da URL.");
            }
            String url = intp.stringify(evaluated.get(0));
            String user = evaluated.size() > 1 ? intp.stringify(evaluated.get(1)) : null;
            String password = evaluated.size() > 2 ? intp.stringify(evaluated.get(2)) : null;

            loadDriverForUrl(url);

            try {
                Connection conn;
                if (user != null && password != null) {
                    conn = DriverManager.getConnection(url, user, password);
                } else {
                    conn = DriverManager.getConnection(url);
                }
                conn.setAutoCommit(true);
                return new DatabaseConnection(conn);
            } catch (SQLException e) {
                throw new ControlFlow.RuntimeError(null, "Falha na ligação à base de dados: " + e.getMessage());
            }
        }));

        // ─── Database.driver(class) ──────────────────────────────────────
        model.staticFields.put("driver", buildAction(1, (intp, args) -> {
            String driverClass = getString(intp, args, 0);
            try {
                Class.forName(driverClass);
                return true;
            } catch (ClassNotFoundException e) {
                throw new ControlFlow.RuntimeError(null, "Driver não encontrado: " + driverClass);
            }
        }));

        // ─── REGISTAR ──────────────────────────────────────────────────────

        interpreter.registry_model.put("Database", model);
        interpreter.environment.defineConst("Database", new XplClass(model, interpreter.globals));
    }

    // ─── CLASSE INTERNA: DatabaseConnection ──────────────────────────────────

    private static class DatabaseConnection implements XplNativeObject {
        private final Connection conn;
        private boolean closed = false;

        public DatabaseConnection(Connection conn) {
            this.conn = conn;
        }

        @Override
        public void invokeMethod() {}

        @Override
        public Object invokeMethod(String methodName, List<Object> args, Interpreter interpreter) {
            if (closed) {
                throw new ControlFlow.RuntimeError(null, "Ligação à base de dados já fechada.");
            }
            try {
                switch (methodName) {
                    case "query": {
                        String sql = (String) args.get(0);
                        List<Object> params = args.size() > 1 ? (List<Object>) args.get(1) : Collections.emptyList();
                        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                            setParameters(stmt, params);
                            try (ResultSet rs = stmt.executeQuery()) {
                                return resultSetToList(rs);
                            }
                        }
                    }
                    case "queryOne": {
                        String sql = (String) args.get(0);
                        List<Object> params = args.size() > 1 ? (List<Object>) args.get(1) : Collections.emptyList();
                        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                            setParameters(stmt, params);
                            try (ResultSet rs = stmt.executeQuery()) {
                                List<Map<String, Object>> rows = resultSetToList(rs);
                                return rows.isEmpty() ? null : rows.getFirst();
                            }
                        }
                    }
                    case "execute": {
                        String sql = (String) args.get(0);
                        List<Object> params = args.size() > 1 ? (List<Object>) args.get(1) : Collections.emptyList();
                        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                            setParameters(stmt, params);
                            int rows = stmt.executeUpdate();
                            return (long) rows;
                        }
                    }
                    case "transaction": {
                        Object funcObj = args.getFirst();
                        if (!(funcObj instanceof XplFunction func)) {
                            throw new ControlFlow.RuntimeError(null, "transaction espera uma função () => { ... }");
                        }
                        boolean originalAutoCommit = conn.getAutoCommit();
                        try {
                            conn.setAutoCommit(false);
                            func.call(interpreter, Collections.emptyList());
                            conn.commit();
                            return true;
                        } catch (Exception e) {
                            conn.rollback();
                            throw e;
                        } finally {
                            conn.setAutoCommit(originalAutoCommit);
                        }
                    }
                    case "begin": {
                        conn.setAutoCommit(false);
                        return null;
                    }
                    case "commit": {
                        conn.commit();
                        conn.setAutoCommit(true);
                        return null;
                    }
                    case "rollback": {
                        conn.rollback();
                        conn.setAutoCommit(true);
                        return null;
                    }
                    case "close": {
                        close();
                        return null;
                    }
                    case "prepare": {
                        String sql = (String) args.get(0);
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        return new PreparedStatementWrapper(stmt);
                    }
                    default:
                        throw new ControlFlow.RuntimeError(null, "Método '" + methodName + "' não suportado para ligação.");
                }
            } catch (SQLException e) {
                throw new ControlFlow.RuntimeError(null, "Erro de SQL: " + e.getMessage());
            }
        }

        @Override
        public Object getProperty(String propertyName) {
            try {
                return switch (propertyName) {
                    case "closed" -> closed;
                    case "autoCommit" -> conn.getAutoCommit();
                    case "url" -> conn.getMetaData().getURL();
                    case "catalog" -> conn.getCatalog();

                    // ⭐ DELEGAÇÃO DE MÉTODOS PARA O MOTOR XPL ⭐
                    case "query", "queryOne", "execute", "transaction", "begin", "commit", "rollback", "close",
                         "prepare" -> new XplCallable() {
                        @Override
                        public int arity() {
                            return -1;
                        }

                        @Override
                        public Object call(Interpreter intp, List<Expr.CallArg> args) {
                            List<Object> evalArgs = Interpreter.unpackNativeArgs(intp, args);
                            return invokeMethod(propertyName, evalArgs, intp);
                        }
                    };
                    default -> null;
                };
            } catch (SQLException e) {
                return null;
            }
        }

        private void close() throws SQLException {
            if (!closed) {
                conn.close();
                closed = true;
            }
        }

        // ─── AUXILIARES ──────────────────────────────────────────────────────

        private void setParameters(PreparedStatement stmt, List<Object> params) throws SQLException {
            for (int i = 0; i < params.size(); i++) {
                Object val = params.get(i);
                switch (val) {
                    case null -> stmt.setNull(i + 1, Types.NULL);
                    case String string -> stmt.setString(i + 1, string);
                    case Number number -> stmt.setObject(i + 1, val);
                    case Boolean b -> stmt.setBoolean(i + 1, b);
                    case Date date -> stmt.setDate(i + 1, date);
                    case LocalDate localDate -> stmt.setDate(i + 1, Date.valueOf(localDate));
                    case LocalDateTime localDateTime -> stmt.setTimestamp(i + 1, Timestamp.valueOf(localDateTime));
                    case LocalTime localTime -> stmt.setTime(i + 1, Time.valueOf(localTime));
                    case java.util.Date date -> stmt.setDate(i + 1, new Date(date.getTime()));
                    case byte[] bytes -> stmt.setBytes(i + 1, bytes);
                    default -> stmt.setObject(i + 1, val);
                }
            }
        }

        private List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
            List<Map<String, Object>> results = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = meta.getColumnLabel(i);
                    if (columnName == null || columnName.isEmpty()) {
                        columnName = meta.getColumnName(i);
                    }
                    Object value = rs.getObject(i);
                    if (value instanceof Blob blob) {
                        value = Base64.getEncoder().encodeToString(blob.getBytes(1, (int) blob.length()));
                    } else if (value instanceof Clob clob) {
                        value = clob.getSubString(1, (int) clob.length());
                    } else if (value instanceof Timestamp) {
                        value = ((Timestamp) value).toLocalDateTime();
                    } else if (value instanceof Date) {
                        value = ((Date) value).toLocalDate();
                    } else if (value instanceof Time) {
                        value = ((Time) value).toLocalTime();
                    }
                    row.put(columnName, value);
                }
                results.add(row);
            }
            return results;
        }
    }

    // ─── CLASS: PreparedStatementWrapper ─────────────────────────────────────

    private static class PreparedStatementWrapper implements XplNativeObject {
        private final PreparedStatement stmt;
        private boolean closed = false;

        public PreparedStatementWrapper(PreparedStatement stmt) {
            this.stmt = stmt;
        }

        @Override
        public void invokeMethod() {}

        @Override
        public Object invokeMethod(String methodName, List<Object> args, Interpreter interpreter) {
            if (closed) throw new ControlFlow.RuntimeError(null, "Statement já fechado.");
            try {
                switch (methodName) {
                    case "set":
                        int index = ((Number) args.get(0)).intValue();
                        Object value = args.get(1);
                        setParameter(index, value);
                        return null;
                    case "setArray":
                        index = ((Number) args.get(0)).intValue();
                        List<?> array = (List<?>) args.get(1);
                        stmt.setArray(index, stmt.getConnection().createArrayOf("TEXT", array.toArray()));
                        return null;
                    case "executeQuery":
                        return executeQuery();
                    case "executeUpdate":
                        return (long) stmt.executeUpdate();
                    case "close":
                        close();
                        return null;
                    default:
                        throw new ControlFlow.RuntimeError(null, "Método '" + methodName + "' não suportado no PreparedStatement.");
                }
            } catch (SQLException e) {
                throw new ControlFlow.RuntimeError(null, "Erro de SQL: " + e.getMessage());
            }
        }

        @Override
        public Object getProperty(String propertyName) {
            return null;
        }

        private void setParameter(int index, Object value) throws SQLException {
            if (value == null) {
                stmt.setNull(index, Types.NULL);
            } else if (value instanceof String) {
                stmt.setString(index, (String) value);
            } else if (value instanceof Number) {
                stmt.setObject(index, value);
            } else if (value instanceof Boolean) {
                stmt.setBoolean(index, (Boolean) value);
            } else if (value instanceof Date) {
                stmt.setDate(index, (Date) value);
            } else if (value instanceof LocalDate) {
                stmt.setDate(index, java.sql.Date.valueOf((LocalDate) value));
            } else if (value instanceof LocalDateTime) {
                stmt.setTimestamp(index, java.sql.Timestamp.valueOf((LocalDateTime) value));
            } else if (value instanceof byte[]) {
                stmt.setBytes(index, (byte[]) value);
            } else {
                stmt.setObject(index, value);
            }
        }

        private Object executeQuery() throws SQLException {
            try (ResultSet rs = stmt.executeQuery()) {
                List<Map<String, Object>> results = new ArrayList<>();
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = meta.getColumnLabel(i);
                        if (columnName == null || columnName.isEmpty()) {
                            columnName = meta.getColumnName(i);
                        }
                        Object value = rs.getObject(i);
                        if (value instanceof Blob blob) {
                            value = Base64.getEncoder().encodeToString(blob.getBytes(1, (int) blob.length()));
                        } else if (value instanceof Clob clob) {
                            value = clob.getSubString(1, (int) clob.length());
                        } else if (value instanceof Timestamp) {
                            value = ((Timestamp) value).toLocalDateTime();
                        } else if (value instanceof Date) {
                            value = ((Date) value).toLocalDate();
                        } else if (value instanceof Time) {
                            value = ((Time) value).toLocalTime();
                        }
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
                return results;
            }
        }

        private void close() throws SQLException {
            if (!closed) {
                stmt.close();
                closed = true;
            }
        }
    }

    // ─── AUXILIARES ──────────────────────────────────────────────────────────

    private static void loadDriverForUrl(String url) {
        for (Map.Entry<String, String> entry : DRIVER_MAP.entrySet()) {
            if (url.startsWith(entry.getKey())) {
                try {
                    Class.forName(entry.getValue());
                    return;
                } catch (ClassNotFoundException e) {
                    // Continua a tentar outros
                }
            }
        }
        // Se não encontrou, tenta carregar um driver genérico (pode falhar)
        try {
            DriverManager.getDriver(url);
        } catch (SQLException e) {
            // Não há driver registado; o utilizador terá de chamar Database.driver()
        }
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