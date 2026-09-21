package br.com.minhavez.model;

import br.com.minhavez.enums.*;
import java.time.LocalTime;
import java.util.Objects;

/** Guarda a rotina fixa e o deslocamento individual; a simulação administra a ocupação das vias. */
public final class Veiculo {
    private static final double TOLERANCIA_KM = 1e-9;
    private final int id;
    private final String placa;
    private final GrupoRodizio grupoRodizio;
    private final Ponto residencia;
    private final Ponto destino;
    private final LocalTime horarioSaida;
    private final LocalTime horarioRetorno;
    private EstadoVeiculo estado;
    private Ponto pontoAtual;
    private Rota rotaAtual;
    private Via viaAtual;
    private int indiceViaAtual;
    private double distanciaPercorridaNaViaKm;
    private double distanciaTotalPercorridaKm;
    private long instanteInicioViagemSegundos;

    public Veiculo(int id, String placa, GrupoRodizio grupoRodizio, Ponto residencia,
                   Ponto destino, LocalTime horarioSaida, LocalTime horarioRetorno) {
        if (id <= 0) throw new IllegalArgumentException("ID deve ser positivo");
        if (placa == null || !placa.matches("[A-Z]{3}-[0-9]{4}")) throw new IllegalArgumentException("Placa inválida");
        this.id = id;
        this.placa = placa;
        this.grupoRodizio = Objects.requireNonNull(grupoRodizio);
        this.residencia = Objects.requireNonNull(residencia);
        this.destino = Objects.requireNonNull(destino);
        this.horarioSaida = Objects.requireNonNull(horarioSaida);
        this.horarioRetorno = Objects.requireNonNull(horarioRetorno);
        if (residencia.equals(destino)) throw new IllegalArgumentException("Destino igual à residência");
        if (!horarioRetorno.isAfter(horarioSaida)) throw new IllegalArgumentException("Retorno deve ser depois da saída");
        resetarParaNovoDia();
    }
    public void resetarParaNovoDia() {
        estado = EstadoVeiculo.EM_CASA;
        pontoAtual = residencia;
        rotaAtual = null;
        viaAtual = null;
        indiceViaAtual = 0;
        distanciaPercorridaNaViaKm = 0;
        distanciaTotalPercorridaKm = 0;
        instanteInicioViagemSegundos = 0;
    }
    public void marcarRestrito() {
        exigirEstado(EstadoVeiculo.EM_CASA);
        estado = EstadoVeiculo.RESTRITO;
    }
    public void iniciarIda(Rota rota, long instanteAtual) {
        exigirEstado(EstadoVeiculo.EM_CASA);
        iniciar(rota, instanteAtual, residencia, destino, EstadoVeiculo.EM_IDA);
    }
    public void iniciarVolta(Rota rota, long instanteAtual) {
        exigirEstado(EstadoVeiculo.NO_DESTINO);
        iniciar(rota, instanteAtual, destino, residencia, EstadoVeiculo.EM_VOLTA);
    }
    private void iniciar(Rota rota, long instanteAtual, Ponto origem, Ponto fim, EstadoVeiculo novoEstado) {
        Objects.requireNonNull(rota);
        if (instanteAtual < 0 || rota.isVazia() || !rota.getOrigem().equals(origem) || !rota.getDestino().equals(fim)) {
            throw new IllegalArgumentException("Rota ou instante incompatível com a viagem");
        }
        rotaAtual = rota;
        instanteInicioViagemSegundos = instanteAtual;
        indiceViaAtual = 0;
        estado = novoEstado;
        definirViaAtual();
    }
    public void definirViaAtual() {
        if (!estaEmMovimento() || viaAtual != null || !possuiProximaVia()) throw new IllegalStateException("Não há via para iniciar");
        viaAtual = rotaAtual.getVia(indiceViaAtual);
        if (!viaAtual.conecta(pontoAtual)) throw new IllegalStateException("Via desconectada da posição");
        distanciaPercorridaNaViaKm = 0;
    }
    public void avancar(double distanciaKm) {
        if (viaAtual == null) throw new IllegalStateException("Veículo fora de via");
        if (!Double.isFinite(distanciaKm) || distanciaKm < 0 || distanciaKm > getDistanciaRestanteNaViaKm() + TOLERANCIA_KM) {
            throw new IllegalArgumentException("Avanço inválido");
        }
        double deslocamento = Math.min(distanciaKm, getDistanciaRestanteNaViaKm());
        distanciaPercorridaNaViaKm += deslocamento;
        distanciaTotalPercorridaKm += deslocamento;
    }
    public boolean atingiuFimDaVia() { return viaAtual != null && getDistanciaRestanteNaViaKm() <= TOLERANCIA_KM; }
    public void concluirViaAtual() {
        if (!atingiuFimDaVia()) throw new IllegalStateException("A via ainda não terminou");
        pontoAtual = viaAtual.getOutroPonto(pontoAtual);
        viaAtual = null;
        distanciaPercorridaNaViaKm = 0;
        indiceViaAtual++;
    }
    public boolean possuiProximaVia() { return rotaAtual != null && indiceViaAtual < rotaAtual.getQuantidadeVias(); }
    public boolean concluiuRota() { return rotaAtual != null && viaAtual == null && !possuiProximaVia(); }
    public void concluirIda() {
        exigirEstado(EstadoVeiculo.EM_IDA);
        if (!concluiuRota()) throw new IllegalStateException("Ida incompleta");
        estado = EstadoVeiculo.NO_DESTINO;
    }
    public void concluirVolta() {
        exigirEstado(EstadoVeiculo.EM_VOLTA);
        if (!concluiuRota()) throw new IllegalStateException("Volta incompleta");
        estado = EstadoVeiculo.FINALIZADO;
    }
    public boolean estaEmMovimento() { return estado == EstadoVeiculo.EM_IDA || estado == EstadoVeiculo.EM_VOLTA; }
    private void exigirEstado(EstadoVeiculo esperado) {
        if (estado != esperado) throw new IllegalStateException("Estado esperado: " + esperado + "; atual: " + estado);
    }
    public int getId() { return id; }
    public String getPlaca() { return placa; }
    public GrupoRodizio getGrupoRodizio() { return grupoRodizio; }
    public Ponto getResidencia() { return residencia; }
    public Ponto getDestino() { return destino; }
    public LocalTime getHorarioSaida() { return horarioSaida; }
    public LocalTime getHorarioRetorno() { return horarioRetorno; }
    public EstadoVeiculo getEstado() { return estado; }
    public Ponto getPontoAtual() { return pontoAtual; }
    public Rota getRotaAtual() { return rotaAtual; }
    public Via getViaAtual() { return viaAtual; }
    public int getIndiceViaAtual() { return indiceViaAtual; }
    public double getDistanciaPercorridaNaViaKm() { return distanciaPercorridaNaViaKm; }
    public double getDistanciaRestanteNaViaKm() { return viaAtual == null ? 0 : Math.max(0, viaAtual.getDistanciaKm() - distanciaPercorridaNaViaKm); }
    public double getDistanciaTotalPercorridaKm() { return distanciaTotalPercorridaKm; }
    public long getInstanteInicioViagemSegundos() { return instanteInicioViagemSegundos; }
}
