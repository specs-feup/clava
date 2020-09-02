# TUErrorPatcher

Modo de utilização
Invocar o programa indicando nos argumentos os ficheiros e diretórios a processar.
Os ficheiros devem ter extensão .c ou .cpp. 

# Funcionamento
O programa cria para cada ficheiro processado um header contendo definições que permitem compilar o código. Isto pode incluir variáveis, funções, typedefs, structs e classes.

Em cada iteração o programa invoca o TUErrorDumper, que passa então informações sobre os erros encontrados ao tentar compilar o arquivo. Então o ErrorPatcher tenta criar ou modificar um patch para corrigir o erro e passa à próxima iteração até que todos os erros sejam corrigidos.

As informações sobre o erro podem vir na forma de argumentos dados pela API do Clang, como "identifier_name", "qualtype", etc. ou na forma de mensagens de erro, trechos de código ou localização no ficheiro. Nos últimos 3 casos a informação desejada é extraída utilizando funções definidas na classe TUPatcherUtils.

As informações sobre as correções a serem aplicadas são guardadas em um objeto da classe PatchData. Este objeto contém Hashmaps com objetos das classes TypeInfo e FunctionInfo, os quais contém as informações necessárias para escrever as definições de cada tipo, função ou variável.

Observações:<br/>
* Todas as funções no patch.h são declaradas com número variável de argumentos.
* Não são gerados namespaces, unions ou enums. Os erros relacionados a isso são resolvidos com classes e structs.
* A pasta test contém exemplos de casos de erro que o programa é capaz de resolver e alguns exemplos (comentados) em que o programa não é capaz de encontrar solução adequada.


**Para tratar mais tipos de erros:**
1. Adicionar número e nome do erro em “enum ErrorKind”
2. Criar na classe ErrorPatcher uma função para tratar deste erro.
3. Adicionar o ErrorKind e a função ao HashMap ERROR_PATCHERS na classe ErrorPatcher.

Importante observar que às  vezes um erro é causado por uma resolução inadequada de um erro anterior, e neste caso é preferível revisar as funções implementadas do que implementar uma nova função.
