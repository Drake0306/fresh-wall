# Onboarding flow — working notes

Snapshot of the onboarding work so any future session can pick up where
this one left off without re-reading the diff.

## Branch / commit state at write time

- Last pushed commit: **`31c1a6e`** — *feat: Implement Unsplash integration
  and enhance search functionality* (author: Drake0306, 2026-05-15).
- Branch: `main`, tracking `origin/main`.
- **Uncommitted, staged** (not pushed):
  - `app/app/src/main/java/com/example/freshwall/ui/onboarding/OnboardingComponents.kt`
  - `app/app/src/main/java/com/example/freshwall/ui/onboarding/OnboardingScreen.kt`

Nothing else is dirty. The onboarding scaffolding, Pexels/Unsplash data
plumbing, settings entry, pull-to-refresh, and search-source refactor are
already on `main`. The two staged files contain the most recent visual
polish + immersive picker layout — see "Uncommitted changes" below.

## What "onboarding" covers (full picture, including already-committed parts)

First-launch flow gated by `CategoryPreferences.onboardingComplete`. The
user is routed into `Screen.Onboarding` instead of `Screen.Home` until
they finish (or skip).

Flow:

```
Welcome → Purpose → ModeSelect → CombinedPicker        → done
                                ↘ PexelsPicker → UnsplashPicker → done
```

Transitions: forward-only slide-from-right via `AnimatedContent`. System
back is intercepted at `MainActivity` while `currentScreen is Screen.Onboarding`,
so the user can't bail mid-flow.

Persistence: `data/CategoryPreferences.kt` writes a JSON blob to
SharedPreferences (`freshwall_prefs` / `category_config_v1`). `CategoryConfig`
holds `mode` (`COMBINED` / `SEPARATE`) plus three selection lists
(combined / pexels / unsplash). `pexelsActive()` / `unsplashActive()`
return the correct list for the current mode.

Re-edit entry: `Settings → Wallpaper preferences` opens `CategoryEditorScreen`
which reuses the picker UI but skips Welcome / Purpose, starting at
ModeSelect with the existing selections pre-loaded.

## Visual / structural changes in the uncommitted diff

### `OnboardingComponents.kt`

- **`BackgroundHeader`** is `internal` (was `private`). Now uses
  `SubcomposeAsyncImage` instead of plain `AsyncImage` — loading + error
  slots both paint a centred `splash_logo` (`SplashLogoPlaceholder`, 120dp)
  over the gradient, so the hero never feels empty when the network is
  slow or the URL fails.
- **`MOUNTAIN_IMAGE_URLS`** — 6 direct CDN URLs (3 Pexels, 3 Unsplash).
  Pre-cached by Coil in `LaunchedEffect(Unit)` inside `OnboardingScreen`
  and `CategoryEditorScreen` so by the time the user reaches ModeSelect
  or a picker step the photo is already in disk + memory cache.
- **`PexelsBrand`** = `Color(0xFF05A081)`, **`UnsplashBrand`** =
  `Color(0xFF111111)` — internal constants. Brand tags use these as
  fill colours; combined header stacks both with a `+` icon between.
- **`TopHeaderAccent`** sealed class — `Combined` / `Pexels` / `Unsplash`,
  each takes an `imageUrl: String?` constructor arg so callers pick which
  mountain photo to use.
- **`BrandTagsStack`** — Pexels tag + plus-icon-in-dark-circle + Unsplash
  tag, vertically stacked. Used by `Combined` and by `ModeSelectStep`.
- **`CompactPillButton`** — content-width pill (not full-width), pill
  shape, surfaceContainerHigh / primary colour, `tonalElevation = 3.dp` /
  `shadowElevation = 4-6.dp`. Matches home bottom-nav rack rhythm. Used
  by ModeSelect.
- **`NextArrowButton`** — 64dp circular FAB with `ArrowForward` icon.
  Now takes a `modifier: Modifier = Modifier` param so callers can
  position it (e.g. `Modifier.align(Alignment.BottomEnd)`). Used in the
  picker step's bottom-right corner.
- **`CategoryPickerStep`** — major redesign:
  - Reduced hero height (200dp Combined / 160dp single-source).
  - `splash_logo` (64dp) shown above the title as visual anchor.
  - Title bumped to `displaySmall` + `SemiBold`.
  - Bordered chip box removed. Instead the chip area is now a
    **`weight(1f)` `Box`** filling the lower screen with a vertical
    `Brush.verticalGradient` background (primaryContainer 35% →
    secondaryContainer 20% → background).
  - Chips scroll inside the gradient via internal `verticalScroll`.
  - Counter floats **bottom-left** as a small `surfaceContainerHigh` pill.
  - Arrow button floats **bottom-right** as a FAB-style action.

### `OnboardingScreen.kt`

