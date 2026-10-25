package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.model.Usuario;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

/**
 * Quem entrou pelas telas com Google ou GitHub.
 *
 * <p>E um {@link UsuarioAutenticado} -- e essa e a peca central: o
 * {@code SegurancaService} so reconhece esse tipo de principal, entao o
 * {@code @seguranca} dos controllers, o recorte por tutor e por clinica e o
 * cabecalho das telas funcionam do mesmo jeito para quem entrou por senha.</p>
 *
 * <p>Implementa {@link OidcUser} porque e o que o Spring espera receber de volta
 * do Google (OIDC). Como {@code OidcUser} estende {@link OAuth2User}, o mesmo tipo
 * serve ao GitHub, que nao tem token de identidade: la, os metodos de OIDC
 * devolvem {@code null}.</p>
 */
public class UsuarioOAuth2 extends UsuarioAutenticado implements OidcUser {

    private final transient OAuth2User contaExterna;

    public UsuarioOAuth2(Usuario usuario, OAuth2User contaExterna) {
        super(usuario);
        this.contaExterna = contaExterna;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return contaExterna.getAttributes();
    }

    @Override
    public Map<String, Object> getClaims() {
        return contaExterna instanceof OidcUser oidc ? oidc.getClaims() : contaExterna.getAttributes();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return contaExterna instanceof OidcUser oidc ? oidc.getUserInfo() : null;
    }

    @Override
    public OidcIdToken getIdToken() {
        return contaExterna instanceof OidcUser oidc ? oidc.getIdToken() : null;
    }

    /** O nome que o Spring usa para o principal: o e-mail daqui, nao o id do provedor. */
    @Override
    public String getName() {
        return getUsername();
    }
}
