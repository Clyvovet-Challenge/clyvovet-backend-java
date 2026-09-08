package br.com.fiap.clyvovet.model;

/**
 * Ciclo de vida de um pedido de alteracao de cadastro.
 *
 * <p>Tres estados, e nao mais: o pedido nasce PENDENTE e termina APROVADA ou
 * RECUSADA. Nao ha EXPIRADA, e a ausencia e deliberada — expiracao exigiria um
 * job varrendo a tabela, e a janela entre o vencimento e a varredura seria um
 * pedido vencido ainda aprovavel. Um pedido que o tutor nunca respondeu continua
 * pendente ate ele decidir, o que e o comportamento honesto: ninguem alterou
 * nada, e a decisao segue sendo dele.</p>
 *
 * <p>Nao ha APLICADA separada de APROVADA. Aprovar E aplicar, na mesma
 * transacao — um estado intermediario entre "o tutor disse sim" e "o dado mudou"
 * so existiria para representar uma falha, e falha aqui desfaz tudo.</p>
 */
public enum StatusSolicitacao {

    /** Aguardando o dono do animal. */
    PENDENTE,

    /** O dono aceitou, e os valores propostos ja foram gravados no animal. */
    APROVADA,

    /** O dono recusou. O motivo fica registrado, para o veterinario entender. */
    RECUSADA
}
