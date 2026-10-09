# Program replacement performance evidence — phone 1.2.12 /2719

Runner: existing `/usr/bin/chromium` in the cloud workspace, checked-in `tests/browser/phase7-performance-19.cjs`, 240 × 8 × 4 reference dataset. No browser or Android tools were installed locally. Before: main `7fce8bf22103bd3544a8cb07829cb504a3aff187`; after: working tree on that base, identified by `appSha256` in each log. Versions differ only because this candidate increments phone metadata.

The before and final after logs pass the unchanged budget guard. Collapsed Journal keeps one history read and 3,617 nodes; Programs keeps 338 nodes. These Chromium timings are not physical-phone guarantees or evidence of a speedup.

The first after run overlapped Node and calendar browser tests: startup was 958.5 ms and exceeded the 900 ms guard. Its full log is retained as `replan-program-2719-after-concurrent.log`. Repeating with the other tests finished passed every guard. The final merged-selection implementation measured 659.4 ms startup and also passed every guard. No budget or product code was changed to resolve this runner-load spike.

Reproduce:

```sh
TRAINPILOT_CHROMIUM=/usr/bin/chromium node tests/browser/phase7-performance-19.cjs > benchmark.log
node tests/browser/phase7-performance-budget.cjs benchmark.log
```
