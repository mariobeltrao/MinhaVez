package br.com.minhavez.model;

import br.com.minhavez.ConfiguracaoSimulacao;
import br.com.minhavez.enums.EstadoTransito;
import br.com.minhavez.enums.TipoVia;
import java.util.Objects;

/** Via bidirecional: os dois sentidos compartilham capacidade e ocupação. */
public final class Via {
    private final String id;
    private final Ponto pontoA;
    private final Ponto pontoB;
    private final TipoVia tipo;
    private final double distanciaKm;
    private final int capacidade;
    private final double velocidadeBaseKmh;
    private int quantidadeVeiculos;

    public Via(String id, Ponto pontoA, Ponto pontoB, TipoVia tipo, double distanciaKm) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("ID vazio");
        this.pontoA = Objects.requireNonNull(pontoA);
        this.pontoB = Objects.requireNonNull(pontoB);
        if (pontoA.equals(pontoB)) throw new IllegalArgumentException("Via precisa de dois pontos distintos");
        if (!Double.isFinite(distanciaKm) || distanciaKm <= 0) throw new IllegalArgumentException("Distância inválida");
        this.id = id;
        this.tipo = Objects.requireNonNull(tipo);
        this.distanciaKm = distanciaKm;
        capacidade = tipo.getCapacidade();
        velocidadeBaseKmh = tipo.getVelocidadeBaseKmh();
    }

    public boolean conecta(Ponto ponto) { return pontoA.equals(ponto) || pontoB.equals(ponto); }
    public Ponto getOutroPonto(Ponto atual) {
        if (pontoA.equals(atual)) return pontoB;
        if (pontoB.equals(atual)) return pontoA;
        throw new IllegalArgumentException("Ponto não pertence à via " + id);
    }
    public double calcularTaxaOcupacao() { return (double) quantidadeVeiculos / capacidade; }
    public double calcularOcupacaoPercentual() { return calcularTaxaOcupacao() * 100; }

    public EstadoTransito getEstadoTransito() {
        return EstadoTransito.paraOcupacao(calcularTaxaOcupacao());
    }
    public double calcularFatorVelocidade() { return getEstadoTransito().getFatorVelocidade(); }
    public double calcularVelocidadeAtualKmh() {
        return Math.max(ConfiguracaoSimulacao.VELOCIDADE_MINIMA_KMH,
                velocidadeBaseKmh * calcularFatorVelocidade());
    }
    public double calcularTempoEstimadoMinutos() { return distanciaKm / calcularVelocidadeAtualKmh() * 60; }
    public void entrarVeiculo() { quantidadeVeiculos = Math.incrementExact(quantidadeVeiculos); }
    public void sairVeiculo() {
        if (quantidadeVeiculos == 0) throw new IllegalStateException("Via já está vazia");
        quantidadeVeiculos--;
    }
    public void resetarOcupacao() { quantidadeVeiculos = 0; }
    public String getId() { return id; }
    public Ponto getPontoA() { return pontoA; }
    public Ponto getPontoB() { return pontoB; }
    public TipoVia getTipo() { return tipo; }
    public double getDistanciaKm() { return distanciaKm; }
    public int getCapacidade() { return capacidade; }
    public double getVelocidadeBaseKmh() { return velocidadeBaseKmh; }
    public int getQuantidadeVeiculos() { return quantidadeVeiculos; }
}
