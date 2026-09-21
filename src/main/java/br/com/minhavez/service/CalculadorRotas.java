package br.com.minhavez.service;

import br.com.minhavez.model.*;
import java.util.*;

/** Dijkstra com peso em minutos. Não modifica a cidade nem recalcula rotas em movimento. */
public class CalculadorRotas {
    private record Candidato(Ponto ponto, double tempo) { }

    /** Em empates, usa o código do ponto para tornar o resultado reproduzível. */
    public Rota calcularMelhorRota(Cidade cidade, Ponto origem, Ponto destino) {
        Objects.requireNonNull(cidade);
        if (!cidade.getPontos().contains(origem) || !cidade.getPontos().contains(destino)) {
            throw new IllegalArgumentException("Origem e destino devem pertencer à cidade");
        }
        Map<Ponto, Double> tempos = new HashMap<>();
        Map<Ponto, Via> anteriores = new HashMap<>();
        PriorityQueue<Candidato> fila = new PriorityQueue<>(Comparator.comparingDouble(Candidato::tempo)
                .thenComparing(c -> c.ponto().getCodigo()));
        tempos.put(origem, 0.0);
        fila.add(new Candidato(origem, 0));
        while (!fila.isEmpty()) {
            Candidato atual = fila.remove();
            if (atual.tempo() > tempos.getOrDefault(atual.ponto(), Double.POSITIVE_INFINITY)) continue;
            if (atual.ponto().equals(destino)) break;
            for (Via via : cidade.getViasDe(atual.ponto())) {
                Ponto vizinho = via.getOutroPonto(atual.ponto());
                double novoTempo = atual.tempo() + via.calcularTempoEstimadoMinutos();
                if (novoTempo < tempos.getOrDefault(vizinho, Double.POSITIVE_INFINITY)) {
                    tempos.put(vizinho, novoTempo);
                    anteriores.put(vizinho, via);
                    fila.add(new Candidato(vizinho, novoTempo));
                }
            }
        }
        if (!tempos.containsKey(destino)) throw new IllegalStateException("Não existe caminho até " + destino);
        List<Via> caminho = new ArrayList<>();
        Ponto atual = destino;
        while (!atual.equals(origem)) {
            Via via = anteriores.get(atual);
            caminho.add(via);
            atual = via.getOutroPonto(atual);
        }
        Collections.reverse(caminho);
        return new Rota(origem, destino, caminho);
    }
}
