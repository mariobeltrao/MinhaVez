package br.com.minhavez.service;

import br.com.minhavez.ConfiguracaoSimulacao;
import br.com.minhavez.enums.*;
import br.com.minhavez.model.*;
import java.time.LocalTime;
import java.util.*;

public final class GeradorCenario {
    public static final int QUANTIDADE_VEICULOS = ConfiguracaoSimulacao.QUANTIDADE_VEICULOS;
    public static final int DIAS_PADRAO = ConfiguracaoSimulacao.DIAS_PADRAO;
    private static final Set<String> POLOS_CENTRO = Set.of("C4", "C5", "C8", "C10");
    private static final Set<String> POLOS_LESTE = Set.of("L3", "L7", "L10");
    private final int quantidadeVeiculos;

    private record FaixaHorario(int inicioMinutos, int duracaoMinutos, int percentual) { }

    public GeradorCenario() { this(QUANTIDADE_VEICULOS); }
    public GeradorCenario(int quantidadeVeiculos) {
        if (quantidadeVeiculos <= 0) throw new IllegalArgumentException("Quantidade de veículos deve ser positiva");
        this.quantidadeVeiculos = quantidadeVeiculos;
    }

    public Cenario gerarCenario(long semente) { return gerarCenario(semente, DIAS_PADRAO); }

    public Cenario gerarCenario(long semente, int quantidadeDias) {
        Random random = new Random(semente);
        Cidade cidade = new FabricaCidade().criar();
        Map<Regiao, List<Ponto>> pontosPorRegiao = new EnumMap<>(Regiao.class);
        for (Regiao regiao : Regiao.values()) {
            pontosPorRegiao.put(regiao, cidade.getPontos().stream().filter(p -> p.getRegiao() == regiao).toList());
        }
        List<LocalTime> horariosSaida = criarHorarios(random, List.of(
                new FaixaHorario(5 * 60, 60, 3),
                new FaixaHorario(6 * 60, 30, 7),
                new FaixaHorario(6 * 60 + 30, 30, 25),
                new FaixaHorario(7 * 60, 30, 35),
                new FaixaHorario(7 * 60 + 30, 30, 22),
                new FaixaHorario(8 * 60, 60, 8)));
        List<LocalTime> horariosRetorno = criarHorarios(random, List.of(
                new FaixaHorario(16 * 60, 60, 8),
                new FaixaHorario(17 * 60, 30, 27),
                new FaixaHorario(17 * 60 + 30, 30, 35),
                new FaixaHorario(18 * 60, 30, 22),
                new FaixaHorario(18 * 60 + 30, 60, 8)));
        Map<Regiao, Deque<Regiao>> destinosPorOrigem = criarDestinosRegionais(random);
        Set<String> placas = new HashSet<>();
        List<Veiculo> veiculos = new ArrayList<>();
        for (int id = 1; id <= quantidadeVeiculos; id++) {
            // Blocos quase iguais mantêm regiões e grupos equilibrados mesmo quando N não é múltiplo de 4.
            Regiao regiao = Regiao.values()[(id - 1) * Regiao.values().length / quantidadeVeiculos];
            List<Ponto> residencias = pontosPorRegiao.get(regiao);
            Ponto residencia = residencias.get(random.nextInt(residencias.size()));
            Regiao regiaoDestino = destinosPorOrigem.get(regiao).removeFirst();
            Ponto destino = sortearDestino(pontosPorRegiao.get(regiaoDestino), residencia, regiaoDestino, random);
            LocalTime saida = horariosSaida.get(id - 1);
            LocalTime retorno = horariosRetorno.get(id - 1);
            veiculos.add(new Veiculo(id, sortearPlaca(random, placas), GrupoRodizio.values()[(id - 1) % 4],
                    residencia, destino, saida, retorno));
        }
        return new Cenario(cidade, veiculos, quantidadeDias, semente);
    }

