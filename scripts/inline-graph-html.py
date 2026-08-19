#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Embute a biblioteca de desenho dentro do graphify-out/graph.html.

Contexto
--------
O `graph.html` gerado pelo graphify carrega a `vis-network` (~690 KB) do CDN
`unpkg.com`. Quem abrir o arquivo sem internet, ou atras de um proxy que bloqueie
o unpkg, ve so a pagina de fundo escuro e nenhum grafo -- o que anula o motivo de
versionar o arquivo, que e justamente ninguem precisar gerar nada.

Este script troca a tag `<script src="https://...">` pelo conteudo da biblioteca,
guardada em `scripts/vendor/`. Depois disso o `graph.html` nao depende de rede.

O graphify reescreve o `graph.html` a cada rebuild que muda o grafo, entao o passo
precisa ser repetido -- e por isso que ele e um script e nao uma edicao manual.

Confianca no arquivo baixado
----------------------------
A tag do CDN traz o hash SRI que o proprio graphify fixou. O script so embute a
biblioteca depois de conferir que o arquivo em `vendor/` casa com esse hash, e o
`--fetch` recusa um download que nao case. Ou seja: o que entra no HTML e
exatamente o que a pagina original teria carregado, nem um byte a mais.

Uso
---
    python scripts/inline-graph-html.py           # embute a biblioteca
    python scripts/inline-graph-html.py --check   # so relata; sai 1 se sobrou link externo
    python scripts/inline-graph-html.py --fetch   # rebaixa a biblioteca para vendor/

Fluxo tipico depois de um rebuild:

    graphify cluster-only .
    python scripts/inline-graph-html.py
"""

from __future__ import annotations

import base64
import hashlib
import re
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
HTML = RAIZ / "graphify-out" / "graph.html"
VENDOR = Path(__file__).resolve().parent / "vendor"

# Casa `<script ... src="http(s)://..." ...></script>`, com ou sem quebras de linha.
TAG_EXTERNA = re.compile(
    r'<script\b[^>]*\bsrc="(?P<url>https?://[^"]+)"[^>]*>\s*</script>', re.S
)
INTEGRITY = re.compile(r'\bintegrity="(?P<hash>sha(?:256|384|512)-[A-Za-z0-9+/=]+)"')


def _sri(dados: bytes, algoritmo: str) -> str:
    digest = hashlib.new(algoritmo, dados).digest()
    return f"{algoritmo}-{base64.b64encode(digest).decode()}"


def _confere_sri(dados: bytes, esperado: str) -> bool:
    algoritmo = esperado.split("-", 1)[0]
    return _sri(dados, algoritmo) == esperado


def _le_html() -> str:
    if not HTML.exists():
        sys.exit(f"erro: {HTML} nao existe -- rode `graphify cluster-only .` antes")
    return HTML.read_text(encoding="utf-8")


def _nome_local(url: str) -> str:
    return url.rstrip("/").rsplit("/", 1)[-1]


def fetch() -> int:
    """Baixa cada biblioteca externa do graph.html e valida contra o SRI da tag."""
    from urllib.request import urlopen

    html = _le_html()
    tags = list(TAG_EXTERNA.finditer(html))
    if not tags:
        print("nada a baixar: o graph.html nao referencia nenhuma biblioteca externa")
        return 0

    VENDOR.mkdir(parents=True, exist_ok=True)
    for tag in tags:
        url = tag.group("url")
        esperado = INTEGRITY.search(tag.group(0))
        print(f"baixando {url}")
        with urlopen(url, timeout=60) as resposta:  # noqa: S310 - URL vem do HTML gerado
            dados = resposta.read()

        if esperado:
            if not _confere_sri(dados, esperado.group("hash")):
                sys.exit(
                    f"erro: o arquivo baixado nao casa com o integrity da tag\n"
                    f"  esperado: {esperado.group('hash')}\n"
                    f"  recebido: {_sri(dados, esperado.group('hash').split('-', 1)[0])}"
                )
            print(f"  integrity confere ({esperado.group('hash')[:24]}...)")
        else:
            print("  aviso: a tag nao declara integrity, nada a conferir", file=sys.stderr)

        destino = VENDOR / _nome_local(url)
        destino.write_bytes(dados)
        print(f"  gravado em {destino.relative_to(RAIZ)} ({len(dados) // 1024} KB)")
    return 0


def _relatorio(html: str) -> list[str]:
    return [t.group("url") for t in TAG_EXTERNA.finditer(html)]


def check() -> int:
    pendentes = _relatorio(_le_html())
    if pendentes:
        for url in pendentes:
            print(f"  ainda externo: {url}")
        print(f"\n{len(pendentes)} biblioteca(s) fora do arquivo -- o graph.html nao abre offline")
        return 1
    print("o graph.html nao depende de rede")
    return 0


def embute() -> int:
    html = _le_html()
    tags = list(TAG_EXTERNA.finditer(html))
    if not tags:
        print("nada a fazer: a biblioteca ja esta embutida")
        return 0

    novo = html
    for tag in tags:
        url = tag.group("url")
        origem = VENDOR / _nome_local(url)
        if not origem.exists():
            sys.exit(
                f"erro: {origem.relative_to(RAIZ)} nao existe.\n"
                f"Rode `python scripts/inline-graph-html.py --fetch` (precisa de internet uma vez)."
            )

        dados = origem.read_bytes()
        esperado = INTEGRITY.search(tag.group(0))
        if esperado and not _confere_sri(dados, esperado.group("hash")):
            sys.exit(
                f"erro: {origem.name} nao casa com o integrity que o graph.html declara.\n"
                f"A versao da biblioteca mudou; rode com --fetch para atualizar o vendor."
            )

        # Uma ocorrencia de `</script` dentro do JS fecharia a tag antes da hora.
        # Nao acontece na vis-network, mas versoes futuras podem trazer.
        js = dados.decode("utf-8").replace("</script", "<\\/script")
        novo = novo.replace(
            tag.group(0),
            f"<!-- {url} embutido por scripts/inline-graph-html.py -->\n<script>\n{js}\n</script>",
        )
        print(f"embutido: {origem.name} ({len(dados) // 1024} KB)")

    HTML.write_text(novo, encoding="utf-8")
    antes, depois = len(html) // 1024, len(novo) // 1024
    print(f"graph.html: {antes} KB -> {depois} KB, sem dependencia de rede")
    return 0


def main() -> int:
    argumentos = set(sys.argv[1:])
    desconhecidos = argumentos - {"--fetch", "--check"}
    if desconhecidos:
        sys.exit(f"erro: argumento desconhecido: {' '.join(sorted(desconhecidos))}\n{__doc__}")
    if "--fetch" in argumentos:
        return fetch()
    if "--check" in argumentos:
        return check()
    return embute()


if __name__ == "__main__":
    raise SystemExit(main())
