package br.com.fiap.clyvovet.security;

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