- **`ModeSelectStep`** rewritten — no longer uses `OnboardingScaffold`.
  Custom `Column` with:
  - `ModeSelectHero` (260dp mountain backdrop + `BrandTagsStack`).
  - Weighted spacer (0.6f).
  - Title in **`displaySmall` / `SemiBold`**, subtitle in `bodyLarge`.
  - Weighted spacer (1f).
  - Two **`CompactPillButton`s** stacked (`Same for both` + `Customize each`).
- **`ModeSelectHero`** — local helper that just calls `BackgroundHeader`
  with the right gradient + `BrandTagsStack`. Internal-only.
- Welcome / Purpose still use `OnboardingScaffold` with `HeroIcon`
  (Material icons — `AutoAwesome` + `Palette`). No changes there in this
  diff.
- All picker accents (`TopHeaderAccent.Combined` / `Pexels` / `Unsplash`)
  receive a random `MOUNTAIN_IMAGE_URLS` URL via `remember {}` so the
  background photo stays consistent through an onboarding visit but
  changes between sessions.
- Image pre-cache `LaunchedEffect(Unit)` added in **both**
  `OnboardingScreen` and `CategoryEditorScreen`.

## Decisions that landed this session

- Mountain photos for picker / ModeSelect backgrounds (not flat gradients).
- Splash drawable used as loading placeholder AND as a "fancy" anchor
  above the picker title.
- Counter is now a small surface pill (not just a `Text` line) so it
  reads cleanly over the gradient.
- Arrow button moved from centred-and-stacked to bottom-right FAB.
- Brand tags use solid brand colours (`#05A081` Pexels green,
  `#111111` Unsplash black) with white content.

## Things explicitly considered and rejected (don't redo these)

- **Don't expand `WALLPAPER_CATEGORIES`.** A bigger list was attempted
  and reverted by the user / linter — current 48-item list stays. If
  you need to add categories later, ask first.
- **Don't auto-commit / auto-push** these onboarding changes. User
  drives commit cadence. The previous prior-session commit (`31c1a6e`)
  was authored by Drake0306 directly.

## Open follow-ups (not yet built)

- **Hourly refresh of the home feeds** — `PexelsHomeViewModel.refresh()` /
  `UnsplashHomeViewModel.refresh()` pick a fresh random category but
  there's no time-based auto-invalidation yet. The user said they want
  the feed to feel new every ~hour. Plan was: 1-hour disk-cached
  manifest with a page seeded by `(epochHour % N) + 1`.
- **Better default endpoints** for Pexels/Unsplash if the user hasn't
  picked any categories — currently falls back to Pexels `curated` /
  Unsplash `popular`. The plan called for swapping to:
  - Pexels: `/search?query=wallpaper&orientation=portrait`
  - Unsplash: **`/topics/wallpapers/photos`**
  Worth implementing as a "no-selection fallback" path.
- **Onboarding-step progress indicator** (dots / pips). The flow is
  4-5 steps; a small indicator could orient users mid-flow. Not asked
  for explicitly — defer.
- **Real illustrations** instead of `splash_logo` + Material icons on
  Welcome / Purpose hero panels. If we later get artwork assets, the
  swap is one line per call site.
- **Confirm visual review of the picker redesign** on device. The
  user hasn't built+walked-through these uncommitted changes yet.

## Pick-up checklist for the next session

1. `cd /Users/roy/Developer/Github/fresh-wall && git status` — confirm
   only the two onboarding files are dirty (or note any new changes).
2. Build + install: `cd app && ./gradlew :app:installDebug`.
3. Wipe app data on the device so the onboarding routing kicks in
   (`adb shell pm clear com.example.freshwall`).
4. Walk the flow:
   - Welcome → tap Next
   - Purpose → tap "Yes, let's customize" (or "Skip for now")
   - ModeSelect → "Same for both" or "Customize each"
   - Picker(s) → search filters, tap chips, watch counter, tap arrow FAB
5. Get user reaction. The picker redesign in particular is a substantial
   visual departure — confirm before pushing.
6. When approved, commit + push from the user's account
   (`abhinavroy.hello@gmail.com` / `Drake0306`). **No `Co-Authored-By`
   trailer** per the standing rule in `CLAUDE.md`.

## Key file paths (quick reference)

| Concern | Path |
|---|---|
| Onboarding entry | `ui/onboarding/OnboardingScreen.kt` |
| Reusable picker pieces | `ui/onboarding/OnboardingComponents.kt` |
| Persisted prefs | `data/CategoryPreferences.kt` |
| Source toggles | `data/SourcePreferences.kt` |
| Routing | `MainActivity.kt` (`Screen.Onboarding` / `Screen.CategoryEditor`) |
| Settings re-edit row | `ui/settings/SettingsScreen.kt` (`onCategoriesClick`) |
| Brand assets | `res/drawable/ic_pexels.xml`, `res/drawable/ic_unsplash.xml`, `res/drawable/splash_logo.xml` |
| Home feed integration | `ui/pexels/PexelsHomeViewModel.kt` + `ui/unsplash/UnsplashHomeViewModel.kt` (read `categoryPreferences.config.value.{pexels|unsplash}Active()`, pick random) |
