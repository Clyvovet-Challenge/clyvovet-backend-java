# Roteiro do vídeo — DevOps Tools & Cloud Computing

> **80 dos 100 pontos da disciplina estão neste vídeo.** O desenho da arquitetura
> vale os outros 20. Não há terceiro item.
>
> Este documento não é sugestão de edição: é a ordem exata do que gravar, com os
> comandos prontos. Ele existe porque a régua proíbe justamente o que salvaria uma
> gravação ruim — cortes.

---

## 1. As três regras que a gravação não pode quebrar

| Regra | De onde vem | O que acontece se quebrar |
|---|---|---|
| **Começar pelo `git clone`** | *"obrigatório começar assim os testes da solução"* | o avaliador não vê a solução nascer do zero |
| **Seguir *exatamente* o README** | item 9.2: *"deploy da aplicação seguindo **exatamente** os passos descritos no README.md"* | qualquer comando que não esteja lá é um passo não documentado |
| **Sem cortes ao evidenciar teste e persistência** | item 9.2 | não dá para consertar no meio; um erro custa a gravação inteira |

A consequência inverte a ordem natural de trabalho: **o README precisa estar certo
e testado antes de gravar**, porque a gravação o segue linha por linha.

> **Confirme a duração máxima no PDF do Challenge antes de gravar.** Este roteiro
> não assume um limite — se houver, o bloco a encurtar é o 4 (esperas de
> provisionamento), nunca o bloco 6.

---

## 2. Antes de apertar REC

Nada aqui aparece no vídeo. É tudo o que precisa estar verdadeiro para que a
gravação corra sem parar.

- [ ] **O caminho inteiro já foi rodado uma vez, do zero.** Do `01` ao `09`. Se o
      `09-verificar.sh` não saiu com código 0, não grave.
- [ ] **`bash azure/99-destruir.sh` executado depois desse ensaio.** O vídeo mostra
      a criação; se os recursos já existirem, os scripts falham com
      `AlreadyExists` na frente do avaliador.
- [ ] **Nomes de recurso ainda livres.** `az webapp list -o table` e
      `az mysql flexible-server list -o table` vazios para os nomes do
      `00-variaveis.sh`.
- [ ] **Os quatro segredos exportados no terminal que vai gravar** — e o terminal
      **não** pode ter o histórico com eles visível. Abra um terminal limpo.
- [ ] **Fonte do terminal em pelo menos 16pt.** O avaliador precisa ler o
      `SELECT`, não adivinhar.
- [ ] **Duas janelas posicionadas**: terminal à esquerda, cliente `mysql`
      à direita. O bloco 6 alterna entre as duas sem cortar.
- [ ] **Notificações desligadas** e nada de aba com credencial aberta.
- [ ] `az account show` confirmando a assinatura certa.

> **Sobre a senha na tela:** o `export MYSQL_PASSWORD=...` aparece digitado. Use uma
> senha descartável, e rode o `99-destruir.sh` depois da correção. Não use senha
> reaproveitada de outro lugar.

---

## 3. Bloco 1 — Abertura e o desenho (≈ 1 min)

Mostre [`docs/arquitetura-azure.svg`](arquitetura-azure.svg) na tela inteira.

> "CLYVO VET é uma plataforma veterinária. O diferencial dela é reunir o histórico
> clínico do animal num lugar só — vacina, consulta, exame e medicação deixam de
> viver em papéis de clínicas diferentes.
>
> São duas APIs sobre **um único banco**. Esta, em Spring Boot, é dona de tutor,
> animal, clínica, veterinário, evento clínico e pagamento. A outra, em ASP.NET
> Core, cuida de catálogo, lembretes e eventos pet — e **lê** animal e tutor daqui,
> sem nunca escrever neles.
>
> Escolhemos a **Opção 2** do edital: App Service mais banco PaaS. **Nada é
> containerizado** — nem a aplicação, nem o banco. As duas APIs dividem um App
> Service Plan B1 Linux, e o banco é um Azure Database for MySQL Flexible Server.
> Tudo criado por Azure CLI, um script por recurso."

Aponte no desenho, nesta ordem: Resource Group → banco → plano → os dois Web Apps →
as quatro setas.

