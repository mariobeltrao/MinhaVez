package br.com.minhavez.snapshot;

@FunctionalInterface
public interface SimulacaoObserver {
    SimulacaoObserver IGNORAR = snapshot -> { };
    void aoAtualizar(SnapshotSimulacao snapshot);
}
