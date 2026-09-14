#!/usr/bin/env python3
"""
=============================================================================
SVIS - SISTEMA DE VOTACIONES Y ENCUESTAS INSTITUCIONALES SEGURAS
SCRIPT DE PRUEBA DE ESTRÉS Y AISLAMIENTO CONTRA RACE CONDITIONS (REGLA 3)
=============================================================================
Objetivo:
Disparar dos peticiones HTTP concurrentes en paralelo con el EXACTO MISMO
token OTP hacia el endpoint transaccional del backend Java:
    POST /api/votos/emitir

Criterio de Evaluación y Aprobación:
- EXACTAMENTE UNA petición debe responder con HTTP 200 (Voto Contabilizado)
- EXACTAMENTE LA OTRA petición debe responder con HTTP 409 Conflict (Rechazo por Concurrencia)
- La base de datos debe reflejar solo +1 en el contador y el token en estado 'USADO'
"""

import sys
import time
import json
import threading
import urllib.request
import urllib.error

# Configuración del Endpoint del Backend Java
API_URL = "http://localhost:8080/svis-api/api/votos/emitir"

# Datos de la prueba: Encuesta 1, Opción 1, y Token OTP de prueba
PAYLOAD = {
    "encuestaId": 1,
    "opcionId": 1,
    "token": "SVIS-DEMO-LAURA-77A1F9"
}

# Barrera para sincronizar los hilos al microsegundo exacto
barrier = threading.Barrier(2)

resultados = []
lock = threading.Lock()

def enviar_voto_concurrente(hilo_id):
    """Ejecuta una petición HTTP POST sincronizada con la barrera."""
    headers = {
        "Content-Type": "application/json; charset=UTF-8",
        "Accept": "application/json"
    }
    data_bytes = json.dumps(PAYLOAD).encode("utf-8")
    req = urllib.request.Request(API_URL, data=data_bytes, headers=headers, method="POST")

    # Esperar a que todos los hilos estén listos antes de soltar la petición
    barrier.wait()
    t_inicio = time.time()

    status_code = 0
    response_body = ""
    error_msg = ""

    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            status_code = response.getcode()
            response_body = response.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        status_code = e.code
        response_body = e.read().decode("utf-8")
    except Exception as e:
        error_msg = str(e)

    latencia_ms = (time.time() - t_inicio) * 1000.0

    with lock:
        resultados.append({
            "hilo": hilo_id,
            "status": status_code,
            "body": response_body,
            "latencia_ms": latencia_ms,
            "error": error_msg
        })

def main():
    print("=" * 75)
    print("SVIS - PRUEBA DE CONCURRENCIA Y EVALUACIÓN DE CONDICIÓN DE CARRERA")
    print("=" * 75)
    print(f"Target URL : {API_URL}")
    print(f"Payload    : {json.dumps(PAYLOAD)}")
    print("Iniciando 2 hilos paralelos sincronizados con threading.Barrier...")
    print("-" * 75)

    t1 = threading.Thread(target=enviar_voto_concurrente, args=("Hilo-A",))
    t2 = threading.Thread(target=enviar_voto_concurrente, args=("Hilo-B",))

    t1.start()
    t2.start()

    t1.join()
    t2.join()

    print("\nRESULTADOS DE LAS PETICIONES SIMULTÁNEAS:")
    print("-" * 75)

    codes = []
    for r in resultados:
        print(f"[{r['hilo']}] -> Código HTTP: {r['status']} | Latencia: {r['latencia_ms']:.2f} ms")
        try:
            parsed = json.loads(r['body'])
            print(f"       Respuesta: {json.dumps(parsed, indent=2, ensure_ascii=False)}")
        except Exception:
            print(f"       Respuesta en bruto: {r['body']}")
        if r['error']:
            print(f"       Error de red: {r['error']}")
        codes.append(r['status'])
        print("-" * 75)

    # Evaluación de criterios del Taller SVIS
    print("\nEVALUACIÓN DEL CRITERIO DE INTEGRIDAD (REGLA 3):")
    if 200 in codes and 409 in codes:
        print("✅ [PRUEBA SUPERADA CON ÉXITO]")
        print("   - Una petición obtuvo HTTP 200 (Voto registrado y Token quemado).")
        print("   - La otra petición concurrente obtuvo HTTP 409 Conflict (Rechazada por bloqueo pesimista).")
        print("   - Se evitó completamente el doble voto y la condición de carrera.")
    elif 0 in codes:
        print("⚠️ [ADVERTENCIA] No fue posible conectar con el servidor backend.")
        print("   Asegúrese de que Tomcat y MySQL estén corriendo en el puerto 8080 y 3306.")
    else:
        print(f"❌ [NO APROBADO] Códigos obtenidos: {codes}. Se esperaba un 200 y un 409.")

if __name__ == "__main__":
    main()
