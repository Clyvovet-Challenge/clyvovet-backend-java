package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.auth.RegistroRequest;
import br.com.fiap.clyvovet.dto.auth.UsuarioRequest;
import br.com.fiap.clyvovet.dto.auth.UsuarioResponse;
import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.mapper.UsuarioMapper;
import br.com.fiap.clyvovet.model.Perfil;
import br.com.fiap.clyvovet.model.Tutor;
import br.com.fiap.clyvovet.model.Usuario;
import br.com.fiap.clyvovet.repository.TutorRepository;
import br.com.fiap.clyvovet.repository.UsuarioRepository;
import br.com.fiap.clyvovet.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Cadastro de usuarios: as duas portas de entrada e a consulta de perfil.
 * Quem autentica e o {@link AuthService}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Auto-cadastro publico. O perfil e sempre TUTOR, nunca vem da requisicao.
     *
     * O REGISTRO CRIA O TUTOR, e isso corrige dois defeitos de uma vez.
     *
     * X12 — antes, so o Usuario era gravado. O tutor_id ficava nulo, e sem ele
     * SegurancaService.podeAcessar() devolve false em tudo: quem se cadastrava
     * nao conseguia sequer cadastrar o proprio animal. O produto comeca pelo
     * auto-cadastro e o auto-cadastro nao levava a lugar nenhum.
     *
     * X13 — antes, o tutorId vinha do corpo, nesta rota que e publica e nao
     * autenticada. Era escalacao de acesso a dado de terceiro por um campo de
     * formulario. Criar em vez de apontar fecha o vetor: nao ha mais para onde
     * apontar.
     */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        Usuario usuario = novoUsuario(request.getEmail(), request.getSenha(), Perfil.TUTOR);
        usuario.setTutor(novoTutor(request.getNome(), request.getEmail()));
        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    /** Criacao com perfil arbitrario. Restrito a ADMIN pela regra de rota. */
    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        Usuario usuario = novoUsuario(request.getEmail(), request.getSenha(), request.getPerfil());

        if (request.getTutorId() != null) {
            usuario.setTutor(tutorRepository.obterPorId(request.getTutorId()));
        }
        if (request.getVeterinarioId() != null) {
            usuario.setVeterinario(veterinarioRepository.obterPorId(request.getVeterinarioId()));
        }

        validarVinculo(usuario);
        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    public UsuarioResponse buscarPorId(UUID id) {
        return usuarioMapper.toResponse(usuarioRepository.obterPorId(id));
    }

    private Usuario novoUsuario(String email, String senha, Perfil perfil) {
        garantirEmailDisponivel(email);

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuario;
    }

    /**
     * Cria o Tutor do usuario que acabou de se registrar.
     *
     * DELIBERADAMENTE NAO VINCULA A UM TUTOR EXISTENTE de mesmo e-mail, mesmo
     * quando ele existe — recusa com 409. O vinculo automatico parece gentil
     * (a clinica ja cadastrou a pessoa; bastaria reconhece-la), mas sem
     * verificacao de e-mail ele reabre o X13 por outra porta, e por uma porta
     * mais larga: adivinhar um e-mail e muito mais facil que adivinhar um UUID.
     *
     * Vincular conta a cadastro preexistente e um fluxo legitimo, mas exige
     * confirmacao de posse do e-mail. Enquanto isso nao existir, a recusa
     * explicita e a resposta honesta.
     */
    private Tutor novoTutor(String nome, String email) {
        if (tutorRepository.existsByEmail(email)) {
            throw new RegraDeNegocioException("email",
                    "Ja existe um cadastro de tutor com este e-mail. Procure a clinica para vincular sua conta");
        }
        Tutor tutor = new Tutor();
        tutor.setNome(nome);
        tutor.setEmail(email);
        return tutorRepository.save(tutor);
    }

    private void garantirEmailDisponivel(String email) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new RegraDeNegocioException("email", "Ja existe usuario com o e-mail informado");
        }
    }

    /**
     * O perfil determina o vinculo, nos dois sentidos.
     *
     * Recusar o vinculo CRUZADO — um TUTOR apontando para veterinario, e
     * vice-versa — ja era feito aqui. O que faltava era recusar o AUSENTE, e a
     * assimetria custava caro: {"perfil":"VETERINARIO"} sem veterinarioId era
     * aceito, e o usuario nascia valido e sem lastro. Como o recorte de acesso
     * se resolve pelo vinculo, esse usuario nao ficava sem enxergar nada;
     * ficava enxergando tudo.
     *
     * O {@link br.com.fiap.clyvovet.security.RecorteDeAcesso} hoje trata a
     * falta de vinculo como quem nao alcanca nada, e continua sendo a garantia
     * que vale para linha vinda de qualquer lugar — migration, correcao manual
     * no banco, bug futuro. Esta validacao e a outra metade: o cadastro nao
     * deveria produzir essa linha para comecar.
     *
     * O ADMIN fica de fora dos dois lados. Ele nao precisa de vinculo, e um
     * vinculo nele e inerte: o recorte do ADMIN e irrestrito de qualquer forma.
     */
    private void validarVinculo(Usuario usuario) {
        Perfil perfil = usuario.getPerfil();

        if (perfil == Perfil.TUTOR && usuario.getTutor() == null) {
            throw new RegraDeNegocioException("tutorId",
                    "Usuario com perfil TUTOR precisa ser vinculado a um tutor");
        }
        if (perfil == Perfil.TUTOR && usuario.getVeterinario() != null) {
            throw new RegraDeNegocioException("veterinarioId",
                    "Usuario com perfil TUTOR nao pode ser vinculado a um veterinario");
        }
        if (perfil == Perfil.VETERINARIO && usuario.getVeterinario() == null) {
            throw new RegraDeNegocioException("veterinarioId",
                    "Usuario com perfil VETERINARIO precisa ser vinculado a um veterinario");
        }
        if (perfil == Perfil.VETERINARIO && usuario.getTutor() != null) {
            throw new RegraDeNegocioException("tutorId",
                    "Usuario com perfil VETERINARIO nao pode ser vinculado a um tutor");
        }
    }
}
