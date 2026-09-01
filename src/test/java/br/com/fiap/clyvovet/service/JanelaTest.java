package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.service.AgendaService.Janela;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A Janela passou a ser o unico lugar que interpreta a hora em texto e o unico
 * que sabe o que e colisao. Antes, a formula estava escrita duas vezes e
 * LocalTime.parse aparecia em dezessete pontos — e a regra so era exercitada de
 * lado, pelos testes de fluxo. Com a decisao concentrada aqui, o teste tambem
 * vem para ca.
 */
class JanelaTest {

    @Test
    @DisplayName("o fim aberto deixa [09:00,09:30) conviver com [09:30,10:00)")
    void fimAbertoNaoColide() {
        Janela primeira = Janela.de("09:00", "09:30");
        Janela segunda = Janela.de("09:30", "10:00");

        // Sem isso, toda agenda cheia teria um furo artificial entre atendimentos.
        assertThat(primeira.colideCom(segunda)).isFalse();
        assertThat(segunda.colideCom(primeira)).isFalse();
    }

    @Test
    @DisplayName("sobreposicao parcial colide dos dois lados")
    void sobreposicaoParcial() {
        Janela nove = Janela.de("09:00", "10:00");
        Janela noveEMeia = Janela.de("09:30", "10:30");

        assertThat(nove.colideCom(noveEMeia)).isTrue();
        assertThat(noveEMeia.colideCom(nove)).isTrue();
    }

    @Test
    @DisplayName("uma janela contida colide, e conter nao e simetrico")
    void contida() {
        Janela manha = Janela.de("08:00", "12:00");
        Janela consulta = Janela.de("09:00", "09:30");

        assertThat(manha.colideCom(consulta)).isTrue();
        assertThat(manha.contem(consulta)).isTrue();
        assertThat(consulta.contem(manha)).isFalse();
    }

    @Test
    @DisplayName("contem aceita a janela que encosta nas bordas")
    void contemNasBordas() {
        Janela faixa = Janela.de("08:00", "12:00");

        assertThat(faixa.contem(Janela.de("08:00", "12:00"))).isTrue();
        assertThat(faixa.contem(Janela.de("08:00", "12:01"))).isFalse();
        assertThat(faixa.contem(Janela.de("07:59", "12:00"))).isFalse();
    }

    @Test
    @DisplayName("deDuracao soma os minutos a partir do texto")
    void duracaoAPartirDoTexto() {
        assertThat(Janela.deDuracao("09:00", 45))
                .isEqualTo(new Janela(LocalTime.of(9, 0), LocalTime.of(9, 45)));
    }

    @Test
    @DisplayName("ehCoerente rejeita a faixa que termina antes de comecar")
    void coerencia() {
        assertThat(Janela.de("08:00", "12:00").ehCoerente()).isTrue();
        assertThat(Janela.de("12:00", "08:00").ehCoerente()).isFalse();
        // Faixa de duracao zero nao e faixa.
        assertThat(Janela.de("08:00", "08:00").ehCoerente()).isFalse();
    }

    @Test
    @DisplayName("o formato e estrito: '9:00' e recusado, nao interpretado torto")
    void formatoEstrito() {
        // Verificado, nao suposto: LocalTime.parse NAO aceita hora de um digito.
        // Entao o risco real nunca foi ler "9:00" como 9h e sim explodir ao le-lo.
        // Falhar alto e o comportamento certo aqui — um horario ambiguo na agenda
        // vale menos que um erro visivel.
        assertThatThrownBy(() -> Janela.de("9:00", "9:30"))
                .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    @DisplayName("com largura fixa, ordem alfabetica e ordem de relogio coincidem")
    void ordemComLarguraFixa() {
        // E por isso que comparar como texto funcionava: por acidente do formato,
        // nao por estar certo. A Janela compara como tempo e nao depende disso.
        assertThat(Janela.de("08:00", "09:00").inicio())
                .isBefore(Janela.de("10:00", "11:00").inicio());
        assertThat("08:00".compareTo("10:00")).isNegative();
    }
}
