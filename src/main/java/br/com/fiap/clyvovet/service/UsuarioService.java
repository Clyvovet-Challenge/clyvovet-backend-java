package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.auth.RegistroRequest;
import br.com.fiap.clyvovet.dto.auth.UsuarioRequest;
import br.com.fiap.clyvovet.dto.auth.UsuarioResponse;
import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.mapper.UsuarioMapper;
import br.com.fiap.clyvovet.model.Perfil;
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

    /** Auto-cadastro publico. O perfil e sempre TUTOR, nunca vem da requisicao. */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        Usuario usuario = novoUsuario(request.getEmail(), request.getSenha(), Perfil.TUTOR);

        if (request.getTutorId() != null) {
            usuario.setTutor(tutorRepository.obterPorId(request.getTutorId()));
        }

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

    private void garantirEmailDisponivel(String email) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new RegraDeNegocioException("email", "Ja existe usuario com o e-mail informado");
        }
    }

    private void validarVinculo(Usuario usuario) {
        if (usuario.getPerfil() == Perfil.TUTOR && usuario.getVeterinario() != null) {
            throw new RegraDeNegocioException("veterinarioId",
                    "Usuario com perfil TUTOR nao pode ser vinculado a um veterinario");
        }
        if (usuario.getPerfil() == Perfil.VETERINARIO && usuario.getTutor() != null) {
            throw new RegraDeNegocioException("tutorId",
                    "Usuario com perfil VETERINARIO nao pode ser vinculado a um tutor");
        }
    }
}
