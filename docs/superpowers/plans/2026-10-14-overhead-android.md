# Overhead Android Implementation Plan

Historical log from 2026-10-14. The app this plan describes is already built. Behavior for people using or building it is in [README.md](../../../README.md) and [DESIGN.md](../../../DESIGN.md). Paths under `/home/anon` below are the machine that wrote the plan, not setup steps for anyone else.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a sideload Overhead app that lists nearby ADS-B traffic for a typed place, alerts on the 1 / 5 / 10 mile rings while Watch is on, and opens a plane on the ADS-B Exchange globe.

**Architecture:** Pure JVM domain code ports the desktop geo, normalize, filter, and alert rules. A presenter turns a snapshot into screen text. One DataStore on the `Application` holds the place, rings, and alert memory. A `dataSync` foreground service polls the feeds and posts the toasts.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, DataStore preferences 1.1.7, Gson 2.11.0, JUnit 4. AGP 8.11.0, Kotlin 2.3.20, Compose BOM 2025.10.01, Gradle 8.13.

**Spec:** [DESIGN.md](../../../DESIGN.md) at the repo root. Executors read it beside this plan.

## Global Constraints

- Package and namespace `io.github.mohuddle.overhead`. App name Overhead. Repo `/home/anon/Work/overhead-android`.
- `minSdk 29`, `compileSdk` 36, `targetSdk` 36. `versionName` `0.1.0`, `versionCode` 1.
- Java 17 via `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }`. No `jvmToolchain`. No signing config.
- Every Gradle command uses `JAVA_HOME=/home/anon/.local/jdk-21` and `ANDROID_HOME=/home/anon/android-sdk`.
- Unit tests are `./gradlew :app:testDebugUnitTest --tests <class or method>`. Do not use `./gradlew test --tests`.
- Dynamic color stays off. Domain files import nothing from `android.*` or Compose.
- Copy the Gradle 8.13 wrapper jar and scripts from `/home/anon/Work/battery-charge-alarm`. Copy Geist, Geist Mono, Inter, and the two SIL OFL files from that app.
- User-Agent `overhead-android/0.1.0 (personal ADS-B watcher; https://github.com/mohuddle/overhead-android)`.
- One `OverheadStore` per process, created in `OverheadApp`. A second DataStore on the same file throws.
- No `ACCESS_BACKGROUND_LOCATION`, no analytics, no WebView, no IP geolocation, no ADS-B Exchange API.
- `formatAlt` and `formatMiles` use `Locale.US`.

## Review Focus

These are the inputs a person will actually hit that the desktop oracle does not pin. Each one has a test in the task that owns the code.

1. A plane on the ground stays in the list, and a position with `seen` over 60 seconds does not. Porting the desktop `ignore_ground` filter would hide airport traffic this app is required to show.
2. When adsb.fi, adsb.lol, and OpenSky all fail, the list already on screen stays and the status line becomes `ADS-B feeds failed (...)`.
3. A toast for one plane opens `https://globe.adsbexchange.com/?icao=` plus that hex. A grouped toast opens Overhead.
4. Alert memory reloaded from disk does not fire the same ring again for a plane that is still inside it.
5. `72714-1234` is a Nominatim `postalcode` query for `72714` with `country=us`, not a free-text search.

## File structure

- `Palette.kt` — hex constants. `ui/theme/` maps them onto Material 3 and the three font families.
- `domain/Geo.kt` — distance, coordinate parse, rings, number formats.
- `domain/PlaceQuery.kt` — Set classification and Nominatim body parsing.
- `domain/Aircraft.kt` — aircraft model, feed normalize, list filter, counts, row text.
- `domain/Alerts.kt` — alert memory, coalesce, toast copy, the 8 second gate.
- `domain/Feeds.kt` — feed URLs and `fetchWith`.
- `domain/Watch.kt` — `shouldRunService`.
- `data/OverheadStore.kt` — the one preferences store and `afterClear`.
- `ui/Present.kt` — `present`, the only input the composable reads.
- `ui/OverheadScreen.kt` — Compose layout of a `ScreenState`.
- `sky/Poller.kt` — one poll, including a failed fetch.
- `sky/NotifyPlan.kt` — channels, open targets, dismiss.
- `sky/OverheadService.kt`, `sky/BootReceiver.kt`, `sky/DeviceLocator.kt` — Android shells around those functions.
- `OverheadApp.kt`, `SkyHolder.kt` — process-wide store and live snapshot.

---

### Task 1: Scaffold and theme

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml`
- Create: `app/src/main/java/io/github/mohuddle/overhead/Palette.kt`
- Create: `app/src/main/java/io/github/mohuddle/overhead/ui/theme/Color.kt`, `Theme.kt`, `Type.kt`
- Create: `app/src/main/java/io/github/mohuddle/overhead/MainActivity.kt`
- Create: `app/src/test/java/io/github/mohuddle/overhead/PaletteTest.kt`
- Create: `.gitignore` covering `.gradle/`, `build/`, `local.properties`, `*.iml`
- Copy: wrapper from `battery-charge-alarm`; fonts `geist.ttf`, `geist_medium.ttf`, `geist_mono.ttf`, `inter.ttf` into `app/src/main/res/font/`; `LICENSE_GEIST.txt` and `LICENSE_INTER.txt` into `app/src/main/assets/`

**Interfaces:**
- Consumes: nothing
- Produces: `object Palette` with the Long constants named in the test. `OverheadTheme` wraps `MaterialTheme` and does not call a dynamic color scheme. `MainActivity` sets the content inside `OverheadTheme`.

- [ ] **Step 1: Write the failing test**

```kotlin
package io.github.mohuddle.overhead

import org.junit.Assert.assertEquals
import org.junit.Test

