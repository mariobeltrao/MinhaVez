package br.com.minhavez.enums;

import br.com.minhavez.ConfiguracaoSimulacao;

public enum EstadoTransito {
    LIVRE(ConfiguracaoSimulacao.FATOR_LIVRE),
    MODERADO(ConfiguracaoSimulacao.FATOR_MODERADO),
    INTENSO(ConfiguracaoSimulacao.FATOR_INTENSO),
    CONGESTIONADO(ConfiguracaoSimulacao.FATOR_CONGESTIONADO),
    SEVERO(ConfiguracaoSimulacao.FATOR_SEVERO),
    COLAPSO(ConfiguracaoSimulacao.FATOR_COLAPSO);

    private final double fatorVelocidade;

    EstadoTransito(double fatorVelocidade) { this.fatorVelocidade = fatorVelocidade; }
    public double getFatorVelocidade() { return fatorVelocidade; }

    public static EstadoTransito paraOcupacao(double ocupacao) {
        if (!Double.isFinite(ocupacao) || ocupacao < 0) {
            throw new IllegalArgumentException("Ocupação deve ser finita e não negativa");
        }
        if (ocupacao <= 0.50) return LIVRE;
        if (ocupacao <= 0.80) return MODERADO;
        if (ocupacao <= 1.00) return INTENSO;
        if (ocupacao <= 1.20) return CONGESTIONADO;
        if (ocupacao <= 1.50) return SEVERO;
        return COLAPSO;
    }
}
