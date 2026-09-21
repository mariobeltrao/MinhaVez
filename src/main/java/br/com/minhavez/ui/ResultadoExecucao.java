package br.com.minhavez.ui;

import br.com.minhavez.result.ResultadoSimulacao;

import java.util.Objects;

public record ResultadoExecucao(ConfiguracaoUi configuracao, ResultadoSimulacao semRodizio,
                                ResultadoSimulacao comRodizio) {
    public ResultadoExecucao {
        Objects.requireNonNull(configuracao);
        Objects.requireNonNull(semRodizio);
        Objects.requireNonNull(comRodizio);
    }
}
