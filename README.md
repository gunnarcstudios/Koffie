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

## Koffie Pro

Gratis: 1 team, tot 10 mensen per team. Pro (eenmalig): grote teams, meerdere teams, thema's, statistieken, eerlijk loten.

- **Prijs en betaallink**: bovenaan het script in `docs/index.html` staan `PRO_PRICE` en `PAY_URL`. Maak bij Mollie of Stripe een betaallink en plak die bij `PAY_URL`.
- **Codes maken**: `node tools/maak-code.js 10` geeft 10 nieuwe codes. Stuur een koper na betaling een code; die vult hij in bij Pro > Heb je een Pro-code.
- **Let op**: de code wordt in de app gecontroleerd en de repo is openbaar. Iemand die de broncode leest kan dus codes maken. Voor harde beveiliging is Google Play Billing of een kleine server nodig.
- Wijzig `SALT` in `docs/index.html` niet meer nadat je codes hebt uitgegeven; dan werken die niet meer.

## Versie 8.0

- **Beheerders**: wie een team aanmaakt is beheerder (kan loten, collega's toevoegen/verwijderen, andere beheerders aanwijzen). Wie via een link meedoet kan zichzelf toevoegen, de loting zien en koffiepunten inzetten. Oude teams zonder beheerder hebben een knop "Ik ben beheerder".
- **Koffiepunten**: 1 punt per keer halen; met 20 punten sla je een ronde over en wordt opnieuw geloot zonder jou.
- **Pro-statistieken** (zonder namen): per dag/week/maand/jaar, per team of bedrijf, ritme en export.
- **Privacyverklaring**: `docs/privacy.html`. Vul bovenaan de plekken `[NAAM OF BEDRIJFSNAAM INVULLEN]` en `[E-MAILADRES INVULLEN]` in.

## Versie 8.1

- Bedrijven-onderdeel verwijderd.
- **Excel-export** (Pro > Statistieken > Download Excel): tabbladen Overzicht, Per dag, Per week, Per maand, Per jaar en Over deze cijfers. Zonder namen. In de Android-app opent het deelmenu; in de browser wordt het bestand gedownload of gedeeld.
