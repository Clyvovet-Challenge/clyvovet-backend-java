package br.com.fiap.clyvovet.fluxo;

import br.com.fiap.clyvovet.controller.DocumentoClinicoController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O interruptor de aborto dos anexos, verificado.
 *
 * <p>A feature e nova e guarda binario no banco. Se ela der problema em
 * producao, desligar nao deveria depender de um {@code git revert} as pressas —
 * {@code clyvovet.documentos.habilitado=false} tem que bastar.</p>
 *
 * <p>Uma promessa dessas envelhece em silencio: basta alguem trocar o
 * {@code @ConditionalOnProperty} por um {@code @Value} injetado, e a flag passa a
 * nao desligar nada enquanto continua parecendo que desliga. Este teste sobe um
 * contexto com a propriedade em {@code false} e verifica as duas coisas que
 * importam: o controller nao existe, e <b>nenhuma rota</b> de documento esta
 * registrada.</p>
 *
 * <p>Custa um contexto extra na suite. E o preco de saber que o plano de aborto
 * funciona antes de precisar dele.</p>
 */
@SpringBootTest(properties = "clyvovet.documentos.habilitado=false")
class DocumentosDesligadosTest {

    @Autowired
    private ApplicationContext contexto;

    // Qualificado: o Actuator registra um segundo bean deste tipo.
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mapeamento;

    @Test
    @DisplayName("com a flag em false, o controller de documentos sai do contexto")
    void oControllerNaoEInstanciado() {
        assertThat(contexto.getBeanNamesForType(DocumentoClinicoController.class))
                .as("o @ConditionalOnProperty precisa apagar o bean, e nao apenas mudar o comportamento dele")
                .isEmpty();
    }

    @Test
    @DisplayName("com a flag em false, nenhuma rota de documento responde")
    void nenhumaRotaDeDocumentoFicaRegistrada() {
        assertThat(mapeamento.getHandlerMethods().keySet().stream()
                .map(Object::toString)
                .filter(rota -> rota.contains("/documentos"))
                .toList())
                .as("bean ausente e rota viva significaria que o desligamento nao desliga")
                .isEmpty();
    }

    @Test
    @DisplayName("o resto da API continua no ar")
    void oRestoDaApiNaoDependeDaFlag() {
        // O desligamento tem de ser cirurgico: uma flag que derruba o historico
        // junto com os anexos nao e plano de aborto, e uma segunda falha.
        assertThat(mapeamento.getHandlerMethods().keySet().stream()
                .map(Object::toString)
                .filter(rota -> rota.contains("/historico"))
                .toList())
                .isNotEmpty();
    }
}
