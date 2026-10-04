# Renkin AI – AI2: batch és apponkénti prompt

Alap: `renkindevbugs/Renkin`, main, `fbec389458560b77e4639dfdd55620743c1a9a66`.
Az upstream GPLv3 licenc és az eredeti szerzők feltüntetése megmarad.

## Mi készült el?

- Apponkénti **Generate with AI → Preview → Use in editor → ✓**.
- Az eredeti launcher ikon az első input; legfeljebb 5 közös stílusreferencia követi.
- Közös prompt, modell és minőség a Settings → AI icon generation alatt.
- OpenAI Images Edits API, 1024×1024, transparent PNG, egy kép/kérés.
- API-kulcs kézi beírása; AES-GCM titkosítás Android Keystore kulccsal, `noBackupFilesDir` ciphertext.
- Megszakítás, időkorlát, válaszméretkorlát, billing/rate-limit hibajelzés. Nincs automatikus fizetős retry.
- PNG-szignatúra, méret és tényleges átlátszó/látható pixelek ellenőrzése. Ez nem garantálja,
  hogy nincs a képen rajzolt sakktábla, szöveg vagy felismerhetetlen logó: az előnézet jóváhagyása szükséges.
- Sikeres képek az előnézet előtt az upstream UploadedImageStore galériájába kerülnek, 512×512 PNG-ként.
  Az adott profil/app legutóbbi előnézete visszanyitható; régebbi változatok a feltöltési galériában maradnak.
- A **Use in editor** képfeltöltési draftot választ ki. Az editor ✓ gombja alkalmazza, a szokásos
  Renkin Save/Build menti a profilba. A Close nem cseréli le az alkalmazás ikonját.
- A meglévő Room/base icon/custom icon/modifier/build/sign/install pipeline marad használatban.
- Külön alkalmazás- és generált packazonosító, eredeti Renkin mellett használható.

Az **AI2** verzióban a Global options fejlécéből a **Generate missing with AI** nyitható meg.
Keresés, Select all/none, apponkénti kijelölés és Prompt szerkesztés után a **Generate selected**
fizetős megerősítést kér, majd sorban feldolgozza az appokat. Minden kész ikon automatikusan custom
ikonként kerül az apphoz, és az egész aktuális profil mentésre kerül APK-build nélkül.
A már meglévő ikonokat és a missing-pack miatt zárolt appokat nem írja felül. A kiválasztás a
Renkin Missing szűrőjével egyezik: `createdIcon == null && key !in lockedIconKeys`.
Ez a jelenlegi profil hiányzó kimeneti ikonját jelenti, nem a forráspack nyers appfilterének auditját:
ha már van fallback vagy korábban kézzel beállított ikon, az app nem számít hiányzónak.

A batch soros, képernyőről indított futás; Wi-Fi-only, automatikus háttérfutás, proxy és pontos
költségkimutatás nincs benne. A meglévő WorkManager (watch/auto-backup) megmarad, de fizetős API-hívásokat
nem ütemezünk automatikusan: a batchet a ViewModel coroutine-ja vezérli, egy tartós AtomicFile
munkajegyzékkel és a Renkin meglévő Room profilmentésével.
A háttérbe tett alkalmazás folyamatát Android leállíthatja. Ilyenkor a már lementett kép megmarad;
a még be nem érkezett válasz elveszhet, miközben a szolgáltató kiszámlázhatta. Nincs automatikus újraküldés.

## Első használat

1. Settings → AI icon generation. Állíts be külön OpenAI API-projektet és alacsony szolgáltatói
   költési korlátot. A ChatGPT Plus nem fedezi ezt az API-használatot.
2. Illeszd be a saját API-kulcsodat az appba. Ne küldd el chatben és ne tedd GitHubra.
3. Kezdetben `gpt-image-2.5-flare`, `medium`; közös stílusprompt és 1–5 referencia.
   Az első körben 4–5 jó ikon alapján finomítsd a stílust. Egy meglévő pack kiegészítéséhez
   abból származó, külön képként kiválasztható referenciaikonokat használj.
4. Nyiss meg egy appot → Generate with AI → Generate. Ezzel az eredeti ikont, a promptot
   és a referenciákat elküldöd az OpenAI-nak. A csomagnév, activity, profil és telepített appok
   listája nem kerül a kérésbe. Az eredeti ikon maga azonosíthatja az alkalmazást.
