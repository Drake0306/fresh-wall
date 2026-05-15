# FreshWall — Release cheat-sheet (personal notes)

A quick-reference for future-me. Not part of the public docs; just enough
to rebuild the release flow from memory.

## The keystore lives at

```
fresh-wall/do not delete/freshwall-release.jks
```

Folder is named "do not delete" on purpose so it screams at you in
Finder. It's also gitignored (see `.gitignore`).

**Back it up to at least two more places** the moment it's created:
1. Cloud storage (Google Drive / Dropbox / iCloud)
2. External drive or second machine

Lose this file and the Play Store listing is bricked — there's no way to
push updates under `io.github.drake0306.freshwall` ever again.

## Keystore metadata I used

- Path: `<repo>/do not delete/freshwall-release.jks`
- Key alias: `freshwall`
- Keystore password: stored in password manager (also used for key password)
- Key password: same as keystore password
- Validity: 25 years

If keystore + key passwords differ, both go into `local.properties` separately.

## Build a signed release APK (Android Studio path)

1. Open `app/` in Android Studio.
2. **Build → Generate Signed Bundle / APK…**
3. Pick **APK** (or **Android App Bundle** for Play Store).
4. Use existing keystore → point at `do not delete/freshwall-release.jks`.
   Enter the keystore password. Alias: `freshwall`. Same password.
5. Check **Remember passwords**.
6. Build variant: **release** (never debug).
7. Click **Create**.

Output:
- APK: `app/build/outputs/apk/release/app-release.apk` (~21 MB, signed)
- AAB: `app/build/outputs/bundle/release/app-release.aab` (Play Store)

## Or via CLI

Add to `app/local.properties` (gitignored):

```properties
release.keystore.path=/Users/roy/Developer/Github/fresh-wall/do not delete/freshwall-release.jks
release.keystore.password=YOUR_KEYSTORE_PASSWORD
release.key.alias=freshwall
release.key.password=YOUR_KEYSTORE_PASSWORD
```

Then from `app/`:

```bash
./gradlew :app:assembleRelease   # signed APK
./gradlew :app:bundleRelease     # signed AAB for Play Store
```

## Verify it's signed (sanity check)

```bash
~/Library/Android/sdk/build-tools/<latest-version>/apksigner verify --verbose \
  app/build/outputs/apk/release/app-release.apk
```

Should print "Verifies" with your signer's certificate fingerprint.

## Bump version before each release

`app/app/build.gradle.kts:58-59`:

```kotlin
versionCode = 2          // increment every release, even hotfixes
versionName = "1.0.0-beta.1"   // human-readable
```

Play Store **requires** `versionCode` to be strictly greater than the
previously uploaded value. `versionName` is just shown to users.

## Cutting a GitHub Release

After the APK is built:

```bash
git tag v1.0.0-beta.1
git push origin v1.0.0-beta.1
```

The `release.yml` GitHub Actions workflow picks up the tag, builds the
release APK, and attaches it to a new GitHub Release with auto-generated
changelog. The signed APK Android Studio just produced can also be
attached manually to the release page if the workflow's signing creds
aren't set up.

## Releases naming convention

- Pre-public beta: `v1.0.0-beta.1`, `v1.0.0-beta.2`, …
- First public release: `v1.0.0`
- Hotfix: `v1.0.1` (always bump `versionCode` too)
- Big feature drop: `v1.1.0`

## Codenames (optional, for fun)

If I name a release, pick something that hints at the headline feature:
- v1.0.0-beta.1 = "Pinterest" (staggered grid + long-press preview)
