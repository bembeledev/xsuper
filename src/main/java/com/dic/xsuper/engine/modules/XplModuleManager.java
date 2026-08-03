package com.dic.xsuper.engine.modules;

import com.dic.xsuper.engine.core.Environment;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.core.Lexer;
import com.dic.xsuper.engine.core.Parser;
import com.dic.xsuper.engine.ast.Stmt;
import com.dic.xsuper.engine.core.Token;
import com.dic.xsuper.engine.exceptions.ControlFlow;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XplModuleManager {

    // A estrutura física de um módulo em RAM
    public static class XplModule {
        public final String path;
        public final Map<String, Object> exports = new HashMap<>();
        public boolean exportAll = false;
        public Environment localEnvironment;
        public boolean isCompiling = true;

        public XplModule(String path) {
            this.path = path;
        }
    }

    // A memória cache global de módulos já carregados (Protege contra loops infinitos circulares!)
    public final Map<String, XplModule> moduleCache = new HashMap<>();

    // Ponteiro quântico para saber que módulo estamos a compilar neste momento
    public XplModule currentCompilingModule = null;

    // Cofre do SDM
    private final Map<String, String> sdmDependencies = new HashMap<>();
    private final Path projectDirectory;

    // =========================================================================
    // ⭐ O ESCUDO ANTI-STACKOVERFLOW (Profundidade Máxima)
    // =========================================================================
    private int currentImportDepth = 0;
    private static final int MAX_IMPORT_DEPTH = 100; // Limite razoável para qualquer projeto real

    public XplModuleManager(Path projectDirectory) {
        this.projectDirectory = projectDirectory;
        loadSdmDependencies();
    }

    private void loadSdmDependencies() {
        File lockFile = new File(projectDirectory.toFile(), "sdm.lock");
        if (!lockFile.exists()) return;

        try {
            String content = Files.readString(lockFile.toPath());
            String[] lines = content.split("\n");
            String currentPkg = null;
            for (String line : lines) {
                if (line.contains("\": {") && !line.contains("resolved_dependencies")) {
                    currentPkg = line.split("\"")[1];
                } else if (line.contains("\"disk_path\":") && currentPkg != null) {
                    String path = line.split("\"")[3];
                    sdmDependencies.put(currentPkg, path);
                    currentPkg = null;
                }
            }
        } catch (Exception ignored) {}
    }

    public File resolvePhysicalFile(String relativePath) {
        relativePath = relativePath.replace("\\", "/");

        // 1. MÓDULOS LOCAIS DO UTILIZADOR (Prioridade Máxima)
        String[] localSearchPaths = {".", "src", "lib"};
        for (String base : localSearchPaths) {
            File localFile = new File(base, relativePath);
            if (localFile.exists() && localFile.isFile()) {
                return localFile;
            }
        }

        // 2. MÓDULOS DO COFRE SDM (Inteligência de Isolamento)
        for (Map.Entry<String, String> dep : sdmDependencies.entrySet()) {
            String pkgNamespace = dep.getKey();
            String pkgPrefix = pkgNamespace.replace(".", "/");

            if (relativePath.startsWith(pkgPrefix)) {
                String absoluteVaultPath = dep.getValue();

                String leftover = relativePath.substring(pkgPrefix.length());
                if (!leftover.startsWith("/")) leftover = "/" + leftover;

                File isolatedFile = new File(absoluteVaultPath + "/src/" + pkgPrefix + leftover);

                if (!isolatedFile.exists()) {
                    isolatedFile = new File(absoluteVaultPath + "/src" + leftover);
                }

                if (!isolatedFile.exists()) {
                    isolatedFile = new File(absoluteVaultPath + leftover);
                }

                if (isolatedFile.exists() && isolatedFile.isFile()) {
                    return isolatedFile;
                }
            }
        }

        return null;
    }

    // =========================================================================
    // ⭐ A MAGIA QUE PEDISTE: O MANAGER EXECUTA E VALIDA O MÓDULO COM PRE-CACHING
    // =========================================================================
    public XplModule loadModule(String modulePath, Token importKeyword, Interpreter engine, Environment parentEnv) {

        // 1. DEPENDÊNCIAS CIRCULARES PERMITIDAS (Retorna a casca JIT)
        if (moduleCache.containsKey(modulePath)) {
            return moduleCache.get(modulePath);
        }

        // 2. ⭐ BLOQUEIO DE PROFUNDIDADE ABSURDA
        if (currentImportDepth >= MAX_IMPORT_DEPTH) {
            throw new ControlFlow.RuntimeError(importKeyword,
                    "Erro Fatal (StackOverflow Preventivo): Profundidade de importação demasiado alta (> " + MAX_IMPORT_DEPTH + "). " +
                            "Isto geralmente indica uma arquitetura de projeto com aninhamento insustentável de dependências.");
        }

        currentImportDepth++; // Aprofunda na árvore de importações

        try {
            String osPath = modulePath.replace(".", "/") + ".xpl";
            File fileOrDir = resolvePhysicalFile(osPath);

            if (fileOrDir == null) {
                throw new ControlFlow.RuntimeError(importKeyword, "Módulo não encontrado: '" + modulePath + "'.");
            }

            System.out.println("[XPL Modularity] -> A compilar módulo: " + modulePath);
            String source;

            try {
                // Leitura do código-fonte (VFS ou ficheiro local)
                if (fileOrDir.getName().endsWith(".xplx")) {
                    try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(fileOrDir)) {
                        java.util.zip.ZipEntry spmEntry = zipFile.getEntry("package.spm");
                        String sourceDir = "src";

                        if (spmEntry != null) {
                            try (java.io.InputStream spmIs = zipFile.getInputStream(spmEntry)) {
                                String spmContent = new String(spmIs.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                                int idx = spmContent.indexOf("source_dir:");
                                if (idx != -1) {
                                    int qStart = spmContent.indexOf("\"", idx);
                                    int qEnd = spmContent.indexOf("\"", qStart + 1);
                                    if (qStart != -1 && qEnd != -1) {
                                        sourceDir = spmContent.substring(qStart + 1, qEnd).trim();
                                    }
                                }
                            }
                        }

                        String finalPath = sourceDir.isEmpty() ? osPath : sourceDir + "/" + osPath;
                        java.util.zip.ZipEntry entry = zipFile.getEntry(finalPath);
                        if (entry == null) {
                            entry = zipFile.getEntry(osPath);
                        }
                        if (entry == null) {
                            throw new java.io.IOException("Caminho não encontrado dentro do pacote selado: " + finalPath);
                        }

                        try (java.io.InputStream is = zipFile.getInputStream(entry)) {
                            source = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        }
                    }
                } else {
                    source = java.nio.file.Files.readString(fileOrDir.toPath());
                }
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(importKeyword, "Erro ao ler ficheiro " + osPath + ": " + e.getMessage());
            }

            Lexer lexer = new Lexer(source, modulePath);
            List<Token> tokens = lexer.tokenize();
            Parser parser = new Parser(tokens);
            List<Stmt> statements = parser.parse();

            XplModule newModule = new XplModule(modulePath);
            Environment moduleEnv = new Environment(parentEnv, 0);
            newModule.localEnvironment = moduleEnv;

            // ⭐ 3. PRE-CACHING: Regista a "casca" ANTES de executar, curando o Ciclo!
            moduleCache.put(modulePath, newModule);

            Environment previousEnv = engine.environment;
            XplModule previousModule = this.currentCompilingModule;

            try {
                engine.environment = moduleEnv;
                this.currentCompilingModule = newModule;

                for (Stmt stmt : statements) {
                    engine.execute(stmt);
                }

                if (newModule.exportAll) {
                    newModule.exports.putAll(moduleEnv.values);
                }
            } catch (RuntimeException e) {
                moduleCache.remove(modulePath);
                throw e;
            } finally {
                newModule.isCompiling = false;
                engine.environment = previousEnv;
                this.currentCompilingModule = previousModule;
            }

            return newModule;

        } finally {
            // ⭐ 4. RETOMA RESPIRAÇÃO SEGURA: Sai do nível de profundidade atual, mesmo que dê erro
            currentImportDepth--;
        }
    }

    public String getProjectGlobalsModule() {
        File manifest = new File(projectDirectory.toFile(), "package.spm");

        if (manifest.exists() && manifest.isFile()) {
            try {
                String spmContent = Files.readString(manifest.toPath());
                String group = extractManifestValue(spmContent, "group");
                String name = extractManifestValue(spmContent, "name");

                if (group != null && name != null) {
                    return group + "." + name + ".globals";
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String extractManifestValue(String content, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(key + "\\s*:\\s*\"([^\"]+)\"").matcher(content);
        if (m.find()) return m.group(1);
        return null;
    }
}