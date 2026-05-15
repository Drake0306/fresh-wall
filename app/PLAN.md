# FreshWall — deferred polish & follow-ups

Everything we've discussed but intentionally skipped during initial
development. Each item names the file(s) to touch and roughly how big it is.

---

## 1. Placeholders to swap before publishing

Four things ship with test/placeholder values today. They MUST be replaced
before submitting to the Play Store.

- **AdMob real app ID** — `app/src/main/AndroidManifest.xml` →
  `<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID">`.
  Currently `ca-app-pub-3940256099942544~3347511713` (Google's official test).
  Register the app at https://apps.admob.com.
- **AdMob real rewarded ad unit ID** —
  `app/src/main/java/com/example/freshwall/ads/RewardedAdManager.kt`,
  constant `REWARDED_AD_UNIT_ID`. Currently
  `ca-app-pub-3940256099942544/5224354917` (test). Same AdMob console.
- **Donation URL** —
  `app/src/main/java/com/example/freshwall/ui/donate/DonateScreen.kt`,
  constant `DONATE_URL`. Currently `https://buymeacoffee.com/freshwall`.
  Replace with the real Buy Me a Coffee / Ko-fi / PayPal URL.
- **Support email** —
  `app/src/main/java/com/example/freshwall/ui/feedback/FeedbackScreen.kt`,
  constant `SUPPORT_EMAIL`. Currently `support@freshwall.app`. Replace with
  whatever address actually receives mail.

> **Important — never tap your own AdMob ads** once real IDs are in place.
> Google bans accounts permanently for this. Configure your test device in
> AdMob console first so you can test without risk.

---

## 2. Branding & visual polish

- ~~**Real launcher icon**~~ — DONE. Mipmap PNGs at every density were
  regenerated from the FreshWall mountain logo (transparent corners). The
  adaptive icon foreground (`drawable/ic_launcher_foreground.xml`) insets
  `@drawable/app_logo` into the 72-dp safe area; background is solid cream.
- ~~**Real splash logo**~~ — DONE. `drawable/splash_logo.xml` is now an
  inset wrapper over `@drawable/app_logo`. Splash background still
  `splash_background = #FFFFFFFF` (white); revisit if it clashes.
- ~~**Verify app name**~~ — DONE. `app_name` already reads `FreshWall`.
- **Real About page content** —
  `app/src/main/java/com/example/freshwall/ui/settings/AboutScreen.kt`.
  Currently shows "Version 1.0" + "Wallpapers, curated." Add real
  version (`BuildConfig.VERSION_NAME`), developer credit, license links.

---

## 3. Pre-launch compliance

- **Privacy policy URL** — required for AdMob and the Play Store. Host it
  somewhere (GitHub Pages works fine for a static page). Link to it from
  About screen.
- **Data safety form** — Google Play submission asks what data you collect.
  Honest answers for FreshWall today: advertising ID (via AdMob), local-only
  theme / favorites / search history (no upload). Easy to fill if you're
  honest.
- **Target SDK review** — currently API 36 (Android 15). Read the Android 15
  behavior changes before launch; they keep adding restrictions (background
  work, sensitive permissions).
- **Verify R8 minification on release build** — `app/build.gradle.kts`,
  `release { isMinifyEnabled = false }`. Flip to `true`, build a release
  variant, test that Coil/AdMob/Kotlinx-Serialization still work. All three
  ship their own ProGuard rules so usually it's fine — but verify.

---

## 4. Quality / robustness

- **Image network retry on transient failures** — Coil's default doesn't
  retry HTTP-level errors (502, 503). Add a small OkHttp `Interceptor` to
  retry once. Mobile networks drop bytes occasionally; this fixes
  permanently-broken tiles until the user scrolls them off and back.
- **Detail-screen error UI** —
  `app/src/main/java/com/example/freshwall/ui/detail/DetailScreen.kt`,
  `SubcomposeAsyncImage.error` slot. Currently falls back to the blurred
  thumbnail. If BOTH URLs fail (offline + cache miss) the screen is just
  black. Add a centered "Couldn't load image — tap to retry."
- **OEM-specific testing for auto-rotate** — older MIUI / ColorOS skins
  sometimes silently block `WallpaperManager.setBitmap()` from background
  workers. Test on representative devices once you have them.
- **Cancel pending Coil requests on ViewModel.onCleared** — minor cleanliness.
  `FeaturedViewModel.viewModelScope` already cancels but enqueued Coil
  requests keep going. They hit the cache and finish quickly so usually OK.
- **Accessibility pass** — TalkBack walkthrough, verify 48-dp minimum touch
  targets, color contrast on text. Already mostly fine; do a deliberate sweep
  before launch.

---

## 5. Performance (already pretty good — room for more)

- **Lambda stability inside `LazyVerticalGrid` items** —
  `ui/home/HomeScreen.kt`. Lambdas inside `items {}` are recreated per
  recomposition. For 10 items it doesn't matter. For 200+ it will. Refactor
  `WallpaperTile`'s callbacks from `() -> Unit` to `(Wallpaper) -> Unit` so
  the parent can pass stable references.
- **Explicit Coil `.size()` hint** — Coil auto-detects display size, but
  passing an explicit `Size` to `ImageRequest.Builder` reduces the requested
  payload when the URL supports it (Unsplash does; picsum has it in the URL).
- **Conditional blur radius by device tier** — `Modifier.blur(20.dp)` on the
  detail loading placeholder is cheap on modern GPUs, can stutter on older
  ones. Detect performance class via `ActivityManager` and drop blur on
  lower tiers.
- **Smaller thumbnail URL size** — bump picsum URLs in
  `assets/wallpapers.json` from 600×1080 to 400×720. Faster decode, less
  RAM, slightly softer in the hero strip (acceptable tradeoff).

---

## 6. Features (in priority order)

### A. Pexels "Featured" tab content  *(now that Pexels search is in)*
- Pexels search is already wired (toggle in SearchScreen).
- Next: populate the second tab (currently shows "Pexels — coming soon")
  with `PexelsRepository.curated()` so users get a passive Pexels stream
  without needing to search.
- The repository already has `.curated()` available; just need a small
  ViewModel + the tab to render its results in the same grid.
- Required attribution: already done in `AboutScreen.kt` ("Photos provided
  by Pexels"). Make sure photographer name is tappable on the detail screen
  (info dialog now has a "View on Pexels" link via `sourceUrl` — verify it
  renders for Pexels items).
- Note: Unsplash was explicitly rejected because their API guidelines
  restrict wallpaper apps; Pexels is the chosen provider.

### B. Daily wallpaper push notification  *(retention play)*
- Firebase Cloud Messaging + a tiny backend cron (Cloudflare Worker fits).
- Send a topic message daily with a wallpaper id.
- An FCM service in the app builds a `Notification` deep-linked into
  `Screen.Detail` so tapping it opens the wallpaper directly.
- Needs `POST_NOTIFICATIONS` runtime permission on Android 13+.

### C. Categories / collections  *(deferred — revisit when catalog grows)*
- Group wallpapers by theme (Nature, Abstract, etc.).
- Worth doing once the catalog passes ~50 wallpapers. Premature today.

### D. Real IAP for ad removal  *(replace honor system)*
- Currently `DonationPreferences.setSupporter(true)` is honor-system; user
  toggles a switch after donating.
- For verified ad-removal: `com.android.billingclient:billing-ktx`, one-time
  product (e.g. `support_freshwall_1`), gate the supporter flag on a
  successful purchase verification.
- Only worth doing if/when honor-system abuse becomes measurable.

---

## 7. Tech debt / engineering hygiene

- **`kotlin-parcelize` on `Wallpaper`** — enables `rememberSaveable` with the
  full Wallpaper object. Currently `Screen.Detail(wallpaper)` survives
  configuration changes via `SaveableStateHolder` but not process death.
  Annotating Wallpaper as `@Parcelize` lets the framework restore the user
  to the same wallpaper after the process is killed.
- **Migrate `SharedPreferences` → Jetpack DataStore** — same use cases, but
  coroutine-native and crash-safe on partial writes. Touches
  `ThemePreferences`, `DonationPreferences`, `AutoRotatePreferences`,
  `FavoritesManager`, `SearchHistoryManager`. Mechanical migration.
- **Unit tests** — pure-Kotlin tests around:
  - `WallpaperActions.renderTransformedBitmap` (the crop math is tricky)
  - `FavoritesManager.toggle` ordering and dedupe
  - `ThemePreferences` round-trip
  - `SearchHistoryManager` dedup case-insensitive + max-items
  - Roughly half a day.
- **Dependency injection** — all managers are `by lazy` singletons on
  `FreshWallApplication`. Works, but isn't cleanly testable. Hilt is the
  canonical move when the surface grows.
- **Migrate to Coil 3** — currently on Coil 2.7.0. Coil 3 is a fresh API
  (multiplatform-ready, separate network module). Not urgent; do it when
  2.x stops getting updates.

---

## 7b. Pexels API limitations (things we can't show)

The Pexels Photo response only includes: photographer name, their Pexels
profile URL, photo dimensions, and size variants. **It does NOT include:**

- **Photographer avatar / photo URL** — we render a letter avatar (first
  initial in a colored circle) in its place. If Pexels ever publishes a
  public `/v1/users/{id}` endpoint, swap the letter avatar's `Box` for an
  `AsyncImage(url)` in `DetailActionSheet.PhotographerAvatar`.
- **Profile description / bio** — not returned by the API.
- **Social handles (Instagram, X, etc.)** — not returned by the API.
- **File size (bytes)** — not returned. We display megapixels as a proxy.
  Could add a `HEAD` request on the image URL to read `Content-Length` if
  this becomes a wanted feature, at the cost of one extra request per photo.

For "I'd really want this stuff" features: switching providers (Unsplash
exposes all of the above, but excludes wallpaper apps from their production
tier — see section 6 history) or using a richer paid stock-photo API.

## 8. Open questions to settle later

- **Donation → ad removal:** honor system right now
  (`DonationPreferences.setSupporter` toggle). Move to real IAP eventually?
  Recommendation: only if honor-system abuse becomes measurable.
- **Auto-rotate minimum interval:** currently 15 min. On bad networks that
  burns data. Add a "low data mode" warning if interval < 1 hour AND
  Wi-Fi-only is off?
- **Search behavior on history tap:** currently refills the TextField (user
  has to hit search again). Should it auto-run the search instead? Decide
  once Unsplash search is actually wired.
- **Favorites across sources:** today only Featured wallpapers can be
  favorited (Unsplash not loaded yet). When Unsplash arrives, `Wallpaper.id`
  collisions become possible. Solution: prefix ids by source
  (`featured:fw-001`, `unsplash:abc123`).
- **Splash kept on screen during cold-start load:** `installSplashScreen()`
  returns a `SplashScreen` instance with `.setKeepOnScreenCondition { … }`.
  Useful once Unsplash adds network-dependent cold start. Skip until then;
  current load is local-asset-fast.

---

## 9. Crashlytics setup (when you're ready to publish)

Crashlytics is **the single highest-value thing to add before launch**. Without it you ship blind: a user gets a crash, force-closes, never tells you. With it, you see the exact stack trace, device model, Android version, and how many users hit it.

It's free, but it needs a Firebase project on your account — that part has to happen in a browser, not from code. Once that's done, the in-app wiring is ~15 minutes.

### Step 1 — create the Firebase project (one-time, ~5 min)

1. Go to https://console.firebase.google.com.
2. Click **Add project** → name it "FreshWall" → enable/disable Analytics as you prefer (Crashlytics doesn't require it; Analytics needs a Google Analytics account).
3. Once the project is created, click **Add app → Android**.
4. Use **`com.example.freshwall`** as the package name (or update both Firebase + `applicationId` in `build.gradle.kts` to your real one).
5. Skip the SHA-1 for now (only needed for Google Sign-In etc.).
6. Click **Download google-services.json**. **Move that file to `app/app/`** (next to the module's `build.gradle.kts` — same level as `src/`).

### Step 2 — add the Gradle bits

In `app/gradle/libs.versions.toml`, add:

```toml
[versions]
googleServicesPlugin = "4.4.2"
firebaseBom = "33.5.1"
firebaseCrashlyticsPlugin = "3.0.2"

[libraries]
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-crashlytics = { group = "com.google.firebase", name = "firebase-crashlytics" }

[plugins]
google-services = { id = "com.google.gms.google-services", version.ref = "googleServicesPlugin" }
firebase-crashlytics = { id = "com.google.firebase.crashlytics", version.ref = "firebaseCrashlyticsPlugin" }
```

In `app/build.gradle.kts` (project-level, the OUTER one), add plugins to be applied later:

```kotlin
plugins {
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}
```

In `app/app/build.gradle.kts` (module-level), apply them and add the dependency:

```kotlin
plugins {
    // ...existing plugins...
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

dependencies {
    // ...existing deps...
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
}
```

### Step 3 — verify

Sync Gradle. If `google-services.json` is in the right place and the package name matches, sync succeeds and Crashlytics auto-initializes on first launch — no code changes needed. You'll see your app in the Firebase console under Crashlytics within ~hours of the first launch (Firebase's pipeline isn't instant; first crash typically appears ~6 hours later).

### Tip — test it works

While debugging, throw a deliberate crash from a button:

```kotlin
throw RuntimeException("Test crash from FreshWall")
```

Force-close the app, relaunch (so it can upload), then check the Firebase Crashlytics dashboard.

---

## 10. Recommended order if/when you come back

1. **Unsplash integration** — biggest visible win, makes the app feel real
   instead of a demo with 10 wallpapers.
2. **Real branded launcher icon + splash logo** — first impressions on the
   Play Store listing and on the user's home screen.
3. **Privacy policy + data safety form + real AdMob IDs** — needed to
   actually publish.
4. **R8 release build + OEM device sweep** — last-mile testing.
5. **Push notifications**, **real IAP for ad removal**, **categories**,
   **DataStore migration**, etc. — order based on what's hurting most at
   that point.
