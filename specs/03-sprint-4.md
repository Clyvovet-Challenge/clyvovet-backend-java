# Sprint 4 — Java Advanced

**Entrega:** 04/11/2026 · **Peso:** 100 pontos · **Status:** não iniciado

Entrega final da disciplina. Consolida o trabalho das Sprints 1 a 3.

---

## Escopo (texto oficial)

> Durante o semestre, seu grupo trabalhou em um projeto cujo desafio central foi propor
> e implementar uma solução tecnológica inovadora para um problema real. Agora é o
> momento de consolidar todo esse trabalho e apresentar o resultado final de forma
> clara, funcional e integrada, demonstrando:
>
> - O funcionamento do sistema;
> - A coerência da solução proposta com o desafio;
> - O uso das tecnologias aprendidas;
> - E a conexão com as demais disciplinas do semestre.

---

## Requisitos e pontuação

### 1. Demonstração Técnica da Solução — 40 pontos

| Sub-requisito | Detalhe |
|---|---|
| Deploy | A aplicação deve estar **rodando online** |
| Navegação | A equipe deve navegar pelos principais fluxos do sistema |
| Conceitos | Aplicar os principais conceitos da disciplina de forma **contextualizada com o app criado** |
| Interface | Deve apresentar **boa UI e UX** |

"Rodando online" é requisito de nota — entrega em localhost não demonstra o item. O
deploy é compartilhado com DevOps (ver [04-dependencias-externas.md](04-dependencias-externas.md)),
que na Sprint 4 exige CI/CD no Azure DevOps com deploy em Azure Web App ou ACI.

### 2. Narrativa da Solução — 20 pontos

| Sub-requisito | Detalhe |
|---|---|
| Proposta | Explicação clara da proposta de solução do grupo |
| Decisões | Decisões de design, escolhas tecnológicas e **justificativas** |
| Originalidade | Destaque para originalidade e criatividade da solução |

### 3. Integração Multidisciplinar — 20 pontos

| Sub-requisito | Detalhe |
|---|---|
| Explicitação | Como as demais disciplinas foram aplicadas na solução |
| Evidências | Documentação, canvas, protótipos, scripts SQL, etc. |

Um quinto da nota depende de mostrar o Java conectado ao resto: o app mobile
consumindo esta API, as procedures de Database chamadas pelo backend, a pipeline de
DevOps, a IA de Disruptive Architectures integrada.

### 4. Apresentação Oral e Comunicação em Equipe — 10 pontos

| Sub-requisito | Detalhe |
|---|---|
| Participação | **Todos os membros devem participar do vídeo** |
| Qualidade | Clareza, objetividade e domínio sobre o que está sendo demonstrado |

### 5. Organização da entrega e da documentação — 10 pontos

---

## Penalidades

| Problema | Desconto |
|---|---|
| Código com violações evidentes de princípios de boas práticas | −10 por ocorrência |
| Repetição de código que poderia ter sido extraído para métodos reutilizáveis ou templates | −5 por ocorrência |
| Ausência de evidência de colaboração entre membros | −10 |
| Falhas graves de usabilidade na interface | −5 por ocorrência |
| Erros visuais ou de fluxo lógico durante a apresentação | −5 |
| Falta de alinhamento com o problema proposto (ex: solução genérica que ignora o desafio) | −10 |
| **Entrega fora do prazo ou fora do portal** | **−100** |

"Ausência de evidência de colaboração entre membros" (−10) é verificável pelo
histórico do Git: commits distribuídos entre os quatro integrantes ao longo do tempo,
não um push único no fim.

---

## Artefatos de entrega

| Artefato | Detalhe |
|---|---|
| Repositório | GitHub com **README completo** |
| Aplicação | **Link de acesso** à aplicação rodando |
| Vídeo | Apresentação com duração **máxima de 15 minutos** |

---

## Checklist de preparação

Derivado dos critérios acima, para uso perto da entrega:

- [ ] Aplicação acessível por URL pública no dia da avaliação
- [ ] Todos os fluxos principais navegáveis sem erro
- [ ] UI sem falhas graves de usabilidade
- [ ] README com visão geral, arquitetura, instalação, execução, acesso e integrantes
- [ ] Evidências de cada disciplina reunidas e referenciadas
- [ ] Vídeo ≤ 15 min com participação dos 4 integrantes
- [ ] Histórico Git mostrando colaboração distribuída
- [ ] Entrega feita **no portal**, dentro do prazo