Diga a frase que o avaliador precisa ouvir:

> "O banco é provisionado **vazio**. Quem cria o schema é o Flyway desta API, no
> primeiro boot — da V1 à V12."

---

## 4. Bloco 2 — Clone e segredos (≈ 1 min)

**Comece a gravação de terminal aqui, e não antes.** Primeiro comando na tela:

```bash
git clone https://github.com/Clyvovet-Challenge/clyvovet-backend-java.git
cd clyvovet-backend-java
git clone https://github.com/Clyvovet-Challenge/ClyvoVet-api.git ../ClyvoVet-api
```

> "Começo pelo clone, como o edital pede. Clono também a API .NET ao lado, porque
> o mesmo conjunto de scripts publica as duas."

Abra o `README.md` e mostre a seção **Deploy na Azure — passo a passo**.

> "Todo comando daqui em diante sai deste README. Não há passo que não esteja
> escrito aqui."

Segredos:

```bash
export MYSQL_PASSWORD='UmaSenhaForte123!'
export JWT_SECRET="$(openssl rand -base64 32)"
export DOTNET_API_KEY="$(openssl rand -hex 24)"
export TELEGRAM_BOT_TOKEN='123456789:AA...'
```

> "Nenhum segredo vive no repositório. Os scripts **recusam rodar** sem estas
> variáveis, em vez de assumir um valor padrão que acabaria commitado. Na Azure
> eles chegam como App Settings."

Prove, em vez de afirmar — é barato e responde à penalidade de −20:

```bash
grep -rn "MYSQL_PASSWORD\|JWT_SECRET" azure/00-variaveis.sh | head -4
```

> "Só o nome da variável e a checagem de que ela existe. Nenhum valor."

---

## 5. Bloco 3 — Criar os recursos (≈ 4 a 6 min)

Um comando por vez, esperando cada um terminar.

```bash
bash azure/00-descobrir-recursos.sh   # não cria nada, só consulta o catálogo
bash azure/01-resource-group.sh
bash azure/02-banco-mysql.sh          # o mais demorado: 3 a 5 minutos
bash azure/03-plano-app-service.sh
bash azure/04-webapp-java.sh
bash azure/05-webapp-dotnet.sh
bash azure/06-configuracoes.sh
```

Enquanto o `02` roda, use o tempo — não fique em silêncio:

> "O `00` não cria nada: conta o que a assinatura de estudante oferece na região.
> Assinatura acadêmica tem catálogo restrito, e ele varia por região — descobrir
> isso durante o provisionamento é tarde demais.
>
> Uma região só para tudo, `brazilsouth`. A infraestrutura anterior tinha o grupo
> numa região e o banco em outra, e toda consulta atravessava região.
>
> O plano é **B1**, e não o F1 gratuito. O F1 existe, mas não tem Always On: o app
> dorme depois de vinte minutos ocioso. B1 é o mais barato **com** Always On."

Ao terminar o `06`:

> "Uma instância, autoscale desligado — e isso está no script, não na lembrança de
> alguém. Cinco componentes das duas APIs guardam estado no processo: revogação de
> token, rate limit e cache aqui; dois serviços de notificação na .NET. Com duas
> instâncias, o logout para de funcionar e a notificação duplica — **sem erro no
> log**."

Confirme no portal ou por CLI que os cinco recursos existem:

```bash
az resource list -g rg-clyvovet-sprint3 -o table
```

---

## 6. Bloco 4 — Publicar (≈ 3 a 5 min)

```bash
bash azure/07-deploy-java.sh
bash azure/08-deploy-dotnet.sh
```

> "A ordem importa, e não é preferência. A Java sobe **primeiro** porque é o Flyway
> dela que cria as tabelas — inclusive as seis que a API .NET consome. Se a .NET
> subisse antes, encontraria um banco vazio."

Depois do `07`, mostre o Flyway trabalhando no log ao vivo:

```bash
az webapp log tail -g rg-clyvovet-sprint3 -n app-clyvovet-java-rm562312
```

> "Aqui está o Flyway aplicando V1 até V12 num banco que estava vazio há dois
> minutos. É isto que substitui rodar DDL à mão."

---

