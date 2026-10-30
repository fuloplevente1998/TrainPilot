# Only this test entry point has a cross-APK ABI. Its implementation and all
# runtime dependencies are otherwise optimized together with the real release.
-keep class com.repforge.app.NativeMigrationProbe { *; }
