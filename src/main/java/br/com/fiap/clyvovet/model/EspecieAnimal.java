package br.com.fiap.clyvovet.model;

/**
 * As especies que o catalogo de racas cobre.
 *
 * E fechada e pequena, ao contrario de raca -- por isso ela e enum e raca e
 * tabela. Acrescentar uma especie e mudanca de dominio (envolve arte, catalogo
 * e provavelmente as predisposicoes da API .NET); acrescentar uma raca e um
 * INSERT.
 *
 * NAO substitui `Animal.especie`, que continua String. Trocar o tipo daquela
 * coluna quebraria o widget de saude preditiva da .NET, que le a especie como
 * texto, e o `AnimalResponse` que o app ja consome. Aqui o enum serve para
 * classificar o CATALOGO; o texto do animal passa a ser derivado dele.
 */
public enum EspecieAnimal {
    CAO,
    GATO,
    ROEDOR,
    AVE,
    REPTIL;

    /**
     * O rotulo que vai para `Animal.especie` e chega ao app.
     *
     * Existe porque o app ja tinha a lista dele antes deste catalogo
     * ("Cachorro", "Gato", "Passaro", "Reptil", "Roedor") e mudar aquilo
     * quebraria os cadastros existentes sem ganho nenhum. Entao o catalogo fala
     * CAO por dentro e escreve "Cachorro" para fora.
     */
    public String rotulo() {
        return switch (this) {
            case CAO -> "Cachorro";
            case GATO -> "Gato";
            case ROEDOR -> "Roedor";
            case AVE -> "Passaro";
            case REPTIL -> "Reptil";
        };
    }
}
