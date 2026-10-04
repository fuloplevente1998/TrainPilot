# TrainPilot release and version policy

TrainPilot uses Semantic Versioning from the current stable line onward. Historical tags and releases are retained as published evidence; they are not renamed, deleted, or rewritten to make the older history look cleaner.

## Version format

Stable releases use:

```text
MAJOR.MINOR.PATCH
```

Test candidates use a SemVer prerelease suffix:

```text
MAJOR.MINOR.PATCH-rc.N
```

`alpha.N` and `beta.N` are also permitted when they are genuinely useful. Android `versionCode` is independent from SemVer and must increase monotonically for every installable build.

Examples after 1.2.6:

- first test candidate: `1.2.7-rc.1` with the next Android versionCode;
- another rejected/fixed candidate: `1.2.7-rc.2`;
- phone-approved stable build: `1.2.7` with a newer versionCode.

Do not publish `1.2.7`, `1.2.8`, `1.2.9` merely to distinguish test APKs.

## When to increment

- **PATCH**: backward-compatible bug fixes, reliability work, refactoring, documentation/repository maintenance that accompanies a release.
- **MINOR**: backward-compatible user-facing features or substantial new capabilities.
- **MAJOR**: intentionally incompatible behavior, storage/API contracts, or migration boundaries.

A refactor by itself does not justify a MINOR bump.

## Branch and release flow

1. Start from the latest accepted `main`.
2. Development and refactoring happen on a short-lived branch.
3. If a phone-test APK is needed, assign the next planned stable version with `-rc.N`.
4. CI builds and validates the candidate. A candidate may be tagged `vX.Y.Z-rc.N`; GitHub must mark it as a prerelease.
5. Rejected candidates keep their identity. Fixes use the next `rc.N`; do not overwrite evidence.
6. After explicit acceptance, promote metadata to plain `X.Y.Z`, run the full gate, merge to `main`, and publish `vX.Y.Z`.
7. `main` may contain only stable versions.

## Historical releases

The existing 1.0.x, 1.1.x candidate and 1.2.x history predates this policy. Those tags/releases remain untouched so URLs, APK provenance, checksums and audit history continue to work. The policy is forward-looking; no history rewrite is required.

## Automated guard

`npm run check:version` validates:

- the allowed stable/prerelease SemVer shape;
- equality of `package.json`, `SOURCE_VERSION.json` and Android `versionName`;
- equality and validity of Android `versionCode`;
- tag/version equality in tagged CI;
- that a prerelease version cannot run as `main`.

The normal `npm test` command includes this guard.
