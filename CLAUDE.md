## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
- Community names are hand-written and live in `graphify-out/.graphify_labels.json`; a rebuild that reshuffles communities replaces the affected ones with hub-derived names. Restore them with `python scripts/label-communities.py`, then `graphify cluster-only .`. Do not leave hub names in place.
- `.graphifyignore` keeps AI-tooling files (skill instructions, `scripts/`) out of the graph so a full `graphify update .` reproduces the versioned graph. Do not remove those entries.
