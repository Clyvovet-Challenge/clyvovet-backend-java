#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Reaplica os nomes curados das comunidades no grafo do graphify.

Contexto
--------
O `graphify` agrupa os nos do grafo em comunidades e da um nome a cada uma.
Sem intervencao esse nome e o do no mais conectado do grupo -- algo como
`org.junit.jupiter.api.Test`, que diz pouco. Os nomes deste projeto foram
escritos a mao ("Testes de CRUD e Integracao", "Emissao e Leitura de JWT", ...)
e ficam em `graphify-out/.graphify_labels.json`.

O problema e que os identificadores de comunidade nao sao estaveis: quando o
codigo muda, o algoritmo reagrupa os nos e o que era a comunidade 7 pode virar
outra coisa. Por isso o graphify so reaproveita um nome salvo se a comunidade
continuar com exatamente os mesmos membros -- ele compara com as assinaturas
de `graphify-out/.graphify_labels.json.sig`. Quem mudou de verdade perde o nome
curado e volta ao nome do hub.

Este script cobre esse caso. Ele nao confia no identificador da comunidade:
guarda em `scripts/community-labels.json` quais nos formavam cada grupo curado
e, depois de um rebuild, devolve o nome a comunidade que herdou a maior parte
deles. Assim o nome segue as pessoas, nao o numero.

Uso
---
    python scripts/label-communities.py             # devolve os nomes curados
    python scripts/label-communities.py --check     # so relata; sai 1 se faltar algum
    python scripts/label-communities.py --snapshot  # grava os nomes atuais como referencia

