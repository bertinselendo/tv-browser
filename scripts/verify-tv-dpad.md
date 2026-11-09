# Vérification manuelle (Android TV 1080p)

Build automatisé : `./gradlew assembleDebug` (succès vérifié).

## Émulateur TV

Créer un AVD **Android TV (1080p)** API 34+ dans Android Studio, puis :

```bash
./gradlew installDebug
adb shell am start -n app.tvbrowser/.MainActivity
```

## Scénario D-pad

- [ ] Barre : navigation gauche/droite entre tous les boutons
- [ ] Bas depuis la barre → cadre du volet actif ; OK → contenu web ; Back → cadre
- [ ] Colonnes : deux sites ; passage grille ; retour volet unique → URLs des slots conservées
- [ ] Zoom 150 % sur tous les volets visibles ; kill process ; relance → zoom 150 %
- [ ] Plein écran ; Back → barre visible
- [ ] Appui long OK dans la page → focus sur le champ d’adresse
- [ ] Erreur réseau → overlay Réessayer focusable
