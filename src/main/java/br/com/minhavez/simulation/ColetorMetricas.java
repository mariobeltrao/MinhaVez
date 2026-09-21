package br.com.minhavez.simulation;

import br.com.minhavez.enums.*;
import br.com.minhavez.model.*;
import br.com.minhavez.result.ResultadoDia;
import br.com.minhavez.result.DiagnosticoVia;
import java.util.*;

/** Acumula um único dia. Tempos estacionados não entram nas viagens ou na velocidade média. */
public final class ColetorMetricas {
    public static final long INICIO_PICO_SEGUNDOS = 17 * 3600;
    public static final long FIM_PICO_SEGUNDOS = 19 * 3600;
    private final List<Double> temposIdaMinutos = new ArrayList<>();
    private final List<Double> temposVoltaMinutos = new ArrayList<>();
    private final Set<Integer> idsVeiculosQueCircularam = new HashSet<>();
    private final Set<Integer> idsVeiculosRestritos = new HashSet<>();
    private final Map<Regiao, Double> somaCongestionamentoPorRegiao = new EnumMap<>(Regiao.class);
    private final Map<Long, Integer> serieVeiculosCirculando = new LinkedHashMap<>();
    private final Map<Long, Double> serieCongestionamentoCidade = new LinkedHashMap<>();
    private final Map<Long, Double> serieVelocidadeMedia = new LinkedHashMap<>();
    private final Map<String, Double> somaOcupacaoPorVia = new LinkedHashMap<>();
    private final Map<String, Double> ocupacaoMaximaPorVia = new LinkedHashMap<>();
    private final Map<String, Long> amostrasAcimaCapacidadePorVia = new LinkedHashMap<>();
    private final Map<String, Long> entradasPorVia = new LinkedHashMap<>();
    private double distanciaTotalPercorridaKm;
    private double tempoTotalMovimentoHoras;
    private double congestionamentoPicoPercentual;
    private double somaCongestionamentoInterregional;
    private int maximoViasCongestionadas;
    private int veiculosEmCirculacaoAs23h;
    private long amostrasComViaAcimaCapacidade;
    private long amostrasComViaSevera;
    private long amostrasComViaEmColapso;
    private long somaViasAcimaCapacidadeNoPico;
    private long quantidadeAmostrasNoPico;
    private double somaOcupacaoViasAtivas;
    private long quantidadeObservacoesViasAtivas;
    private long somaViasAtivas;
    private long quantidadeObservacoesViasAcimaCapacidade;
    private int quantidadeVias;

    public void registrarInicioViagem(Veiculo veiculo) { idsVeiculosQueCircularam.add(veiculo.getId()); }
    public void registrarVeiculoRestrito(Veiculo veiculo) { idsVeiculosRestritos.add(veiculo.getId()); }
    public void registrarEntradaVia(Via via) { entradasPorVia.merge(via.getId(), 1L, Long::sum); }
    public void registrarConclusaoIda(double minutos) { validarMedida(minutos); temposIdaMinutos.add(minutos); }
    public void registrarConclusaoVolta(double minutos) { validarMedida(minutos); temposVoltaMinutos.add(minutos); }
    public void registrarMovimento(double distanciaKm, double segundos) {
        validarMedida(distanciaKm);
        validarMedida(segundos);
        distanciaTotalPercorridaKm += distanciaKm;
        tempoTotalMovimentoHoras += segundos / 3600.0;
    }
    public void registrarVeiculosEmCirculacaoAs23h(int quantidade) {
        if (quantidade < 0) throw new IllegalArgumentException("Quantidade inválida");
        veiculosEmCirculacaoAs23h = quantidade;
    }