class PaletteTest {
    @Test fun studioAndSkyHexes() {
        assertEquals(0xFF12141B, Palette.Ink)
        assertEquals(0xFF1D2029, Palette.Graphite)
        assertEquals(0xFF2C303C, Palette.Slate)
        assertEquals(0xFF8D919B, Palette.Concrete)
        assertEquals(0xFF5E6470, Palette.ConcreteDeep)
        assertEquals(0xFFF4F2ED, Palette.Limestone)
        assertEquals(0xFFE6E1D7, Palette.Travertine)
        assertEquals(0xFFEE9A5B, Palette.Copper)
        assertEquals(0xFFD9783A, Palette.CopperDeep)
        assertEquals(0xFFF8E2D0, Palette.CopperMist)
        assertEquals(0xFF4A2A12, Palette.CopperInk)
        assertEquals(0xFF2F6FE4, Palette.Sky)
        assertEquals(0xFF7AABFF, Palette.SkyBright)
        assertEquals(0xFF1C4EAE, Palette.SkyDeep)
        assertEquals(0xFF0E2A5C, Palette.SkyInk)
        assertEquals(0xFFD6E4FB, Palette.SkyMist)
        assertEquals(0xFFB42318, Palette.Error)
    }
}
```

- [ ] **Step 2: Run the test and confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests io.github.mohuddle.overhead.PaletteTest`

Expected: compile failure because `Palette` does not exist. The project must be on disk first so Gradle can run. Add the wrapper, the app module, and an empty `Palette` object only if the test file will not compile without a module. Do not fill the constants in this step.

- [ ] **Step 3: Implement the scaffold**

`rootProject.name = "Overhead"`. `include(":app")`. Plugin versions AGP 8.11.0, Kotlin 2.3.20, compose compiler plugin 2.3.20.

`app/build.gradle.kts` matches the spec constraints. Dependencies: Compose BOM `2025.10.01`, `material3`, `material-icons-extended`, `activity-compose:1.10.1`, `lifecycle-runtime-ktx:2.9.2`, `lifecycle-viewmodel-compose:2.9.2`, `lifecycle-runtime-compose:2.9.2`, `core-ktx:1.16.0`, `junit:junit:4.13.2` as `testImplementation`. `compileOptions` Java 17.

`Palette` holds the constants in the test. `Color.kt` exposes a `Color` for each. `Type.kt` uses Geist for titles, Geist Mono for labels, Inter for body, copied from ChargeAlert's `Type.kt` with `R.font` in this package.

`OverheadTheme` light scheme: primary `SkyDeep`, onPrimary `Limestone`, primaryContainer `SkyMist`, onPrimaryContainer `SkyInk`, secondary `CopperDeep`, onSecondary `Ink`, secondaryContainer `CopperMist`, onSecondaryContainer `CopperInk`, background and surface `Limestone`, onBackground and onSurface `Ink`, surfaceVariant `Travertine`, onSurfaceVariant `ConcreteDeep`, error `Error`. Dark scheme: primary `SkyBright`, onPrimary `SkyInk`, primaryContainer `SkyDeep`, onPrimaryContainer `SkyMist`, secondary `Copper`, onSecondary `Ink`, secondaryContainer `CopperInk`, onSecondaryContainer `CopperMist`, background `Ink`, surface `Graphite`, onSurface `Limestone`, surfaceVariant `Slate`, onSurfaceVariant `Concrete`. `surfaceTint` is `Color.Transparent` in both.

Manifest: exported launcher activity, label `@string/app_name` = `Overhead`, theme `Theme.Overhead`. No permissions yet.

`MainActivity` shows a center-aligned top bar titled Overhead on `MaterialTheme.colorScheme.background`.

- [ ] **Step 4: Run the test and assemble**

Run: `./gradlew :app:testDebugUnitTest --tests io.github.mohuddle.overhead.PaletteTest :app:assembleDebug`

Expected: `PaletteTest` PASS and `assembleDebug` succeeds.

- [ ] **Step 5: Commit**

`git init` if this tree is not a repo. Do not change git config.

```bash
git add DESIGN.md docs/superpowers/plans/2026-10-14-overhead-android.md settings.gradle.kts build.gradle.kts gradle.properties gradlew gradlew.bat gradle/wrapper app .gitignore
git commit -m "Scaffold Overhead with the sky and limestone theme."
```

### Task 2: Places and geo

**Files:**
- Create: `app/src/main/java/io/github/mohuddle/overhead/domain/Geo.kt`
- Create: `app/src/main/java/io/github/mohuddle/overhead/domain/PlaceQuery.kt`
- Create: `app/src/test/java/io/github/mohuddle/overhead/domain/GeoTest.kt`
- Modify: `app/build.gradle.kts` to add `implementation("com.google.code.gson:gson:2.11.0")`

**Interfaces:**
- Consumes: nothing from Task 1
- Produces:

```kotlin
data class Place(val lat: Double, val lon: Double, val source: String, val label: String, val accuracyM: Double?)

fun haversineMiles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double
fun validCoords(lat: Double, lon: Double): Boolean
fun parseLocationText(text: String): Pair<Double, Double>?
fun ringsInside(miles: Double, enabled: List<Int>): List<Int>
fun innermostRing(miles: Double, enabled: List<Int>): Int?
fun formatMiles(miles: Double): String
fun formatAlt(altFt: Int?, ground: Boolean): String
fun normalizeRings(value: List<Int>?): List<Int>
fun isUsZip(text: String): Boolean

sealed class SetQuery {
    data class Manual(val place: Place) : SetQuery()
    data class Remote(val url: String) : SetQuery()
    data class Rejected(val message: String) : SetQuery()
}
fun classifySet(text: String): SetQuery
fun placeFromNominatim(query: String, body: String): Place
fun noMatchMessage(query: String): String
const val USER_AGENT: String
```

`normalizeRings` keeps only 1, 5, and 10, in the order given, without duplicates. An empty result is `listOf(1, 5, 10)`.

