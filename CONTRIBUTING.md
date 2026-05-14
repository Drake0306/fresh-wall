# Contributing to FreshWall

Thanks for your interest in helping out. FreshWall is a personal project,
but pull requests, issues, and ideas are welcome.

## Quick start

1. Fork the repo and clone your fork.
2. `cd fresh-wall/app` — this is the Android project root.
3. Add a Pexels API key to `app/local.properties` (see [README](README.md#setup)).
4. Open `fresh-wall/app/` in Android Studio (Iguana or newer), or build from
   the command line:
   ```bash
   ./gradlew :app:assembleDebug
   ./gradlew :app:installDebug
   ```
5. Make your change on a feature branch, push, open a pull request against
   `main`.

## What we're looking for

- Bug fixes (UI glitches, crashes, layout regressions).
- New category filters or wallpaper sources (the bundled `WallpaperCategory`
  enum is intentionally easy to extend).
- Accessibility improvements — content descriptions, larger tap targets,
  TalkBack flow fixes.
- Performance work on the Pexels grid (pagination, prefetch, image cache).
- Better placeholders, transitions, or polish — but please match the existing
  Material 3 Expressive vocabulary (rounded surfaces, secondaryContainer for
  selected state, etc.).

## What we're not looking for

- Bundling Pexels-sourced images into the app (the Pexels License explicitly
  prohibits that — Pexels photos are runtime-only).
- AdMob ad-unit IDs in the source. Keep the public repo on Google's test IDs.
- API keys, signing keys, or any other secret checked into git.
- Cosmetic-only sweeping refactors with no clear product win.

## Code style

- Kotlin, Compose, Material 3.
- Comments only when the *why* is non-obvious — don't restate the *what*.
- No emojis in code, comments, or UI strings.
- Prefer editing existing files over creating new ones.
- Don't introduce abstractions for hypothetical future cases — three similar
  lines beats a premature helper.
- See [CLAUDE.md](CLAUDE.md) for more detail on architecture and conventions.

## Commits and pull requests

- Commit message: short imperative title, blank line, then body explaining
  the *why*. Reference the issue if relevant.
- Don't add `Co-Authored-By` trailers for AI assistants in commits on this
  repo.
- Keep PRs focused — one logical change per PR. If you've got multiple
  improvements, split them.
- Include a short test plan (manual is fine — describe what you actually
  exercised on a device or emulator).
- The CI workflow runs a debug build on PR; please make sure it passes
  before requesting review.

## Reporting bugs

Open an issue. Useful things to include:

- Device + Android version
- Reproduction steps
- Expected vs. actual behavior
- Logcat snippet if the app crashed

If it's a UI bug, a screen recording or screenshot helps a lot.

## Licensing

FreshWall is licensed under the [Apache License 2.0](LICENSE). By submitting
a pull request, you agree that your contribution is offered under the same
license.
