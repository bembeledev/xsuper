package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.ControlFlow;
import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;
import com.dic.xsuper.lang.ui.document.XplNativeObject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Módulo Nativo para MongoDB via driver síncrono.
 * Suporta conexão, CRUD, agregação, índices e contagem.
 * <p>
 * Uso:
 *   var conn = mongo_connect("mongodb://localhost:27017");
 *   var db = conn.getDatabase("meu_banco");
 *   var coll = db.getCollection("users");
 *   coll.insertOne({ "name": "Alice", "age": 30 });
 *   var users = coll.find({ "age": { "$gt": 18 } });
 *   for (u in users) println(u.name);
 *   conn.close();
 */
public class NativeMongoDB {

    // Mapeamento de opções comuns para filtros/consultas
    private static final Map<String, String> OPERATOR_MAP = new HashMap<>();
    static {
        OPERATOR_MAP.put("$gt", "$gt");
        OPERATOR_MAP.put("$gte", "$gte");
        OPERATOR_MAP.put("$lt", "$lt");
        OPERATOR_MAP.put("$lte", "$lte");
        OPERATOR_MAP.put("$ne", "$ne");
        OPERATOR_MAP.put("$eq", "$eq");
        OPERATOR_MAP.put("$in", "$in");
        OPERATOR_MAP.put("$nin", "$nin");
        OPERATOR_MAP.put("$and", "$and");
        OPERATOR_MAP.put("$or", "$or");
        OPERATOR_MAP.put("$not", "$not");
        OPERATOR_MAP.put("$exists", "$exists");
        OPERATOR_MAP.put("$type", "$type");
        OPERATOR_MAP.put("$regex", "$regex");
        OPERATOR_MAP.put("$options", "$options");
        OPERATOR_MAP.put("$text", "$text");
        OPERATOR_MAP.put("$where", "$where");
    }

