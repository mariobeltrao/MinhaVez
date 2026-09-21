package br.com.minhavez.snapshot;

import br.com.minhavez.enums.EstadoVeiculo;

/** Posição visual de um veículo, sem referências ao modelo mutável. */
public record SnapshotVeiculo(int id, EstadoVeiculo estado, String viaAtual,
                              String pontoOrigem, String pontoDestino, double progresso) {
    public SnapshotVeiculo {
        if (progresso < 0 || progresso > 1 || !Double.isFinite(progresso)) {
            throw new IllegalArgumentException("Progresso deve estar entre 0 e 1");
        }
    }
}
