# Health first-frame flash — 1.2.13 /2720 (#153)

Same managed workspace, existing Chromium, 240 workouts × 8 exercises × 4 sets; no installs. Before is released main `dfa3f049ed3f5eb507dec6697f045a3186fffa68` (1.2.12). After is the working-tree fix committed with this evidence (1.2.13), so its recorded Git sourceCommit names the first fix revision `36217f439a5e48239b3ca2b4026cb1b80bf73aa8` before adding the direct legacy hub entry. Both unchanged budget guards passed. Local timings are not physical-phone guarantees.

The fix is in inline Health code in `www/index.html`; appSha256 alone does not identify it. Measured index SHA-256: before `c7be8322d547cc80b61c25c49fd6af5bb14260615c46f13a9a7c855f1a5f668a`, after `cab31af9f5d0c20164b999556d5b0e059c0128cefce1af1382325adc8cc86547`.

| Metric | Before | After |
|---|---:|---:|
| Health first feedback | 59.3 ms | 56.3 ms |
| Health median total | 51.6 ms | 32.7 ms |
| Health median history reads | 3 | 1 |
| Health median mutations | 340 | 260 |
| Health nodes | 460 | 460 |
| Home median | 36 ms | 31.4 ms |
| Programs median | 31.4 ms | 31.6 ms |
| Journal median | 102 ms | 88.9 ms |

Journal still has 1 median history read and 3,617 collapsed nodes; Programs has 338 nodes. The redundant deferred Health rebuild is removed rather than adding a new render, listener, observer or history cache. Actual Health timer/mutation/animation-frame boundary checks fail on the old source's bare chevrons/absent final trend and pass after the fix, across 32 size/language/theme cases including first/repeated navigation async data refresh and direct legacy hub entry. Pulse disclosure retains the mounted trend subtree as before. Final-source full CI and signed phone test remain required; physical acceptance has not been claimed.
