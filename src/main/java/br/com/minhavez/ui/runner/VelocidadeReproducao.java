package br.com.minhavez.ui.runner;

public enum VelocidadeReproducao {
    X1("1x", 500), X2("2x", 250), X4("4x", 125), X8("8x", 63), X16("16x", 31), MAX("MAX", 0);

    private final String rotulo;
    private final long intervaloMilissegundos;

    VelocidadeReproducao(String rotulo, long intervaloMilissegundos) {
        this.rotulo = rotulo;
        this.intervaloMilissegundos = intervaloMilissegundos;
    }
    public String getRotulo() { return rotulo; }
    public long getIntervaloMilissegundos() { return intervaloMilissegundos; }

    public static VelocidadeReproducao porRotulo(String rotulo) {
        for (VelocidadeReproducao valor : values()) if (valor.rotulo.equalsIgnoreCase(rotulo)) return valor;
        throw new IllegalArgumentException("Velocidade desconhecida: " + rotulo);
    }
}
