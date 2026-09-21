package br.com.minhavez.model;

import java.util.List;
import java.util.Objects;

/** Sequência fixa de vias; o tempo estimado é preservado no instante da escolha. */
public final class Rota {
    private final Ponto origem;
    private final Ponto destino;
    private final List<Via> vias;
    private final double distanciaTotalKm;
    private final double tempoEstimadoMinutos;

    public Rota(Ponto origem, Ponto destino, List<Via> vias) {
        this.origem = Objects.requireNonNull(origem);
        this.destino = Objects.requireNonNull(destino);
        this.vias = List.copyOf(vias);
        Ponto atual = origem;
        for (Via via : this.vias) atual = via.getOutroPonto(atual);
        if (!atual.equals(destino)) throw new IllegalArgumentException("Rota não termina no destino");
        distanciaTotalKm = this.vias.stream().mapToDouble(Via::getDistanciaKm).sum();
        tempoEstimadoMinutos = this.vias.stream().mapToDouble(Via::calcularTempoEstimadoMinutos).sum();
    }
    public Ponto getOrigem() { return origem; }
    public Ponto getDestino() { return destino; }
    public List<Via> getVias() { return vias; }
    public Via getVia(int indice) { return vias.get(indice); }
    public int getQuantidadeVias() { return vias.size(); }
    public double getDistanciaTotalKm() { return distanciaTotalKm; }
    public double getTempoEstimadoMinutos() { return tempoEstimadoMinutos; }
    public boolean isVazia() { return vias.isEmpty(); }
}
