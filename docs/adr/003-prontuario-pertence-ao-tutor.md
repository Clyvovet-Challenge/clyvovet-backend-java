# 003 — O prontuário pertence ao tutor: três níveis de acesso e quebra de vidro

**Data:** 2026-10-10 (registra uma decisão anterior à Sprint 4) · **Status:** aceito

## Contexto

O histórico clínico de um animal fica espalhado entre as clínicas que já o atenderam. Quando o
tutor troca de veterinário, ou chega numa emergência, quem atende não sabe de alergia, condição
crônica ou medicação em uso, e o tutor nem sempre lembra. Duas forças puxam em sentidos opostos:

- o dado é do tutor, então ninguém deveria ler sem o consentimento dele;
- negar uma informação de alergia numa emergência pode ser pior do que expô-la.

Um controle de acesso tudo-ou-nada (ou o veterinário vê o prontuário, ou não vê) não atende às duas.

## Decisão

O histórico pertence ao **tutor**, e o acesso tem três níveis:

| Nível | Quem alcança | O que vê |
|---|---|---|
| 0 · operacional | quem tem agendamento | nome, espécie, raça, porte, idade |
| 1 · resumo de segurança | qualquer veterinário autenticado, sempre | alergias, condições crônicas, medicação contínua, vacinas, último peso, contato |
| 2 · histórico completo | só com consentimento do tutor | linha do tempo, laudos, desfechos, dados do tutor |

Regras que sustentam os níveis:

- **O consentimento nasce no agendamento**, sem ciclo de pedir e aprovar. Vale 2 anos após o último
  atendimento naquela clínica (quem continua indo mantém; quem parou expira sozinho) e o tutor
  revoga quando quiser.
- **Quebra de vidro** para quem atende sem agendamento. Nunca é bloqueada, mas exige motivo, avisa
  o tutor na hora e entra destacada na auditoria.
- **Toda leitura é registrada**: uma linha por profissional, por dia. Baixar um anexo também.
- **Os anexos seguem a autorização do histórico** (nível 2), não uma regra própria.
- **O microchip identifica; nunca autoriza.** Quem credencia o nível 1 é a autenticação do veterinário.

## Consequências

- Atendimento sem agendamento só tem caminho pela quebra de vidro, e é por isso que ela existe.
- Ler o prontuário **grava** uma linha de auditoria. Não há porta que leia sem deixar rastro, e o
  preço é uma escrita a mais em cada consulta ao histórico.
- A decisão aparece em vários pontos do código (agendamento, histórico, anexos, auditoria), então
  mudar uma regra exige conferir todos. A matriz completa está em [`../08-seguranca.md`](../08-seguranca.md).
- Detalhes dos endpoints: seção "Fluxo C" do [README](../../README.md#fluxo-c--consentimento-e-histórico-clínico).
