package br.com.minhavez.ui.runner;

import br.com.minhavez.snapshot.SnapshotSimulacao;

import java.util.Objects;

/** Dois cenários no mesmo instante lógico da simulação. */
public record SnapshotComparacao(SnapshotSimulacao semRodizio, SnapshotSimulacao comRodizio) {
    public SnapshotComparacao {
        Objects.requireNonNull(semRodizio);
        Objects.requireNonNull(comRodizio);
        if (semRodizio.diaAtual() != comRodizio.diaAtual()
                || semRodizio.tempoAtualSegundos() != comRodizio.tempoAtualSegundos()) {
            throw new IllegalArgumentException("Os snapshots comparados devem representar o mesmo instante.");
        }
    }

    public long posicaoTimeline() {
        long tickNoDia = Math.min(HistoricoSnapshots.TICKS_POR_DIA,
                Math.max(0, semRodizio.tempoAtualSegundos() - HistoricoSnapshots.INICIO_DIA_SEGUNDOS)
                        / HistoricoSnapshots.PASSO_SIMULACAO_SEGUNDOS);
        return semRodizio.diaAtual() * HistoricoSnapshots.TICKS_POR_DIA + tickNoDia;
    }
}
