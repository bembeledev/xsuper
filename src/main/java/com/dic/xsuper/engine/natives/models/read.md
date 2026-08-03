# Documentação dos Módulos Nativos da Linguagem XPL

Esta documentação descreve todos os módulos nativos (bibliotecas padrão) disponíveis no motor de execução XPL. Cada módulo é implementado em Java e expõe funcionalidades diretamente para os scripts XPL, permitindo acesso a recursos do sistema, redes, criptografia, concorrência, bancos de dados, entre outros.

---

## Índice

1. [Módulos de Núcleo e Utilitários](#1-módulos-de-núcleo-e-utilitários)
    - Assert
    - Console
    - Log
    - Error
    - XplElement
2. [Data e Hora](#2-data-e-hora)
    - DateTime
3. [Manipulação de Texto e Dados](#3-manipulação-de-texto-e-dados)
    - Regex
    - Json
    - Url
4. [Matemática e Aleatoriedade](#4-matemática-e-aleatoriedade)
    - Math
5. [Sistema Operacional e Arquivos](#5-sistema-operacional-e-arquivos)
    - OS
    - File
    - Stream
    - Compress
    - Hardware
    - Network
6. [Segurança e Criptografia](#6-segurança-e-criptografia)
    - Security
    - Crypto
7. [Concorrência e Sincronização](#7-concorrência-e-sincronização)
    - Task
    - Mutex
    - Channel
    - EventLoop
    - Scheduler
8. [Banco de Dados](#8-banco-de-dados)
    - Database
    - Mongo
9. [HTTP e Web](#9-http-e-web)
    - Http
    - HttpServer
    - WebSocket
10. [Interoperabilidade](#10-interoperabilidade)
    - Interop
11. [Comunicação Serial](#11-comunicação-serial)
    - Serial
12. [E-mail (SMTP)](#12-e-mail-smtp)
    - Smtp
13. [Observabilidade (Observer)](#13-observabilidade-observer)
    - Observer
14. [Buffer (Manipulação de Bytes)](#14-buffer-manipulação-de-bytes)
    - Buffer

---

## 1. Módulos de Núcleo e Utilitários

### Assert
Módulo para testes e validações em tempo de execução.

**Métodos estáticos:**
- `eq(a, b)` – lança erro se `a` não for igual a `b`.
- `neq(a, b)` – lança erro se `a` for igual a `b`.
- `isTrue(cond)` – lança erro se `cond` for falso.
- `isFalse(cond)` – lança erro se `cond` for verdadeiro.
- `throws(fn)` – executa a função `fn` e espera que ela lance uma exceção; caso contrário, lança erro.

### Console
Fornece entrada e saída interativa no terminal.

**Métodos estáticos:**
- `readLine()` – lê uma linha de texto do console.
- `readInt()` – lê um número inteiro.
- `readFloat()` – lê um número decimal.
- `readBool()` – lê um valor booleano (true/false, sim/não, 1/0).
- `prompt(msg)` – exibe `msg` e lê uma linha.
- `promptInt(msg)`, `promptFloat(msg)`, `promptBool(msg)` – similar, com conversão de tipo.
- `clear()` – limpa a tela do terminal (ANSI).

### Log
Sistema de logging com níveis e saída para arquivo.

**Constantes:** `LEVELS` (DEBUG, INFO, WARN, ERROR)

**Métodos estáticos:**
- `setLevel(level)` – define o nível mínimo de log (ex: `Log.setLevel(Log.LEVELS.DEBUG)`).
- `toFile(path)` – redireciona os logs para um arquivo.
- `debug(msg)`, `info(msg)`, `warn(msg)`, `error(msg)` – registram mensagens.
- `history()` – retorna uma lista com as últimas mensagens registradas (até 10.000).

### Error
Classe base para erros/ exceções em XPL.

**Propriedades:**
- `message` – mensagem de erro.
- `toString()` – retorna a mensagem.

### XplElement
Classe base para elementos de UI (sistema de componentes). Usada para herança em componentes visuais.

**Propriedade:**
- `id` – identificador do elemento.

---

## 2. Data e Hora

### DateTime
Manipulação de datas, horas, fusos horários e durações.

**Construtores estáticos:**
- `now([timezone])` – data/hora atual no fuso informado (padrão: sistema).
- `fromUnix(timestampMs)` – a partir de milissegundos desde 1970.
- `parse(str, pattern)` – analisa uma string com formato personalizado (ex: `"yyyy-MM-dd HH:mm"`).

**Métodos de instância (retornam a própria instância para encadeamento):**
- `format(pattern)` – formata a data conforme padrão.
- `add(amount, unit)` – adiciona tempo (unidades: "years", "months", "days", "hours", "minutes", "seconds").
- `subtract(amount, unit)` – subtrai tempo.
- `diff(otherDate, unit)` – diferença entre duas datas.
- `toTimezone(zone)` – converte para outro fuso horário.

**Propriedades (readonly):** `year`, `month`, `day`, `hour`, `minute`, `second`, `timezone`, `timestamp`.

**Parsing de duração:** `DateTime.parseDuration("40s", "s")` – converte strings como "10m", "2h", "3d" para segundos (ou outra unidade especificada).

---

## 3. Manipulação de Texto e Dados

### Regex
Expressões regulares.

**Construtor estático:**
- `compile(pattern)` – cria uma instância de regex.

**Métodos da instância:**
- `test(string)` – retorna `true` se encontrar o padrão.
- `match(string)` – retorna `true` se toda a string corresponder.
- `captures(string)` – retorna uma lista com todos os grupos capturados.
- `replaceFirst(string, replacement)` – substitui a primeira ocorrência.
- `replaceAll(string, replacement)` – substitui todas as ocorrências.
- `split(string)` – divide a string pelo padrão.

**Validações estáticas (retornam booleano):**
- `isEmail`, `isUrl`, `isNumber`, `isUuid`, `isHexColor`, `isIpv4`, `isBase64`, `isStrongPassword`

**Utilitários estáticos:**
- `escape(string)` – escapa caracteres especiais para uso em regex.
- `extractNumbers(string)` – remove tudo que não é número.
- `extractLetters(string)` – remove tudo que não é letra.
- `normalizeSpaces(string)` – remove espaços duplicados.
- `slugify(string)` – converte para slug (ex: "Olá Mundo" → "ola-mundo").

### Json
Codificação e decodificação JSON.

**Métodos estáticos:**
- `encode(obj)` – converte objeto XPL para string JSON.
- `decode(json)` – converte string JSON para objeto XPL (mapas, listas, primitivos).

### Url
Manipulação de URLs e parâmetros de consulta.

**Métodos estáticos:**
- `encode(string)` – codifica para URL.
- `decode(string)` – decodifica URL.
- `parse(url)` – retorna um mapa com `scheme`, `host`, `port`, `path`, `query`, `fragment`.
- `parseHost(url)` – extrai o host.
- `parseQuery(query)` – converte query string (`a=1&b=2`) para mapa de listas.
- `buildQuery(params)` – constrói query string a partir de um mapa.

---

## 4. Matemática e Aleatoriedade

### Math
Funções matemáticas, constantes e estatísticas.

**Constantes:** `PI`, `E`, `INFINITY`, `NAN`, `EPSILON`, `MAX_INT`, `MIN_INT`.

**Funções trigonométricas:** `sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `atan2`, `sinh`, `cosh`, `tanh`.

**Conversão:** `toRadians`, `toDegrees`.

**Exponenciais e logaritmos:** `exp`, `expm1`, `log`, `log10`, `log1p`, `pow`, `sqrt`, `cbrt`, `hypot`.

**Arredondamento e sinal:** `abs`, `ceil`, `floor`, `round`, `rint`, `signum`, `nextUp`, `nextDown`.

**Comparação:** `min`, `max`, `clamp(val, min, max)`, `lerp(a, b, t)`.

**Verificações:** `isFinite`, `isNaN`, `isInfinite`.

**Aleatoriedade:**
- `random()` – número aleatório entre 0 e 1.
- `randomInt(min, max)` – inteiro aleatório.
- `randomDouble(min, max)` – decimal aleatório.
- `randomGaussian()` – distribuição normal (média 0, desvio 1).
- `randomGaussian(mean, stddev)` – normal com parâmetros.

**Estatísticas sobre listas:**
- `sum(list)`, `average(list)`, `minOf(list)`, `maxOf(list)`, `variance(list)`, `stddev(list)`.

---

## 5. Sistema Operacional e Arquivos

### OS
Informações do sistema, variáveis de ambiente, execução de processos.

**Propriedades (booleanas):** `IS_WINDOWS`, `IS_LINUX`, `IS_MAC`, `IS_ANDROID`.

**Métodos estáticos:**
- `name()`, `version()`, `arch()`, `hostname()`, `username()` – informações do sistema.
- `cpus()`, `totalMemory()`, `freeMemory()` – hardware.
- `diskInfo()` – lista de discos com espaço total, usado, livre.
- `networkInterfaces()` – interfaces de rede com MAC, IPs, etc.
- `env(var)` – valor de uma variável de ambiente.
- `envDefault(var, fallback)` – valor com fallback.
- `envAll()` – todas as variáveis de ambiente.
- `userHome()`, `userDir()`, `tmpDir()` – diretórios comuns.
- `pid()` – ID do processo atual.
- `sleep(ms)` – pausa a thread.
- `exit(code)` – encerra a JVM.
- `exec(options)` – executa um comando externo.

**Opções para `exec`:**
- `command` (string) – comando a executar.
- `args` (lista) – argumentos.
- `cwd` (string) – diretório de trabalho.
- `env` (mapa) – variáveis de ambiente adicionais.
- `timeout` (ms) – tempo limite.
- `stdin` (string) – entrada padrão.

Retorna um mapa com `exitCode`, `stdout`, `stderr`.

### File
Operações avançadas com ficheiros.

**Métodos estáticos:**
- Leitura/escrita: `read(path)`, `write(path, content)`, `append(path, content)`.
- Metadados: `exists`, `size`, `modified`, `created`, `isDir`, `isFile`, `isReadonly`, `isHidden`, `type` (retorna "file", "directory", "symlink", "other").
- Diretórios: `list(path)`, `mkdir(path)`, `mkdirAll(path)`, `rmdir(path)`.
- Operações: `delete`, `copy(src, dst)`, `move(src, dst)`.
- Caminhos: `name`, `dir`, `extension`, `stem`.
- Linhas: `readLines`, `writeLines`.
- Baixo nível: `open(path, mode)` (modos "r", "rw"), `close(handle)`, `seek(handle, pos)`, `tell(handle)`, `readChunk(handle, offset, length)`.
- Utilitários: `truncate(path, size)`, `touch(path)`, `temp()` (cria ficheiro temporário).
- Busca: `glob(pattern)` – lista ficheiros no diretório atual; `search(root, pattern)` – recursivo.
- Checksum: `checksum(path)` – retorna MD5 do ficheiro.

### Stream
Manipulação de fluxos de dados com acesso aleatório.

**Métodos estáticos:**
- `open(path, mode)` – retorna um handle com métodos:
    - `read([bytes])` – lê bytes (retorna base64) ou `null` se EOF.
    - `readLine()` – lê uma linha.
    - `write(data)` – escreve dados (string ou base64).
    - `seek(pos)` – posiciona o cursor.
    - `tell()` – posição atual.
    - `flush()` – sincroniza com disco.
    - `close()` – fecha o fluxo.

**Atalhos:**
- `readAll(path)` – lê todo o conteúdo como string.
- `writeAll(path, content)` – escreve todo o conteúdo.

### Compress
Compactação e descompactação nos formatos ZIP, TAR, GZIP, BZIP2, XZ, 7z.

**Métodos estáticos:**
- `compress(input, output, format, [options])` – compacta ficheiro ou diretório.
- `decompress(input, output, format, [options])` – descompacta.
- `listArchive(archive, format, [options])` – lista o conteúdo do arquivo.

**Formatos suportados:** `"zip"`, `"tar"`, `"tar.gz"`, `"tgz"`, `"tar.bz2"`, `"tbz2"`, `"tar.xz"`, `"txz"`, `"gzip"`, `"gz"`, `"bzip2"`, `"bz2"`, `"xz"`, `"7z"`.

**Opções comuns:**
- `password` (para ZIP/7z).
- `level` (nível de compressão, 0–9).

### Hardware
Informações detalhadas sobre CPU, memória, discos, rede, GPU, bateria, BIOS.

**Métodos estáticos:**
- `cpuInfo()` – modelo, vendor, arquitetura, núcleos, frequências, cache, uso, temperatura.
- `cpuUsage()` – percentual de uso da CPU.
- `cpuTemp()` – temperatura (se disponível).
- `memoryInfo()` – total, usado, livre, máximo, percentual.
- `diskInfo()` – lista de discos com tipo (SSD/HDD), modelo, espaço.
- `diskUsage(path)` – espaço do caminho especificado.
- `networkInfo()` – interfaces de rede detalhadas.
- `systemInfo()` – fabricante, modelo, serial, BIOS.
- `gpuInfo()` – lista de GPUs com nome, memória, driver.
- `batteryInfo()` – percentual de carga, status, tempo restante.

### Network
Utilitários de rede básicos.

**Métodos estáticos:**
- `ping(host, [timeout])` – verifica se o host responde.
- `dnsLookup(host)` – resolve host para IP.
- `reverseLookup(ip)` – resolve IP para nome.
- `localIp()` – IP da máquina.
- `localHostname()` – nome da máquina.
- `allIps()` – lista todos os IPs das interfaces.
- `portOpen(host, port, [timeout])` – verifica se a porta está acessível.
- `interfaces()` – lista interfaces de rede com MAC, IPs, MTU, etc.

---

## 6. Segurança e Criptografia

### Security
Funções criptográficas e de segurança.

**Hashes:** `md5`, `sha1`, `sha224`, `sha256`, `sha384`, `sha512`, `sha3_224`, `sha3_256`, `sha3_384`, `sha3_512`, `blake2b`, `blake2s`.

**HMAC:** `hmacMd5`, `hmacSha256`, `hmacSha512`.

**AES (CBC):**
- `aesEncrypt(keyBase64, dataBase64, ivBase64)` – retorna base64 do cifrado.
- `aesDecrypt(keyBase64, cipherBase64, ivBase64)` – retorna texto original.

**ChaCha20:**
- `chacha20Encrypt(keyBase64, dataBase64, nonceBase64)` – retorna base64.
- `chacha20Decrypt(keyBase64, cipherBase64, nonceBase64)` – retorna texto.

**RSA:**
- `rsaGenerate(keySize)` – gera par de chaves (retorna `public` e `private` em base64).
- `rsaEncrypt(publicKeyPem, data)` – cifra com chave pública.
- `rsaDecrypt(privateKeyPem, cipherBase64)` – decifra com chave privada.
- `rsaSign(privateKeyPem, data)` – assina com chave privada (SHA256withRSA).
- `rsaVerify(publicKeyPem, data, signatureBase64)` – verifica assinatura.

**ECDSA (secp256r1):**
- `ecdsaGenerate()` – gera par de chaves.
- `ecdsaSign(privateKeyPem, data)` – assina.
- `ecdsaVerify(publicKeyPem, data, signatureBase64)` – verifica.

**KDF (derivação de chave):**
- `pbkdf2Sha256(password, salt, iterations)` – retorna hash base64.
- `argon2idHash(password, salt)` – retorna hash base64.
- `argon2idVerify(password, salt, storedHash)` – verifica hash.

**Codificações:**
- `base64Encode`, `base64Decode`, `base58Encode`, `base58Decode`, `hexEncode`, `hexDecode`.

**Aleatoriedade:**
- `randomBytes(size)` – bytes aleatórios em base64.
- `randomInt(min, max)` – inteiro aleatório.
- `randomUuid()` – UUID v4.

**Outros:**
- `secureCompare(a, b)` – comparação resistente a ataques de temporização.
- `sandboxRun(fn)` – executa função em sandbox (placeholder).

### Crypto
Módulo avançado para PKI, certificados, JWT, SSH, etc. (depende do Bouncy Castle).

**CSR (Certificate Signing Request):**
- `csrGenerate(subject, privateKeyPem, publicKeyPem)` – gera CSR.
- `csrSign(csrPem, caPrivateKeyPem, caCertificatePem)` – assina CSR, retorna certificado.

**PKCS#12:**
- `pkcs12Create(certificatePem, privateKeyPem, password)` – cria keystore PKCS12 (retorna base64).
- `pkcs12Extract(pkcs12Base64, password)` – extrai certificado e chave privada.

**SSH (Ed25519):**
- `sshKeygenEd25519()` – gera par de chaves SSH (retorna `public` no formato OpenSSH e `private` PEM).

**ECDSA com curvas personalizadas:**
- `ecdsaGenerateExt(curve)` – gera par com curva (ex: "secp256r1", "secp384r1", "secp521r1").
- `ecdsaSignExt(privateKeyPem, data, curve)` – assina.
- `ecdsaVerifyExt(publicKeyPem, data, signature, curve)` – verifica.

**Encriptação híbrida (RSA + AES):**
- `hybridEncrypt(publicKeyPem, plaintext)` – retorna `{iv, encryptedKey, encryptedData}`.
- `hybridDecrypt(privateKeyPem, package)` – decifra e retorna texto.

**X.509:**
- `x509GenerateSelfSigned(subject, publicKeyPem, privateKeyPem)` – gera certificado autoassinado.
- `x509Validate(pem)` – valida certificado (verifica validade e assinatura), retorna atributos.

**JWT:**
- `jwtSign(payload, secret, algorithm)` – gera token JWT (algoritmos: HS256, RS256, ES256).
- `jwtVerify(token, secret, algorithm)` – verifica e retorna payload.

**SSH (conversão de chaves):**
- `sshPublicKey(publicKeyPem)` – converte para formato OpenSSH.
- `sshParsePublicKey(sshKey)` – analisa chave OpenSSH.

---

## 7. Concorrência e Sincronização

### Task
Gerenciamento de tarefas assíncronas (promessas/futuros).

**Métodos estáticos:**
- `run(fn)` – executa `fn` em outra thread, retorna uma promessa.
- `delay(ms, fn)` – executa `fn` após um atraso.
- `sleep(ms)` – pausa a thread atual.
- `yield()` – cede o processador.
- `await(promise)` – aguarda a conclusão e retorna o valor.
- `all([promises])` – aguarda todas as promessas, retorna lista de resultados.
- `race([promises])` – aguarda a primeira promessa a concluir.
- `allSettled([promises])` – aguarda todas, retorna lista com status e valor/erro.
- `cancel(promise)` – tenta cancelar.
- `isDone(promise)`, `isCancelled(promise)` – estado.
- `current()` – retorna a promessa da tarefa atual.
- `timeout(promise, ms)` – define um tempo limite para a promessa.

### Mutex
Exclusão mútua (lock).

**Construtor estático:**
- `create()` – retorna uma instância de mutex.

**Métodos da instância:**
- `lock()` – bloqueia (espera).
- `tryLock()` – tenta bloquear sem espera.
- `tryLockTimeout(timeoutMs)` – tenta com timeout.
- `unlock()` – desbloqueia.
- `isLocked()`, `isHeldByCurrentThread()`, `queueLength()`.

### Channel
Comunicação entre threads via filas (bloqueante/não-bloqueante).

**Construtor estático:**
- `create()` – retorna um canal.

**Métodos da instância:**
- `send(value)` – enfileira (não‑bloqueante, retorna booleano).
- `sendBlocking(value)` – bloqueia até conseguir enfileirar.
- `receive()` – bloqueia até receber.
- `receiveNonBlock()` – não‑bloqueante (retorna null se vazio).
- `receiveTimeout(timeoutMs)` – espera até timeout.
- `size()`, `isEmpty()`, `clear()`, `remainingCapacity()`.
- `close()` – libera recursos.

### EventLoop
Loop de eventos estilo Node.js (microtasks, macrotasks, timers).

**Métodos estáticos:**
- `nextTick(fn)` – adiciona tarefa à fila de microtasks.
- `defer(fn)` – adiciona à fila de macrotasks.
- `setTimeout(fn, ms)` – agenda execução após `ms`.
- `clearTimeout(id)` – cancela.
- `setInterval(fn, ms)` – agenda execução repetida.
- `clearInterval(id)` – cancela.
- `runMicrotasks()` – processa microtasks.
- `runMacrotasks()` – processa macrotasks.
- `runTick()` – processa ambas.
- `pendingCount()` – número de tarefas pendentes.
- `shutdown()` – encerra o loop.

### Scheduler
Agendamento de tarefas com cron e intervalos.

**Métodos estáticos:**
- `interval(segundos, fn)` – executa `fn` a cada `segundos`; retorna um job com `id` e `cancel()`.
- `cron(expressao, fn)` – agendamento estilo cron (5 campos: minuto hora dia mês diaSemana).
- `cancel(id)` – cancela um job.

---

## 8. Banco de Dados

### Database
Acesso a bancos de dados relacionais via JDBC.

**Métodos estáticos:**
- `connect(url, [user, password])` – estabelece ligação, retorna uma conexão.
- `driver(class)` – carrega um driver JDBC manualmente.

**Métodos da conexão:**
- `query(sql, [params])` – executa SELECT e retorna lista de mapas.
- `queryOne(sql, [params])` – retorna o primeiro registro ou null.
- `execute(sql, [params])` – executa INSERT/UPDATE/DELETE, retorna número de linhas afetadas.
- `transaction(fn)` – executa `fn` dentro de uma transação (auto-commit restaurado).
- `begin()`, `commit()`, `rollback()` – controle manual.
- `prepare(sql)` – retorna um statement preparado.
- `close()` – fecha a conexão.

**Métodos do PreparedStatement:**
- `set(index, value)` – define parâmetro.
- `setArray(index, list)` – define array.
- `executeQuery()` – executa SELECT, retorna lista de mapas.
- `executeUpdate()` – executa UPDATE/DELETE.
- `close()`.

**Suporte nativo para drivers:** PostgreSQL, MySQL, MariaDB, SQLite, H2, Oracle, Derby, SQL Server (via reflexão de classes).

### Mongo
Cliente para MongoDB (depende do driver MongoDB).

**Métodos estáticos:**
- `connect(uri)` – conecta ao MongoDB, retorna uma conexão.
- `connectDefault()` – conecta ao `mongodb://localhost:27017`.

**Métodos da conexão:**
- `getDatabase(name)` – obtém uma base de dados.
- `listDatabaseNames()` – lista nomes.
- `close()` – fecha a conexão.

**Métodos da base de dados:**
- `getCollection(name)` – obtém uma coleção.
- `listCollectionNames()`, `createCollection(name)`, `drop()`.

**Métodos da coleção:**
- `insertOne(document)`, `insertMany([documents])`.
- `find([filter], [options])` – retorna um cursor.
- `findOne([filter], [options])` – retorna um documento ou null.
- `updateOne(filter, update, [options])`, `updateMany(...)`.
- `deleteOne(filter)`, `deleteMany(filter)`.
- `countDocuments([filter])`.
- `aggregate(pipeline)` – agregação.
- `createIndex(keys)`, `createIndexes([{keys, unique, name}])`.
- `drop()`, `dropIndex(name)`, `listIndexes()`.

**Cursor (retornado por `find` e `aggregate`):**
- `next()` – próximo documento.
- `hasNext()` – booleano.
- `toArray()` – converte para lista.
- `close()`.

**Documentos:** são representados como mapas/dicionários XPL, com suporte a tipos aninhados.

---

## 9. HTTP e Web

### Http
Cliente HTTP.

**Constantes:** `METHODS` (GET, POST, etc.), `STATUS` (códigos agrupados), `HEADERS` (cabeçalhos comuns), `MIME_TYPES`.

**Métodos estáticos:**
- `get(url)` – retorna o corpo da resposta (string).
- `post(url, body)`, `put(url, body)`, `delete(url)`, `patch(url, body)`, `head(url)`, `options(url)` – retornam objeto com `status`, `body`, `headers`, `ok`.

- `request(url, method, body, options)` – personalizado.

**Opções:** `headers` (mapa), `timeout` (ms).

### HttpServer
Servidor HTTP embutido (Java HttpServer).

**Métodos estáticos:**
- `serve(port, handler)` – inicia o servidor; `handler` é uma função `(request, response) => { ... }`.

**Objeto request:**
- `method`, `path`, `query`, `headers`, `body`.

**Objeto response:**
- `status` (número, padrão 200)
- `headers` (mapa de cabeçalhos)
- `body` (string)

O servidor retorna uma instância com `id`, `port` e método `stop()`.

### WebSocket
Cliente WebSocket (Java HttpClient).

**Métodos estáticos:**
- `connect(url, [options])` – conecta e retorna um handle.

**Opções:**
- `onMessage` – função `(msg) => { ... }` (mensagem de texto ou base64 se `binary` for true).
- `onOpen` – função `() => { ... }`.
- `onClose` – função `(code, reason) => { ... }`.
- `onError` – função `(error) => { ... }`.
- `binary` – booleano (se true, `onMessage` recebe base64).

**Métodos do handle:**
- `send(text)` – envia mensagem de texto.
- `sendBytes(base64)` – envia dados binários.
- `close()` – fecha a conexão.
- `isOpen()` – verifica estado.

---

## 10. Interoperabilidade

### Interop
Execução de código em outras linguagens e chamada a processos externos.

**Métodos estáticos:**
- `eval(lang, code, [options])` – executa código em Python, Node.js, Ruby, PHP, Perl, Lua (necessita os interpretadores instalados). Retorna a saída padrão.
    - Opções: `charset` (padrão UTF-8), `timeout` (ms).

- `spawn(command, args, [options])` – inicia um processo e retorna um objeto para comunicação interativa.
    - Opções: `cwd`, `env`, `charset`, `timeout`, `stream` (booleano, ativa modo fluxo), `stdoutChannel`, `stderrChannel` (nomes de canais XPL para receber dados em tempo real).

**Métodos do processo:**
- `write(data)`, `writeLine(data)` – envia dados para stdin.
- `read()` – lê uma linha do stdout.
- `readAll()` – lê todo o stdout.
- `readError()` – lê todo o stderr.
- `wait([timeoutMs])` – aguarda a conclusão e retorna o código de saída.
- `terminate()` – termina graciosamente.
- `kill()` – força a terminação.
- `close()` – libera recursos.

**Propriedades do processo:** `pid`, `isAlive`, `exitCode`, `charset`.

**FFI (placeholder):** `ffiLoad`, `ffiCall` – ainda não implementados (futuro com JNA ou FFM).

---

## 11. Comunicação Serial

### Serial
Portas série (RS-232) usando jSerialComm.

**Métodos estáticos:**
- `list()` – retorna lista de portas com detalhes (nome, descrição, VID, PID, etc.).
- `open(path, baudRate, dataBits, stopBits, parity, readTimeout, writeTimeout)` – abre a porta, retorna um handle.
- `close(handle)` – fecha a porta.
- `read(handle, bytes)` – lê texto.
- `write(handle, data)` – escreve texto.
- `readBytes(handle, bytes)` – lê bytes (retorna base64).
- `writeBytes(handle, base64)` – escreve bytes.
- `flush(handle)` – limpa buffers.
- `setParams(handle, baud, data, stop, parity)` – reconfigura parâmetros.

**Métodos do handle:**
- `read(bytes)`, `readBytes(bytes)`, `write(data)`, `writeBytes(base64)`, `flush()`, `close()`, `setParams(...)`.
- Propriedades: `baudRate()`, `dataBits()`, `stopBits()`, `parity()`, `isOpen()`, `bytesAvailable()`, `path()`.

---

## 12. E-mail (SMTP)

### Smtp
Envio de e-mails via SMTP (requer JavaMail).

**Métodos estáticos:**
- `client(host, port, user, password)` – cria um cliente SMTP, retorna um objeto com método `send`.

**Método `send(from, to, subject, body)`** – envia o e-mail.

**Nota:** Suporta TLS/SSL automaticamente conforme a porta (465 para SSL, 587 para STARTTLS).

---

## 13. Observabilidade (Observer)

### Observer
Sistema de observação de variáveis e objetos (reactive).

**Constantes:**
- `Event` – `CREATE`, `UPDATE`, `READ`, `DELETE`.
- `Type` – `VARIABLE`, `INTERFACE`, `DECLARE`, `CLASS`, `INSTANCE`, `FUNCTION`.

**Método estático:**
- `observable(targetName, targetType)` – cria um observável para uma variável/objeto.

**Métodos do observável:**
- `watch(callback)` – registra um callback para eventos. O callback recebe `(evento, oldValue, newValue)`.
- `stop()` – remove todos os listeners.

**Funcionamento:** O observável monitora alterações na variável especificada e dispara o callback para eventos `CREATE`, `UPDATE`, `READ`, `DELETE`, filtrando por tipo estrutural.

---

## 14. Buffer (Manipulação de Bytes)

### Buffer
Buffer de bytes com operações de leitura/escrita.

**Construtor estático:**
- `alloc(size)` – aloca um buffer de `size` bytes, retorna uma instância.

**Métodos da instância:**
- `write(index, value)` – escreve um byte (0–255).
- `read(index)` – lê um byte (retorna inteiro).
- `writeString(index, text)` – escreve uma string UTF-8 a partir do índice, retorna número de bytes escritos.
- `toString()` – converte o conteúdo para string (ignorando bytes nulos).

**Propriedade:** `size` (readonly).

---

## Considerações Finais

- **Dependências externas:** Alguns módulos requerem bibliotecas adicionais (ex: zip4j, Bouncy Castle, MongoDB driver, JavaMail, jSerialComm). Certifique-se de que estão no classpath.
- **Segurança:** Os módulos de criptografia utilizam provedores padrão e Bouncy Castle. Em ambientes restritos, algumas funcionalidades podem não estar disponíveis.
- **Desempenho:** Módulos como `Task`, `EventLoop` e `Scheduler` utilizam pools de threads internos; monitore o uso para evitar vazamentos.
- **Portabilidade:** Módulos como `OS`, `Hardware` e `Serial` dependem do sistema operacional subjacente; algumas funcionalidades podem não funcionar em todos os ambientes.

---

*Esta documentação cobre a versão atual dos modelos nativos do motor XPL. Para mais detalhes, consulte o código-fonte ou a especificação da linguagem.*