5. Nézd meg világos és sötét háttéren. Use in editor → szükség esetén meglévő módosítók → ✓.
6. Mentsd/építsd a csomagot a Renkin megszokott gombjaival. Telepítsd, majd próbáld ki Theme Parkban.
7. Újrageneráláskor ismét az eredeti appikon és a rögzített közös referenciák mennek el;
   nem az előző, esetleg hibás generálást alakítjuk tovább.

A szolgáltató `api.openai.com` címére közvetlen TLS-kérés történik; nincs proxy vagy saját szerver.
Saját sideloados kísérlethez használható, de egy feltört/rootolt folyamat kiolvashatja a kulcsot.
Nyilvános termékhez külön, hitelesített backend és szerveroldali költési korlátozás szükséges.
A modell elérhetősége az API-fiók jogosultságaitól függhet. A modelllistát új modellhez kódban kell frissíteni.

## Fájlok és rétegek

| Fájl | Feladat |
|---|---|
| `ai/AiSettings.kt` | Globális DataStore beállítások, strukturált prompt; nincs titok |
| `ai/AiCredentialStore.kt` | Eszközhöz kötött titkosított API-kulcs, mentésekből kizárva |
| `ai/OpenAiImageClient.kt` | Fix HTTPS cél, multipart, korlátozott válasz, megszakítás, biztonságos hibák |
| `ai/AiIconGenerationService.kt` | Egyidejű kérések kizárása, PNG-validálás, meglévő galériába mentés |
| `ai/AiPromptStore.kt` | Profil/app kulcs hashével tárolt opcionális prompt-kiegészítés a DataStore-ban |
| `ai/AiBatchRunner.kt` | Soros, tesztelhető munkasor, állapotok, Pause/Retry és megszakadás-helyreállítás |
| `ai/AiBatchStore.kt` | Atomikus batch journal a noBackupFilesDir-ben; nincs API-kulcs |
| `ai/AiBatchViewModel.kt` | Kijelölés, profil- és hálózatellenőrzés, eredeti ikon → AI → custom-icon mentés |
| `ui/AiBatchDialog.kt` | Kereshető kijelölés, prompt, fizetős megerősítés, progress és hibalista |
| `GlobalOptionsActivity.kt` / `ui/GlobalOptionsScreen.kt` | Batch belépési pont és main session-bookkeeping átadás |
| `apk/ApplicationProvider.kt` | Hiányzó slot atomikus, profilt ellenőrző custom-render/edit/save művelete |
| `ai/AiIconViewModel.kt` | Hilt-injektált szolgáltatások, UI állapot és coroutine életciklus |
| `ui/AiIconDialog.kt` | Előnézet, Generate/Regenerate/Cancel/Use, globális beállítások |
| `ui/IndividualOptions.kt` | AI-gomb; elfogadás az eredeti upload draftba, profil ellenőrzése |
| `MainViewModel.kt` | Kézi alkalmazás összehangolása a meglévő profilváltási mutexszel |
| `ui/Settings.kt` | AI-beállítások belépőpontja |
| `apk/IconPackBuilder.kt` | A generált pack saját buildvariant-azonosítója; az eredeti smali classnevek megmaradnak |

Nincs új Room séma vagy új APK-builder. Az egyetlen új futásidejű dependency az OkHttp.
A namespace és Kotlin package-ek upstream értéke megmarad, ahogy az Alembicons könyvtárak neve is.

## Build Windowson

1. Telepíts Android Studiót olyan verzióban, amely támogatja az AGP 8.10.1-et és API 36-ot.
2. SDK Manager: Android SDK Platform 36, Build Tools 35.0.0, Platform Tools; emulatorhoz Emulator
   és egy megfelelő system image. A projekt minimuma API 23, targetje 35.
3. Git clone **submodule-okkal**:
   ```powershell
   git clone --recurse-submodules <a-sajat-forkod-URL-je> RenkinAI
   cd RenkinAI
   git checkout feature/ai-icons
   ```
4. Nyisd meg a gyökérmappát Android Studióban, Gradle JDK: **17**, majd Gradle Sync.
   JDK szükséges, puszta JRE nem elegendő. A wrapper Gradle 8.11.1; ne cseréld találomra.
