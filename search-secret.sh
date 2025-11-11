#!/bin/bash
 
# Valor que você quer procurar
SEARCH_VALUE=${1:-"autorizador-plataforma.credcesta.com.br"}
AZ=${2}

# Extrai a região da AZ (ex: us-east-1a → us-east-1)
REGION=$(echo "$AZ" | sed 's/[a-z]$//')

echo "🔎 Iniciando busca por '$SEARCH_VALUE' nas secrets da região $REGION (AZ: $AZ)..."
echo
 
# Lista todas as secrets
SECRET_LIST=$(aws secretsmanager list-secrets --region "$REGION" --query 'SecretList[].Name' --output text)
 
# Conta quantas secrets existem
TOTAL=$(echo "$SECRET_LIST" | wc -w)
FOUND=0
CURRENT=0
 
# Loop nas secrets
for SECRET_NAME in $SECRET_LIST; do
  ((CURRENT++))

  # Atualiza contador em tempo real na mesma linha
  echo -ne "\r🔁 Verificando secret $CURRENT de $TOTAL..."

  # Busca o conteúdo da secret
  SECRET_VALUE=$(aws secretsmanager get-secret-value --region "$REGION" --secret-id "$SECRET_NAME" --query 'SecretString' --output text 2>/dev/null)

  # Verifica se contém o valor
  if echo "$SECRET_VALUE" | grep -q "$SEARCH_VALUE"; then
    ((FOUND++))
    echo -e "\n✅ Encontrado em: $SECRET_NAME"
  fi
done

# Linha em branco pra separar do resultado final
echo
echo "📊 Busca concluída!"
echo "🔹 Secrets verificadas: $TOTAL"
echo "🔹 Secrets com o termo encontrado: $FOUND"

if [ "$FOUND" -eq 0 ]; then
  echo "❌ Nenhuma secret contém '$SEARCH_VALUE'."
else
  echo "✅ Termo '$SEARCH_VALUE' encontrado em $FOUND secret(s)."
fi
