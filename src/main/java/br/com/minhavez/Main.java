package br.com.minhavez;

import br.com.minhavez.model.Cenario;
import br.com.minhavez.result.*;
import br.com.minhavez.service.*;
import br.com.minhavez.simulation.Simulacao;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Main {
    private static final long SEMENTE_PADRAO = 12345;
    private static final Locale LOCAL = Locale.forLanguageTag("pt-BR");

    public static void main(String[] args) {
        int codigo = executar(args, System.in, new PrintStream(System.out, true, StandardCharsets.UTF_8),
                new PrintStream(System.err, true, StandardCharsets.UTF_8));
        if (codigo != 0) System.exit(codigo);
    }
    public static int executar(String[] args, InputStream entrada, PrintStream saida, PrintStream erros) {
        try {
            saida.println("Minha Vez - Simulador de Rodízio");
            int dias;
            long semente;
            int quantidadeVeiculos = ConfiguracaoSimulacao.QUANTIDADE_VEICULOS;
            if (args.length == 0) {
                Scanner scanner = new Scanner(entrada, StandardCharsets.UTF_8);
                saida.println("Período: 1 - 1 dia | 2 - 7 dias | 3 - 28 dias");
                saida.print("Escolha [3]: ");
                String opcao = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
                dias = switch (opcao) {
                    case "1" -> 1;
                    case "2" -> 7;
                    case "", "3" -> 28;
                    default -> throw new IllegalArgumentException("Escolha 1, 2 ou 3 no menu");
                };
                saida.print("Seed [12345]: ");
                String texto = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
                semente = texto.isEmpty() ? SEMENTE_PADRAO : Long.parseLong(texto);
            } else {
                if (args.length > 4) throw new IllegalArgumentException("Argumentos em excesso");
                dias = Integer.parseInt(args[0]);
                semente = args.length >= 2 ? Long.parseLong(args[1]) : SEMENTE_PADRAO;
            }
            if (dias != 1 && dias != 7 && dias != 28) throw new IllegalArgumentException("Período deve ser 1, 7 ou 28 dias");
            boolean detalhar = dias == 1;
            for (int i = 2; i < args.length; i++) {
                if (args[i].equals("--detalhado")) detalhar = true;
                else if (args[i].equals("--resumo")) detalhar = false;
                else if (args[i].startsWith("--veiculos=")) {
                    quantidadeVeiculos = Integer.parseInt(args[i].substring("--veiculos=".length()));
                    if (quantidadeVeiculos <= 0) throw new IllegalArgumentException("Quantidade de veículos deve ser positiva");
                } else throw new IllegalArgumentException("Use --resumo, --detalhado ou --veiculos=N");
            }
            Cenario base = new GeradorCenario(quantidadeVeiculos).gerarCenario(semente, dias);
            imprimirConfiguracao(saida, base, semente, dias);
            saida.printf("%nSeed: %d | %d dias | %d pontos | %d vias | %d veículos%n", semente, dias,
                    base.getCidade().getPontos().size(), base.getCidade().getVias().size(), base.getVeiculos().size());
            ResultadoSimulacao sem = executarCenario(base, false, detalhar, saida);
            ResultadoSimulacao com = executarCenario(base, true, detalhar, saida);
            saida.print(new ComparadorResultados().gerarRelatorioComparativo(sem, com));
            return 0;
        } catch (IllegalArgumentException erro) {
            erros.println("Entrada inválida: " + erro.getMessage());
            erros.println("Uso: java -jar target/minha-vez-0.0.jar [1|7|28] [seed] [--resumo|--detalhado] [--veiculos=N]");
            return 2;
        }
    }
    private static void imprimirConfiguracao(PrintStream saida, Cenario base, long semente, int dias) {
        long internas = base.getVeiculos().stream()
                .filter(v -> v.getResidencia().getRegiao() == v.getDestino().getRegiao()).count();
        double percentualInternas = internas * 100.0 / base.getVeiculos().size();
        Map<br.com.minhavez.enums.Regiao, Long> destinosPorRegiao = new EnumMap<>(br.com.minhavez.enums.Regiao.class);
        for (br.com.minhavez.enums.Regiao regiao : br.com.minhavez.enums.Regiao.values()) {
            destinosPorRegiao.put(regiao, base.getVeiculos().stream()
                    .filter(v -> v.getDestino().getRegiao() == regiao).count());
        }
        long destinosCentro = destinosPorRegiao.get(br.com.minhavez.enums.Regiao.CENTRO);
        long destinosLeste = destinosPorRegiao.get(br.com.minhavez.enums.Regiao.LESTE);
        long polosCentro = base.getVeiculos().stream()
                .filter(v -> Set.of("C4", "C5", "C8", "C10").contains(v.getDestino().getCodigo())).count();
        long polosLeste = base.getVeiculos().stream()
                .filter(v -> Set.of("L3", "L7", "L10").contains(v.getDestino().getCodigo())).count();
        saida.println("\n====================================");
        saida.println("CONFIGURAÇÃO DA SIMULAÇÃO");
        saida.println("====================================");
        saida.printf(LOCAL, "Veículos: %d%n%n", base.getVeiculos().size());
        saida.printf(LOCAL, "Capacidade principal: %d%n", ConfiguracaoSimulacao.CAPACIDADE_VIA_PRINCIPAL);
        saida.printf(LOCAL, "Capacidade secundária: %d%n%n", ConfiguracaoSimulacao.CAPACIDADE_VIA_SECUNDARIA);
        saida.printf(LOCAL, "Viagens internas: ~%.0f%%%n", percentualInternas);
        saida.printf(LOCAL, "Viagens inter-regionais: ~%.0f%%%n%n", 100 - percentualInternas);
        saida.println("Destinos por região:");
        for (br.com.minhavez.enums.Regiao regiao : br.com.minhavez.enums.Regiao.values()) {
            long quantidade = destinosPorRegiao.get(regiao);
            saida.printf(LOCAL, "  %s: %d (%.2f%%)%n", regiao, quantidade,
                    quantidade * 100.0 / base.getVeiculos().size());
        }
        saida.printf(LOCAL, "Polos do CENTRO: %d de %d (%.2f%%)%n", polosCentro, destinosCentro,
                destinosCentro == 0 ? 0 : polosCentro * 100.0 / destinosCentro);
        saida.printf(LOCAL, "Polos do LESTE: %d de %d (%.2f%%)%n%n", polosLeste, destinosLeste,
                destinosLeste == 0 ? 0 : polosLeste * 100.0 / destinosLeste);
        saida.printf(LOCAL, "Velocidade principal: %.0f km/h%n", ConfiguracaoSimulacao.VELOCIDADE_VIA_PRINCIPAL_KMH);
        saida.printf(LOCAL, "Velocidade secundária: %.0f km/h%n%n", ConfiguracaoSimulacao.VELOCIDADE_VIA_SECUNDARIA_KMH);
        saida.printf(LOCAL, "Seed: %d%n", semente);
        saida.printf(LOCAL, "Período: %d dias%n", dias);
    }
    private static ResultadoSimulacao executarCenario(Cenario base, boolean rodizio, boolean detalhar, PrintStream saida) {
        saida.println("\nExecutando " + (rodizio ? "COM" : "SEM") + " rodízio...");
        return new Simulacao(base, rodizio).executar((tempo, circulando) -> {
            if (detalhar) {
                // Atualiza a mesma linha; não espera tempo real entre os ticks simulados.
                saida.printf("\rRelógio %02d:%02d | %3d veículos em circulação   ", tempo / 3600, tempo % 3600 / 60, circulando);
                saida.flush();
            }
        }, dia -> {
            if (detalhar) saida.println();
            saida.printf(LOCAL, "Dia %02d | restrito: %s | circularam: %d | viagens: %d | tempo médio: %.2f min | ocupação: %.2f%%%n",
                    dia.getDia() + 1, dia.getGrupoRestrito() == null ? "nenhum" : dia.getGrupoRestrito(),
                    dia.getVeiculosQueCircularam(), dia.getQuantidadeViagensConcluidas(),
                    dia.getTempoMedioViagemMinutos(), dia.getCongestionamentoMedioPercentual());
        });
    }
}
