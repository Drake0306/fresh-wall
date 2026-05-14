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

## Known limitations

- Pexels' free API caps requests at **200/hour, 20k/month**. The repository
  uses a paged fetch (9 items/page) and a single in-flight request guard
  (`Mutex.tryLock`) to stay well under the limit.
- AdMob ad unit IDs in the codebase are **test IDs** — replace before release.
- The donation URL, support email, and Firebase setup are placeholders. See
  `PLAN.md` for the publishing checklist.

## License

Not currently licensed. All rights reserved.
