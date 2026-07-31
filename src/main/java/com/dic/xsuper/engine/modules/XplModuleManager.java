package com.dic.xsuper.engine.modules;

import com.dic.xsuper.engine.core.Environment;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.core.Lexer;
import com.dic.xsuper.engine.core.Parser;
import com.dic.xsuper.engine.ast.Stmt;
import com.dic.xsuper.engine.core.Token;
import com.dic.xsuper.engine.execution.ControlFlow;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XplModuleManager {

    // A estrutura físsica de um módulo em RAM
    public static class XplModule {
        public final String path;
        public final Map<String, Object> exports = new HashMap<>();
        public boolean exportAll = false;
        public Environment localEnvironment;

        public XplModule(String path) {
            this.path = path;
        }
    }

    // A memória cache global de módulos já carregados (Protege contra loops infinitos!)
    public final Map<String, XplModule> moduleCache = new HashMap<>();

    // Ponteiro quântico para saber que módulo estamos a compilar neste momento
    public XplModule currentCompilingModule = null;

    // Cofre do SDM
    private final Map<String, String> sdmDependencies = new HashMap<>();
    private final Path projectDirectory;

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

        // ⭐ 1. MÓDULOS LOCAIS DO UTILIZADOR (Prioridade Máxima)
        // O motor verifica primeiro se o ficheiro existe no projeto atual (em src/, lib/, etc.)
        // Isto garante que os ficheiros do programador nunca quebram e podem até
        // sobrepor-se (shadowing) a bibliotecas externas se necessário.
        String[] localSearchPaths = {".", "src", "lib"};
        for (String base : localSearchPaths) {
            File localFile = new File(base, relativePath);
            if (localFile.exists() && localFile.isFile()) {
                return localFile;
            }
        }

        // ⭐ 2. MÓDULOS DO COFRE SDM (Inteligência de Isolamento)
        // Se não encontrou localmente, verifica se o pacote pertence ao sdm.lock
        for (Map.Entry<String, String> dep : sdmDependencies.entrySet()) {
            String pkgNamespace = dep.getKey(); // ex: com.dic.validators
            String pkgPrefix = pkgNamespace.replace(".", "/"); // ex: com/dic/validators

            if (relativePath.startsWith(pkgPrefix)) {
                // Caminho absoluto guardado no lock (ex: C:\...\validators-1.0.0)
                String absoluteVaultPath = dep.getValue();

                // O que sobra do import após o namespace (ex: /Validators.xpl)
                String leftover = relativePath.substring(pkgPrefix.length());
                if (!leftover.startsWith("/")) leftover = "/" + leftover;

                // Tentativa A: Estrutura perfeita (disk_path / src / namespace / ficheiro)
                File isolatedFile = new File(absoluteVaultPath + "/src/" + pkgPrefix + leftover);

                // Tentativa B (Fallback): Ficheiro na raiz da pasta src (sem subpastas do namespace)
                if (!isolatedFile.exists()) {
                    isolatedFile = new File(absoluteVaultPath + "/src" + leftover);
                }

                // Tentativa C (Fallback): Ficheiro na raiz do pacote (sem pasta src)
                if (!isolatedFile.exists()) {
                    isolatedFile = new File(absoluteVaultPath + leftover);
                }

                // Devolve o ficheiro do ambiente isolado
                if (isolatedFile.exists() && isolatedFile.isFile()) {
                    return isolatedFile;
                }
            }
        }

        return null; // Módulo realmente não existe em lado nenhum
    }

    // =========================================================================
    // ⭐ A MAGIA QUE PEDISTE: O MANAGER EXECUTA E VALIDA O MÓDULO!
    // =========================================================================
    public XplModule loadModule(String modulePath, Token importKeyword, Interpreter engine, Environment parentEnv) {
        if (moduleCache.containsKey(modulePath)) {
            return moduleCache.get(modulePath);
        }

        String osPath = modulePath.replace(".", "/") + ".xpl";
        File fileOrDir = resolvePhysicalFile(osPath);

        if (fileOrDir == null) {
            throw new ControlFlow.RuntimeError(importKeyword, "Módulo não encontrado: '" + modulePath + "'.");
        }

        System.out.println("[XPL Modularity] -> A compilar módulo: " + modulePath);
        String source;

        try {
            // ⭐ A MAGIA DE LEITURA BLINDADA E DINÂMICA
            if (fileOrDir.getName().endsWith(".xplx")) {
                // É UMA BIBLIOTECA COMPACTADA! Lê o código diretamente da memória (VFS)
                try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(fileOrDir)) {

                    // 1. Ler o manifesto package.spm primeiro para descobrir o source_dir
                    java.util.zip.ZipEntry spmEntry = zipFile.getEntry("package.spm");
                    String sourceDir = "src"; // Fallback padrão

                    if (spmEntry != null) {
                        try (java.io.InputStream spmIs = zipFile.getInputStream(spmEntry)) {
                            String spmContent = new String(spmIs.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

                            // Extração JIT do valor source_dir: "pasta"
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

                    // 2. Montar o caminho dinâmico respeitando o programador
                    String finalPath = sourceDir.isEmpty() ? osPath : sourceDir + "/" + osPath;

                    java.util.zip.ZipEntry entry = zipFile.getEntry(finalPath);
                    if (entry == null) {
                        // Fallback de segurança se o código estiver na raiz
                        entry = zipFile.getEntry(osPath);
                    }
                    if (entry == null) {
                        throw new java.io.IOException("Caminho não encontrado dentro do pacote selado: " + finalPath);
                    }

                    // 3. Ler o código-fonte final
                    try (java.io.InputStream is = zipFile.getInputStream(entry)) {
                        source = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    }
                }
            } else {
                // É UM FICHEIRO LOCAL DE DESENVOLVIMENTO
                source = java.nio.file.Files.readString(fileOrDir.toPath());
            }
        } catch (Exception e) {
            throw new ControlFlow.RuntimeError(importKeyword, "Erro ao ler ficheiro " + osPath + ": " + e.getMessage());
        }

        // 2. Lexer & Parser normais
        Lexer lexer = new Lexer(source, modulePath); // Usamos o modulePath como nome do ficheiro para os erros
        List<Token> tokens = lexer.tokenize();
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // 3. Criação da estrutura do módulo
        XplModule newModule = new XplModule(modulePath);
        Environment moduleEnv = new Environment(parentEnv, 0);
        newModule.localEnvironment = moduleEnv;

        // Early-Caching (Registar ANTES de executar para evitar Loops Circulares)
        moduleCache.put(modulePath, newModule);

        // 4. ⭐ INJEÇÃO DE EXECUÇÃO: O Manager usa o Motor para validar e correr o código
        Environment previousEnv = engine.environment;
        XplModule previousModule = this.currentCompilingModule;

        try {
            engine.environment = moduleEnv;
            this.currentCompilingModule = newModule;

            // O Interpretador apenas executa as árvores AST, o Manager orquestra!
            for (Stmt stmt : statements) {
                engine.execute(stmt);
            }

            // Tratamento de Exports
            if (newModule.exportAll) {
                newModule.exports.putAll(moduleEnv.values);
            }
        } catch (RuntimeException e) {
            moduleCache.remove(modulePath); // Em caso de erro, limpa o módulo quebrado da RAM
            throw e;
        } finally {
            engine.environment = previousEnv;
            this.currentCompilingModule = previousModule;
        }

        return newModule;
    }
}