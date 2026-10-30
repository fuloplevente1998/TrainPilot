'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');

// Parse the reproducible Phase 7 Chromium benchmark; never synthesize missing measurements.
const input = fs.readFileSync(process.argv[2] || 0, 'utf8');
const marker = 'PHASE7_METRICS ';
const lines = input.split(/\r?\n/).filter(line => line.startsWith(marker));
assert.equal(lines.length, 1, 'expected exactly one PHASE7_METRICS record');
const metrics = JSON.parse(lines[0].slice(marker.length));
const routes = Object.fromEntries(metrics.routes.map(row => [row.label, row]));
const required = ['home', 'programs', 'history', 'coach-panel'];
required.forEach(name => assert.ok(routes[name], 'missing route metric: ' + name));
const failures = [];
const checks = [];
const check = (name, actual, limit, unit) => {
  if (!Number.isFinite(actual) || actual > limit) failures.push(name + ': ' + actual + ' > ' + limit + ' ' + unit);
  checks.push({name, actual, limit, unit});
};

// Structural guards protect the actual Phase 7 architecture, even on a noisy CI runner.
check('Journal history() calls, median', routes.history.medianHistoryCalls, 3, 'calls');
check('Journal collapsed DOM nodes', routes.history.nodes, 6000, 'nodes');
check('Programs lazy DOM nodes', routes.programs.nodes, 1000, 'nodes');
check('Home history() calls, median', routes.home.medianHistoryCalls, 6, 'calls');
check('Journal expansion history() calls, median', metrics.workoutExpand.medianHistoryCalls, 3, 'calls');

// Generous CI tripwires: approximately 4–8x the accepted 1.7.4 reference.
// These are not claims about Android milliseconds and should be calibrated against CI variance.
check('Startup boot', metrics.startup.bootMs, 900, 'ms');
check('Journal route median', routes.history.medianTotal, 650, 'ms');
check('Home route median', routes.home.medianTotal, 250, 'ms');
check('Programs route median', routes.programs.medianTotal, 250, 'ms');
check('Coach first visible feedback', routes['coach-panel'].firstFeedback, 250, 'ms');
check('Journal editor feedback, median', metrics.exerciseOpen.medianFeedback, 150, 'ms');
const slowestRoundtrip = Math.max(...metrics.roundtrips.map(x => x.medianTotal));
check('Repeated navigation slowest roundtrip', slowestRoundtrip, 1000, 'ms');

// Deliberately do not budget deferred Coach total: full statistics continue after first paint.
console.log('PHASE7_PERFORMANCE_GUARD ' + JSON.stringify({checks, failures}));
if (failures.length) {
  console.error('PERFORMANCE REGRESSION:\n' + failures.join('\n'));
  process.exitCode = 1;
} else {
  console.log('PASS Phase 7 structural and generous CI timing regression budgets');
}
