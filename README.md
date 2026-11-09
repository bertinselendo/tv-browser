# TV Browser

Navigateur **minimal** pour **Android TV** et Google TV, basé sur le moteur **WebView Chromium** du système. Une seule barre d’outils, un zoom global persistant, le plein écran et jusqu’à quatre volets (split vertical, horizontal ou grille).

[English summary](#english-summary) · Licence [MIT](LICENSE)

## Fonctionnalités

| Zone | Comportement |
|------|----------------|
| **Barre** | Retour / avancer / recharger (volet actif), champ d’adresse, zoom − / % / + (reset à 100 %), plein écran |
| **Split** | 1 volet · 2 colonnes · 2 lignes · grille 2×2 — réappuyer sur le mode actif revient à 1 volet |
| **Zoom** | Un seul réglage pour tous les volets et tous les sites, conservé après redémarrage de l’app |
| **Télécommande** | Parcours barre → cadre du volet → page web ; Back = sortie (pas d’historique via Back) ; appui long sur OK = focus adresse |
| **Sécurité** | Connexion SSL refusée côté app, contenu mixte bloqué, permissions web refusées |

## Captures

> Lance l’app sur un émulateur ou une box TV, puis ajoute des captures dans `docs/screenshots/` si tu contribues.

## Prérequis

- **JDK 17**
- **Android SDK** (compile SDK 36 recommandé, min SDK 26)
- Appareil ou émulateur **Android TV** (profil Leanback) pour une expérience fidèle

Configurer le SDK :

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"   # adapter selon ta machine
```

Le fichier `local.properties` (généré par Android Studio ou à la main) doit contenir `sdk.dir=...` — il n’est **pas** versionné.

## Installation

### Depuis les sources

```bash
git clone https://github.com/bertinselendo/tv-browser.git
cd tv-browser
./gradlew assembleDebug
```

APK debug : `app/build/outputs/apk/debug/app-debug.apk`

```bash
./gradlew installDebug
adb shell am start -n app.tvbrowser/.MainActivity
```

### Émulateur TV

Dans Android Studio : **Device Manager** → créer un appareil **Android TV (1080p)**, API 34+.

Checklist manuelle D-pad : [scripts/verify-tv-dpad.md](scripts/verify-tv-dpad.md).

## Utilisation (télécommande)

1. **Barre** : flèches gauche/droite entre les contrôles ; **Bas** → cadre du volet actif.
2. **Cadre** : **OK** entre dans la page ; **Back** revient à la barre.
3. **Zoom** : − / + (pas de 10 %) ; touche **%** remet 100 %.
4. **Split** : icônes colonnes, lignes, grille ; chaque slot garde sa propre URL.
5. **Plein écran** : masque la barre ; **Back** la réaffiche.
6. **Adresse** : depuis la page, **appui long OK** (~600 ms) ou focus sur le champ dans la barre.

Les saisies sans schéma sont préfixées par `https://`.

## Architecture

```
MainActivity
├── BrowserViewModel + DataStore (zoom, layout, URLs, volet actif)
├── Barre (ViewBinding)
└── PaneGrid
    └── 4 × PaneSlot → PaneWebView (Chromium système)
```

- **Kotlin**, vues XML (pas Compose) pour un focus D-pad prévisible.
- Package : `app.tvbrowser`.
- Pas de fork Chromium embarqué : APK léger, mises à jour WebView via le système / Play.

## Développement

```bash
./gradlew assembleDebug
./gradlew :app:lintDebug   # optionnel
```

Débogage WebView (build debug) : Chrome → `chrome://inspect`.

## Contribuer

1. Fork & branche feature.
2. Commits au format court : `🚀 feat: ...`, `🛠️ fix: ...`, etc.
3. PR avec description et, si UI, capture ou étapes de test TV.

## Licence

[MIT](LICENSE) — Copyright (c) 2026 Bertin Selendo

---

## English summary

**TV Browser** is a minimal **Android TV** web shell using the system **Chromium WebView**: one toolbar, persistent global zoom, fullscreen, and up to four panes (column, row, or 2×2 grid). Built with Kotlin and XML views for reliable D-pad focus. MIT licensed.
