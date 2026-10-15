# Overhead — Android v1

Native port of the Omarchy plugin [`io.github.mohuddle.overhead`](https://github.com/mohuddle/omarchy-overhead) (v0.2.1), plus one phone-only action: open a plane on the ADS-B Exchange globe.

The phone and the desktop do not share a process or a settings file.

## Product

- You set a place. The app lists live aircraft within 20 miles and, while Watch is on, notifies you when one enters the 1, 5, or 10 mile ring.
- A typed ZIP, city, or coordinates is enough. Device GPS is a separate button. It is never requested at launch and is never required.
- Tapping a listed plane, or a notification for one plane, opens `https://globe.adsbexchange.com/?icao={hex}` in the browser. `{hex}` is the Mode S address, lowercase, as in `https://globe.adsbexchange.com/?icao=acdfe5`.
- A grouped notification names the closest plane and opens Overhead, because that toast is not a single aircraft.

Out of v1: an in-app map, a WebView of the globe, continuous GPS, background-location permission, the TUI, the CLI, GeoClue, Quickshell, a Play listing, and sync with `~/.local/state/omarchy/overhead/`.

## Constraints

- Package and namespace: `io.github.mohuddle.overhead`
- App name: Overhead
- Repository: https://github.com/mohuddle/overhead-android
- Kotlin, Jetpack Compose, Material 3. Dynamic color off.
- `minSdk 29`, `compileSdk` / `targetSdk` 36
- AGP 8.11.0, Kotlin 2.3.20, Compose BOM 2025.10.01, activity-compose 1.10.1, DataStore preferences 1.1.7
- Java 17 via `compilerOptions { jvmTarget }` so Gradle does not provision a JDK. The Gradle 8.13 wrapper is in the repository. Build with JDK 17 or newer and an Android SDK that has platform 36.
- Sideload APK. No signing config in Gradle for v1.
- MIT for app code. Carry `NOTICE.md` from the plugin, rewritten for the Android paths.
- Domain code is a JVM port of `overhead/geo.py`, `overhead/ads.py` (normalize and filter), `overhead/alerts.py`, and the status strings in `Model.js`. No Android imports in that code. `tests/test_overhead.py` and `tests/model.test.js` are the oracle. Desktop socket, path, and `notify-send` tests are not ported.

## Look

Same studio frame as ChargeAlert and Aftercast.

| Token | Hex | Use |
| --- | --- | --- |
| Ink | `#12141B` | Dark background, light-theme text |
| Graphite | `#1D2029` | Dark surface |
| Slate | `#2C303C` | Dark surface variant |
| Concrete | `#8D919B` | Outline, muted labels |
| Concrete deep | `#5E6470` | Light-theme secondary text |
| Limestone | `#F4F2ED` | Light background |
| Travertine | `#E6E1D7` | Light surface variant |
| Copper | `#EE9A5B` | Dark-theme secondary |
| Copper deep | `#D9783A` | Light-theme secondary |
| Sky | `#2F6FE4` | Overhead's own hue |
| Sky bright | `#7AABFF` | Dark-theme primary |
| Sky deep | `#1C4EAE` | Light-theme primary |
| Sky ink | `#0E2A5C` | Text on sky mist |
| Sky mist | `#D6E4FB` | Light primary container |

Sky is primary. Copper is secondary. The light primary is Sky deep on limestone, and the dark primary is Sky bright on ink. Titles are Geist, labels are Geist Mono, body is Inter. The font files are in `app/src/main/res/font/`. The SIL OFL notices are in `app/src/main/assets/`.

Sky marks the watching badge, the nearest line while watching, and any row inside 1 mile. Selected ring chips use copper. Set uses the primary filled button. Everything else stays ink, limestone, and concrete.

The launcher is the Mobitecture Overhead mark from the desktop repo (`branding/overhead-icon-vibrant.svg`): a sky squircle, the side-view plane, and a copper wing light. Adaptive layers live in `branding/android/`. `scripts/materialize_icons.py` copies them into the resource tree. The notification small icon is that plane in white, tinted Sky. `branding/icon.svg` is the desktop bar's Cessna and is not the phone launcher.

## Screen

One screen. Scaffold, limestone or ink background, center-aligned top bar titled Overhead. The bar's trailing badge says Watching or Idle. Watching uses the primary container.

Under the bar, in one scroll column:

1. Status line. The feed error if there is one, otherwise the nearest line from `Model.js`: `UAL2373 · 0.8 mi`, `No aircraft in range`, or `Set a location to watch the sky`. Accent while watching and there is a nearest plane. Place-field failures and a denied notification permission do not replace this line. They sit under the control that failed.
2. Place field. Placeholder `72714, Santa Monica, or 34.05, -118.24`. **Set** geocodes the draft. **Device** asks for location permission and takes one fix. Device is always visible. It does not depend on a desktop GeoClue check.
3. Stored place, muted: the label, then the provider when a fetch has succeeded (`Fayetteville · adsb.fi`).
4. Three ring chips, `1 mi`, `5 mi`, `10 mi`. A selected chip uses copper. The count for that ring is Geist Mono beside the label. Counts are cumulative, matching `_counts`: a plane at 0.4 mi increments 1, 5, and 10.
5. **Watch** / **Stop**, and **Notify on** / **Notify off**. **Clear** shows only when a place is stored. Clear drops the place, stops watching, and clears alert memory.
6. Aircraft list. Empty copy: `Set a location to list nearby flights.` or `No aircraft in range.` A row is `0.8 mi  UAL2373  B39M  4,200 ft`, the same pieces as `aircraftLine`. Ground is the altitude word when the row is on the ground. A row inside 1 mile is Sky. Tap opens the globe URL. The list does not embed the site.

A caption under the list shows the provider and the local time of the last successful fetch.

## Location

First launch has no place, Watch off, Notify on, all three rings on. Nothing is fetched.

**Set** parses coordinates locally with the desktop rules (`34.05, -118.24`, `34.05 N 118.24 W`, a Google Maps `@lat,lon` URL). A parsed pair is stored with source `manual` and a four-decimal label. It is not sent to a geocoder. `0, 0` is invalid. A leading `--` is rejected with `enter a ZIP, city, or coordinates`.

Anything else goes to OpenStreetMap Nominatim once, when Set is pressed. A US ZIP (`^\d{5}(?:-\d{4})?$`) is `postalcode` plus `country=us`, using the first five digits. Any other text is `q`. Limit 1, `format=jsonv2`. No match: `no match for {query}. Try a ZIP, city, or lat, lon.` User-Agent: `overhead-android/0.1.0 (personal ADS-B watcher; https://github.com/mohuddle/overhead-android)`.

**Device** requests `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` from that tap. Denial leaves the stored place as it was and shows `Location permission is off. A ZIP or city works too.` under the Device button. A grant takes one `LocationManager` fix (fused provider on API 31+, otherwise GPS). The fix is stored with source `device`, the four-decimal coordinates as the label, and `accuracy_m`. Ten seconds with no fix shows `Couldn't read a device location.` under that button. A Nominatim miss shows its sentence under Set. Watching does not subscribe to location updates and does not request `ACCESS_BACKGROUND_LOCATION`.

## Sky

While Watch is on and a place is stored, a foreground service polls every 12 seconds. The service type is `dataSync`. There is no battery-optimization prompt.

On Android 15 and later, a background `dataSync` service is limited to about six hours in a rolling day. When that limit hits, the service stops, Watch is set off, and the status line becomes `Watch stopped. Android limits how long this can run in the background.` The place, rings, and Notify switch stay. Turning Watch on again the same day can hit the same limit. Opening the app resumes Watch when a place is already stored.

Fetch order, stopping at the first feed that returns aircraft:

1. `https://opendata.adsb.fi/api/v3/lat/{lat}/lon/{lon}/dist/{nm}`
2. `https://api.adsb.lol/v2/lat/{lat}/lon/{lon}/dist/{nm}`
3. OpenSky `states/all` in a bounding box of the same radius

`nm` is 25. The list keeps aircraft inside 20 statute miles, drops positions older than 60 seconds (`seen` or `seen_pos`), sorts by distance, and caps at 40. Aircraft on the ground stay on the list. ADS-B Exchange's keyed API is not called.

The desktop default also drops ground aircraft in `ads._filter` when `ignore_ground` is true. Android does not. The list shows them, and `alertable` keeps them out of notifications, which is the rule already approved for this app. There is no ground switch in v1.

If every feed fails, the status line is `ADS-B feeds failed (...)` and the previous list stays. An empty success from adsb.fi or adsb.lol still falls through to OpenSky, matching `fetch_nearby`.

Rings default to 1, 5, and 10. Turning the last ring off restores all three, matching `normalize_rings`.

## Alerts

Alert state is the map of hex to the rings that aircraft is already inside. It is stored on disk so a killed service does not repeat the same toast. `new_alerts` is unchanged from `alerts.py`:

- An aircraft alerts when it is newly inside an enabled ring.
- The event's ring is the smallest newly entered ring.
- The same hex does not alert again for a ring it is still inside.
- Leaving every enabled ring forgets that hex. Coming back alerts again.
- Ground, and altitude under 500 ft, are not candidates.

Notify off still updates the list and the alert map. It does not post ring toasts.

At most one ring toast per poll, and not sooner than 8 seconds after the previous one. Several events in one poll collapse through `coalesce`: the closest aircraft is named, and `extra` is the count of the rest.

Copy, from `title_for` / `body_for`:

- One plane, ring 1: title `Plane overhead`
- One plane, ring 5 or 10: title `Plane within {ring} miles`
- Group: title `{n} aircraft within {ring} mile` or `miles`
- Body: `{callsign} ({type}) is {miles} away, {alt}, {speed} kt.` Type, altitude, and speed are omitted when missing. A group appends `{extra} more inside the same ring.`

Channels:

| Id | Importance | When |
| --- | --- | --- |
| `overhead-watch` | low, silent, ongoing | Watch is on |
| `overhead-1` | high | Ring 1 |
| `overhead-5` | default | Ring 5 |
| `overhead-10` | low | Ring 10 |

The ongoing notice is titled `Overhead is watching`. Tapping it opens the app. Dismissing it sets Watch off and stops the service. Tapping a single-plane alert opens that plane's globe URL. Tapping a grouped alert opens the app.

`POST_NOTIFICATIONS` is requested when the user turns Watch on, or turns Notify on. If they deny it, Watch still runs, ring toasts are skipped, and a line under the Notify control says `Notifications are off.` The app reads that permission again on launch. A ring that could not be posted because permission is off is not stored, so granting notifications later can still announce a plane that is inside the ring. The 8 second gap still stores the ring.

Watch with no stored place does not start the service. The status line stays `Set a location to watch the sky`.

If Watch was on and a place was stored, `BOOT_COMPLETED` starts the service again. A reboot with Watch off does nothing.

## Data

DataStore preferences:

| Key | Default |
| --- | --- |
| `lat`, `lon` | absent |
| `source`, `label` | empty |
| `accuracyM` | absent |
| `rings` | `1,5,10` |
| `notify` | true |
| `watch` | false |
| `alerted` | `{}` |

`alerted` is a JSON object of hex to a list of ints. The live aircraft list, provider, error, and last fetch time are memory only. The service publishes them to the activity through a process-local holder the screen collects. Process death reloads the DataStore and refetches. It does not invent a list.

## Errors

Feed failures use the status line, not a dialog. Geocoder, device-fix, and notification-permission failures use the sentence under that control. A corrupt `alerted` value is treated as `{}`. A corrupt rings value is treated as `1,5,10`.

## Tests

`./gradlew :app:testDebugUnitTest` covers the domain port. Required cases, with the same numbers as the desktop tests:

- NYC `(40.7128, -74.0060)` to Philadelphia `(39.9526, -75.1652)` is between 79 and 83 miles.
- `parse_location_text` accepts `34.05, -118.24`, `34.05 N 118.24 W`, and `https://maps.google.com/@34.05,-118.24,14z`. It rejects `Santa Monica` and `91, 0`. `valid_coords(0, 0)` is false.
- ZIP pattern accepts `72714` and `72714-1234` and rejects `Santa Monica`.
- `rings_inside(0.4) = [1, 5, 10]`, `rings_inside(3.2) = [5, 10]`, `rings_inside(12) = []`, `innermost_ring(3.2) = 5`.
- `format_miles(0.8) = 0.8 mi`, `format_miles(12.4) = 12 mi`.
- Exchange sample hex `A5D28C`, flight `UAL2373 `, type `B39M`, origin `(34.0195, -118.4912)`: hex `a5d28c`, callsign `UAL2373`, ring 1, under 1 mile, not ground. Globe URL for that row is `https://globe.adsbexchange.com/?icao=a5d28c`.
- `alt_baro: "ground"` sets `ground`. OpenSky row altitude `1280.16` m is 4200 ft within 5.
- Entering at 0.8 mi alerts once at ring 1 with title `Plane overhead` and urgency critical. The next poll alerts nothing. Moving to 8 mi alerts nothing. Leaving and returning at 8 mi alerts ring 10, and the body contains `8.0 mi`.
- Three events at 4.0, 2.0, and 3.0 mi coalesce to callsign `B` with `extra` 2 and a title that contains `3 aircraft`.
- `alertable` is false for ground and for 200 ft, true for 1200 ft.

Feed HTTP and Nominatim are not called from unit tests. A fake client covers provider fallback: adsb.fi error, then adsb.lol empty, then OpenSky used.

## Privacy

The stored place stays in the app's DataStore. A poll sends that lat/lon and a radius to adsb.fi, adsb.lol, or OpenSky. Nominatim receives the typed query only when Set is pressed. The globe URL runs only when a row or a single-plane notification is tapped, and the only identifier in it is the hex. There is no account, no analytics, and no IP geolocation.

## Status

v1 of this spec is implemented. The slice order was scaffold and theme, domain plus the tests above, DataStore, the screen against a fake status, geocoding and feeds, the foreground service and notifications, the Device fix, boot resume, then the launcher, `NOTICE.md`, and `README.md`. The working notes for that order are in `docs/superpowers/plans/2026-10-14-overhead-android.md`. That file is a build log. It still names the machine paths used while writing it. The README is the build guide.
