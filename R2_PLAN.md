# Wallpaper hosting on Cloudflare R2 — plan

Working notes for the wallpaper-source migration. **Read this first** before
the code changes land.

## Goal

Move the Featured catalog off bundled JSON / picsum placeholders to
**Cloudflare R2** so we can:

- Ship real curated wallpapers without burning into the APK
- Update the catalog (add / remove / re-order) without an app release
- Pay basically nothing — R2 has zero egress fees and 10 GB free storage
- Keep doing filtering / search / categories with no backend code

## The "no backend" trick

R2 alone doesn't give us a `/wallpapers?category=Nature&q=mountain&sort=newest`
endpoint. R2 stores objects, not queries. We don't want to run a server.

**Solution: a single `manifest.json` on R2 lists every wallpaper with all its
metadata.** The app fetches the manifest once (cached, refresh on pull) and
runs filter / search / sort entirely client-side. This is the standard
serverless pattern — same approach Hugo, Jekyll, and most "static site +
CDN" content apps use.

Tradeoff: the manifest grows linearly with the catalog. At 1k wallpapers
the JSON is maybe 200 KB gzipped, still trivial. If we ever hit 10k+, we
can:

1. Split the manifest into per-category shards.
2. Add a Cloudflare Worker that proxies R2 and does server-side filtering.

Both are escape hatches we don't need yet.

## Bucket layout

```
freshwall-wallpapers (R2 bucket, public)
├── manifest.json                 # the catalog (fetched at startup)
├── thumbs/
│   ├── fw-001.jpg                # 600×1080 portrait thumb
│   ├── fw-002.jpg
│   └── …
└── full/
    ├── fw-001.jpg                # original 1440×2560+ wallpaper
    ├── fw-002.jpg
    └── …
```

We serve everything via a public custom domain (e.g.
`https://wallpapers.freshwall.app`) routed to the bucket. Public R2 buckets
have zero egress cost and Cloudflare's global CDN in front automatically.

## Manifest schema

```jsonc
{
  "version": 1,
  "generatedAt": "2026-05-14T15:30:00Z",
  "wallpapers": [
    {
      "id": "fw-001",
      "name": "Aurora",
      "description": "Soft, drifting tones — calm, atmospheric backdrop.",
      "thumbnailUrl": "https://wallpapers.freshwall.app/thumbs/fw-001.jpg",
      "fullUrl":      "https://wallpapers.freshwall.app/full/fw-001.jpg",
      "author": "Abhinav Roy",
      "authorUrl": null,
      "sourceUrl": null,
      "categories": ["Nature", "Minimal"],
      "tags": ["aurora", "sky", "calm"],
      "width": 1440,
      "height": 2560,
      "fileSize": 412348,
      "uploadedAt": "2026-05-14T15:00:00Z",
      "source": "FEATURED"
    }
  ]
}
```

All wallpaper fields except `id`, `thumbnailUrl`, `fullUrl` are optional in
the parser so old manifests / hand-edited entries don't break the app.

## Client-side behaviour

| What the user does | How the app handles it |
|--------------------|------------------------|
| Open Featured tab | Fetch manifest from R2 (or serve from local cache while revalidating). Render grid. |
| Pull to refresh | Force-refresh manifest from R2. |
| Tap a category chip | Filter wallpapers where `categories` contains the selected label. Re-render grid. |
| Tap "All" | No filter — show every wallpaper. |
| Tap "Daily" | Date-seeded `Random` shuffle, take 6. Same set all day, different tomorrow. |
| Tap "New" | Sort by `uploadedAt` descending, take ~12. |
| (Future) search box | Substring match against `name`, `description`, `tags`. |
| (Future) sort dropdown | Sort by `uploadedAt`, `name`, `fileSize`, or `width × height`. |
| Open detail | Show `dimensions` (from `width` × `height`), `fileSize` ("412 KB"), `uploadedAt`, photographer, link to `sourceUrl`. No more "N/A" placeholders. |

## Categories — dynamic from manifest

The current `WallpaperCategory` enum hard-codes `All` / `Daily` / `Trending`
/ `Curated` / `Classics`. Once the manifest drives content, the categories
also become data-driven:

- **`All`, `Daily`, `New`** stay as built-in meta-categories (computed
  from any manifest).
- **Content categories** (`Nature`, `Minimal`, `Abstract`, etc.) come from
  the union of `categories` arrays across all wallpapers. Chip row shows
  every distinct value sorted alphabetically.

