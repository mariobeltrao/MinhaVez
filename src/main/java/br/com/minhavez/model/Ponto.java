package br.com.minhavez.model;

import br.com.minhavez.enums.Regiao;
import java.util.Objects;

public final class Ponto {
    private final String codigo;
    private final Regiao regiao;

    public Ponto(String codigo, Regiao regiao) {
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("Código vazio");
        this.codigo = codigo;
        this.regiao = Objects.requireNonNull(regiao, "Região obrigatória");
    }

    public String getCodigo() { return codigo; }
    public Regiao getRegiao() { return regiao; }

    @Override public boolean equals(Object outro) {
        return outro instanceof Ponto ponto && codigo.equals(ponto.codigo) && regiao == ponto.regiao;
    }
    @Override public int hashCode() { return Objects.hash(codigo, regiao); }
    @Override public String toString() { return codigo; }
}
