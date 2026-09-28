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
