# Stable Android debug signing

Android preserves app data across updates only when the replacement APK is
signed with the same certificate as the installed package. If different agents
or tool environments generate different debug keystores, `adb install -r` can
fail with:

```text
INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package ... signatures do not match newer version
```

Use one stable debug keystore for local development builds. In a normal Android
development environment, use:

```text
$HOME/.android/debug.keystore
```

Its expected SHA-256 fingerprint is:

```text
7F:23:B6:FA:11:F3:38:BC:E6:90:EA:5D:FE:13:CB:42:9E:36:C0:E8:F4:A5:43:29:CD:12:3C:FB:60:F4:A2:50
```

Add these entries to ignored `local.properties`:

```properties
androidDebugSigning.storeFile=$HOME/.android/debug.keystore
androidDebugSigning.storePassword=android
androidDebugSigning.keyAlias=androiddebugkey
androidDebugSigning.keyPassword=android
```

If `androidDebugSigning.storeFile` is absent, the project falls back to the
Android Gradle Plugin's normal debug signing behavior.

Verify the debug APK:

```sh
./gradlew assembleDebug
/var/home/nedr/Android/Sdk/build-tools/36.0.0/apksigner verify --print-certs \
  app/build/outputs/apk/debug/app-debug.apk
```

If a watch or emulator already has `com.nedrichards.cricketwatch` installed with
a different debug certificate, uninstall it once and reinstall.
