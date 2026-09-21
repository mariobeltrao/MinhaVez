package br.com.minhavez.simulation;

import br.com.minhavez.enums.*;
import br.com.minhavez.model.*;
import br.com.minhavez.result.*;
import br.com.minhavez.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SimulacaoTest {
    @Test void movimentoCongestionadoTambemIndependeDaOrdemDaPopulacao() {
        Ponto a = ponto("a"), b = ponto("b"), c = ponto("c");
        Cidade cidade = cidade(a, b, c);
        cidade.adicionarVia(new Via("ab", a, b, TipoVia.PRINCIPAL, 0.2));
        cidade.adicionarVia(new Via("bc", b, c, TipoVia.SECUNDARIA, 1.0));
        List<Veiculo> veiculos = new ArrayList<>();
        for (int id = 1; id <= 25; id++) veiculos.add(veiculo(id, a, c, "06:00", "17:00"));
        for (int id = 26; id <= 40; id++) veiculos.add(veiculo(id, c, a, "06:01", "17:01"));
        ResultadoDia primeiro = executar(cidade, veiculos);
        Collections.reverse(veiculos);
        ResultadoDia segundo = executar(cidade, veiculos);
        assertTrue(primeiro.getMaximoViasCongestionadas() > 0);
        assertTrue(primeiro.getTempoMedioViagemMinutos() > 1.7);
        assertEquals(primeiro.getSerieVeiculosCirculando(), segundo.getSerieVeiculosCirculando());
        assertEquals(primeiro.getSerieCongestionamentoCidade(), segundo.getSerieCongestionamentoCidade());
        assertEquals(primeiro.getTempoTotalViagensMinutos(), segundo.getTempoTotalViagensMinutos(), 1e-8);
        assertEquals(primeiro.getDistanciaTotalPercorridaKm(), segundo.getDistanciaTotalPercorridaKm(), 1e-8);
    }
    @Test void mesmaInstanciaPodeSerExecutadaNovamenteSemEstadoResidual() {
        Simulacao simulacao = new Simulacao(new GeradorCenario().gerarCenario(123, 1), true);
        ResultadoDia primeiro = simulacao.executar().getResultadosDias().getFirst();
        ResultadoDia segundo = simulacao.executar().getResultadosDias().getFirst();
        assertEquals(primeiro.getSerieCongestionamentoCidade(), segundo.getSerieCongestionamentoCidade());
        assertEquals(primeiro.getQuantidadeViagensConcluidas(), segundo.getQuantidadeViagensConcluidas());
        assertEquals(primeiro.getTempoTotalViagensMinutos(), segundo.getTempoTotalViagensMinutos(), 1e-9);
    }
    private Ponto ponto(String codigo) { return new Ponto(codigo, Regiao.NORTE); }
    private Veiculo veiculo(int id, Ponto a, Ponto b, String saida, String retorno) {
        return new Veiculo(id, String.format(Locale.ROOT, "AAA-%04d", id), GrupoRodizio.values()[(id - 1) % 4],
                a, b, LocalTime.parse(saida), LocalTime.parse(retorno));
    }
    private Cidade cidade(Ponto... pontos) {
        Cidade cidade = new Cidade();
        Arrays.stream(pontos).forEach(cidade::adicionarPonto);
        return cidade;
    }
    private ResultadoDia executar(Cidade cidade, List<Veiculo> veiculos) {
        return new Simulacao(new Cenario(cidade, veiculos, 1, 0), false).executar().getResultadosDias().getFirst();
    }
    @ParameterizedTest @ValueSource(ints = {1, 7, 28})
    void comparaCenariosSemContaminarBaseERespeitaCiclo(int dias) {
        int quantidade = 100;
        Cenario base = new GeradorCenario(quantidade).gerarCenario(12345, dias);
        ResultadoSimulacao sem = new Simulacao(base, false).executar();
        ResultadoSimulacao com = new Simulacao(base, true).executar();
        assertEquals(dias, sem.getResultadosDias().size());
        assertEquals(quantidade / 4 * dias, com.getTotalVeiculosRestritos());
        for (int dia = 0; dia < dias; dia++) {
            ResultadoDia a = sem.getResultadosDias().get(dia), b = com.getResultadosDias().get(dia);
            assertEquals(0, a.getVeiculosRestritos());
            assertNull(a.getGrupoRestrito());
            assertEquals(quantidade, a.getVeiculosQueCircularam());
            assertEquals(quantidade * 2, a.getQuantidadeViagensConcluidas());
            assertEquals(quantidade * 3 / 4, b.getVeiculosQueCircularam());
            assertEquals(quantidade * 3 / 2, b.getQuantidadeViagensConcluidas());
            assertEquals(quantidade / 4, b.getVeiculosRestritos());
            assertEquals(GrupoRodizio.values()[dia % 4], b.getGrupoRestrito());
            assertEquals(0, a.getVeiculosEmCirculacaoAs23h());
            assertEquals(0, b.getVeiculosEmCirculacaoAs23h());
            assertEquals(sem.getResultadosDias().getFirst().getTempoTotalViagensMinutos(), a.getTempoTotalViagensMinutos(), 1e-9);
            assertEquals(a.getTempoTotalViagensMinutos() / 60, a.getTempoTotalMovimentoHoras(), 1e-8);
            if (dia >= 4) assertEquals(com.getResultadosDias().get(dia - 4).getSerieCongestionamentoCidade(), b.getSerieCongestionamentoCidade());
        }
        assertTrue(base.getCidade().getVias().stream().allMatch(v -> v.getQuantidadeVeiculos() == 0));
        assertTrue(base.getVeiculos().stream().allMatch(v -> v.getEstado() == EstadoVeiculo.EM_CASA && v.getRotaAtual() == null));
    }
    @Test void restritoNaoIniciaNenhumaViagem() {
        Ponto a = ponto("a"), b = ponto("b");
        Cidade cidade = cidade(a, b);
        cidade.adicionarVia(new Via("ab", a, b, TipoVia.PRINCIPAL, 1));
        CalculadorRotas proibido = new CalculadorRotas() {
            @Override public Rota calcularMelhorRota(Cidade c, Ponto origem, Ponto destino) { fail("Restrito não calcula rota"); return null; }
        };
        ResultadoDia dia = new Simulacao(new Cenario(cidade, List.of(veiculo(1, a, b, "06:00", "17:00")), 1, 0), true, proibido)
                .executar().getResultadosDias().getFirst();
        assertEquals(1, dia.getVeiculosRestritos());
        assertEquals(0, dia.getQuantidadeViagensConcluidas());
        assertEquals(0, dia.getDistanciaTotalPercorridaKm());
    }
    @Test void tempoRestanteAtravessaViasComVelocidadesDiferentes() {
        Ponto a = ponto("a"), b = ponto("b"), c = ponto("c");
        Cidade cidade = cidade(a, b, c);
        cidade.adicionarVia(new Via("ab", a, b, TipoVia.PRINCIPAL, 0.2));
        cidade.adicionarVia(new Via("bc", b, c, TipoVia.SECUNDARIA, 0.1));
        ResultadoDia dia = executar(cidade, List.of(veiculo(1, a, c, "06:00", "17:00")));
        assertEquals(0.35, dia.getTempoMedioIdaMinutos(), 1e-9); // 12 + 9 segundos, dentro do mesmo tick.
        assertEquals(0.35, dia.getTempoMedioVoltaMinutos(), 1e-9);
        assertEquals(0.6, dia.getDistanciaTotalPercorridaKm(), 1e-9);
        assertEquals(42.0 / 3600, dia.getTempoTotalMovimentoHoras(), 1e-9);
        assertEquals(2, dia.getDiagnosticosVias().get("ab").totalEntradas());
        assertEquals(2, dia.getDiagnosticosVias().get("bc").totalEntradas());
    }
    @Test void naoIniciaAs23MasConcluiViagemEmAndamento() {
        Ponto a = ponto("a"), b = ponto("b");
        Cidade cidade = cidade(a, b);
        cidade.adicionarVia(new Via("ab", a, b, TipoVia.PRINCIPAL, 10));
        ResultadoDia dia = executar(cidade, List.of(
                veiculo(1, a, b, "06:00", "22:59"),
                veiculo(2, a, b, "06:00", "23:00"),
                veiculo(3, a, b, "23:00", "23:30")));
        assertEquals(2, dia.getVeiculosQueCircularam());
        assertEquals(3, dia.getQuantidadeViagensConcluidas());
        assertEquals(1, dia.getVeiculosEmCirculacaoAs23h());
        assertEquals(1, dia.getSerieVeiculosCirculando().get(23L * 3600));
        assertEquals(0, dia.getSerieVeiculosCirculando().get(23L * 3600 + 9 * 60));
        assertEquals(10, dia.getTempoMedioVoltaMinutos(), 1e-9);
    }
    @Test void naoComecaVoltaQuandoIdaSoTerminaDepoisDas23() {
        Ponto a = ponto("a"), b = ponto("b");
        Cidade cidade = cidade(a, b);
        cidade.adicionarVia(new Via("ab", a, b, TipoVia.PRINCIPAL, 10));
        ResultadoDia dia = executar(cidade, List.of(veiculo(1, a, b, "22:59", "23:00")));
        assertEquals(1, dia.getQuantidadeIdasConcluidas());
        assertEquals(0, dia.getQuantidadeVoltasConcluidas());
        assertEquals(1, dia.getVeiculosEmCirculacaoAs23h());
    }
    @Test void rotasSimultaneasUsamMesmaOcupacaoESoSaoCalculadasNaPartida() {
        Ponto a = ponto("a"), b = ponto("b");
        Cidade cidade = cidade(a, b);
        cidade.adicionarVia(new Via("ab", a, b, TipoVia.PRINCIPAL, 2));
        List<Integer> ocupacoes = new ArrayList<>();
        List<Rota> rotas = new ArrayList<>();
        CalculadorRotas espiao = new CalculadorRotas() {
            @Override public Rota calcularMelhorRota(Cidade c, Ponto origem, Ponto destino) {
                ocupacoes.add(c.getVias().getFirst().getQuantidadeVeiculos());
                Rota rota = super.calcularMelhorRota(c, origem, destino);
                rotas.add(rota);
                return rota;
            }
        };
        List<Veiculo> veiculos = new ArrayList<>();
        for (int id = 1; id <= 25; id++) veiculos.add(veiculo(id, a, b, "06:00", "17:00"));
        ResultadoDia dia = new Simulacao(new Cenario(cidade, veiculos, 1, 0), false, espiao).executar().getResultadosDias().getFirst();
        assertEquals(50, ocupacoes.size());
        assertTrue(ocupacoes.stream().allMatch(o -> o == 0));
        assertEquals(24, dia.getTempoMedioIdaMinutos(), 1e-9); // Todos juntos: 166,67%, piso de 5 km/h.
        assertNotSame(rotas.getFirst(), rotas.get(25));
        assertEquals(a, rotas.getFirst().getOrigem());
        assertEquals(b, rotas.get(25).getOrigem());
    }
    @Test void ordemDosVeiculosNaoAlteraMovimentoOuResultados() {
        Cenario base = new GeradorCenario().gerarCenario(42, 1);
        List<Veiculo> invertidos = new ArrayList<>(base.getVeiculos());
        Collections.reverse(invertidos);
        ResultadoDia a = new Simulacao(base, false).executar().getResultadosDias().getFirst();
        ResultadoDia b = new Simulacao(new Cenario(base.getCidade(), invertidos, 1, 42), false).executar().getResultadosDias().getFirst();
        assertEquals(a.getSerieVeiculosCirculando(), b.getSerieVeiculosCirculando());
        assertEquals(a.getSerieCongestionamentoCidade(), b.getSerieCongestionamentoCidade());
        assertEquals(a.getTempoTotalViagensMinutos(), b.getTempoTotalViagensMinutos(), 1e-9);
        assertEquals(a.getDistanciaTotalPercorridaKm(), b.getDistanciaTotalPercorridaKm(), 1e-9);
    }
    @Test void voltaPodeEscolherOutroCaminhoComTransitoAlterado() {
        Ponto a = ponto("a"), b = ponto("b"), c = ponto("c");
        Cidade cidade = cidade(a, b, c);
        cidade.adicionarVia(new Via("ab", a, b, TipoVia.PRINCIPAL, 3));
        cidade.adicionarVia(new Via("ac", a, c, TipoVia.PRINCIPAL, 2));
        cidade.adicionarVia(new Via("cb", c, b, TipoVia.PRINCIPAL, 2));
        List<Veiculo> veiculos = new ArrayList<>();
        veiculos.add(veiculo(1, a, b, "06:00", "17:00"));
        for (int id = 2; id <= 26; id++) veiculos.add(veiculo(id, a, b, "16:59", "20:00"));
        List<List<String>> caminhos = new ArrayList<>();
        CalculadorRotas espiao = new CalculadorRotas() {
            @Override public Rota calcularMelhorRota(Cidade c, Ponto origem, Ponto destino) {
                Rota rota = super.calcularMelhorRota(c, origem, destino);
                caminhos.add(rota.getVias().stream().map(Via::getId).toList());
                return rota;
            }
        };
        new Simulacao(new Cenario(cidade, veiculos, 1, 0), false, espiao).executar();
        assertEquals(List.of("ab"), caminhos.getFirst());
        assertEquals(List.of("cb", "ac"), caminhos.get(26));
        assertEquals(52, caminhos.size());
    }
}