5. Parancssorban JAVA_HOME mutasson ugyanarra a JDK-ra, például:
   ```powershell
   $env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
   .\gradlew.bat compileDebugKotlin testDebugUnitTest assembleDebug
   ```
   Az SDK útvonalát Android Studio a gitignored `local.properties` fájlban állítja be.
6. APK: `app\build\outputs\apk\debug\RenkinAI-v2026.10.04-ai2-debug.apk`.
7. Telefon: Developer options → USB debugging; csatlakoztatás, RSA engedély a telefonon.
   Android Studio eszközlistájában válaszd ki, majd Run; vagy:
   ```powershell
   adb devices
   adb install -r app\build\outputs\apk\debug\RenkinAI-v2026.10.04-ai2-debug.apk
   ```
8. Emulátorban ugyanez a Run/ADB út; Samsung Theme Parkot és a cover screent fizikai telefonon kell vizsgálni.

### Saját release és adatvesztés nélküli frissítés

Android Studio → Build → Generate Signed App Bundle or APK → APK → saját új keystore.
Tedd biztonságos helyre a keystore-t, aliasát és jelszavait. Ezek nélkül a már telepített APK
azonos csomagnévvel nem frissíthető. A fájlokat és jelszavakat soha ne commitold.

Automatikus helyi release-hez a már meglévő Renkin signing konfiguráció használható a gitignored
`local.properties` négy értékével:

```properties
renkin.keystore=C:/private-signing/renkinai.jks
renkin.keystore.password=<sajat-jelszo>
renkin.key.alias=<sajat-alias>
renkin.key.password=<sajat-jelszo>
```

`.\gradlew.bat assembleRelease`, eredmény az `app\build\outputs\apk\release` mappában.
Jelszavak nélkül az upstream konfiguráció unsigned release-t készít: azt nem lehet közvetlenül telepíteni.
Következő verziónál ugyanaz az applicationId, ugyanaz az aláírókulcs, növekvő versionCode, majd `adb install -r`.

| Változat | App | Generált pack (alapprofil) |
|---|---|---|
| Debug | `hu.zsombor.renkinai.dev` | `hu.zsombor.renkinai.dev.pack` |
| Release | `hu.zsombor.renkinai` | `hu.zsombor.renkinai.pack` |

További profil: `.p<profilId>`. Debug és release külön alkalmazás, külön adatbázis és külön packaláírókulcs.
A helyi debug keystore rendszerint `%USERPROFILE%\.android\debug.keystore`; megőrzése szükséges a
helyi debug APK-k frissíthetőségéhez. A CI egy új runneren új debug kulcsot készíthet, ezért az Actions
APK tesztpéldány, önmagában nem biztosít stabil frissítési aláírást. Tartós használatra saját release ajánlott.

A generált icon-pack **másik** kulcsot használ: Renkin `filesDir/renkinpack.keystore` és a kapcsolódó
`.pwd` fájl. A meglévő teljes Renkin backup tartalmazza ezeket. Uninstall/Clear data előtt készíts
és őrizz meg teljes backupot. Csak egy profil exportja nem elég. Kulcsvesztés után a régi pack nem
frissíthető az új kulccsal; el kell távolítani és újratelepíteni, Theme Parkban újraalkalmazni.
Az API-kulcs viszont szándékosan nincs benne a backupban; azt újra be kell írni.

GitHub Actions nem kötelező. A CI fordít, tesztel, debug APK-t épít és artifactként feltölti;
kézzel is indítható. Nincs benne API-kulcs, és nem végez fizetős OpenAI-hívást.

## Tesztelés és fennmaradó ellenőrzések

Automatikus parancsok: `compileDebugKotlin`, `testDebugUnitTest`, `assembleDebug`.
Az új `AiImagePipelineTest` a multipart input sorrendjét, biztonsági korlátokat, alpha/PNG-validálást,
JSON/Base64-kezelést, rate-limit hibák adatvédelmét, HTTP-megszakítást és packazonosítókat vizsgálja.
A végrehajtás tényleges eredményeit az átadási jegyzet tartalmazza.

Valódi kulccsal és Flip8-on ellenőrizendő:

1. Beállításmentés, app újraindítása, Keystore-olvasás; backupban nincs API-kulcs.
2. Spotify, Gmail, Reddit, Samsung Health és egy magyar app generálása. Az ikonok felismerhetők,
   egységesek, világos/sötét háttéren tiszták; a beállítások szerinti modell hozzáférhető.
