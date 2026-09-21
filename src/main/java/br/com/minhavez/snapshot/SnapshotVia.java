package br.com.minhavez.snapshot;

import br.com.minhavez.enums.EstadoTransito;
import br.com.minhavez.enums.TipoVia;

/** Cópia somente de valores do estado instantâneo de uma via. */
public record SnapshotVia(String id, String pontoA, String pontoB, TipoVia tipo,
                          int quantidadeVeiculos, int capacidade, double ocupacaoPercentual,
                          double velocidadeAtualKmh, EstadoTransito estado) { }
