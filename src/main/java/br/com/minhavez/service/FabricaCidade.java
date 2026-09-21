package br.com.minhavez.service;

import br.com.minhavez.enums.Regiao;
import br.com.minhavez.enums.TipoVia;
import br.com.minhavez.model.*;

/** Mantém a topologia fictícia explícita, separada do sorteio das rotinas. */
public final class FabricaCidade {
    public Cidade criar() {
        Cidade cidade = new Cidade();
        String[] prefixos = {"N", "L", "C", "S"};
        for (Regiao regiao : Regiao.values()) {
            for (int i = 1; i <= 10; i++) cidade.adicionarPonto(new Ponto(prefixos[regiao.ordinal()] + i, regiao));
        }
        adicionar(cidade, "N", TipoVia.PRINCIPAL, "5-6:3 6-7:3 7-8:3 2-6:3 6-9:3");
        adicionar(cidade, "N", TipoVia.SECUNDARIA, "1-2:1.5 2-3:1.5 3-4:1.5 1-5:1.5 4-8:1.5 5-9:3 7-10:3 9-10:1.5 3-7:1.5");
        adicionar(cidade, "L", TipoVia.PRINCIPAL, "1-2:3 2-3:3 3-4:3 3-7:3 7-10:3");
        adicionar(cidade, "L", TipoVia.SECUNDARIA, "1-5:1.5 5-6:1.5 6-7:1.5 4-8:1.5 8-9:3 9-10:1.5 2-6:1.5 5-9:3 7-8:1.5");
        adicionar(cidade, "C", TipoVia.PRINCIPAL, "3-4:3 4-5:3 5-6:3 4-8:3 8-10:3");
        adicionar(cidade, "C", TipoVia.SECUNDARIA, "1-2:1.5 2-3:1.5 1-7:3 7-8:1.5 2-7:3 5-9:1.5 9-10:1.5 6-10:1.5 8-9:1.5");
        adicionar(cidade, "S", TipoVia.PRINCIPAL, "1-2:3 2-3:3 3-4:3 2-6:3 6-9:3");
        adicionar(cidade, "S", TipoVia.SECUNDARIA, "1-5:1.5 5-6:1.5 6-7:1.5 7-8:1.5 4-8:1.5 7-10:3 9-10:1.5 3-7:1.5 5-9:3");
        conectar(cidade, "N4", "L1", TipoVia.PRINCIPAL, 4.5);
        conectar(cidade, "N8", "L5", TipoVia.SECUNDARIA, 3.0);
        conectar(cidade, "N5", "C1", TipoVia.PRINCIPAL, 4.5);
        conectar(cidade, "N9", "C7", TipoVia.SECUNDARIA, 3.0);
        conectar(cidade, "C6", "S1", TipoVia.PRINCIPAL, 4.5);
        conectar(cidade, "C10", "S5", TipoVia.SECUNDARIA, 3.0);
        conectar(cidade, "L9", "S4", TipoVia.PRINCIPAL, 4.5);
        conectar(cidade, "L10", "S8", TipoVia.SECUNDARIA, 3.0);
        return cidade;
    }

    private void adicionar(Cidade cidade, String prefixo, TipoVia tipo, String ligacoes) {
        for (String ligacao : ligacoes.split(" ")) {
            String[] partes = ligacao.split("[-:]");
            conectar(cidade, prefixo + partes[0], prefixo + partes[1], tipo, Double.parseDouble(partes[2]));
        }
    }
    private void conectar(Cidade cidade, String a, String b, TipoVia tipo, double distancia) {
        cidade.adicionarVia(new Via(a + "-" + b, cidade.buscarPonto(a).orElseThrow(),
                cidade.buscarPonto(b).orElseThrow(), tipo, distancia));
    }
}
