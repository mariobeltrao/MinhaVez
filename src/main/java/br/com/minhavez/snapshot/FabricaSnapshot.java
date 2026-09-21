package br.com.minhavez.snapshot;

import br.com.minhavez.enums.*;
import br.com.minhavez.model.*;
import br.com.minhavez.service.SistemaRodizio;

import java.util.*;

/** Converte o estado interno em valores independentes e seguros para outras threads. */
public final class FabricaSnapshot {
    private final SeletorVeiculosVisuais seletor = new SeletorVeiculosVisuais();
    private final SistemaRodizio sistemaRodizio = new SistemaRodizio();

    public SnapshotSimulacao criar(int dia, int quantidadeDias, long tempoAtualSegundos,
                                   boolean rodizioAtivo, Cidade cidade, List<Veiculo> veiculos,
                                   double distanciaTickKm, double segundosMovimentoTick) {
        Objects.requireNonNull(cidade);
        Objects.requireNonNull(veiculos);
        List<Via> vias = cidade.getVias();
        List<SnapshotVia> snapshotsVias = vias.stream().map(this::via).toList();
        List<SnapshotVeiculo> snapshotsVeiculos = seletor
                .selecionar(veiculos, SeletorVeiculosVisuais.LIMITE_PADRAO).stream().map(this::veiculo).toList();
        Map<Regiao, Double> regioes = new EnumMap<>(Regiao.class);
        for (Regiao regiao : Regiao.values()) {
            regioes.put(regiao, vias.stream()
                    .filter(v -> v.getPontoA().getRegiao() == regiao && v.getPontoB().getRegiao() == regiao)
                    .mapToDouble(Via::calcularOcupacaoPercentual).average().orElse(0));
        }
        int circulando = (int) veiculos.stream().filter(Veiculo::estaEmMovimento).count();
        int restritos = (int) veiculos.stream().filter(v -> v.getEstado() == EstadoVeiculo.RESTRITO).count();
        double velocidade = segundosMovimentoTick == 0 ? 0 : distanciaTickKm / (segundosMovimentoTick / 3600);
        double ocupacao = vias.stream().mapToDouble(Via::calcularOcupacaoPercentual).average().orElse(0);
        int acima = (int) vias.stream().filter(v -> v.calcularTaxaOcupacao() > 1).count();
        GrupoRodizio grupo = rodizioAtivo ? sistemaRodizio.getGrupoRestrito(dia) : null;
        return new SnapshotSimulacao(dia, quantidadeDias, tempoAtualSegundos, rodizioAtivo, grupo,
                circulando, restritos, velocidade, ocupacao, acima, regioes, snapshotsVias, snapshotsVeiculos);
    }

    private SnapshotVia via(Via via) {
        return new SnapshotVia(via.getId(), via.getPontoA().getCodigo(), via.getPontoB().getCodigo(),
                via.getTipo(), via.getQuantidadeVeiculos(), via.getCapacidade(),
                via.calcularOcupacaoPercentual(), via.calcularVelocidadeAtualKmh(), via.getEstadoTransito());
    }

    private SnapshotVeiculo veiculo(Veiculo veiculo) {
        Via via = veiculo.getViaAtual();
        Ponto origem = veiculo.getPontoAtual();
        double progresso = Math.clamp(veiculo.getDistanciaPercorridaNaViaKm() / via.getDistanciaKm(), 0, 1);
        return new SnapshotVeiculo(veiculo.getId(), veiculo.getEstado(), via.getId(),
                origem.getCodigo(), via.getOutroPonto(origem).getCodigo(), progresso);
    }
}