- [ ] **Step 1: Write the failing test**

`GeoTest` in package `io.github.mohuddle.overhead.domain`:

- `nycToPhiladelphia`: `haversineMiles(40.7128, -74.0060, 39.9526, -75.1652)` is greater than 79 and less than 83.
- `parseLocation`: `parseLocationText("34.05, -118.24")` is `(34.05, -118.24)`. `parseLocationText("34.05 N 118.24 W")` is the same. `parseLocationText("https://maps.google.com/@34.05,-118.24,14z")` is the same. `parseLocationText("Santa Monica")` is null. `parseLocationText("91, 0")` is null. `validCoords(0.0, 0.0)` is false.
- `zipPattern`: `isUsZip("72714")` and `isUsZip("72714-1234")` are true. `isUsZip("Santa Monica")` is false.
- `rings`: `ringsInside(0.4, listOf(1, 5, 10))` is `[1, 5, 10]`. `ringsInside(3.2, listOf(1, 5, 10))` is `[5, 10]`. `ringsInside(12.0, listOf(1, 5, 10))` is empty. `innermostRing(3.2, listOf(1, 5, 10))` is 5. `formatMiles(0.8)` is `0.8 mi`. `formatMiles(12.4)` is `12 mi`.
- `normalizeRingsDropsUnknownAndRestoresEmpty`: `normalizeRings(listOf(5, 5, 2, 1))` is `[5, 1]`. `normalizeRings(emptyList())` is `[1, 5, 10]`.
- `zipPlusFourUsesPostalCode`: `classifySet("72714-1234")` is `SetQuery.Remote` whose url is `https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&postalcode=72714&country=us`.
- `cityUsesFreeText`: `classifySet("Santa Monica")` is `SetQuery.Remote` whose url is `https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=Santa+Monica`.
- `coordinatesStayLocal`: `classifySet("34.05, -118.24")` is `SetQuery.Manual` with source `manual`, label `34.05, -118.24`, and null accuracy.
- `leadingDashRejected`: `classifySet("--help")` is `SetQuery.Rejected("enter a ZIP, city, or coordinates")`.
- `nominatimLabel`: `placeFromNominatim("72714", """[{"lat":"36.0","lon":"-94.1","display_name":"Fayetteville"}]""")` has lat 36.0, lon -94.1, source `manual`, label `Fayetteville`.
- `nominatimMiss`: `placeFromNominatim("Nope", "[]")` throws with message `no match for 'Nope'. Try a ZIP, city, or lat, lon.`
- `userAgent`: `USER_AGENT` is `overhead-android/0.1.0 (personal ADS-B watcher; https://github.com/mohuddle/overhead-android)`.

- [ ] **Step 2: Run the test and confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests io.github.mohuddle.overhead.domain.GeoTest`

Expected: compile failure because the domain functions are missing.

- [ ] **Step 3: Port `overhead/geo.py` and the Set rules**

Port the desktop formulas, including earth radius `3958.7613` and the three location patterns. `formatAlt` returns `ground` when `ground` is true or `altFt` is null, otherwise a `Locale.US` grouped foot value such as `4,200 ft`.

`classifySet` trims the text. A blank or a leading `--` is `Rejected`. A successful `parseLocationText` is `Manual` and does not build a URL. A ZIP uses `postalcode` of the first five digits and `country=us`. Any other text uses `q`, with spaces encoded as `+`.

`placeFromNominatim` reads the first JSON object. An empty array throws `noMatchMessage`. Unusable coordinates throw `geocoder returned unusable coordinates`. A missing `display_name` uses the trimmed query as the label.

- [ ] **Step 4: Run the test and confirm it passes**

Run: `./gradlew :app:testDebugUnitTest --tests io.github.mohuddle.overhead.domain.GeoTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle.kts app/src/main/java/io/github/mohuddle/overhead/domain app/src/test/java/io/github/mohuddle/overhead/domain/GeoTest.kt
git commit -m "Port place parsing and statute-mile geo."
```

### Task 3: Aircraft, feeds, and alerts

**Files:**
- Create: `domain/Aircraft.kt`, `domain/Alerts.kt`, `domain/Feeds.kt`
- Create: `app/src/test/java/io/github/mohuddle/overhead/domain/AircraftTest.kt`
- Create: `app/src/test/java/io/github/mohuddle/overhead/domain/AlertsTest.kt`
- Create: `app/src/test/java/io/github/mohuddle/overhead/domain/FeedsTest.kt`

**Interfaces:**
- Consumes: `haversineMiles`, `ringsInside`, `innermostRing`, `formatMiles`, `formatAlt`, `USER_AGENT` from Task 2
- Produces:

```kotlin
data class Aircraft(
    val hex: String, val callsign: String, val registration: String, val type: String,
    val desc: String, val lat: Double, val lon: Double, val altFt: Int?, val ground: Boolean,
    val gsKt: Double?, val track: Double?, val miles: Double, val bearing: Double,
    val ring: Int?, val seen: Double?
)
fun normalizeExchange(item: Map<String, Any?>, origin: Pair<Double, Double>, rings: List<Int>): Aircraft?
fun normalizeOpensky(row: List<Any?>, origin: Pair<Double, Double>, rings: List<Int>): Aircraft?
fun filterListed(aircraft: List<Aircraft>): List<Aircraft>
fun summarizeCounts(aircraft: List<Aircraft>): Map<String, Int>
fun globeUrl(hex: String): String
fun aircraftLine(item: Aircraft): String
fun nearestLine(hasLocation: Boolean, nearest: Aircraft?): String

