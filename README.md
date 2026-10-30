# Mod Hider

Ein clientseitiger Fabric-Mod für **Minecraft 26.3**, der ausgewählte Mods versteckt aus:

- **Mod Menu**: Liste, Suche, Konfigurationsmodus und Mod-Anzahl
- **Tastenbelegung**: Tasten und leere Kategorien
- **Controlling**: Liste, Suche und Autovervollständigung
- **Kreativ-Inventar**: die eigenen Tabs eines Mods (die übrigen Tabs rücken ohne Lücken nach)

Versteckte Tasten funktionieren weiterhin, sie tauchen nur nicht mehr in den Menüs auf.

## Einstellungen

Nach dem Spielstart **http://127.0.0.1:25580/** im Browser öffnen. Jeder Bereich (Mod Menu, Tastenbelegung, Kreativ-Tabs) hat dort eine eigene Liste. Änderungen gelten sofort, ohne Neustart.

Die Seite ist nur von diesem Computer aus erreichbar. Ist der Port belegt, weicht der Mod automatisch auf den nächsten freien aus. Die genaue Adresse steht im Log (`Mod Hider Einstellungen: …`). Gespeichert wird in `config/mod-hider.json`.

## Voraussetzungen

| | Version |
|---|---|
| Minecraft | 26.3 |
| Fabric Loader | ≥ 0.19.5 |
| Fabric API | beliebig für 26.3 |
| Java | 25 |
| Mod Menu, Controlling | optional |

## Bauen

```bash
./gradlew build
```

Die fertige Jar liegt danach in `build/libs/`. Gradle lädt ein JDK 25 automatisch herunter, falls keins installiert ist.

## Lizenz

MIT