    private List<LocalTime> criarHorarios(Random random, List<FaixaHorario> faixas) {
        int[] quantidades = distribuir(quantidadeVeiculos, faixas.stream().mapToInt(FaixaHorario::percentual).toArray());
        List<LocalTime> horarios = new ArrayList<>(quantidadeVeiculos);
        for (int indice = 0; indice < faixas.size(); indice++) {
            FaixaHorario faixa = faixas.get(indice);
            for (int i = 0; i < quantidades[indice]; i++) {
                horarios.add(LocalTime.ofSecondOfDay((faixa.inicioMinutos()
                        + random.nextInt(faixa.duracaoMinutos())) * 60L));
            }
        }
        if (horarios.size() != quantidadeVeiculos) {
            throw new IllegalStateException("Distribuição de horários incompatível com a população");
        }
        Collections.shuffle(horarios, random);
        return horarios;
    }
    private Map<Regiao, Deque<Regiao>> criarDestinosRegionais(Random random) {
        int[][] percentuais = {
                {20, 20, 60, 0},
                {10, 20, 60, 10},
                {20, 40, 20, 20},
                {0, 20, 60, 20}
        };
        Map<Regiao, Deque<Regiao>> destinos = new EnumMap<>(Regiao.class);
        for (Regiao origem : Regiao.values()) {
            int divisor = Regiao.values().length;
            int inicio = (origem.ordinal() * quantidadeVeiculos + divisor - 1) / divisor;
            int fim = ((origem.ordinal() + 1) * quantidadeVeiculos + divisor - 1) / divisor;
            int[] quantidades = distribuir(fim - inicio, percentuais[origem.ordinal()]);
            List<Regiao> regioes = new ArrayList<>(fim - inicio);
            for (Regiao destino : Regiao.values()) {
                for (int i = 0; i < quantidades[destino.ordinal()]; i++) regioes.add(destino);
            }
            Collections.shuffle(regioes, random);
            destinos.put(origem, new ArrayDeque<>(regioes));
        }
        return destinos;
    }
    private int[] distribuir(int total, int[] percentuais) {
        if (Arrays.stream(percentuais).sum() != 100) throw new IllegalArgumentException("Percentuais devem somar 100");
        int[] quantidades = new int[percentuais.length];
        int usados = 0;
        List<Integer> ordem = new ArrayList<>();
        for (int i = 0; i < percentuais.length; i++) {
            quantidades[i] = total * percentuais[i] / 100;
            usados += quantidades[i];
            ordem.add(i);
        }
        ordem.sort(Comparator.<Integer>comparingInt(i -> total * percentuais[i] % 100).reversed()
                .thenComparingInt(Integer::intValue));
        for (int i = 0; i < total - usados; i++) quantidades[ordem.get(i)]++;
        return quantidades;
    }
    private Ponto sortearDestino(List<Ponto> pontosRegiao, Ponto residencia, Regiao regiaoDestino, Random random) {
        List<Ponto> candidatos = pontosRegiao.stream().filter(p -> !p.equals(residencia)).toList();
        Set<String> polos = regiaoDestino == Regiao.CENTRO ? POLOS_CENTRO
                : regiaoDestino == Regiao.LESTE ? POLOS_LESTE : Set.of();
        int percentualPolo = regiaoDestino == Regiao.CENTRO ? 65 : regiaoDestino == Regiao.LESTE ? 60 : 0;
        if (!polos.isEmpty()) {
            boolean usarPolo = random.nextInt(100) < percentualPolo;
            List<Ponto> ponderados = candidatos.stream()
                    .filter(p -> polos.contains(p.getCodigo()) == usarPolo).toList();
            if (!ponderados.isEmpty()) candidatos = ponderados;
        }
        return candidatos.get(random.nextInt(candidatos.size()));
    }
    private String sortearPlaca(Random random, Set<String> usadas) {
        String placa;
        do {
            placa = "" + (char) ('A' + random.nextInt(26)) + (char) ('A' + random.nextInt(26))
                    + (char) ('A' + random.nextInt(26)) + "-" + String.format(Locale.ROOT, "%04d", random.nextInt(10000));
        } while (!usadas.add(placa));
        return placa;
    }
}
