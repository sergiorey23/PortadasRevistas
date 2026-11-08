# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Android app (package `sergirex.portadasrevistas`) that shows magazine covers from kioskoymas.com and lets the user read their pages. Kotlin, single `:app` module (plus `:baselineprofile`), XML layouts + ViewBinding (no Compose), Hilt, DataStore, Coil, WorkManager, Material 3 Expressive. minSdk 23, targetSdk 36, compileSdk 37. Sister app of PortadasPeriodicos (`../PortadasPeriodicos`): both are kept alike, so a change made in one usually belongs in the other too.

## Commands

Gradle needs a JDK 17 and `JAVA_HOME` is not set in the shell here:

```bash
export JAVA_HOME=/home/sergiorey/.jdks/temurin-17 PATH=$JAVA_HOME/bin:$PATH
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew testDebugUnitTest --tests "sergirex.portadasrevistas.domain.ReminderScheduleTest"   # one class (add .method for one test)
./gradlew lintDebug
./gradlew assembleRelease      # minified (R8) + signed; also bundleRelease for the Play .aab
```

- Run `testDebugUnitTest`, `lintDebug` and `assembleRelease` as separate invocations: chaining them intermittently fails `lintAnalyzeDebugUnitTest`, and it passes when rerun alone.
- Unit tests are plain JVM (no Robolectric): `android.util.Log` is not mocked, so code under test must not call it on the executed path (the catalog repository takes an `onProblem` hook for this).
- Release signing reads `keystore.properties` (gitignored; template in `keystore.properties.example`). Never print or commit the keystore, `*.jks` or `keystorePortadas.txt`.
- `gradle.properties` must keep `android.builtInKotlin=false` and `android.newDsl=false`: the build applies the Kotlin plugin explicitly and fails without them.
- Versioning: bump `versionCode`/`versionName` in `app/build.gradle`; releases are tagged `vX.Y.Z`.
- Baseline profile: `./gradlew :app:generateBaselineProfile` with a device or emulator connected regenerates `app/src/release/generated/baselineProfiles/`, which is committed and packaged by every release build. Regenerate after big changes to the home grid, tabs, detail screen or transitions.

## Architecture

Package-by-layer inside one module: `domain/` (pure Kotlin models, repository interfaces, one-method use cases), `data/` (implementations), `presentation/` (ViewModels, UI state), `di/AppModule.kt` (Hilt bindings), `notifications/`, `ui/` (custom views and motion). Activities/Fragments sit at the package root. Storage is DataStore everywhere (old SharedPreferences are migrated once with `SharedPreferencesMigration`/`DataMigration`; keep those migrations, users upgrade from old versions). `data/local/DataStoreExt.kt` has `safeData`/`editSafely` — use them so a bad preferences file falls back to defaults instead of crashing.

**Magazine catalog.** `RevistasRepositoryImpl` picks the highest `catalogVersion` among the downloaded copy (cached in `filesDir/catalog`) and `assets/revistas.json`. `MyApplication` refreshes it on every foreground (ProcessLifecycle), rate-limited to once per 6 h, ETag-based. A downloaded file is adopted only if `CatalogParser` validates it (bad entries skipped, file rejected if invalid / newer schema / older version / <10 entries). It is published by GitHub Pages from `docs/catalog/revistas.json`. To change the list: edit the asset, **bump `catalogVersion`**, copy the file byte-for-byte to `docs/catalog/` (`CatalogAssetTest` fails if they differ) — see `docs/catalog/README.md`. Favorites take their names, web, pages and category from the catalog and hide magazines that left it (unless the catalog is unreadable).

**Covers, issues and pages.** `CoverUrls` is the only place that knows the URL layouts: the latest cover (`getLatestImage`, 424 px wide), the issue calendar (`getHistory`, `yyyyMMdd|...` newest first), an issue's cover and its pages (`t.prcdn.co`). `RevistaCoverRepositoryImpl` HEAD-probes the latest cover (an unknown magazine redirects to an SVG placeholder, so "exists" = a raster image) and dates it with the issue calendar (`IssueCalendarRepositoryImpl`, cached). Covers out of date for their category show a red corner ribbon with the date (`OldIssuePolicy`: 7 days for Corazón, 45 for the rest; `ui/CornerRibbonView`). The service refuses (403) pages from the second one on above a per-magazine `scale` limit: `PageScaleRepositoryImpl` binary-searches the largest accepted one with HEAD requests and remembers it.

