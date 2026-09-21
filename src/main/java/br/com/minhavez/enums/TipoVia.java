package br.com.minhavez.enums;

import br.com.minhavez.ConfiguracaoSimulacao;

public enum TipoVia {
    PRINCIPAL(ConfiguracaoSimulacao.CAPACIDADE_VIA_PRINCIPAL,
            ConfiguracaoSimulacao.VELOCIDADE_VIA_PRINCIPAL_KMH),
    SECUNDARIA(ConfiguracaoSimulacao.CAPACIDADE_VIA_SECUNDARIA,
            ConfiguracaoSimulacao.VELOCIDADE_VIA_SECUNDARIA_KMH);

    private final int capacidade;
    private final double velocidadeBaseKmh;

    TipoVia(int capacidade, double velocidadeBaseKmh) {
        this.capacidade = capacidade;
        this.velocidadeBaseKmh = velocidadeBaseKmh;
    }

    public int getCapacidade() { return capacidade; }
    public double getVelocidadeBaseKmh() { return velocidadeBaseKmh; }
}