data class AlertEvent(
    val hex: String, val callsign: String, val type: String, val miles: Double, val ring: Int,
    val altFt: Int?, val ground: Boolean, val gsKt: Double?, val extra: Int = 0
)
fun alertable(item: Aircraft, minAltFt: Int = 500): Boolean
fun newAlerts(previous: Map<String, List<Int>>, aircraft: List<Aircraft>, rings: List<Int>): Pair<List<AlertEvent>, Map<String, List<Int>>>
fun coalesce(events: List<AlertEvent>): List<AlertEvent>
fun titleFor(event: AlertEvent): String
fun bodyFor(event: AlertEvent): String
fun urgencyFor(event: AlertEvent): String
fun selectToast(events: List<AlertEvent>, notify: Boolean, secondsSinceLast: Double): AlertEvent?

data class FeedEndpoint(val name: String, val url: String)
data class FeedChoice(val replace: Boolean, val aircraft: List<Aircraft>, val provider: String, val error: String)
fun feedUrls(lat: Double, lon: Double): List<FeedEndpoint>
fun fetchWith(get: (url: String, userAgent: String) -> String, lat: Double, lon: Double, rings: List<Int>): FeedChoice
```

`fetchWith` calls `get` with `USER_AGENT` from Task 2.

`filterListed` keeps ground aircraft. It drops `seen != null && seen > 60`, drops `miles > 20`, sorts by miles ascending, and keeps 40. `summarizeCounts` keys are `"1"`, `"5"`, and `"10"`. A plane at 0.4 miles increments all three. `globeUrl` lowercases the hex. `nearestLine` with no plane is `No aircraft in range` when a place exists and `Set a location to watch the sky` when it does not. With a plane it is `{callsign} · {formatMiles}`.

- [ ] **Step 1: Write the failing tests**

`AircraftTest`:

- Exchange map `hex=A5D28C`, `flight="UAL2373 "`, `r=N47412`, `t=B39M`, `desc=BOEING 737 MAX 9`, `lat=34.02`, `lon=-118.49`, `alt_baro=4200`, `gs=288.1`, `track=268.2`, `seen_pos=1.2`, origin `(34.0195, -118.4912)`, rings `[1, 5, 10]`. Assert hex `a5d28c`, callsign `UAL2373`, type `B39M`, ring 1, miles `0.08`, ground false. `globeUrl` of that hex is `https://globe.adsbexchange.com/?icao=a5d28c`. `aircraftLine` is `0.1 mi  UAL2373  B39M  4,200 ft`.
- `alt_baro="ground"` sets `ground` true. `aircraftLine` for that plane ends with `ground`.
- OpenSky row `["a5d28c", "UAL2373 ", "United States", 0, 0, -118.49, 34.02, 1280.16, false, 148.0, 270.0]` has callsign `UAL2373` and altitude within 5 of 4200.
- `filterListed` keeps a ground aircraft at 0.2 miles with `seen` 1. It drops an airborne aircraft with `seen` 61. It keeps `seen` null. It drops miles 20.01 and keeps miles 20.0. Given 41 aircraft at miles 0.1 through 4.1, the result has 40 and does not contain miles 4.1.
- `summarizeCounts` of one plane at 0.4 miles is `{"1":1,"5":1,"10":1}`.
- `nearestLine(false, null)` is `Set a location to watch the sky`. `nearestLine(true, null)` is `No aircraft in range`.

`AlertsTest`, using a plane hex `aa`, callsign `UAL1`, type `B738`, miles 0.8, alt 3000, ground false:

- First `newAlerts` from an empty map and rings `[1, 5, 10]` returns one event at ring 1. `titleFor` is `Plane overhead`. `urgencyFor` is `critical`. The next call with the returned map returns no events. The same plane moved to 8.0 miles returns no events. An empty aircraft list then returns an empty map. A later call at 8.0 miles returns ring 10, and `bodyFor` is `UAL1 (B738) is 8.0 mi away, 3,000 ft.`
- `alertable` is false for ground at 8000 ft and for 200 ft, true for 1200 ft.
- Three events, miles 4.0 / 2.0 / 3.0, all ring 5, type `C172`, callsigns A / B / C, coalesce to one event, callsign `B`, `extra` 2. `titleFor` is `3 aircraft within 5 miles`.
- `selectToast` of one fresh event with `notify` true and `secondsSinceLast` 3.0 is null. The same with 8.0 returns that event. `notify` false and 100.0 returns null.
- `bodyFor` of an event with `gsKt` 288.1 contains `288 kt`.

`FeedsTest`:

- `feedUrls(34.0195, -118.4912)` is exactly:
  - `adsb.fi`, `https://opendata.adsb.fi/api/v3/lat/34.0195/lon/-118.4912/dist/25`
  - `adsb.lol`, `https://api.adsb.lol/v2/lat/34.0195/lon/-118.4912/dist/25`
  - `opensky`, `https://opensky-network.org/api/states/all?lamin=33.6028&lomin=-118.9939&lamax=34.4362&lomax=-117.9885`
- Fake `get`: an `adsb.fi` URL throws `java.io.IOException("timeout")`, an `adsb.lol` URL returns `{"ac":[]}`, an OpenSky URL returns `{"states":[["a5d28c","UAL2373 ","United States",0,0,-118.49,34.02,1280.16,false,148.0,270.0]]}`. Every call receives `USER_AGENT`. `fetchWith` at origin `(34.0195, -118.4912)` has `replace` true, provider `opensky`, empty error, and one aircraft with hex `a5d28c`.
- Fake `get` that throws `IOException("down")` for every URL: `replace` is false, `error` is `ADS-B feeds failed (adsb.fi: down; adsb.lol: down; opensky: down)`, provider is empty.

- [ ] **Step 2: Run the tests and confirm they fail**

Run: `./gradlew :app:testDebugUnitTest --tests io.github.mohuddle.overhead.domain.AircraftTest --tests io.github.mohuddle.overhead.domain.AlertsTest --tests io.github.mohuddle.overhead.domain.FeedsTest`

Expected: compile failure because the types are missing.

- [ ] **Step 3: Port the desktop rules**

