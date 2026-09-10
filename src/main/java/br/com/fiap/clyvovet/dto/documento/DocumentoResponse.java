package br.com.fiap.clyvovet.dto.documento;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um documento do prontuario, do ponto de vista de quem lista.
 *
 * <p>Nao carrega o arquivo. O byte sai por
 * {@code GET /animais/{animalId}/documentos/{id}/arquivo}, uma requisicao por
 * arquivo, para que a lista continue custando alguns kilobytes.</p>
 *
 * @param origem "CLINICA" ou "TUTOR". A tela precisa da diferenca: um laudo que
 *               a clinica anexou e registro clinico, e o que o tutor anexou e
 *               documento trazido de fora — a mesma lista, com procedencias que
 *               nao valem o mesmo. Derivar isso de {@code clinicaNome == null}
 *               no cliente funcionaria hoje e viraria regra escondida amanha.
 */
public record DocumentoResponse(
        UUID id,
        String titulo,
        String nomeArquivo,
        String tipoConteudo,
        Long tamanhoBytes,
        LocalDateTime enviadoEm,
        String enviadoPorEmail,
        String clinicaNome,
        UUID eventoId,
        String origem) {
}
