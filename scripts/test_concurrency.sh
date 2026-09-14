#!/bin/bash
# =============================================================================
# SVIS - PRUEBA DE CONCURRENCIA EN BASH / cURL
# Dispara dos peticiones POST en paralelo con el mismo Token OTP
# =============================================================================

API_URL="http://localhost:8080/svis-api/api/votos/emitir"
PAYLOAD='{"encuestaId": 1, "opcionId": 1, "token": "SVIS-DEMO-LAURA-77A1F9"}'

echo "====================================================================="
echo "SVIS: Lanzando peticiones concurrentes a $API_URL"
echo "Payload: $PAYLOAD"
echo "====================================================================="

# Archivos temporales para capturar cabeceras y respuestas
RESP1=$(mktemp)
RESP2=$(mktemp)

# Lanzar dos peticiones cURL en subshell en segundo plano al mismo tiempo
curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X POST "$API_URL" \
     -H "Content-Type: application/json" \
     -d "$PAYLOAD" > "$RESP1" &
PID1=$!

curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X POST "$API_URL" \
     -H "Content-Type: application/json" \
     -d "$PAYLOAD" > "$RESP2" &
PID2=$!

# Esperar a que ambos procesos terminen
wait $PID1
wait $PID2

echo ""
echo "--- RESPUESTA PETICIÓN 1 (PID: $PID1) ---"
cat "$RESP1"
echo ""
echo "--- RESPUESTA PETICIÓN 2 (PID: $PID2) ---"
cat "$RESP2"
echo ""

# Limpiar temporales
rm -f "$RESP1" "$RESP2"

echo "====================================================================="
echo "Verifique que una petición retorne HTTP_STATUS:200 y la otra HTTP_STATUS:409"
echo "====================================================================="
