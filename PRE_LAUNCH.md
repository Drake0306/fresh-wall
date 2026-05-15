# FreshWall — Pre-launch punch list

Tracking the path from "code complete" to "live on Play Store + Unsplash
production-tier approved." Each item is tagged with **owner**: who has to do
it (you / me / either) and what I need from you to start.

Status legend: `[ ]` open · `[~]` in progress · `[x]` done · `[—]` skipped

---

## 🔴 Hard blockers — must clear before Play Store submission

### `[x]` 0. Replace placeholder package name `com.example.freshwall`
**Done** — renamed to `io.github.drake0306.freshwall` across `build.gradle.kts`
(namespace + applicationId), every Kotlin file's `package` / `import`
statements, the source directory tree under `app/src/main/java/`, plus
`androidTest/` and `test/` trees. Documentation (`README.md`, `PLAN.md`,
`PRE_LAUNCH.md`, `CLAUDE.md`, `ONBOARDING_NOTES.md`) updated to reference
the new path. **Cannot be changed after Play Store publish.**

### `[x]` 1. Replace AdMob test IDs with real ones
**Owner**: you (register at AdMob) → me (paste IDs into code)
**What I need from you**:
- The real **App ID** (looks like `ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX`)
- The real **Rewarded ad unit ID** (looks like `ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX`)
**Where they go**:
- App ID → `app/app/src/main/AndroidManifest.xml`, the
  `com.google.android.gms.ads.APPLICATION_ID` meta-data tag
- Rewarded unit ID → `app/app/src/main/java/io/github/drake0306/freshwall/ads/RewardedAdManager.kt`,
  `REWARDED_AD_UNIT_ID` constant
**Warning**: after the swap, **do not tap your own ads** — Google
permanently bans accounts for it. Add your test device in the AdMob console
before testing on hardware.

### `[x]` 2. Replace placeholder donation URL
**Owner**: you (decide donation platform) → me (paste URL)
**What I need from you**:
- The real donation URL (Buy Me a Coffee / Ko-fi / PayPal / GitHub Sponsors …)
**Where it goes**:
- `app/app/src/main/java/io/github/drake0306/freshwall/ui/donate/DonateScreen.kt`,
  `DONATE_URL` constant (currently `https://buymeacoffee.com/freshwall` —
  that page doesn't resolve, so it must be replaced or the Donate row
  removed for v1).

### `[~]` 3. Privacy policy — drafted, awaits GH Pages enable
**Owner**: you (host the page) → me (add link)
**What I need from you**:
- A public URL to your privacy policy. Cheapest path: a one-page GitHub
  Pages site under your repo. Template content: what data the app collects
  (advertising ID via AdMob; local-only theme/favorites/search history; no
  account, no upload), what's shared (nothing), how to contact you.
**Where it goes**:
- New row in About screen
  (`app/app/src/main/java/io/github/drake0306/freshwall/ui/settings/AboutScreen.kt`)
- Also referenced from the Play Store listing form
**Why mandatory**: Play Store rejects apps that use AdMob's advertising ID
without a linked policy.

### `[ ]` 4. Turn on R8 minification + test release build
**Owner**: me (edit gradle, add keep rules) → you (install release APK, walk
through every screen on a real device)
**What I need from you**:
- Permission to flip `isMinifyEnabled = true` in
  `app/app/build.gradle.kts` and to add explicit `-keep` rules for our
  `@Serializable` classes
- A real device (or two) to install the release APK on
**What might break under minification**: Coil image loading, AdMob ad load,
favorite persistence (kotlinx-serialization reflective lookup). All three
libs ship consumer ProGuard rules; missing `@Serializable` keep rules for
our own data classes is the most likely failure.

### `[x]` 5. Display real version, not literal "Version 1.0"
**Owner**: me — purely mechanical
**What I need from you**: just say go.
**What I'll change**:
- `app/app/src/main/java/io/github/drake0306/freshwall/ui/settings/AboutScreen.kt`:
  swap the literal `"Version 1.0"` for `BuildConfig.VERSION_NAME` so it
  tracks `versionName` in `build.gradle.kts`.

---

## 🟡 Unsplash production-tier — important context

### `[ ]` 6. Submit FreshWall for Unsplash Production API approval
**Owner**: you (write + submit the form)
**What I'll prepare for you**:
- A "compliance checklist" you can paste into your submission, citing the
  features we already shipped: photographer attribution UI, download
  tracking on every apply/save, "Powered by Unsplash" badge, CDN hotlinking
  (no re-hosting), profile/social link surfacing.
**What you need to do**:
- File your submission via Unsplash's developer dashboard. Lead with the
  checklist; link your Play Store listing once you have one.
