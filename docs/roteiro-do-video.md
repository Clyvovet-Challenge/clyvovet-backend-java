# Roteiro do vídeo — API Java

Este repositório responde por **duas** disciplinas, e elas pedem coisas
diferentes do mesmo vídeo. Grave **um só**, nesta ordem.

| Disciplina | Peso do vídeo | O que ele tem de provar |
|---|---|---|
| **DevOps Tools & Cloud Computing** | **80 dos 100 pontos** | o deploy acontecendo, seguindo o README |
| **Java Advanced** | — | a aplicação funcionando, com frontend |

---

> ## ⚠️ Leia isto antes de planejar a gravação
>
> **1. O vídeo de DevOps não é uma demo — é o deploy acontecendo.** A régua diz,
> textualmente: *"deploy da aplicação seguindo **exatamente** os passos descritos
> no README.md"*. Não se grava o resultado: grava-se a execução. Se o seu README
> tem 9 scripts, o vídeo tem 9 scripts rodando.
>
> **2. O item 9.3 exige CRUD em duas tabelas relacionadas, provado por `SELECT`
> direto no banco.** Não basta o Postman devolver 200 — tem de aparecer a linha
> no MySQL antes e depois de cada operação.
>
> **3. O frontend de Java Advanced não existe.** Conferido hoje neste
> repositório: **0** `@Controller` de MVC, **0** templates, **0** dependência
> Thymeleaf. Sem ele, essa parte do vídeo não tem o que mostrar — e a régua da
> disciplina considera não implementado o que não aparece.

---

## Antes de apertar o REC

| | |
|---|---|
| ☐ | `az login` **já feito**, e a assinatura certa selecionada — ninguém quer ver o fluxo de login no vídeo |
| ☐ | `azure/00-variaveis.sh` revisado: nomes de recurso são únicos em toda a Azure |
| ☐ | Terminal com **fonte grande** (16pt+). Comando ilegível não prova nada |
| ☐ | Postman (ou `test_api.sh`) com as requisições **já montadas**, para não digitar JSON no ar |
| ☐ | O grupo de recursos **não existir ainda** — o vídeo mostra criando, não recriando |

---

## O roteiro

### Parte 1 — Provisionar (≈4 min)

| Cena | Comando | Narração |
|---|---|---|
| Abertura | — | "API Java do ClyvoVet, no Azure App Service com MySQL Flexible Server. Nada em container: o artefato publicado é o `.jar`" |
| Grupo | `bash azure/01-resource-group.sh` | "tudo nasce dentro de um grupo só, o que também facilita destruir depois" |
| Banco | `bash azure/02-banco-mysql.sh` | "MySQL gerenciado — **serviço PaaS, não container**. É o que a régua exige" |
| Plano + apps | `03-plano-app-service.sh`, `04-webapp-java.sh`, `05-webapp-dotnet.sh` | "um plano, dois apps: a API Java e a .NET do time dividem o mesmo plano e o mesmo banco" |
| Configuração | `bash azure/06-configuracoes.sh` | "as variáveis entram aqui. **Nenhum segredo está no repositório** — o `appsettings` só tem placeholder" |
| Deploy | `bash azure/07-deploy-java.sh` | "sobe o `.jar` construído pelo Maven, direto no App Service" |
| Verificação | `bash azure/09-verificar.sh` | "o script confere o que subiu e imprime as duas URLs" |

> Mostre o **portal da Azure** por 10 segundos aqui: os recursos criados, lado a
> lado. Vale mais que qualquer slide.

### Parte 2 — CRUD com prova no banco (≈4 min) — *o item 9.3*

Use **`t_clyvo_tutor` e `t_clyvo_animal`**: são o núcleo da solução e têm chave
estrangeira entre si, o que atende ao *"duas tabelas relacionadas"* e ao
*"tabelas significativas"* na mesma cena.

Abra a sessão SQL ao lado: `bash azure/10-sql-do-video.sh`

| # | Operação | Faça | Prove |
|---|---|---|---|
| 1 | **CREATE** tutor | `POST /api/v1/tutores` | `SELECT id, nome, email FROM t_clyvo_tutor ORDER BY id DESC LIMIT 1;` |
| 2 | **CREATE** animal | `POST /api/v1/animais` com o `tutorId` acima | `SELECT a.nome, t.nome FROM t_clyvo_animal a JOIN t_clyvo_tutor t ON t.id = a.tutor_id ...` — **o JOIN é o que evidencia o relacionamento** |
| 3 | **READ** | `GET /api/v1/animais` | a mesma linha, agora na resposta |
| 4 | **UPDATE** | `PUT /api/v1/animais/{id}` mudando o nome | rodar o `SELECT` **antes e depois**, lado a lado |
| 5 | **DELETE** | `DELETE /api/v1/animais/{id}` | o `SELECT` volta vazio |

> **Narre o que o `SELECT` prova**, não o que ele mostra: *"a linha sumiu do
> banco — não foi só a API respondendo 204"*.

### Parte 3 — Java Advanced (≈1 min)

Enquanto o frontend não existir, esta parte só pode mostrar o que há:

- Swagger em `/swagger-ui.html`, percorrendo os controllers
- A separação em camadas no editor: `controller` → `service` → `repository`
- As **19 migrations** do Flyway, nos dois dialetos, e os **392 testes** passando

Se o frontend for feito antes da gravação, ele **substitui** esta parte: navegue
por ele, faça um cadastro pela tela e mostre o dado aparecendo no banco.

---

## O que **não** fazer

- **Não corte o meio do deploy.** A régua quer o passo a passo; um corte no meio
  levanta a suspeita de que ele não funcionou.
- **Não mostre segredo na tela.** Senha, `JWT_SECRET`, connection string. Os
  scripts leem do ambiente justamente para isso — se algo vazar no vídeo, a
  credencial tem de ser trocada antes de entregar.
- **Não rode `azure/99-destruir.sh` no vídeo.** E não rode depois: recurso
  apagado equivale a entrega em localhost. Só **depois da correção**.

---

## Depois de subir

1. Link do YouTube no `readme.md` e no PDF de entrega.
2. PDF com **nome completo e RM de todos**, link do GitHub e link do YouTube —
   e *"não pode ter mais nada no PDF"*.
3. Vídeo **não listado**, nunca privado.
