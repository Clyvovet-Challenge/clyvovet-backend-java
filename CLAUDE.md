## graphify

This repository ships with a prebuilt knowledge graph of the codebase in `graphify-out/`:
god nodes, community structure and cross-file relationships. Everything is committed, so a
fresh clone needs no rebuild and no LLM calls. Use it before grepping or opening source files —
it is built to answer with less context.

Reading order, cheapest first:

1. `graphify-out/wiki/index.md` — index of the communities, one article each. Plain markdown,
   nothing to install. Start here.
2. `graphify query "<question>"` — returns a scoped subgraph, far smaller than the report or a
   raw grep. Also `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"`.
   Needs `pip install graphifyy`.
3. `graphify-out/GRAPH_REPORT.md` — whole-architecture review. Only when the two above fall short.
4. `graphify-out/graph.json` — the source the tools above read. Large; never read it whole.

Maintenance:

- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
- Community names are hand-written and live in `graphify-out/.graphify_labels.json`; a rebuild that
  reshuffles communities replaces the affected ones with hub-derived names. Restore them with
  `python scripts/label-communities.py`, then `graphify cluster-only .`. Do not leave hub names in place.
- `.graphifyignore` keeps AI-tooling files (skill instructions, `scripts/`) out of the graph so a full
  `graphify update .` reproduces the versioned graph. Do not remove those entries.