Port `normalize_exchange`, `normalize_opensky`, `new_alerts`, `coalesce`, `title_for`, `body_for`, and `urgency_for` from `omarchy-overhead`. `filterListed` follows `_filter` except it never removes ground aircraft. `fetchWith` walks `feedUrls` in order. An exception or an unexpected payload (`ac` missing on the first two feeds, `states` missing on OpenSky) records `{name}: {message}` and continues. A list that still contains aircraft after `filterListed` wins immediately. An empty success is remembered. OpenSky is last. If OpenSky returns aircraft, use it. If every feed failed and no empty success was remembered, return `replace false` and the joined error. If an earlier feed succeeded with an empty list, return `replace true`, that provider, and an empty error. `selectToast` returns null when notify is off, the list is empty, or `secondsSinceLast < 8.0`; otherwise it returns the single coalesced event.

- [ ] **Step 4: Run the tests and confirm they pass**

Run the same command as Step 2.

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/io/github/mohuddle/overhead/domain app/src/test/java/io/github/mohuddle/overhead/domain
git commit -m "Port aircraft, alerts, and feed fallback."
```

### Task 4: DataStore

**Files:**
- Create: `data/OverheadConfig.kt`, `data/OverheadStore.kt`, `OverheadApp.kt`
- Create: `app/src/test/java/io/github/mohuddle/overhead/data/OverheadStoreTest.kt`
- Modify: `app/build.gradle.kts` to add `implementation("androidx.datastore:datastore-preferences:1.1.7")`
- Modify: `AndroidManifest.xml` so `android:name` is `.OverheadApp`

**Interfaces:**
- Consumes: `Place`, `normalizeRings`
- Produces:

```kotlin
data class OverheadConfig(
    val place: Place?, val rings: List<Int>, val notify: Boolean,
    val watch: Boolean, val alerted: Map<String, List<Int>>
)
fun afterClear(config: OverheadConfig): OverheadConfig
fun decodeAlerted(raw: String?): Map<String, List<Int>>
fun decodeRings(raw: String?): List<Int>

class OverheadStore {
    val config: Flow<OverheadConfig>
    suspend fun setPlace(place: Place?)
    suspend fun setRings(rings: List<Int>)
    suspend fun setNotify(on: Boolean)
    suspend fun setWatch(on: Boolean)
    suspend fun setAlerted(alerted: Map<String, List<Int>>)
    suspend fun clearPlace()
    companion object {
        fun create(context: Context): OverheadStore
        fun create(file: File): OverheadStore
    }
}
```

Keys: `lat`, `lon`, `source`, `label`, `accuracyM`, `rings`, `notify`, `watch`, `alerted`. Absent lat/lon means no place. Default rings `1,5,10`, notify true, watch false, alerted `{}`. `alerted` is a JSON object of hex to int arrays. `afterClear` nulls the place, sets watch false, sets alerted empty, and leaves rings and notify. `OverheadApp` constructs the store once.

- [ ] **Step 1: Write the failing test**

`OverheadStoreTest` uses `OverheadStore.create` on a temp file and `runBlocking`.

- A new store's first config has a null place, rings `[1, 5, 10]`, notify true, watch false, and empty alerted.
- `setPlace` of lat 36.0, lon -94.1, source `manual`, label `Fayetteville`, null accuracy, then `setRings(listOf(5))`, `setNotify(false)`, `setWatch(true)`, `setAlerted(mapOf("aa" to listOf(1, 5)))` reads back those values. `setRings(emptyList())` then reads rings `[1, 5, 10]`.
- `clearPlace` then reads a null place, watch false, empty alerted, rings still `[5]`, notify still false.
- `decodeAlerted("not json")` is empty. `decodeRings("nope")` is `[1, 5, 10]`.
- `afterClear` on a config with a place and a non-empty alerted map returns a null place, watch false, and an empty map, with rings and notify unchanged.

- [ ] **Step 2: Run the test and confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests io.github.mohuddle.overhead.data.OverheadStoreTest`

Expected: compile failure because `OverheadStore` is missing.

- [ ] **Step 3: Implement the store**

Use `PreferenceDataStoreFactory` for both factories. `decodeAlerted` uses Gson and returns an empty map on any parse failure. `decodeRings` splits on commas and passes the ints through `normalizeRings`. `clearPlace` writes the `afterClear` result. `setRings` persists `normalizeRings` so the last chip turning off stores all three again.

- [ ] **Step 4: Run the test and confirm it passes**

