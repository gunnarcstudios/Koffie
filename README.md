# Wie haalt er koffie? ☕

Eén app voor Android, iPhone en browser.

- `docs/index.html` is de échte app (webapp). Iedereen opent dezelfde link, vult naam en drankje in, en de loting is live te volgen.
- De Android-app (`app/`) is een omhulsel om die webapp. Wijzig je `docs/index.html`, dan krijgt ook de Android-app de wijziging, zonder nieuwe APK.

## Eenmalig instellen
1. Zet de repository op **Public** (Settings → General → Danger Zone → Change visibility). Er staat niets geheims in.
2. **Settings → Pages**: Source = "Deploy from a branch", branch `main`, map `/docs`. Na ±1 minuut staat de app op `https://<gebruikersnaam>.github.io/<repo-naam>/`.
3. Klopt dat adres niet met `gunnarcstudios.github.io/Koffie`? Pas dan bovenin `app/build.gradle.kts` `webHost` en `webPath` aan.

## APK bouwen
**Actions → Build APK** (start vanzelf bij een push), download daarna de APK onder Artifacts → WieHaaltErKoffie-apk.

## Gebruiken
Open de app of de link → "Start koffieronde" → "Deel via WhatsApp". Collega's op Android of iPhone openen de link, doen mee, en de host drukt op "Loot".
