package br.com.minhavez.ui.runner;

import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/** Checkpoints visuais para replay; não modifica nem tenta reverter o motor. */
public final class HistoricoSnapshots {
    public static final long INICIO_DIA_SEGUNDOS = 5 * 3600L;
    public static final long PASSO_SIMULACAO_SEGUNDOS = 30L;
    public static final long TICKS_POR_DIA = 18 * 3600L / PASSO_SIMULACAO_SEGUNDOS;
    public static final long INTERVALO_CHECKPOINT_SEGUNDOS = 5 * 60L;

    private final NavigableMap<Long, SnapshotComparacao> checkpoints = new TreeMap<>();
    private SnapshotComparacao maisRecente;

    public synchronized void registrar(SnapshotComparacao snapshot) {
        maisRecente = snapshot;
        long posicao = snapshot.posicaoTimeline();
        long tempoNoDia = Math.max(0,
                snapshot.semRodizio().tempoAtualSegundos() - INICIO_DIA_SEGUNDOS);
        if (checkpoints.isEmpty() || tempoNoDia == PASSO_SIMULACAO_SEGUNDOS
                || tempoNoDia % INTERVALO_CHECKPOINT_SEGUNDOS == 0) {
            checkpoints.put(posicao, snapshot);
        }
    }

    public synchronized Optional<SnapshotComparacao> buscar(long posicao) {
        if (maisRecente == null) return Optional.empty();
        if (posicao >= maisRecente.posicaoTimeline()) return Optional.of(maisRecente);
        var entrada = checkpoints.floorEntry(Math.max(0, posicao));
        return Optional.ofNullable(entrada == null ? checkpoints.firstEntry().getValue() : entrada.getValue());
    }

    public synchronized Optional<SnapshotComparacao> maisRecente() {
        return Optional.ofNullable(maisRecente);
    }

    public synchronized long posicaoMaxima() {
        return maisRecente == null ? 0 : maisRecente.posicaoTimeline();
    }

    public synchronized int quantidadeCheckpoints() { return checkpoints.size(); }

    public synchronized void limpar() {
        checkpoints.clear();
        maisRecente = null;
    }
}
