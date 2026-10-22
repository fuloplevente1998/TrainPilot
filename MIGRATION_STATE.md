# TrainPilot v1.3.71 Natív Migrációs Állapot

## Összegzés
A hibrid Capacitor/webes réteg véglegesen törölve lett. Az alkalmazás 100%-ban natív Android (Jetpack Compose, Room, DataStore, Foreground Services) és Wear OS (Wear Compose, Health Services) multi-modul ökoszisztémává alakult át kódduplikáció nélkül.

## Szeletek Állapota (Migration Slices)

| Szelet | Leírás | Állapot | Megvalósított komponensek |
| :--- | :--- | :---: | :--- |
| **Slice 1** | `:shared` Közös motorok és szerződések |  KÉSZ | `IntervalTimeline193`, `GpsRoutePacket193`, `GpsRouteSampler193`, `DistanceAccumulator193`, `GpsRouteTrace193`, `GpsRouteSpool193`, `GpsRouteLedger193`, `WorkoutOwnership193`, `OwnershipJournal193`, `LegacySyncIdentity18`, `WorkoutStatsCalculator` |
| **Slice 2** | `:app` Room Adatbázis & Tárolók |  KÉSZ | `TrainPilotDatabase` (10 entitás, 6 DAO), `TrainPilotSettingsStore` (Preferences DataStore), `LegacyWebViewMigrator` (LevelDB/SAF mentésmigráló), `ActiveWorkoutStore`, `PhoneWorkoutExecutionStore`, `GpsRouteStore`, `HealthJournalStore` |
| **Slice 3** | `:app` Wear Szinkron & Parancsmotor |  KÉSZ | `WearLiveHealthStore`, `WearWorkoutHandoff`, `WearCommandQueueStore`, `WearCommandReceipts`, `PhoneGpsRoute193`, `PhoneOwnership193`, `PhoneWorkoutExecution`, `PersonalRecordCalculator226`, `WearSyncRepository`, `WearCommandListenerService` |
| **Slice 4** | `:app` Előtérszolgáltatások & Szenzorok |  KÉSZ | `IntervalTimerService`, `DistanceTrackingService`, `BleWatchAndProtocols` (0x180D, 0x2A37, 0xED), `HealthBackgroundService`, `HealthBackgroundJob`, `BleDiscoveryRepository` (#111 BLE), `HealthConnectJournal` (26 metrika), `HealthConnectRepository`, `SamsungHealthRepository`, `AppHapticsManager` (#227) |
| **Slice 5** | `:app` Fájl, Fotó & Felhőszinkron |  KÉSZ | `ArchiveIO` (Zip Slip védelemmel), `WorkoutPhotoRepository` (1080p JPEG), `PrivacyPolicy` (HU/EN/DE/RO), `HealthPrivacyActivity`, `StartupScreen`, `BackupRestoreManager` (SAF JSON/ZIP, naptár Intent), `GoogleRequestPolicy`, `GoogleSyncRepository` (#18 Drive merge motor) |
| **Slice 6** | `:app` Compose Alap, Nav & Főképernyők |  KÉSZ | `TrainPilotTheme`, `MainActivity`, `TrainPilotNavigation` (TopBar, BottomBar, lebegő edzéssáv), `HomeViewModel`, `HomeScreen` (Phase 5 #44 Mai állapot 6-os rács + Coach), `TrainingProgramsViewModel`, `TrainingProgramsScreen` (#107 + #37 Tervező) |
| **Slice 7** | `:app` Aktív Edzés Képernyő |  KÉSZ | `ActiveWorkoutViewModel`, `ActiveWorkoutScreen` (szett táblázat, #229 RIR, #188 Undo sáv 8 mp, timerek, élő GPS, óra-pulzus, fotócsatolás, FinishWorkoutDialog RPE) |
| **Slice 8/A**| `:app` Napló & Fejlődés |  KÉSZ | `JournalViewModel`, `JournalScreen` (#226 PR badge/details), `WorkoutRouteMapDialog` (#163 Mercator Canvas offline GPS térkép), `ProgressViewModel`, `ProgressScreen` (#177 Izom-anatómia + 1RM görbe + #215 testsúly) |
| **Slice 8/B**| `:app` Egészségnapló & Beállítások | ⏳ KÖVETKEZIK | `HealthDashboardViewModel`, `HealthDashboardScreen` (26 metrika + BLE), `SettingsViewModel`, `SettingsScreen` (paletták, nyelv, Drive szinkron) |
