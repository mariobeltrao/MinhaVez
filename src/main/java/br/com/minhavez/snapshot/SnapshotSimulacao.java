package br.com.minhavez.snapshot;

import br.com.minhavez.enums.GrupoRodizio;
import br.com.minhavez.enums.Regiao;

import java.util.List;
import java.util.Map;

/** Estado completo e imutável entregue aos consumidores externos ao motor. */
public record SnapshotSimulacao(int diaAtual, int quantidadeDias, long tempoAtualSegundos,
                                boolean rodizioAtivo, GrupoRodizio grupoRestrito,
                                int quantidadeVeiculosCirculando, int quantidadeVeiculosRestritos,
                                double velocidadeMediaInstantaneaKmh,
                                double ocupacaoMediaInstantaneaPercentual,
                                int quantidadeViasAcimaCapacidade,
                                Map<Regiao, Double> ocupacaoPorRegiao,
                                List<SnapshotVia> vias, List<SnapshotVeiculo> veiculosVisiveis) {
    public SnapshotSimulacao {
        ocupacaoPorRegiao = Map.copyOf(ocupacaoPorRegiao);
        vias = List.copyOf(vias);
        veiculosVisiveis = List.copyOf(veiculosVisiveis);
    }
}
