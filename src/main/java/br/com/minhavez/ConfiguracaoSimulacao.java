package br.com.minhavez;

/** Parâmetros da calibração experimental da V1. */
public final class ConfiguracaoSimulacao {
    public static final int QUANTIDADE_VEICULOS = 1000;
    public static final int DIAS_PADRAO = 28;

    public static final int CAPACIDADE_VIA_PRINCIPAL = 15;
    public static final int CAPACIDADE_VIA_SECUNDARIA = 7;
    public static final double VELOCIDADE_VIA_PRINCIPAL_KMH = 60.0;
    public static final double VELOCIDADE_VIA_SECUNDARIA_KMH = 40.0;
    public static final double VELOCIDADE_MINIMA_KMH = 5.0;

    public static final double PROPORCAO_VIAGENS_INTERNAS = 0.20;
    public static final double PROPORCAO_VIAGENS_INTERREGIONAIS = 0.80;

    public static final double FATOR_LIVRE = 1.00;
    public static final double FATOR_MODERADO = 0.80;
    public static final double FATOR_INTENSO = 0.55;
    public static final double FATOR_CONGESTIONADO = 0.35;
    public static final double FATOR_SEVERO = 0.20;
    public static final double FATOR_COLAPSO = 0.08;

    private ConfiguracaoSimulacao() { }
}
