# .mvn/

Configuração do **Maven Wrapper** — o que permite rodar `./mvnw` sem instalar
Maven na máquina. `maven-wrapper.properties` fixa a versão (3.9.14) que todo
mundo do grupo usa, e o `Dockerfile` e os dois pipelines de CI dependem disso.

## ⚠️ O `mvnw` foi modificado à mão

O `mvnw` na raiz **não é mais o arquivo gerado pelo Maven**. Se alguém rodar
`mvn wrapper:wrapper`, a correção abaixo é perdida em silêncio — e o build volta
a quebrar nas máquinas afetadas.

### O que foi corrigido

O script original escolhia como baixar o Maven pela **existência** da ferramenta,
e abortava com `|| die` se ela falhasse:

```sh
if command -v wget; then  wget ... || die     # se wget existe mas falha, morre aqui
elif command -v curl; then curl ... || die
elif set_java_home; then   # downloader em Java, nunca alcançado
```

O `wget` do Git Bash no Windows (`C:\WINDOWS\wget`) falha ao baixar a distribuição
do Maven Central. Como ele *existe*, o script o escolhia, ele falhava, e o build
parava antes de compilar — mesmo com `curl` funcionando na mesma máquina (a mesma
URL baixa em 0,14 s) e com JDK instalado.

Agora o comando está **dentro da condição do `if`**: se falhar, a condição é falsa
e o próximo baixador assume. São três tentativas de verdade, e a última — o
downloader em Java — funciona em qualquer máquina que rode Maven, porque toda uma
tem JDK.

Também entrou um `else die` explícito: antes, quando nenhum dos três servia, o
script seguia e estourava depois no `unzip`, com uma mensagem que não dizia o que
tinha acontecido.

### Se precisar regenerar o wrapper

Rode `mvn wrapper:wrapper` e **reaplique a correção** — ou confira antes se a
versão nova do wrapper já faz o fallback por conta própria. Teste assim, que é a
condição real de quem clona o projeto pela primeira vez:

```bash
rm -rf ~/.m2/wrapper/dists
./mvnw -v
```

## O `mvnw.cmd` não tem esse problema

Ele é o script do Windows nativo (cmd/PowerShell) e baixa com
`System.Net.WebClient`, sem depender de wget ou curl. Está intocado.
