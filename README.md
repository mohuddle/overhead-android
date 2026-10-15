# Overhead

Overhead watches nearby flights on Android. It is the phone port of the Omarchy plugin `io.github.mohuddle.overhead`, with one extra action: open a plane on the ADS-B Exchange globe.

A Mobitecture app. Ink `#12141B`, limestone `#F4F2ED`, copper `#EE9A5B`, and Overhead sky `#2F6FE4`.

## Watch

Set a place, then turn Watch on. While Watch is on and a place is stored, a foreground service polls adsb.fi, then adsb.lol, then OpenSky every 12 seconds and lists aircraft inside 20 miles. Rings at 1, 5, and 10 miles can notify you when a plane newly enters them.

A ZIP, a city, or coordinates are enough. You do not need device GPS. Device asks for location permission only when you tap it, then takes one fix.

Tapping a listed plane, or a notification for one plane, opens `https://globe.adsbexchange.com/?icao={hex}` with the hex in lowercase, for example `https://globe.adsbexchange.com/?icao=acdfe5`. A grouped notification opens Overhead.

## Privacy

The stored place stays in the app's DataStore. A poll sends that lat/lon and a radius to adsb.fi, adsb.lol, or OpenSky. Nominatim receives the typed query only when Set is pressed. The globe URL runs only when a row or a single-plane notification is tapped, and the only identifier in it is the hex. There is no account, no analytics, and no IP geolocation.

## Build

```bash
JAVA_HOME=/home/anon/.local/jdk-21 ANDROID_HOME=/home/anon/android-sdk ./gradlew :app:assembleDebug
JAVA_HOME=/home/anon/.local/jdk-21 ANDROID_HOME=/home/anon/android-sdk ./gradlew :app:testDebugUnitTest
```

The debug APK is the install. See `NOTICE.md` for the ADS-B feeds, the Cessna glyph, and the font licenses.
