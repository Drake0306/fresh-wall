# FreshWall — Alpha release playbook

Step-by-step guide for cutting the first internal-test build and shipping
it to Play Console. Everything here is one-time setup except the build
+ upload steps at the end (which repeat every release).

Status legend: `[ ]` you do this · `[i]` informational only

---

## Part A — One-time setup

### `[ ]` A1. Generate the release keystore

The keystore is the cryptographic identity of FreshWall on Play Store.
Lose it, and you can never publish another update — Play Store would
require a brand-new app listing under a different package name. **Back it
up to two more places** the moment you create it.

Run this from your `~` (home directory) so the keystore ends up at
`~/freshwall-release.jks`:

```bash
keytool -genkeypair -v \
  -keystore ~/freshwall-release.jks \
  -alias freshwall \
  -keyalg RSA -keysize 2048 \
  -validity 25000
```

`keytool` will prompt for:

| Prompt | What to put |
|---|---|
| Keystore password | A long random string. **Save this** to your password manager. |
| Re-enter keystore password | Same as above. |
| First name / last name | Your full name (Abhinav Roy). |
| Organizational unit | Personal / Indie / whatever |
| Organization | Personal / your company name |
| City, State, Country code | Your city / state / `IN` |
| Confirm | yes |
| Key password (for `freshwall`) | Press **return** to reuse the keystore password. Simplest. |

Verify the file exists:

```bash
ls -l ~/freshwall-release.jks
```

**Back it up now.** Copy to:
1. Cloud storage (Google Drive / Dropbox / iCloud — encrypted)
2. An external drive or second machine

Without this file, the project is bricked for Play Store updates. Take
this seriously.

### `[ ]` A2. Wire the creds into `local.properties`

Open `app/local.properties` and add four lines:

```properties
# Existing keys (leave as they are):
# pexels.api.key=...
# unsplash.api.key=...
# wallpaper.manifest.url=...

# Release signing — used by build.gradle.kts when building :app:bundleRelease
release.keystore.path=/Users/roy/freshwall-release.jks
release.keystore.password=PASTE_YOUR_KEYSTORE_PASSWORD_HERE
release.key.alias=freshwall
release.key.password=PASTE_YOUR_KEYSTORE_PASSWORD_HERE
```

(The key password and the keystore password are the same if you pressed
return at the second prompt in A1. If you set a separate key password,
put the actual key password under `release.key.password`.)

**`local.properties` is gitignored** — these secrets stay on your machine.

### `[ ]` A3. Re-sync Gradle

In Android Studio: **File → Sync Project with Gradle Files**, or from
terminal:

```bash
cd app
./gradlew :app:tasks > /dev/null && echo "sync OK"
```

The new signing config registers silently.

---

## Part B — Build the signed AAB

The Play Store accepts Android App Bundles (AAB), not raw APKs. AAB is
what Google needs for size-optimised per-device APK delivery.

```bash
cd app
./gradlew :app:bundleRelease
```

Output: `app/app/build/outputs/bundle/release/app-release.aab`

That's the file you upload to Play Console. Roughly 15–25 MB.

### Sanity-check before uploading

```bash
# Confirm the AAB is signed by your keystore (not unsigned).
keytool -printcert -jarfile app/app/build/outputs/bundle/release/app-release.aab
```

You should see your CN, OU, and so on from the keystore. If you see
"WARNING: signer information unavailable", the build was unsigned — go
back to A2 and check `local.properties`.

---

## Part C — Play Console: first-time app setup

If you've never published an Android app, you'll need:

1. A **Google Play Console** account → <https://play.google.com/console>
   — costs a one-time **$25 USD** registration fee.
