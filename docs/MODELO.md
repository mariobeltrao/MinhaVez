# Topologia exata da cidade fictícia

Todas as ligações abaixo são bidirecionais e representam **uma única via compartilhada pelos dois sentidos**. Existem 14 vias internas por região (5 principais e 9 secundárias) e oito inter-regionais: 64 no total.

```text
NORTE  | LESTE
-------+-------
CENTRO | SUL
```

## Norte

| Tipo | Distância (km) | Ligações |
|---|---:|---|
| Principal | 3,0 | N5–N6, N6–N7, N7–N8, N2–N6, N6–N9 |
| Secundária | 1,5 | N1–N2, N2–N3, N3–N4, N1–N5, N4–N8, N9–N10, N3–N7 |
| Secundária | 3,0 | N5–N9, N7–N10 |

## Leste

| Tipo | Distância (km) | Ligações |
|---|---:|---|
| Principal | 3,0 | L1–L2, L2–L3, L3–L4, L3–L7, L7–L10 |
| Secundária | 1,5 | L1–L5, L5–L6, L6–L7, L4–L8, L9–L10, L2–L6, L7–L8 |
| Secundária | 3,0 | L8–L9, L5–L9 |

## Centro

| Tipo | Distância (km) | Ligações |
|---|---:|---|
| Principal | 3,0 | C3–C4, C4–C5, C5–C6, C4–C8, C8–C10 |
| Secundária | 1,5 | C1–C2, C2–C3, C7–C8, C5–C9, C9–C10, C6–C10, C8–C9 |
| Secundária | 3,0 | C1–C7, C2–C7 |

## Sul

| Tipo | Distância (km) | Ligações |
|---|---:|---|
| Principal | 3,0 | S1–S2, S2–S3, S3–S4, S2–S6, S6–S9 |
| Secundária | 1,5 | S1–S5, S5–S6, S6–S7, S7–S8, S4–S8, S9–S10, S3–S7 |
| Secundária | 3,0 | S7–S10, S5–S9 |

## Conexões inter-regionais

| Regiões | Via principal (4,5 km) | Via secundária (3,0 km) |
|---|---|---|
| Norte–Leste | N4–L1 | N8–L5 |
| Norte–Centro | N5–C1 | N9–C7 |
| Centro–Sul | C6–S1 | C10–S5 |
| Leste–Sul | L9–S4 | L10–S8 |

Não existem ligações diretas Norte–Sul ou Centro–Leste. Esses deslocamentos atravessam regiões intermediárias. As conexões inter-regionais não são atribuídas às métricas de uma região isolada.
