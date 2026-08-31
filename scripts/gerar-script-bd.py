# -*- coding: utf-8 -*-
"""
Gera documentos/script_bd.sql a partir das migrations do Oracle.

A disciplina de DevOps pede o DDL num arquivo separado (item D1 da spec 04).
Mantê-lo à mão em paralelo às migrations o faria divergir do banco no primeiro
ALTER que alguém esquecesse de replicar — e aí o script serviria para enganar,
não para documentar. Por isso ele é gerado.

    python scripts/gerar-script-bd.py

O ScriptDoBancoTest quebra se o arquivo ficar defasado em relação às migrations.
"""
import glob
import io
import os
import re

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ORIGEM = os.path.join(RAIZ, 'src', 'main', 'resources', 'db', 'migration', 'oracle')
DESTINO = os.path.join(RAIZ, 'documentos', 'script_bd.sql')

CABECALHO = """-- ============================================================================
-- CLYVOVET -- Script completo do banco de dados (Oracle)
--
-- GERADO A PARTIR DAS MIGRATIONS, nao escrito a mao. A fonte da verdade e
-- src/main/resources/db/migration/oracle/, aplicada pelo Flyway; este arquivo
-- e a consolidacao delas num lugar so, para a entrega da disciplina de DevOps.
--
-- Um DDL mantido a mao em paralelo divergiria do banco no primeiro ALTER que
-- alguem esquecesse de replicar -- e ai o script serviria para enganar, nao
-- para documentar. Para regerar:
--
--     python scripts/gerar-script-bd.py
--
-- O EQUIVALENTE EM MYSQL esta em db/migration/mysql/, com as mesmas versoes.
-- Os dois conjuntos sao espelhos, e o MigrationsMySqlTest quebra se um deles
-- ficar para tras.
-- ============================================================================

"""


def versao(caminho):
    return int(re.search(r'V(\d+)__', os.path.basename(caminho)).group(1))


def gerar():
    arquivos = sorted(glob.glob(os.path.join(ORIGEM, 'V*.sql')), key=versao)
    if not arquivos:
        raise SystemExit('nenhuma migration encontrada em ' + ORIGEM)

    partes = []
    for caminho in arquivos:
        nome = os.path.basename(caminho)[:-len('.sql')]
        corpo = io.open(caminho, encoding='utf-8').read().strip()
        partes.append('-- ' + '=' * 72 + '\n-- ' + nome + '\n-- ' + '=' * 72
                      + '\n\n' + corpo)

    return CABECALHO + '\n\n\n'.join(partes) + '\n'


if __name__ == '__main__':
    conteudo = gerar()
    io.open(DESTINO, 'w', encoding='utf-8', newline='\n').write(conteudo)
    print('%s gerado a partir de %d migrations'
          % (os.path.relpath(DESTINO, RAIZ), len(glob.glob(os.path.join(ORIGEM, 'V*.sql')))))
