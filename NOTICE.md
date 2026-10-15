# Notices

## ADS-B data

Live positions come from public ADS-B feeder networks that speak the same
readsb / ADS-B Exchange v2 JSON used by [globe.adsbexchange.com](https://globe.adsbexchange.com/):

- [adsb.fi](https://opendata.adsb.fi/) open data API
- [adsb.lol](https://adsb.lol/) public API
- [The OpenSky Network](https://opensky-network.org/) as a last-resort fallback

ADS-B Exchange's own REST gateway is keyed and is not called. This project
does not scrape the globe site. A globe link is opened only from a listed
row or a single-plane notification, and the only identifier in that URL is
the aircraft hex.

Please treat the feeds gently (this watcher polls about once every 12 seconds)
and consider running a receiver if you use the data regularly.

## Cessna icon

The launcher and notification glyph is the Cessna silhouette published at
[adsb-radar.com/help/icons/cessna.svg](https://adsb-radar.com/help/icons/cessna.svg).
The source file in this repo is `branding/icon.svg`.

## Fonts

Geist, Geist Medium, and Geist Mono are under the SIL Open Font License 1.1.
The fonts are `app/src/main/res/font/geist.ttf`, `geist_medium.ttf`, and
`geist_mono.ttf`. The license is `app/src/main/assets/LICENSE_GEIST.txt`.

Inter is under the SIL Open Font License 1.1. The font is
`app/src/main/res/font/inter.ttf`. The license is
`app/src/main/assets/LICENSE_INTER.txt`.

## Location

The default path is a ZIP, city, or coordinates you type. That query goes to
OpenStreetMap Nominatim only because you pressed Set. Device location runs
only when you press Device, which asks for permission first, and takes one
fix. There is no background-location permission and no IP geolocation. The
stored place stays in the app DataStore and is sent only as a lat/lon radius
query to the ADS-B APIs above.
