# FreshWall

A modern wallpaper app for Android, built with Jetpack Compose and Material 3.
Browse a hand-curated collection on the Featured tab, or search the entire
[Pexels](https://www.pexels.com) library on the Pexels tab. Favorite what
you like, set it as wallpaper, or schedule auto-rotation.

## Highlights

- **Curated + community photos** — Featured tab ships with a bundled collection;
  Pexels tab pulls from Pexels' free photography library at runtime.
- **Apply wallpaper** to lock screen, home screen, or both, with an optional
  rewarded-ad gate.
- **Auto-rotate** wallpapers on a schedule via WorkManager, sourced from your
  favorites or the Featured collection.
- **Pinch-to-zoom detail view** with shared-element transitions back to the grid.
- **Favorites** that persist across launches (Featured + Pexels alike).
- **Light / Dark / System** theming with a pitch-black dark variant.

## Tech stack

- Kotlin 2.2.10 + Jetpack Compose (BOM 2026.02.01)
- Material 3 (Expressive design language)
- Coil 2.7.0 for image loading (singleton via `ImageLoaderFactory`)
- OkHttp with a shared `RetryInterceptor`
- WorkManager 2.9.1 (auto-rotate)
- AdMob (rewarded gate)
- kotlinx.serialization for persistence
- AndroidX SplashScreen

## Repo layout

```
fresh-wall/
└── app/                       Android project root (Gradle, settings, gradlew)
    ├── PLAN.md                Working backlog / deferred work notes
    ├── build.gradle.kts
    ├── settings.gradle.kts
    ├── gradle/
    ├── gradlew, gradlew.bat
    └── app/                   :app module
        └── src/main/
            ├── java/com/example/freshwall/
            │   ├── actions/            WallpaperActions, downloads
            │   ├── ads/                Rewarded-ad gating
            │   ├── data/               Models, FavoritesManager, repositories
            │   │   └── pexels/         PexelsRepository
            │   ├── ui/
            │   │   ├── home/           Home, hero, tabs, drawer
            │   │   ├── detail/         Pinch-zoom detail + info sheet
            │   │   ├── search/         Search screen + view model
            │   │   ├── favorites/      Saved-wallpaper grid
            │   │   ├── settings/       Settings, About, Theme, Donate, Feedback
            │   │   ├── autorotate/     Auto-rotate config
            │   │   └── theme/          M3 theme (light/dark/black)
            │   ├── work/               AutoRotateWorker (WorkManager)
            │   ├── FreshWallApplication.kt  Singletons + Coil ImageLoader
            │   └── MainActivity.kt
            └── res/
```

## Setup

1. **Clone**
   ```bash
   git clone git@github.com:Drake0306/fresh-wall.git
   cd fresh-wall/app
   ```

2. **Pexels API key** — get a free key at <https://www.pexels.com/api/>, then
   add it to `app/local.properties` (this file is git-ignored):
   ```properties
   sdk.dir=/Users/you/Library/Android/sdk
   pexels.api.key=YOUR_PEXELS_KEY_HERE
   ```
   The key is exposed at build time as `BuildConfig.PEXELS_API_KEY`.

3. **AdMob** — the project currently uses Google's public **test** ad unit IDs,
   so it works out of the box. Before publishing, swap them for real IDs from
   your AdMob console (see `WallpaperActions` and `RewardedAdManager`).

4. **Firebase (optional, for in-app feedback)** — without it, the feedback
   screen falls back to an email intent. To enable Firestore-backed
   feedback:

   1. **Create a project** at <https://console.firebase.google.com>.
   2. **Register the Android app**: on the project overview page, click the
      Android icon (or **+ Add app**). Enter `com.example.freshwall` as the
      package name. Click **Register app**.
   3. **Download `google-services.json`** from the next screen and drop it at
      `app/app/google-services.json` (git-ignored).
   4. **Enable Anonymous Auth**: left sidebar → **Authentication** →
      **Sign-in method** tab → **Anonymous** → toggle on.
   5. **Enable Firestore**: left sidebar → **Build** → **Firestore Database**
      → **Create database**. Pick a region, choose **Production mode** — the
      rules we ship lock things down correctly. Works on the free Spark tier.
   6. **Enable Storage**: left sidebar → **Build** → **Storage** →
      **Get started**. Storage requires the **Blaze (pay-as-you-go) plan** as
      of 2024 — the console will prompt you to upgrade. The always-free tier
      (5 GB stored / 1 GB egress per day) covers normal feedback-screenshot
      usage, and Google offers a $300 starter credit. Pick a region (same as
      Firestore is fine) and finish.
   7. **Deploy the security rules** at the repo root:
      ```bash
      firebase deploy --only firestore:rules,storage
      ```
      (Install the CLI with `npm install -g firebase-tools` first if you
      don't have it.)
   8. **Register your debug SHA-1** (recommended) so Play Integrity attestation
      passes during sign-in:
      ```bash
      cd app && ./gradlew :app:signingReport
      ```
      Copy the `SHA1:` value under **Variant: debug** and paste it into
      **Project settings → Your apps → Add fingerprint**. Re-download
      `google-services.json` after — fingerprints get baked in.

   Submitted feedback lands in the `feedback` collection. Each doc has
   `kind`, `body`, optional `screenshotUrl`, plus device + app metadata
   (`appVersionName`, `androidSdk`, `device`, `anonId`, `createdAt`).

## Build & run

From `app/`:

```bash
# Assemble a debug APK
./gradlew :app:assembleDebug

# Install to a connected device or running emulator
./gradlew :app:installDebug

# Kotlin compile check (fast)
./gradlew :app:compileDebugKotlin

# Lint + unit tests
./gradlew :app:lintDebug
./gradlew :app:testDebugUnitTest
```

Android Studio: open `fresh-wall/app/` as the project root and use the Run
button — minSdk is **26**, targetSdk is **36**.

## Releases

Releases follow the same pattern as Muzei and similar open-source Android
apps: each Play Store version corresponds to a tagged GitHub Release with
the signed APK attached.

To cut a release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The `Release` GitHub Actions workflow (`.github/workflows/release.yml`)
picks up the tag, builds the release APK, and attaches it to a new GitHub
Release with auto-generated changelog. The same APK is what gets uploaded
to the Play Store.

**Required repo secrets** (Settings → Secrets and variables → Actions):

- `PEXELS_API_KEY` — read at build time, embedded as `BuildConfig.PEXELS_API_KEY`.

**Optional (for a signed APK that Play Store accepts):**

- `SIGNING_KEYSTORE_BASE64` — `base64 -w 0 release.keystore`
- `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`, `SIGNING_STORE_PASSWORD`

Without those four, the workflow still produces an unsigned release APK
suitable for direct-install testing.

## Known limitations

- Pexels' free API caps requests at **200/hour, 20k/month**. The repository
  uses a paged fetch (9 items/page) and a single in-flight request guard
  (`Mutex.tryLock`) to stay well under the limit.
- AdMob ad unit IDs in the codebase are **test IDs** — replace before release.
- The donation URL, support email, and Firebase setup are placeholders. See
  `PLAN.md` for the publishing checklist.

## Contributing

PRs welcome — see [CONTRIBUTING.md](CONTRIBUTING.md) for the flow, code style,
and the (short) list of things to avoid (mainly: don't bundle Pexels imagery,
don't commit ad-unit IDs or signing keys).

## License

FreshWall is licensed under the [Apache License 2.0](LICENSE). Third-party
attributions live in [NOTICE](NOTICE).