    /** Amostra ao fim do tick. Velocidade da série = distância / tempo de movimento nesse tick. */
    public void registrarAmostra(long instante, Cidade cidade, List<Veiculo> veiculos,
                                double distanciaTickKm, double segundosMovimentoTick) {
        if (instante < 0 || serieCongestionamentoCidade.containsKey(instante)) throw new IllegalArgumentException("Instante inválido ou repetido");
        validarMedida(distanciaTickKm);
        validarMedida(segundosMovimentoTick);
        List<Via> vias = cidade.getVias();
        quantidadeVias = vias.size();
        double ocupacao = mediaOcupacao(vias);
        serieCongestionamentoCidade.put(instante, ocupacao);
        serieVeiculosCirculando.put(instante, (int) veiculos.stream().filter(Veiculo::estaEmMovimento).count());
        serieVelocidadeMedia.put(instante, segundosMovimentoTick == 0 ? 0 : distanciaTickKm / (segundosMovimentoTick / 3600));
        boolean horarioPico = instante >= INICIO_PICO_SEGUNDOS && instante < FIM_PICO_SEGUNDOS;
        if (horarioPico) {
            congestionamentoPicoPercentual = Math.max(congestionamentoPicoPercentual, ocupacao);
        }
        int acimaCapacidade = (int) vias.stream().filter(v -> v.calcularTaxaOcupacao() > 1).count();
        List<Via> viasAtivas = vias.stream().filter(v -> v.getQuantidadeVeiculos() > 0).toList();
        somaOcupacaoViasAtivas += viasAtivas.stream().mapToDouble(Via::calcularOcupacaoPercentual).sum();
        quantidadeObservacoesViasAtivas += viasAtivas.size();
        somaViasAtivas += viasAtivas.size();
        quantidadeObservacoesViasAcimaCapacidade += acimaCapacidade;
        for (Via via : vias) {
            double ocupacaoVia = via.calcularOcupacaoPercentual();
            somaOcupacaoPorVia.merge(via.getId(), ocupacaoVia, Double::sum);
            ocupacaoMaximaPorVia.merge(via.getId(), ocupacaoVia, Math::max);
            if (ocupacaoVia > 100) amostrasAcimaCapacidadePorVia.merge(via.getId(), 1L, Long::sum);
        }
        maximoViasCongestionadas = Math.max(maximoViasCongestionadas, acimaCapacidade);
        if (acimaCapacidade > 0) amostrasComViaAcimaCapacidade++;
        if (vias.stream().anyMatch(v -> v.getEstadoTransito() == EstadoTransito.SEVERO)) amostrasComViaSevera++;
        if (vias.stream().anyMatch(v -> v.getEstadoTransito() == EstadoTransito.COLAPSO)) amostrasComViaEmColapso++;
        if (horarioPico) {
            somaViasAcimaCapacidadeNoPico += acimaCapacidade;
            quantidadeAmostrasNoPico++;
        }
        for (Regiao regiao : Regiao.values()) {
            List<Via> internas = vias.stream().filter(v -> v.getPontoA().getRegiao() == regiao && v.getPontoB().getRegiao() == regiao).toList();
            somaCongestionamentoPorRegiao.merge(regiao, mediaOcupacao(internas), Double::sum);
        }
        List<Via> interregionais = vias.stream().filter(v -> v.getPontoA().getRegiao() != v.getPontoB().getRegiao()).toList();
        somaCongestionamentoInterregional += mediaOcupacao(interregionais);
    }
    private double mediaOcupacao(List<Via> vias) { return vias.stream().mapToDouble(Via::calcularOcupacaoPercentual).average().orElse(0); }
    private void validarMedida(double valor) {
        if (!Double.isFinite(valor) || valor < 0) throw new IllegalArgumentException("Medida inválida");
    }
    public ResultadoDia gerarResultadoDia(int dia, GrupoRodizio grupoRestrito) {
        int amostras = serieCongestionamentoCidade.size();
        Map<Regiao, Double> medias = new EnumMap<>(Regiao.class);
        for (Regiao regiao : Regiao.values()) medias.put(regiao, amostras == 0 ? 0 : somaCongestionamentoPorRegiao.getOrDefault(regiao, 0.0) / amostras);
        Map<String, DiagnosticoVia> diagnosticos = new LinkedHashMap<>();
        for (String viaId : somaOcupacaoPorVia.keySet()) {
            diagnosticos.put(viaId, new DiagnosticoVia(viaId, somaOcupacaoPorVia.get(viaId),
                    ocupacaoMaximaPorVia.getOrDefault(viaId, 0.0), entradasPorVia.getOrDefault(viaId, 0L),
                    amostrasAcimaCapacidadePorVia.getOrDefault(viaId, 0L), amostras));
        }
        return new ResultadoDia(dia, grupoRestrito, idsVeiculosQueCircularam.size(), idsVeiculosRestritos.size(),
                veiculosEmCirculacaoAs23h, temposIdaMinutos, temposVoltaMinutos,
                distanciaTotalPercorridaKm, tempoTotalMovimentoHoras, congestionamentoPicoPercentual,
                maximoViasCongestionadas, amostrasComViaAcimaCapacidade, amostrasComViaSevera,
                amostrasComViaEmColapso, somaViasAcimaCapacidadeNoPico, quantidadeAmostrasNoPico,
                somaOcupacaoViasAtivas, quantidadeObservacoesViasAtivas, somaViasAtivas,
                quantidadeObservacoesViasAcimaCapacidade, quantidadeVias, diagnosticos,
                medias, amostras == 0 ? 0 : somaCongestionamentoInterregional / amostras,
                serieVeiculosCirculando, serieCongestionamentoCidade, serieVelocidadeMedia);
    }
}