## 7. Bloco 5 — Verificação de ponta a ponta, sem cortes (≈ 2 min)

```bash
bash azure/09-verificar.sh
```

**Não corte durante este comando.** Ele é a evidência do item 9.2.

> "Saúde das duas APIs, cadastro, login, CRUD de animal na Java, e o fluxo cruzado:
> um lembrete criado na API .NET que resolve o nome do animal lendo a tabela que a
> API Java acabou de gravar. É a prova de que o banco é o mesmo.
>
> No final ele confere o `max_connections` real do servidor contra o orçamento de
> conexões das duas APIs — medido, não presumido."

---

## 8. Bloco 6 — CRUD no banco, operação por operação (≈ 5 a 7 min)

**É o bloco que mais vale, e o mais fácil de perder pontos.** Três penalidades
moram aqui:

| Penalidade | Como este bloco a evita |
|---|---|
| Sem evidência clara de cada operação CRUD no banco | **−30** → um `SELECT` depois de *cada* operação |
| Usar apenas uma tabela no CRUD | **−20** → duas tabelas, `t_clyvo_tutor` e `t_clyvo_animal` |
| Tabelas fora do CORE da solução | **−30** → as duas são do núcleo, e têm FK entre si |

Tela dividida: **API à esquerda, banco à direita.** Alterne, não corte.

Abra o cliente na janela da direita e deixe aberto:

```bash
mysql -h mysql-clyvovet-rm562312.mysql.database.azure.com \
      -u clyvovetadmin -p clyvovet --ssl-mode=REQUIRED
```

> "Duas tabelas relacionadas, e as duas são o coração da solução: o tutor, e o
> animal que pertence a ele por chave estrangeira."

### Estado inicial

```sql
SELECT COUNT(*) AS tutores FROM t_clyvo_tutor;
SELECT COUNT(*) AS animais FROM t_clyvo_animal;
```

> "O que existe aqui veio do seed da V2 — o Flyway criou e populou. Nada foi
> inserido à mão."

### 8.1 — CREATE, nas duas tabelas

**API**, tutor:

```bash
curl -s -X POST "$JAVA/api/v1/auth/registrar" -H 'Content-Type: application/json' \
  -d '{"nome":"Marina Video","email":"marina.video@clyvovet.com","senha":"Video@12345","cpf":"39053344705","telefone":"11988887777"}'
```

**Banco** — a evidência:

```sql
SELECT id, nome, email, telefone FROM t_clyvo_tutor
 WHERE email = 'marina.video@clyvovet.com';
```

**API**, animal ligado a esse tutor (use o `tutorId` devolvido acima):

```bash
curl -s -X POST "$JAVA/api/v1/animais" -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Thor","especie":"Cachorro","raca":"Golden Retriever","porte":"GRANDE","sexo":"MACHO","cor":"Dourado","dataNascimento":"2022-05-10","tutorId":"<TUTOR_ID>"}'
```

**Banco** — a evidência, já mostrando o relacionamento:

```sql
SELECT a.id, a.nome AS animal, a.raca, a.cor, t.nome AS tutor, t.email
  FROM t_clyvo_animal a
  JOIN t_clyvo_tutor  t ON t.id = a.tutor_id
 WHERE t.email = 'marina.video@clyvovet.com';
```

> "Aqui estão as duas tabelas ligadas: o animal aponta para o tutor por chave
> estrangeira, e o `JOIN` traz os dois."

### 8.2 — READ

```bash
curl -s "$JAVA/api/v1/animais/<ANIMAL_ID>" -H "Authorization: Bearer $TOKEN"
```

```sql
SELECT id, nome, especie, raca, porte, cor, data_nascimento, tutor_id
  FROM t_clyvo_animal WHERE id = '<ANIMAL_ID>';
```

> "O que a API devolve e o que está gravado são a mesma linha."

### 8.3 — UPDATE, nas duas tabelas

```bash
curl -s -X PATCH "$JAVA/api/v1/animais/<ANIMAL_ID>" -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"cor":"Dourado claro"}'

curl -s -X PATCH "$JAVA/api/v1/tutores/<TUTOR_ID>" -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"telefone":"11977776666"}'
```

