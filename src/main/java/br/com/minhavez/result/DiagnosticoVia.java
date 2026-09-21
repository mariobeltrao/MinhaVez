package br.com.minhavez.result;

/** Acumuladores diagnósticos de uma via; não participam da física da simulação. */
public record DiagnosticoVia(String viaId, double somaOcupacaoPercentual, double ocupacaoMaximaPercentual,
                             long totalEntradas, long amostrasAcimaCapacidade, long quantidadeAmostras) {
    public DiagnosticoVia {
        if (viaId == null || viaId.isBlank() || !Double.isFinite(somaOcupacaoPercentual)
                || !Double.isFinite(ocupacaoMaximaPercentual) || somaOcupacaoPercentual < 0
                || ocupacaoMaximaPercentual < 0 || totalEntradas < 0 || amostrasAcimaCapacidade < 0
                || quantidadeAmostras < 0) throw new IllegalArgumentException("Diagnóstico de via inválido");
    }
    public double ocupacaoMediaPercentual() {
        return quantidadeAmostras == 0 ? 0 : somaOcupacaoPercentual / quantidadeAmostras;
    }
    public double percentualTempoAcimaCapacidade() {
        return quantidadeAmostras == 0 ? 0 : amostrasAcimaCapacidade * 100.0 / quantidadeAmostras;
    }
}
