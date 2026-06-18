# Android release signing

Release signing is optional in local checkouts. If no release signing values are
present, release builds remain unsigned so development and CI smoke tests can
still run without private key material unless local debug-signed release testing
is explicitly enabled.

## Versioning

Set release version values with Gradle properties or ignored `local.properties`:

```properties
cricketWatch.versionCode=1
cricketWatch.versionName=1.0
```

If absent, the app defaults to version code `1` and version name `1.0`.

## Signing inputs

The app module reads release signing values from these sources, in order:

1. Gradle properties named `cricketWatch.storeFile`,
   `cricketWatch.storePassword`, `cricketWatch.keyAlias`, and
   `cricketWatch.keyPassword`.
2. Ignored `local.properties` entries with the same `cricketWatch.*` names.
3. Ignored `keystore.properties` using the names in `keystore.properties.example`.
4. Environment variables:
   `CRICKET_WATCH_KEYSTORE_FILE`, `CRICKET_WATCH_KEYSTORE_PASSWORD`,
   `CRICKET_WATCH_KEY_ALIAS`, and `CRICKET_WATCH_KEY_PASSWORD`.

All four signing values must be present before the release signing config is
attached.

## Debug-signed release testing

For local performance testing, a release build can be signed with the debug
certificate by adding this ignored `local.properties` entry:

```properties
cricketWatch.debugSignRelease=true
```

Release signing values still take precedence when present. Do not use a
debug-signed release artifact for Play upload.

Build a release artifact:

```sh
./gradlew bundleRelease
```

If release signing values are configured, verify the generated AAB contains
signing metadata:

```sh
unzip -l app/build/outputs/bundle/release/app-release.aab | grep 'META-INF/'
```