Run the Step 2 command.

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/java/io/github/mohuddle/overhead/data app/src/main/java/io/github/mohuddle/overhead/OverheadApp.kt app/src/test/java/io/github/mohuddle/overhead/data
git commit -m "Remember the place, rings, and alert memory."
```

### Task 5: Screen presenter

**Files:**
- Create: `ui/Present.kt`, `ui/OverheadScreen.kt`
- Create: `app/src/test/java/io/github/mohuddle/overhead/ui/PresentTest.kt`
- Modify: `MainActivity.kt` to show `OverheadScreen` from a fixed `present(...)` call with an empty snapshot. Live data is Task 7.

**Interfaces:**
- Consumes: `Place`, `Aircraft`, `nearestLine`, `aircraftLine`, `globeUrl`, `summarizeCounts` is not called here; the caller passes counts
- Produces:

```kotlin
data class RingChip(val ring: Int, val label: String, val count: Int, val selected: Boolean)
data class PlaneRow(val line: String, val insideOneMile: Boolean, val url: String)
data class ScreenState(
    val badge: String, val watching: Boolean, val statusLine: String, val statusAccent: Boolean,
    val placeholder: String, val placeLine: String?, val rings: List<RingChip>,
    val watchLabel: String, val notifyLabel: String, val clearVisible: Boolean,
    val emptyCopy: String?, val rows: List<PlaneRow>, val caption: String?,
    val placeError: String?, val deviceError: String?, val notifyError: String?
)
data class PresentInput(
    val place: Place?, val rings: List<Int>, val notify: Boolean, val watch: Boolean,
    val aircraft: List<Aircraft>, val error: String, val counts: Map<String, Int>,
    val lastSuccessProvider: String, val timeLabel: String?,
    val placeError: String?, val deviceError: String?, val notifyDenied: Boolean
)
fun present(input: PresentInput): ScreenState
```

- [ ] **Step 1: Write the failing test**

`PresentTest`:

- No place, watch false, empty aircraft: badge `Idle`, status `Set a location to watch the sky`, status accent false, placeholder `72714, Santa Monica, or 34.05, -118.24`, place line null, clear hidden, empty copy `Set a location to list nearby flights.`, rows empty, caption null, watch label `Watch`, notify label `Notify on`.
- Place label `Fayetteville`, watch true, one aircraft callsign `UAL2373` at 0.8 miles with ring 1, empty error, counts `mapOf("1" to 1)`, last provider `adsb.fi`, time `3:04 PM`: status `UAL2373 · 0.8 mi`, accent true, badge `Watching`, place line `Fayetteville · adsb.fi`, caption `adsb.fi · 3:04 PM`, watch label `Stop`, the row's `insideOneMile` true and its url is `globeUrl` of that hex, empty copy null.
- Same place, watch true, aircraft empty, error `ADS-B feeds failed (adsb.fi: down)`: status is that error, accent false, empty copy `No aircraft in range.`
- `notifyDenied` true does not change the status line and sets `notifyError` to `Notifications are off.`
- A place error `no match for 'Nope'. Try a ZIP, city, or lat, lon.` is `placeError` and is not the status line.
- Aircraft at 0.4 miles whose `ring` is 5, because the 1 mile chip is off, still has `insideOneMile` true.
- Place null with a stale aircraft list produces no rows and the no-place empty copy.
- Rings `[1, 5, 10]` and counts `"5" to 2` produce a chip labeled `5 mi` with count 2 and selected true. A rings list of `[5, 10]` leaves the `1 mi` chip unselected.

- [ ] **Step 2: Run the test and confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests io.github.mohuddle.overhead.ui.PresentTest`

Expected: compile failure because `present` is missing.

- [ ] **Step 3: Implement `present` and the composable**

Status line is `error` when it is not blank, otherwise `nearestLine(place != null, aircraft.firstOrNull())`. Accent only when watch is on, the error is blank, and the first aircraft exists. Rows are hidden when place is null. `insideOneMile` is `miles <= 1.0`. Caption is null when `lastSuccessProvider` is blank or `timeLabel` is null; otherwise `{provider} · {timeLabel}`. Place line is the label, and appends ` · {lastSuccessProvider}` when that provider is not blank.

`OverheadScreen` is a scaffold on `colorScheme.background`, center-aligned title Overhead, and a trailing badge. Watching uses `primaryContainer`. The column scrolls: status, a text field with the placeholder, Set, Device, the place line, three chips, Watch / Notify, Clear only when `clearVisible`, then rows or `emptyCopy`, then the caption. A row's click is a lambda `(String) -> Unit` given the globe url. Set, Device, Watch, Notify, Clear, and ring taps are lambdas the activity will fill in Task 7. Chip selected uses `secondary`. A 1 mile row uses `primary`. Other text uses `onBackground` and `onSurfaceVariant`.

- [ ] **Step 4: Run the test and assemble**

Run: `./gradlew :app:testDebugUnitTest --tests io.github.mohuddle.overhead.ui.PresentTest :app:assembleDebug`

Expected: PASS and assemble succeeds.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/io/github/mohuddle/overhead/ui app/src/main/java/io/github/mohuddle/overhead/MainActivity.kt app/src/test/java/io/github/mohuddle/overhead/ui
git commit -m "Show the Overhead screen from a snapshot."
```

### Task 6: One poll

**Files:**
- Create: `domain/Watch.kt`, `sky/SkyState.kt`, `sky/Poller.kt`
- Create: `app/src/test/java/io/github/mohuddle/overhead/sky/PollerTest.kt`

**Interfaces:**
- Consumes: `Place`, `Aircraft`, `AlertEvent`, `FeedChoice`, `newAlerts`, `selectToast`, `alertable`
- Produces:

```kotlin
fun shouldRunService(watch: Boolean, place: Place?): Boolean

data class SkyState(
    val aircraft: List<Aircraft>, val provider: String, val error: String,
    val updatedEpochMs: Long, val lastSuccessProvider: String, val lastSuccessEpochMs: Long
)
fun emptySky(): SkyState

data class PollResult(val sky: SkyState, val alerted: Map<String, List<Int>>, val toast: AlertEvent?, val fetched: Boolean)
fun poll(
    place: Place?, rings: List<Int>, notify: Boolean,
    alerted: Map<String, List<Int>>, sky: SkyState, secondsSinceLast: Double, nowEpochMs: Long,
    fetch: (Place, List<Int>) -> FeedChoice
): PollResult
```

`shouldRunService` is true only when watch is on and place is non-null. `poll` candidates are `sky` aircraft after a successful replace, filtered by `alertable`, before `newAlerts`. Notify off still stores the new alert map.

- [ ] **Step 1: Write the failing test**

`PollerTest`:

- `shouldRunService(true, null)` is false. `shouldRunService(false, aPlace)` is false. `shouldRunService(true, aPlace)` is true.
- `poll` with a null place does not call `fetch`, returns `fetched` false, and returns the same sky and alerted map.
- Previous sky holds one aircraft. `fetch` returns `replace false`, error `ADS-B feeds failed (adsb.fi: down; adsb.lol: down; opensky: down)`, empty provider. The result keeps that aircraft, keeps `lastSuccessProvider`, sets `error` to that sentence, and sets `provider` to empty. `toast` is null and alerted is unchanged.
- Place set, notify true, `secondsSinceLast` 8.0, `nowEpochMs` 1_700_000_000_000, empty alerted, fetch returns provider `adsb.fi` and one alertable plane at 0.8 miles. `toast.ring` is 1, the result alerted map contains that hex, `lastSuccessProvider` is `adsb.fi`, and `lastSuccessEpochMs` is 1_700_000_000_000.
- The same fetch with the alerted map from the previous result and `secondsSinceLast` 8.0 returns a null toast.
- notify false, `secondsSinceLast` 100, empty alerted, one alertable plane: toast is null and the alerted map contains the hex.
- notify true, `secondsSinceLast` 3.0, empty alerted, one alertable plane: toast is null and the alerted map contains the hex.

- [ ] **Step 2: Run the test and confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests io.github.mohuddle.overhead.sky.PollerTest`

