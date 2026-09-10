package br.com.fiap.clyvovet.repository.projecao;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um documento clinico SEM o arquivo.
 *
 * <p>Existe porque a entidade {@code DocumentoClinico} carrega o BLOB junto: uma
 * listagem de dez laudos de 8 MB traria oitenta megabytes para desenhar dez
 * linhas de texto. A projecao e o que mantem a tela barata — o byte so sai do
 * banco no download, um arquivo por vez.</p>
 *
 * @param clinicaNome nulo quando quem enviou foi o proprio tutor
 * @param eventoId    nulo quando o documento nao esta ligado a um atendimento
 */
public record DocumentoResumo(
        UUID id,
        String titulo,
        String nomeArquivo,
        String tipoConteudo,
        Long tamanhoBytes,
        LocalDateTime enviadoEm,
        String enviadoPorEmail,
        String clinicaNome,
        UUID eventoId) {
}
