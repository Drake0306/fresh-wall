# Notes for Claude Code

This file gives Claude Code the context it needs to work on FreshWall
productively.

## Working directory

The Android project root is **`app/`**, not the repo root. All Gradle commands
have to run from there:

```bash
cd app
./gradlew :app:compileDebugKotlin
```

The outer `fresh-wall/` folder just hosts README/CLAUDE/PLAN/.gitignore.

## Build & verify

- Compile only (fastest sanity check): `./gradlew :app:compileDebugKotlin`
- Full debug APK: `./gradlew :app:assembleDebug`
- Install on connected device: `./gradlew :app:installDebug`
- Lint: `./gradlew :app:lintDebug`
- Unit tests: `./gradlew :app:testDebugUnitTest`

There are no instrumented tests yet — feature correctness has to be verified
on a device or emulator.

## Required local config

`app/local.properties` must contain a Pexels API key:

```properties
pexels.api.key=...
```

It's exposed via `BuildConfig.PEXELS_API_KEY` (declared in
`app/app/build.gradle.kts`). Without it the Pexels tab will fail to load.

## Architecture in one paragraph

- `FreshWallApplication` owns the singletons: `rewardedAdManager`,
  `themePreferences`, `favoritesManager`, `autoRotatePreferences`,
  `wallpaperActions`, `searchHistoryManager`, `pexelsRepository`, plus the
  shared `OkHttpClient` with `RetryInterceptor` and Coil's `ImageLoaderFactory`.
- `MainActivity` uses a stack-based navigator (`List<Screen>`) inside a
  `SharedTransitionLayout` + `AnimatedContent`. The Home ↔ Search transition
  expands/contracts from the bottom-right corner; everything else is a fade.
- Home (`ui/home/HomeScreen.kt`) hosts two tabs (Featured / Pexels) and a
  fixed bottom pill rack (3 pills: menu, tabs, search). Title pill at top
  fades + scales out as the user scrolls; the Pexels mark fades into the title
  pill when the Pexels tab is active.
- Featured grid is filtered by category chips (`WallpaperCategory`): All,
  Daily (date-seeded shuffle), Trending, Curated, Classics.
- Pexels grid paginates 9 items/page, race-safe via `Mutex.tryLock` and
  `.distinctBy { it.id }` on every append.
- Detail screen (`ui/detail/`) renders with `ContentScale.Fit` plus a
  `graphicsLayer { scaleX/scaleY }` that auto-zooms to a "fitCrop" ratio on
  open. User can pinch out to scale 1 (Fit baseline — whole image visible).
- Favorites store full `Wallpaper` objects (not just IDs) so Pexels favorites
  show up correctly. Persisted as JSON in SharedPreferences under
  `favorites_v2`.

## Conventions

- No emojis in code, comments, commits, or UI unless the user explicitly asks.
- Comments are the exception, not the default — only explain *why*, never *what*.
- Prefer editing existing files; don't create new docs unless requested.
- Material 3 Expressive: selected pills get `secondaryContainer` /
  `onSecondaryContainer`; floating pills sit on `surfaceContainerHigh` with
  small tonal + shadow elevation.
- Use `vectorResource(R.drawable.ic_pexels)` for the Pexels brand mark
  (`app/app/src/main/res/drawable/ic_pexels.xml`). The fillColor is white so
  `Icon(tint = ...)` fully controls the color.

## Files most often touched

- `app/app/src/main/java/com/example/freshwall/ui/home/HomeScreen.kt` — screen
  layout, scroll-driven title state, category filter wiring.
- `app/app/src/main/java/com/example/freshwall/ui/home/HomeComponents.kt` —
  shared bits: `HomeTitleBar`, `BottomNavBar`, `TabsPill`, `TabSegment`,
  `CategoryChipsRow`, `WallpaperTile`.
- `app/app/src/main/java/com/example/freshwall/ui/detail/DetailScreen.kt` —
  pinch-zoom, immersive mode, apply/download sheet.
- `app/app/src/main/java/com/example/freshwall/data/FavoritesManager.kt` —
  JSON-persisted favorites of `Wallpaper`.
- `app/app/src/main/java/com/example/freshwall/data/pexels/PexelsRepository.kt`
  — paged search/curated calls + friendly HTTP error mapping.
- `app/PLAN.md` — deferred backlog (Crashlytics setup, Pexels API limits,
  publish checklist).

## Watchouts

- Don't mock the Pexels API in tests — the wrap-on-failure friendly messaging
  is part of the contract.
- Don't add `--no-verify` or skip hooks when committing.
- `WallpaperManager.setBitmap` can silently return 0 on some OEM skins; the
  code already throws if it does. Preserve that.
- `runCatching` swallows `CancellationException` — use the
  `cancellationAwareCatch` helper in `PexelsRepository` instead.