**Image loading.** One shared `OkHttpClient` (Hilt) serves Coil (`MyApplication` is the `SingletonImageLoader.Factory`), the probes and the catalog. `CoverCacheControlInterceptor` makes issue covers/pages immutable in the cache. Grid thumbnails are decoded at the column width with an explicit size (leaving Coil to measure the wrap_content item `ImageView` left recreated pages blank). Bitmaps for the detail screen and the widget go through `Context.loadBitmap()` (don't combine `ImageRequest.target()` with `execute()`: it hangs); save/share write the original bytes from Coil's disk cache (`loadImageBytes`).

**Home screen state.** `PortadasViewModel` is scoped to the `Portadas` Activity and holds one loader per `HomeTab` (favorites + 10 categories). Fragments are recreated freely by the ViewPager2; a tab fetches only when it's RESUMED and re-loads when its magazine list changed (favorites, a newer catalog) or on pull-to-refresh. Long-pressing a cover offers share/save/website. The Activity is never destroyed when the user leaves (back = `moveTaskToBack`).

**Detail screen and reader.** `PortadaDetalle` + `PortadaDetalleFragment` receive a `Parcelable` list (`Portada`); `PortadaDetalleViewModel` loads each magazine's issue calendar, and the date picker only allows published issues (`IssueDateValidator`). Tapping the cover opens `RevistaPagerActivity`, which resumes at the saved page (`ReadingProgressRepository`, per magazine and issue).

**Ads / purchase.** Meta Audience Network via `AdManager`: 50dp banners (home + detail) and a detail-screen interstitial shown when the user leaves it (back), gated by `InterstitialPolicy` (every 5th cover opened, at least 3 min apart, not on an open that asked for a Play review; loaded only when due); `BillingManager` (Play Billing, one-time `remove_ads_id`) makes Google Play the authority and writes `AdsEntitlementRepository`; screens observe it through `AdsViewModel` (null = not loaded yet, don't show a banner). Debug builds enable Meta test mode; `adb shell run-as sergirex.portadasrevistas touch files/force_ads` shows ads even if the purchase is owned.

**UI motion and theming (Material 3 Expressive, `ui/`).** Screen transitions go through `ActivityTransitions`: home is the parent (`asParent`), a cover opens the detail screen with a container transform from its grid cell (`launchFrom` → `prepareExpandingChild`/`expandFrom`), Settings and the reader fade (`asChild`). These, and `PureBlackTheme.apply` (optional AMOLED overlay), must run in `onCreate` **before** `super.onCreate`. Property animations use `SpringMotion` (springs from the theme's `motionSpring*` attributes). Pull-to-refresh is the custom `PullToRefreshLayout` with Material's loading indicator.

**New-issue alerts.** `NotificationScheduler` keeps one WorkManager job (network required, retried with backoff) set for the next moment allowed by the settings (`ReminderSchedule`: time of day + daily on chosen days / weekly / monthly); each run (`NewIssuesWorker`) checks up to 20 favorites with `CheckNewFavoriteIssuesUseCase`, notifies about the first one with a newer issue, and queues the next run. A magazine seen for the first time is only recorded, never reported.

**Debug tools.** `DebugOptions` lives in the `debug` source set (the `release` one is an inert twin) and adds a "Desarrollo" section to Settings ("Simular número nuevo ahora").

## Working in this repo

- The user writes in Spanish; code, comments and commits are in English.
- The emulator (`medium_phone`) is shared with the user's other app (`sergirex.portadasperiodicos`); don't start a second instance (two instances of the same AVD kill each other). Compiling is enough to check a change unless the user asks to see it running.
- Layouts adapt to the banner: after an ad loads, floating buttons move up, so UI-automation taps must use bounds from a fresh `uiautomator dump`, not fixed coordinates.
