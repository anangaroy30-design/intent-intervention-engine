# INTENT — The Infinite Scroll Intervention Engine

Native Android app (Kotlin + Jetpack Compose) with an Express/SQLite API. The app helps users compare intended use with actual session duration and offers respectful check-ins. It does not diagnose addiction or assume that long sessions are bad.

## What is included
- Android app with Home, Start Intention, Active Session, History, Insights, Intervention Lab, and Settings screens.
- Local session persistence using SharedPreferences for this compact starter implementation.
- Explicit Android Usage Access guidance and `UsageStatsManager` integration.
- Offline demo flow with simulated time advancement.
- Express API with SQLite persistence, input validation, password hashing, JWT auth, sessions, interventions, settings, insights, experiments, export, and account deletion endpoints.
- Backend tests and setup instructions.

## Run the backend
Requirements: Node.js 20+ and npm.

```bash
cd server
cp .env.example .env
npm install
npm start
```

The server listens on port 3000 by default. Set `JWT_SECRET` in `.env` to a long random value before using accounts outside a local demo.

Run backend tests with `npm test`.

## Open the Android app
1. Open the `android/` folder in Android Studio.
2. Let Gradle sync. Use JDK 17 or the version required by the configured Android Gradle Plugin.
3. Run the `app` configuration on an emulator or Android device.

For a physical phone, configure `API_BASE_URL` in `android/app/build.gradle.kts` to your computer's LAN IP (include the trailing slash), and allow local HTTP only for development. The default app is designed to work offline in demo mode. An emulator can generally reach a host machine using `10.0.2.2`.

## Android usage access
Usage statistics require the user to enable Usage Access in Android Settings. The app explains this and opens the system settings page; it does not silently enable monitoring. Usage statistics may be delayed and are not a guaranteed real-time feed. Disable monitoring in Settings to stop reading these statistics.

## Demo flow
1. Launch the app and tap **Start an intention**.
2. Enter a purpose, choose a duration, and start.
3. On the session screen, use **Advance demo time** to test an intervention immediately. This clock is explicitly simulated.
4. Choose finish, continue, extend, or break.
5. Review saved history and insights.

## Limitations and next steps
This is a runnable hackathon starter, not a security-audited production release. Android usage access is platform-controlled; the app cannot read other apps' private content. This starter keeps sessions locally and exposes a backend API; full offline-to-server synchronization, refresh tokens, production signing, and an external AI provider should be completed and audited before public release. AI is intentionally optional: the first version uses transparent deterministic rules.
