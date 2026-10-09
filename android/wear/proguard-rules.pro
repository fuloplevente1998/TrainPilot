# TrainPilot Wear OS
# Framework entry points are retained by the Android Gradle Plugin, and the
# Health Services / Wear libraries provide their own consumer rules.
# JSONObject persistence uses explicit keys, without reflection over app models.
# Avoid broad keep rules: they would retain unused Compose / Play Services code.
