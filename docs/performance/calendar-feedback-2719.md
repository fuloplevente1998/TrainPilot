# Calendar planning and shared action feedback — 1.2.12 /2719

Same managed workspace, existing Chromium, 240 workouts × 8 exercises × 4 sets; no installs. Before is committed PR #152 head `58cabcc78e6e50ac18b1617c940a352e47302316`. After is the working-tree implementation committed with this evidence, so the benchmark's Git sourceCommit still names its parent; its appSha256 identifies the measured application bytes. Both unchanged budget guards passed. These local timings do not claim physical-phone performance.

| Metric | Before | After |
|---|---:|---:|
| Startup boot | 620 ms | 638 ms |
| home median | 31.1 ms | 31.8 ms |
| programs median | 31.9 ms | 31 ms |
| history median | 118 ms | 101.3 ms |

Journal structure is unchanged: 1 median history() read, 3,617 collapsed DOM nodes; Programs remains 338 nodes. Shared success feedback has one DOM host, one current dismissal timer and no full-page render; the behavioral regression proves that 20 successive feedback calls do not render the app and that redraws keep the single host. Existing error/dirty feedback remains in place. Final-source CI benchmarks and the complete release gate are required before merge.
