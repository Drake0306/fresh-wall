# FreshWall — Play Store release playbook

End-to-end guide for shipping FreshWall to the Google Play Store.

**Big picture:** $25 Play Console account → build an AAB (not APK) →
fill out content declarations → upload to Internal testing track first
(no review delay) → promote to closed/open beta (3–7 day Google review)
→ promote to production (another review) with staged rollout.

Companion docs: [`RELEASE_CHEATSHEET.md`](RELEASE_CHEATSHEET.md) covers
the local build + signing, [`PRE_LAUNCH.md`](PRE_LAUNCH.md) tracks
remaining blockers.

---

## 1. Create the Play Console account ($25, one-time)

1. Go to <https://play.google.com/console> → **Get started**.
2. Choose **Personal** (skip Organization unless you have a D-U-N-S
   number ready — that's a separate ~$200/yr business identifier).
3. Pay the **$25 USD** registration fee.
4. **Identity verification**: upload a government ID (passport / driving
   licence). Google emails when verified — usually under 24h, sometimes
   48h. You can't do anything else until this clears.

---

## 2. Create the app entry

In Play Console → **Create app**:

| Field | Value |
|---|---|
| App name | FreshWall |
| Default language | English (United States) |
| App or game | App |
| Free or paid | Free |
| Declarations | Tick both (Developer Program Policies + US export laws) |

After creating, you'll land on the app dashboard with a long todo list
on the left sidebar. Work through it top-to-bottom.

---

## 3. Build the Android App Bundle (AAB)

Play Store accepts AAB only, not APK. Same wizard as the APK build, just
pick the other option:

1. Android Studio → **Build → Generate Signed Bundle / APK…**
2. Pick **Android App Bundle**.
3. Use existing keystore → `do not delete/freshwall-release.jks`.
4. Variant: **release**.
5. Output lands at:
   ```
   app/build/outputs/bundle/release/app-release.aab
   ```

Around 12–15 MB. Play Store splits this into per-device APKs
automatically — users only download what their phone needs (~5–8 MB).

---

## 4. Configure Play App Signing

When you upload your first AAB, Play Console asks whether to enrol in
**Play App Signing**. Say **yes** — it's mandatory for new apps now.

How it works:

- Your `freshwall-release.jks` becomes the **upload key** — what you
  sign uploads with.
- Google generates and holds the **app signing key** — what users'
  phones verify against.
- If you ever lose `freshwall-release.jks`, you can request a key reset
  from Play Console. (Couldn't with the old direct-signing model.)

**Important gotcha**: the upload SHA-1 (your keystore's) and the
distribution SHA-1 (Google's) differ. The distribution SHA-1 is what
matters for Firebase Auth — when you set up the production Firebase
project, get the distribution SHA-1 from Play Console → **Release →
Setup → App integrity** and add it to your Firebase project's Android
app fingerprints.

---

## 5. Fill out the app content declarations

Play Console → **Policy → App content**. Roughly an hour of work the
first time. Each item must be marked complete before you can publish.

| Section | What to declare for FreshWall |
|---|---|
| **Privacy policy** | `https://drake0306.github.io/fresh-wall/privacy.html` |
| **App access** | All functionality available without restrictions |
| **Ads** | **Yes, my app contains ads** — required because AdMob |
| **Content ratings** | Run the IARC questionnaire (~5 min). Wallpaper app should land on PEGI 3 / Everyone. |
| **Target audience** | 13+ (or 18+ — your call). Don't pick a children's audience. |
| **News app** | No |
| **COVID-19 contact tracing** | No |
| **Data safety** | **Yes, advertising ID** (AdMob). **No** personal info, **no** data sharing. Add "Crash logs" once Crashlytics is live with a real Firebase project. |
| **Government apps** | No |
| **Financial features** | No |
| **Health** | No |
| **VPN service** | No |

The Data Safety form is what reviewers scrutinise. Be honest — for
FreshWall, advertising ID is the only thing collected.

---

## 6. Set up the store listing

Play Console → **Grow → Store presence → Main store listing**.

| Asset | Spec | Notes |
|---|---|---|
| **App icon** | 512×512 PNG, ≤1 MB | High-res render of the existing launcher icon. |
| **Feature graphic** | 1024×500 PNG/JPG | Shows at the top of the listing on phones. Use one of the bundled wallpapers as the backdrop + the FreshWall wordmark. |
| **Phone screenshots** | 1080×1920 portrait, 2–8 of them | Take on your phone or in the AS emulator. Show: Home grid, Long-press preview, Detail screen, Auto-rotate settings, Onboarding hero. |
| **Short description** | ≤80 chars | "Beautiful curated wallpapers from Pexels + Unsplash. Free and ad-light." |
| **Full description** | ≤4000 chars | See draft below |
| **App category** | Personalisation | |
| **Contact details** | Email (required), website (`https://drake0306.github.io/fresh-wall`), phone (optional) |

### Suggested full description (~600 chars — leaves room to grow)

> Fresh wallpapers, every day.
>
> FreshWall pulls from a hand-curated catalogue plus the full Pexels and
> Unsplash photography libraries — millions of free, high-resolution
> images you can apply to your home screen, lock screen, or both.
>
> Features:
> • Pinterest-style staggered grid that respects each photo's natural shape
> • Long-press any wallpaper for photographer details and full attribution
> • Apply through Android's built-in cropper — pan, zoom, pick your screen
> • Schedule auto-rotation from your favourites or the Featured collection
> • Light, dark, and pitch-black themes with system-aware switching
> • Search across both sources, with category quick-picks
> • Favourites that persist across launches
>
> Photographer credit is non-negotiable: every wallpaper carries
> attribution and links back to the original on Pexels or Unsplash.

Tweak the wording — that's just a starting point.

---

## 7. Add internal testers

Play Console → **Testing → Internal testing → Testers**:

1. **Create a test list** — give it a name like "Drake0306 internal".
2. Add tester emails (the Gmail accounts on the phones you'll install
   to). Up to 100 testers per list.
3. Copy the **opt-in URL** Play Console gives you. Send it to each
   tester; they open it on their phone, click **Become a tester**, and
   the app shows up in their Play Store within ~15 minutes.

---

## 8. Create the internal-testing release

Play Console → **Testing → Internal testing → Create new release**:

1. **Upload the AAB** — drag in `app-release.aab`.
2. **Release name**: Play autofills as `1.0.0-beta.1 (2)` from the AAB
   metadata.
3. **Release notes**: paste a short version of the GitHub Release
   changelog. Capped at 500 chars per language.
4. **Next → Save → Review release → Start rollout to Internal testing**.

The build is live for testers within **15–60 minutes**. No Google
review on the internal track — this is the right first step.

---

## 9. Promote to production

When you're confident from internal testing (usually a few days):

1. **Testing → Closed testing → Create release** → import release from
   internal. Add a closed-testing list (or open it up to anyone with the
   link). **Google reviews this**, 3–7 days.
2. After closed-test review passes: **Production → Create release** →
   promote from closed testing. **Another Google review**, 3–7 days.
3. **Staged rollout**: ship to 1% first, then 5%, 20%, 50%, 100% over a
   few days. Lets you halt if Play Console shows ANR / crash spikes.

---

## Watchouts

- **First production review is slow.** Plan 5–7 days from "submit" to
  "live in Play Store". Subsequent updates are usually <24h.
- **Pexels / Unsplash attribution.** Reviewers spot-check that
  photographer credit is intact. FreshWall already surfaces photographer
  name + "Powered by Pexels"/"Powered by Unsplash" badges — don't strip
  them out.
- **R8 is still off.** `PRE_LAUNCH.md` item 4 tracks flipping
  `isMinifyEnabled = true` + smoke-testing on a closed-beta build before
  promoting to production. Not blocking for internal testing.
- **AdMob `app-ads.txt`.** Once you start earning, AdMob may ask you to
  host an `app-ads.txt` file on a verified developer website to prevent
  ad fraud. Not blocking for launch; AdMob emails you when it matters.
- **Target API level.** Play requires API 34+ as of August 2024. We
  target 36 (`build.gradle.kts`), so you're fine.
- **Permissions justification.** If reviewers flag
  `WRITE_EXTERNAL_STORAGE`, point to the `maxSdkVersion=28` cap in the
  manifest — it's only there for legacy gallery saves on Android 9 and
  older.

---

## Suggested timeline

| Day | Action |
|---|---|
| Day 0 | Pay $25, start Play Console verification, build the AAB locally. |
| Day 1 (after verification clears) | Create app entry, fill out App content, set up store listing, add yourself as an internal tester, upload AAB, roll out to Internal track. |
| Week 1 | Install from Play Store on your phone, hammer it for a few days. |
| Week 2 | Promote to closed testing with friends — wait 3–7 days for Google's review. |
| Week 3 | Production release, staged 1% → 100% over 3–4 days. |

---

## What I can help with from this session

Anything that touches the codebase or repo:

- Draft store listing copy (short / full descriptions)
- Generate screenshots from a connected emulator (`adb screencap`)
- Bump `versionCode` + `versionName` before each new internal/closed/prod release
- Wire R8 + `-keep` rules before the production smoke-test
- Update the privacy policy if the data-safety form needs different wording
- Cut subsequent GitHub Releases that match Play Store rollouts

What I can't help with: the Play Console UI itself, payment, identity
verification. Those are entirely your-side work.
