# TrainPilot Wear OS 1.2.15

- Active-workout recording recovers the original exercise type and GPS configuration after a service/process restart. A pending stop is persisted, so recovery closes the existing recording instead of starting another one.
- The stop command waits for the final Health Services update before publishing the summary. A missing final update produces a clearly marked partial result. The ongoing measurement notification opens the workout and offers a measurement-only stop action.
- The measurement screen distinguishes enabled, recording, ending and completed states. The post-workout summary shows the same workout's measured average/max pulse, total energy, steps and distance when available; missing data stays absent and measured zero stays zero.
- The weekly calendar switches between the already cached weeks with horizontal swipes instead of small arrow targets. Its heading shows the selected week's date range and both months when a week crosses a month boundary. Day taps still open details and require a separate Start action.
- Release builds use R8 and resource shrinking. Signing identity, application ID and Data Layer compatibility are preserved. Physical Galaxy Watch7 sensor, screen-off recovery, notification and compact/enlarged-text layout validation remains required before promotion to main.
