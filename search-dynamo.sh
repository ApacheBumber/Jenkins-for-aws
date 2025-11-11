#!/usr/bin/env bash
set -euo pipefail



# =======================================
# CORES
# =======================================
YELLOW='\033[0;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

# =======================================
# LOG
# =======================================
LOG_DIR="${LOG_DIR:-./logs}"
mkdir -p "${LOG_DIR}"
LOG_FILE="${LOG_DIR}/dynamo-find-$(date +%Y%m%d-%H%M%S).log"
exec > >(tee -a "${LOG_FILE}") 2>&1

echo -e "${YELLOW}[INFO] LOG SALVO EM: ${LOG_FILE}${NC}"

# =======================================
# PARÂMETROS DE ENTRADA
# =======================================
SEARCH_VALUE="${1:-"autorizador-plataforma.credcesta.com.br"}"
AZ="${2:-us-east-1a}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXCLUDE_FILE="${3:-$SCRIPT_DIR/exclude-tabelas.txt}"

# Extrai região da AZ (ex.: us-east-1a → us-east-1)
AWS_REGION=$(echo "$AZ" | sed 's/[a-z]$//')

# Segundo termo de busca é a própria AZ
SEARCH_VALUE2="$AZ"

echo -e "${YELLOW}[INFO] TERMOS DE BUSCA:${NC} '$SEARCH_VALUE' e '$SEARCH_VALUE2'"
echo -e "${YELLOW}[INFO] REGIÃO DERIVADA:${NC} $AWS_REGION"
echo -e "${YELLOW}[INFO] ARQUIVO DE EXCLUDE:${NC} $EXCLUDE_FILE"

# =======================================
# CARREGA EXCLUDE
# =======================================
EXCLUDE_TABLES=()
if [[ -f "${EXCLUDE_FILE}" ]]; then
  echo -e "${YELLOW}[INFO] LENDO LISTA DE EXCLUDE: ${EXCLUDE_FILE}${NC}"

  while IFS= read -r line || [[ -n "$line" ]]; do

    line="${line#"${line%%[![:space:]]*}"}"
    line="${line%"${line##*[![:space:]]}"}"

    [[ -z "$line" ]] && continue
    [[ "$line" == \#* ]] && continue

    EXCLUDE_TABLES+=("$line")
  done < "${EXCLUDE_FILE}"

  echo -e "${YELLOW}[INFO] TABELAS EXCLUÍDAS:${NC} ${EXCLUDE_TABLES[*]:-nenhuma}"
fi

is_excluded() {
  local table="$1"
  for ex in "${EXCLUDE_TABLES[@]:-}"; do
    [[ "$table" == "$ex" ]] && return 0
  done
  return 1
}

# =======================================
# LISTA TABELAS
# =======================================
echo -e "${YELLOW}[INFO] INICIANDO SCAN NAS TABELAS DA REGIÃO ${AWS_REGION}${NC}"

TABLES=$(aws dynamodb list-tables \
  --region "${AWS_REGION}" \
  --output text \
  --query 'TableNames[]' || true)

[[ -z "$TABLES" ]] && { echo -e "${RED}[ERROR] Nenhuma tabela encontrada.${NC}"; exit 1; }

# =======================================
# LOOP PRINCIPAL
# =======================================
for TABLE_NAME in $TABLES; do
  [[ -z "$TABLE_NAME" ]] && continue

  # EXCLUDE
  if is_excluded "$TABLE_NAME"; then
    echo -e "${YELLOW}[INFO] IGNORANDO (EXCLUDE):${NC} $TABLE_NAME"
    continue
  fi

  echo -e "${YELLOW}[INFO] PROCESSANDO:${NC} $TABLE_NAME"

  # ---------------------------------------
  # PASSO 1: Verifica COUNT da tabela
  # ---------------------------------------
  ITEM_COUNT=$(aws dynamodb scan \
      --region "${AWS_REGION}" \
      --table-name "${TABLE_NAME}" \
      --select "COUNT" \
      --output json | jq -r '.Count')

  echo -e "${YELLOW}[INFO] COUNT:${NC} $TABLE_NAME → $ITEM_COUNT itens"

  if [[ "$ITEM_COUNT" -eq 40 ]]; then
    echo -e "${YELLOW}[INFO] ADICIONANDO AO EXCLUDE (TEM 40 ITENS):${NC} $TABLE_NAME"
    echo "$TABLE_NAME" >> "$EXCLUDE_FILE"
    EXCLUDE_TABLES+=("$TABLE_NAME")
    continue
  fi

  # ---------------------------------------
  # PASSO 2: Descreve tabela (PK / SK)
  # ---------------------------------------
  TABLE_DESC=$(aws dynamodb describe-table \
    --region "${AWS_REGION}" \
    --table-name "${TABLE_NAME}" \
    --output json 2>/dev/null || echo '{}')

  PARTITION_KEY=$(echo "$TABLE_DESC" | jq -r '.Table.KeySchema[]? | select(.KeyType=="HASH") | .AttributeName // "N/A"')
  SORT_KEY=$(echo "$TABLE_DESC" | jq -r '.Table.KeySchema[]? | select(.KeyType=="RANGE") | .AttributeName // empty')

  MATCHED=0

  # ---------------------------------------
  # PASSO 3: SCAN COM FILTRO
  # ---------------------------------------
  while read -r ITEM_JSON; do
    [[ -z "$ITEM_JSON" ]] && continue

    if [[ $MATCHED -eq 0 ]]; then
      echo -e "${GREEN}[SUCCESS] MATCH ENCONTRADO NA TABELA:${NC} $TABLE_NAME"
    fi
    MATCHED=1

    PK_VALUE=$(echo "$ITEM_JSON" | jq -r --arg key "$PARTITION_KEY" '.[$key].S // .[$key].N // .[$key].B // "N/A"')

    SK_VALUE=""
    if [[ -n "$SORT_KEY" ]]; then
      SK_VALUE=$(echo "$ITEM_JSON" | jq -r --arg key "$SORT_KEY" '.[$key].S // .[$key].N // .[$key].B // empty')
    fi

    echo -e "${YELLOW}----------------------------------------${NC}"
    echo -e "${YELLOW}[TABLE]${NC} $TABLE_NAME"
    echo -e "${YELLOW}[PARTITION_KEY]${NC} $PARTITION_KEY = $PK_VALUE"
    [[ -n "$SK_VALUE" ]] && echo -e "${YELLOW}[SORT_KEY]${NC} $SORT_KEY = $SK_VALUE"

  done < <(
    aws dynamodb scan \
        --region "${AWS_REGION}" \
        --table-name "${TABLE_NAME}" \
        --output json \
    | jq -c --arg term1 "$SEARCH_VALUE" --arg term2 "$SEARCH_VALUE2" '
        .Items // [] |
        map(
          select(
            (tostring | ascii_downcase)
            | (contains($term1 | ascii_downcase) or contains($term2 | ascii_downcase))
          )
        ) | .[]
      '
  )

  [[ $MATCHED -eq 0 ]] && echo -e "${YELLOW}[INFO] Nenhum match em ${TABLE_NAME}${NC}"

  # SE A TABELA FOI PROCESSADA (COM OU SEM MATCH), SALVA NO EXCLUDE
  if ! is_excluded "$TABLE_NAME"; then
    echo -e "${YELLOW}[INFO] ADICIONANDO AO EXCLUDE (JÁ PROCESSADA):${NC} $TABLE_NAME"
    echo "$TABLE_NAME" >> "$EXCLUDE_FILE"
  fi

done

echo -e "${GREEN}[SUCCESS] BUSCA FINALIZADA.${NC}"
