package br.com.minhavez.service;

import br.com.minhavez.enums.*;
import br.com.minhavez.model.*;
import br.com.minhavez.ConfiguracaoSimulacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GeradorCenarioTest {
    @ParameterizedTest @ValueSource(longs = {0, 12345, -7, 999999})
    void populacaoEquilibradaReproduzivelSemPlacasRepetidas(long semente) {
        GeradorCenario gerador = new GeradorCenario();
        Cenario a = gerador.gerarCenario(semente), b = gerador.gerarCenario(semente);
        int quantidade = ConfiguracaoSimulacao.QUANTIDADE_VEICULOS;
        assertEquals(1000, quantidade);
        assertEquals(quantidade, a.getVeiculos().size());
        assertEquals(28, a.getQuantidadeDias());
        assertEquals(quantidade, a.getVeiculos().stream().map(Veiculo::getPlaca).distinct().count());
        assertEquals(quantidade / 5, a.getVeiculos().stream()
                .filter(v -> v.getResidencia().getRegiao() == v.getDestino().getRegiao()).count());
        verificarEquilibrio(Arrays.stream(Regiao.values())
                .mapToLong(regiao -> a.getVeiculos().stream().filter(v -> v.getResidencia().getRegiao() == regiao).count()).toArray());
        verificarEquilibrio(Arrays.stream(GrupoRodizio.values())
                .mapToLong(grupo -> a.getVeiculos().stream().filter(v -> v.getGrupoRodizio() == grupo).count()).toArray());
        for (GrupoRodizio grupo : GrupoRodizio.values()) {
            assertEquals(quantidade / 4, a.getVeiculos().stream().filter(v -> v.getGrupoRodizio() == grupo).count());
        }
        for (int i = 0; i < quantidade; i++) {
            Veiculo v = a.getVeiculos().get(i), outro = b.getVeiculos().get(i);
            assertEquals(v.getId(), outro.getId());
            assertEquals(v.getPlaca(), outro.getPlaca());
            assertEquals(v.getResidencia(), outro.getResidencia());
            assertEquals(v.getDestino(), outro.getDestino());
            assertEquals(v.getHorarioSaida(), outro.getHorarioSaida());
            assertEquals(v.getHorarioRetorno(), outro.getHorarioRetorno());
            assertEquals(v.getGrupoRodizio(), outro.getGrupoRodizio());
            assertEquals(GrupoRodizio.values()[i % 4], v.getGrupoRodizio());
            assertNotEquals(v.getResidencia(), v.getDestino());
            assertTrue(v.getHorarioSaida().getHour() >= 5 && v.getHorarioSaida().getHour() < 9);
            assertFalse(v.getHorarioRetorno().isBefore(LocalTime.of(16, 0)));
            assertTrue(v.getHorarioRetorno().isBefore(LocalTime.of(19, 30)));
        }
        assertEquals(820, contarEntre(a, true, "06:30", "08:00"));
        assertEquals(620, contarEntre(a, false, "17:00", "18:00"));
        assertEquals(840, contarEntre(a, false, "17:00", "18:30"));
        assertEquals(125, contarDestinos(a, Regiao.NORTE));
        assertEquals(250, contarDestinos(a, Regiao.LESTE));
        assertEquals(500, contarDestinos(a, Regiao.CENTRO));
        assertEquals(125, contarDestinos(a, Regiao.SUL));
        long polosCentro = a.getVeiculos().stream().filter(v -> Set.of("C4", "C5", "C8", "C10")
                .contains(v.getDestino().getCodigo())).count();
        long polosLeste = a.getVeiculos().stream().filter(v -> Set.of("L3", "L7", "L10")
                .contains(v.getDestino().getCodigo())).count();
        assertTrue(polosCentro >= 280 && polosCentro <= 370);
        assertTrue(polosLeste >= 120 && polosLeste <= 180);
    }
    @ParameterizedTest @ValueSource(ints = {800, 1000, 1200, 1500, 1001})
    void quantidadeConfiguravelMantemGruposERegioesEquilibrados(int quantidade) {
        Cenario cenario = new GeradorCenario(quantidade).gerarCenario(12345, 1);
        assertEquals(quantidade, cenario.getVeiculos().size());
        verificarEquilibrio(Arrays.stream(GrupoRodizio.values())
                .mapToLong(g -> cenario.getVeiculos().stream().filter(v -> v.getGrupoRodizio() == g).count()).toArray());
        verificarEquilibrio(Arrays.stream(Regiao.values())
                .mapToLong(r -> cenario.getVeiculos().stream().filter(v -> v.getResidencia().getRegiao() == r).count()).toArray());
        long internas = cenario.getVeiculos().stream()
                .filter(v -> v.getResidencia().getRegiao() == v.getDestino().getRegiao()).count();
        assertEquals(quantidade * 0.20, internas, 2);
    }
    @Test void copiaLimpaNaoCompartilhaObjetosMutaveis() {
        Cenario base = new GeradorCenario().gerarCenario(12345, 7);
        base.getCidade().getVias().getFirst().entrarVeiculo();
        base.getVeiculos().getFirst().marcarRestrito();
        Cenario copia = base.copiarParaExecucao();
        assertEquals(7, copia.getQuantidadeDias());
        assertNotSame(base.getCidade(), copia.getCidade());
        assertNotSame(base.getCidade().getVias().getFirst(), copia.getCidade().getVias().getFirst());
        assertNotSame(base.getVeiculos().getFirst(), copia.getVeiculos().getFirst());
        assertEquals(0, copia.getCidade().getVias().getFirst().getQuantidadeVeiculos());
        assertEquals(EstadoVeiculo.EM_CASA, copia.getVeiculos().getFirst().getEstado());
        assertThrows(UnsupportedOperationException.class, () -> copia.getVeiculos().clear());
    }
    @Test void cicloCompletoRestringeCadaGrupoSeteVezes() {
        SistemaRodizio sistema = new SistemaRodizio();
        Map<GrupoRodizio, Integer> dias = new EnumMap<>(GrupoRodizio.class);
        for (int dia = 0; dia < 28; dia++) dias.merge(sistema.getGrupoRestrito(dia), 1, Integer::sum);
        for (GrupoRodizio grupo : GrupoRodizio.values()) assertEquals(7, dias.get(grupo));
        assertThrows(IllegalArgumentException.class, () -> sistema.getGrupoRestrito(-1));
    }
    private void verificarEquilibrio(long[] quantidades) {
        assertTrue(Arrays.stream(quantidades).max().orElseThrow()
                - Arrays.stream(quantidades).min().orElseThrow() <= 1);
    }
    private long contarEntre(Cenario cenario, boolean saida, String inicio, String fim) {
        LocalTime a = LocalTime.parse(inicio), b = LocalTime.parse(fim);
        return cenario.getVeiculos().stream().map(v -> saida ? v.getHorarioSaida() : v.getHorarioRetorno())
                .filter(h -> !h.isBefore(a) && h.isBefore(b)).count();
    }
    private long contarDestinos(Cenario cenario, Regiao regiao) {
        return cenario.getVeiculos().stream().filter(v -> v.getDestino().getRegiao() == regiao).count();
    }
}