So you control the category list by tagging wallpapers in the manifest —
no app release to add a new category.

## Caching + offline

The manifest itself is a single JSON file under ~200 KB. Strategy:

1. On startup the repository hits the network with a short timeout.
2. On success, it overwrites the cached manifest in the app's files dir.
3. On failure (offline, R2 down), it falls back to the cached copy.
4. If there's no cache yet either, the UI shows an empty state with a
   "Try again" button.

Image bytes are cached by Coil's existing disk cache (150 MB), so once
a thumbnail has been viewed it loads instantly while offline.

## What changes in the codebase

### Already done in this PR

- `Wallpaper` data class extended with `categories`, `tags`, `width`,
  `height`, `fileSize`, `uploadedAt`. All optional, backwards-compatible
  with existing favorites blobs.
- `RemoteWallpaperRepository` fetches the manifest from a configurable URL,
  caches it on disk, and exposes a refresh method.
- `FeaturedViewModel` switched to the new repository. Pull-to-refresh
  hook added.
- Manifest URL exposed as `BuildConfig.WALLPAPER_MANIFEST_URL`, read from
  `local.properties` like the Pexels key.
- Bundled `app/src/main/assets/wallpapers.json` removed.
- `WallpaperCategory` becomes a sealed class: built-in meta-categories +
  a dynamic content-category variant pulled from the manifest.
- Empty / error states in the Featured grid.

### Still on me, once the URL is real

- Wire the actual dynamic-category chip rendering (currently the chip row
  is still the old hardcoded set — easy follow-up).
- Add search-within-Featured if you want it (Pexels search is separate).
- Sort dropdown (`Newest` / `Name` / `Size`).

## What I need from you

Once you've set up the R2 side, paste me back the answers to these three:

1. **The public manifest URL** — e.g. `https://wallpapers.freshwall.app/manifest.json`.
   Whatever domain/path the manifest is reachable at over plain HTTPS, no auth.

2. **The image base URL pattern** — usually the manifest URL's directory.
   I just need confirmation that thumbs and fulls are at
   `<base>/thumbs/<id>.jpg` and `<base>/full/<id>.jpg`. If you'd rather
   embed full URLs in each manifest entry (which the schema already
   supports), just say so — slightly more verbose JSON but cleaner.

3. **Are you using the bucket's default `<account>.r2.dev` subdomain**, or
   are you putting it behind a custom domain via Cloudflare?
   - Default `r2.dev` is fine for testing, but Cloudflare rate-limits it
     (intended to push devs to custom domains for prod).
   - Custom domain (e.g. `wallpapers.freshwall.app`) gives the full CDN
     and is what we want for shipped builds.

**Nothing else.** No API tokens, no bucket access keys — they stay on your
machine for upload scripts. The app only reads, and only over public HTTPS.

## Uploading new wallpapers — your side of the workflow

You don't have to hand-edit `manifest.json`. The cleanest workflow:

1. Drop new image files into a local folder.
2. Run a small script (Node / Python / Bash — pick your poison) that:
   - For each image, generates a 600px-wide portrait thumb.
   - Uploads original to `full/<id>.jpg` and thumb to `thumbs/<id>.jpg`.
   - Reads EXIF / `identify` to grab dimensions, file size, mtime.
   - Builds the manifest entry. Categories / tags can come from a sidecar
     `<id>.meta.json` you write per wallpaper, or be inferred from
     folder structure (`nature/aurora.jpg` → category `Nature`).
   - Writes the full `manifest.json`.
   - Uploads `manifest.json` to the bucket root.

Cloudflare's `wrangler r2 object put` does single-file uploads. I can write
the script once you've got the bucket set up and know what your local
authoring layout looks like — just tell me your preferred runtime.

## Costs sanity-check

- R2 storage: $0.015/GB/month after 10 GB. 1000 wallpapers averaging
  ~500 KB each = ~500 MB — free forever on free tier.
- R2 egress: $0 (this is R2's killer feature vs S3).
- Cloudflare custom-domain SSL: free.
- Class A operations (writes): $4.50/million; you'd have to add ~225k
  wallpapers a month to hit a single dollar.
- Class B operations (reads): $0.36/million. At 100k installs each
  fetching the manifest once on launch — that's $0.04.

So this entire architecture costs effectively zero unless we somehow
hit Pinterest scale.
