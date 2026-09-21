package br.com.minhavez;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    @Test void terminalExecutaComparacaoCurtaComRelatorioCompleto() {
        ByteArrayOutputStream saida = new ByteArrayOutputStream(), erros = new ByteArrayOutputStream();
        int codigo = Main.executar(new String[]{"1", "12345", "--resumo", "--veiculos=80"}, InputStream.nullInputStream(),
                new PrintStream(saida, true, StandardCharsets.UTF_8), new PrintStream(erros));
        String texto = saida.toString(StandardCharsets.UTF_8);
        assertEquals(0, codigo);
        assertEquals("", erros.toString());
        assertTrue(texto.contains("MINHA VEZ - RESULTADOS"));
        assertTrue(texto.contains("CONFIGURAÇÃO DA SIMULAÇÃO"));
        assertTrue(texto.contains("Veículos: 80"));
        assertTrue(texto.contains("Capacidade principal: 15"));
        assertTrue(texto.contains("Viagens inter-regionais: ~80%"));
        assertTrue(texto.contains("SEM RODÍZIO"));
        assertTrue(texto.contains("COM RODÍZIO"));
        assertTrue(texto.contains("INTERREGIONAIS"));
        assertTrue(texto.contains("VARIAÇÕES"));
        assertTrue(texto.contains("ANÁLISE DE SATURAÇÃO"));
        assertTrue(texto.contains("TOP 10 VIAS MAIS UTILIZADAS"));
        assertTrue(texto.contains("Resultado:"));
        assertFalse(texto.contains("NaN"));
        assertFalse(texto.contains("Infinity"));
    }
    @Test void menuAceitaPeriodoESeed() {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        int codigo = Main.executar(new String[0], new ByteArrayInputStream("1\n42\n".getBytes(StandardCharsets.UTF_8)),
                new PrintStream(saida), new PrintStream(OutputStream.nullOutputStream()));
        assertEquals(0, codigo);
        assertTrue(saida.toString().contains("Seed: 42"));
        assertTrue(saida.toString().contains("06:00"));
    }
    @Test void entradaInvalidaTemMensagemSemStackTrace() {
        ByteArrayOutputStream erros = new ByteArrayOutputStream();
        assertEquals(2, Main.executar(new String[]{"2"}, InputStream.nullInputStream(),
                new PrintStream(OutputStream.nullOutputStream()), new PrintStream(erros)));
        assertTrue(erros.toString().contains("Uso:"));
        assertFalse(erros.toString().contains("Exception"));
    }
}
