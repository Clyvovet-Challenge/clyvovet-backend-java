package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.model.Bloqueio;
import br.com.fiap.clyvovet.model.DiaSemana;
import br.com.fiap.clyvovet.model.DisponibilidadeVeterinario;
import br.com.fiap.clyvovet.model.EventoClinico;
import br.com.fiap.clyvovet.model.Servico;
import br.com.fiap.clyvovet.repository.BloqueioRepository;
import br.com.fiap.clyvovet.repository.DisponibilidadeVeterinarioRepository;
import br.com.fiap.clyvovet.repository.EventoClinicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Responde a unica pergunta que o agendamento precisa fazer sobre horario:
 * "este veterinario esta livre neste intervalo?".
 *
 * POR QUE ISTO E UMA CLASSE SEPARADA
 * A resposta e composta por tres fontes que nao se conhecem — a grade semanal,
 * os bloqueios pontuais e os atendimentos ja marcados — e e consultada de dois
 * lugares com propositos diferentes: o agendamento, que pergunta sobre UM
 * horario, e a listagem de vagas, que pergunta sobre TODOS os de um periodo.
 * Se a regra morasse no AgendamentoService, a listagem teria de reimplementa-la
 * — e as duas divergiriam no primeiro ajuste.
 *
 * SOBRE A COMPARACAO DE HORARIO COMO TEXTO
 * O projeto guarda hora como VARCHAR(5) no formato "HH:mm", decisao herdada de
 * EventoClinico.hora. Aqui as horas sao convertidas para LocalTime antes de
 * qualquer comparacao: comparar "09:00" com "10:30" como texto funciona por
 * acidente do formato de largura fixa, e para de funcionar no dia em que
 * alguem gravar "9:00". Converter torna a intencao explicita e o codigo imune
 * a isso.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgendaService {

    private final DisponibilidadeVeterinarioRepository disponibilidadeRepository;
    private final BloqueioRepository bloqueioRepository;
    private final EventoClinicoRepository eventoClinicoRepository;

    /** Um intervalo de tempo fechado no inicio e aberto no fim: [inicio, fim). */
    public record Janela(LocalTime inicio, LocalTime fim) {

        /**
         * Duas janelas colidem quando uma comeca antes de a outra terminar, dos
         * dois lados. O fim aberto e o que permite uma consulta das 09:00 as
         * 09:30 conviver com a seguinte as 09:30 — sem ele, toda agenda cheia
         * teria um furo artificial entre atendimentos.
         */
        public boolean colideCom(Janela outra) {
            return inicio.isBefore(outra.fim) && outra.inicio.isBefore(fim);
        }

        public boolean contem(Janela outra) {
            return !outra.inicio.isBefore(inicio) && !outra.fim.isAfter(fim);
        }
    }

    /**
     * O veterinario atende neste intervalo, nesta data?
     *
     * Tres perguntas em sequencia, e a ordem importa para o custo: a grade e
     * consultada primeiro porque descarta a maioria dos casos (fora do
     * expediente) antes de tocar em bloqueio ou agenda.
     */
    public boolean estaLivre(UUID veterinarioId, LocalDate data, Janela janela) {
        return dentroDaGrade(veterinarioId, data, janela)
                && !colideComBloqueio(veterinarioId, data, janela)
                && !colideComAtendimento(veterinarioId, data, janela, null);
    }

    /**
     * Variante que ignora um evento especifico ao procurar colisao.
     *
     * Existe para o remarcar: sem ela, um evento sempre colidiria consigo
     * mesmo e nenhuma alteracao de horario passaria.
     */
    public boolean estaLivreIgnorando(UUID veterinarioId, LocalDate data, Janela janela, UUID eventoIgnorado) {
        return dentroDaGrade(veterinarioId, data, janela)
                && !colideComBloqueio(veterinarioId, data, janela)
                && !colideComAtendimento(veterinarioId, data, janela, eventoIgnorado);
    }

    public String porQueNaoEstaLivre(UUID veterinarioId, LocalDate data, Janela janela) {
        if (!dentroDaGrade(veterinarioId, data, janela)) {
            return "O veterinário não atende neste horário";
        }
        if (colideComBloqueio(veterinarioId, data, janela)) {
            return "O veterinário está indisponível neste horário";
        }
        if (colideComAtendimento(veterinarioId, data, janela, null)) {
            return "Já existe atendimento marcado para o veterinário neste horário";
        }
        return null;
    }

    /**
     * As vagas livres de um veterinario num dia, para um servico.
     *
     * O passo e a propria duracao do servico: uma consulta de 30 min gera vagas
     * de meia em meia hora dentro de cada faixa da grade. Isso mantem a agenda
     * alinhada e evita o cenario em que quatro marcacoes de 20 min deixam
     * buracos de 10 que nunca serao usados.
     */
    public List<Janela> vagasLivres(UUID veterinarioId, LocalDate data, Servico servico) {
        List<Janela> vagas = new ArrayList<>();
        int duracao = servico.getDuracaoMinutos();

        for (DisponibilidadeVeterinario faixa : gradeDe(veterinarioId, data)) {
            LocalTime inicio = LocalTime.parse(faixa.getHoraInicio());
            LocalTime limite = LocalTime.parse(faixa.getHoraFim());

            // O ultimo slot precisa CABER inteiro na faixa: uma faixa que
            // termina as 12:00 nao comporta uma cirurgia de 60 min iniciada as
            // 11:30, e oferece-la seria prometer o que a agenda nao sustenta.
            while (!inicio.plusMinutes(duracao).isAfter(limite)) {
                Janela candidata = new Janela(inicio, inicio.plusMinutes(duracao));
                if (!colideComBloqueio(veterinarioId, data, candidata)
                        && !colideComAtendimento(veterinarioId, data, candidata, null)) {
                    vagas.add(candidata);
                }
                inicio = inicio.plusMinutes(duracao);
            }
        }
        return vagas;
    }

    private List<DisponibilidadeVeterinario> gradeDe(UUID veterinarioId, LocalDate data) {
        return disponibilidadeRepository.vigentesEm(veterinarioId, DiaSemana.de(data), data);
    }

    private boolean dentroDaGrade(UUID veterinarioId, LocalDate data, Janela janela) {
        return gradeDe(veterinarioId, data).stream()
                .map(faixa -> new Janela(
                        LocalTime.parse(faixa.getHoraInicio()),
                        LocalTime.parse(faixa.getHoraFim())))
                .anyMatch(faixa -> faixa.contem(janela));
    }

    private boolean colideComBloqueio(UUID veterinarioId, LocalDate data, Janela janela) {
        for (Bloqueio bloqueio : bloqueioRepository.queAlcancam(veterinarioId, data)) {
            // Ferias sao dias inteiros: nao ha hora a comparar, o dia todo cai.
            if (bloqueio.diaInteiro()) {
                return true;
            }
            Janela faixaBloqueada = new Janela(
                    LocalTime.parse(bloqueio.getHoraInicio()),
                    LocalTime.parse(bloqueio.getHoraFim()));
            if (faixaBloqueada.colideCom(janela)) {
                return true;
            }
        }
        return false;
    }

    private boolean colideComAtendimento(UUID veterinarioId, LocalDate data, Janela janela, UUID ignorado) {
        for (EventoClinico evento : eventoClinicoRepository.ocupandoAAgenda(veterinarioId, data)) {
            if (ignorado != null && ignorado.equals(evento.getId())) {
                continue;
            }
            if (janelaDe(evento).colideCom(janela)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Quanto tempo um evento ja marcado ocupa.
     *
     * Evento sem servico vinculado — todos os anteriores ao catalogo — recebe
     * 30 minutos. Ignora-los seria pior: um atendimento historico deixaria de
     * bloquear o horario e a agenda passaria a aceitar marcacao em cima dele.
     */
    private Janela janelaDe(EventoClinico evento) {
        LocalTime inicio = LocalTime.parse(evento.getHora());
        int duracao = evento.getServico() != null
                ? evento.getServico().getDuracaoMinutos()
                : DURACAO_PADRAO_MINUTOS;
        return new Janela(inicio, inicio.plusMinutes(duracao));
    }

    private static final int DURACAO_PADRAO_MINUTOS = 30;
}