2. Identity verification — government ID + a real address.
3. A payment method for fee collection (only relevant if your app is
   paid, which FreshWall isn't).

### `[ ]` C1. Create the app entry

Play Console → **Create app**.

| Field | Value |
|---|---|
| App name | FreshWall |
| Default language | English (United States) |
| App or game | App |
| Free or paid | Free |
| Declarations | tick the 3 policy checkboxes — they're standard |

### `[ ]` C2. Fill the "Set up your app" checklist

Play Console walks you through ~10 forms before letting you publish.
Order doesn't matter — you can save partial answers. Items you'll need:

- **App access** — "All functionality is available without restrictions" (we have no signup)
- **Ads** — **Yes, my app contains ads** (we use AdMob rewarded ads)
- **Content rating** — answer the questionnaire honestly; FreshWall ends up at **Everyone**
- **Target audience and content** — pick the **13+** age range (we don't target kids; AdMob doesn't allow apps targeting <13 without COPPA compliance)
- **News app** — No
- **COVID-19 contact tracing** — No
- **Data safety** — see Part D below for what to declare
- **Government app** — No
- **Financial features** — No
- **Health** — No

### `[ ]` C3. Store listing

| Field | Use |
|---|---|
| App name (max 30) | `FreshWall` |
| Short description (max 80) | see "Listing copy" section below |
| Full description (max 4000) | see "Listing copy" section below |
| App icon (512×512 PNG) | Export `app_logo.png` at 512×512 — already shipping at that res in `drawable-nodpi/app_logo.png` |
| Feature graphic (1024×500 PNG) | You'll need to make one — see Part E |
| Phone screenshots (at least 2) | Take 4-6 from the running app: Home (Pexels), Detail screen, Settings, About |
| App category | **Personalization** |
| Tags | wallpapers, personalization |
| Contact details — email | `abhinavroy.hello@gmail.com` |
| Contact details — website (optional) | `https://drake0306.github.io/fresh-wall/` |
| Privacy policy URL | `https://drake0306.github.io/fresh-wall/privacy.html` |

---

## Part D — Data safety form

Play Console asks what data your app handles. Answer honestly — Google
cross-references this with your manifest, and getting caught lying ends
your developer account.

### Data collected — items to tick

| Data type | Collected? | Shared? | Optional? | Purpose | Why |
|---|---|---|---|---|---|
| **Advertising or marketing ID** | Yes | Yes (with AdMob) | Required | Ads | AdMob serves rewarded ads |
| **Crash logs** | Yes | Yes (with Google) | Required | App functionality | Firebase Crashlytics |
| **Diagnostics** | Yes | Yes (with Google) | Required | App functionality | Crashlytics also reports performance / device info |
| **Other user-generated content** | Yes | No | Optional | App functionality | Only if user sends feedback via the Feedback screen |
| **Photos** | Yes | No | Optional | App functionality | Only if user attaches a screenshot to feedback |

### Data NOT collected — make sure these stay unticked

- Name, email, phone number, address, user IDs
- Location of any kind
- Financial info
- Health info
- Contacts, calendar, SMS, call history
- Files & docs (other than the optional feedback screenshot)
- Web browsing history

### Data security questions

- **Is all of your user data encrypted in transit?** Yes (HTTPS everywhere)
- **Do you provide a way for users to request their data be deleted?**
  Yes — they email `abhinavroy.hello@gmail.com`. (For feedback data, you
  manually delete from Firestore. For favorites etc., they uninstall the
  app and it's gone.)

---

## Part E — Listing copy (ready to paste)

### Short description (80 chars max — currently 77)

```
Hand-picked wallpapers from Pexels and Unsplash. No accounts, no clutter.
```

### Full description (4000 chars max — used ~1900)

```
FreshWall is a simple wallpaper app built by one developer in spare time. Free, open-source, no signup required.

Browse wallpapers from two big free libraries:

- Pexels — a stock photography library used by millions
- Unsplash — high-quality landscape, lifestyle, and abstract photography

Pick your top categories during a quick first-launch flow — mountains, sunsets, abstract patterns, whatever you want. The app shows those first in your feed, mixing in the rest occasionally so things stay interesting.

WHAT YOU CAN DO

• Set any wallpaper to home screen, lock screen, or both
• Save favorites to a local-only list (never uploaded anywhere)
• Search Pexels and Unsplash directly
• Auto-rotate your wallpaper on a schedule
• Switch between light, dark, or system theme
• Browse offline — recently viewed wallpapers stay cached

WHAT IT DOESN'T DO

• Ask for your email, phone, or any account
• Track you across apps
• Hide good wallpapers behind a paywall

A short rewarded ad shows up before applying a wallpaper. That's how the API quotas and Play Store fees get covered. If you'd rather contribute directly, the in-app Support screen has options for India (UPI) and international (Ko-fi) tips.

The full app is open source under the Apache 2.0 license. Source code, issue tracker, and pull requests: github.com/Drake0306/fresh-wall

If you spot a bug, send feedback from inside the app or open an issue on GitHub.

Thanks for trying it.
```

### "What's new" — first alpha release notes (max 500 chars)

```
First alpha. Browse wallpapers from Pexels and Unsplash, pick favourite categories with a star-priority system, save favourites locally, set wallpapers on home / lock / both, auto-rotate on a schedule, light / dark / system theme. Open source on GitHub.
```

### Feature graphic prompt

A 1024×500 banner is required for Play Console listings. Easiest path:
take a screenshot of the **Welcome step** (the faded grid + logo + "Get
started" pill) on a 6.7" emulator, then crop/resize to 1024×500 keeping
the central column intact. The faded mountain grid backdrop reads well
at that aspect ratio.

If you'd rather generate it: any of the rounded-square Featured tiles
arranged in a 4×2 grid behind a large "FreshWall" wordmark + the app
logo on the left works fine.

---

## Part F — Publish the alpha

Play Console → your app → **Testing → Internal testing**.

### `[ ]` F1. Create a release

1. Click **Create new release**.
2. App signing — when prompted, **let Google manage your app signing key**
   (Play App Signing). Upload your `app-release.aab`. Google generates a
   new signing key on their end; your local keystore is only used to
   prove you own future updates (the "upload key").
3. Release name — `alpha-1` or whatever.
4. **Release notes** — paste the "What's new" text from Part E.
5. **Save** → **Review release** → **Start rollout to internal testing**.

### `[ ]` F2. Create a testers list

1. **Testers** tab → **Create email list**.
2. Add the Gmail addresses of anyone you want testing it (yourself + 2-5
   friends).
3. Copy the **opt-in URL** — testers open this in their phone's Chrome,
   tap "Become a tester", then install via the linked Play Store entry.

### `[ ]` F3. Wait for review

Play Store reviews the build (even internal-only). Usually 2-48 hours.
You'll get an email when it's live for testers.

---

## Part G — Iteration loop (subsequent alpha builds)

Once the keystore + Play Console are set up, each new alpha is:

1. Bump `versionCode` (and optionally `versionName`) in
   `app/app/build.gradle.kts`:
   ```kotlin
   versionCode = 2  // monotonically increasing
   versionName = "1.0.1"
   ```
2. `./gradlew :app:bundleRelease` from `app/`.
3. Play Console → Internal testing → **Create new release** → upload the
   new AAB → update "What's new" → roll out.

Total round-trip: ~5 min once you're used to it.

---

## CI — what's already wired

`.github/workflows/build.yml` runs `:app:assembleDebug` on every push to
`main` and every PR. It compiles the project, uploads the debug APK as
an artifact (downloadable from the Actions tab for 30 days), and
cancels in-flight runs when a newer commit lands. **CI doesn't sign
releases** — that stays local with your keystore.

If a CI build goes red, fix the underlying cause; don't skip the check.

---

## Quick checklist before clicking "Start rollout"

- [ ] AdMob real IDs in manifest + RewardedAdManager (already done)
- [ ] Privacy policy URL live and pasted into Play Console (already done)
- [ ] Package name is `io.github.drake0306.freshwall` (already done)
- [ ] `versionCode` and `versionName` set
- [ ] Keystore created and backed up
- [ ] `local.properties` has all four signing keys
- [ ] `bundleRelease` produces a signed AAB (verified via `keytool -printcert`)
- [ ] Listing copy + screenshots + feature graphic uploaded
- [ ] Data safety form answered honestly
- [ ] Internal-test email list created
- [ ] Tester opt-in URL shared with at least one person who'll install it

Once all of the above are ticked, you're ready to ship the alpha.
