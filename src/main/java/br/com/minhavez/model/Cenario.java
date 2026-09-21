package br.com.minhavez.model;

import java.util.*;

public final class Cenario {
    private final Cidade cidade;
    private final List<Veiculo> veiculos;
    private final int quantidadeDias;
    private final long semente;

    public Cenario(Cidade cidade, List<Veiculo> veiculos, int quantidadeDias, long semente) {
        this.cidade = Objects.requireNonNull(cidade);
        this.veiculos = List.copyOf(veiculos);
        if (quantidadeDias <= 0) throw new IllegalArgumentException("Quantidade de dias deve ser positiva");
        Set<Integer> ids = new HashSet<>();
        Set<String> placas = new HashSet<>();
        for (Veiculo veiculo : this.veiculos) {
            if (!ids.add(veiculo.getId()) || !placas.add(veiculo.getPlaca())) throw new IllegalArgumentException("Veículos duplicados");
            if (!cidade.getPontos().contains(veiculo.getResidencia()) || !cidade.getPontos().contains(veiculo.getDestino())) {
                throw new IllegalArgumentException("Rotina fora da cidade");
            }
        }
        this.quantidadeDias = quantidadeDias;
        this.semente = semente;
    }
    /** Copia a topologia e as rotinas; nenhum estado de circulação é reaproveitado. */
    public Cenario copiarParaExecucao() {
        Cidade copia = new Cidade();
        for (Ponto ponto : cidade.getPontos()) copia.adicionarPonto(new Ponto(ponto.getCodigo(), ponto.getRegiao()));
        for (Via via : cidade.getVias()) {
            copia.adicionarVia(new Via(via.getId(), pontoCopiado(copia, via.getPontoA()),
                    pontoCopiado(copia, via.getPontoB()), via.getTipo(), via.getDistanciaKm()));
        }
        List<Veiculo> novos = veiculos.stream().map(v -> new Veiculo(v.getId(), v.getPlaca(), v.getGrupoRodizio(),
                pontoCopiado(copia, v.getResidencia()), pontoCopiado(copia, v.getDestino()),
                v.getHorarioSaida(), v.getHorarioRetorno())).toList();
        return new Cenario(copia, novos, quantidadeDias, semente);
    }
    private Ponto pontoCopiado(Cidade cidade, Ponto ponto) { return cidade.buscarPonto(ponto.getCodigo()).orElseThrow(); }
    public Cidade getCidade() { return cidade; }
    public List<Veiculo> getVeiculos() { return veiculos; }
    public int getQuantidadeDias() { return quantidadeDias; }
    public long getSemente() { return semente; }
}
