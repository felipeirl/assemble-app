# Contributing

Thanks for taking the time to contribute.

## Setup

1. Install Android Studio with AGP 9.0.0 support and JDK 17.
2. Clone the repository and open it. Without a `local.properties`, the app runs on fake data, which is enough for most work.
3. For the real backend, follow [Getting started](README.md#getting-started) in the README.

Run the checks before you open a pull request:

```bash
./gradlew testDebugUnitTest assembleDebug
```

## Pull requests

- Keep a PR to one change. Split refactors from behavior changes.
- Add or update unit tests for changes in domain rules (`core/domain`) and view models.
- Keep `values/` and `values-pt-rBR/` in sync when you add or change a string. The app ships in Portuguese and English.
- If a change needs a backend change, link the matching PR in [assemble-api](https://github.com/felipeirl/assemble-api).
- Add a line to [`CHANGELOG.md`](CHANGELOG.md) under a new version heading when the change is user-visible.
- Use the commit style already in the history: `feat:`, `fix:`, `chore:`, `docs:`, `test:`, `perf:`.

## Code style

- Follow the existing Compose and design-system components in `core/designsystem` before creating new ones.
- Keep domain logic in pure functions under `core/domain`, without Android dependencies.
- Do not commit `local.properties`, `google-services.json`, keystores, or any other secret.

## Reporting bugs

Open an issue with the steps to reproduce, the device and Android version, and the expected and actual results. Remove personal data from screenshots.

Security issues are handled differently; see [SECURITY.md](SECURITY.md).
