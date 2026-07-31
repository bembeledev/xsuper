package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.execution.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;
import com.dic.xsuper.dom.node.XplNativeObject;

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

public class MongoNativeModel {

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Mongo", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        // ─── Mongo.connect(uri) ──────────────────────────────────────────────
        model.staticFields.put("connect", buildAction(1, (intp, args) -> {
            String uri = getString(intp, args, 0);
            try {
                MongoClientSettings settings = MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(uri))
                        .build();
                MongoClient client = MongoClients.create(settings);
                return new MongoConnection(client);
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "Falha na ligação ao MongoDB: " + e.getMessage());
            }
        }));

        // ─── Mongo.connectDefault() ──────────────────────────────────────────
        model.staticFields.put("connectDefault", buildAction(0, (intp, args) -> {
            try {
                MongoClient client = MongoClients.create();
                return new MongoConnection(client);
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "Falha na ligação ao MongoDB (padrão): " + e.getMessage());
            }
        }));

        // ─── REGISTAR ──────────────────────────────────────────────────────

        interpreter.registry_model.put("Mongo", model);
        interpreter.environment.defineConst("Mongo", new XplClass(model, interpreter.globals));
    }

    // ─── CLASSE: MongoConnection ──────────────────────────────────────────────

    private static class MongoConnection implements XplNativeObject {
        private final MongoClient client;
        private boolean closed = false;

        public MongoConnection(MongoClient client) {
            this.client = client;
        }

        @Override public void invokeMethod() {}

        @Override
        public Object invokeMethod(String methodName, List<Object> args, Interpreter interpreter) {
            if (closed) {
                throw new ControlFlow.RuntimeError(null, "Ligação ao MongoDB já fechada.");
            }
            switch (methodName) {
                case "getDatabase": {
                    String dbName = (String) args.getFirst();
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
            return propertyName.equals("closed") ? closed : null;
        }
    }

    // ─── CLASSE: MongoDatabaseWrapper ─────────────────────────────────────────

    private static class MongoDatabaseWrapper implements XplNativeObject {
        private final MongoDatabase db;

        public MongoDatabaseWrapper(MongoDatabase db) { this.db = db; }

        @Override public void invokeMethod() {}

        @Override
        public Object invokeMethod(String methodName, List<Object> args, Interpreter interpreter) {
            switch (methodName) {
                case "getCollection": {
                    String collName = (String) args.getFirst();
                    MongoCollection<Document> coll = db.getCollection(collName);
                    return new MongoCollectionWrapper(coll);
                }
                case "listCollectionNames": {
                    return db.listCollectionNames().into(new ArrayList<>());
                }
                case "createCollection": {
                    String collName = (String) args.getFirst();
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
            return propertyName.equals("name") ? db.getName() : null;
        }
    }

    // ─── CLASSE: MongoCollectionWrapper ───────────────────────────────────────

    private static class MongoCollectionWrapper implements XplNativeObject {
        private final MongoCollection<Document> coll;

        public MongoCollectionWrapper(MongoCollection<Document> coll) { this.coll = coll; }

        @Override public void invokeMethod() {}

        @Override
        public Object invokeMethod(String methodName, List<Object> args, Interpreter interpreter) {
            try {
                switch (methodName) {
                    case "insertOne": {
                        Map<String, Object> docMap = (Map<String, Object>) args.getFirst();
                        Document doc = mapToDocument(docMap);
                        InsertOneResult result = coll.insertOne(doc);
                        return Map.of("insertedId", result.getInsertedId());
                    }
                    case "insertMany": {
                        List<Map<String, Object>> docsList = (List<Map<String, Object>>) args.getFirst();
                        List<Document> docs = docsList.stream()
                                .map(MongoNativeModel::mapToDocument)
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
                        Map<String, Object> filterMap = !args.isEmpty() ? (Map<String, Object>) args.get(0) : new HashMap<>();
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
                                .map(MongoNativeModel::mapToBson)
                                .collect(Collectors.toList());
                        AggregateIterable<Document> iterable = coll.aggregate(pipeline);
                        return new MongoCursorWrapper(iterable.iterator());
                    }
                    case "createIndex": {
                        Map<String, Object> keysMap = (Map<String, Object>) args.getFirst();
                        Bson keys = mapToBson(keysMap);
                        String indexName = coll.createIndex(keys);
                        return indexName;
                    }
                    case "createIndexes": {
                        List<Map<String, Object>> indexList = (List<Map<String, Object>>) args.getFirst();
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
                        String indexName = (String) args.getFirst();
                        coll.dropIndex(indexName);
                        return null;
                    }
                    case "listIndexes": {
                        List<Map<String, Object>> indexes = new ArrayList<>();
                        coll.listIndexes().forEach(doc -> {
                            Map<String, Object> idxMap = new LinkedHashMap<>(doc);
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
            return propertyName.equals("name") ? coll.getNamespace().getCollectionName() : null;
        }
    }

    // ─── CLASSE: MongoCursorWrapper ───────────────────────────────────────────

    private static class MongoCursorWrapper implements XplNativeObject {
        private final MongoCursor<Document> cursor;
        private boolean closed = false;

        public MongoCursorWrapper(MongoCursor<Document> cursor) { this.cursor = cursor; }

        @Override public void invokeMethod() {}

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
            return list.stream().map(MongoNativeModel::convertValue).collect(Collectors.toList());
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

    // ─── AUXILIARES ──────────────────────────────────────────────────────────

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

    private static String getString(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        return interpreter.stringify(val);
    }
}