# Google login in Venice AI

The app uses Firebase Authentication with Android Credential Manager. The existing Google button in Privacy Vault signs a Google account into **Venice AI's Firebase project**. ChatGPT OAuth remains a separate account connection.

## Configure Firebase

1. In Firebase Console, register an Android app with package name `com.aistudio.veniceai.prvxzq`. Enable **Authentication → Sign-in method → Google**. If you use Firestore cloud sync, configure Firestore and rules for your users separately.
2. Add the signing certificate's SHA-1 fingerprint to that Android app in Firebase **Project settings → Your apps**. Use the certificate that signs the APK actually installed on your device. Debug, release, and Google Play app-signing certificates have different fingerprints.
3. Download the **updated** `google-services.json` after enabling Google. Place it at `app/google-services.json` for a local build. The file is ignored by Git. Check that its Android package matches the application ID and that it contains the Web OAuth client, which generates `default_web_client_id`.
4. Build and install the app, then open **Privacy Vault → Google Sign-In**. If already in Guest Mode, use **Link Google Account** to retain the guest Firebase UID and its cloud data. If Google reports the credential is already linked to another account, sign out of Guest Mode and then sign into the existing Google account.

The app reports missing Firebase configuration or a missing Web client ID directly; it never uses a placeholder client ID. The Google account chooser requires Google Play services and an account on the device.

## GitHub Actions APK

Set repository **Actions secrets**:

- `GOOGLE_SERVICES_JSON_BASE64`: base64 encoding of the updated `app/google-services.json`.
- `VENICE_DEBUG_KEYSTORE_BASE64`: base64 encoding of a persistent Android debug keystore. This keeps the GitHub Actions debug APK signature and SHA-1 consistent across runs. Its alias and passwords must use the normal Android debug values (`androiddebugkey` / `android` / `android`).

On Linux, `base64 -w 0 app/google-services.json` and `base64 -w 0 ~/.android/debug.keystore` produce the respective secret values. If the debug keystore does not exist yet, generate it once with:

```bash
keytool -genkeypair -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
```

Get its SHA-1 to register in Firebase:

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

GitHub Actions builds still run when these secrets are absent, but Google login cannot work in that APK. A release APK needs its **release signing certificate's SHA-1** registered separately. Do not commit keystore passwords or keystore files.

Firebase setup: https://firebase.google.com/docs/auth/android/google-signin
Android Credential Manager: https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation

## Signed release APK

Use the **existing release signing keystore** if this package has already been installed as a signed release. Android requires the same signing certificate for an in-place update. The old debug APK may have a different certificate.

Set these GitHub Actions repository secrets:

- `VENICE_RELEASE_KEYSTORE_BASE64`: `base64 -w 0 /path/to/existing-release-key.jks`.
- `VENICE_RELEASE_STORE_PASSWORD`: keystore password.
- `VENICE_RELEASE_KEY_PASSWORD`: password for the signing key.
- `VENICE_RELEASE_KEY_ALIAS`: alias of that key (optional when the alias is `upload`).
- `GOOGLE_SERVICES_JSON_BASE64`: the updated Firebase file as described above, required so the Google-login release is functional.

In **Actions → Build Android APK (Adaptive CI/CD Runner) → Run workflow**, select the branch containing the Google-login changes and choose `release`. The workflow now fails if a required signing input or Firebase config is missing. It runs `assembleRelease`, selects only `app-release.apk`, verifies its signature with `apksigner`, and uploads the release APK artifact. It never substitutes a debug APK for a release.

To register the release certificate in Firebase, read its SHA-1 from the existing keystore:

```bash
keytool -list -v -keystore /path/to/existing-release-key.jks -alias upload
```

Use your actual alias if different. Keep the keystore and passwords backed up outside the repository.