3. Regenerate → előző galériakép megmarad; Cancel → nem változik az app ikonja.
4. Forgatás, háttérbe helyezés, process death, app újranyitása; nincs automatikus újraküldés.
5. Profilváltás, majd régi előnézet elfogadása: nem kerülhet rossz profilba.
6. Helytelen kulcs, elfogyott keret, 429, kapcsolatmegszakadás, sérült/nem átlátszó válasz.
7. Öt ikonból pack build/sign/install, majd ugyanennek a packnek update: az aláírás maradjon azonos.
8. Theme Park felismerés, hozzárendelések, külön launcher activity-k, One UI kezdőképernyő,
   alkalmazáslista és cover screen. Ezek kompatibilitását helyi JVM-teszt nem bizonyítja.
9. Eredeti Renkin és fork egymás mellett; debug/release külön packjai nem ütköznek.
10. Batch: először 2–5 hiányzó app, majd 20, végül a teljes hiányzó lista. Kijelölés,
    keresés, Prompt, Pause/Resume/Cancel, app újraindítása és csak egy hibás app Retry ellenőrzése.
    A kész ikonokat a Global options rácsban nézd át; nem tetsző eredményt egyenként újragenerálhatsz.

## Dokumentációs alap

A megvalósításkor, 2026. október 3-án ellenőrzött OpenAI referencia:
- https://developers.openai.com/api/reference/resources/images/methods/edit
- https://developers.openai.com/api/docs/guides/image-generation
- https://developers.openai.com/api/docs/models/gpt-image-2.5-flare

A paraméterek szolgáltatói változásakor újra ellenőrizendők. A manuális generálás nem tartalmaz
beépített pénzügyi garanciát: a költési korlátot az API-projektben állítsd be.

Buildjavítás: AGP 8.9.0 → 8.10.1 a függőségek Kotlin-metaadatainak és API 36-nak megfelelő feldolgozásához. Gradle 8.11.1 és JDK 17 megmarad.
Forrás: https://developer.android.com/build/releases/agp-8-10-0-release-notes


## AI2 használata és valós Pause / Cancel / Resume működés

1. Telepítsd a `RenkinAI-v2026.10.04-ai2-debug.apk`-t a meglévő Renkin AI frissítéseként.
   Ne töröld az előző appot, és ne töröld az adatait. Az applicationId változatlan,
   versionCode 56; a kiadott APK a korábbi debug aláírókulcsot használja.
2. Főképernyő → Advanced options → Global style → a fejléc alatti **Generate missing with AI**.
   A Global style gomb továbbra is a Renkin globális módosítóképernyőjét nyitja;
   az AI-nak külön gombja van ezen a képernyőn.
3. Ellenőrizd az AI settingsben a providert/kulcsot, a közös stílust, modellt és referenciákat.
   A batch indításkor rögzíti ezeket; Resume az eredeti batch-beállításokat használja.
   A Renkin globális módosítói a már elmentett preferenciákból kerülnek rá a custom ikonra:
   a Global style űrlap még nem mentett változásait a batch nem alkalmazza.
4. Jelöld ki a hiányzó appokat. **Select all** minden hiányzó appot kijelöl, keresés mellett is;
   a darabszám a keresésben nem látható, de kijelölt appokat is számolja.
5. A **Prompt** az adott profil/app külön utasítása. A közös stílus megmarad; az extra egy
   külön APP-SPECIFIC INSTRUCTION blokk, és utána szerepelnek a kötött kimeneti szabályok.
   Üres érték örökli a globális stílust. Legfeljebb 2000 karakter. Az egyedi AI-dialogban is
   ugyanaz a mező használható, **Reset to global style** törli a kiegészítést.
6. Generate selected → ellenőrizd az appok számát és a szolgáltatót → Generate and save.
   Új ikononként fizetős generálás történik; nincs automatikus újrapróbálkozás vagy párhuzamos hívás.
   A kész ikon bekerül az Upload galériába, majd custom ikonként a profilba és Room mentésbe.
7. **Pause after current**: az éppen futó ikon befejezése/mentése után megáll a sor.
   A batch ablak bezárása és az app háttérbe küldése is ezt kéri. A következő request nem indul el.
