# Aufgabe 10 – REST Simulation Client

Der REST-Server speichert pro Fahrzeug einen Zustand (`id`, `lat`, `lon`, `speed`, `direction`) unter:

- `GET  /api/trasima/vehicles`
- `GET  /api/trasima/vehicles/{id}`
- `POST /api/trasima/vehicles/{id}`
- `PUT  /api/trasima/vehicles/{id}`
- `DELETE /api/trasima/vehicles/{id}`

## Realistische Simulation (Konfiguration)

Die Simulation wird über eine JSON-Datei gesteuert. Vorlage:

- `trasima-aufgabe-10/sim-config.example.json`

Erwartete Defaults (wenn keine Config gefunden wird):

- versucht `trasima-aufgabe-10/sim-config.json`, dann `sim-config.json`, sonst interne Defaults

Wichtige Felder in der Config:

- `spawn.centerLat/centerLon` + `spawn.radiusMeters`: Spawn-Bereich (z. B. Mannheim)
- `destination.lat/lon`: Ziel (z. B. SAP Arena)
- `speed.minMps/maxMps`: Geschwindigkeit in m/s
- `updateIntervalMillis`: Polling/Update Intervall
- `onArrival`: `"respawn"` oder `"stop"`

## Starten

Server:

- `mvn -pl trasima-aufgabe-10 exec:java`

Simulation (mit Config):

- `mvn -pl trasima-aufgabe-10 exec:java -Dexec.mainClass=dhbw.trasima.trasima_aufgabe_10.client.RestSimulationClientMain -Dexec.args="--config trasima-aufgabe-10/sim-config.json"`

Optional kannst du `--vehicles` oder `--baseUrl` als Override zur Config mitgeben.

