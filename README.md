# Overhead

![Overhead icon](branding/overhead-icon.svg)

> A [Mobitecture](https://github.com/mohuddle) app · *apps, architected.*

Native Kotlin port of the Omarchy plugin [`io.github.mohuddle.overhead`](https://github.com/mohuddle/omarchy-overhead).

Package: `io.github.mohuddle.overhead`

This is the phone app. The desktop plugin stays in its own repository. The two do not share a process or a settings file.

There is no Play listing. Install a debug APK from a build of this repository. Behavior spec: **[DESIGN.md](DESIGN.md)**. Feed, font, and icon notices: **[NOTICE.md](NOTICE.md)**.

First device pass: LG G5 (`h830`), LineageOS 22.2, Android 15.

---

## Overview

Overhead lists live aircraft within 20 statute miles of a place you choose, and can notify you when one enters the 1, 5, or 10 mile ring.

A ZIP, a city, or coordinates are enough. Device GPS is a separate button. The app does not ask for location at launch, and it does not need location permission to watch a typed place.

Tapping a listed plane, or a notification for one plane, opens that aircraft on the ADS-B Exchange globe:

`https://globe.adsbexchange.com/?icao={hex}`

`{hex}` is the Mode S address in lowercase, as in `https://globe.adsbexchange.com/?icao=acdfe5`. A grouped notification names the closest plane and opens Overhead, because that toast is not one aircraft.

The globe is opened in the browser. The app does not embed it.

## The screen

One screen. The top bar says Overhead. The badge on the right says **Watching** or **Idle**.

1. **Status.** The nearest aircraft (`UAL2373 · 0.8 mi`), `No aircraft in range`, or `Set a location to watch the sky`. While Watch is on and a plane is in the list, this line uses the sky accent. A feed failure replaces it with `ADS-B feeds failed (...)` and leaves the previous list up. A bad ZIP, a failed GPS fix, and a denied notification permission do not replace this line. Those sentences sit under the control that failed.
2. **Place field.** Placeholder `72714, Santa Monica, or 34.05, -118.24`. **Set** stores what you typed. **Device** asks for location permission and takes one fix.
3. **Stored place.** The label, then the feed that last returned aircraft (`Fayetteville · adsb.fi`).
4. **Ring chips.** `1 mi`, `5 mi`, `10 mi`. A selected chip is copper. The number beside it is how many listed aircraft are inside that distance, and the counts stack: a plane at 0.4 mi counts for 1, 5, and 10. Turning the last ring off turns all three back on.
5. **Watch / Stop** and **Notify on / Notify off.** **Clear** appears only when a place is stored. Clear drops the place, stops Watch, and forgets which rings have already alerted. It leaves the ring selection and the Notify switch as they were.
6. **Aircraft list.** Each row is distance, callsign, type, and altitude, for example `0.8 mi  UAL2373  B39M  4,200 ft`. A row at 1.0 mile or closer is sky and opens the globe. Other rows open the globe too. Ground aircraft stay in the list, with altitude `ground`. Empty copy is `Set a location to list nearby flights.` or `No aircraft in range.`

Under the list, a caption names the feed and the local time of the last successful fetch.

## Place

**Set** accepts coordinates on the device, with no network lookup:

- `34.05, -118.24`
- `34.05 N 118.24 W`
- a Google Maps link that contains `@34.05,-118.24`

A parsed pair is stored with a four-decimal label (`34.0500, -118.2400`). `0, 0` is rejected. A blank field, or text that starts with `--`, shows `enter a ZIP, city, or coordinates`.

Anything else is sent to OpenStreetMap Nominatim once, when you press Set. A US ZIP (`72714` or `72714-1234`) is looked up as postal code `72714` in the United States. Other text is looked up as a place name. No match shows `no match for '{query}'. Try a ZIP, city, or lat, lon.`

**Device** requests fine and coarse location from that tap. If you deny it, the stored place stays and the button shows `Location permission is off. A ZIP or city works too.` A grant takes one fix: the fused provider on Android 12 and later, GPS on older releases, with a 10 second timeout. The label is the four-decimal coordinates. No fix shows `Couldn't read a device location.` Watch does not subscribe to location updates.

## Watch

While Watch is on and a place is stored, a foreground service polls every 12 seconds. The ongoing notification is titled **Overhead is watching**. Tapping it opens the app. Dismissing it turns Watch off and stops the service.

Fetch order, stopping at the first feed that still has aircraft after filtering:

1. [adsb.fi](https://opendata.adsb.fi/) open data
2. [adsb.lol](https://adsb.lol/) public API
3. [OpenSky Network](https://opensky-network.org/) `states/all` in a box of the same radius

The query radius is 25 nautical miles. The list keeps aircraft inside 20 statute miles, drops positions older than 60 seconds, sorts by distance, and caps at 40. Aircraft on the ground stay on the list. ADS-B Exchange's keyed API is not called.

If every feed fails, the status line is `ADS-B feeds failed (...)` and the list from the last success stays. An empty answer from adsb.fi or adsb.lol still tries the next feed.

Watch with no stored place does not start the service. If Watch was on and a place was stored, boot starts the service again. A reboot with Watch off does nothing. Opening the app also resumes Watch when a place is saved.

### How long Watch can run

The service type is `dataSync`. On Android 15 and later, while the app is in the background, that type is limited to about six hours in a rolling day. When the system reaches the limit, Overhead stops the service, turns Watch off, and sets the status line to `Watch stopped. Android limits how long this can run in the background.` The place, rings, and Notify switch stay. Turn Watch on again when you want another session. Starting it again the same day can hit the same limit. There is no battery-optimization prompt.

## Alerts

An aircraft alerts when it newly enters an enabled ring. The toast uses the smallest new ring. The same aircraft does not alert again for a ring it is still inside. Leaving every enabled ring forgets it. Coming back alerts again.

Ground aircraft, and aircraft under 500 ft, stay on the list and do not alert. Missing altitude does not by itself suppress the alert.

Notify off still updates the list and the memory of which rings have already fired. It does not post ring toasts.

At most one ring toast per poll, and not sooner than 8 seconds after the previous one. A ring skipped only because of that gap is stored, so it does not toast again when the gap ends. Several aircraft in one poll collapse into one toast: the closest is named, and the title counts the rest.

| Situation | Title |
| --- | --- |
| One plane, 1 mile | Plane overhead |
| One plane, 5 or 10 miles | Plane within {ring} miles |
| Several planes | {n} aircraft within {ring} mile or miles |

The body names the callsign, type, distance, altitude, and speed when those exist. A group adds `{n} more inside the same ring.`

| Channel | Importance | When |
| --- | --- | --- |
| `overhead-watch` | low, silent | Watch is on |
| `overhead-1` | high | 1 mile ring |
| `overhead-5` | default | 5 mile ring |
| `overhead-10` | low | 10 mile ring |

Notification permission is requested when you turn Watch on or turn Notify on. If you deny it, Watch still runs, ring toasts are skipped, and **Notify** shows `Notifications are off.` The app reads that permission again on launch. A ring that could not be posted because permission is off is not stored, so granting notifications later can still announce a plane that is inside a ring. Turning Notify off is different: the ring is stored, and turning Notify back on does not announce a plane that is still inside it.

## Permissions

| Permission | Why |
| --- | --- |
| Internet | ADS-B feeds and, only when you press Set, Nominatim |
| Notifications | The watching notice and ring toasts |
| Foreground service (`dataSync`) | The 12 second poll while Watch is on |
| Fine and coarse location | The Device button only |
| Boot completed | Resume Watch after reboot when a place is saved |

There is no background-location permission.

## Privacy

The stored place stays in the app's DataStore. A poll sends that lat/lon and a radius to adsb.fi, adsb.lol, or OpenSky. Nominatim receives the typed query only when Set is pressed. The globe URL runs only when a row or a single-plane notification is tapped, and the only identifier in it is the hex. There is no account, no analytics, and no IP geolocation.

The User-Agent on those requests is `overhead-android/0.1.0 (personal ADS-B watcher; https://github.com/mohuddle/overhead-android)`.

Android backup is left at the platform default, so a device backup can include the preferences file that holds the place.

## Build and install

JDK 17 or newer, and an Android SDK with platform 36. Point Gradle at the SDK with `ANDROID_HOME` or `sdk.dir` in `local.properties` (that file stays untracked).

```bash
git clone https://github.com/mohuddle/overhead-android.git
cd overhead-android
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`minSdk` is 29 (Android 10). `compileSdk` and `targetSdk` are 36. Version 0.1.0, version code 1. Unit tests cover the distance, place, feed, alert, screen, and poll rules. They do not call the live feeds or Nominatim.

The debug APK is signed with the local debug key. There is no release signing config in Gradle.

## Technical specs

| | |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3, dynamic color off |
| minSdk | 29 |
| compileSdk / targetSdk | 36 |
| Application id | `io.github.mohuddle.overhead` |
| Place, rings, alerts | DataStore preferences, one store on the application |
| Live list | Memory only. Process death reloads the place and refetches. It does not invent a list. |
| Domain | `domain/Geo.kt`, `PlaceQuery.kt`, `Aircraft.kt`, `Alerts.kt`, `Feeds.kt`, `Watch.kt` |
| Screen | `ui/Present.kt`, `ui/OverheadScreen.kt` |
| Service | `sky/OverheadService.kt`, poll every 12 seconds |
| Desktop oracle | plugin `overhead/*.py`, `Model.js`, and their tests |

Ink `#12141B`, limestone `#F4F2ED`, copper `#EE9A5B` / `#D9783A`, sky `#2F6FE4`. Titles are Geist, counts are Geist Mono, body is Inter. The launcher is a sky squircle with the Cessna silhouette from [adsb-radar.com](https://adsb-radar.com/help/icons/cessna.svg).

## Not in v1

An in-app map, a WebView of the globe, continuous GPS, background location, a ground-traffic switch, a Play listing, and sync with the desktop plugin's state directory.

## License

MIT. See [LICENSE](LICENSE). Feeds, the Cessna glyph, and the font licenses are in [NOTICE.md](NOTICE.md).

---

Made by [Mobitecture](https://github.com/mohuddle) · apps, architected.