```sql
SELECT a.nome AS animal, a.cor, t.nome AS tutor, t.telefone
  FROM t_clyvo_animal a
  JOIN t_clyvo_tutor  t ON t.id = a.tutor_id
 WHERE a.id = '<ANIMAL_ID>';
```

> "Cor do animal e telefone do tutor mudaram no banco. Duas tabelas, uma consulta."

### 8.4 — DELETE

A ordem é imposta pela chave estrangeira: **animal antes do tutor**.

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE "$JAVA/api/v1/animais/<ANIMAL_ID>" -H "Authorization: Bearer $TOKEN"
```

```sql
SELECT COUNT(*) AS deve_ser_zero FROM t_clyvo_animal WHERE id = '<ANIMAL_ID>';
```

> "Zero. A linha não existe mais."

Se for apagar o tutor também, mostre o `SELECT` depois — e vale comentar a ordem:

> "O animal sai antes do tutor. É a chave estrangeira mandando, e é ela que garante
> que não sobra animal sem dono no banco."

### 8.5 — Fechamento do bloco

```sql
SELECT t.nome AS tutor, COUNT(a.id) AS qtd_animais
  FROM t_clyvo_tutor t
  LEFT JOIN t_clyvo_animal a ON a.tutor_id = t.id
 GROUP BY t.id, t.nome
 ORDER BY qtd_animais DESC;
```

> **Atalho:** o `09-verificar.sh` imprime esses `SELECT` prontos, com os nomes de
> recurso já preenchidos. Rode antes e deixe a saída num arquivo para copiar
> durante a gravação.

---

## 9. Bloco 7 — DDL e encerramento (≈ 1 min)

Mostre o DDL — não incluí-lo vale **−10**:

```sql
SHOW CREATE TABLE t_clyvo_animal;
```

E onde ele vive versionado:

```bash
ls src/main/resources/db/migration/mysql/
```

> "O DDL não é um arquivo solto: são as migrations do Flyway, V1 a V12, versionadas
> no repositório. Foram elas que criaram este banco."

Fechamento:

> "Recapitulando: cinco recursos criados por Azure CLI, um script por recurso; duas
> APIs publicadas nativamente em App Service, sem container; um banco MySQL
> gerenciado, provisionado vazio e versionado por Flyway; e o CRUD completo
> demonstrado em duas tabelas relacionadas do núcleo da solução, com evidência no
> banco a cada operação."

**Não** rode o `99-destruir.sh` no vídeo — e nem depois, antes da correção. Recurso
apagado equivale a entrega em localhost, que é **zero**.

---

## 10. O que não mostrar

| Não mostre | Por quê |
|---|---|
| `docker`, `docker compose`, o `Dockerfile` | app ou banco containerizado é **−40 cada**. O Dockerfile existe para desenvolvimento local, mas não aparece |
| H2, `localhost`, `spring-boot:run` | H2 é banco não permitido (**−40**); entrega em localhost é **zero** |
| O `azure-pipelines.yml` | CI/CD é requisito da **Sprint 4** — mostrar agora só levanta pergunta fora de escopo |
| Portal da Azure criando recurso | recurso não criado via CLI é **−30**. O portal só para *conferir* o que a CLI criou |
| Segredo em valor legível fora do `export` | **−20** |

---

## 11. Depois de gravar

- [ ] Assista inteiro **antes de subir**, com atenção a: começou pelo clone? algum
      comando fora do README? algum corte durante teste ou persistência?
- [ ] YouTube **não listado** (não privado — privado bloqueia o avaliador, e
      *"professor sem acesso ao vídeo"* é **zero**). Teste o link numa janela
      anônima.
- [ ] Link no PDF de entrega, junto do link do GitHub, nome completo e RM de todos.
      *"Não pode ter mais nada no PDF"*.
- [ ] Link também no README, para quem chegar pelo repositório.
- [ ] **Não destrua os recursos.** Só depois do feedback, em 26/09.

---

## 12. O outro vídeo

A disciplina de Mobile pede o seu próprio vídeo, e ele **não** é este. Este é a
infraestrutura; o de Mobile é o aplicativo funcionando. Roteiro separado, gravação
separada, e a data do cronograma é **11/09** — depois deste, para que uma
regravação aqui não coma o tempo de lá.
