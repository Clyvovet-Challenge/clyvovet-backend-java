package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.model.Clinica;
import br.com.fiap.clyvovet.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador entre a entidade Usuario e o contrato UserDetails do Spring Security.
 *
 * Manter a entidade acessivel (getUsuario) e o que permite ao SegurancaService
 * resolver ownership sem uma segunda ida ao banco a cada requisicao.
 */
public class UsuarioAutenticado implements UserDetails {

    private final transient Usuario usuario;

    public UsuarioAutenticado(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public UUID getId() {
        return usuario.getId();
    }

    /** Id do tutor vinculado, ou null se o usuario nao for um tutor. */
    public UUID getTutorId() {
        return usuario.getTutor() != null ? usuario.getTutor().getId() : null;
    }

    /**
     * Id do veterinario vinculado, ou null se o usuario nao for um veterinario.
     *
     * O vinculo Usuario -> Veterinario existia desde o inicio e era gravado no
     * cadastro, mas NADA o lia: so o lado do tutor tinha getter, e por isso so
     * o tutor tinha escopo. O veterinario ou via a base inteira ou nao via nada.
     *
     * Sem este metodo nao existe "minha agenda", nem "meus atendimentos", nem
     * metrica por profissional; e o evento clinico nasce atribuido a quem o
     * corpo da requisicao mandar, e nao a quem o registrou.
     */
    public UUID getVeterinarioId() {
        return usuario.getVeterinario() != null ? usuario.getVeterinario().getId() : null;
    }

    /**
     * A clinica deste usuario. Sustenta as regras de escopo por clinica.
     *
     * <p>Ha DOIS caminhos, e a ordem importa. O vinculo direto
     * ({@code usuario.clinica}) e o do ADMIN_CLINICA, que responde pelo
     * estabelecimento. O transitivo ({@code veterinario.clinica}) e o do
     * profissional que atende nela.</p>
     *
     * <p>O direto vem primeiro porque e o mais especifico: se um dia alguem for as
     * duas coisas — o dono da clinica que tambem atende —, o alcance que vale e o
     * de quem responde pelo negocio, nao o de quem faz consulta.</p>
     *
     * <p>A resolucao em si mora em {@link Usuario#getClinicaEfetiva()}: o
     * {@code /auth/me} precisa da mesma resposta, e duas copias dela sao duas
     * chances de divergir.</p>
     */
    public UUID getClinicaId() {
        Clinica clinica = usuario.getClinicaEfetiva();
        return clinica != null ? clinica.getId() : null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // O prefixo ROLE_ e o que faz hasRole("ADMIN") funcionar nas regras.
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name()));
    }

    @Override
    public String getPassword() {
        return usuario.getSenha();
    }

    @Override
    public String getUsername() {
        return usuario.getEmail();
    }

    @Override
    public boolean isAccountNonLocked() {
        return !usuario.estaBloqueado();
    }

    @Override
    public boolean isEnabled() {
        return usuario.isAtivo();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