Expected: compile failure because `poll` is missing.

- [ ] **Step 3: Implement `poll`**

A null place returns immediately. On `replace false`, update `error` and clear `provider` only. On `replace true`, replace aircraft, clear `error`, set provider, and set both timestamps to `nowEpochMs`. Run `newAlerts` on the alertable subset of the new list. Pass those events to `selectToast`.

- [ ] **Step 4: Run the test and confirm it passes**

Run the Step 2 command.

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/io/github/mohuddle/overhead/domain/Watch.kt app/src/main/java/io/github/mohuddle/overhead/sky app/src/test/java/io/github/mohuddle/overhead/sky/PollerTest.kt
git commit -m "Decide one poll's list, memory, and toast."
```

### Task 7: Service, notifications, and the live screen

**Files:**
- Create: `sky/NotifyPlan.kt`, `sky/OverheadService.kt`, `sky/SkyHolder.kt`
- Create: `ui/OverheadViewModel.kt`
- Create: `app/src/test/java/io/github/mohuddle/overhead/sky/NotifyPlanTest.kt`
- Modify: `MainActivity.kt`, `AndroidManifest.xml`

**Interfaces:**
- Consumes: `poll`, `fetchWith`, `shouldRunService`, `afterClear`, `emptySky`, `OverheadStore`, `present`, `ScreenState`
- Produces:

```kotlin
data class ChannelSpec(val id: String, val importance: String, val silent: Boolean, val ongoing: Boolean)
fun channels(): List<ChannelSpec>
sealed class AlertOpen { data class Globe(val url: String) : AlertOpen(); data object App : AlertOpen() }
fun alertOpen(event: AlertEvent): AlertOpen
const val WATCH_TITLE: String // "Overhead is watching"
const val STOP_WATCH: String // "io.github.mohuddle.overhead.action.STOP_WATCH"
fun afterWatchDismissed(config: OverheadConfig): OverheadConfig

object SkyHolder { val sky: StateFlow<SkyState>; fun publish(sky: SkyState) }
```

`channels` is `overhead-watch` low silent ongoing, `overhead-1` high, `overhead-5` default, `overhead-10` low. `alertOpen` is `App` when `extra > 0`, otherwise `Globe(globeUrl(hex))`. `afterWatchDismissed` sets watch false and leaves place, rings, notify, and alerted.

- [ ] **Step 1: Write the failing test**

`NotifyPlanTest`:

- Channel ids and importances match the four rows above. Only `overhead-watch` is silent and ongoing.
- An event with extra 0 and hex `acdfe5` opens `https://globe.adsbexchange.com/?icao=acdfe5`.
- An event with extra 2 opens `AlertOpen.App`.
- `WATCH_TITLE` is `Overhead is watching`.
- `afterWatchDismissed` on a watching config with a place returns watch false and the same place.

- [ ] **Step 2: Run the test and confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests io.github.mohuddle.overhead.sky.NotifyPlanTest`

Expected: compile failure because `alertOpen` is missing.

- [ ] **Step 3: Wire the service and the screen**

Add `lifecycle-service:2.9.2`. `OverheadService` extends `LifecycleService`. Manifest: `foregroundServiceType="dataSync"`, exported false, and permissions `INTERNET`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `POST_NOTIFICATIONS`.

The service calls `startForeground` with the `overhead-watch` notification before the first poll. Its delete intent sends `STOP_WATCH`. That action writes `afterWatchDismissed` and `stopSelf`. The tap intent opens `MainActivity`. A poll loop waits 12 seconds between fetches. It reads the app store, calls `poll` and `fetchWith`, publishes the sky, and writes alerted. A toast uses the channel for its ring. `importance` strings map to `IMPORTANCE_HIGH`, `IMPORTANCE_DEFAULT`, and `IMPORTANCE_LOW`. A `Globe` target is `ACTION_VIEW` on that url. An `App` target opens `MainActivity`. Pending intents use `FLAG_IMMUTABLE`.

`SkyHolder` starts at `emptySky()`. `OverheadViewModel` collects the app store and `SkyHolder` and exposes `ScreenState`. `PresentInput.lastSuccessProvider` is `SkyState.lastSuccessProvider`, not the provider field cleared on a failed poll. `PresentInput.error` is `SkyState.error`. The ViewModel holds `placeError`, `deviceError`, and `notifyDenied`, starting empty and false.

Set runs `classifySet`. `Manual` calls `setPlace`. `Rejected` sets `placeError` to that message. `Remote` GETs the url with header `User-Agent: USER_AGENT`, then `placeFromNominatim`. A thrown message becomes `placeError`. Watch calls `shouldRunService` and starts or stops the service. Watch or Notify requests `POST_NOTIFICATIONS` on API 33+. Denial sets `notifyDenied` and still allows Watch. Clear calls `clearPlace`, `SkyHolder.publish(emptySky())`, and clears `placeError` and `deviceError`. A row click is `ACTION_VIEW` on the url. `timeLabel` is `DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)` in the system zone, and null when `lastSuccessEpochMs` is 0.

- [ ] **Step 4: Run the test and assemble**

Run: `./gradlew :app:testDebugUnitTest --tests io.github.mohuddle.overhead.sky.NotifyPlanTest :app:assembleDebug`

Expected: PASS and assemble succeeds.

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/java/io/github/mohuddle/overhead app/src/test/java/io/github/mohuddle/overhead/sky/NotifyPlanTest.kt
git commit -m "Watch the sky and open a plane on the globe."
```

