package br.com.fiap.clyvovet.model;

public enum Perfil {
    TUTOR,
    VETERINARIO,

    /**
     * O administrador de UMA clinica.
     *
     * <p>Nao e o ADMIN abaixo com menos poder: e outro sujeito. O ADMIN e da
     * plataforma e enxerga todas as clinicas; este e o estabelecimento, e so
     * alcanca o proprio. Um veterinario tambem nao serve no lugar dele — ele e um
     * profissional <i>dentro</i> da clinica, e o alcance dele e o dos atendimentos
     * que faz, nao o do negocio.</p>
     *
     * <p>Sem este perfil nao havia a quem pertencer um painel da clinica: nao da
     * para recortar faturamento ou metrica por estabelecimento sem alguem que SEJA
     * o estabelecimento.</p>
     */
    ADMIN_CLINICA,

    /** Da plataforma. Enxerga tudo. */
    ADMIN
}
