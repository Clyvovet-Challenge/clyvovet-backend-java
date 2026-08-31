package br.com.fiap.clyvovet.dto.historico;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Nivel 1 — o que qualquer veterinario autenticado alcanca pelo microchip.
 *
 * O QUE NAO ESTA AQUI e tao deliberado quanto o que esta: nao ha CPF, endereco,
 * linha do tempo de atendimentos, laudo, diagnostico nem valor. Do tutor vem so
 * o telefone, porque para atender uma emergencia basta conseguir ligar.
 *
 * O que sobrou e o que decide conduta clinica nos primeiros minutos com um
 * animal que o profissional nunca viu.
 */
public record ResumoDeSegurancaResponse(
        UUID animalId,
        String nome,
        String especie,
        String raca,
        String porte,
        Integer idadeEmMeses,
        Boolean castrado,
        BigDecimal ultimoPesoKg,
        List<AlertaResponse> alertas,
        List<VacinaResponse> vacinas,
        String telefoneDeEmergencia
) {}
