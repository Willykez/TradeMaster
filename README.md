# TradeMaster Pro (Android)

Native Kotlin + Jetpack Compose rebuild of the original web app. Zero XML
layouts. Five bottom-nav destinations (Dashboard, Signals, Setups,
Community, Learn) -- Feed/Polls/Q&A merged into **Community**,
Courses/Media merged into **Learn**.

This build is wired for two "real" things:

1. **Live market prices** via Twelve Data's REST API (falls back to a local
   random-walk simulator if unconfigured, so the app never breaks).
2. **A real backend** via Firebase: Firestore is the shared source of truth
   for signals/posts/polls/Q&A/courses/media, mirrored into a local Room
   cache on every device in real time. Room is what the UI reads from
   (fast, works offline); Firestore is what makes admin's publish show up
   on *every* installed copy of the app, not just the admin's own phone.

Nothing in this repo can be compiled or run without your own credentials --
none are included, on purpose. Here's exactly what to set up.

## 1. Firebase project

1. Create a project at https://console.firebase.google.com
2. Add an Android app with package name `com.trademaster.pro`
3. Download `google-services.json` and place it at `app/google-services.json`
   (already gitignored -- never commit this file)
4. Enable **Firestore Database** (start in production mode -- the rules
   below replace the default deny-all)
5. Enable **Authentication → Anonymous** sign-in (the app signs every
   install in anonymously on launch so Firestore rules can tell "someone
   with the app" apart from "an admin")
6. Enable **Cloud Messaging** (on by default for most projects)

## 2. Deploy the security rules

```
npm install -g firebase-tools
firebase login
firebase use --add            # pick the project you just created
firebase deploy --only firestore:rules
```

Without this step, Firestore's default rules deny everything and the app
will show empty screens forever (reads will fail silently in the listener,
see `FirestoreDataSource`).

## 3. Grant yourself admin access

The Admin toggle in the app's top bar is cosmetic on its own -- what
actually grants write access is a document at `admins/{your uid}` in
Firestore, which nothing in the app is allowed to create (see
`firestore.rules` -- writes to `/admins` are blocked entirely, on purpose).

1. Run the app once (triggers anonymous sign-in)
2. Firebase Console → Authentication → Users → copy the UID of the (only)
   anonymous user
3. Firebase Console → Firestore → start collection `admins` → document ID =
   that UID → any field, e.g. `{ "grantedAt": <timestamp> }`
4. Relaunch the app (or wait a moment -- it's a live listener) and the
   admin icon in the top bar lights up gold

Repeat step 3 for anyone else you want to be able to publish content.

## 4. Live market data (optional but recommended)

1. Get a free key at https://twelvedata.com/pricing (free tier: 8
   requests/min, 800/day -- the app polls all 10 pairs in one batched
   request every 15s, well inside that)
2. In the project root, copy `local.properties.example` → `local.properties`
   (Android Studio already creates this file and gitignores it for you --
   it's where `sdk.dir` lives too) and add:
   ```
   TWELVE_DATA_API_KEY=your_key_here
   ```
3. Rebuild. The Dashboard's "● Live prices" / "○ Demo prices" badge tells
   you which one is actually active.

## 5. Push notifications (optional)

The app subscribes every install to the `new_signals` FCM topic on launch.
Something server-side has to actually publish to that topic when a signal
is created -- that's `functions/src/index.ts`, a Cloud Function that fires
on new Firestore documents.

```
cd functions
npm install
firebase deploy --only functions
```

Needs the **Blaze (pay-as-you-go)** plan -- Cloud Functions aren't
available on Firebase's free Spark plan. The free tier's monthly quota
(2M invocations) is enormous for this use case, so realistically this
costs nothing unless the app gets very large.

## 6. CI (GitHub Actions)

`app/google-services.json` is gitignored on purpose (it's a real Firebase
credential), which means a fresh CI runner has none -- `assembleDebug` will
fail on `:app:processDebugGoogleServices` with "File google-services.json is
missing" until it has one. Two ways to give it one:

- **Real credentials (production builds):** base64-encode your
  `google-services.json`, store it as a GitHub Actions secret named
  `GOOGLE_SERVICES_JSON_B64`, and decode it back to `app/google-services.json`
  as a build step. `TWELVE_DATA_API_KEY` works the same way for
  `local.properties`.
- **Placeholder (compile-check only, e.g. PRs from forks that shouldn't get
  real secrets):** `app/google-services.json.ci-placeholder` is a
  schema-valid dummy that lets the Google Services plugin succeed and the
  APK compile and package -- Firebase calls will fail at runtime with it,
  but that's fine for "does this even build" gating.

`.github/workflows/android-ci.yml.example` does both: uses the real secret
when it's set, falls back to the placeholder otherwise. Rename it (drop
`.example`) once the secret is in place.

## 7. Release signing (for the tag-triggered release workflow)

`.github/workflows/release.yml` decodes a keystore secret and passes signing
credentials as env vars; `app/build.gradle.kts` reads them and wires a real
`signingConfig` for the `release` build type -- if any of these aren't set,
the release build type is simply left unsigned (buildable locally for
testing, not something to distribute). Four repo secrets needed:

- `KEYSTORE_B64` -- your `.jks`/`.keystore` file, base64-encoded
  (`base64 -i your.keystore | tr -d '\n'`)
- `KEY_ALIAS`
- `STORE_PASSWORD`
- `KEY_PASSWORD`

Don't have a keystore yet: `keytool -genkeypair -v -keystore release.keystore -alias trademaster -keyalg RSA -keysize 2048 -validity 10000`.
Keep that file and its passwords somewhere durable outside git -- losing it
means you can never publish an update to the same app listing again.

## What's still not done

- **No email/password login, no per-user identity** -- every install is an
  anonymous Firebase user. That's enough to gate admin writes (which is the
  security-sensitive part), but there's no "your account", no user profiles,
  no per-device signal history synced across a user's own devices.
- **Settings screen is a stub** -- no export/import/reset yet; those need a
  deliberate design decision (wipe just Room's cache? actually delete cloud
  data? that's a lot more dangerous with a shared backend than it was with
  per-device localStorage).
- **No app icon, ProGuard rules, or release signing config** -- this is a
  dev-config build, not a Play Store-ready one.
- **I could not compile or run any of this** -- no Android SDK and no
  network access to Google's Maven repo, Firebase, or Twelve Data from
  where this was built. Everything here was written and manually
  reviewed (brace-balance checked, call-signature checked, import-checked)
  but the real test is a Gradle sync + run in Android Studio. Paste me
  the first error if one shows up.
