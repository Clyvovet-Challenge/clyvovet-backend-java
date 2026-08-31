package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.historico.AlertaRequest;
import br.com.fiap.clyvovet.dto.historico.AlertaResponse;
import br.com.fiap.clyvovet.model.AlertaClinico;
import br.com.fiap.clyvovet.model.OrigemAlerta;
import br.com.fiap.clyvovet.model.Perfil;
import br.com.fiap.clyvovet.repository.AlertaClinicoRepository;
import br.com.fiap.clyvovet.repository.AnimalRepository;
import br.com.fiap.clyvovet.security.SegurancaService;
import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.security.UsuarioAutenticado;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Os alertas clinicos que compoem o resumo de seguranca (nivel 1).
 *
 * Tanto o tutor quanto o veterinario registram — o tutor sabe que o cachorro
 * dele passa mal com dipirona, e essa informacao vale. O que muda e a ORIGEM,
 * derivada do perfil de quem grava e nunca aceita do corpo: "o tutor disse" e
 * "o veterinario registrou" pesam diferente na decisao clinica, e quem le o
 * resumo precisa saber qual dos dois esta lendo.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertaService {

    private final AlertaClinicoRepository alertaRepository;
    private final AnimalRepository animalRepository;
    private final SegurancaService seguranca;

    @Transactional
    public AlertaResponse registrar(UUID animalId, AlertaRequest request) {
        AlertaClinico alerta = new AlertaClinico();
        alerta.setAnimal(animalRepository.obterPorId(animalId));
        alerta.setTipo(request.getTipo());
        alerta.setDescricao(request.getDescricao());
        alerta.setOrigem(origemDeQuemRegistra());

        AlertaClinico salvo = alertaRepository.save(alerta);
        return new AlertaResponse(salvo.getId(), salvo.getTipo(), salvo.getDescricao(),
                salvo.getOrigem(), salvo.getRegistradoEm());
    }

    /**
     * Desativa em vez de apagar: o registro de que a alergia foi informada, e
     * por quem, nao pode sumir junto.
     *
     * A autorizacao e verificada aqui, e nao no controller, porque o id da rota
     * e do ALERTA e a regra fala do ANIMAL.
     */
    @Transactional
    public void desativar(UUID id) {
        AlertaClinico alerta = alertaRepository.obterPorId(id);
        garantirQuePodeDesativar(alerta);

        alerta.setAtivo(false);
        alertaRepository.save(alerta);
    }

    /**
     * Ownership, e mais uma barreira: o tutor nao derruba alerta registrado por
     * profissional. Ele corrige o que informou; achado clinico e retirado por
     * quem tem competencia para reavalia-lo.
     */
    private void garantirQuePodeDesativar(AlertaClinico alerta) {
        UUID animalId = alerta.getAnimal().getId();
        if (!seguranca.podeAcessarAnimal(animalId)) {
            throw new AccessDeniedException("Sem acesso a este animal");
        }
        if (alerta.getOrigem() == OrigemAlerta.VETERINARIO && ehTutor()) {
            throw new RegraDeNegocioException("origem",
                    "Alerta registrado por veterinário só pode ser retirado pelo corpo clínico");
        }
    }

    private boolean ehTutor() {
        UsuarioAutenticado usuario = seguranca.autenticadoOuNulo();
        return usuario != null && usuario.getUsuario().getPerfil() == Perfil.TUTOR;
    }

    private OrigemAlerta origemDeQuemRegistra() {
        UsuarioAutenticado usuario = seguranca.autenticadoOuNulo();
        boolean profissional = usuario != null
                && usuario.getUsuario().getPerfil() != Perfil.TUTOR;
        return profissional ? OrigemAlerta.VETERINARIO : OrigemAlerta.TUTOR;
    }
}
