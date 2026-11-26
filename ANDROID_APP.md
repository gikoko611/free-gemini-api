# G.I Gemini Android app

The Android app module lives in `app/` and targets Android 10+ (API 29). It is built by GitHub Actions; no local Android SDK or PC is required.

## Current app features

- Midnight Gold / emerald G.I branding and custom launcher icon based on the uploaded G.I + Android artwork.
- Gemini Website opens in Chrome Custom Tabs so the user's normal Google browser session can be used. Google credentials and website cookies are not copied to this repository's server.
- Native OpenAI-compatible chat client.
- API Base URL, API key and model ID settings.
- API keys encrypted at rest using Android Keystore AES-GCM.
- Model discovery through the provider's `GET /v1/models` endpoint.
- Local SQLite chat history.
- GitHub Actions debug APK build.

## Important Hydrogen distinction

The Gemini Website is the supported route for using Hydrogen with the Google Account and plan that Google makes eligible. This app opens the official website; it does not reverse-engineer private Gemini web endpoints, extract session cookies, or claim that a Gemini Apps model name is a public Gemini API model ID.

The native API chat is a separate OpenAI-compatible provider connection. Set its Base URL, API key and model ID to values actually issued by that provider. The app does not translate Google Website login into API access.

## Build

Open the repository's **Actions** tab and select **Build G.I Gemini Android APK**. The workflow builds `app-debug.apk` and uploads it as the `gikoko-gemini-debug` artifact.

Package: `com.ikoko.gemini4`

Minimum Android: 10 / API 29.

## Security notes

- Do not paste Google session cookies into the API key field.
- Use HTTPS API endpoints.
- API keys are encrypted on-device with an Android Keystore key.
- The legacy Go server and Chrome cookie-bridge are separate from the Android app flow and are not called by this app.
