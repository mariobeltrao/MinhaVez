package br.com.minhavez.model;

import java.util.*;

public final class Cidade {
    private final Map<String, Ponto> pontosPorCodigo = new LinkedHashMap<>();
    private final List<Via> vias = new ArrayList<>();
    private final Map<Ponto, List<Via>> adjacencias = new LinkedHashMap<>();

    public void adicionarPonto(Ponto ponto) {
        Objects.requireNonNull(ponto);
        if (pontosPorCodigo.containsKey(ponto.getCodigo())) throw new IllegalArgumentException("Código duplicado");
        pontosPorCodigo.put(ponto.getCodigo(), ponto);
        adjacencias.put(ponto, new ArrayList<>());
    }
    public void adicionarVia(Via via) {
        Objects.requireNonNull(via);
        if (!adjacencias.containsKey(via.getPontoA()) || !adjacencias.containsKey(via.getPontoB())) {
            throw new IllegalArgumentException("Cadastre os pontos antes da via");
        }
        if (vias.stream().anyMatch(v -> v.getId().equals(via.getId()))
                || buscarVia(via.getPontoA(), via.getPontoB()).isPresent()) {
            throw new IllegalArgumentException("Via duplicada");
        }
        vias.add(via);
        adjacencias.get(via.getPontoA()).add(via);
        adjacencias.get(via.getPontoB()).add(via);
    }
    public Optional<Ponto> buscarPonto(String codigo) { return Optional.ofNullable(pontosPorCodigo.get(codigo)); }
    public Optional<Via> buscarVia(Ponto pontoA, Ponto pontoB) {
        return getViasDe(pontoA).stream().filter(v -> v.getOutroPonto(pontoA).equals(pontoB)).findFirst();
    }
    public List<Via> getViasDe(Ponto ponto) { return List.copyOf(adjacencias.getOrDefault(ponto, List.of())); }
    public List<Ponto> getPontosVizinhos(Ponto ponto) {
        return getViasDe(ponto).stream().map(v -> v.getOutroPonto(ponto)).toList();
    }
    public Collection<Ponto> getPontos() { return List.copyOf(pontosPorCodigo.values()); }
    public List<Via> getVias() { return List.copyOf(vias); }
    public void resetarOcupacaoDasVias() { vias.forEach(Via::resetarOcupacao); }
}