**Fallback**: if Unsplash rejects, the app degrades cleanly to Pexels-only —
the source-toggle UI in `ui/search/SearchScreen.kt` and the
`SourcePreferences` flag already gate Unsplash behind a switch.

---

## 🟡 Polish — fix if you have time before launch

### `[x]` 7. Detail screen "image couldn't load" state
**Owner**: me
**What I need from you**: just confirm you want it.
**What I'll change**:
- `app/app/src/main/java/io/github/drake0306/freshwall/ui/detail/DetailScreen.kt`'s
  `SubcomposeAsyncImage.error` slot — replace the blurred-thumbnail
  fallback (which can show a black screen if BOTH URLs fail) with a centred
  "Couldn't load image — tap to retry" message that re-issues the request.

### `[x]` 8. Loading skeleton on grids
**Owner**: me
**What I need from you**: confirm the visual style (greyed-out tile vs
shimmer animation vs spinner overlay). My recommendation: greyed-out tiles
matching grid spacing — cheapest, matches Material 3 defaults.
**Where it goes**: `ui/pexels/PexelsHomeScreen.kt` and
`ui/unsplash/UnsplashHomeScreen.kt` (the home tab grids) and
`ui/search/SearchScreen.kt`.

### `[x]` 9. Finish `data_extraction_rules.xml`
**Owner**: me, but needs a decision from you
**Decision needed**: which preferences should follow a user to a new device
via Google's auto-backup?
- **Suggested back up**: theme choice, favorites, category selection
- **Suggested don't back up**: search history, image cache
**Where it goes**: `app/app/src/main/res/xml/data_extraction_rules.xml`
(currently has TODO placeholders).

### `[x]` 10. Add ProGuard keep rules for `@Serializable` classes
**Owner**: me — happens together with item 4.
**What I'll add to `proguard-rules.pro`**:
- `-keep` rules for `Wallpaper`, `CategoryConfig`, `UnsplashPhoto`, etc., so
  kotlinx-serialization's reflective lookup keeps working under minify.

### `[ ]` 11. Featured tab — finish or hide
**Owner**: you (decide which) → me (implement)
**Decision needed**:
- **Finish**: wire `PexelsRepository.curated()` into a `FeaturedHomeViewModel`
  + grid. ~2 hours of work, gives a third tab.
- **Hide for v1**: remove the "Featured tab (experimental)" toggle from
  Settings; ship with just Pexels + Unsplash tabs. Cleaner for launch.
**My recommendation**: hide for v1. The Featured catalog needs more content
than the current placeholder before it earns its tab. Easy to bring back
later.

### `[x]` 12. Accessibility sweep
**Owner**: me
**What I'll do**: walk every interactive element, add `contentDescription`
where missing, verify TalkBack reads the apply/save buttons correctly,
verify 48dp minimum touch targets on the pill bottom nav.
**What I need from you**: nothing — just an OK.

---

## ⚪ Post-launch backlog (do after v1 ships)

These are in `app/PLAN.md` already. Re-listed for visibility, not blockers.

### `[~]` 13. Crashlytics (Firebase) — code wired, awaiting Firebase project
The dependency + plugin are now in `libs.versions.toml` and `app/build.gradle.kts`.
Crashlytics auto-init runs the moment a real `google-services.json` lands
in `app/app/`. No further code work — when you create the Firebase project
this just starts working. Walk-through to create the project is in
`app/PLAN.md` section 9.

### `[ ]` 14. Daily wallpaper push notification
FCM + a small backend cron (Cloudflare Worker fits). Retention play. Needs
`POST_NOTIFICATIONS` runtime permission on Android 13+.

### `[ ]` 15. Real IAP for ad-removal
Currently honor-system via `DonationPreferences.setSupporter`. Switch to
`com.android.billingclient:billing-ktx` once abuse becomes measurable.

### `[ ]` 16. `@Parcelize` on `Wallpaper`
Restores `Screen.Detail(wallpaper)` after process death. Minor UX win.

### `[ ]` 17. SharedPreferences → DataStore migration
Mechanical migration touching `ThemePreferences`,
`AutoRotatePreferences`, `CategoryPreferences`, `FavoritesManager`,
`SearchHistoryManager`. Coroutine-native, crash-safe writes.

### `[ ]` 18. Unit tests
`WallpaperActions.renderTransformedBitmap`, `FavoritesManager.toggle`,
`ThemePreferences` round-trip, `SearchHistoryManager` dedup. ~half a day.

---

## Suggested order

Block 1 — code-only, I can do today: **5, 7, 8, 9, 10, 12** (in parallel).
Block 2 — needs your info: **1, 2, 3**.
Block 3 — needs your device + decision: **4, 11**.
Block 4 — Unsplash paperwork: **6**.
Block 5 — post-launch: everything in the ⚪ section.

Tell me which item to start on and I'll go.
