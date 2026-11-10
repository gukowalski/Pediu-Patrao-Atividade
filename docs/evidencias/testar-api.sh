#!/usr/bin/env bash
# Teste manual da API do dashboard com a aplicação rodando (http://localhost:8080).
# Faz login com os usuários de demonstração, chama cada endpoint e grava status + corpo
# em docs/evidencias/api/. Uso: bash docs/evidencias/testar-api.sh
set -u

BASE="${BASE:-http://localhost:8080}"
DIR="$(cd "$(dirname "$0")" && pwd)/api"
mkdir -p "$DIR"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

login() { # $1 = usuário; cria o cookie de sessão em $TMP/$1.txt
  local jar="$TMP/$1.txt"
  local csrf
  csrf=$(curl -s -c "$jar" "$BASE/login" | grep -o 'name="_csrf" value="[^"]*"' | sed 's/.*value="//;s/"$//')
  curl -s -o /dev/null -b "$jar" -c "$jar" -X POST "$BASE/login" \
       --data-urlencode "username=$1" --data-urlencode "password=teste123" --data-urlencode "_csrf=$csrf"
}

chamar() { # $1 = arquivo de saída, $2 = usuário (ou "anonimo"), $3 = caminho
  local saida="$DIR/$1.txt" jar=()
  [ "$2" != "anonimo" ] && jar=(-b "$TMP/$2.txt")
  {
    echo "GET $3   (usuário: $2)"
    curl -s "${jar[@]}" -H 'Accept: application/json' -w '\nHTTP %{http_code}\n' "$BASE$3"
  } > "$saida"
  printf '%-45s %s\n' "$1" "$(tail -n1 "$saida")"
}

login gerente01
login admin
login atendente01

# Respostas de sucesso (GERENTE / ADMIN)
chamar 01-filtros                  gerente01 "/api/dashboard/filtros"
chamar 02-resumo                   gerente01 "/api/dashboard/resumo"
chamar 03-resumo-filtrado          gerente01 "/api/dashboard/resumo?dataInicio=2026-10-01&dataFim=2026-10-31&tipoAtendimento=ENTREGA&categoria=Pizza"
chamar 04-evolucao-semana          gerente01 "/api/dashboard/vendas/evolucao?agrupamento=SEMANA"
chamar 05-distribuicao             gerente01 "/api/dashboard/vendas/distribuicao"
chamar 06-por-categoria            gerente01 "/api/dashboard/vendas/por-categoria"
chamar 07-por-tipo-atendimento     gerente01 "/api/dashboard/vendas/por-tipo-atendimento"
chamar 08-status                   gerente01 "/api/dashboard/pedidos/status"
chamar 09-recentes                 gerente01 "/api/dashboard/pedidos/recentes?limite=5"
chamar 10-mais-vendidos            admin     "/api/dashboard/produtos/mais-vendidos?limite=5&criterio=FATURAMENTO"

# Erros tratados
chamar 11-erro-401-sem-login       anonimo     "/api/dashboard/resumo"
chamar 12-erro-403-atendente       atendente01 "/api/dashboard/resumo"
chamar 13-erro-400-periodo         gerente01   "/api/dashboard/resumo?dataInicio=2026-11-09&dataFim=2026-11-01"
chamar 14-erro-400-data-formato    gerente01   "/api/dashboard/resumo?dataInicio=09/11/2026"
chamar 15-erro-400-status          gerente01   "/api/dashboard/pedidos/status?status=ENTREGUE"
chamar 16-erro-400-limite          gerente01   "/api/dashboard/produtos/mais-vendidos?limite=100"
chamar 17-erro-400-intervalo       gerente01   "/api/dashboard/resumo?dataInicio=2025-01-01&dataFim=2026-11-09"
