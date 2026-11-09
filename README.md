# TV Browser

A **minimal web browser** for **Android TV** and Google TV, powered by the system **Chromium WebView**. One compact toolbar, **persistent global zoom**, fullscreen mode, and up to **four panes** (single, side-by-side columns, stacked rows, or a 2×2 grid).

**License:** [MIT](LICENSE)

## Features

| Area | Behavior |
| ---- | -------- |
| **Toolbar** | Back / forward / reload (active pane), address field, zoom − / % / + (tap % to reset to 100%), fullscreen |
| **Split layout** | 1 pane · 2 columns · 2 rows · 2×2 grid — press the active layout again to return to a single pane |
| **Zoom** | One setting shared by every pane and every site, restored after app restart |
| **Remote (D-pad)** | Toolbar → pane frame → web content; **Back** exits layers (not history); long **OK** (~600 ms) focuses the address bar |
| **Security** | SSL errors blocked in-app, mixed content disabled, web permission requests denied |

## Screenshots

Run the app on a TV box or Android TV emulator, then add images under `docs/screenshots/` if you contribute.

## Requirements

- **JDK 17**
- **Android SDK** (compile SDK **36**, min SDK **26**)
- **Android TV** device or **Leanback** emulator for the intended UX

Set `ANDROID_HOME` and ensure `local.properties` contains `sdk.dir=...` (not committed).

## Quick start

```bash
git clone https://github.com/bertinselendo/tv-browser.git
cd tv-browser
./gradlew assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

```bash
./gradlew installDebug
adb shell am start -n app.tvbrowser/.MainActivity
```

### Android TV emulator

In Android Studio: **Device Manager** → create **Android TV (1080p)**, API 34+.

Manual D-pad checklist: [scripts/verify-tv-dpad.md](scripts/verify-tv-dpad.md).

## Using the remote

1. **Toolbar:** left/right between controls; **Down** moves focus to the active pane frame.
2. **Frame:** **OK** enters the page; **Back** returns to the toolbar (inside the page, **Back** exits to the frame first).
3. **Zoom:** − / + in steps of 10%; **%** button resets to 100%.
4. **Split:** column, row, and grid icons; each slot keeps its own URL when hidden.
5. **Fullscreen:** hides the toolbar; **Back** shows it again.
6. **Address bar:** long **OK** from web content, or move focus to the URL field on the toolbar.

URLs without a scheme are opened with `https://`.

## Architecture

```
MainActivity
├── BrowserViewModel + DataStore (zoom, layout, URLs, active pane)
├── Toolbar (ViewBinding)
└── PaneGrid
    └── 4 × PaneSlot → PaneWebView (system Chromium)
```

- **Kotlin** + XML views (not Compose) for predictable D-pad focus.
- Application ID: `app.tvbrowser`.
- No bundled Chromium fork — small APK; WebView updates come from the system / Play Store.

## Development

```bash
./gradlew assembleDebug
./gradlew :app:lintDebug   # optional
```

WebView debugging (debug builds): Chrome → `chrome://inspect`.

## Contributing

1. Fork and branch.
2. Short commit messages: `🚀 feat: …`, `🛠️ fix: …`, `🔁 refactor: …`, `🎨 style: …`.
3. Open a PR with a clear description and TV test steps (or a screenshot).

## License

[MIT](LICENSE) — Copyright (c) 2026 Bertin Selendo