### Task 8: Device fix and boot

**Files:**
- Create: `sky/DeviceLocator.kt`, `sky/BootReceiver.kt`
- Create: `app/src/test/java/io/github/mohuddle/overhead/sky/DeviceTest.kt`
- Modify: `MainActivity.kt`, `AndroidManifest.xml`

**Interfaces:**
- Consumes: `Place`, `OverheadConfig`, `shouldRunService`, `OverheadStore`
- Produces:

```kotlin
const val DEVICE_DENIED: String // "Location permission is off. A ZIP or city works too."
const val DEVICE_TIMEOUT: String // "Couldn't read a device location."
fun devicePlace(lat: Double, lon: Double, accuracyM: Double?): Place
fun onPermissionDenied(config: OverheadConfig): OverheadConfig
```

`devicePlace` uses source `device`, a four-decimal label, and the given accuracy. `onPermissionDenied` returns the same config.

- [ ] **Step 1: Write the failing test**

`DeviceTest`:

- `devicePlace(34.0522, -118.2437, 12.0)` has source `device`, label `34.0522, -118.2437`, accuracy 12.0.
- `onPermissionDenied` of a config whose label is `Fayetteville` returns that same place.
- `DEVICE_DENIED` and `DEVICE_TIMEOUT` equal the two sentences above.

- [ ] **Step 2: Run the test and confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests io.github.mohuddle.overhead.sky.DeviceTest`

Expected: compile failure because `devicePlace` is missing.

- [ ] **Step 3: Implement the fix and the boot receiver**

Device requests `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` from the Device tap only. Denial feeds `DEVICE_DENIED` to `PresentInput.deviceError` and does not call `setPlace`. A grant uses `LocationManager`, `FUSED_PROVIDER` on API 31+ and `GPS_PROVIDER` otherwise, with a 10 second timeout. No fix shows `DEVICE_TIMEOUT` under Device. A fix calls `setPlace(devicePlace(...))`. The locator does not register a location listener.

`BootReceiver` is exported true and listens for `BOOT_COMPLETED`. It reads the store and starts the service only when `shouldRunService` is true. Add `RECEIVE_BOOT_COMPLETED`. Do not add `ACCESS_BACKGROUND_LOCATION`.

- [ ] **Step 4: Run the test, assemble, and check the manifest**

Run: `./gradlew :app:testDebugUnitTest --tests io.github.mohuddle.overhead.sky.DeviceTest :app:processDebugMainManifest`

Expected: PASS. The merged manifest at `app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml` contains `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `RECEIVE_BOOT_COMPLETED`, and `BOOT_COMPLETED`. It does not contain `ACCESS_BACKGROUND_LOCATION`. If AGP writes the merged file somewhere else, search `app/build/intermediates` for it and check that file.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/io/github/mohuddle/overhead app/src/test/java/io/github/mohuddle/overhead/sky/DeviceTest.kt
git commit -m "Add an optional device fix and resume watching after boot."
```

### Task 9: Launcher, notice, and README

**Files:**
- Create: `branding/` SVGs, `scripts/materialize_icons.py`, launcher mipmaps, `res/drawable/ic_stat_plane.xml`
- Create: `NOTICE.md`, `README.md`, `LICENSE`
- Modify: `AndroidManifest.xml` only if the icon resource names need it

**Interfaces:**
- Consumes: the app built in Tasks 1–8
- Produces: an adaptive icon and the public docs. No further Kotlin API.

- [ ] **Step 1: Write the failing check**

`branding/icon.svg` is the plugin silhouette. The check is a file assertion the implementer runs before the art exists:

```bash
test -f branding/icon.svg && test -f NOTICE.md && test -f README.md
```

Expected: the shell exits non-zero because those files are absent.

- [ ] **Step 2: Add the art and the docs**

Copy `/home/anon/Work/omarchy-overhead/icon.svg` to `branding/icon.svg` and `/home/anon/Work/omarchy-overhead/LICENSE` to `LICENSE`. Copy `scripts/materialize_icons.py` from `battery-charge-alarm` and point it at a sky `#2F6FE4` background, a limestone `#F4F2ED` Cessna foreground, and a white monochrome Cessna. Run the script. Add `ic_stat_plane.xml`, a white vector of that silhouette, and use it as the notification small icon with tint `Palette.Sky`.

`NOTICE.md` keeps the plugin's ADS-B feed notice, the Cessna attribution to `https://adsb-radar.com/help/icons/cessna.svg`, and the font SIL notices. Replace desktop paths with the Android ones: fonts in `app/src/main/res/font/`, licenses in `app/src/main/assets/`. State that a globe link is opened only from a row or a single-plane notification.

`README.md` states the app name, what Watch does, that a ZIP or city is enough, that Device asks first, the globe link, the build command with `JAVA_HOME` and `ANDROID_HOME`, and the privacy paragraph from `DESIGN.md`. It does not claim a Play listing.

- [ ] **Step 3: Verify**

Run: `test -f branding/icon.svg && test -f NOTICE.md && test -f README.md && ./gradlew :app:assembleDebug :app:testDebugUnitTest`

Expected: the file check passes, assemble succeeds, and the unit tests pass.

- [ ] **Step 4: Commit**

```bash
git add branding scripts app/src/main/res NOTICE.md README.md LICENSE app/src/main/AndroidManifest.xml
git commit -m "Add the Overhead launcher, notice, and README."
```
