# PR270 active checkpoint

Updated 2026-10-29. PR270 stays DRAFT on `feat/studio-ui-topnav-trial-20261027`,
base `feat/native-android-migration`. Root works alone. No merge or release.

## Verified current gate

Repair source **79ccd5c39575c8654dc4fc56655627e801a46def**.
[Full CI40815986152](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40815986152)
**SUCCESS, 7/7 jobs**; [Quick40815986144](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40815986144)
**SUCCESS**. Actual CI merge23b3ae0c0d89ae08f1e91b6d2b5e730bf38f68ae.
Android tree53ac9dde7ea810e65edbf7e781d84e21b9026433 is identical on repair,
CI merge and incoming151ced0a. Later docs-only commits do not create a new APK.

Independently downloaded raw evidence: **1346 JVM +123 phone +183 Wear PASS**,
0 failure/error/skip/missing;71 original320×640 phone PNGs CRC/hash validated,
app/test APK hashes matched. All183 Wear cases are unique across68+59+56 shards.
Both signed old-release→native upgrade jobs SUCCESS. Phone/Wear release APK hashes
match independently downloaded bytes. [Machine evidence](builds/2026-10-29-pr270-wear-cold-boot.json).

## Wear boot fix proven in this run

Incoming151ced0a /run40761838896 Wear0 restored default_boot, then never reported
sys.boot_completed within10minutes; tests never started and evidence upload failed.
Snapshot corruption is not proven. The repair removes this restore dependency:
all3 Wear shards create fresh AVDs and use `-no-snapshot`; no AVD/adb cache or
snapshot-preparation action. Test gates, scoped cleanup and SDK adb guard remain.
Startup intent is written before launch; bounded failure diagnostics always allow
an artifact even when the test script never starts.20min action/25min job budgets.

New Wear0 log: boot completed in24257ms;68 tests PASS; scoped cleanup stopped
both emulator/helper with no remaining PID. All3 startup manifests confirm fresh
cold boot and instrumentation started; all3 cleanup reports say stopped/exit0.
Local63 host tests, YAML, inline/runner shell syntax and diff checks PASS.

## Visual recheck and next work

[New visual/source recheck](reviews/PR270_VISUAL_RECHECK_2026-10-29.md).
Seven original Drive references hash-verified and opened. Current compact screenshots
show brand-name truncation and ISO Health date formatting. Progress top overview
and initially collapsed Settings still need matched Graphite/Gold owner-profile
captures:1080×2392/450dpi/fontScale0.90. Current CI is320dp, not that profile.
Full Studio parity and physical-device acceptance remain open.

The350d7f54 audit's three old findings were already implemented before this repair:
pulse channel ebe9e739; recovery source/qualifications32e95607; direct existing
weight editor151ced0a. Do not duplicate them. Health intentionally has2columns at
320dp and3 at384dp; the scrolled anatomy PNG does not mean volume cards are absent.

Next: presentation-only compact brand/date refinement and matched overview captures
using the existing native states/writers; preserve48dp controls, localization and
navigation/finalization guards. Do not treat a green gate as full visual acceptance.

Native version stays1.3.71-native-test/2736; both applicationIds remain com.repforge.app.
Original signing key, Room/data, Health Connect/Samsung, Wear, GPS and workout engine
are unchanged. Signed APK pair links/hashes are in the machine evidence and PR body.
