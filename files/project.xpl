Requisitos Funcionais
Fazer upload e download dos arquivos...

 Requisitos não Funcionais

 - Todos os arquivos devem ter:
    - id
    - url(url do download)
    - size
    - tipo

 - todos arquivos serão armazenados num banco de arquivos
 - todos metadados dos arquivos serão armazenados dentro dum banco de dados
 - o banco de dodos e o banco de images são servidores diferentes
 - o download dos arquivos em pasta saõ empacotados como zip assim que o usuario escolher os arquivos especificos
 - o total de upload dos arquivos saõ 20MB por arquivos e 50 arquivos por pasta e o size de 50MB no total
 - deve ser capaz de baixar no máximo 5 arquivos em simultâneo
 - deve ser capaz de fazer upload de 5 arquivos ou pasta em simultâneo
 - capacidade de quando a rede cair ele continuar de onde parou
 - fazer pause e continue do download.


 rotas:
  -/ -> notfound
  -/home
  -/pessoas
  -/pessoas/1
