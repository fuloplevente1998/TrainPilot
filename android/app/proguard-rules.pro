# TrainPilot phone R8 release: keep application entry points and native JS
# bridges stable across reflective Capacitor, JSON and wearable callbacks.
# Conservative initial scope: reduce library code/resources first; narrow these
# keep rules only after a release-APK-on-device compatibility matrix passes.
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# All app-owned native plugins, Parcelable/JSON models, manifest services and
# background callbacks. Plugin annotation methods are looked up by JS names.
-keep class com.repforge.app.** { *; }
-keep class com.repforge.shared.** { *; }

# Capacitor bridge/plugins use reflection and annotated method discovery.
-keep class com.getcapacitor.** { *; }
-keep class * extends com.getcapacitor.Plugin { *; }

# Samsung Health data library models are resolved by SDK-specific adapters.
-keep class com.samsung.android.sdk.health.data.** { *; }

# Watch Data Layer message/data transport, including Play Services consumers.
-keep class com.google.android.gms.wearable.** { *; }

# Preserve application-owned Gson JSON contracts and fields with SerializedName.
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
