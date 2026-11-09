# Manual verification (Android TV 1080p)

Automated build: `./gradlew assembleDebug`.

## TV emulator

Create an **Android TV (1080p)** AVD, API 34+, then:

```bash
./gradlew installDebug
adb shell am start -n app.tvbrowser/.MainActivity
```

## D-pad checklist

- [ ] Toolbar: left/right across all controls
- [ ] Down from toolbar → active pane frame; OK → web content; Back → frame
- [ ] Columns: two sites; switch to grid; back to single pane → hidden slot URLs preserved
- [ ] Zoom 150% on all visible panes; kill process; relaunch → still 150%
- [ ] Fullscreen; Back → toolbar visible
- [ ] Long OK in page → address field focused
- [ ] Network error → Retry overlay is focusable
