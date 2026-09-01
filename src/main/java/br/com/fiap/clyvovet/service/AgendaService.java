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
 * "Este veterinario esta livre neste intervalo?" — a resposta combina a grade
 * semanal, os bloqueios e os atendimentos ja marcados.
 *
 * Classe separada porque a mesma pergunta e feita de dois lugares: o
 * agendamento, sobre UM horario, e a listagem de vagas, sobre todos os de um
 * periodo. Junto, um dos dois reimplementaria a regra.
 *
 * As horas viram LocalTime antes de qualquer comparacao: como texto funciona
 * por acidente do formato de largura fixa, e quebra no dia em que alguem
 * gravar "9:00".
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgendaService {

    private final DisponibilidadeVeterinarioRepository disponibilidadeRepository;
    private final BloqueioRepository bloqueioRepository;
    private final EventoClinicoRepository eventoClinicoRepository;

    /**
     * Um intervalo de tempo fechado no inicio e aberto no fim: [inicio, fim).
     *
     * A conversao de texto para LocalTime mora AQUI, nas fabricas, e nao em
     * cada chamador. Enquanto estava espalhada, {@code LocalTime.parse} aparecia
     * em dezessete pontos e a regra de colisao chegou a ser reescrita a mao numa
     * segunda implementacao, em AgendaCadastroService. Com a decisao neste
     * ponto, o dia em que o formato da hora mudar toca uma classe.
     */
    public record Janela(LocalTime inicio, LocalTime fim) {

        /** A partir do texto guardado no banco: "09:00". */
        public static Janela de(String inicio, String fim) {
            return new Janela(hora(inicio), hora(fim));
        }

        /** Comeco mais duracao — a forma que o agendamento usa. */
        public static Janela deDuracao(String inicio, int minutos) {
            LocalTime comeco = hora(inicio);
            return new Janela(comeco, comeco.plusMinutes(minutos));
        }

        public Janela deDuracao(int minutos) {
            return new Janela(inicio, inicio.plusMinutes(minutos));
        }

        /**
         * Unico ponto que interpreta a hora em texto.
         *
         * Comparar as horas como String so funciona por acidente do formato de
         * largura fixa. E o parse e estrito: "9:00" nao vira 9h, lanca
         * DateTimeParseException — falhar alto vale mais que um horario torto
         * na agenda. Ver JanelaTest.formatoEstrito.
         */
        public static LocalTime hora(String texto) {
            return LocalTime.parse(texto);
        }

        /**
         * O fim aberto permite [09:00, 09:30) conviver com [09:30, 10:00). Sem
         * ele, toda agenda cheia teria um furo artificial entre atendimentos.
         */
        public boolean colideCom(Janela outra) {
            return inicio.isBefore(outra.fim) && outra.inicio.isBefore(fim);
        }

        public boolean contem(Janela outra) {
            return !outra.inicio.isBefore(inicio) && !outra.fim.isAfter(fim);
        }

        /** A faixa termina depois de comecar? Coerencia minima de um intervalo. */
        public boolean ehCoerente() {
            return fim.isAfter(inicio);
        }
    }

    /**
     * A grade vem primeiro: descarta a maioria dos casos antes de tocar no resto.
     * Devolve o motivo, e nao um booleano, porque quem chama precisa dizer ao
     * usuario qual das tres coisas impediu.
     */
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
     * O passo e a duracao do servico, o que mantem a agenda alinhada e evita
     * buracos que nunca seriam usados.
     */
    public List<Janela> vagasLivres(UUID veterinarioId, LocalDate data, Servico servico) {
        List<Janela> vagas = new ArrayList<>();
        int duracao = servico.getDuracaoMinutos();

        for (DisponibilidadeVeterinario faixa : gradeDe(veterinarioId, data)) {
            Janela grade = Janela.de(faixa.getHoraInicio(), faixa.getHoraFim());
            LocalTime inicio = grade.inicio();
            LocalTime limite = grade.fim();

            // O ultimo slot precisa caber inteiro na faixa.
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
                .map(faixa -> Janela.de(faixa.getHoraInicio(), faixa.getHoraFim()))
                .anyMatch(faixa -> faixa.contem(janela));
    }

    private boolean colideComBloqueio(UUID veterinarioId, LocalDate data, Janela janela) {
        for (Bloqueio bloqueio : bloqueioRepository.queAlcancam(veterinarioId, data)) {
            if (bloqueio.diaInteiro()) {
                return true;
            }
            Janela faixaBloqueada = Janela.de(bloqueio.getHoraInicio(), bloqueio.getHoraFim());
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
     * Evento sem servico (anterior ao catalogo) recebe 30 min. Ignora-lo faria
     * a agenda aceitar marcacao em cima de um atendimento existente.
     */
    private Janela janelaDe(EventoClinico evento) {
        int duracao = evento.getServico() != null
                ? evento.getServico().getDuracaoMinutos()
                : DURACAO_PADRAO_MINUTOS;
        return Janela.deDuracao(evento.getHora(), duracao);
    }

    private static final int DURACAO_PADRAO_MINUTOS = 30;
}
