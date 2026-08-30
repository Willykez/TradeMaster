# Release builds & signing

`.github/workflows/release.yml` builds a release APK on every push of a `v*` tag
(e.g. `v1.0.0`), or on demand via the Actions tab ("Run workflow").

## Without signing secrets
The workflow still runs and produces an **unsigned** release APK as a build
artifact (Actions → run → Artifacts). Fine for CI smoke-testing; not
installable as-is on most devices without signing it yourself first.

## With signing secrets (recommended)
Add these four repo secrets — **Settings → Secrets and variables → Actions → New
repository secret**:

| Secret name                  | Value                                              |
|-------------------------------|-----------------------------------------------------|
| `ANDROID_KEYSTORE_BASE64`     | your `.jks`/`.keystore` file, base64-encoded         |
| `ANDROID_KEYSTORE_PASSWORD`   | keystore password                                    |
| `ANDROID_KEY_ALIAS`           | the key alias inside the keystore                    |
| `ANDROID_KEY_PASSWORD`        | password for that specific key                       |

### Generating a keystore (one-time, do this locally — never commit the file)
```
keytool -genkeypair -v -keystore release.keystore \
  -alias codeorganizer -keyalg RSA -keysize 2048 -validity 10000
```

### Base64-encoding it for the secret
```
# macOS / Linux
base64 -i release.keystore | tr -d '\n' | pbcopy   # copies to clipboard (macOS)
base64 -w0 release.keystore                          # Linux: prints it, copy manually

# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Set-Clipboard
```
Paste the result as `ANDROID_KEYSTORE_BASE64`.

## Cutting a release
```
git tag v1.0.0
git push origin v1.0.0
```
The workflow builds, signs (if secrets are set), uploads the APK as a build
artifact, and — because this was a tag push — also attaches it to a new
GitHub Release automatically.

## Local release builds
The same env vars work outside CI too:
```
export ANDROID_KEYSTORE_PATH=/path/to/release.keystore
export ANDROID_KEYSTORE_PASSWORD=...
export ANDROID_KEY_ALIAS=...
export ANDROID_KEY_PASSWORD=...
./gradlew assembleRelease
```
Leave them unset and `assembleRelease` still works — you just get an unsigned APK.