Depois de restaurar, rode `graphify cluster-only .` para regenerar
GRAPH_REPORT.md e graph.html com os nomes corrigidos.
"""

from __future__ import annotations

import json
import sys
from collections import defaultdict
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
GRAFO = RAIZ / "graphify-out" / "graph.json"
ROTULOS = RAIZ / "graphify-out" / ".graphify_labels.json"
ASSINATURAS = RAIZ / "graphify-out" / ".graphify_labels.json.sig"
REFERENCIA = Path(__file__).resolve().parent / "community-labels.json"

# Fracao minima para aceitar um nome: ou a comunidade nova e feita
# majoritariamente de membros do grupo curado, ou o grupo curado foi parar
# majoritariamente nela. Um dos dois basta -- o primeiro cobre comunidades que
# se dividiram, o segundo cobre comunidades que se fundiram.
LIMIAR = 0.4


def _carrega_grafo() -> dict:
    if not GRAFO.exists():
        sys.exit(f"erro: {GRAFO} nao existe -- rode `graphify .` antes")
    return json.loads(GRAFO.read_text(encoding="utf-8"))


def _comunidades(grafo: dict) -> tuple[dict[int, list[str]], dict[int, str]]:
    """Devolve ({cid: [ids dos nos]}, {cid: nome atual})."""
    membros: dict[int, list[str]] = defaultdict(list)
    nomes: dict[int, str] = {}
    for no in grafo["nodes"]:
        cid = no.get("community")
        if cid is None:
            continue
        cid = int(cid)
        membros[cid].append(no["id"])
        if no.get("community_name"):
            nomes[cid] = no["community_name"]
    return dict(membros), nomes


def snapshot() -> int:
    grafo = _carrega_grafo()
    membros, nomes = _comunidades(grafo)
    referencia = [
        {"nome": nomes[cid], "membros": sorted(ids)}
        for cid, ids in sorted(membros.items())
        if cid in nomes
    ]
    REFERENCIA.write_text(
        json.dumps({"comunidades": referencia}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"referencia gravada: {len(referencia)} comunidades em {REFERENCIA.name}")
    return 0


def _casa_nomes(membros: dict[int, list[str]], referencia: list[dict]) -> dict[int, str]:
    """Da a cada comunidade nova o nome do grupo curado de onde vieram seus nos.

    Cada par (comunidade nova, grupo curado) e pontuado pelo numero de nos em
    comum. O casamento e guloso pela maior contagem e cada nome so e usado uma
    vez: se um grupo se partiu, o nome fica com a metade que herdou mais nos e a
    outra conserva o nome que o graphify deu.
    """
    conjuntos = {cid: set(ids) for cid, ids in membros.items()}
    candidatos = []
    for indice, item in enumerate(referencia):
        antigos = set(item["membros"])
        if not antigos:
            continue
        for cid, atuais in conjuntos.items():
            comuns = len(antigos & atuais)
            if not comuns:
                continue
            fatia = comuns / len(atuais)    # quanto da comunidade nova veio dali
            herdado = comuns / len(antigos)  # quanto do grupo curado veio parar aqui
            if fatia >= LIMIAR or herdado >= LIMIAR:
                candidatos.append((comuns, fatia, indice, cid))
    candidatos.sort(key=lambda c: (-c[0], -c[1], c[2], c[3]))

    escolhido: dict[int, str] = {}
    usados: set[int] = set()
    for _, _, indice, cid in candidatos:
        if cid in escolhido or indice in usados:
            continue
        escolhido[cid] = referencia[indice]["nome"]
        usados.add(indice)
    return escolhido


def restaura(apenas_relatorio: bool) -> int:
    if not REFERENCIA.exists():
        sys.exit(f"erro: {REFERENCIA} nao existe -- rode com --snapshot primeiro")
    referencia = json.loads(REFERENCIA.read_text(encoding="utf-8"))["comunidades"]
    grafo = _carrega_grafo()
    membros, atuais = _comunidades(grafo)
    curados = _casa_nomes(membros, referencia)

    devolvidos = {cid: nome for cid, nome in curados.items() if atuais.get(cid) != nome}
    sem_referencia = sorted(set(membros) - set(curados))

    for cid, nome in sorted(devolvidos.items()):
        print(f"  c{cid:<3} {atuais.get(cid, '(sem nome)')}  ->  {nome}")
    for cid in sem_referencia:
        print(f"  c{cid:<3} sem correspondencia na referencia, mantem: {atuais.get(cid, '(sem nome)')}")

    if apenas_relatorio:
        if devolvidos:
            print(f"\n{len(devolvidos)} comunidade(s) estao com nome nao curado")
            return 1
        print("\ntodos os nomes curados estao no lugar")
        return 0

    if not devolvidos:
        print("nada a fazer: todos os nomes curados ja estao no lugar")
        return 0

    finais = {cid: curados.get(cid) or atuais.get(cid, f"Community {cid}") for cid in membros}
    ROTULOS.write_text(
        json.dumps({str(cid): finais[cid] for cid in sorted(finais)}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    # A assinatura precisa descrever a mesma clusterizacao dos nomes que acabamos
    # de gravar, senao o proximo rebuild considera todos eles obsoletos.
    from graphify.cluster import community_member_sigs

    assinaturas = community_member_sigs(membros)
    ASSINATURAS.write_text(
        json.dumps({str(cid): assinaturas[cid] for cid in sorted(assinaturas)}), encoding="utf-8"
    )

    for no in grafo["nodes"]:
        cid = no.get("community")
        if cid is not None:
            no["community_name"] = finais[int(cid)]
    GRAFO.write_text(json.dumps(grafo, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    print(f"\n{len(devolvidos)} nome(s) devolvido(s).")
    print("rode `graphify cluster-only .` para regenerar GRAPH_REPORT.md e graph.html")
    return 0


def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    argumentos = set(sys.argv[1:])
    desconhecidos = argumentos - {"--snapshot", "--check"}
    if desconhecidos:
        sys.exit(f"erro: argumento desconhecido: {' '.join(sorted(desconhecidos))}\n{__doc__}")
    if "--snapshot" in argumentos:
        return snapshot()
    return restaura(apenas_relatorio="--check" in argumentos)


if __name__ == "__main__":
    raise SystemExit(main())
