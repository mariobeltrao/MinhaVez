package br.com.minhavez.ui.runner;

import br.com.minhavez.snapshot.SnapshotSimulacao;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HistoricoSnapshotsTest {
    @Test void replayUsaCheckpointAnteriorENaoPermiteFuturo() {
        HistoricoSnapshots historico = new HistoricoSnapshots();
        SnapshotComparacao cinco = par(0, 5 * 3600 + 5 * 60);
        SnapshotComparacao dez = par(0, 5 * 3600 + 10 * 60);
        historico.registrar(cinco);
        historico.registrar(dez);

        assertSame(cinco, historico.buscar(cinco.posicaoTimeline() + 4).orElseThrow());
        assertSame(dez, historico.buscar(dez.posicaoTimeline() + 100).orElseThrow());
        assertEquals(dez.posicaoTimeline(), historico.posicaoMaxima());
        assertEquals(2, historico.quantidadeCheckpoints());
    }

    @Test void rejeitaComparacaoDeInstantesDiferentes() {
        assertThrows(IllegalArgumentException.class,
                () -> new SnapshotComparacao(snapshot(0, 18_030, false), snapshot(0, 18_060, true)));
    }

    @Test void primeiroTickDeCadaDiaTambemViraCheckpoint() {
        HistoricoSnapshots historico = new HistoricoSnapshots();
        SnapshotComparacao fimDiaUm = par(0, 23 * 3600);
        SnapshotComparacao inicioDiaDois = par(1, 5 * 3600 + 30);
        historico.registrar(fimDiaUm);
        historico.registrar(inicioDiaDois);

        assertNotEquals(fimDiaUm.posicaoTimeline(), inicioDiaDois.posicaoTimeline());
        assertSame(inicioDiaDois, historico.buscar(inicioDiaDois.posicaoTimeline()).orElseThrow());
    }

    private SnapshotComparacao par(int dia, long tempo) {
        return new SnapshotComparacao(snapshot(dia, tempo, false), snapshot(dia, tempo, true));
    }

    private SnapshotSimulacao snapshot(int dia, long tempo, boolean rodizio) {
        return new SnapshotSimulacao(dia, 1, tempo, rodizio, null, 0, 0,
                0, 0, 0, Map.of(), List.of(), List.of());
    }
}
