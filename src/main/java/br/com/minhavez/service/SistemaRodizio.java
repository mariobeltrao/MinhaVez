package br.com.minhavez.service;

import br.com.minhavez.enums.GrupoRodizio;
import br.com.minhavez.model.Veiculo;
import java.util.Objects;

/** Ciclo contínuo de quatro dias, iniciado em zero; placas não participam da decisão. */
public final class SistemaRodizio {
    public GrupoRodizio getGrupoRestrito(int dia) {
        if (dia < 0) throw new IllegalArgumentException("Dia não pode ser negativo");
        return GrupoRodizio.values()[dia % 4];
    }
    public boolean podeCircular(Veiculo veiculo, int dia) {
        return Objects.requireNonNull(veiculo).getGrupoRodizio() != getGrupoRestrito(dia);
    }
}