    public static void register(Interpreter interpreter) {

        // ─── mongo_connect(uri) ──────────────────────────────────────────────
        interpreter.globals.defineConst("mongo_connect", buildFunc(1, (intp, args) -> {
            String uri = getString(intp, args, 0);
            try {
                MongoClientSettings settings = MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(uri))
                        .build();
                MongoClient client = MongoClients.create(settings);
                return new MongoConnection(client);
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null,
                        "Falha na ligação ao MongoDB: " + e.getMessage());
            }
        }));

        // ─── mongo_connect_default() ──────────────────────────────────────────
        interpreter.globals.defineConst("mongo_connect_default", buildFunc(0, (intp, args) -> {
            try {
                MongoClient client = MongoClients.create();
                return new MongoConnection(client);
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null,
                        "Falha na ligação ao MongoDB (padrão): " + e.getMessage());
            }
        }));
    }

    // ─── CLASSE: MongoConnection ──────────────────────────────────────────────

    private static class MongoConnection implements XplNativeObject {
        private final MongoClient client;
        private boolean closed = false;

        public MongoConnection(MongoClient client) {
            this.client = client;
        }

        @Override
        public void invokeMethod() {

        }

        @Override
        public Object invokeMethod(String methodName, List<Object> args, Interpreter interpreter) {
            if (closed) {
                throw new ControlFlow.RuntimeError(null, "Ligação ao MongoDB já fechada.");
            }
            switch (methodName) {
                case "getDatabase": {
                    String dbName = (String) args.get(0);
                    MongoDatabase db = client.getDatabase(dbName);
                    return new MongoDatabaseWrapper(db);
                }
                case "listDatabaseNames": {
                    return client.listDatabaseNames().into(new ArrayList<>());
                }
                case "close": {
                    try { client.close(); } catch (Exception ignored) {}
                    closed = true;
                    return null;
                }
                default:
                    throw new ControlFlow.RuntimeError(null,
                            "Método '" + methodName + "' não suportado na ligação.");
            }
        }

        @Override
        public Object getProperty(String propertyName) {
            if (propertyName.equals("closed")) {
                return closed;
            }
            return null;
        }
    }

    // ─── CLASSE: MongoDatabaseWrapper ─────────────────────────────────────────

    private static class MongoDatabaseWrapper implements XplNativeObject {
        private final MongoDatabase db;

        public MongoDatabaseWrapper(MongoDatabase db) {
            this.db = db;
        }

        @Override
        public void invokeMethod() {

        }

        @Override
        public Object invokeMethod(String methodName, List<Object> args, Interpreter interpreter) {
            switch (methodName) {
                case "getCollection": {
                    String collName = (String) args.get(0);
                    MongoCollection<Document> coll = db.getCollection(collName);
                    return new MongoCollectionWrapper(coll);
                }
                case "listCollectionNames": {
                    return db.listCollectionNames().into(new ArrayList<>());
                }
                case "createCollection": {
                    String collName = (String) args.get(0);
                    db.createCollection(collName);
                    return null;
                }
                case "drop": {
                    db.drop();
                    return null;
                }
                default:
                    throw new ControlFlow.RuntimeError(null,
                            "Método '" + methodName + "' não suportado na base de dados.");
            }
        }

        @Override
        public Object getProperty(String propertyName) {
            if (propertyName.equals("name")) {
                return db.getName();
            }
            return null;
        }
    }

    // ─── CLASSE: MongoCollectionWrapper ───────────────────────────────────────

    private static class MongoCollectionWrapper implements XplNativeObject {
        private final MongoCollection<Document> coll;

        public MongoCollectionWrapper(MongoCollection<Document> coll) {
            this.coll = coll;
        }

        @Override
        public void invokeMethod() {

        }

        @Override
        public Object invokeMethod(String methodName, List<Object> args, Interpreter interpreter) {
            try {
                switch (methodName) {
                    case "insertOne": {
                        Map<String, Object> docMap = (Map<String, Object>) args.get(0);
                        Document doc = mapToDocument(docMap);
                        InsertOneResult result = coll.insertOne(doc);
                        return Map.of("insertedId", result.getInsertedId());
                    }
                    case "insertMany": {
                        List<Map<String, Object>> docsList = (List<Map<String, Object>>) args.get(0);
                        List<Document> docs = docsList.stream()
                                .map(NativeMongoDB::mapToDocument)
                                .collect(Collectors.toList());
                        InsertManyResult result = coll.insertMany(docs);
                        return result.getInsertedIds().entrySet().stream()
                                .collect(Collectors.toMap(e -> (long) e.getKey(), e -> e.getValue()));
                    }
                    case "find": {
                        Map<String, Object> filterMap = args.size() > 0 ? (Map<String, Object>) args.get(0) : new HashMap<>();
                        Map<String, Object> optionsMap = args.size() > 1 ? (Map<String, Object>) args.get(1) : new HashMap<>();
                        Bson filter = mapToBson(filterMap);
                        FindIterable<Document> iterable = coll.find(filter);
                        if (optionsMap.containsKey("limit")) {
                            int limit = ((Number) optionsMap.get("limit")).intValue();
                            iterable = iterable.limit(limit);
                        }
                        if (optionsMap.containsKey("skip")) {
                            int skip = ((Number) optionsMap.get("skip")).intValue();
                            iterable = iterable.skip(skip);
                        }
                        if (optionsMap.containsKey("sort")) {
                            Map<String, Object> sortMap = (Map<String, Object>) optionsMap.get("sort");
                            iterable = iterable.sort(mapToBson(sortMap));
                        }
                        if (optionsMap.containsKey("projection")) {
                            Map<String, Object> projMap = (Map<String, Object>) optionsMap.get("projection");
                            iterable = iterable.projection(mapToBson(projMap));
                        }
                        return new MongoCursorWrapper(iterable.iterator());
                    }
                    case "findOne": {
                        Map<String, Object> filterMap = args.size() > 0 ? (Map<String, Object>) args.get(0) : new HashMap<>();
                        Map<String, Object> optionsMap = args.size() > 1 ? (Map<String, Object>) args.get(1) : new HashMap<>();
                        Bson filter = mapToBson(filterMap);
                        FindIterable<Document> iterable = coll.find(filter).limit(1);
                        if (optionsMap.containsKey("sort")) {
                            Map<String, Object> sortMap = (Map<String, Object>) optionsMap.get("sort");
                            iterable = iterable.sort(mapToBson(sortMap));
                        }
                        if (optionsMap.containsKey("projection")) {
                            Map<String, Object> projMap = (Map<String, Object>) optionsMap.get("projection");
                            iterable = iterable.projection(mapToBson(projMap));
                        }
                        Document doc = iterable.first();
                        return doc != null ? documentToMap(doc) : null;
                    }
                    case "updateOne": {
                        Map<String, Object> filterMap = (Map<String, Object>) args.get(0);
                        Map<String, Object> updateMap = (Map<String, Object>) args.get(1);
                        Map<String, Object> optionsMap = args.size() > 2 ? (Map<String, Object>) args.get(2) : new HashMap<>();
                        Bson filter = mapToBson(filterMap);
                        Bson update = mapToBson(updateMap);
                        UpdateOptions opts = new UpdateOptions();
                        if (optionsMap.containsKey("upsert") && (boolean) optionsMap.get("upsert")) {
                            opts.upsert(true);
                        }
                        UpdateResult result = coll.updateOne(filter, update, opts);
                        return Map.of(
                                "matchedCount", result.getMatchedCount(),
                                "modifiedCount", result.getModifiedCount(),
                                "upsertedId", result.getUpsertedId()
                        );
                    }
                    case "updateMany": {
                        Map<String, Object> filterMap = (Map<String, Object>) args.get(0);
                        Map<String, Object> updateMap = (Map<String, Object>) args.get(1);
                        Map<String, Object> optionsMap = args.size() > 2 ? (Map<String, Object>) args.get(2) : new HashMap<>();
                        Bson filter = mapToBson(filterMap);
                        Bson update = mapToBson(updateMap);
                        UpdateOptions opts = new UpdateOptions();
                        if (optionsMap.containsKey("upsert") && (boolean) optionsMap.get("upsert")) {
                            opts.upsert(true);
                        }
                        UpdateResult result = coll.updateMany(filter, update, opts);
                        return Map.of(
                                "matchedCount", result.getMatchedCount(),
                                "modifiedCount", result.getModifiedCount(),
                                "upsertedId", result.getUpsertedId()
                        );
                    }
                    case "deleteOne": {
                        Map<String, Object> filterMap = (Map<String, Object>) args.get(0);
                        Bson filter = mapToBson(filterMap);
                        DeleteResult result = coll.deleteOne(filter);
                        return result.getDeletedCount();
                    }
                    case "deleteMany": {
                        Map<String, Object> filterMap = (Map<String, Object>) args.get(0);
                        Bson filter = mapToBson(filterMap);
                        DeleteResult result = coll.deleteMany(filter);
                        return result.getDeletedCount();
                    }
                    case "countDocuments": {
                        Map<String, Object> filterMap = args.size() > 0 ? (Map<String, Object>) args.get(0) : new HashMap<>();
                        Bson filter = mapToBson(filterMap);
                        long count = coll.countDocuments(filter);
                        return count;
                    }
                    case "aggregate": {
                        List<Map<String, Object>> pipelineList = (List<Map<String, Object>>) args.get(0);
                        List<Bson> pipeline = pipelineList.stream()
                                .map(NativeMongoDB::mapToBson)
                                .collect(Collectors.toList());
                        AggregateIterable<Document> iterable = coll.aggregate(pipeline);
                        return new MongoCursorWrapper(iterable.iterator());
                    }
                    case "createIndex": {
                        Map<String, Object> keysMap = (Map<String, Object>) args.get(0);
                        Bson keys = mapToBson(keysMap);
                        String indexName = coll.createIndex(keys);
                        return indexName;
                    }
                    case "createIndexes": {
                        List<Map<String, Object>> indexList = (List<Map<String, Object>>) args.get(0);
                        List<IndexModel> models = new ArrayList<>();
                        for (Map<String, Object> idx : indexList) {
                            Bson keys = mapToBson((Map<String, Object>) idx.get("keys"));
                            IndexOptions opts = new IndexOptions();
                            if (idx.containsKey("unique") && (boolean) idx.get("unique")) {
                                opts.unique(true);
                            }
                            if (idx.containsKey("name")) {
                                opts.name((String) idx.get("name"));
                            }
                            models.add(new IndexModel(keys, opts));
                        }
                        coll.createIndexes(models);
                        return null;
                    }
                    case "drop": {
                        coll.drop();
                        return null;
                    }
                    case "dropIndex": {
                        String indexName = (String) args.get(0);
                        coll.dropIndex(indexName);
                        return null;
                    }
                    case "listIndexes": {
                        List<Map<String, Object>> indexes = new ArrayList<>();
                        coll.listIndexes().forEach(doc -> {
                            Map<String, Object> idxMap = new LinkedHashMap<>();
                            doc.forEach((k, v) -> idxMap.put(k, v));
                            indexes.add(idxMap);
                        });
                        return indexes;
                    }
                    default:
                        throw new ControlFlow.RuntimeError(null,
                                "Método '" + methodName + "' não suportado na coleção.");
                }
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "Erro no MongoDB: " + e.getMessage());
            }
        }

        @Override
        public Object getProperty(String propertyName) {
            switch (propertyName) {
                case "name": return coll.getNamespace().getCollectionName();
                default: return null;
            }
        }
    }

    // ─── CLASSE: MongoCursorWrapper ───────────────────────────────────────────

    private static class MongoCursorWrapper implements XplNativeObject {
        private final MongoCursor<Document> cursor;
        private boolean closed = false;

        public MongoCursorWrapper(MongoCursor<Document> cursor) {
            this.cursor = cursor;
        }

        @Override
        public void invokeMethod() {

        }

        @Override
        public Object invokeMethod(String methodName, List<Object> args, Interpreter interpreter) {
            if (closed) throw new ControlFlow.RuntimeError(null, "Cursor já fechado.");
            switch (methodName) {
                case "next": {
                    if (cursor.hasNext()) {
                        Document doc = cursor.next();
                        return documentToMap(doc);
                    }
                    return null;
                }
                case "hasNext": {
                    return cursor.hasNext();
                }
                case "toArray": {
                    List<Map<String, Object>> results = new ArrayList<>();
                    cursor.forEachRemaining(doc -> results.add(documentToMap(doc)));
                    return results;
                }
                case "close": {
                    cursor.close();
                    closed = true;
                    return null;
                }
                default:
                    throw new ControlFlow.RuntimeError(null,
                            "Método '" + methodName + "' não suportado no cursor.");
            }
        }

        @Override
        public Object getProperty(String propertyName) {
            return switch (propertyName) {
                case "closed" -> closed;
                case "hasNext" -> cursor.hasNext();
                default -> null;
            };
        }
    }

    // ─── CONVERSORES ──────────────────────────────────────────────────────────

    private static Document mapToDocument(Map<String, Object> map) {
        Document doc = new Document();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            Object value = convertValue(e.getValue());
            doc.append(e.getKey(), value);
        }
        return doc;
    }

    @SuppressWarnings("unchecked")
    private static Object convertValue(Object value) {
        if (value == null) return null;
        if (value instanceof Map) {
            return mapToDocument((Map<String, Object>) value);
        }
        if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            return list.stream().map(NativeMongoDB::convertValue).collect(Collectors.toList());
        }
        return value;
    }

    private static Bson mapToBson(Map<String, Object> map) {
        return mapToDocument(map);
    }

    private static Map<String, Object> documentToMap(Document doc) {
        Map<String, Object> result = new LinkedHashMap<>();
        doc.forEach((k, v) -> {
            if (v instanceof Document) {
                result.put(k, documentToMap((Document) v));
            } else if (v instanceof List) {
                List<Object> list = (List<Object>) v;
                List<Object> convertedList = list.stream()
                        .map(item -> item instanceof Document ? documentToMap((Document) item) : item)
                        .collect(Collectors.toList());
                result.put(k, convertedList);
            } else {
                result.put(k, v);
            }
        });
        return result;
    }

    // ─── AUXILIARES GENÉRICOS ──────────────────────────────────────────────────

    private static String getString(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        return interpreter.stringify(val);
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