8. **Cancel**: a kliens megszakítja az aktív hívást; a várakozó appokat skippedként jelöli.
   A már mentett képek és profilikonok megmaradnak. Egy elküldött kérés ettől még számlázható.
9. **Resume remaining**: csak Pending / PNG saved állapotokat dolgoz fel, fizetős megerősítés után.
   A sikeres ikonokat kihagyja. Az eredeti ikon/stílus/model/quality/extra és referenciák tartalmával egyező
   mentett draftot API-hívás nélkül újra felhasználja. Régi AI1 draftot nem használ fel automatikusan,
   mert ahhoz nem tartozott generálási fingerprint.
10. Process death után nincs automatikus request. A journal helyreáll, a korábban sikeres ikonok
    a profilból töltődnek. Ha egy interrupted requesthez már van megfelelő mentett PNG, azt Resume
    fel tudja venni. Ha nincs mentett válasz, az app Failed/uncertain állapotban marad:
    ellenőrizd az API usage-ot, majd külön Retry this app szükséges.
11. Hálózati vagy API-hibánál (például 401/403/429/quota) megáll a sor. Nincs automatikus backoff/retry.
    A hibaüzenet és a Retry az adott app alatt látható. A Retry this app csak azt az appot futtatja;
    a többi várakozó apphoz külön Resume kell. Hibás PNG/alpha esetén az adott app hibás marad,
    és a sor folytathatja a következő appot.
12. Siker után térj vissza a Global options rácsra, nézd át az ikonokat, majd **Build icon pack**.
    Az építés/aláírás/telepítés a meglévő Renkin útvonal. Samsung Theme Park és a cover screen
    eredményét továbbra is a Flip8-on kell ellenőrizni.

### Tárolás és korlátozások

- A képek a `filesDir/uploaded_images` galériába kerülnek. A fingerprintes target-draft és a batch
  journal `noBackupFilesDir` alatt él, ezért a teljes Renkin backup nem viszi át a futás állapotát.
- A kész custom ikonokat a meglévő Room mentés és full/profile export kezeli. A teljes backup a
  globális preferenciákat és a hash-kulcsos egyedi promptokat is tartalmazza; egy profil megosztott
  exportja nem viszi át az egyedi AI promptbeállításokat. App uninstall/Clear data törli a journal-t.
- Egy batch sikeres lépése az egész aktuális profilt menti, az addigi egyéb session-editekkel együtt;
  a refresh outputot is lezárja a Renkin meglévő Save szemantikája szerint.
- A cache egyezése nem bizonyítja a vizuális minőséget. A batch automatikusan elfogadja a strukturálisan
  megfelelő képet, ezért build előtt ellenőrizd az eredményt; no-text/checkerboard/felismerhetőség
  vizsgálatot nem végez automatikus képfelismeréssel.
- A kiválasztott appok egyenként, az eredeti ikonból generálódnak, nem az előző eredményből.
  App eltűnése, új közben elkészült ikon vagy lock esetén a cél skipped. Profilváltás megállítja a sort.
- Az apponkénti promptok nem garantálják, hogy a modell minden utasítást pontosan követ.
- A helyi store kulcs app+profil szintű, külön launcher activity-k külön célt képeznek.
- A jelenlegi checkout meglévő kísérleti Stability AI támogatása megmarad: egy stílusreferenciát használ,
  style-transfer + külön background-removal, két szolgáltatói hívással. Flip8-on ez nincs élőben tesztelve.
  Az OpenAI útvonal és kulcstárolási alias változatlan, az API-kulcs a frissítéskor megmarad.
- WorkManager helyett képernyőhöz kötött, helyreállítható sor van: Android leállíthatja a folyamatot;
  az addig nem mentett, szolgáltatónál készülő válasz elveszhet és számlázható lehet.

Új automatikus tesztek: `AiBatchRunnerTest`, `AiPersistenceTest` és a Missing-szűrő/paritás teszt.
Ezek API-hívás nélkül szimulálják a sikert, 429-et, PNG-hibát, cache használatát, Pause/Cancel,
profilváltást és process-death utáni bizonytalan kérés helyreállítását. Nem helyettesítik a telefonos
UI- és Theme Park-próbát. A futtatás eredménye az `AI_HANDOFF_HU.md` dokumentumban szerepel.
