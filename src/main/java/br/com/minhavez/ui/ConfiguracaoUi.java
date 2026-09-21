package br.com.minhavez.ui;

/** Valores validados escolhidos na tela inicial. */
public record ConfiguracaoUi(int quantidadeVeiculos, int quantidadeDias, long semente) {
    public ConfiguracaoUi {
        if (quantidadeVeiculos <= 0) throw new IllegalArgumentException("A quantidade de veículos deve ser positiva.");
        if (quantidadeDias != 1 && quantidadeDias != 7 && quantidadeDias != 28) {
            throw new IllegalArgumentException("O período deve ser 1, 7 ou 28 dias.");
        }
    }
}
