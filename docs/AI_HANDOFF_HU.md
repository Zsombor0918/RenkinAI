# Renkin AI2 átadás – 2026. október 4.

## Telepítés és használat

1. Töltsd le és nyisd meg a `RenkinAI-v2026.10.04-ai2-debug.apk`-t a telefonon.
   **Frissítés** a korábbi Renkin AI-ra; ne uninstalláld és ne töröld az appadatait.
2. Advanced options → Global style → a felső **Generate missing with AI** gomb.
3. Keresés vagy Select all → kívánt appok kijelölése → szükség esetén apponként Prompt.
4. Generate selected → fizetős megerősítés → Generate and save. Először 2–5 appot válassz.
5. A batch sorban menti és hozzárendeli az ikonokat. A progress mellett a siker/hiba is látható.
6. Nézd át a Global options rácsban; nem tetsző ikonnál az egyedi AI-dialogban Regenerate.
7. Build icon pack → telepítés → Samsung Theme Parkban alkalmazás.

Egyedi prompt: app editor → Generate with AI → App-specific instruction. Üres mező a közös
stílust örökli; Reset to global style törli a külön utasítást. A közös stílus/kimeneti szabályok
és referenciák megmaradnak. A batch kijelölő listájában és a hibás soroknál is van Prompt szerkesztő.

Pause megvárja az aktuális eredmény mentését; Cancel megszakítja a kliens hívását, de az már
számlázható lehet. Resume nem ismétli a sikeres vagy failed/uncertain appokat. Retry this app
csak az adott appra fut, külön megerősítéssel. Hálózati/API-hiba megállítja a sort. Újraindítás
után nincs automatikus request: a journalból kézzel lehet folytatni. A teljes útmutató:
`AI_FORK_HU.md`.

## Mit módosítottunk?

- `AiSettings.kt` / API-kliensek: app-specifikus extra utasítás, kötött közös stílus és output contract.
- `AiPromptStore.kt`: DataStore mentés, profil/component kulcs SHA-256 hashével.
- `AiIconGenerationService.kt`: meglévő UploadedImageStore csatorna, fingerprintes PNG-draft;
  cache-egyezés az eredeti képpel, prompttal, providerrel, modellel, quality-val és referencia-tartalommal.
- `AiBatchRunner.kt`: külön tesztelhető soros állapotgép, atomikus journal, Pause/Cancel/Resume,
  egyappos Retry, bizonytalan megszakítás és célváltozás kezelése. Nincs automatikus újraküldés.
- `AiBatchStore.kt` / `AiBatchViewModel.kt`: profilonkénti tartós munkajegyzék, kijelölés,
  hálózat/kulcs/profil ellenőrzés és az eredeti ikon rasterizálása.
- `AiBatchDialog.kt`: kereshető, kijelölhető Missing-lista, Prompt, fizetős megerősítés, progress és hibalista.
- `AiIconDialog.kt` / `AiIconViewModel.kt`: egyedi promptmező és reset a működő egyappos flow-ban.
- `GlobalOptionsScreen.kt` / `GlobalOptionsActivity.kt`: külön AI belépési pont, mentett edit-key
  bookkeeping átadása a MainViewModelnek. A meglévő Global style működése megmarad.
- `ApplicationProvider.kt`: hiányzó slot ellenőrzése és custom render/edit/profile save ugyanazon
  profileOperations gate alatt. App key alapján dolgozik, nem korábban eltárolt listaindex alapján.
- Manifest: ACCESS_NETWORK_STATE. Build: versionCode 56, versionName 2026.10.04-ai2.
- Megmaradt a korábbi fork applicationId-ja, generated pack ID-je, korábbi OpenAI kulcsaliasa,
  valamint a checkout kísérleti Stability támogatása. Nincs új Room séma vagy új APK-builder.

## Ellenőrzési eredmény

- `compileDebugKotlin`: sikeres.
- `testDebugUnitTest assembleDebug`: **BUILD SUCCESSFUL**.
- **592 teszt, 0 failure, 0 error, 0 skipped**.
- Új tesztek: batch siker/429/corrupt PNG/cancel/pause/cache/profilváltás/uncertain interruption,
  journal és prompt tartós mentés/reset, cache-fingerprint és Missing szűrő-paritás.
- API-tesztek MockWebServerrel; tényleges OpenAI/Stability kérés nem történt.
- APK: `hu.zsombor.renkinai.dev`, versionCode 56, minSdk 23, targetSdk 35, compileSdk 36.
- APK signature: v1 és v2 érvényes. SHA-256 aláíró tanúsítvány:
  `e8ca5fa01de881b837b2dbddfc87d579de21e8fa88f7a9ee8e8911a2da400dc5`.
  Ez pontosan egyezik a korábban kiadott AI1 APK tanúsítványával.
- A build pontosan a csomaghoz mentett Kotlin/main/test forrásokból készült; külön összehasonlítva.
- Környezet: JDK 17.0.20.1, Gradle 8.11.1, AGP 8.10.1, SDK 36, Build Tools 35.0.0.
  A környezet szinkronizált buildmappájában generált KSP cache-ütközés volt, ezért a végső build
  egy tiszta /tmp projektmásolatban futott, `-Pkotlin.compiler.execution.strategy=in-process` opcióval.
  A forrásban ehhez nem módosítottuk az upstream KSP vagy Gradle konfigurációt.

## Telefonon még ellenőrizendő

Checkbox/search/Prompt UI, 2–5 app valódi generálása, háttérbe helyezés/Pause/Resume/Cancel,
process death utáni visszatérés, batchből épített APK install/replace, Theme Park/One UI/cover screen.
A JVM-tesztek és a fordítás nem bizonyítják az ikonok vizuális minőségét vagy a Samsung kompatibilitását.
Az első telefonos próba után legyen 20 app, majd a teljes hiányzó lista.

## Mentés és visszaállítás

A ZIP tartalmazza az APK-t, dokumentációt, Git patch-et és commit bundle-t, valamint a módosított
forrásfájlokat. A private-debug-signing könyvtár a korábbi debug kulcsot tartalmazza, ezért a teljes
ZIP saját használatra való; azt ne töltsd fel nyilvános GitHub repositoryba. A Kotlin forrásokat,
dokumentációt és patch-et publikálhatod GPLv3 alatt.

GitHubra ebben a körben nincs feltöltés; a commit/bundle és a forráscsomag letölthető mentés.
A generált icon pack saját signing keystore-ja továbbra is a telefon Renkin-adatain belül van:
uninstall/Clear data előtt teljes Renkin backup szükséges. A ZIP-ben lévő app-debugkulcs ezt nem helyettesíti.
