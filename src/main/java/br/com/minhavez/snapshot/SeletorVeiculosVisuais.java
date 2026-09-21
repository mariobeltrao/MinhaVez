package br.com.minhavez.snapshot;

import br.com.minhavez.model.Veiculo;

import java.util.Comparator;
import java.util.List;

/** Mantém a amostra visual estável: os menores IDs que estão em movimento. */
public final class SeletorVeiculosVisuais {
    public static final int LIMITE_PADRAO = 125;

    public List<Veiculo> selecionar(List<Veiculo> veiculos, int limite) {
        if (limite < 0) throw new IllegalArgumentException("Limite não pode ser negativo");
        return veiculos.stream().filter(Veiculo::estaEmMovimento)
                .sorted(Comparator.comparingInt(Veiculo::getId)).limit(limite).toList();
    }
}
