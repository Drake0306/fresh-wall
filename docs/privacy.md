---
title: FreshWall Privacy Policy
---

# FreshWall — Privacy Policy

*Last updated: 15 May 2026*

FreshWall is a wallpaper app I build in my free time. This page is the
honest version of what the app does and doesn't do with your data. Plain
English, no legalese, no template boilerplate.

If something here isn't clear or you spot a mistake, email me:
**abhinavroy.hello@gmail.com**.

---

## The short version

- **No signup, no account.** You can use FreshWall without giving me your
  name, email, phone number, or anything else about you.
- **Wallpapers come from Pexels and Unsplash.** Both are third-party
  services. Your phone fetches images from them directly — they see your
  IP and what category you asked for, the same as visiting their websites.
- **There are ads.** A short rewarded ad before applying a wallpaper. The
  ad provider (Google AdMob) gets your Android advertising ID, which you
  can reset or limit any time from your phone's settings.
- **Crashes are sent to me.** If the app crashes, a stack trace is shipped
  to Firebase Crashlytics so I can fix it. No personal info, just
  device / OS / app-version context.
- **Your favorites, theme, search history, category preferences all stay
  on your phone.** I never see them.
- **Feedback you write goes into a private Firebase database** — but only
  when you actually tap Send on the Feedback screen.

That's it. The rest of this page is the same thing in more detail.

---

## What stays on your phone (never leaves)

These all live in `SharedPreferences` on your device. I have zero
visibility into them:

- Your theme choice (light / dark / system)
- Your favourited wallpapers
- Your search history
- Your category preferences and starred top picks
- Your auto-rotate settings
- Cached images (handled by Coil)
- Cached API responses (handled by OkHttp's disk cache)

If you uninstall FreshWall, all of this is wiped with it.

---

## What goes to third parties

### Google AdMob — ads

The app shows a short rewarded ad before applying a wallpaper. Ad serving
is handled entirely by Google AdMob. AdMob receives:

- Your Android **advertising ID** (a random, resettable identifier — not
  your name or account)
- Approximate location derived from your IP, plus standard device
  attributes (model, OS, language)

You can reset or limit this any time on your phone:
**Settings → Privacy → Ads → Delete advertising ID** (or
"Opt out of personalised ads").

AdMob's own privacy details:
<https://policies.google.com/technologies/ads>

### Pexels and Unsplash — wallpapers

When you browse wallpapers, your phone fetches images and metadata
directly from `api.pexels.com` and `api.unsplash.com`. Each request
includes:

- Your IP address (because every HTTP request includes one)
- A standard `User-Agent` string
- The category or search term you asked for ("mountains", "sunset", etc.)

Neither Pexels nor Unsplash gets any persistent identifier from us. They
have no way to tie a request to a specific user.

Unsplash additionally counts when one of their photos is downloaded or
applied — their API guidelines require this so photographers get
attribution credit. When you apply or save an Unsplash photo, the app
makes one extra HTTPS request to Unsplash with the photo's ID. Anonymous
from your side; Unsplash doesn't know it was you.

- Pexels privacy policy: <https://www.pexels.com/privacy-policy/>
- Unsplash privacy policy: <https://unsplash.com/privacy>

### Firebase Crashlytics — crash reports

If FreshWall crashes, a stack trace gets sent to Firebase Crashlytics
(part of Google Firebase). The report includes:

- Your device model, Android version, and app version
- The stack trace of what crashed
- An anonymous Firebase installation ID

It does **not** include your name, email, location, account, or anything
about what you were doing in the app other than where the crash
originated. It exists solely so I can find and fix bugs I'd otherwise
never hear about.

Firebase privacy: <https://firebase.google.com/support/privacy>

### Firebase Firestore + Storage — feedback you send me

The in-app feedback screen uses Firebase. **Nothing is sent unless you
actually tap Send.** When you do:

- The text you typed is written to a private Firestore collection only I
  can read
- If you attached a screenshot, it's uploaded to Firebase Storage
- Firebase creates an **anonymous** auth user behind the scenes so its
  security rules can permit the write — this anonymous user is not
  linked to any Google account or any other identifier you have

Don't submit feedback if you don't want me to see what you typed. That's
the whole guarantee.

### Razorpay and Ko-fi — donations (only if you choose to tip)

The Donate screen has buttons that open your browser to either Razorpay
(for INR) or Ko-fi (for USD). FreshWall does not process any payment
information itself. If you decide to send a tip, the donation happens on
their hosted pages and they hold whatever payment info you provide. Their
privacy policies apply:

- Razorpay: <https://razorpay.com/privacy/>
- Ko-fi: <https://more.ko-fi.com/privacy>

---

## Permissions the app asks for

- **Internet** — required to fetch wallpapers and load ads
- **Set wallpaper** — required so the app can change your home/lock screen
- **Write external storage** — only on Android 8 and 9, only so saving a
  wallpaper to your gallery works. Android 10+ doesn't ask for it.
- **Advertising ID** — added automatically by the Google Mobile Ads SDK
  for Android 13+. Already covered under the AdMob section above.

The app **does not** ask for:

- Location
- Camera
- Microphone
- Contacts
- Calendar
- SMS / call logs
- Storage read access to your other apps

If you ever see FreshWall asking for one of those, something's wrong —
email me.

---

## What I deliberately don't do

To be explicit:

- I don't run analytics that profile you. Crashlytics is for crash
  reports, full stop.
- I don't sell or share data with anyone.
- I don't send your favourites, browsing history, or which categories you
  tap to any server.
- I don't target you based on personal info — I don't have any to target
  with.
- I don't use tracking pixels or third-party SDKs beyond AdMob,
  Crashlytics, and the Firebase services described above.

---

## Children

FreshWall isn't designed for children under 13. I don't knowingly collect
personal information from anyone under 13. If you're a parent and think
your child has interacted with the app in a way that gave me personal
info — feedback they sent, mostly — email me and I'll remove it.

---

## Security

Everything goes over HTTPS. Firebase access is locked down by
[security rules](https://github.com/Drake0306/fresh-wall/blob/main/firestore.rules)
that you can read in the repo (they're open source like the rest of the
app). That said, no internet system is 100% safe, and I'm one person
building this in spare time. If you spot a security issue, please email
me before disclosing it publicly so I can fix it.

---

## Changes to this policy

If the app starts collecting something new — say, push notifications get
added or a new third-party service comes in — I'll update this page and
bump the "Last updated" date at the top. Big changes (collecting something
materially different from what's here) I'll mention in the next app
update's release notes too.

---

## Contact

**abhinavroy.hello@gmail.com** — open inbox for questions, concerns,
data-deletion requests, or polite roasts.
