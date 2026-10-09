# Health first-frame flash — 1.2.13 /2720 (#153)

Same managed workspace, existing Chromium, 240 workouts × 8 exercises × 4 sets; no installs. Before is released main `dfa3f049ed3f5eb507dec6697f045a3186fffa68` (1.2.12). After is the working-tree fix committed with this evidence (1.2.13), so its recorded Git sourceCommit still names the parent. Both unchanged budget guards passed. Local timings are not physical-phone guarantees.

The fix is in inline Health code in `www/index.html`; appSha256 alone does not identify it. Measured index SHA-256: before `c7be8322d547cc80b61c25c49fd6af5bb14260615c46f13a9a7c855f1a5f668a`, after `9e06f0f080a457748beeab2bc23211162cc3b01d07391d2894b98baefaaa53fb`.

| Metric | Before | After |
|---|---:|---:|
| Health first feedback | 59.3 ms | 58.8 ms |
| Health median total | 51.6 ms | 31.4 ms |
| Health median history reads | 3 | 1 |
| Health median mutations | 340 | 260 |
| Health nodes | 460 | 460 |
| Home median | 36 ms | 34.1 ms |
| Programs median | 31.4 ms | 30.9 ms |
| Journal median | 102 ms | 91.2 ms |

Journal still has 1 median history read and 3,617 collapsed nodes; Programs has 338 nodes. The redundant deferred Health rebuild is removed rather than adding a new render, listener, observer or history cache. Actual Health timer/mutation/animation-frame boundary checks fail on the old source's bare chevrons/absent final trend and pass after the fix, across 32 size/language/theme cases including first/repeated navigation and async data refresh. Pulse disclosure retains the mounted trend subtree as before. Final-source full CI and signed phone test remain required; physical acceptance has not been claimed.
