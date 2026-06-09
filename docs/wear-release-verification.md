# Wear release verification

Run this before treating a CricketWatch artifact as a release candidate.

Use the supported local environment:

```sh
env JAVA_HOME=/var/home/nedr/.jdks/jbr-17.0.14 \
  PATH=/var/home/nedr/.jdks/jbr-17.0.14/bin:$PATH \
  ANDROID_HOME=/var/home/nedr/Android/Sdk \
  ANDROID_SDK_ROOT=/var/home/nedr/Android/Sdk \
  GRADLE_USER_HOME=/tmp/watch-cricket-gradle
```

## Build checks

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
./gradlew :app:assembleRelease :app:bundleRelease
```

Record the debug APK, release APK and release AAB sizes:

```sh
ls -lh app/build/outputs/apk/debug \
  app/build/outputs/apk/release \
  app/build/outputs/bundle/release
```

## Small round watch check

Use either a small round Wear OS emulator profile around 1.2 inches / 192dp or a
physical watch with equivalent constraints.

Install the debug APK:

```sh
adb -s <device-id> install -r app/build/outputs/apk/debug/app-debug.apk
```

If the device already has the app installed with a different debug certificate,
uninstall once and reinstall:

```sh
adb -s <device-id> uninstall com.nedrichards.cricketwatch
adb -s <device-id> install app/build/outputs/apk/debug/app-debug.apk
```

Capture release evidence:

```sh
adb -s <device-id> exec-out screencap -p > /tmp/cricket-watch-small-round.png
```

Required checks:

- loading, error, empty and populated match-list states are readable
- match cards fit team abbreviations, scores, overs and status without overlap
- manual Refresh chip is reachable
- Last updated text is visible and not misleading
- larger system font remains usable
- rotary scrolling works through the match list
- no API key, API-limit and network-failure states are clear enough for release

Keep the screenshot path and tested device/profile name with release notes.
